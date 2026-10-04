#!/usr/bin/env python3
"""Cache public, immutable test weights. Always verify before use in Android."""
import hashlib
from pathlib import Path
import re
import subprocess

source = Path('app/src/main/java/com/edward/physicalcraft/ModelDownload.java').read_text()
url = re.search(r'String URL="([^"]+)"', source).group(1)
expected = re.search(r'String SHA256="([^"]+)"', source).group(1)
path = Path('.ci-model/qwen.litertlm')
path.parent.mkdir(exist_ok=True)
if not path.exists():
    subprocess.run(['curl', '-fL', '--retry', '3', '--connect-timeout', '30', '--max-time', '600', url, '-o', str(path)], check=True)
with path.open('rb') as stream:
    actual = hashlib.file_digest(stream, 'sha256').hexdigest()
if actual != expected:
    raise RuntimeError('Model checksum mismatch')
print('Public model fixture verified:', path.stat().st_size, 'bytes')
