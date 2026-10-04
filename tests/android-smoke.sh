#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p build/smoke/classes build/smoke/dex
"$ANDROID_BUILD_TOOLS/aapt2" link -I "$ANDROID_JAR" --manifest tests/android/AndroidManifest.xml -o build/smoke/resources.apk
javac -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID_JAR:build/classes" -d build/smoke/classes tests/android/StartupTest.java
jar --create --file build/smoke/classes.jar -C build/smoke/classes .
java -cp deps/r8-tool.jar com.android.tools.r8.D8 --min-api 31 --lib "$ANDROID_JAR" --classpath build/classes.jar --output build/smoke/dex build/smoke/classes.jar
python3 - <<'PY'
import zipfile,shutil
shutil.copyfile('build/smoke/resources.apk','build/smoke/unsigned.apk')
with zipfile.ZipFile('build/smoke/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write('build/smoke/dex/classes.dex','classes.dex')
PY
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 build/smoke/unsigned.apk build/smoke/aligned.apk
"$ANDROID_BUILD_TOOLS/apksigner" sign --ks .dev/physicalcraft-debug.keystore --ks-key-alias physicalcraft --ks-pass pass:android --out build/smoke/test.apk build/smoke/aligned.apk
adb install -r releases/PhysicalCraft-v0.2.0.apk
adb install -r build/smoke/test.apk
adb shell settings put secure immersive_mode_confirmations confirmed
adb logcat -c
adb shell am instrument -w -e full_ai "${PHYSICALCRAFT_AI_TEST:-false}" com.edward.physicalcraft.smoke/com.edward.physicalcraft.StartupTest | tee build/smoke/result.txt
adb logcat -d > build/smoke/logcat.txt
grep -A 20 -E "Model import failed|Local inference failed" build/smoke/logcat.txt || true
adb pull /sdcard/Android/data/com.edward.physicalcraft.explorer/files/preview.png build/smoke/screen.png
grep -q PHYSICALCRAFT_STARTUP_PASS build/smoke/result.txt
if grep -E 'FATAL EXCEPTION|Fatal signal' build/smoke/logcat.txt; then exit 1; fi
