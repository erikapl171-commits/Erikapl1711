# LIVE INTERPRETER: DEVELOPMENT LOG

**Project:** Live Interpreter (Android)
**Last updated:** 2026-10-28
**Status:** Stage 6 English TTS source integration implemented on 2026-10-28. Real translated segments may be spoken using local Android TextToSpeech voices while the subtitle UI is active. Playback, compilation, and hardware runtime remain UNVERIFIED. Whisper native model/source still absent; no APK. Stage 5 contextual translation remains incomplete.
**Owner goal:** Zero-subscription, zero-API-cost mobile interpreter that listens continuously at events/conversations, translates into English with near-real-time text, and optionally reads English aloud.

## 1. Non-negotiable requirements

1. No paid AI API, per-minute charges, mandatory server, or recurring hosting fee.
2. Run on an Android phone, preferably fully on-device after a first-time model download.
3. Continuously capture speaker audio without relying on end-of-utterance silence.
4. Translate speech meaningfully in context rather than literally word by word.
5. Display English subtitles while speakers are still talking; show provisional and finalized text distinctly.
6. Optional English speech output; headphones supported.
7. Intended for long talks/conferences, not just short voice prompts.
8. Prioritize Lithuanian and Latvian input with English output, and support other languages where practical.
9. Clear microphone disclosure and explicit user control; do not retain audio by default.

## 2. Architecture (planned, unimplemented)

- **Platform and UI:** Native Android, Kotlin, Jetpack Compose.
- **Audio capture:** Android AudioRecord feeding an in-memory PCM ring buffer. Microphone foreground service for long sessions, started while app is visible and with proper permissions. Do not store raw audio by default.
- **ASR abstraction:** `SpeechEngine`, evaluated first using quantized multilingual whisper.cpp; compare with sherpa-onnx only if an appropriate supported-language model can be verified. Never assume a streaming model supports Lithuanian or Latvian without checking its exact model card.
- **Translation abstraction:** `TranslationEngine`; first candidate Google ML Kit on-device translation to English (models downloaded initially). Optionally compare Whisper direct-to-English interpretation if model speed/quality permits.
- **Coordinator:** `InterpreterCoordinator` links captured audio, provisional ASR, stable ASR, rolling context, translation windows, finalization and deduplication. Work off the UI thread.
- **Subtitle state:** Immutable stable segments plus one revisable provisional segment. Each segment carries internal sequence and source time bounds so stale asynchronous outputs cannot overwrite newer results.
- **Speech output:** Android TextToSpeech with queue policy, optional mute, headphones and adjustable voice/rate where available. Do not speak provisional text that may change.
- **State and persistence:** Local settings; optional transcript export only after explicit enablement. No automatic upload.
- **UI:** Large English subtitles, start/stop, source language select (manual as MVP), English destination, sound toggle, status/latency indicator, transcript view.
- **No authentication, external backend or monetized service.**

### Architecture and feasibility risks

- Android platform `SpeechRecognizer` is explicitly *not intended for continuous recognition*, and can involve remote recognition. It is not the primary solution.
- Whisper is generally chunk-based, not truly simultaneous. Continuous audio buffering plus overlapping windows may reduce but cannot eliminate interpretation latency. Small models may be inaccurate in Lithuanian/Latvian and large models may be too slow on a phone.
- Google ML Kit provides on-device translation but its documentation says translation is intended for casual/simple use, so conference quality is unverified.
- A completely free solution cannot guarantee cloud-level translation accuracy or a natural high-end synthetic voice. Text-first usability is the fallback if TTS lags.
- Android microphone foreground service requires permissions and complies with modern background-start restrictions.
- Additional local models require initial download and storage. "Offline" means after verified installation and model downloads; availability/performance may depend on device.
- First release selects source language manually; automatic speech language detection is a possible enhancement, not promised as MVP.
- Recordings of people and conferences may be subject to local law/venue rules. Default design does not store audio.

## 3. Dependency-ordered development queue and exit gates

### Stage 1: Architecture and feasibility (RESEARCH COMPLETE; HARDWARE GATE OPEN)
- DONE: Define requirements, architecture, costs, privacy, model candidates, review public licenses and Android constraints.
- DONE: Create runnable benchmark scoring tool and template, representative test protocol, and automated regression checks (9/9 PASS).
- PENDING, requires physical Android device: Benchmark multilingual ASR on 2+ representative Lithuanian/Latvian clips, including uninterrupted speech, memory, thermals and battery.
- PENDING, requires running translation engine: Compare direct speech-to-English output vs ASR + ML Kit English translation for speed and meaning on the same audio.
- PENDING: Verify offline operation with network disabled after model/voice downloads and attribution in the app.
- **Gate:** No evidence yet that the candidate stack meets real-time expectations. Stage 2 UI work can proceed independently; do not mark speech/translation feasible or field-ready until device testing.

### Stage 2: Android interface and shell (SOURCE COMPLETE; ANDROID RUNTIME GATE OPEN)
- DONE: Created native Jetpack Compose single-screen app shell with bilingual source-language selector (`lv` / `lt`), fixed English output, sample captions, preview Start/Stop, transcript area, permissions control, visible demo disclosure, settings bottom sheet, accessibility text sizing, light/dark colors, status and screen-awake preference.
- DONE: Implemented pure immutable interpreter state reducer; 15 JVM smoke assertions pass. Stage 1 9/9 tests still pass. Privacy static checks and manifest parse pass.
- DONE: Android Compose test source and JUnit unit tests added, not executable in this environment.
- PENDING: Gradle dependency sync, Android compilation, emulator/device permission behavior, UI tests, Android 17 runtime compatibility and screenshot/accessibility inspection. No APK yet.
- **Gate:** Source and isolated state logic implemented. UI controls and runtime behavior on Android must be verified before declaring the Stage 2 exit gate passed.

