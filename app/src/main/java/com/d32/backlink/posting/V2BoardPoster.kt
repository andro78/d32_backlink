package com.d32.backlink.posting

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class V2BoardPoster(private val store: PlatformTokenStore) {

    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .build()

    companion object {
        private const val TAG = "V2BoardPoster"
        private const val BASE = "https://v2.d32.org"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
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
        val body = JSONObject().apply {
            put("email", store.v2Email)
            put("password", store.v2Password)
        }.toString().toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url("$BASE/auth/login.php")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        Log.d(TAG, "login HTTP ${response.code}: $responseBody")

        if (!response.isSuccessful && response.code != 302) return null

        val json = runCatching { JSONObject(responseBody) }.getOrNull()
        if (json != null && json.optBoolean("success", false) == false) {
            Log.w(TAG, "login rejected: ${json.optString("message")}")
            return null
        }

        // collect Set-Cookie headers
        val cookies = response.headers.values("Set-Cookie")
        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { it.substringBefore(";").trim() }
    }

    private fun writePost(cookie: String, content: SeoContent): PostResult {
        val body = JSONObject().apply {
            put("title", content.title)
            put("content", content.bodyHtml)
        }.toString().toRequestBody(JSON_TYPE)

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
        val success = json?.optBoolean("success", false) ?: response.isSuccessful
        val postId = json?.optString("id") ?: json?.optString("post_id")
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
