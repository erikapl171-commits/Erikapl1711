#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash tests/check_stage6.sh
TEMP_DIR=$(mktemp -d); trap 'rm -rf "$TEMP_DIR"' EXIT
kotlinc android/app/src/main/java/app/liveinterpreter/capture/SessionRepository.kt tests/stage7_session_smoke.kt -include-runtime -d "$TEMP_DIR/stage7.jar"
java -jar "$TEMP_DIR/stage7.jar"
python3 - <<'PY'
from pathlib import Path
ui=Path('android/app/src/main/java/app/liveinterpreter/ui/InterpreterScreen.kt').read_text()
service=Path('android/app/src/main/java/app/liveinterpreter/capture/MicrophoneService.kt').read_text()
assert 'Intent.ACTION_SEND' in ui and 'Intent.createChooser' in ui
assert 'enabled = translatedSegments.isNotEmpty()' in ui
assert 'SessionRepository.snapshot()' in ui
assert 'SessionRepository.start()' in service and 'SessionRepository.stop()' in service
assert 'SessionRepository.translation(part.sequence' in service
assert 'SessionRepository.source(tail)' in service
assert 'File(context.filesDir, "models/ggml-base.bin").isFile' in ui
print('Stage 7 structural checks PASS; APK/device NOT verified')
PY