### Stage 3: Continuous microphone capture (SOURCE IMPLEMENTED; ANDROID HARDWARE GATE OPEN)
- DONE: Foreground microphone service, user-initiated capture and Stop notification, in-memory 30-second PCM ring buffer, UI sample count/status, bounded read-loop failure handling, no network or saved audio.
- DONE: Nine pure Kotlin ring-buffer assertions and structural checks; older Stage 1 and 2 tests rerun.
- PENDING: Android Gradle compilation and 30-minute continuous physical-phone session, screen lock, permission revoke, call interruption and service lifecycle tests.
- PENDING: UI/service state reconciliation across activity recreation, validated service restart and Android version-specific behavior.
- Integrate AudioRecord, ring buffer, lifecycle management and foreground service.
- Exit gate: continuous PCM capture, start/stop, permission denial handling, and screen-locked operation where Android permits. Confirm no unauthorized recording persistence.

### Stage 4: Real-time speech recognition
- Integrate selected ASR engine, sliding windows, timestamps, duplicate suppression and provisional/stable text.
- Exit gate: representative speech appears while the speaker is talking; no duplicated/omitted segments across windows; long-session performance measured.

### Stage 5: Context-aware translation
- Integrate local translation model downloads; process clauses on rolling windows with context.
- Use provisional translations that can be revised, committing text only when stable.
- Exit gate: test rapid, uninterrupted speech and quantify word-to-subtitle delay and meaningful translation quality.

### Stage 6: Spoken interpretation
- Android TTS, optional headphone output, voice/rate options when supported, backpressure policy and optional mute.
- Exit gate: spoken translations follow committed segments without endless queue growth, repetitive audio or missing subtitles.

### Stage 7: Usability and robustness
- Add scrollable history, opt-in export, error states, model install UI, battery and thermal management, recovery from audio interruptions.
- Exit gate: successful long-session test and sensible fallback when model is too slow.

### Stage 8: End-to-end QA and delivery
- Regressions for fast speech, uninterrupted speech, accents, foreign names, background noise, language mismatches, low storage, permission refusal, headset removal, model unavailable and screen locking.
- Produce installable APK plus source code and usage instructions where build tools allow, verifying signing, build, installation and basic runtime behavior on device.
- Exit gate: acceptance checks pass and unresolved limitations are clearly documented. An APK is NOT yet built.

## 4. Provisional acceptance criteria (targets, not verified performance)

- Interprets continuously for a 60-minute talk without intentional stop/start cycles.
- Provides English updates without waiting for a speaker's pause.
- No duplicate sentences on overlapping windows.
- Provisional and stable subtitles are visually distinct and stable text does not change retroactively.
- Reconnect/restart behavior does not silently falsify transcript integrity.
- All operational speech processing can run without any paid API; offline claim tested after downloading models.
- Real-world subtitle latency, translation quality, memory consumption, battery and audio lag are measured, not assumed.
- No raw audio saved or sent anywhere by default.

## 5. Test strategy

- **Unit:** segment deduplication, out-of-order results, rolling context boundaries, stable/provisional merging, language mapping, translation queue cancellation.
- **Integration:** microphone -> ASR -> ML Kit -> UI -> TTS; permission denial; model download failure; TTS unavailable.
- **Adversarial:** no pauses for 10 minutes, overlapping chatter, changing noise levels, unusually long words, code-switching, partial recognizer revisions, delayed callbacks, resource exhaustion, screen lock, low battery, thermal throttling.
- **Field:** English outputs from Lithuanian and Latvian events, compared against manual human interpretation for substance and lag.
- Record actual device, Android version, model, measurements, outcomes and fixes in this file after each stage.

## 6. Decisions and unresolved dependencies

| ID | Item | State | Rationale / next check |
| --- | --- | --- | --- |
| D-01 | Native Android, not PWA | Selected for implementation | Better supported microphone lifecycle and local inference |
| D-02 | Zero paid dependencies | Locked requirement | User explicitly requests fully free tool |
| D-03 | Android SpeechRecognizer as core ASR | Rejected | Google documents it as not intended for continuous speech recognition |
| D-04 | Multilingual whisper.cpp | Feasibility candidate | Android example and continuous microphone example documented; CPU and overlap throughput unverified |
| D-05 | ML Kit offline translation | Initial candidate | Translation and Latvian/Lithuanian support documented, quality unverified |
| D-06 | Android TextToSpeech | Selected initial engine | Free system capability, voice quality varies by phone/voice |
| D-07 | Automatic language detection | Deferred | Prefer manual input language for initial reliability |
| D-08 | Transcript history | Local only, opt-in export | Avoid unintended retention and privacy issues |
| U-01 | Accurate real-time Lithuanian/Latvian ASR on actual phone | Unresolved | Device benchmark required |
| U-02 | Meaningful subtitle lag at acceptable quality | Unresolved | Evaluate with recorded, uninterrupted speeches |
| U-03 | License notices/model redistribution | Partially verified | whisper.cpp and Whisper code are MIT; preserve required notices, verify bundled model and dependency terms before distribution. ML Kit attribution is mandatory. |
| U-04 | Specific hardware and available memory | Unresolved | Test on physical Android device when possible; this environment cannot benchmark phone hardware. |

## 7. Development changelog

- **2026-10-28, Planning:** Established 8-stage development queue, candidate native Android architecture, privacy and cost requirements, ASR risks, exit gates and test plan.
- **2026-10-28, Stage 1 research:** Verified supported `lt`/`lv` language codes in Whisper tokenizer and ML Kit supported-language list; Android whisper.cpp sample, chunk-based stream example, free ML Kit on-device SDK, voice offline capability API, microphone foreground-service limitations, source licenses, and mandatory translation attribution. Chose a **provisional** dual-stage ASR->MT stack. Observed a reported performance hazard in repeated overlapping Whisper inference on Android; do not treat the example stream as proof of live performance.
- **2026-10-28, Stage 1 tooling:** Added standard-library-only `benchmarks/score.py`, blank measurement manifest, usage/test protocol, tests and project README. **9/9 automated unit tests PASS** and unpopulated benchmark input was correctly rejected. Real Android functional, quality, battery, latency, and no-network tests **NOT RUN** because a phone/runtime and representative permitted speech recordings are not available here. No APK created.

