package com.ldp.reader.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.ldp.reader.R;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by ldp on 17-4-14.
 */

public class SplashActivity extends AppCompatActivity {

    private Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        PermissionUtils.permission(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.WRITE_EXTERNAL_STORAGE).callback((isAllGranted, granted, deniedForever, denied) -> {
            if (isAllGranted) {
                skipToMain();
            } else {
                if (denied.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    ToastUtils.showLong("请允许存储权限");
                }
                skipToMain();
            }
            if (!deniedForever.isEmpty()) {
                ToastUtils.showLong("请允许存储权限");
                PermissionUtils.launchAppDetailsSettings();
            }

        }).request();
        super.onCreate(savedInstanceState);
        BarUtils.transparentStatusBar(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        unbinder = ButterKnife.bind(this);
    }


    private synchronized void skipToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
