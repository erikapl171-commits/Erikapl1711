import app.liveinterpreter.capture.SessionRepository

fun main() {
    val r = SessionRepository
    check(!r.snapshot().active)
    r.start(); check(r.snapshot().active)
    r.status("Capturing", 16000)
    check(r.snapshot().samples == 16000L)
    r.source("hello")
    r.source(" world")
    check(r.snapshot().source == "hello world")
    r.translation(1, "One")
    r.translation(1, "duplicate")
    r.translation(0, "old")
    check(r.snapshot().english == listOf("One"))
    r.translation(2, "Two")
    check(r.snapshot().sequence == 2L)
    r.stop(); check(!r.snapshot().active)
    check(r.snapshot().english.size == 2)
    r.start(); check(r.snapshot().english.isEmpty() && r.snapshot().source.isEmpty())
    println("Stage 7 repository: 11 assertions PASS")
}