- **2026-10-28, Stage 2 interface implementation:** Added Android Gradle project (`android/`), Kotlin/Compose UI, isolated immutable state reducer, ViewModel-driven timed fictitious subtitles with revisable draft and stable committed text, status, manual language selection, Settings sheet (subtitle sizes, audio preference for Stage 6, stay-awake toggle, separate mic-permission prompt), privacy disclosure, dark/light palette, and stopped-on-background demo lifecycle. Deliberately avoided `AudioRecord`, `SpeechRecognizer`, any translation SDK and INTERNET permission in Stage 2. No real audio, translation or TTS is represented as operational.
- **2026-10-28, Stage 2 validation:** Stage 1 benchmark regressions **9/9 PASS**; pure Kotlin Stage 2 state smoke assertions **15 PASS** using Kotlin/JVM compiler; Android manifest and resources XML parse **PASS**; code/manifest check for forbidden capture/network/service calls and required demo labels **PASS**. Added 5 Android JUnit tests and 2 Compose instrumentation tests as **NOT RUN**. Full Android build **NOT RUN**, no Android SDK/Gradle/network. Stage 1 device feasibility still unverified.

## 8. Bug ledger

### S3-B01, unresolved: status desynchronizes on UI recreation
- Symptom: Reopening/recreating the UI may display `Stopped` while microphone service remains active.
- Root cause: Stage 3 uses non-sticky, app-private broadcasts for progress rather than a queryable service state repository.
- Fix: Pending persistent service-to-UI state via explicit binding or observable repository, reconcile on attach.
- Regression test: Recreate activity with capture active and verify true status, then stop from either screen or notification (DEVICE NOT RUN).

### S3-B02, potential: microphone interruption during foreground capture
- Symptom: OEM audio routing or privacy revocation can cause repeated read errors / loss of input.
- Root cause: AudioRecord hardware/device policy variation; the code stops on repeated read failures rather than silently continuing.
- Fix: Device testing and error-specific recovery policy pending.
- Regression test: Revoke permission, take a call and flip microphone toggle while capturing (DEVICE NOT RUN).


No reproduced runtime defects recorded so far. A full Android build and UI execution have not been possible; unexecuted checks are not counted as passing. Stage 2 protections implemented proactively: stop/cancel sample coroutine when requested or app backgrounds; reset provisional captions on stop; prevent caption changes after complete; separately label permission testing and demo playback. Regression names: `doesNotShowCaptionsWhenStopped`, `draftsAreReplaceableButFinalsAreImmutable`, `ignoresBlankTranscriptsAndLateResults`, `restartClearsOldCaptionHistoryButKeepsUserChoices`, and `stage2_state_smoke.kt`. For each future defect, record symptom, cause, fix and named regression. Consult this ledger before debugging.

## 8a. Stage 3 changelog and validation (2026-10-28)
- Added `capture/AudioRingBuffer.kt` with bounded, synchronized oldest-first snapshot and zeroizing clear. Added `capture/MicrophoneService.kt` with 16 kHz mono PCM, AudioRecord blocking reads, microphone-type foreground service, explicit stop notification, START_NOT_STICKY and periodic app-scoped status broadcasts.
- Added RECORD_AUDIO and foreground-service permissions, non-exported microphone service manifest entry, and a separate real-microphone control/status in the UI. Retained demo captions and explicit non-translation disclosures.
- Validation: `tests/check_stage3.sh`: Python benchmarks 9/9 PASS; Stage 2 state smoke 15 PASS; Stage 3 ring-buffer smoke 9 PASS; XML/permission/structural checks PASS.
- Not validated: Actual Gradle build, APK, Android permissions/runtime, source rate availability, long-session captures, foreground restrictions, screen-lock behavior and restart safety. Source implementation is not a demonstrated device-working app.
- Handoff: Open `android/` in Android Studio, compile and resolve any build errors first, then run the README device acceptance plan before moving into Stage 4. No paid API, server or Internet permission included.

## 9. Research references (verified public documentation)

- Android SpeechRecognizer restrictions: https://developer.android.com/reference/android/speech/SpeechRecognizer
- ML Kit on-device translation: https://developers.google.com/ml-kit/language/translation
- ML Kit language support (lv, lt): https://developers.google.com/ml-kit/language/translation/translation-language-support
- ML Kit translation guidelines/attribution: https://developers.google.com/ml-kit/language/translation/translation-terms
- whisper.cpp Android example: https://github.com/ggml-org/whisper.cpp/blob/master/examples/whisper.android/README.md
- whisper.cpp continuous microphone stream example: https://github.com/ggml-org/whisper.cpp/blob/master/examples/stream/README.md
- sherpa-onnx capabilities: https://github.com/k2-fsa/sherpa-onnx
- Android microphone foreground limitations: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android TextToSpeech API: https://developer.android.com/reference/android/speech/tts/TextToSpeech

## 10. Handoff instructions

- Current state is **Stage 3 capture source implemented, Android build/installation unverified**. No APK, verified device microphone operation, speech recognition, translation or spoken interpretation yet.
- Next step: sync/build `android/` with an Android SDK 37 and Gradle 9.4.1+ (wrapper JAR absent), run the existing Android tests and device smoke checks, resolve compile/runtime defects, then verify Stage 3 capture service on a physical Android device. Stage 1 hardware feasibility gate remains open and must be revisited with genuine Lithuanian/Latvian audio.
- Keep this file as the single project record. Update the architecture/queue/decisions/change history after every substantive stage. Put bugs and regression results here, not in the production app.
- No paid API integrations. Do not silently introduce hosting or proprietary licensed runtime charges.
- Validate all actual model capabilities and licenses before declaring a stage complete.

## 11. Stage 1 architecture baseline and implementation contract (2026-10-28)

### Verified research vs inferred feasibility

| Capability | Evidence | Decision / confidence |
| --- | --- | --- |
| No per-minute software API bill | Google ML Kit documentation describes on-device APIs as available at no cost; whisper.cpp and Whisper implementations are MIT. | Verified at documentation level, subject to dependency compliance. Internet/data charges for one-time model downloads are not covered. |
| Latvian / Lithuanian candidates | Whisper tokenizer lists `lv` and `lt`; ML Kit language list includes `lv`, `lt`, `en`. | Listed support verified; real ASR/translation quality **unknown**. |
| Android Whisper execution | Upstream repository contains a sample Android app; the streaming example runs repeated inference on incoming microphone audio. | Technical starting point verified; not evidence of low-latency speech interpretation. |
| On-device translation | ML Kit downloads and manages offline language models and translates text on device. | Documented capability, not yet tested on device. Quality explicitly limited for some complex material. |
| Long-running microphone | A correctly started Android microphone foreground service can continue capture; Android 14+ while-in-use/background-start constraints apply. | Start service from visible activity after RECORD_AUDIO grant; verify on actual Android version. |
| Offline spoken translation | Android TextToSpeech voice metadata identifies whether network is required. | Only choose a voice returning false for `Voice.isNetworkConnectionRequired`; availability and perceived voice quality remain unverified. |

