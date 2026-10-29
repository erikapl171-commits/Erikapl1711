# Live Interpreter Android project (Stage 2)

**This is an app interface prototype, NOT a live interpreter yet.** It never opens the microphone and does not transcribe, translate or play audio. All subtitle sentences are fictional English examples.

## Open on a development computer

1. Install current Android Studio with Android SDK Platform 37, Android Gradle Plugin support, and a JDK 17 or later compatible with the chosen Gradle/AGP combination.
2. Open the `android` folder as the project, not the top-level Stage 2 folder.
3. Allow Android Studio to download declared Gradle plugins and SDK dependencies from `google()` and `mavenCentral()`.
4. **This project does not include a generated Gradle wrapper JAR**, because this authoring environment has no Gradle installation/network downloads. Set Android Studio to use a locally installed Gradle 9.4.1+ (AGP 9.2 requires 9.4.1+) and use `gradle wrapper --gradle-version 9.4.1` once, or generate the wrapper using the IDE. Do not mistake this missing build bootstrap for an app runtime feature.
5. Run the `app` configuration on an Android emulator/device, Android 8.0 (API 26) or above.
6. Once Gradle is ready, run `gradle testDebugUnitTest connectedDebugAndroidTest` for project tests and `gradle assembleDebug` for a local debug APK.

The build has **not** been performed in this environment, because there is no Android SDK, Gradle distribution, or network access to obtain them. An APK has not been generated or verified.

## Interface controls

- **From**: choose Latvian or Lithuanian for the eventual recognizer. This does not change the fictional English preview text.
- **To**: fixed English for the first release.
- **Play sample captions / Stop sample**: shows and cancels a timed sample, including mutable draft text and stable caption history. This does not request or use a microphone.
- **Settings**: spoken English preference (stored in memory; synthesis is Stage 6), subtitle size (effective immediately), keep screen awake while demo is active, and **Check microphone access**. The last item uses Android's runtime permission flow but does not open the microphone.
- **Theme**: follows light/dark system settings.
- **Accessibility**: descriptions for actionable icons, scalable captions, high contrast, test tags for the interface states.

The running demo stops on app backgrounding. No captured audio, transcript files, analytics or network requests are produced.

## Build configuration

- Kotlin + Jetpack Compose + Material 3.
- AGP `9.2.0`, Gradle `9.4.1+`, Kotlin Compose compiler `2.3.21`, Compose BOM `2026.09.00`, `compileSdk=37`, `targetSdk=36`, `minSdk=26`.
- No internet permission and no service in the Stage 2 manifest.
- Source state reducer is pure Kotlin for cross-platform static regression checking.
- Generated app package does not contain the developer log or the benchmark tooling.

## Validating without an Android SDK

From the Stage 2 project root:

```bash
bash tests/check_stage2.sh
```

Requires Python 3 and a local Kotlin JVM compiler (`kotlinc`). This does not replace compilation and UI testing on Android.

## Planned next integration

Stage 3 adds `AudioRecord`, a microphone foreground service, an ongoing status notification, and a real mic-driven Start/Stop flow. Only at that stage should the primary action be relabeled from **Play sample captions** to **Start listening**. Stage 4 adds on-device ASR; Stage 5 adds on-device English translation. Stage 6 connects the current audio preference to offline TTS.


Stage 3: Real microphone capture code is under `app/src/main/java/app/liveinterpreter/capture`. See root README and development log for current constraints and hardware testing.
