package com.d32.backlink.posting

import android.content.Context
import android.content.SharedPreferences

class PlatformTokenStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var sencemomEnabled: Boolean
        get() = prefs.getBoolean(KEY_SENCEMOM_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_SENCEMOM_ENABLED, v).apply()

    var sencemomToken: String
        get() = prefs.getString(KEY_SENCEMOM_TOKEN, "") ?: ""
        set(v) = prefs.edit().putString(KEY_SENCEMOM_TOKEN, v).apply()

    var tistoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_TISTORY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_TISTORY_ENABLED, v).apply()

    var tistoryToken: String
        get() = prefs.getString(KEY_TISTORY_TOKEN, "") ?: ""
        set(v) = prefs.edit().putString(KEY_TISTORY_TOKEN, v).apply()

    var tistoryBlog: String
        get() = prefs.getString(KEY_TISTORY_BLOG, "") ?: ""
        set(v) = prefs.edit().putString(KEY_TISTORY_BLOG, v).apply()

    var mediumEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEDIUM_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_MEDIUM_ENABLED, v).apply()

    var mediumToken: String
        get() = prefs.getString(KEY_MEDIUM_TOKEN, "") ?: ""
        set(v) = prefs.edit().putString(KEY_MEDIUM_TOKEN, v).apply()

    var mediumAuthorId: String
        get() = prefs.getString(KEY_MEDIUM_AUTHOR_ID, "") ?: ""
        set(v) = prefs.edit().putString(KEY_MEDIUM_AUTHOR_ID, v).apply()

    val anyEnabled get() = sencemomEnabled || tistoryEnabled || mediumEnabled

    companion object {
        const val PREF_NAME = "platform_settings"
        private const val KEY_SENCEMOM_ENABLED  = "sencemom_enabled"
        private const val KEY_SENCEMOM_TOKEN    = "sencemom_token"
        private const val KEY_TISTORY_ENABLED   = "tistory_enabled"
        private const val KEY_TISTORY_TOKEN     = "tistory_token"
        private const val KEY_TISTORY_BLOG      = "tistory_blog"
        private const val KEY_MEDIUM_ENABLED    = "medium_enabled"
        private const val KEY_MEDIUM_TOKEN      = "medium_token"
        private const val KEY_MEDIUM_AUTHOR_ID  = "medium_author_id"

        @Volatile private var INSTANCE: PlatformTokenStore? = null

        fun getInstance(context: Context): PlatformTokenStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlatformTokenStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}