### First-release scope

- Android only, Kotlin and Jetpack Compose; no account, advertising, backend, analytics, subscription or paid API.
- Mic capture: Android `AudioRecord` (16-kHz mono PCM target), short rolling in-memory audio buffer, foreground microphone service with ongoing notification, user-initiated start/stop and stop control in notification.
- ASR: multilingual whisper.cpp (not English-only models), language fixed by user in MVP. Start tests with `tiny` and `base`, then quantization variants where quality/compatibility are checked. Use JNI abstraction, model loaded once, CPU time controlled and measured.
- Translation: `TranslationEngine` interface. First candidate Google ML Kit source `lt` or `lv` to target `en`. Pre-download model on Wi-Fi; translation runs locally. Google attribution required in user-facing UI. Compare Whisper multilingual direct English-translation mode as a secondary path, not a proven feature.
- Flow: `AudioCapture -> SpeechEngine -> SegmentStabilizer -> TranslationEngine -> CaptionRepository -> Compose UI`. A committed caption is immutable; one provisional caption may be replaced. Tag each ASR window with monotonic capture timestamps and sequence IDs. Out-of-order callbacks are ignored, not mis-ordered.
- Timing candidate for research, NOT tuned: approximately 4-6 seconds audio context, update every 1.5-2 seconds with overlap; select timing only after measured CPU time. Do not assume multiple concurrent inferences are faster than a bounded serial inference queue.
- Context: translate short meaningful phrases; include prior stable source text as local context if supported by the translator. **ML Kit does not expose long-conversation semantic memory**; simply concatenating sentences may hurt, not improve translation. Provisional text revised on more evidence; never claim human-equivalent simultaneous interpreting.
- UI: large English captions, visible Listening/Processing/Behind/Model Download Required status, fixed-language chooser, sound toggle, scrollback in memory, distinct provisional style, clear privacy disclosure.
- Audio: only translate committed phrases aloud; skip stale items rather than queueing minutes of narration. TTS not allowed to interfere with microphone capture (headphones preferred; test echo).
- Privacy: no raw audio storage by default; no remote upload; no persistent transcript by default; on-device permission, storage and session handling; optional export is explicit. Disable sensitive-content logging. Original recordings used for testing are permissioned and are not included in releases.
- Packaging: no model bundled until binary/model source rights are verified. Validate downloaded model checksums, provide attribution and licenses, avoid arbitrary third-party model mirrors. Verify all operations work in airplane mode **after** model acquisition.

### Proposed module boundaries and future Android project tree

```text
live-interpreter/
  LIVE-INTERPRETER-DEVELOPMENT-LOG.md   # developer-only, excluded from APK
  README.md
  benchmarks/
    score.py
    manifest.example.json
    README.md
  tests/
    test_score.py
  android/                              # added in Stage 2
    app/src/main/java/.../
      ui/                               # Compose screen and UI state
      capture/                          # AudioRecord, mic foreground service
      asr/                              # SpeechEngine, whisper JNI adapter
      translation/                      # TranslationEngine, ML Kit adapter
      captions/                         # stabilization, sequencing, history
      speech/                           # TTS and queue policy
      models/                           # download status, checksum checks
      settings/                         # local settings only
    app/src/main/cpp/                  # whisper.cpp JNI bridge
    app/src/test/                      # pure-logic tests
    app/src/androidTest/               # Android lifecycle/integration tests
```

**Stage 2 is allowed to create the UI shell with mock data while device benchmarks remain pending.** Avoid copying preliminary unverified timings into user-facing UI as guaranteed performance.

### Stage 1 verification matrix

| Test | Outcome | Evidence/next action |
| --- | --- | --- |
| Research model support for `lt`, `lv`, `en` | PASS: documented | Whisper tokenizer and ML Kit language list |
| Confirm Whisper candidate local Android project | PASS: documented | Official Android example, still must integrate |
| Confirm no per-minute translation bill | PASS: documented | ML Kit at no cost; no paid service selected |
| Review microphone foreground service and TTS metadata | PASS: documented | Official Android references |
| Create scoring and device-benchmark protocol | PASS | `benchmarks/` source and notes |
| Benchmark scoring unit regression | PASS: 9/9 | `python3 -m unittest discover -s tests -v` |
| Reject empty measurement manifest | PASS | CLI exits 2 with explicit diagnostic |
| Actual multilingual model on Android phone | **NOT RUN** | Hardware / model / recorded speech needed |
| Test Whisper direct translation against ASR + ML Kit | **NOT RUN** | Physical device + bilingual assessment |
| Prove subtitle accuracy, speed, long-session reliability | **NOT RUN** | Cannot infer these from documentation |
| Prove airplane-mode translation / TTS | **NOT RUN** | Requires model and offline voice downloaded |

### Risk and mitigation ledger (no bugs yet)

- **R1 / Critical:** Rolling Whisper inference can recompute windows too often, creating processing backlog. Benchmark fixed-window cadence and throttling, choose language-appropriate streaming model if validated, degrade gracefully rather than queue work indefinitely.
- **R2 / Critical:** Tiny/base models may fail on Latvian/Lithuanian fast speech. Compare with human reference; a bigger model is not automatically acceptable if it misses latency targets.
- **R3 / High:** Phrase-by-phrase ML Kit translations may omit context, mishandle negation, names and numbers. Mark provisional outputs and audit meaning, not just WER.
- **R4 / High:** Android microphone lifecycle, low-power conditions or thermal throttling may interrupt long sessions. Foreground service + permission tests + explicit lost-audio indication.
- **R5 / Medium:** Offline TTS voice may sound robotic; never silently select network-required voice when offline-only is expected. Select best available offline voice; text-first fallback.
- **R6 / Medium:** First-time model download requires connectivity and storage; show status and failure handling, don't misrepresent downloading as free data usage.
- **R7 / Compliance:** ML Kit translation attribution and third-party notices must be implemented before distribution. No unlicensed audio fixtures.

