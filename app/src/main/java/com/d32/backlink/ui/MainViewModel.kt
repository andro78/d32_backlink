package com.d32.backlink.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.d32.backlink.api.RetrofitClient
import com.d32.backlink.model.BacklinkTarget
import com.d32.backlink.worker.BacklinkScheduler
import com.d32.backlink.worker.BacklinkWorker
import com.d32.backlink.worker.PostingScheduler
import com.d32.backlink.worker.PostingWorker
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _targets = MutableLiveData<List<BacklinkTarget>>(emptyList())
    private val _isLoading = MutableLiveData(false)
    private val _status = MutableLiveData("대기 중")
    private val _progressMap = MutableLiveData<Map<String, BacklinkTarget.Status>>(emptyMap())
    private val accumulatedStatuses = mutableMapOf<String, BacklinkTarget.Status>()

    val isLoading: LiveData<Boolean> = _isLoading
    val status: LiveData<String> = _status

    val workInfos: LiveData<List<WorkInfo>> =
        WorkManager.getInstance(app).getWorkInfosByTagLiveData("d32_backlink_manual")

    val postingWorkInfos: LiveData<List<WorkInfo>> =
        WorkManager.getInstance(app).getWorkInfosByTagLiveData(PostingWorker.WORK_TAG)

    private val _postingStatus = MutableLiveData("플랫폼을 설정하고 포스팅을 실행하세요")
    val postingStatus: LiveData<String> = _postingStatus

    val displayTargets: MediatorLiveData<List<BacklinkTarget>> = MediatorLiveData<List<BacklinkTarget>>().apply {
        fun merge() {
            val base = _targets.value ?: emptyList()
            val map = _progressMap.value ?: emptyMap()
            value = base.map { t -> t.copy(status = map[t.url] ?: BacklinkTarget.Status.PENDING) }
        }
        addSource(_targets) { merge() }
        addSource(_progressMap) { merge() }
    }

    fun onWorkInfosChanged(infos: List<WorkInfo>) {
        infos.forEach { info ->
            val p = info.progress
            val url = p.getString(BacklinkWorker.KEY_URL) ?: return@forEach
            val statusStr = p.getString(BacklinkWorker.KEY_STATUS) ?: return@forEach
            accumulatedStatuses[url] = when (statusStr) {
                "RUNNING" -> BacklinkTarget.Status.RUNNING
                "SUCCESS" -> BacklinkTarget.Status.SUCCESS
                "FAILED"  -> BacklinkTarget.Status.FAILED
                else      -> return@forEach
            }
            val done  = p.getInt(BacklinkWorker.KEY_DONE, 0)
            val total = p.getInt(BacklinkWorker.KEY_TOTAL, 0)
            if (total > 0) _status.value = "진행 중: $done / $total"
        }
        _progressMap.value = accumulatedStatuses.toMap()

        if (infos.any { it.state == WorkInfo.State.SUCCEEDED }) {
            val success = accumulatedStatuses.values.count { it == BacklinkTarget.Status.SUCCESS }
            _status.value = "완료: $success / ${accumulatedStatuses.size} 성공"
        }
    }

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
                    BacklinkTarget(url = link, title = post.title, referer = sourceSites[post.id % sourceSites.size])
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
        accumulatedStatuses.clear()
        _progressMap.value = emptyMap()
        BacklinkScheduler.runNow(getApplication())
        _status.value = "백링크 작업 시작됨"
    }

    fun setSchedule(hours: Long) {
        BacklinkScheduler.schedule(getApplication(), hours)
        _status.value = "${hours}시간마다 자동 실행 설정됨"
    }

    fun cancelSchedule() {
        BacklinkScheduler.cancel(getApplication())
        _status.value = "스케줄 취소됨"
    }

    fun runPostingNow() {
        PostingScheduler.runNow(getApplication())
        _postingStatus.value = "포스팅 작업 시작됨"
    }

    fun onPostingWorkInfosChanged(infos: List<WorkInfo>) {
        infos.forEach { info ->
            val p        = info.progress
            val platform = p.getString(PostingWorker.KEY_PLATFORM) ?: return@forEach
            val status   = p.getString(PostingWorker.KEY_STATUS)   ?: return@forEach
            val done     = p.getInt(PostingWorker.KEY_DONE, 0)
            val total    = p.getInt(PostingWorker.KEY_TOTAL, 0)
            _postingStatus.value = when (status) {
                "RUNNING"  -> "$platform 포스팅 중... ($done/$total)"
                "SUCCESS"  -> "$platform 포스팅 완료 ✅ ($done/$total)"
                "FAILED"   -> "$platform 포스팅 실패 ❌ ($done/$total)"
                else       -> _postingStatus.value
            }
        }
        if (infos.any { it.state == WorkInfo.State.SUCCEEDED })
            _postingStatus.value = "모든 플랫폼 포스팅 완료 ✅"
        if (infos.any { it.state == WorkInfo.State.FAILED })
            _postingStatus.value = "일부 포스팅 실패 — 플랫폼 설정을 확인하세요"
    }
}
