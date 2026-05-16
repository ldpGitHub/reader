package com.ldp.reader.widget.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.widget.Scroller

/**
 * Created by Administrator on 2016/8/1 0001.
 */
abstract class AnimationProvider(width: Int, height: Int) {
    protected var mCurPageBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    protected var mNextPageBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    protected var myStartX = 0f
    protected var myStartY = 0f
    protected var myEndX = 0
    protected var myEndY = 0
    protected var myDirection: Direction? = null

    protected var mScreenWidth = width
    protected var mScreenHeight = height

    protected var mTouch = PointF() // 拖拽点
    private var direction = Direction.NONE
    private var isCancel = false

    // 绘制滑动页面
    abstract fun drawMove(canvas: Canvas)

    // 绘制不滑动页面
    abstract fun drawStatic(canvas: Canvas)

    // 设置开始拖拽点
    fun setStartPoint(x: Float, y: Float) {
        myStartX = x
        myStartY = y
    }

    // 设置拖拽点
    fun setTouchPoint(x: Float, y: Float) {
        mTouch.x = x
        mTouch.y = y
    }

    fun setDirection(direction: Direction) {
        this.direction = direction
    }

    fun getDirection(): Direction {
        return direction
    }

    fun setCancel(isCancel: Boolean) {
        this.isCancel = isCancel
    }

    abstract fun startAnimation(scroller: Scroller)

    /**
     * 转换页面，在显示下一章的时候，必须首先调用此方法
     */
    fun changePage() {
        val bitmap = mCurPageBitmap
        mCurPageBitmap = mNextPageBitmap
        mNextPageBitmap = bitmap
    }

    fun getNextBitmap(): Bitmap {
        return mNextPageBitmap
    }

    fun getBgBitmap(): Bitmap {
        return mNextPageBitmap
    }

    fun getCancel(): Boolean {
        return isCancel
    }

    enum class Direction(@JvmField val isHorizontal: Boolean) {
        NONE(true),
        NEXT(true),
        PRE(true),
        UP(false),
        DOWN(false)
    }
}
