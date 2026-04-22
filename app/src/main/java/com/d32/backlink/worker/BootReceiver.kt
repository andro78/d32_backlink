package com.d32.backlink.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "부팅 완료 — 백링크 스케줄 재등록")
            BacklinkScheduler.schedule(context)
        }
    }
}