### Stage 1 benchmark targets (engineering goals, NOT measured)

- Update English caption during ongoing speech; no requirement for pause detection.
- First visible English subtitle: aim p50 <= 3 seconds and p95 <= 6 seconds relative to aligned phrase end, with review of mid-phrase updates. Targets subject to real-device feasibility.
- Processing throughput no growing backlog over 10 continuous minutes; test 60-minute endurance before event-ready declaration.
- Stable text remains stable; no unnoticed dropped/duplicated phrases; display out-of-sync state when processor falls behind.
- Quality: bilingual reviewer tracks semantic fidelity, facts, numeric values, negatives and names; WER alone is insufficient.
- Device/thermal/battery measurement and offline-with-network-disabled test required.

### Benchmark tool notes

`benchmarks/score.py` uses only Python standard library. It preserves Latvian and Lithuanian diacritics, calculates Unicode-token ASR word error rate, evaluates p50/p95 subtitle timing, counts missing output, and reports compute-time/audio-duration ratio if provided. The tool rejects empty or implausible manifests and has synthetic regression tests. It does **not** collect audio or measure hardware itself, and it is not part of the production APK.

### Source links verified 2026-10-28

- Free local ML Kit APIs: https://developers.google.com/ml-kit/guides
- ML Kit translation / limitations: https://developers.google.com/ml-kit/language/translation
- ML Kit Android API and dynamic models: https://developers.google.com/ml-kit/language/translation/android
- ML Kit languages (`lv`, `lt`): https://developers.google.com/ml-kit/language/translation/translation-language-support
- ML Kit attribution: https://developers.google.com/ml-kit/language/translation/translation-terms
- Whisper language tokenizer: https://github.com/openai/whisper/blob/main/whisper/tokenizer.py
- Whisper direct translation model caveat: https://github.com/openai/whisper/blob/main/README.md
- Whisper code license: https://github.com/openai/whisper/blob/main/LICENSE
- whisper.cpp code license: https://github.com/ggml-org/whisper.cpp/blob/master/LICENSE
- whisper.cpp Android sample: https://github.com/ggml-org/whisper.cpp/blob/master/examples/whisper.android/README.md
- whisper.cpp microphone streaming example: https://github.com/ggml-org/whisper.cpp/blob/master/README.md
- Android microphone service restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android microphone service permission: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android TTS offline voice flag: https://developer.android.com/reference/android/speech/tts/Voice#isNetworkConnectionRequired()
- Community report of overlapping inference slowdown, unverified on our device: https://github.com/ggml-org/whisper.cpp/discussions/3567

## 12. Stage 2 implementation and verification record (2026-10-28)

### Delivered source organization

```text
android/
  settings.gradle.kts                  # Android project and dependency repositories
  build.gradle.kts                     # AGP + Compose compiler plugins
  app/build.gradle.kts                 # compile/target/min SDK and dependencies
  app/src/main/AndroidManifest.xml     # RECORD_AUDIO only, no INTERNET or service
  app/src/main/java/app/liveinterpreter/
    MainActivity.kt                    # Activity, stay-awake policy, demo stop on background
    core/InterpreterState.kt           # immutable state and pure state-machine transitions
    ui/InterpreterViewModel.kt         # coroutine-backed synthetic sample stream
    ui/InterpreterScreen.kt            # Compose UI and settings sheet
  app/src/test/                        # Android-targeted JVM unit cases
  app/src/androidTest/                 # Compose UI instrumentation cases
tests/check_stage2.sh                  # deterministic no-SDK verification entry point
tests/stage2_state_smoke.kt            # pure Kotlin smoke assertions
```

### Deliberate scope boundary

- Stage 2 demo shows *fictional prewritten English* text regardless of the selected source language; explicit demo banners and primary action wording avoid suggesting that it listens or translates. There is no microphone access except an isolated Android permission test triggered from Settings.
- The spoken English toggle changes an in-memory preference but cannot and does not play speech until Stage 6. Persisted preferences are Stage 7 work.
- App stops sample playback upon `onStop()` because real background capture and its foreground service are Stage 3, not silently simulated.
- No internet permission, network code, analytics, recording files, login, server or paid integration.
- Gradle config: AGP 9.2.0, Gradle requirement >=9.4.1, Kotlin Compose plugin 2.3.21, Compose BOM 2026.09.00, SDK 37 compile / SDK 36 target / min 26. Component versions based on 2026 Android official references. Build compatibility is unverified until compilation.
- No Gradle wrapper binary/JAR included (unavailable in isolated environment). Generating a standard wrapper and running full Gradle sync is the first task in a development environment with SDK/network tools.
- Development log and test harness excluded from app module and APK inputs.

### Stage 2 acceptance matrix

| Check | Result | Method |
| --- | --- | --- |
| Stage 1 scoring regressions remain passing | PASS (9 tests) | Python `unittest` |
| Stage 2 pure Kotlin start/stop/draft/commit/finish | PASS (15 assertions) | `kotlinc` then `java -jar` |
| State ignores captions after stop and completion | PASS | Kotlin smoke and unexecuted Android JUnit counterparts |
| State retains selected language/voice preference on demo restart | PASS | Kotlin smoke |
| Manifest XML/resources parse | PASS | Python XML parser |
| No `INTERNET` or service declared; microphone not opened | PASS (static check only) | Manifest/source assertions |
| Visibly labeled fake captions and mic-inactive status | PASS (source inspection only) | UI source and static check |
| Android app Gradle sync and compile | NOT RUN | No Android SDK, Gradle, or dependency network |
| Android emulator/device interactions | NOT RUN | No emulator or connected Android phone |
| Android mic permission dialog functional | NOT RUN | Requires Android runtime |
| UI screenshot, font scaling and accessibility inspection | NOT RUN | Requires Android runtime |
| Actual speech/translation performance and offline benchmarks | NOT RUN | Planned for later stages/device gate |

