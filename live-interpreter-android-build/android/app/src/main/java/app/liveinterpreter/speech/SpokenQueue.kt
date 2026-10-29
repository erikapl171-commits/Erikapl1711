package app.liveinterpreter.speech

import java.util.ArrayDeque

/** Bounded audio queue: old unspoken captions are dropped to keep up with the stage. */
class SpokenQueue(private val maxPending: Int = 3) {
    init { require(maxPending > 0) }
    private val pending = ArrayDeque<String>()
    var dropped: Long = 0
        private set
    fun enqueue(text: String): Boolean {
        val clean = text.trim()
        if (clean.isBlank()) return false
        while (pending.size >= maxPending) { pending.removeFirst(); dropped++ }
        pending.addLast(clean)
        return true
    }
    fun take(): String? = pending.pollFirst()
    fun clear() { pending.clear() }
    fun count(): Int = pending.size
}
