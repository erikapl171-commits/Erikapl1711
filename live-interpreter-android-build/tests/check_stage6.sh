#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m unittest discover -s tests -p 'test_*.py' -q
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
kotlinc android/app/src/main/java/app/liveinterpreter/core/InterpreterState.kt tests/stage2_state_smoke.kt -include-runtime -d "$TMP/state.jar"; java -jar "$TMP/state.jar"
kotlinc android/app/src/main/java/app/liveinterpreter/capture/AudioRingBuffer.kt tests/stage3_buffer_smoke.kt -include-runtime -d "$TMP/buffer.jar"; java -jar "$TMP/buffer.jar"
kotlinc android/app/src/main/java/app/liveinterpreter/recognition/StreamingWindow.kt tests/stage4_asr_smoke.kt -include-runtime -d "$TMP/asr.jar"; java -jar "$TMP/asr.jar"
kotlinc android/app/src/main/java/app/liveinterpreter/translation/TranslationQueue.kt tests/stage5_translation_smoke.kt -include-runtime -d "$TMP/trans.jar"; java -jar "$TMP/trans.jar"
kotlinc android/app/src/main/java/app/liveinterpreter/speech/SpokenQueue.kt tests/stage6_speech_smoke.kt -include-runtime -d "$TMP/speech.jar"; java -jar "$TMP/speech.jar"
python3 - <<'PY2'
from pathlib import Path
from xml.etree import ElementTree
p=Path('android/app');root=p/'src/main/java/app/liveinterpreter'
for x in p.rglob('*.xml'): ElementTree.parse(x)
ui=(root/'ui/InterpreterScreen.kt').read_text()
tts=(root/'speech/EnglishSpeaker.kt').read_text()
assert all(k in ui for k in ('speaker.submit(english)','speaker.reset()','playbackSpeed','chosenVoice','Spoken English'))
assert all(k in tts for k in ('TextToSpeech','!it.isNetworkConnectionRequired','setSpeechRate','setVoice','QUEUE_FLUSH','engine?.shutdown()'))
assert 'android.permission.INTERNET' in (p/'src/main/AndroidManifest.xml').read_text()
assert 'AudioRecord(' in (root/'capture/MicrophoneService.kt').read_text()
print('Stage 6 source structural checks passed; Android compile/device NOT verified')
PY2