### New decisions / risks

- **D-09:** Run Stage 2 as an explicitly labeled demonstration; avoid a fake "Start listening" button. Rename only after genuine capture is implemented in Stage 3.
- **D-10:** Expose mic permission check separately from preview so Stage 2 cannot falsely imply it is recording.
- **D-11:** Use a pure Kotlin state reducer and VM-driven Compose UI to keep caption stability, language settings and future integration testable outside the Android SDK.
- **D-12:** Android Studio development project without bundled Gradle wrapper JAR due to tooling isolation. A developer must generate wrapper with Gradle >=9.4.1 and verify compatibility before shipping APK.
- **R8:** Android compile/API incompatibilities cannot be ruled out until full Gradle sync, despite static and Kotlin smoke checks. Regression tests must be run immediately on first capable Android build host.
- **R9:** Permission prompt on Android might require user settings after permanent denial. Stage 2 shows denial and should be tested on real device.

### Next actions, ordered

1. Open `android/` on a development machine with SDK 37 and Gradle 9.4.1+; generate wrapper and perform `gradle assembleDebug`.
2. Run `gradle testDebugUnitTest connectedDebugAndroidTest` on emulator/device, fix any actual issues, and update bug ledger with cause, fix, regression.
3. Test landscape, large font, screen awake toggle, denied/granted mic permission and background/foreground transitions on a modern Pixel.
4. Begin Stage 3 `AudioRecord` + microphone foreground service, preserving the distinction between active capture and mock captions.
5. Eventually revisit Stage 1 hardware benchmark before asserting functional near-real-time interpreter.


## 12. Stage 4 implementation and handoff (2026-10-28)

**Scope completed in source:**
- Native whisper.cpp JNI binding (`recognition/WhisperBridge.kt`, `src/main/cpp/live_whisper.cpp`), with explicit `lv`/`lt` original-language transcription. The `base` *multilingual* GGML model is required.
- NDK CMake glue requiring a locally checked-out whisper.cpp under `android/app/vendor/whisper.cpp`. No prebuilt native libraries, models, proprietary APIs, or Internet permission added. Neither the model nor the upstream native code is included in the project archive.
- Live audio windows: 5-second snapshots, triggered every 4 seconds, with single-threaded inference and busy skip to avoid unbounded backlog. Original-language text broadcast to a separate live transcript card; fictional English demo captions remain explicitly separate.
- Transcript overlap removal heuristic and portable scheduling tests. Stage 4 source tests added to `tests/check_stage4.sh`.

**Regression results:** Stage 1 Python 9/9, Stage 2 state assertions 15/15, Stage 3 ring buffer 9/9, Stage 4 scheduler/overlap 9/9, XML/permission/structural checks PASS. Full Gradle, native JNI linking, Android device behavior, recognition correctness, word error rate and latency NOT RUN. No installable APK produced.

**Known blockers / unresolved dependencies:**
- `whisper.cpp` dependency checkout + version pin, NDK tooling, and multilingual `ggml-base.bin` model must be provided to build and provision a running test installation. The current app has no model download/import UI, so model provision requires developer tooling.
- Chunked Whisper decoding may lag behind real speech, especially on modest hardware; performance remains unknown.
- `S3-B01` UI microphone status reconciliation remains unresolved. Service state is broadcast-only.
- Native inference shutdown can race service destroy, needs native lifecycle validation before field use.
- Overlap matching is text-based and only reliable for exact token repetition; timestamps/robust alignment are not yet available.
- Model initialization is asynchronous and status notifications may be overwritten by subsequent capture status updates. Improve diagnostics.
- Some input windows may be intentionally skipped under load to avoid unlimited backlog, so speech can be lost.

**Bug ledger addition:**
### S4-B01, fixed: transcription window scheduled too early after initial 5-second trigger
- Symptom: after first 5-second window, another inference was scheduled only 3 seconds later when accumulated samples crossed the wrong stride boundary.
- Root cause: `sinceLast` modulo 4 seconds ran before the 5-second initialization boundary, creating a 1-second offset.
- Fix: Track absolute nextBoundary, start at 5 seconds, increment by 4 seconds on trigger; skip missed boundaries monotonically.
- Regression: `stage4_asr_smoke.kt` checks 0->5->9 seconds and reset (PASS).

### S4-B02, unresolved: native inference shutdown and model initialization lifecycle
- Symptom (potential): service stop while JNI inference/init executes may race native context free or late initialization.
- Root cause: worker termination, `shutdownNow` and native context lifetime aren't coordinated through a single executor teardown guarantee.
- Fix pending: maintain native context exclusively on one inference executor, await completion and prevent publishing results after stop; test rapid Start/Stop and lifecycle recreation on device.
- Regression pending: 20 rapid starts/stops while model loads and infers, verify no crash/leaked threads.

**Next implementation priority:** complete native dependency integration/compile and on-phone bench to close the Stage 1 feasibility gate before asserting Stage 4 is operational. After verified ASR, Stage 5 translates stable source segments into English with a clearly labeled provisional subtitle state.

## 13. Stage 5 source implementation and handoff (2026-10-28)

### Architecture / implementation
- `translation/OnDeviceTranslator.kt`: Google ML Kit translate 17.0.3, `lv`/`lt` to English. `downloadModelIfNeeded` requires Wi-Fi on the first model acquisition and uses downloaded model afterwards. Not a paid API; network permission was added only for initial model downloads.
- `translation/TranslationQueue.kt`: bounded ordered queue with session identifiers, monotonically increasing sequence IDs and explicit stale callback rejection. Queue overflow emits a notice rather than silently claiming complete coverage.
- `MicrophoneService.kt`: recognizes original-language ASR tails, enqueues them on the main handler, initializes one translator for the chosen session, serially translates source fragments and broadcasts committed English segments. Stop invalidates outstanding session state.
- `InterpreterScreen.kt`: adds English translation panel, preparation/errors, stable append-only English segments and a visible pending-translation indicator. Existing demo subtitle panel remains separately labeled.

