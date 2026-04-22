package com.d32.backlink.posting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MediumPoster(private val store: PlatformTokenStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun post(content: SeoContent): PostResult {
        val token = store.mediumToken
        if (token.isBlank())
            return PostResult(PostResult.Platform.MEDIUM, false, error = "토큰 없음")

        return try {
            val authorId = ensureAuthorId(token)
                ?: return PostResult(PostResult.Platform.MEDIUM, false, error = "authorId 조회 실패")

            val tagsArr = JSONArray().apply { content.tags.forEach { put(it) } }
            val json = JSONObject().apply {
                put("title",         content.title)
                put("contentFormat", "html")
                put("content",       content.bodyHtml)
                put("tags",          tagsArr)
                put("publishStatus", "public")
            }

            val req = Request.Builder()
                .url("https://api.medium.com/v1/users/$authorId/posts")
                .addHeader("Authorization", "Bearer $token")
                .post(json.toString().toRequestBody(JSON_TYPE))
                .build()

            val res  = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            val body = res.body?.string() ?: ""

            if (res.code in 200..201) {
                val url = JSONObject(body).optJSONObject("data")?.optString("url")
                PostResult(PostResult.Platform.MEDIUM, true, postUrl = url)
            } else {
                PostResult(PostResult.Platform.MEDIUM, false, error = "HTTP ${res.code}: $body")
            }
        } catch (e: Exception) {
            PostResult(PostResult.Platform.MEDIUM, false, error = e.message)
        }
    }

    suspend fun fetchAndCacheAuthorId(): String {
        val token = store.mediumToken
        if (token.isBlank()) return "토큰을 먼저 입력하세요"
        return try {
            val id = fetchAuthorId(token)
            if (id != null) {
                store.mediumAuthorId = id
                "연결 성공 ✅ (ID: $id)"
            } else {
                "authorId 조회 실패"
            }
        } catch (e: Exception) {
            "오류: ${e.message}"
        }
    }

    private suspend fun ensureAuthorId(token: String): String? {
        val cached = store.mediumAuthorId
        if (cached.isNotBlank()) return cached
        val id = fetchAuthorId(token) ?: return null
        store.mediumAuthorId = id
        return id
    }

    private suspend fun fetchAuthorId(token: String): String? {
        val req = Request.Builder()
            .url("https://api.medium.com/v1/me")
            .addHeader("Authorization", "Bearer $token")
            .get().build()
        val res  = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        val body = res.body?.string() ?: return null
        return JSONObject(body).optJSONObject("data")?.optString("id")
    }
}
