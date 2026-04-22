package com.d32.backlink.model

data class BacklinkTarget(
    val url: String,
    val title: String,
    val referer: String,
    var status: Status = Status.PENDING
) {
    enum class Status { PENDING, RUNNING, SUCCESS, FAILED }
}
