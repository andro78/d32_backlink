package com.d32.backlink.model

import com.google.gson.annotations.SerializedName

data class BoardPost(
    val id: Int,
    val name: String,
    val title: String,
    val content: String,
    val link: String?,
    val date: String
)

data class BoardResponse(
    val total: Int,
    val page: Int,
    val size: Int,
    val items: List<BoardPost>
)
