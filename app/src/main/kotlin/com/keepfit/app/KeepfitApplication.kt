package com.keepfit.app

import android.app.Application
import com.keepfit.core.preferences.ReminderNotifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KeepfitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannels(this)
    }
}
