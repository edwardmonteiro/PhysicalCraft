# Open-source dependencies

These are development/runtime libraries, not a game engine. No model weights are included.

- LiteRT-LM Android 0.17.1: Google AI Edge. Apache-2.0; `deps/LiteRT-LM-LICENSE.txt` and full upstream transitive notices `deps/THIRD_PARTY_NOTICE.txt`. JNI ARM64 runtime and `classes.jar` extracted from the same upstream AAR.
- Kotlin stdlib/reflect 2.4.0: JetBrains, Apache-2.0. https://github.com/JetBrains/kotlin
- Kotlin coroutines 1.11.0: JetBrains, Apache-2.0. https://github.com/Kotlin/kotlinx.coroutines
- JetBrains annotations: Apache-2.0. https://github.com/JetBrains/java-annotations
- Gson 2.14.0: Google, Apache-2.0. https://github.com/google/gson
- R8/D8: Google Android tools, BSD-style license. Build tool only; not packaged as a library in the APK. https://r8.googlesource.com/r8/
- JSON-java: test only, https://github.com/stleary/JSON-java ; Android supplies `org.json` in production.

Dependency checksums are recorded in `deps/SHA256SUMS`. The build downloads pinned artifacts from official Maven repositories through fetch-deps.py, then verifies their hashes. Licenses embedded in source artifacts must be preserved when redistributing.

The project has its own MIT license in LICENSE. Gemma weights require their separate terms and are downloaded/imported by the user.
