package app.liveinterpreter.core

import org.junit.Assert.*
import org.junit.Test

class InterpreterStateMachineTest {
    @Test fun startsWithSafeDefaults() {
        val state = InterpreterUiState()
        assertEquals(DemoPhase.READY, state.phase)
        assertFalse(state.audioEnabled)
        assertTrue(state.committed.isEmpty())
        assertEquals(MicPermission.NOT_CHECKED, state.microphonePermission)
    }

    @Test fun doesNotShowCaptionsWhenStopped() {
        val state = InterpreterUiState()
        assertEquals(state, InterpreterStateMachine.showDraft(state, "wrong"))
        assertEquals(state, InterpreterStateMachine.commit(state, "wrong"))
    }

    @Test fun draftsAreReplaceableButFinalsAreImmutable() {
        var state = InterpreterStateMachine.start(InterpreterUiState())
        state = InterpreterStateMachine.showDraft(state, "partial")
        state = InterpreterStateMachine.showDraft(state, "revised partial")
        assertEquals("revised partial", state.provisional)
        state = InterpreterStateMachine.commit(state, "final sentence")
        assertNull(state.provisional)
        assertEquals(listOf(Caption(1, "final sentence")), state.committed)
        state = InterpreterStateMachine.showDraft(state, "next provisional")
        assertEquals("final sentence", state.committed.first().english)
    }

    @Test fun ignoresBlankTranscriptsAndLateResults() {
        var state = InterpreterStateMachine.start(InterpreterUiState())
        state = InterpreterStateMachine.commit(state, "  ")
        assertTrue(state.committed.isEmpty())
        state = InterpreterStateMachine.complete(state)
        assertEquals(state, InterpreterStateMachine.showDraft(state, "late"))
        assertEquals(state, InterpreterStateMachine.commit(state, "late"))
    }

    @Test fun restartClearsOldCaptionHistoryButKeepsUserChoices() {
        var state = InterpreterStateMachine.start(InterpreterUiState(
            language = SourceLanguage.LITHUANIAN,
            subtitleSize = SubtitleSize.EXTRA_LARGE,
            audioEnabled = true,
            microphonePermission = MicPermission.DENIED
        ))
        state = InterpreterStateMachine.commit(state, "first")
        state = InterpreterStateMachine.stop(state)
        state = InterpreterStateMachine.start(state)
        assertTrue(state.committed.isEmpty())
        assertEquals(1L, state.nextCaptionId)
        assertEquals(SourceLanguage.LITHUANIAN, state.language)
        assertEquals(SubtitleSize.EXTRA_LARGE, state.subtitleSize)
        assertTrue(state.audioEnabled)
        assertEquals(MicPermission.DENIED, state.microphonePermission)
    }
}
