# Live Interpreter, Stage 8 release evaluation

Stage 8 is a **release-readiness evaluation**, not a released app. It retains all Stage 7 source and adds release checks and a physical-phone test plan.

Run release source checks: `python3 -m unittest discover -s tests -p check_stage8.py -v`.
Run combined suite (requires Python, Kotlin, Java): `bash tests/check_stage8.sh`. The combined suite may stall in this environment; consult `release/STAGE8-TEST-RESULTS.txt` and the development log.

**Release BLOCKED:** Android SDK, Gradle, NDK native whisper.cpp checkout, model, actual Android compilation and device quality testing still missing. No APK was generated. See `release/DEVICE-ACCEPTANCE-CHECKLIST.md` and `LIVE-INTERPRETER-DEVELOPMENT-LOG.md`.
