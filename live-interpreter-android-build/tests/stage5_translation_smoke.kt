import app.liveinterpreter.translation.TranslationQueue

fun main() {
 val q=TranslationQueue(2)
 val s=q.start()
 check(q.enqueue(" ")==null)
 val a=q.enqueue("  Sveiki ")!!
 val b=q.enqueue("Labdien")!!
 check(a.source=="Sveiki" && a.sequence==1L)
 check(q.enqueue("overflow")==null && q.dropped==1L)
 check(q.take()==a)
 check(q.take()==null)
 check(!q.complete(b))
 check(q.complete(a))
 check(q.take()==b && q.complete(b))
 check(!q.hasPending())
 q.enqueue("stale")
 q.take()
 check(q.start()!=s)
 check(!q.complete(a))
 check(q.take()==null && q.dropped==0L)
 println("Stage 5 translation queue: 12 assertions PASS")
}
