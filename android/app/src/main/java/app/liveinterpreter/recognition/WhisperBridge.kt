package app.liveinterpreter.recognition

/** Native whisper.cpp inference. Requires an installed multilingual GGML model and NDK build. */
class WhisperBridge(modelPath: String) : AutoCloseable {
    private var handle: Long
    init {
        System.loadLibrary("live_whisper")
        handle = initModel(modelPath)
        require(handle != 0L) { "Unable to load multilingual Whisper model" }
    }
    fun transcribe(samples: ShortArray, language: String): String {
        check(handle != 0L) { "Whisper is closed" }
        require(language == "lt" || language == "lv")
        return infer(handle, samples, language).trim()
    }
    override fun close() {
        if (handle != 0L) { release(handle); handle = 0L }
    }
    private external fun initModel(path: String): Long
    private external fun infer(handle: Long, pcm: ShortArray, language: String): String
    private external fun release(handle: Long)
}
