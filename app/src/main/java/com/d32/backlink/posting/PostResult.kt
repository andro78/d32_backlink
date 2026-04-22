package com.d32.backlink.posting

data class PostResult(
    val platform: Platform,
    val success: Boolean,
    val postUrl: String? = null,
    val error: String? = null
) {
    enum class Platform(val label: String) {
        SENCEMOM("Sencemom"),
        TISTORY("Tistory"),
        MEDIUM("Medium")
    }
}
