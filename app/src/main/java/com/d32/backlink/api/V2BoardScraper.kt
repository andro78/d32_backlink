package com.d32.backlink.api

import com.d32.backlink.model.BacklinkTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object V2BoardScraper {

    private const val BASE = "https://v2.d32.org"
    private const val REFERER = "$BASE/board.php"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val linkRegex = Regex("""href="/board_view\.php\?id=(\d+)">([^<]+)</a>""")

    suspend fun fetchTargets(email: String, password: String): List<BacklinkTarget> = withContext(Dispatchers.IO) {
        val cookie = login(email, password) ?: return@withContext emptyList()
        val targets = mutableListOf<BacklinkTarget>()
        var page = 1
        while (true) {
            val html = fetchPage(cookie, page) ?: break
            val found = parsePosts(html)
            if (found.isEmpty()) break
            targets += found
            if (!hasNextPage(html, page)) break
            page++
        }
        targets
    }

    private fun login(email: String, password: String): String? {
        val body = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()
        val response = client.newCall(
            Request.Builder().url("$BASE/auth/login.php").post(body).build()
        ).execute()
        response.body?.string()
        val cookies = response.headers.values("Set-Cookie")
        return if (cookies.isNotEmpty())
            cookies.joinToString("; ") { it.substringBefore(";").trim() }
        else null
    }

    private fun fetchPage(cookie: String, page: Int): String? {
        val request = Request.Builder()
            .url("$BASE/board.php?page=$page")
            .addHeader("Cookie", cookie)
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) response.body?.string() else null
    }

    private fun parsePosts(html: String): List<BacklinkTarget> =
        linkRegex.findAll(html).map { m ->
            val id = m.groupValues[1]
            val title = m.groupValues[2].trim()
            BacklinkTarget(
                url = "$BASE/board_view.php?id=$id",
                title = title,
                referer = REFERER
            )
        }.toList()

    private fun hasNextPage(html: String, currentPage: Int): Boolean =
        html.contains("href=\"?page=${currentPage + 1}\"") ||
        html.contains("href=\"/board.php?page=${currentPage + 1}\"")
}
