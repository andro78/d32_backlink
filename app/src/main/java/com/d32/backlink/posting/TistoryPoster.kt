package com.d32.backlink.posting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TistoryPoster(private val store: PlatformTokenStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun post(content: SeoContent): PostResult {
        val token = store.tistoryToken
        val blog  = store.tistoryBlog
        if (token.isBlank() || blog.isBlank())
            return PostResult(PostResult.Platform.TISTORY, false, error = "토큰 또는 블로그명 없음")

        return try {
            val form = FormBody.Builder()
                .add("access_token", token)
                .add("output",       "json")
                .add("blogName",     blog)
                .add("title",        content.title)
                .add("content",      content.bodyHtml)
                .add("visibility",   "3")
                .add("acceptComment","1")
                .add("tag",          content.tags.joinToString(","))
                .build()

            val req = Request.Builder()
                .url("https://www.tistory.com/apis/post/write")
                .post(form)
                .build()

            val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            val body = res.body?.string() ?: ""

            val json   = JSONObject(body).optJSONObject("tistory")
            val status = json?.optString("status")
            val url    = json?.optString("url")

            if (status == "200") PostResult(PostResult.Platform.TISTORY, true, postUrl = url)
            else PostResult(PostResult.Platform.TISTORY, false, error = "status=$status body=$body")
        } catch (e: Exception) {
            PostResult(PostResult.Platform.TISTORY, false, error = e.message)
        }
    }

    suspend fun testConnection(): String {
        val token = store.tistoryToken
        val blog  = store.tistoryBlog
        if (token.isBlank() || blog.isBlank()) return "토큰/블로그명을 먼저 입력하세요"
        return try {
            val req = Request.Builder()
                .url("https://www.tistory.com/apis/blog/info?access_token=$token&output=json&blogName=$blog")
                .get().build()
            val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            val json = JSONObject(res.body?.string() ?: "")
            val status = json.optJSONObject("tistory")?.optString("status")
            if (status == "200") "연결 성공 ✅" else "연결 실패: $json"
        } catch (e: Exception) {
            "오류: ${e.message}"
        }
    }
}
