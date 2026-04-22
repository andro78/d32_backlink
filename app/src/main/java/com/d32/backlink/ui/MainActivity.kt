package com.d32.backlink.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkInfo
import com.d32.backlink.databinding.ActivityMainBinding
import com.d32.backlink.worker.BacklinkScheduler

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private val adapter = TargetAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupRecyclerView()
        setupSpinner()
        setupButtons()
        observeViewModel()

        // 앱 시작 시 링크 자동 로드
        vm.loadTargets()
        // 기본 스케줄 등록
        BacklinkScheduler.schedule(this, 6)
    }

    private fun setupRecyclerView() {
        b.recyclerView.adapter = adapter
    }

    private fun setupSpinner() {
        val intervals = listOf("1시간", "3시간", "6시간", "12시간", "24시간")
        val hours     = listOf(1L, 3L, 6L, 12L, 24L)
        b.spinnerInterval.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        b.spinnerInterval.setSelection(2) // 기본 6시간
        b.spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                vm.setSchedule(hours[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        b.btnRunNow.setOnClickListener { vm.runNow() }
        b.btnReload.setOnClickListener { vm.loadTargets() }
        b.btnCancel.setOnClickListener { vm.cancelSchedule() }
    }

    private fun observeViewModel() {
        vm.targets.observe(this) { list ->
            adapter.submitList(list)
            b.tvTargetCount.text = "타겟 ${list.size}개"
        }

        vm.isLoading.observe(this) { loading ->
            b.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            b.btnReload.isEnabled    = !loading
        }

        vm.status.observe(this) { msg ->
            b.tvStatus.text = msg
        }

        vm.workInfos.observe(this) { infos ->
            val running = infos.any { it.state == WorkInfo.State.RUNNING }
            b.btnRunNow.isEnabled = !running
            b.tvWorkState.text = when {
                running                                        -> "⚙️ 실행 중..."
                infos.any { it.state == WorkInfo.State.SUCCEEDED } -> "✅ 마지막 실행 성공"
                infos.any { it.state == WorkInfo.State.FAILED }    -> "❌ 마지막 실행 실패"
                else                                               -> "⏳ 대기 중"
            }
        }
    }
}
