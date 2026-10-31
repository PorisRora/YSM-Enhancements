"""Download hash-pinned, unmodified test inputs into ignored build/ directories."""
import hashlib
import json
from pathlib import Path
import zipfile
from generate_fabric_mappings import CACHE, download, generate

FIXTURES = {
    '1.20.1': ('LF0NYrRj', 'ed2b5a6fc97df098ef7e731c12f2367ebec2612a2a08b72bb958f508b5802e82858dc406f6b6cc4f99406988aabd6f2bd929a459afd1d101bdd2fdfd125ab1be'),
    '1.21.1': ('eGOUtHWJ', 'c5125c05c9d681ecb38ed7fca4a7915c8af65f90927393b84ab7e0494eda967c3398265c44ac15c2c25188bc2ee16b1311f5cd09848f1d38ad0846d526340ed1'),
}


if __name__ == '__main__':
    for mc, (version_id, expected_hash) in FIXTURES.items():
        metadata = json.loads(download(f'https://api.modrinth.com/v2/version/{version_id}', CACHE / f'ysm-{version_id}.json'))
        name = f'ysm-2.6.5-fabric+mc{mc}-release.jar'
        source = next(f for f in metadata['files'] if f['filename'] == name)
        if source['hashes']['sha512'] != expected_hash:
            raise ValueError(f'Unexpected upstream fixture: {name}')
        content = download(source['url'], CACHE / name)
        if hashlib.sha512(content).hexdigest() != expected_hash:
            raise ValueError(f'Corrupt fixture: {name}')
        with zipfile.ZipFile(CACHE / name) as jar:
            config_api = next(n for n in jar.namelist() if n.startswith('META-INF/jars/forge-config-api-port-') and n.endswith('.jar'))
            (CACHE / Path(config_api).name).write_bytes(jar.read(config_api))
        generate(mc)
        print(f'Verified original YSM + bundled config API: {mc}')
