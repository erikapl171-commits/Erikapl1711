#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 -m unittest discover -s "$ROOT/tests" -p 'test_*.py' -v
JAR="$(mktemp /tmp/live-interpreter-stage3-XXXX.jar)"
trap 'rm -f "$JAR"' EXIT
kotlinc "$ROOT/android/app/src/main/java/app/liveinterpreter/core/InterpreterState.kt" "$ROOT/tests/stage2_state_smoke.kt" -include-runtime -d "$JAR"
java -jar "$JAR"
kotlinc "$ROOT/android/app/src/main/java/app/liveinterpreter/capture/AudioRingBuffer.kt" "$ROOT/tests/stage3_buffer_smoke.kt" -include-runtime -d "$JAR"
java -jar "$JAR"
python3 - "$ROOT/android" <<'PY'
import sys
from pathlib import Path
from xml.etree import ElementTree
root=Path(sys.argv[1]); manifest=root/'app/src/main/AndroidManifest.xml'
for xml in root.rglob('*.xml'): ElementTree.parse(xml)
m=manifest.read_text()
for s in ('android.permission.RECORD_AUDIO','android.permission.FOREGROUND_SERVICE','android.permission.FOREGROUND_SERVICE_MICROPHONE','android:foregroundServiceType="microphone"','android:exported="false"'):
 assert s in m,s
# Stage 5 adds Internet strictly for initial on-device model download
service=(root/'app/src/main/java/app/liveinterpreter/capture/MicrophoneService.kt').read_text()
for s in ('AudioRecord(', 'START_NOT_STICKY','READ_BLOCKING','stopSelf()', 'buffer.clear()', 'FOREGROUND_SERVICE_TYPE_MICROPHONE','setPackage(packageName)'):
 assert s in service,s
ui=(root/'app/src/main/java/app/liveinterpreter/ui/InterpreterScreen.kt').read_text()
assert 'english_translation_panel' in ui
assert 'microphone_toggle' in ui
assert 'demo_toggle' in ui
print('PASS: manifest, no Internet permission, service lifecycle safeguards, separate demo and microphone UI')
PY
