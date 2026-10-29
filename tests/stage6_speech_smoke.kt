import app.liveinterpreter.speech.SpokenQueue
fun main() {
 val q=SpokenQueue(2)
 check(!q.enqueue("  "))
 check(q.enqueue(" first "))
 check(q.enqueue("second"))
 check(q.count()==2)
 check(q.enqueue("third"))
 check(q.dropped==1L)
 check(q.take()=="second")
 check(q.take()=="third")
 check(q.take()==null)
 q.enqueue("new");q.clear();check(q.count()==0)
 check(runCatching { SpokenQueue(0) }.isFailure)
 println("Stage 6 queue: 10 checks passed")
}
