package com.keepfit.feature.steps.data

enum class StepsStatus {
    LOADING,
    UNAVAILABLE,
    UPDATE_REQUIRED,
    PERMISSION_REQUIRED,
    CONNECTED,
    ERROR,
}

data class StepsSummary(
    val todaySteps: Long,
    val sevenDayTotal: Long,
) {
    val sevenDayAverage: Long
        get() = if (sevenDayTotal == 0L) 0L else kotlin.math.round(sevenDayTotal / 7.0).toLong()

    val hasData: Boolean
        get() = todaySteps > 0L || sevenDayTotal > 0L
}

data class StepsUiState(
    val status: StepsStatus = StepsStatus.LOADING,
    val summary: StepsSummary? = null,
    val message: String? = null,
)

sealed interface StepsSnapshot {
    data object Unavailable : StepsSnapshot

    data object UpdateRequired : StepsSnapshot

    data object PermissionRequired : StepsSnapshot

    data class Connected(val summary: StepsSummary) : StepsSnapshot
}

fun stepsUiStateForSnapshot(snapshot: StepsSnapshot): StepsUiState {
    return when (snapshot) {
        StepsSnapshot.PermissionRequired -> StepsUiState(status = StepsStatus.PERMISSION_REQUIRED)
        StepsSnapshot.Unavailable -> StepsUiState(status = StepsStatus.UNAVAILABLE)
        StepsSnapshot.UpdateRequired -> StepsUiState(status = StepsStatus.UPDATE_REQUIRED)
        is StepsSnapshot.Connected -> StepsUiState(
            status = StepsStatus.CONNECTED,
            summary = snapshot.summary,
        )
    }
}

fun stepsUiStateForError(error: Throwable): StepsUiState {
    return StepsUiState(
        status = StepsStatus.ERROR,
        message = error.message?.takeUnless(String::isBlank)
            ?: "Health Connect steps could not be loaded.",
    )
}
