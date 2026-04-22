package com.d32.backlink

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    companion object {
        const val NOTIF_CHANNEL_ID = "backlink_worker"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "백링크 작업",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백링크 자동화 백그라운드 작업"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
