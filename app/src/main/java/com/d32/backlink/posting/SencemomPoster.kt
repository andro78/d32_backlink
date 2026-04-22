package com.d32.backlink.posting

import com.d32.backlink.api.RetrofitClient
import com.d32.backlink.api.WritePostRequest

class SencemomPoster(private val store: PlatformTokenStore) {

    suspend fun post(content: SeoContent): PostResult {
        return try {
            RetrofitClient.boardApi.writePost(
                WritePostRequest(
                    name    = "관리자",
                    title   = content.title,
                    content = content.bodyHtml,
                    link    = "https://www.d32.org"
                )
            )
            PostResult(platform = PostResult.Platform.SENCEMOM, success = true)
        } catch (e: Exception) {
            PostResult(platform = PostResult.Platform.SENCEMOM, success = false, error = e.message)
        }
    }
}