### Scope limitations and corrections
- The provisional indicator is **not** a partially translated English sentence. It indicates speech awaiting translation. More contextual revised English text, rollback of ASR hypotheses and synchronization to audio timestamps remain pending. This should not be described as completed context-aware simultaneous interpretation.
- Segments are translated independently via ML Kit. Contextual coherence across segments is **not** established, and quality on continuous speeches is untested. ML Kit can be insufficient for nuanced stage speeches.
- `INTERNET` permission means current application cannot categorically claim to be network-incapable, but no audio or source/translated text is intentionally transmitted by this code. Offline runtime must still be verified after downloading the model and reviewing SDK behavior.
- Prior Stage 3/4 test scripts were updated for intentional Stage 5 changes to Internet permission and UI, while retaining historical functionality checks; historical Stage 3 'no INTERNET' assertion no longer applies.
- Translator model availability callback and `WhisperBridge` preparation can occur at different times; translation queue drains after both ASR and translation are ready. First download requires internet and can block until Wi-Fi. There is no explicit progress or cancellation UI.
- No full Android build or APK: Android SDK, native whisper.cpp checkout/model and NDK build remain unresolved. Do not assert deployable or functioning live translation.

### Validation
- PASS: Stage 1 benchmark unit tests 9, Stage 2 pure Kotlin state assertions 15, Stage 3 audio buffer tests 9, Stage 4 scheduling/overlap assertions 9, Stage 5 queue assertions 12 and Stage 5 manifest/wiring static checks.
- NOT RUN: Gradle/NDK build, ML Kit initialization, real translation output, offline mode, latency, source-language translation correctness, load, speaker continuity, background lifecycle and physical Android device.

### Bug ledger additions
- **S5-B01, fixed.** Symptom: pure Kotlin compiler rejected `ArrayDeque.addLast`. Root cause: unsupported Kotlin default collection method in bundled compiler. Fix: use `java.util.ArrayDeque` and `pollFirst` for queue. Regression: Stage 5 Kotlin smoke test passes.
- **S5-B02, open.** Symptom: translation model is prepared automatically upon recording start and may remain pending indefinitely when Wi-Fi is unavailable. Cause: `requireWifi()` and no explicit download management UI. Planned fix: Stage 7 model manager with current download state and retry; verify offline behaviour.
- **S5-B03, open.** Symptom: speakers' context across fragment boundaries can be lost and draft English text is not actually revised. Cause: per-ASR-tail ML Kit translation rather than a context-aware incremental translation model. Fix pending: rolling context, stable/draft distinction, proper source timestamps, translation reconciliation and benchmark comparison.
- **S5-B04, open.** Symptom: out-of-order broadcast results can escape across session restart if listeners reconnect. Cause: UI sequence counter reset without a session identifier on broadcast, plus Stage 3 existing status-reconciliation bug. Fix pending: explicit session IDs for service broadcasts and queryable bound service state.

### Next development steps
1. Resolve Stage 4 build/runtime blockers, verify native model and recording end to end.
2. Resolve session identity and stop/start races; test actual ML Kit English translations and model preparation.
3. Benchmark on-device latency and accuracy for real Latvian and Lithuanian sustained speeches; optimize window sizes from measurements.
4. Implement truly provisional/context-aware English revisions before claiming Stage 5 functional gate is closed.
5. Stage 6 optional Android offline TextToSpeech may be developed independently but must not be advertised as verified until tested on hardware.


## 14. Stage 6 implementation and handoff (2026-10-28)

### Delivered in source
- `speech/EnglishSpeaker.kt`: Android `TextToSpeech` English (`Locale.US`) with explicit offline-voice requirement; enumerates selectable non-network English voices and adjustable playback speed (0.75x to 1.5x). Engine is lifecycle owned by the Compose screen. Translation callbacks, not demo text, call `speaker.submit`.
- `speech/SpokenQueue.kt`: bounded backlog of 3 pending captions, FIFO, drops oldest pending when behind. No speculative/draft text is spoken. A currently speaking caption is not forcibly cut short when new captions arrive.
- On/off immediately cancels speech and clears queued captions. Microphone Stop and fresh Start reset the audio queue and stop playback. Utterance ID and session epoch reject late callbacks after reset. TTS initialization callback posted to main handler so the engine instance is assigned first.
- Settings now include spoken English toggle, speed slider, and a list of installed offline English voices. Visible TTS errors and missing-voice state. Audio route follows Android's system output route; no forced Bluetooth routing.
- No paid APIs or uploads added in Stage 6; Android local TTS may require separate speech voice-data installation by user.

### Verification
- PASS: 9 Python benchmark tests, 15 Stage 2 state assertions, 9 Stage 3 ring-buffer tests, 9 Stage 4 scheduling assertions, 12 Stage 5 translation queue assertions, 10 Stage 6 bounded audio queue assertions, source structural checks.
- NOT RUN: Android SDK/Gradle build, device TTS initialization, voice availability, real audio route, screen backgrounding, speaker latency, full recording-to-spoken translation pipeline.
- SDK/NDK and whisper.cpp native source/model remain missing; no APK and no end-to-end interpretation verified.

### Open bugs and operational limitations
- **S6-B01, unresolved:** TTS instance is tied to Compose screen, so if the UI is disposed while microphone foreground service remains active, audible interpretation stops. Root cause: the service has no TTS ownership. Planned fix: move TTS to service or application-scoped audio controller and expose configuration state; test backgrounded and screen-locked playback.
- **S6-B02, unresolved:** Voice picker may initially be empty if TTS engine initializes after settings are opened, because voice list is not observable state. Planned fix: publish engine-ready state and observe voice availability; test first install on phone.
- **S6-B03, unresolved:** Entire translated chunks are read as separate utterances without synchronized source timestamps; spoken output may lag stage audio or miss text if queue overflows. Policy is intentionally bounded and prioritizes current content. Planned fix: instrument backlog/latency and test uninterrupted 30-minute speeches.
- **S6-B04, unresolved:** Speech may come out of phone speaker when earphones are disconnected; follows Android default route. Planned fix: optional earphones-only toggle and audio route detection with explicit handling.
- **S6-B05, unresolved:** Speech on screen disposal may be terminated while ongoing; UI recreation loses queued audio and voice selection is only session-local. Planned fix: move controller ownership and persist audio preferences.
- **S6-B06, unresolved:** The Stage 5 broadcast path lacks session identifiers, so extremely late translation broadcasts could cross new sessions; do not claim strong session isolation until resolved.

