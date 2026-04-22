package com.d32.backlink.worker

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.d32.backlink.App
import com.d32.backlink.posting.MediumPoster
import com.d32.backlink.posting.PlatformTokenStore
import com.d32.backlink.posting.PostResult
import com.d32.backlink.posting.SencemomPoster
import com.d32.backlink.posting.SeoContentGenerator
import com.d32.backlink.posting.TistoryPoster
import com.d32.backlink.posting.V2BoardPoster

class PostingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        const val TAG          = "PostingWorker"
        const val WORK_TAG     = "d32_posting_manual"
        const val KEY_PLATFORM = "platform"
        const val KEY_STATUS   = "status"
        const val KEY_DONE     = "done"
        const val KEY_TOTAL    = "total"
    }

    override suspend fun getForegroundInfo() = buildForegroundInfo("SEO 포스팅 준비 중...")

    override suspend fun doWork(): Result {
        setForeground(buildForegroundInfo("SEO 포스팅 실행 중..."))

        val store = PlatformTokenStore.getInstance(applicationContext)
        val tasks = buildList {
            if (store.sencemomEnabled) add(PostResult.Platform.SENCEMOM)
            if (store.tistoryEnabled)  add(PostResult.Platform.TISTORY)
            if (store.mediumEnabled)   add(PostResult.Platform.MEDIUM)
            if (store.v2Enabled)       add(PostResult.Platform.V2BOARD)
        }

        if (tasks.isEmpty()) {
            Log.w(TAG, "활성화된 플랫폼 없음")
            return Result.failure()
        }

        val content       = SeoContentGenerator.generate()
        val sencemom      = SencemomPoster(store)
        val tistory       = TistoryPoster(store)
        val medium        = MediumPoster(store)
        val v2board       = V2BoardPoster(store)
        var successCount  = 0

        tasks.forEachIndexed { i, platform ->
            setProgress(workDataOf(KEY_PLATFORM to platform.label, KEY_STATUS to "RUNNING", KEY_DONE to i, KEY_TOTAL to tasks.size))

            val result = when (platform) {
                PostResult.Platform.SENCEMOM -> sencemom.post(content)
                PostResult.Platform.TISTORY  -> tistory.post(content)
                PostResult.Platform.MEDIUM   -> medium.post(content)
                PostResult.Platform.V2BOARD  -> v2board.post(content)
            }

            if (result.success) successCount++
            Log.d(TAG, "${platform.label}: ${if (result.success) "✅ ${result.postUrl}" else "❌ ${result.error}"}")

            setProgress(workDataOf(KEY_PLATFORM to platform.label, KEY_STATUS to if (result.success) "SUCCESS" else "FAILED", KEY_DONE to i + 1, KEY_TOTAL to tasks.size))
        }

        Log.d(TAG, "포스팅 완료: $successCount / ${tasks.size}")
        return if (successCount > 0) Result.success() else Result.failure()
    }

    private fun buildForegroundInfo(text: String): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(applicationContext, App.NOTIF_CHANNEL_ID)
            .setContentTitle("SEO 포스팅")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1002, notification)
    }
}
