package com.ldp.reader.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager

/**
 * Created by ldp on 17-5-16.
 * 基于 Android 4.4
 */
object SystemBarUtils {
    private const val UNSTABLE_STATUS = View.SYSTEM_UI_FLAG_FULLSCREEN
    private const val UNSTABLE_NAV = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    private const val STABLE_STATUS = View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private const val STABLE_NAV = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private const val EXPAND_STATUS = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    private const val EXPAND_NAV = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE

    @JvmStatic
    fun hideUnStableStatusBar(activity: Activity?) {
        setFlag(activity, UNSTABLE_STATUS)
    }

    @JvmStatic
    fun showUnStableStatusBar(activity: Activity?) {
        clearFlag(activity, UNSTABLE_STATUS)
    }

    @JvmStatic
    fun hideUnStableNavBar(activity: Activity?) {
        setFlag(activity, UNSTABLE_NAV)
    }

    @JvmStatic
    fun showUnStableNavBar(activity: Activity?) {
        clearFlag(activity, UNSTABLE_NAV)
    }

    @JvmStatic
    fun hideStableStatusBar(activity: Activity?) {
        setFlag(activity, STABLE_STATUS)
    }

    @JvmStatic
    fun showStableStatusBar(activity: Activity?) {
        clearFlag(activity, STABLE_STATUS)
    }

    @JvmStatic
    fun hideStableNavBar(activity: Activity?) {
        setFlag(activity, STABLE_NAV)
    }

    @JvmStatic
    fun setStableNavBarColor(activity: Activity?, color: Int) {
        activity!!.window.navigationBarColor = color
    }

    @JvmStatic
    fun showStableNavBar(activity: Activity?) {
        clearFlag(activity, STABLE_NAV)
    }

    /**
     * 视图扩充到StatusBar
     */
    @JvmStatic
    fun expandStatusBar(activity: Activity?) {
        setFlag(activity, EXPAND_STATUS)
    }

    /**
     * 视图扩充到NavBar
     */
    @JvmStatic
    fun expandNavBar(activity: Activity?) {
        setFlag(activity, EXPAND_NAV)
    }

    @JvmStatic
    fun transparentStatusBar(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= 21) {
            expandStatusBar(activity)
            activity!!.window
                .statusBarColor = activity.resources.getColor(android.R.color.transparent)
        } else if (Build.VERSION.SDK_INT >= 19) {
            val attrs = activity!!.window.attributes
            attrs.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or attrs.flags
            activity.window.attributes = attrs
        }
    }

    @JvmStatic
    fun transparentNavBar(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= 21) {
            expandNavBar(activity)
            activity!!.window
                .navigationBarColor = activity.resources.getColor(android.R.color.transparent)
        }
    }

    @JvmStatic
    fun setFlag(activity: Activity?, flag: Int) {
        if (Build.VERSION.SDK_INT >= 19) {
            val decorView = activity!!.window.decorView
            val option = decorView.systemUiVisibility or flag
            decorView.systemUiVisibility = option
        }
    }

    @JvmStatic
    fun clearFlag(activity: Activity?, flag: Int) {
        if (Build.VERSION.SDK_INT >= 19) {
            val decorView = activity!!.window.decorView
            val option = decorView.systemUiVisibility and flag.inv()
            decorView.systemUiVisibility = option
        }
    }

    @JvmStatic
    fun setToggleFlag(activity: Activity?, option: Int) {
        if (Build.VERSION.SDK_INT >= 19) {
            if (isFlagUsed(activity, option)) {
                clearFlag(activity, option)
            } else {
                setFlag(activity, option)
            }
        }
    }

    /**
     * @return flag是否已被使用
     */
    @JvmStatic
    fun isFlagUsed(activity: Activity?, flag: Int): Boolean {
        val currentFlag = activity!!.window.decorView.systemUiVisibility
        return (currentFlag and flag) == flag
    }
}
