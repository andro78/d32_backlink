package com.d32.backlink.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.d32.backlink.api.RetrofitClient
import com.d32.backlink.model.BacklinkTarget
import com.d32.backlink.worker.BacklinkScheduler
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _targets = MutableLiveData<List<BacklinkTarget>>(emptyList())
    val targets: LiveData<List<BacklinkTarget>> = _targets

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _status = MutableLiveData("대기 중")
    val status: LiveData<String> = _status

    private val _intervalHours = MutableLiveData(6L)
    val intervalHours: LiveData<Long> = _intervalHours

    val workInfos: LiveData<List<WorkInfo>> =
        WorkManager.getInstance(app).getWorkInfosByTagLiveData("d32_backlink_manual")

    fun loadTargets() {
        viewModelScope.launch {
            _isLoading.value = true
            _status.value = "게시판 링크 수집 중..."
            try {
                val response = RetrofitClient.boardApi.getPosts()
                val sourceSites = listOf(
                    "https://www.d32.org",
                    "https://v2.d32.org",
                    "https://sencemom.site/board.html"
                )
                val list = response.items.mapNotNull { post ->
                    val link = post.link?.trim() ?: return@mapNotNull null
                    if (!link.startsWith("http")) return@mapNotNull null
                    BacklinkTarget(
                        url     = link,
                        title   = post.title,
                        referer = sourceSites[post.id % sourceSites.size]
                    )
                }
                _targets.value = list
                _status.value = "${list.size}개 타겟 로드 완료"
                Log.d("ViewModel", "${list.size}개 링크 로드")
            } catch (e: Exception) {
                _status.value = "로드 실패: ${e.message}"
                Log.e("ViewModel", "API 오류", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun runNow() {
        BacklinkScheduler.runNow(getApplication())
        _status.value = "백링크 작업 시작됨"
    }

    fun setSchedule(hours: Long) {
        _intervalHours.value = hours
        BacklinkScheduler.schedule(getApplication(), hours)
        _status.value = "${hours}시간마다 자동 실행 설정됨"
    }

    fun cancelSchedule() {
        BacklinkScheduler.cancel(getApplication())
        _status.value = "스케줄 취소됨"
    }
}
