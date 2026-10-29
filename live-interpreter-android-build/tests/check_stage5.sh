#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash tests/check_stage4.sh
DIR=$(mktemp -d); trap 'rm -rf "$DIR"' EXIT
kotlinc android/app/src/main/java/app/liveinterpreter/translation/TranslationQueue.kt tests/stage5_translation_smoke.kt -include-runtime -d "$DIR/t.jar"
java -jar "$DIR/t.jar"
python - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
r=Path('android/app')
root=ET.parse(r/'src/main/AndroidManifest.xml').getroot()
assert 'android.permission.INTERNET' in {x.attrib.get('{http://schemas.android.com/apk/res/android}name') for x in root.findall('uses-permission')}
script=(r/'src/main/java/app/liveinterpreter/capture/MicrophoneService.kt').read_text()
ui=(r/'src/main/java/app/liveinterpreter/ui/InterpreterScreen.kt').read_text()
assert 'drainTranslations()' in script and 'translateQueue.start()' in script
assert 'ACTION_TRANSLATION' in script and 'english_translation_panel' in ui
assert 'com.google.mlkit:translate:17.0.3' in (r/'build.gradle.kts').read_text()
print('Stage 5 structural checks PASS')
PY
