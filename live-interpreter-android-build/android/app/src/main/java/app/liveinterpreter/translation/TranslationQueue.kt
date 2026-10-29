package app.liveinterpreter.translation

import java.util.ArrayDeque

/** Pure state and scheduling: source fragments arrive in chronological order.
 * Only the head of the queue is translated, never reorder asynchronous responses.
 * Every new session invalidates previous results.
 */
class TranslationQueue(private val capacity: Int = 12) {
    data class Fragment(val session: Long, val sequence: Long, val source: String)
    private val pending = ArrayDeque<Fragment>()
    private var nextSequence = 0L
    var session: Long = 0L
        private set
    var active: Fragment? = null
        private set
    var dropped: Long = 0
        private set

    fun start(): Long {
        session++
        pending.clear()
        active = null
        nextSequence = 0
        dropped = 0
        return session
    }

    fun enqueue(source: String): Fragment? {
        if (source.isBlank()) return null
        val fragment = Fragment(session, ++nextSequence, source.trim())
        if (pending.size >= capacity) { dropped++; return null }
        pending.addLast(fragment)
        return fragment
    }

    fun take(): Fragment? {
        if (active != null) return null
        active = pending.pollFirst()
        return active
    }

    fun complete(fragment: Fragment): Boolean {
        if (fragment.session != session || active != fragment) return false
        active = null
        return true
    }

    fun hasPending(): Boolean = pending.isNotEmpty()
}
