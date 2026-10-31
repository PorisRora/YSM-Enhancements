"""Check installable artifacts: metadata, Java target, namespaces and SHA-256."""
import hashlib
import json
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]

if __name__ == '__main__':
    for mc, java in [('1.20.1', 17), ('1.21.1', 21)]:
        path = ROOT / 'fabric' / ('mc' + mc.replace('.', '')) / 'build/libs' / f'ysm-enhancements-1.2.0-fabric-mc{mc}.jar'
        with zipfile.ZipFile(path) as jar:
            metadata = json.loads(jar.read('fabric.mod.json'))
            assert metadata['depends']['minecraft'] == mc
            assert metadata['depends']['yes_steve_model'] == f'2.6.5-fabric+mc{mc}'
            assert metadata['depends']['java'] == f'>={java}'
            assert metadata['depends']['fabricloader'] == ('>=0.15.0' if mc == '1.20.1' else '>=0.16.0')
            assert metadata['environment'] == 'client'
            mixins = json.loads(jar.read('ysm_enhancements.fabric.mixins.json'))
            assert mixins['compatibilityLevel'] == f'JAVA_{java}'
            assert not {'META-INF/mods.toml', 'META-INF/neoforge.mods.toml', 'ysm_enhancements.mixins.json'} & set(jar.namelist())
            count = 0
            for name in jar.namelist():
                if not name.endswith('.class'):
                    continue
                assert name.startswith('dev/local/ysmenhancements/'), name
                assert '/testservice/' not in name and not name.endswith('Test.class'), name
                assert '/loader/' not in name and not name.endswith('ShortcutFixes.class'), name
                content = jar.read(name)
                assert content[:4] == b'\xca\xfe\xba\xbe'
                assert struct.unpack('>H', content[6:8])[0] <= java + 44, name
                count += 1
            assert f'Minecraft {mc}' in jar.read('ysm_enhancements.names.tsv').decode().splitlines()[0]
        print(f'PASS: {path.name}; {count} addon classes; Java {java}; SHA256 {hashlib.sha256(path.read_bytes()).hexdigest()}')
