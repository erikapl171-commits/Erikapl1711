import app.liveinterpreter.recognition.StreamingWindow
import app.liveinterpreter.recognition.TranscriptOverlap
fun main() {
    val s = StreamingWindow(rate=10, windowSeconds=5, strideSeconds=4)
    check(!s.accept(39)); check(!s.accept(10)); check(s.accept(1))
    check(!s.accept(39)); check(s.accept(1))
    s.reset(); check(!s.accept(40)); check(s.accept(10))
    check(TranscriptOverlap.newTail("Sveiki visi", "visi šodien") == "šodien")
    check(TranscriptOverlap.newTail("Labas rytas.", "rytas, visi") == "visi")
    check(TranscriptOverlap.newTail("", "Labas rytas") == "Labas rytas")
    check(TranscriptOverlap.newTail("Labas", "") == "")
    println("Stage 4 scheduler / overlap assertions passed (9)")
}
