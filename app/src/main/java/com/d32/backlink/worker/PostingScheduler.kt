package com.d32.backlink.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PostingScheduler {

    private const val WORK_NAME = "d32_posting_periodic"

    fun schedule(context: Context, intervalHours: Long = 12) {
        val req = PeriodicWorkRequestBuilder<PostingWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            req
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun runNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<PostingWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(PostingWorker.WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }
}
