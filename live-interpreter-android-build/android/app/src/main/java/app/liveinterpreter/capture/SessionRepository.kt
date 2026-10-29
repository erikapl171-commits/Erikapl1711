package app.liveinterpreter.capture

/** In-memory session status, survives Activity recreation while the app process remains alive. */
object SessionRepository {
    data class Snapshot(val active: Boolean, val status: String, val samples: Long,
                        val source: String, val english: List<String>, val sequence: Long)
    private var value = Snapshot(false, "Stopped", 0L, "", emptyList(), 0L)
    @Synchronized fun snapshot(): Snapshot = value.copy(english = value.english.toList())
    @Synchronized fun start() { value = Snapshot(true, "Starting microphone", 0L, "", emptyList(), 0L) }
    @Synchronized fun status(message: String, samples: Long = value.samples) {
        value = value.copy(status = message, samples = samples)
    }
    @Synchronized fun source(text: String) {
        if (text.isNotBlank()) value = value.copy(source = (value.source + " " + text.trim()).trim().takeLast(20000))
    }
    @Synchronized fun translation(sequence: Long, english: String) {
        if (sequence > value.sequence && english.isNotBlank())
            value = value.copy(sequence = sequence, english = (value.english + english).takeLast(200))
    }
    @Synchronized fun stop() { value = value.copy(active = false, status = "Stopped") }
}
