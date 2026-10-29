import app.liveinterpreter.core.*

fun main() {
    var s = InterpreterUiState()
    check(s.phase == DemoPhase.READY && !s.hasAnyCaptions && !s.audioEnabled)
    check(InterpreterStateMachine.showDraft(s, "should not appear") == s)
    check(InterpreterStateMachine.commit(s, "should not appear") == s)

    s = InterpreterStateMachine.start(s)
    check(s.phase == DemoPhase.RUNNING && s.committed.isEmpty())
    s = InterpreterStateMachine.showDraft(s, "  waiting for context  ")
    check(s.provisional == "waiting for context" && s.hasAnyCaptions)
    s = InterpreterStateMachine.showDraft(s, "revised words")
    check(s.provisional == "revised words")
    s = InterpreterStateMachine.commit(s, "  completed sentence ")
    check(s.provisional == null && s.committed.single().english == "completed sentence")
    check(s.committed.single().id == 1L)
    s = InterpreterStateMachine.commit(s, "second sentence")
    check(s.committed.size == 2 && s.committed.last().id == 2L)
    check(InterpreterStateMachine.commit(s, "  ") == s)
    s = InterpreterStateMachine.complete(s)
    check(s.phase == DemoPhase.COMPLETE)
    check(InterpreterStateMachine.showDraft(s, "too late") == s)
    s = InterpreterStateMachine.stop(s)
    check(s.phase == DemoPhase.READY && s.committed.size == 2)
    s = s.copy(language = SourceLanguage.LITHUANIAN, microphonePermission = MicPermission.DENIED, audioEnabled = true)
    s = InterpreterStateMachine.start(s)
    check(s.language == SourceLanguage.LITHUANIAN && s.microphonePermission == MicPermission.DENIED)
    check(s.audioEnabled && s.committed.isEmpty() && s.nextCaptionId == 1L)

    println("PASS: 15 Stage 2 state-machine assertions")
}
