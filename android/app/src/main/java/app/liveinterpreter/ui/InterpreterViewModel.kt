package app.liveinterpreter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.liveinterpreter.core.DemoPhase
import app.liveinterpreter.core.InterpreterStateMachine
import app.liveinterpreter.core.InterpreterUiState
import app.liveinterpreter.core.MicPermission
import app.liveinterpreter.core.SourceLanguage
import app.liveinterpreter.core.SubtitleSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * STAGE 2 ONLY. Uses prewritten English sample captions, not microphone audio,
 * transcription, translation, or speech synthesis. Keep that explicit in the UI.
 */
class InterpreterViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(InterpreterUiState())
    val state = mutableState.asStateFlow()
    private var demoJob: Job? = null

    fun setLanguage(value: SourceLanguage) = mutableState.update { it.copy(language = value) }
    fun setMicPermission(value: MicPermission) = mutableState.update {
        it.copy(microphonePermission = value)
    }
    fun setAudioEnabled(value: Boolean) = mutableState.update { it.copy(audioEnabled = value) }
    fun setKeepScreenOn(value: Boolean) = mutableState.update { it.copy(keepScreenOn = value) }
    fun setSubtitleSize(value: SubtitleSize) = mutableState.update { it.copy(subtitleSize = value) }

    fun startDemo() {
        demoJob?.cancel()
        mutableState.update(InterpreterStateMachine::start)
        demoJob = viewModelScope.launch {
            for (frame in DEMO_FRAMES) {
                delay(frame.delayMs)
                if (frame.isFinal) mutableState.update {
                    InterpreterStateMachine.commit(it, frame.english)
                } else mutableState.update {
                    InterpreterStateMachine.showDraft(it, frame.english)
                }
            }
            mutableState.update(InterpreterStateMachine::complete)
        }
    }

    fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
        mutableState.update(InterpreterStateMachine::stop)
    }

    private data class DemoFrame(val delayMs: Long, val english: String, val isFinal: Boolean)

    private companion object {
        // Fictional English examples deliberately unrelated to the selected source language.
        val DEMO_FRAMES = listOf(
            DemoFrame(650, "A good idea is", false),
            DemoFrame(1100, "A good idea is only the beginning.", true),
            DemoFrame(800, "Progress comes from", false),
            DemoFrame(1200, "Progress comes from the decisions we make consistently, not from waiting for the perfect moment.", true),
            DemoFrame(850, "That is how we", false),
            DemoFrame(1250, "That is how we turn a long-term vision into something practical today.", true)
        )
    }
}
