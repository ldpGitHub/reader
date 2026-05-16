package com.ldp.reader.ui.activity

import android.graphics.Color
import android.os.Build
import android.view.View
import com.ldp.reader.R
import com.ldp.reader.databinding.ActivityAboutBinding
import com.ldp.reader.ui.base.BaseActivity

class AboutActivity : BaseActivity<ActivityAboutBinding>() {
    override fun getViewBinding(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun initWidget() {
        super.initWidget()
        setLightStatusBar()
        binding.aboutAppName.text = getString(R.string.app_name)
        binding.aboutVersion.text = "版本 ${versionName()}"
    }

    override fun initClick() {
        super.initClick()
        binding.aboutBack.setOnClickListener { finish() }
    }

    private fun versionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
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