### Bug fixes and regressions
- **S6-F01, fixed in source:** TTS initialization callback could observe `engine == null` before object assignment. Deferral using main-thread handler. Structural test plus pending Android runtime test.
- **S6-F02, fixed in source:** Old speech completion callback could resume playback after explicit Stop. Utterance ID + generation epoch matching and queue clearing. Pure queue tests + pending device test.
- **S6-F03, historic tests need contextual updates:** Stage 2's original static test expects no microphone, translator or Internet permissions. Stage 3's original static test message says "no Internet permission" despite Stage 5 requiring model downloads. These old stage-specific static checks are not valid against the later integrated project. `tests/check_stage6.sh` runs retained component logic regressions plus current-stage structural invariants without discarding historical test files.

### Next actions
1. Build app with Android SDK, NDK, pinned whisper.cpp, multilingual model and run tests on real phone.
2. Repair broadcast session/reconnect state and native-service lifetime safety issues.
3. Move TTS controller to service for background spoken translation; add headphone disconnect policy and persisted settings.
4. Measure audio latency and backlog on long Latvian/Lithuanian speeches.
5. Complete Stage 5 genuinely context-aware draft and stable English revisions, then progress to Stage 7 hardening.

## 16. Stage 8 release evaluation, 2026-10-28

**Release verdict: BLOCKED.** This stage adds release safety checks and a 16-case on-device acceptance checklist but does NOT produce or claim a verified APK.

### Work completed
- Copied Stage 7 project without modifying production runtime behavior; added `tests/check_stage8.py`, `tests/check_stage8.sh`, `release/DEVICE-ACCEPTANCE-CHECKLIST.md` and `release/STAGE8-TEST-RESULTS.txt`.
- Confirmed installed runtime has Java and Kotlin compiler, but no accessible Gradle, Android SDK/ADB, vendored whisper.cpp source, or multilingual Whisper model. Native JNI and Android build cannot proceed here.
- New release source safety checks: 8/8 PASS (foreground service permissions, no configured external AI endpoints, no demo-caption TTS, native dependency gate, missing model detection, user-initiated transcript export, language tags, existing NOT RUN documentation). Static checks do not prove behavior or privacy of third-party SDKs.
- Stages 1–6 checks passed in earlier initial and integrated runs. Stage 7 repository 11 checks passed in an earlier invocation; a later full Stage 8 suite timed out after Stage 6 and is NOT considered a complete green end-to-end run. The initial Stage 8 safety test failed due to invalid test assumption that source languages use named ML Kit constants. Source actually validates `lv` and `lt` and calls `TranslateLanguage.fromLanguageTag`. Corrected test and reran: 8/8 PASS.
- Device checks D01–D16 are NOT RUN. No accuracy, word error rate, latency, runtime stability, online/offline, or translated-speech quality measured.

### Stage 8 bug ledger
- **S8-B01, fixed (test only):** release gate asserted presence of `TranslateLanguage.LATVIAN`/`LITHUANIAN` in translator source. Actual code converts validated BCP-47 tags via `fromLanguageTag`. Test assumption was too restrictive. Corrected test to check validated `lt`/`lv` mapping and tag conversion. Regression: 8/8 Stage 8 tests PASS. This is not evidence that ML Kit delivers good contextual translations.
- **S8-B02, open:** integration regression runner intermittently stalls or is terminated during Kotlin compilation for Stage 7 repository. Root cause not confirmed. Preserve test-output artifacts, rerun with Gradle or stable JVM runner and diagnose resource contention; do not report integration PASS based on a terminated run.
- Existing S4-B02, S5-B03, S5-B04, S6-B01/B03/B04/B06 remain open. They are release blockers, particularly JNI teardown races, missing true context-aware translation, and screen-scoped TTS.

### Next dependency-ordered handoff
1. Provision reproducible Android Gradle wrapper, SDK/NDK and pinned whisper.cpp checkout, verify model origin/license and install method. Run `assembleDebug`, unit tests and instrumentation tests; resolve compilation errors and record each fix.
2. Fix lifecycle races (JNI initialization/close, session broadcasts, screen background TTS) before any public trial. Repeat stop/start regression.
3. Run device acceptance D01–D16 with Latvian/Lithuanian reference recordings and bilingual review; report WER, translation accuracy and p50/p95 latency. Resolve meaningful errors.
4. Implement and validate rolling context/revisable English hypotheses. This is required for the original user promise and not accomplished by per-chunk ML Kit translation.
5. Only after release gates pass, produce and sign an APK and version the project. No release is approved now.

## 17. Android build candidate preparation (2026-10-28)

- Continued from Stage 8, no compiled APK existed. Preserved existing code, benchmarks and release checks.
- Changed Gradle build configuration to a compatible *candidate* toolchain: AGP 8.7.3, Kotlin Android/Compose plugin 2.0.21, Gradle 8.9, compile/target SDK 35, Java 17 and NDK 27.2.12479018. Added missing Kotlin Android plugin. Updated Compose BOM and AndroidX dependencies to established release versions. These versions are pinned but have NOT been downloaded or verified by Android compilation.
- Added a GitHub Actions workflow that provisions SDK/NDK/CMake and whisper.cpp v1.7.6, generates Gradle wrapper, invokes assembleDebug and uploads the debug APK only on success. No model or proprietary credentials bundled. CI workflow has NOT been executed.
- Added README-BUILD.md with Android Studio, CI instructions, model dependency and explicit limitations.
- **B8-B03 OPEN:** No Gradle/SDK/NDK or network in execution environment; build could not run. Regression: run CI assembleDebug and Android device instrumentation before closing.
- **B8-B04 OPEN:** whisper.cpp JNI API compatibility with v1.7.6 and CMake configuration unverified; resolve compilation results and pin commit SHA.
- **B8-B05 OPEN:** No Gradle wrapper JAR included; Android Studio local Gradle 8.9 or CI bootstrap must generate wrapper.
- No APK produced. Release verdict: BLOCKED.
