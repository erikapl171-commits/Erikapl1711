#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash tests/check_stage3.sh
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc android/app/src/main/java/app/liveinterpreter/recognition/StreamingWindow.kt tests/stage4_asr_smoke.kt -include-runtime -d "$TMP/asr.jar"
java -jar "$TMP/asr.jar"
python - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path('android/app')
ET.parse(root/'src/main/AndroidManifest.xml')
cpp=(root/'src/main/cpp/live_whisper.cpp').read_text()
ui=(root/'src/main/java/app/liveinterpreter/ui/InterpreterScreen.kt').read_text()
s=(root/'src/main/java/app/liveinterpreter/capture/MicrophoneService.kt').read_text()
assert 'whisper_full(' in cpp and 'whisper_full_get_segment_text' in cpp
assert 'ACTION_TRANSCRIPT' in s and 'ACTION_TRANSCRIPT' in ui
assert 'real_transcript' in ui
assert 'DemoNotice()' in ui
# Stage 5 intentionally adds INTERNET for one-time ML Kit model acquisition.
print('Stage 4 structural tests passed; Android build and runtime NOT verified')
PY
