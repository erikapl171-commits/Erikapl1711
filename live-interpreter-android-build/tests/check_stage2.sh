#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_JAR="$(mktemp "${TMPDIR:-/tmp}/interpreter-stage2-XXXXXX.jar")"
trap 'rm -f "$TMP_JAR"' EXIT

python3 -m unittest discover -s "$ROOT/tests" -p 'test_*.py' -v
kotlinc \
  "$ROOT/android/app/src/main/java/app/liveinterpreter/core/InterpreterState.kt" \
  "$ROOT/tests/stage2_state_smoke.kt" \
  -include-runtime -d "$TMP_JAR"
java -jar "$TMP_JAR"
python3 - "$ROOT/android" <<'PY'
import sys
from pathlib import Path
from xml.etree import ElementTree as ET
base = Path(sys.argv[1])
for p in base.rglob('*.xml'):
    ET.parse(p)
manifest = (base / 'app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.RECORD_AUDIO' in manifest
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.POST_NOTIFICATIONS' not in manifest
assert '<service' not in manifest
app_sources = '\n'.join(p.read_text() for p in (base / 'app/src/main/java').rglob('*.kt'))
for forbidden in ('AudioRecord(', 'SpeechRecognizer(', 'TextToSpeech(', 'TranslatorOptions', 'http://', 'https://'):
    assert forbidden not in app_sources, f'Stage 2 unexpectedly includes: {forbidden}'
for expected in ('Stage 2 interface demonstration', 'rememberLauncherForActivityResult', 'DemoPhase.RUNNING', 'MicPermission'):
    assert expected in app_sources, f'Missing UI control or disclosure: {expected}'
print('PASS: XML well-formed; permissions bounded; no microphone capture or paid API; demo clearly identified')
PY
