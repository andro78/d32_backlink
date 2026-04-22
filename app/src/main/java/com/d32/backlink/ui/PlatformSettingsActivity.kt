package com.d32.backlink.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.d32.backlink.R
import com.d32.backlink.posting.MediumPoster
import com.d32.backlink.posting.PlatformTokenStore
import com.d32.backlink.posting.TistoryPoster
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PlatformSettingsActivity : AppCompatActivity() {

    private lateinit var store: PlatformTokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_platform_settings)
        supportActionBar?.apply { title = "플랫폼 설정"; setDisplayHomeAsUpEnabled(true) }

        store = PlatformTokenStore.getInstance(this)
        loadValues()
        bindListeners()
    }

    private fun loadValues() {
        f<Switch>(R.id.switchSencemom).isChecked  = store.sencemomEnabled
        f<EditText>(R.id.etSencemomToken).setText(store.sencemomToken)

        f<Switch>(R.id.switchTistory).isChecked   = store.tistoryEnabled
        f<EditText>(R.id.etTistoryToken).setText(store.tistoryToken)
        f<EditText>(R.id.etTistoryBlog).setText(store.tistoryBlog)

        f<Switch>(R.id.switchMedium).isChecked    = store.mediumEnabled
        f<EditText>(R.id.etMediumToken).setText(store.mediumToken)
    }

    private fun bindListeners() {
        f<Button>(R.id.btnSencemomTest).setOnClickListener {
            f<TextView>(R.id.tvSencemomResult).text = "테스트 중..."
            // sencemom은 POST 성공 여부로만 확인 가능 — 저장 후 포스팅 실행으로 검증
            f<TextView>(R.id.tvSencemomResult).text = "토큰 저장 후 포스팅 실행으로 확인 가능합니다."
        }

        f<Button>(R.id.btnTistoryTest).setOnClickListener {
            val result = f<TextView>(R.id.tvTistoryResult)
            result.text = "연결 확인 중..."
            store.tistoryToken = f<EditText>(R.id.etTistoryToken).text.toString().trim()
            store.tistoryBlog  = f<EditText>(R.id.etTistoryBlog).text.toString().trim()
            lifecycleScope.launch {
                result.text = TistoryPoster(store).testConnection()
            }
        }

        f<Button>(R.id.btnMediumTest).setOnClickListener {
            val result = f<TextView>(R.id.tvMediumResult)
            result.text = "계정 조회 중..."
            store.mediumToken = f<EditText>(R.id.etMediumToken).text.toString().trim()
            lifecycleScope.launch {
                result.text = MediumPoster(store).fetchAndCacheAuthorId()
            }
        }

        f<Button>(R.id.btnSave).setOnClickListener { save() }
    }

    private fun save() {
        store.sencemomEnabled = f<Switch>(R.id.switchSencemom).isChecked
        store.sencemomToken   = f<EditText>(R.id.etSencemomToken).text.toString().trim()

        store.tistoryEnabled  = f<Switch>(R.id.switchTistory).isChecked
        store.tistoryToken    = f<EditText>(R.id.etTistoryToken).text.toString().trim()
        store.tistoryBlog     = f<EditText>(R.id.etTistoryBlog).text.toString().trim()

        store.mediumEnabled   = f<Switch>(R.id.switchMedium).isChecked
        store.mediumToken     = f<EditText>(R.id.etMediumToken).text.toString().trim()

        Snackbar.make(findViewById(android.R.id.content), "저장 완료", Snackbar.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> f(id: Int): T = findViewById(id)
}
