package com.ldp.reader.utils

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import com.ldp.reader.App
import java.lang.reflect.Method

/**
 * Created by ldp on 17-5-1.
 */
object ScreenUtils {
    @JvmStatic
    fun dpToPx(dp: Int): Int {
        val metrics = getDisplayMetrics()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), metrics)
            .toInt()
    }

    @JvmStatic
    fun pxToDp(px: Int): Int {
        val metrics = getDisplayMetrics()
        return (px / metrics.density).toInt()
    }

    @JvmStatic
    fun spToPx(sp: Int): Int {
        val metrics = getDisplayMetrics()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics)
            .toInt()
    }

    @JvmStatic
    fun pxToSp(px: Int): Int {
        val metrics = getDisplayMetrics()
        return (px / metrics.scaledDensity).toInt()
    }

    /**
     * 获取手机显示App区域的大小（头部导航栏+ActionBar+根布局），不包括虚拟按钮
     */
    @JvmStatic
    fun getAppSize(): IntArray {
        val size = IntArray(2)
        val metrics = getDisplayMetrics()
        size[0] = metrics.widthPixels
        size[1] = metrics.heightPixels
        return size
    }

    /**
     * 获取整个手机屏幕的大小(包括虚拟按钮)
     * 必须在onWindowFocus方法之后使用
     */
    @JvmStatic
    fun getScreenSize(activity: AppCompatActivity?): IntArray {
        val size = IntArray(2)
        val decorView = activity!!.window.decorView
        size[0] = decorView.width
        size[1] = decorView.height
        return size
    }

    /**
     * 获取导航栏的高度
     */
    @JvmStatic
    fun getStatusBarHeight(): Int {
        val resources = App.getContext().resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    /**
     * 获取虚拟按键的高度
     */
    @JvmStatic
    fun getNavigationBarHeight(): Int {
        var navigationBarHeight = 0
        val rs = App.getContext().resources
        val id = rs.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id > 0 && hasNavigationBar()) {
            navigationBarHeight = rs.getDimensionPixelSize(id)
        }
        return navigationBarHeight
    }

    /**
     * 是否存在虚拟按键
     */
    private fun hasNavigationBar(): Boolean {
        var hasNavigationBar = false
        val rs = App.getContext().resources
        val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id)
        }
        try {
            val systemPropertiesClass: Class<*> = Class.forName("android.os.SystemProperties")
            val m: Method = systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
            if ("1" == navBarOverride) {
                hasNavigationBar = false
            } else if ("0" == navBarOverride) {
                hasNavigationBar = true
            }
        } catch (e: Exception) {
        }
        return hasNavigationBar
    }

    @JvmStatic
    fun getDisplayMetrics(): DisplayMetrics {
        return App
            .getContext()
            .resources
            .displayMetrics
    }

    /**
     * 获取字体高度
     */
    @JvmStatic
    fun getTextHeight(p: Paint?): Float {
        val fm = p!!.fontMetrics
        return ((Math.ceil((fm.descent - fm.top).toDouble()) + 2) / 2).toFloat()
    }

    @JvmStatic
    fun getTextWidth(str: String?, paint: Paint?): Int {
        val bounds = Rect()
        paint!!.getTextBounds(str, 0, str!!.length, bounds)
        return bounds.width()
    }
}
