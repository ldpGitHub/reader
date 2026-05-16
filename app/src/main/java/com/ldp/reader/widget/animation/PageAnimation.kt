package com.ldp.reader.widget.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Scroller

/**
 * Created by ldp on 17-7-24.
 * 翻页动画抽象类
 */
abstract class PageAnimation {
    // 正在使用的View
    protected var mView: View?

    // 滑动装置
    protected var mScroller: Scroller

    // 监听器
    protected var mListener: OnPageChangeListener

    // 移动方向
    protected var mDirection = Direction.NONE

    protected var running = false

    // 屏幕的尺寸
    protected var mScreenWidth: Int
    protected var mScreenHeight: Int

    // 屏幕的间距
    protected var mMarginWidth: Int
    protected var mMarginHeight: Int

    // 视图的尺寸
    protected var mViewWidth: Int
    protected var mViewHeight: Int

    // 起始点
    protected var mStartX = 0f
    protected var mStartY = 0f

    // 触碰点
    protected var mTouchX = 0f
    protected var mTouchY = 0f

    // 上一个触碰点
    protected var mLastX = 0f
    protected var mLastY = 0f

    constructor(w: Int, h: Int, view: View, listener: OnPageChangeListener) :
        this(w, h, 0, 0, view, listener)

    constructor(
        w: Int,
        h: Int,
        marginWidth: Int,
        marginHeight: Int,
        view: View,
        listener: OnPageChangeListener
    ) {
        mScreenWidth = w
        mScreenHeight = h

        mMarginWidth = marginWidth
        mMarginHeight = marginHeight

        mViewWidth = mScreenWidth - mMarginWidth * 2
        mViewHeight = mScreenHeight - mMarginHeight * 2

        mView = view
        mListener = listener

        mScroller = Scroller(mView!!.context, LinearInterpolator())
    }

    open fun setStartPoint(x: Float, y: Float) {
        mStartX = x
        mStartY = y

        mLastX = mStartX
        mLastY = mStartY
    }

    open fun setTouchPoint(x: Float, y: Float) {
        mLastX = mTouchX
        mLastY = mTouchY

        mTouchX = x
        mTouchY = y
    }

    fun isRunning(): Boolean {
        return running
    }

    /**
     * 开启翻页动画
     */
    open fun startAnim() {
        if (running) {
            return
        }
        running = true
    }

    open fun setDirection(direction: Direction) {
        mDirection = direction
    }

    fun getDirection(): Direction {
        return mDirection
    }

    fun clear() {
        mView = null
    }

    /**
     * 点击事件的处理
     */
    abstract fun onTouchEvent(event: MotionEvent): Boolean

    /**
     * 绘制图形
     */
    abstract fun draw(canvas: Canvas)

    /**
     * 滚动动画
     * 必须放在computeScroll()方法中执行
     */
    abstract fun scrollAnim()

    /**
     * 取消动画
     */
    abstract fun abortAnim()

    /**
     * 获取背景板
     */
    abstract fun getBgBitmap(): Bitmap?

    /**
     * 获取内容显示版面
     */
    abstract fun getNextBitmap(): Bitmap?

    enum class Direction(@JvmField val isHorizontal: Boolean) {
        NONE(true),
        NEXT(true),
        PRE(true),
        UP(false),
        DOWN(false)
    }

    interface OnPageChangeListener {
        fun hasPrev(): Boolean
        fun hasNext(): Boolean
        fun pageCancel()
    }
}
