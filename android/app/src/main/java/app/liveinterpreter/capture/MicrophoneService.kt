package app.liveinterpreter.capture

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.liveinterpreter.MainActivity
import app.liveinterpreter.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Executors
import app.liveinterpreter.recognition.WhisperBridge
import app.liveinterpreter.recognition.StreamingWindow
import app.liveinterpreter.recognition.TranscriptOverlap
import java.io.File
import app.liveinterpreter.translation.OnDeviceTranslator
import app.liveinterpreter.translation.TranslationQueue
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread

/** Foreground-only start, continuous PCM acquisition; ASR is a later stage. */
class MicrophoneService : Service() {
    companion object {
        const val ACTION_START = "app.liveinterpreter.capture.START"
        const val ACTION_STOP = "app.liveinterpreter.capture.STOP"
        const val ACTION_STATUS = "app.liveinterpreter.capture.STATUS"
        const val EXTRA_STATUS = "status"
        const val EXTRA_SAMPLES = "samples"
        const val ACTION_TRANSCRIPT = "app.liveinterpreter.capture.TRANSCRIPT"
        const val EXTRA_TRANSCRIPT = "text"
        const val EXTRA_LANGUAGE = "language"
        const val ACTION_TRANSLATION = "app.liveinterpreter.capture.TRANSLATION"
        const val EXTRA_ENGLISH = "english"
        const val EXTRA_SEQUENCE = "sequence"
        const val ACTION_TRANSLATION_STATE = "app.liveinterpreter.capture.TRANSLATION_STATE"
        private const val CHANNEL = "microphone_capture"
        private const val NOTIFICATION_ID = 101
        private const val RATE = 16000
    }
    private val running = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val translateQueue = TranslationQueue()
    private var translator: OnDeviceTranslator? = null
    private var translatorReady = false
    private var pipelineSession = 0L
    private val buffer = AudioRingBuffer(RATE * 30)
    private var worker: Thread? = null
    private val inference = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    @Volatile private var bridge: WhisperBridge? = null
    @Volatile private var asrState = "ASR not initialized"
    private var lastTranscript = ""
    private val scheduler = StreamingWindow()
    private var language = "lv"
    @Volatile private var recorder: AudioRecord? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Microphone capture", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { halt(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> { language = if (intent.getStringExtra(EXTRA_LANGUAGE) == "lt") "lt" else "lv"; begin() }
        }
        return START_NOT_STICKY // never silently auto-restart recording
    }

    private fun notifyStatus(status: String, samples: Long = 0L) {
        SessionRepository.status(status, samples)
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName)
            .putExtra(EXTRA_STATUS, status).putExtra(EXTRA_SAMPLES, samples))
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, MicrophoneService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Live Interpreter microphone active")
            .setContentText("On-device original-language transcription, when model is installed.")
            .setContentIntent(open).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop microphone", stop)
            .build()
    }

    private fun begin() {
        if (running.get()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) { notifyStatus("Permission denied"); stopSelf(); return }
        try {
            if (Build.VERSION.SDK_INT >= 29)
                startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIFICATION_ID, notification())
        } catch (e: RuntimeException) {
            notifyStatus("Unable to start microphone service: ${e.javaClass.simpleName}")
            stopSelf(); return
        }
        running.set(true)
        SessionRepository.start()
        pipelineSession = translateQueue.start()
        translatorReady = false
        try {
            translator = OnDeviceTranslator(this, language)
            val session = pipelineSession
            notifyTranslationState("Preparing English translation model (Wi-Fi required for first download)")
            translator?.prepare(onReady = {
                if (running.get() && translateQueue.session == session) {
                    translatorReady = true
                    notifyTranslationState("English translation ready")
                    drainTranslations()
                }
            }, onError = { error -> if (running.get() && translateQueue.session == session) notifyTranslationState(error) })
        } catch (e: Exception) { notifyTranslationState("Translation unavailable: ${e.message}") }
        buffer.clear()
        scheduler.reset()
        lastTranscript = ""
        val model = File(filesDir, "models/ggml-base.bin")
        if (!model.isFile) {
            asrState = "ASR model missing"
            notifyStatus(asrState)
        } else inference.execute {
            asrState = "Initializing Whisper"
            try {
                val instance = WhisperBridge(model.absolutePath)
                if (running.get()) {
                    bridge = instance
                    asrState = "Whisper ready"
                } else {
                    instance.close()
                }
            } catch (e: Exception) {
                asrState = "ASR unavailable: ${e.message ?: e.javaClass.simpleName}"
            } catch (e: UnsatisfiedLinkError) {
                asrState = "ASR native library unavailable"
            }
            if (running.get()) notifyStatus(asrState)
        }
        worker = thread(name = "mic-capture", isDaemon = true) { captureLoop() }
    }

    private fun captureLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        var record: AudioRecord? = null
        try {
            val minimum = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0) { "16 kHz recording unsupported" }
            val capacity = maxOf(minimum * 2, RATE / 2 * 2)
            record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, capacity)
            check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }
            recorder = record
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Could not record" }
            notifyStatus("Capturing microphone; $asrState")
            val chunk = ShortArray(RATE / 10)
            var total = 0L
            var lastReport = 0L
            var consecutiveErrors = 0
            while (running.get()) {
                val count = record.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
                if (count > 0) {
                    consecutiveErrors = 0
                    buffer.write(chunk, count)
                    total += count
                    if (scheduler.accept(count) && bridge != null && busy.compareAndSet(false, true)) {
                        val snapshot = buffer.snapshot().takeLast(16000 * 5).toShortArray()
                        val currentLanguage = language
                        try { inference.execute {
                            try {
                                if (running.get()) {
                                    val result = bridge?.transcribe(snapshot, currentLanguage).orEmpty()
                                    val tail = TranscriptOverlap.newTail(lastTranscript, result)
                                    if (tail.isNotBlank() && running.get()) {
                                        lastTranscript = result
                                        mainHandler.post { if (running.get()) {
                                            val queued = translateQueue.enqueue(tail)
                                            if (queued == null) notifyTranslationState("Translation backlog full; some speech was skipped")
                                            else {
                                                notifyTranslationState("Translating speech…")
                                                drainTranslations()
                                            }
                                        } }
                                        SessionRepository.source(tail)
                                        sendBroadcast(Intent(ACTION_TRANSCRIPT).setPackage(packageName)
                                            .putExtra(EXTRA_TRANSCRIPT, tail))
                                    }
                                }
                            } catch (e: Exception) {
                                asrState = "ASR error: ${e.message ?: e.javaClass.simpleName}"
                                notifyStatus(asrState)
                            } finally { snapshot.fill(0); busy.set(false) }
                        } } catch (_: RejectedExecutionException) {
                            snapshot.fill(0)
                            busy.set(false)
                        }
                    }
                    if (total - lastReport >= RATE) {
                        notifyStatus("Capturing microphone; $asrState", total)
                        lastReport = total
                    }
                } else {
                    consecutiveErrors++
                    if (consecutiveErrors >= 3) error("AudioRecord read failure: $count")
                }
            }
        } catch (e: Exception) {
            if (running.get()) notifyStatus("Microphone stopped: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            try { record?.stop() } catch (_: IllegalStateException) { }
            record?.release()
            recorder = null
            running.set(false)
            buffer.clear()
            SessionRepository.stop()
            stopSelf()
        }
    }

    private fun notifyTranslationState(message: String) {
        sendBroadcast(Intent(ACTION_TRANSLATION_STATE).setPackage(packageName)
            .putExtra(EXTRA_STATUS, message))
    }

    /** Called on main thread, ML Kit callbacks are delivered in sequence. */
    private fun drainTranslations() {
        if (!running.get() || !translatorReady) return
        val part = translateQueue.take() ?: return
        translator?.translate(part.source, onResult = { english ->
            if (running.get() && translateQueue.complete(part)) {
                if (english.isNotBlank()) {
                    SessionRepository.translation(part.sequence, english.trim())
                    sendBroadcast(Intent(ACTION_TRANSLATION).setPackage(packageName)
                    .putExtra(EXTRA_ENGLISH, english.trim())
                    .putExtra(EXTRA_SEQUENCE, part.sequence))
                }
                drainTranslations()
            }
        }, onError = { message ->
            if (translateQueue.complete(part)) {
                notifyTranslationState(message)
                drainTranslations()
            }
        })
    }

    private fun halt() {
        running.set(false)
        translateQueue.start()
        translatorReady = false
        // Stop unblocks a blocking read. The worker owns release to avoid double release.
        try { recorder?.stop() } catch (_: IllegalStateException) { }
        worker?.interrupt()
        buffer.clear()
        SessionRepository.stop()
        notifyStatus("Stopped")
    }

    override fun onDestroy() {
        halt()
        // All Whisper JNI calls, including close(), must execute on the SAME serial
        // worker. Never close a native context while transcribe() may be using it.
        try {
            inference.execute {
                try { bridge?.close() } catch (_: Exception) { }
                finally { bridge = null }
            }
        } catch (_: RejectedExecutionException) { /* already shutting down */ }
        inference.shutdown() // drain in-flight recognition and queued native close
        translator?.close()
        translator = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
