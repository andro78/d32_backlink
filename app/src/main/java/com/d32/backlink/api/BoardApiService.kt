package com.d32.backlink.api

import com.d32.backlink.model.BoardResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BoardApiService {
    @GET("api/board")
    suspend fun getPosts(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 100
    ): BoardResponse
}
