package app.liveinterpreter.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/** On-device TTS output only. Does not synthesize fictional sample captions. */
class EnglishSpeaker(context: Context, private val onStatus: (String) -> Unit) : AutoCloseable {
    private val queue = SpokenQueue()
    private var engine: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private var enabled = false
    private var speaking = false
    private var serial = 0L
    private var activeId: String? = null
    private var epoch = 0L
    private var speed = 1.0f

    init {
        engine = TextToSpeech(context.applicationContext) { result ->
            mainHandler.post {
            val tts = engine
            if (result != TextToSpeech.SUCCESS || tts == null) {
                onStatus("English audio engine unavailable")
            } else {
                val localeResult = tts.setLanguage(Locale.US)
                if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onStatus("English TTS voice missing. Install an English voice in Android settings.")
                } else {
                    tts.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    val localVoice = tts.voices.orEmpty().firstOrNull {
                        it.locale.language == "en" && !it.isNetworkConnectionRequired
                    }
                    if (localVoice == null) {
                        onStatus("No offline English voice installed")
                        return@post
                    }
                    tts.voice = localVoice
                    tts.setSpeechRate(speed)
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) { }
                        override fun onDone(id: String?) { finish(id) }
                        override fun onError(id: String?) { finish(id) }
                    })
                    ready = true
                    onStatus("English voice ready")
                    pump()
                }
            }
            }
        }
    }

    @Synchronized fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) { epoch++; queue.clear(); speaking = false; activeId = null; engine?.stop() }
        else pump()
    }
    @Synchronized fun setSpeed(value: Float) {
        speed = value.coerceIn(0.75f, 1.5f)
        engine?.setSpeechRate(speed)
    }
    @Synchronized fun availableVoices(): List<Voice> = engine?.voices.orEmpty()
        .filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
        .sortedBy { it.name }
    @Synchronized fun chooseVoice(name: String): Boolean {
        val voice = availableVoices().firstOrNull { it.name == name } ?: return false
        return engine?.setVoice(voice) == TextToSpeech.SUCCESS
    }
    @Synchronized fun submit(english: String) {
        if (!enabled) return
        if (queue.enqueue(english)) pump()
    }
    @Synchronized fun reset() {
        epoch++; queue.clear(); speaking = false; activeId = null; engine?.stop()
    }
    @Synchronized private fun pump() {
        if (!ready || !enabled || speaking) return
        val next = queue.take() ?: return
        speaking = true
        val id = "$epoch:${++serial}"
        activeId = id
        val result = engine?.speak(next, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
        if (result != TextToSpeech.SUCCESS) { speaking = false; onStatus("Audio playback failed") }
    }
    private fun finish(id: String?) {
        synchronized(this) {
            if (id == null || id != activeId || id.substringBefore(':') != epoch.toString()) return
            mainHandler.post { synchronized(this) { if (id == activeId) { activeId = null; speaking = false; pump() } } }
        }
    }
    @Synchronized override fun close() {
        enabled = false; reset(); ready = false; engine?.shutdown(); engine = null
    }
}
