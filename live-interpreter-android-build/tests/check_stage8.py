"""Stage 8 release safety gates, intentionally standard-library only."""
import pathlib, re, unittest
R = pathlib.Path(__file__).resolve().parents[1]
APP = R / 'android/app'

def source(relative):
    return (APP / relative).read_text()

class ReleaseSafetyGates(unittest.TestCase):
    def test_foreground_mic_permissions(self):
        manifest = source('src/main/AndroidManifest.xml')
        for item in ('android.permission.RECORD_AUDIO', 'android.permission.FOREGROUND_SERVICE_MICROPHONE'):
            self.assertIn(item, manifest)
        self.assertIn('foregroundServiceType="microphone"', manifest)
    def test_no_external_audio_endpoint(self):
        text='\n'.join(f.read_text(errors='ignore') for f in (APP/'src/main').rglob('*.kt'))
        for secret in ('api.openai.com', 'generativelanguage.googleapis.com', 'Authorization: Bearer', 'https://api.'):
            self.assertNotIn(secret, text)
    def test_no_fictional_tts(self):
        screen=source('src/main/java/app/liveinterpreter/ui/InterpreterScreen.kt')
        self.assertIn('speaker.submit(', screen)
        self.assertIn('ACTION_TRANSLATION', screen)
    def test_native_dependency_gate(self):
        self.assertTrue((APP/'src/main/cpp/CMakeLists.txt').exists())
        self.assertFalse((APP/'vendor/whisper.cpp').exists(), 'Native vendor dependency now present: re-evaluate build readiness')
    def test_no_unverified_models_bundled(self):
        self.assertFalse(list(APP.rglob('ggml-*.bin')), 'Model bundled: validate license/provenance before release')
    def test_transcripts_not_automatically_exported(self):
        ui=source('src/main/java/app/liveinterpreter/ui/InterpreterScreen.kt')
        self.assertIn('Intent.ACTION_SEND', ui)
        self.assertIn('Intent.createChooser', ui)
    def test_offline_languages_explicit(self):
        tr=source('src/main/java/app/liveinterpreter/translation/OnDeviceTranslator.kt')
        self.assertTrue('TranslateLanguage.ENGLISH' in tr)
        self.assertIn('TranslateLanguage.fromLanguageTag(language)', tr)
        service=source('src/main/java/app/liveinterpreter/capture/MicrophoneService.kt')
        self.assertIn('== "lt"', service)
        self.assertIn('else "lv"', service)
    def test_sample_text_is_not_verified_translation(self):
        log=(R/'LIVE-INTERPRETER-DEVELOPMENT-LOG.md').read_text()
        self.assertIn('NOT RUN',log)

if __name__ == '__main__':
    unittest.main(verbosity=2)
