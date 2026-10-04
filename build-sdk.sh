#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_JAR:?Set ANDROID_JAR}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS}"
: "${JAVA_HOME:?Set JAVA_HOME}"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p build/generated build/classes build/dex releases
rm -rf build/classes/* build/dex/*
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$ANDROID_BUILD_TOOLS/aapt2" link -I "$ANDROID_JAR" --manifest app/src/main/AndroidManifest.xml --java build/generated --min-sdk-version 31 --target-sdk-version 36 -o build/resources.apk build/resources.zip
python3 - <<'PY'
from pathlib import Path
Path('build/sources.txt').write_text('\n'.join(str(f) for d in ['app/src/main/java','build/generated'] for f in Path(d).rglob('*.java')))
PY
CP="$ANDROID_JAR"
for f in deps/*.jar; do [[ "$f" == *r8-tool.jar ]] || CP="$CP:$f"; done
javac -encoding UTF-8 -source 17 -target 17 -classpath "$CP" -d build/classes @build/sources.txt
jar --create --file build/classes.jar -C build/classes .
LIBS=()
for f in deps/*.jar; do [[ "$f" == *r8-tool.jar ]] || LIBS+=("$f"); done
java -Xmx2g -cp deps/r8-tool.jar com.android.tools.r8.D8 --release --min-api 31 --lib "$ANDROID_JAR" --output build/dex build/classes.jar "${LIBS[@]}"
python3 - <<'PY'
from pathlib import Path
import zipfile,shutil,os
shutil.copyfile('build/resources.apk','build/unsigned.apk')
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in Path('app/src/main/assets').rglob('*'):
  if f.is_file():z.write(f,'assets/'+str(f.relative_to('app/src/main/assets')))
 for f in Path('build/dex').glob('*.dex'):z.write(f,f.name)
 abi=os.environ.get('PHYSICALCRAFT_ABI','arm64-v8a')
 for f in Path('deps/jni',abi).glob('*.so'):z.write(f,'lib/'+abi+'/'+f.name)
 z.write('deps/LiteRT-LM-LICENSE.txt','assets/licenses/LiteRT-LM-LICENSE.txt')
 z.write('deps/THIRD_PARTY_NOTICE.txt','assets/licenses/THIRD_PARTY_NOTICE.txt')
PY
"$ANDROID_BUILD_TOOLS/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
# Private signing material is local-only and never committed.
# CI generates a temporary development key unless a private key was supplied.
if [[ -z "${PHYSICALCRAFT_KEYSTORE:-}" && ! -f .dev/physicalcraft-debug.keystore ]]; then
 mkdir -p .dev
 keytool -genkeypair -keystore .dev/physicalcraft-debug.keystore -storepass android -keypass android -alias physicalcraft -dname 'CN=PhysicalCraft Development, O=PhysicalCraft, C=BR' -keyalg RSA -keysize 2048 -validity 10000
fi
"$ANDROID_BUILD_TOOLS/apksigner" sign --ks "${PHYSICALCRAFT_KEYSTORE:-.dev/physicalcraft-debug.keystore}" --ks-key-alias "${PHYSICALCRAFT_KEY_ALIAS:-physicalcraft}" --ks-pass "${PHYSICALCRAFT_KEY_PASS:-pass:android}" --out releases/PhysicalCraft-v0.2.0.apk build/aligned.apk
"$ANDROID_BUILD_TOOLS/apksigner" verify --verbose releases/PhysicalCraft-v0.2.0.apk
