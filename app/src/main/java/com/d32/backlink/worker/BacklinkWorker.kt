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
import com.d32.backlink.api.RetrofitClient
import com.d32.backlink.api.V2BoardScraper
import com.d32.backlink.model.BacklinkTarget
import com.d32.backlink.posting.PlatformTokenStore
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class BacklinkWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        const val TAG = "BacklinkWorker"
        const val KEY_URL    = "url"
        const val KEY_STATUS = "status"
        const val KEY_DONE   = "done"
        const val KEY_TOTAL  = "total"
        private val SOURCE_SITES = listOf(
            "https://www.d32.org",
            "https://v2.d32.org",
            "https://v2.d32.org/board.php",
            "https://sencemom.site/board.html"
        )
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun getForegroundInfo() = buildForegroundInfo("백링크 작업 준비 중...")

    override suspend fun doWork(): Result {
        setForeground(buildForegroundInfo("백링크 작업 실행 중..."))
        Log.d(TAG, "백링크 작업 시작")

        val targets = collectTargets()
        if (targets.isEmpty()) {
            Log.w(TAG, "타겟 URL 없음")
            return Result.success()
        }

        Log.d(TAG, "총 ${targets.size}개 타겟 처리 시작")
        var successCount = 0
        var doneCount = 0

        targets.forEach { target ->
            setProgress(workDataOf(KEY_URL to target.url, KEY_STATUS to "RUNNING", KEY_DONE to doneCount, KEY_TOTAL to targets.size))
            val ok = visitWithReferer(target.url, target.referer)
            if (ok) successCount++
            doneCount++
            setProgress(workDataOf(KEY_URL to target.url, KEY_STATUS to if (ok) "SUCCESS" else "FAILED", KEY_DONE to doneCount, KEY_TOTAL to targets.size))
            delay(Random.nextLong(3_000, 8_000))
        }

        Log.d(TAG, "완료: $successCount / ${targets.size} 성공")
        return Result.success()
    }

    // sencemom 게시판에서 link 수집 + 소스사이트 자체도 방문
    private suspend fun collectTargets(): List<BacklinkTarget> {
        val targets = mutableListOf<BacklinkTarget>()

        // 1) 게시판 posts의 link 필드 수집
        try {
            val response = RetrofitClient.boardApi.getPosts(page = 1, size = 100)
            response.items.forEach { post ->
                val link = post.link?.trim() ?: ""
                if (link.startsWith("http")) {
                    // 3개 소스 사이트를 referer로 순환 배정
                    val referer = SOURCE_SITES[post.id % SOURCE_SITES.size]
                    targets.add(
                        BacklinkTarget(
                            url     = link,
                            title   = post.title,
                            referer = referer
                        )
                    )
                }
            }
            Log.d(TAG, "게시판에서 ${targets.size}개 링크 수집")
        } catch (e: Exception) {
            Log.e(TAG, "게시판 API 실패: ${e.message}")
        }

        // 2) v2.d32.org 게시판 게시글 수집
        try {
            val store = PlatformTokenStore.getInstance(applicationContext)
            if (store.v2Email.isNotEmpty() && store.v2Password.isNotEmpty()) {
                val v2Targets = V2BoardScraper.fetchTargets(store.v2Email, store.v2Password)
                targets += v2Targets
                Log.d(TAG, "v2.d32.org에서 ${v2Targets.size}개 링크 수집")
            }
        } catch (e: Exception) {
            Log.e(TAG, "v2.d32.org 스크래핑 실패: ${e.message}")
        }

        // 3) 소스 사이트 자체도 서로 백링크 교차 방문
        SOURCE_SITES.forEachIndexed { i, url ->
            val referer = SOURCE_SITES[(i + 1) % SOURCE_SITES.size]
            targets.add(BacklinkTarget(url = url, title = "소스사이트", referer = referer))
        }

        return targets.distinctBy { it.url }
    }

    private fun visitWithReferer(url: String, referer: String): Boolean {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("Referer",    referer)
                .header("User-Agent", randomUserAgent())
                .header("Accept",     "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                .get()
                .build()
            val resp = http.newCall(req).execute()
            val code = resp.code
            resp.close()
            Log.d(TAG, "[$code] $url  (referer: $referer)")
            code in 200..399
        } catch (e: Exception) {
            Log.w(TAG, "방문 실패 $url : ${e.message}")
            false
        }
    }

    private fun buildForegroundInfo(text: String): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(applicationContext, App.NOTIF_CHANNEL_ID)
            .setContentTitle("D32 Backlink")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1001, notification)
    }

    private fun randomUserAgent(): String {
        val agents = listOf(
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        return agents.random()
    }
}
