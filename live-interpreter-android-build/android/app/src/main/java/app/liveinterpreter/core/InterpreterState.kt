package app.liveinterpreter.core

/** Pure immutable state, testable without the Android SDK or a network connection. */
enum class SourceLanguage(val code: String, val displayName: String) {
    LATVIAN("lv", "Latvian"),
    LITHUANIAN("lt", "Lithuanian");
}

enum class DemoPhase { READY, RUNNING, COMPLETE }
enum class MicPermission { NOT_CHECKED, GRANTED, DENIED }
enum class SubtitleSize { NORMAL, LARGE, EXTRA_LARGE }

data class Caption(val id: Long, val english: String)

data class InterpreterUiState(
    val language: SourceLanguage = SourceLanguage.LATVIAN,
    val phase: DemoPhase = DemoPhase.READY,
    val microphonePermission: MicPermission = MicPermission.NOT_CHECKED,
    val audioEnabled: Boolean = false,
    val keepScreenOn: Boolean = true,
    val subtitleSize: SubtitleSize = SubtitleSize.LARGE,
    val committed: List<Caption> = emptyList(),
    val provisional: String? = null,
    val nextCaptionId: Long = 1L
) {
    val hasAnyCaptions: Boolean get() = committed.isNotEmpty() || !provisional.isNullOrBlank()
}

object InterpreterStateMachine {
    fun start(previous: InterpreterUiState): InterpreterUiState = previous.copy(
        phase = DemoPhase.RUNNING,
        committed = emptyList(),
        provisional = null,
        nextCaptionId = 1L
    )

    fun stop(previous: InterpreterUiState): InterpreterUiState = previous.copy(
        phase = DemoPhase.READY,
        provisional = null
    )

    fun showDraft(previous: InterpreterUiState, english: String): InterpreterUiState =
        if (previous.phase == DemoPhase.RUNNING && english.isNotBlank())
            previous.copy(provisional = english.trim())
        else previous

    fun commit(previous: InterpreterUiState, english: String): InterpreterUiState =
        if (previous.phase == DemoPhase.RUNNING && english.isNotBlank()) {
            previous.copy(
                committed = previous.committed + Caption(previous.nextCaptionId, english.trim()),
                provisional = null,
                nextCaptionId = previous.nextCaptionId + 1
            )
        } else previous

    fun complete(previous: InterpreterUiState): InterpreterUiState =
        if (previous.phase == DemoPhase.RUNNING)
            previous.copy(phase = DemoPhase.COMPLETE, provisional = null)
        else previous
}
