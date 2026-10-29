import app.liveinterpreter.capture.AudioRingBuffer

fun main() {
    val b = AudioRingBuffer(4)
    check(b.snapshot().isEmpty())
    b.write(shortArrayOf(1, 2), 2)
    check(b.snapshot().contentEquals(shortArrayOf(1, 2)))
    b.write(shortArrayOf(3, 4, 5), 3)
    check(b.snapshot().contentEquals(shortArrayOf(2, 3, 4, 5)))
    b.write(shortArrayOf(6, 7, 8, 9, 10), 5)
    check(b.snapshot().contentEquals(shortArrayOf(7, 8, 9, 10)))
    check(b.size() == 4)
    val snapshot = b.snapshot()
    snapshot[0] = 99
    check(b.snapshot()[0] == 7.toShort())
    b.clear()
    check(b.snapshot().isEmpty())
    check(runCatching { b.write(shortArrayOf(1), 2) }.isFailure)
    val minimum = AudioRingBuffer(0)
    minimum.write(shortArrayOf(12, 13), 2)
    check(minimum.snapshot().contentEquals(shortArrayOf(13)))
    println("Stage 3 ring buffer smoke: 9 checks passed")
}
