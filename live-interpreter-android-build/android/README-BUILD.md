# Android build candidate

This is a build **candidate**, not a compiled or tested APK.

## Android Studio
1. Open `android/` in Android Studio with JDK 17, Android SDK 35, Android NDK 27.2.12479018, and CMake 3.22.1.
2. Clone `https://github.com/ggml-org/whisper.cpp.git` at tag `v1.7.6` into `android/app/vendor/whisper.cpp`. Native source is not bundled in this package.
3. Generate the Gradle wrapper with installed Gradle 8.9 (`gradle wrapper --gradle-version 8.9`) or invoke Gradle 8.9 directly. No wrapper JAR is bundled.
4. Sync and run `:app:assembleDebug`.
5. Install `android/app/build/outputs/apk/debug/app-debug.apk` using Android Studio or ADB.
6. The app also needs a legally obtained multilingual whisper GGML `ggml-base.bin` at app-private path `files/models/ggml-base.bin`. The current app has **no model installer**. Without a model, capture may start but recognition will not work. ML Kit English translation may require first-time Wi-Fi model download.

## GitHub Actions
Push the project root to GitHub and run **Android debug APK** from the Actions tab. The workflow installs SDK/NDK, checks out whisper.cpp, creates a wrapper, attempts compilation and uploads the APK **only if compilation succeeds**. Successful CI compilation is not evidence of working translation.

## Known release blockers
- Not compile-verified in this environment (no SDK, Gradle or network).
- Native whisper.cpp API compatibility with tag v1.7.6 has not been verified.
- Whisper model is missing and no user-friendly installer exists.
- Lifecycle races, source-to-translation latency and background text-to-speech remain unresolved.
- No device acceptance tests passed.
