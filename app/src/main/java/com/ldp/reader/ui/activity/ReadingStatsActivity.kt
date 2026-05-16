package com.ldp.reader.ui.activity

import android.graphics.Color
import android.os.Build
import android.view.View
import com.ldp.reader.databinding.ActivityReadingStatsBinding
import com.ldp.reader.ui.base.BaseActivity
import com.ldp.reader.utils.ReadingStatsUtils

class ReadingStatsActivity : BaseActivity<ActivityReadingStatsBinding>() {
    override fun getViewBinding(): ActivityReadingStatsBinding {
        return ActivityReadingStatsBinding.inflate(layoutInflater)
    }

    override fun initWidget() {
        super.initWidget()
        setLightStatusBar()
        refreshStats()
    }

    override fun initClick() {
        super.initClick()
        binding.readingStatsBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val nowMs = System.currentTimeMillis()
        binding.readingStatsTotalValue.text =
            ReadingStatsUtils.formatDurationLabel(ReadingStatsUtils.getTotalReadingMillis())
        binding.readingStatsTodayValue.text =
            ReadingStatsUtils.formatDurationLabel(ReadingStatsUtils.getTodayReadingMillis(nowMs))
        binding.readingStatsWeekValue.text =
            ReadingStatsUtils.formatDurationLabel(ReadingStatsUtils.getWeeklyReadingMillis(nowMs))
    }

    private fun setLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.WHITE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}
