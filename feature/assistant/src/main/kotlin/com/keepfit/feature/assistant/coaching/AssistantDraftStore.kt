package com.keepfit.feature.assistant.coaching

data class AssistantDraftSnapshot(
    val draftText: String = "",
    val selectedIntent: CoachingIntent = CoachingIntent.WEEKLY_SUMMARY,
    val proposal: CoachingProposal? = null,
)

interface AssistantDraftStore {
    fun read(): AssistantDraftSnapshot
    fun write(snapshot: AssistantDraftSnapshot)
    fun clear()
}
