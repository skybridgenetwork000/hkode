package com.hkode.h3nrican3.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class SketchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)
        NotificationScheduler.createNotificationChannel(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                AppSecurityManager.onAppBackgrounded()
            }

            override fun onStart(owner: LifecycleOwner) {
                AppSecurityManager.onAppForegrounded(this@SketchApplication)
            }
        })
    }
}

