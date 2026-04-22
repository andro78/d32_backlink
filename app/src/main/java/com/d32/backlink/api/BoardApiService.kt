package com.d32.backlink.api

import com.d32.backlink.model.BoardPost
import com.d32.backlink.model.BoardResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BoardApiService {
    @GET("api/board")
    suspend fun getPosts(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 100
    ): BoardResponse

    @POST("api/board")
    suspend fun writePost(@Body request: WritePostRequest): BoardPost
}

data class WritePostRequest(
    val name: String,
    val title: String,
    val content: String,
    val link: String
)
