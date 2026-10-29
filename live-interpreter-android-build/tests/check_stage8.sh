#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash tests/check_stage7.sh
python3 -m unittest discover -s tests -p 'check_stage8.py' -v
python3 - <<'PY'
from pathlib import Path
import shutil
sdk = bool(shutil.which('sdkmanager') or (Path.home()/'Android/Sdk/platforms').exists())
gradle = bool(shutil.which('gradle') or Path('android/gradlew').exists())
adb = bool(shutil.which('adb'))
whisper = Path('android/app/vendor/whisper.cpp').exists()
model = bool(list(Path('android/app').rglob('ggml-base.bin')))
print(f'Release build prerequisites: sdk={sdk}, gradle={gradle}, adb={adb}, native_whisper={whisper}, model={model}')
print('RELEASE GATE: BLOCKED. No verified APK; hardware and audio quality benchmarks still required.')
PY
