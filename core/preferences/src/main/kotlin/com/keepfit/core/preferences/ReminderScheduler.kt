package com.keepfit.core.preferences

interface ReminderScheduler {
    suspend fun sync(settings: AppSettings)
}
