package app.liveinterpreter.capture

/** Bounded, thread-safe, volatile PCM window; never persists recordings. */
class AudioRingBuffer(capacitySamples: Int) {
    private val samples = ShortArray(capacitySamples.coerceAtLeast(1))
    private var position = 0
    private var used = 0
    @Synchronized fun write(input: ShortArray, length: Int) {
        require(length in 0..input.size)
        for (i in 0 until length) {
            samples[position] = input[i]
            position = (position + 1) % samples.size
            used = (used + 1).coerceAtMost(samples.size)
        }
    }
    @Synchronized fun snapshot(): ShortArray {
        val result = ShortArray(used)
        val start = (position - used + samples.size) % samples.size
        for (i in 0 until used) result[i] = samples[(start + i) % samples.size]
        return result
    }
    @Synchronized fun clear() { samples.fill(0); position = 0; used = 0 }
    @Synchronized fun size(): Int = used
}
