#!/usr/bin/env python3
"""Sign a release with a private backup directory, never putting credentials in Git."""
import argparse
import hashlib
import json
import os
import pathlib
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--key-dir', required=True, type=pathlib.Path)
parser.add_argument('--sdk', default=os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', '')))
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parent.parent
versions = dict(line.strip().split('=', 1) for line in (root / 'version.properties').read_text().splitlines() if '=' in line)
keys = json.loads((args.key_dir / 'signing.json').read_text())
tools = pathlib.Path(args.sdk) / 'build-tools' / '35.0.0'
source = root / 'app/build/outputs/apk/release/app-release-unsigned.apk'
destination = root / 'dist' / ('DriveMod-' + versions['versionName'] + '.apk')
destination.parent.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='drivemod-sign-') as temporary:
    directory = pathlib.Path(temporary)
    store_pass, key_pass = directory/'storepass', directory/'keypass'
    for path, value in [(store_pass, keys['store_password']), (key_pass, keys['key_password'])]:
        path.write_text(value)
        path.chmod(0o600)
    aligned = directory/'aligned.apk'
    subprocess.run([str(tools/'zipalign'), '-f', '4', str(source), str(aligned)], check=True)
    subprocess.run([str(tools/'apksigner'), 'sign', '--ks', str(args.key_dir/'drivemod.jks'),
        '--ks-key-alias', keys['alias'], '--ks-pass', 'file:'+str(store_pass), '--key-pass', 'file:'+str(key_pass),
        '--v4-signing-enabled', 'false', '--out', str(destination), str(aligned)], check=True)
    subprocess.run([str(tools/'apksigner'), 'verify', '--verbose', str(destination)], check=True)
digest = hashlib.sha256(destination.read_bytes()).hexdigest()
(root/'dist/SHA256SUMS').write_text(digest+'  '+destination.name+'\n')
print('Signed:', destination.name)
print('SHA-256:', digest)
