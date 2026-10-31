"""Generate the reflection bridge from Mojang's and Fabric's published mappings.

No Minecraft classes or YSM implementation code are copied to the output.
Run: python tools/generate_fabric_mappings.py 1.20.1 1.21.1
"""
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "build" / "fabric-fixtures"


def download(url, dest, sha1=None):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.exists():
        with urllib.request.urlopen(url, timeout=120) as response:
            dest.write_bytes(response.read())
    if sha1 and hashlib.sha1(dest.read_bytes()).hexdigest() != sha1:
        raise ValueError(f"SHA-1 mismatch: {dest}")
    return dest.read_bytes()


def generate(version):
    manifest = json.loads(download("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", CACHE / "manifest.json"))
    info = next(v for v in manifest["versions"] if v["id"] == version)
    metadata = json.loads(download(info["url"], CACHE / f"{version}.json", info["sha1"]))
    source = metadata["downloads"]["client_mappings"]
    mojang = download(source["url"], CACHE / f"{version}-client.txt", source["sha1"]).decode()
    client = metadata["downloads"]["client"]
    client_path = CACHE / f"{version}-client.jar"
    download(client["url"], client_path, client["sha1"])
    inter = download(f"https://maven.fabricmc.net/net/fabricmc/intermediary/{version}/intermediary-{version}-v2.jar", CACHE / f"{version}-intermediary.jar")
    with zipfile.ZipFile(io.BytesIO(inter)) as z:
        tiny = z.read("mappings/mappings.tiny").decode().splitlines()
    classes, fields, methods = {}, {}, {}
    owner = None
    for line in tiny[1:]:
        parts = line.split("\t")
        if parts[0] == "c":
            owner = parts[1]
            classes[owner] = parts[2]
        elif parts[:2] == ["", "f"]:
            fields[owner, parts[3], parts[2]] = parts[4]
        elif parts[:2] == ["", "m"]:
            methods[owner, parts[3], parts[2]] = parts[4]
    # Intermediary omits inherited overrides. Resolve against the real class
    # hierarchy; falling back to an obfuscated name would silently break reflection.
    parents = {}
    with zipfile.ZipFile(client_path) as z:
        for entry in z.namelist():
            if not entry.endswith('.class'):
                continue
            data = io.BytesIO(z.read(entry))
            def u1(): return struct.unpack('>B', data.read(1))[0]
            def u2(): return struct.unpack('>H', data.read(2))[0]
            data.read(8)
            pool = [None] * u2()
            index = 1
            while index < len(pool):
                tag = u1()
                if tag == 1: pool[index] = data.read(u2()).decode('utf-8', errors='replace')
                elif tag == 7: pool[index] = u2()
                elif tag in (3, 4, 9, 10, 11, 12, 17, 18): data.read(4)
                elif tag in (5, 6): data.read(8); index += 1
                elif tag in (8, 16, 19, 20): data.read(2)
                elif tag == 15: data.read(3)
                else: raise ValueError(f'Unknown constant pool tag {tag}')
                index += 1
            data.read(2)
            owner = pool[pool[u2()]]
            parent = u2()
            values = [pool[pool[parent]]] if parent else []
            values += [pool[pool[u2()]] for _ in range(u2())]
            parents[owner] = values

    def mapped_method(owner, name, desc):
        direct = methods.get((owner, name, desc))
        if direct: return direct
        for parent in parents.get(owner, []):
            result = mapped_method(parent, name, desc)
            if result: return result
        return None
    names = {}
    members = []
    for line in mojang.splitlines():
        if not line or line.startswith("#"):
            continue
        if not line.startswith(" "):
            named, obf = line[:-1].split(" -> ")
            owner = named.replace(".", "/")
            names[owner] = obf.replace(".", "/")
        else:
            members.append((owner, line.strip()))

    def descriptor(name):
        arrays = len(name) - len(name.rstrip("[]"))
        name = name.replace("[]", "")
        primitive = {"void": "V", "boolean": "Z", "byte": "B", "short": "S", "int": "I", "long": "J", "float": "F", "double": "D", "char": "C"}
        raw = name.replace(".", "/")
        return "[" * (arrays // 2) + (primitive[name] if name in primitive else "L" + names.get(raw, raw) + ";")

    def inter_desc(desc):
        return re.sub(r"L([^;]+);", lambda m: "L" + classes.get(m[1], m[1]) + ";", desc)

    # Only reflection-facing types; include their parents/interfaces in this list.
    prefixes = ("net/minecraft/client/gui/", "net/minecraft/client/Minecraft", "net/minecraft/client/KeyboardHandler", "net/minecraft/client/MouseHandler", "net/minecraft/client/KeyMapping", "net/minecraft/client/Options", "net/minecraft/network/chat/", "net/minecraft/world/entity/Entity", "net/minecraft/world/entity/LivingEntity", "net/minecraft/world/entity/player/Player", "net/minecraft/sounds/", "net/minecraft/client/resources/sounds/", "net/minecraft/client/sounds/", "com/mojang/blaze3d/", "net/minecraft/util/FormattedCharSequence", "net/minecraft/client/StringSplitter")
    selected = {n for n in names if n.startswith(prefixes)}
    # Export only identifiers used by this addon, plus their declaring/parameter
    # types. This is a reflection bridge, not a redistributed full mapping set.
    code = '\n'.join(p.read_text(encoding='utf-8') for base in
        (ROOT / 'src/main/java', ROOT / 'fabric/common/src/main/java') for p in base.rglob('*.java'))
    literals = re.findall(r'"([^"\n]+)"', code)
    used = {piece for text in literals for piece in text.split('|') if re.fullmatch(r'[\w$]+', piece)}
    needed_classes = {n for n in names if n.replace('/', '.') in code or n in code}
    out = []
    for owner, line in members:
        if owner not in selected:
            continue
        original, obf = line.split(" -> ")
        original = re.sub(r"^\d+:\d+:", "", original)
        if "(" in original:
            match = re.match(r"(\S+) (\S+)\(([^)]*)\)", original)
            if not match:
                continue
            result, name, args = match.groups()
            desc = "(" + "".join(descriptor(a) for a in args.split(",") if a) + ")" + descriptor(result)
            mapped = mapped_method(names[owner], obf, desc) or obf
            kind = "m"
        else:
            field_type, name = original.split(" ")
            desc = descriptor(field_type)
            mapped = fields.get((names[owner], obf, desc), obf)
            kind = "f"
        if name.startswith("<"):
            continue
        if name not in used:
            continue
        needed_classes.add(owner)
        out.append("\t".join((kind, owner, name, mapped, inter_desc(desc))))
    # YSM's Fabric port bundles its own obfuscated slider implementation.
    # These reviewed aliases are version-specific, unlike Minecraft mappings.
    aliases = ROOT / 'fabric' / ('mc' + version.replace('.', '')) / 'ysm-widget-aliases.tsv'
    out.extend(aliases.read_text(encoding='utf-8').splitlines())
    reverse_types = {classes.get(obf, obf): named for named, obf in names.items()}
    for line in out:
        for type_name in re.findall(r'L([^;]+);', line.split('\t')[-1]):
            if type_name in reverse_types:
                needed_classes.add(reverse_types[type_name])
    out = ['# Mojang official names -> Fabric intermediary; Minecraft ' + version] + [
        '\t'.join(('c', name, classes.get(names[name], names[name]))) for name in sorted(needed_classes)] + out
    dest = ROOT / "fabric" / ("mc" + version.replace(".", "")) / "src/main/resources/ysm_enhancements.names.tsv"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text("\n".join(dict.fromkeys(out)) + "\n", encoding="utf-8")
    print(f"{version}: {len(out)} mapping entries -> {dest}")


if __name__ == "__main__":
    for version in sys.argv[1:]:
        generate(version)
