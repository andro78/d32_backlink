package com.d32.backlink.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val boardApi: BoardApiService = Retrofit.Builder()
        .baseUrl("https://sencemom.site/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BoardApiService::class.java)
}
