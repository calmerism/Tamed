package com.tamed.music.playback.smart

import kotlinx.coroutines.flow.MutableStateFlow

enum class TrackAnalysisState {
    WAITING,
    ANALYSING,
    ANALYSED,
    REFINING,
    READY,
    FAILED,
}

data class SmartAnalysis(
    val current: TrackAnalysisState = TrackAnalysisState.WAITING,
    val next: TrackAnalysisState = TrackAnalysisState.WAITING,
)

data class TransitionWindow(val start: Float, val end: Float)

enum class AutomixPerformanceMode(val inferenceThreads: Int) {
    BALANCED(2),
    PERFORMANCE(4),
    EFFICIENT(1),
    BATTERY_SAVER(1),
}

object AutomixSettings {
    val enabled = MutableStateFlow(false)
    val performanceMode = MutableStateFlow(AutomixPerformanceMode.BALANCED)
    val crossfadeDurationSeconds = MutableStateFlow(0)
    val smartMixInProgress = MutableStateFlow(false)
    val smartTransitionWindow = MutableStateFlow<TransitionWindow?>(null)
    val smartAnalysis = MutableStateFlow(SmartAnalysis())
    val playbackSpeed = MutableStateFlow(1f)
}
