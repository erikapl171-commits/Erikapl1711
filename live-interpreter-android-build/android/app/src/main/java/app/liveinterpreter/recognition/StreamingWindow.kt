package app.liveinterpreter.recognition

/** Pure Kotlin rolling-window scheduler; caller serializes execution. */
class StreamingWindow(private val rate: Int = 16000,
                      private val windowSeconds: Int = 5,
                      private val strideSeconds: Int = 4) {
    init { require(rate > 0 && windowSeconds >= strideSeconds && strideSeconds > 0) }
    private val stride = rate * strideSeconds
    private val window = rate * windowSeconds
    private var total = 0L
    private var nextBoundary = window.toLong()
    /** Emit first at window duration, then every stride; never double emit. */
    fun accept(count: Int): Boolean {
        require(count >= 0)
        total += count
        if (total < nextBoundary) return false
        nextBoundary += stride
        while (nextBoundary <= total) nextBoundary += stride
        return true
    }
    fun reset() { total = 0; nextBoundary = window.toLong() }
}

/** Prevents the same repeated prefix from being emitted on neighboring overlapping windows. */
object TranscriptOverlap {
    fun newTail(previous: String, current: String): String {
        val old = previous.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val now = current.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val limit = minOf(old.size, now.size, 16)
        val overlap = (limit downTo 1).firstOrNull { n ->
            old.takeLast(n).map(::norm) == now.take(n).map(::norm)
        } ?: 0
        return now.drop(overlap).joinToString(" ")
    }
    private fun norm(word: String) = word.lowercase().trim('.', ',', '!', '?', ':', ';')
}
