#!/usr/bin/env python3
"""Fetch pinned open-source runtime/build dependencies. No model weights or credentials."""
from pathlib import Path
import urllib.request, zipfile, hashlib, concurrent.futures
root=Path(__file__).resolve().parent
p=root/'deps';p.mkdir(exist_ok=True)
def download(path,url):
 path.parent.mkdir(parents=True,exist_ok=True)
 if not path.exists():
  req=urllib.request.Request(url,headers={'User-Agent':'PhysicalCraft-build/0.1'})
  with urllib.request.urlopen(req,timeout=120) as response:path.write_bytes(response.read())
 return path
version='0.17.1'
aar=download(p/'litertlm.aar',f'https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/{version}/litertlm-android-{version}.aar')
with zipfile.ZipFile(aar) as z:
 (p/'litertlm.jar').write_bytes(z.read('classes.jar'))
 f=p/'jni/arm64-v8a/liblitertlm_jni.so';f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(z.read('jni/arm64-v8a/liblitertlm_jni.so'))
 (p/'LiteRT-LM-LICENSE.txt').write_bytes(z.read('LICENSE'))
 (p/'THIRD_PARTY_NOTICE.txt').write_bytes(z.read('THIRD_PARTY_NOTICE.txt'))
download(p/'r8-tool.jar','https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.4.28/r8-9.4.28.jar')
items=[('org.jetbrains.kotlin','kotlin-stdlib','2.4.0'),('org.jetbrains.kotlin','kotlin-reflect','2.4.0'),('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.11.0'),('org.jetbrains.kotlinx','kotlinx-coroutines-android','1.11.0'),('com.google.code.gson','gson','2.14.0'),('org.jetbrains','annotations','23.0.0')]
def get(item):
 g,a,v=item;download(p/f'{a}.jar',f'https://repo.maven.apache.org/maven2/{g.replace(".","/")}/{a}/{v}/{a}-{v}.jar')
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:list(pool.map(get,items))
download(root/'tests/json.jar','https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar')
for line in (p/'SHA256SUMS').read_text().splitlines():
 expected,path=line.split(None,1);actual=hashlib.sha256((root/path).read_bytes()).hexdigest()
 if expected!=actual:raise RuntimeError(f'Dependency checksum mismatch: {path}')
print('Pinned dependencies downloaded and checksums verified.')
