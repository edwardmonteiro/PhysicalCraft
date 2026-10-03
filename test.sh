#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/tests
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp tests/json.jar -d build/tests app/src/main/java/com/edward/physicalcraft/Physics.java app/src/main/java/com/edward/physicalcraft/Blueprint.java tests/PhysicsTest.java
"$JAVA_HOME/bin/java" -cp build/tests:tests/json.jar com.edward.physicalcraft.PhysicsTest
