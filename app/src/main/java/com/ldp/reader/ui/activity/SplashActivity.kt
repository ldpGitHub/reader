package com.ldp.reader.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.ldp.reader.R

/**
 * Created by ldp on 17-4-14.
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {


        PermissionUtils.permission(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        )
            .callback { isAllGranted: Boolean, granted: List<String?>?, deniedForever: List<String?>, denied: List<String?> ->
                if (isAllGranted) {
                    skipToMain()
                } else {
                    if (denied.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
//                        ToastUtils.showLong("请允许存储权限")
                    }
                    skipToMain()
                }

//                if (!deniedForever.isEmpty()) {
//                    ToastUtils.showLong("请允许存储权限")
//                    PermissionUtils.launchAppDetailsSettings()
//                }
            }.request()
        BarUtils.transparentStatusBar(this)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    @Synchronized
    private fun skipToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
