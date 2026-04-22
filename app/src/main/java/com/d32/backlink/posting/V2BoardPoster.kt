package com.d32.backlink.posting

import android.util.Log
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class V2BoardPoster(private val store: PlatformTokenStore) {

    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .build()

    companion object {
        private const val TAG = "V2BoardPoster"
        private const val BASE = "https://v2.d32.org"
    }

    suspend fun post(content: SeoContent): PostResult {
        return try {
            val cookie = login() ?: return PostResult(
                platform = PostResult.Platform.V2BOARD,
                success = false,
                error = "로그인 실패 — 이메일/비밀번호 확인"
            )
            writePost(cookie, content)
        } catch (e: Exception) {
            Log.e(TAG, "post error", e)
            PostResult(platform = PostResult.Platform.V2BOARD, success = false, error = e.message)
        }
    }

    private fun login(): String? {
        val body = FormBody.Builder()
            .add("email", store.v2Email)
            .add("password", store.v2Password)
            .build()

        val request = Request.Builder()
            .url("$BASE/auth/login.php")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        Log.d(TAG, "login HTTP ${response.code}: $responseBody")

        val json = runCatching { JSONObject(responseBody) }.getOrNull()
        if (json?.optBoolean("success", false) != true) {
            Log.w(TAG, "login rejected: ${json?.optString("message")}")
            return null
        }

        val cookies = response.headers.values("Set-Cookie")
        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { it.substringBefore(";").trim() }
    }

    private fun writePost(cookie: String, content: SeoContent): PostResult {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("action", "create")
            .addFormDataPart("title", content.title)
            .addFormDataPart("content", content.bodyHtml)
            .addFormDataPart("target_url", "https://www.d32.org")
            .addFormDataPart("tags", content.tags.joinToString(","))
            .build()

        val request = Request.Builder()
            .url("$BASE/api/board.php")
            .post(body)
            .addHeader("Cookie", cookie)
            .addHeader("Referer", "$BASE/board_write.php")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        Log.d(TAG, "writePost HTTP ${response.code}: $responseBody")

        val json = runCatching { JSONObject(responseBody) }.getOrNull()
        val success = json?.optBoolean("success", false) ?: false
        val postId = json?.optString("id")?.takeIf { it.isNotEmpty() }
        val postUrl = if (postId != null) "$BASE/board_view.php?id=$postId" else "$BASE/board.php"

        return if (success) {
            PostResult(platform = PostResult.Platform.V2BOARD, success = true, postUrl = postUrl)
        } else {
            val msg = json?.optString("message") ?: "HTTP ${response.code}"
            PostResult(platform = PostResult.Platform.V2BOARD, success = false, error = msg)
        }
    }

    suspend fun testConnection(): String {
        return try {
            login() ?: return "로그인 실패 — 이메일/비밀번호를 확인하세요"
            "로그인 성공 ✅ (세션 확인됨)"
        } catch (e: Exception) {
            "오류: ${e.message}"
        }
    }
}
