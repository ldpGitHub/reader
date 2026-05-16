package com.ldp.reader.widget.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * Created by ldp on 17-7-24.
 * 横向动画的模板
 */
abstract class HorizonPageAnim : PageAnimation {
    protected var mCurBitmap: Bitmap
    protected var mNextBitmap: Bitmap

    // 是否取消翻页
    protected var isCancel = false

    // 可以使用 mLast代替
    private var mMoveX = 0
    private var mMoveY = 0

    // 是否移动了
    private var isMove = false

    // 是否翻阅下一页。true表示翻到下一页，false表示上一页。
    private var isNext = false

    // 是否没下一页或者上一页
    private var noNext = false

    constructor(w: Int, h: Int, view: View, listener: OnPageChangeListener) :
        this(w, h, 0, 0, view, listener)

    constructor(
        w: Int,
        h: Int,
        marginWidth: Int,
        marginHeight: Int,
        view: View,
        listener: OnPageChangeListener
    ) : super(w, h, marginWidth, marginHeight, view, listener) {
        // 创建图片
        mCurBitmap = Bitmap.createBitmap(mViewWidth, mViewHeight, Bitmap.Config.RGB_565)
        mNextBitmap = Bitmap.createBitmap(mViewWidth, mViewHeight, Bitmap.Config.RGB_565)
    }

    /**
     * 转换页面，在显示下一章的时候，必须首先调用此方法
     */
    fun changePage() {
        val bitmap = mCurBitmap
        mCurBitmap = mNextBitmap
        mNextBitmap = bitmap
    }

    abstract fun drawStatic(canvas: Canvas)

    abstract fun drawMove(canvas: Canvas)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 获取点击位置
        val x = event.x.toInt()
        val y = event.y.toInt()
        // 设置触摸点
        setTouchPoint(x.toFloat(), y.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 移动的点击位置
                mMoveX = 0
                mMoveY = 0
                // 是否移动
                isMove = false
                // 是否存在下一章
                noNext = false
                // 是下一章还是前一章
                isNext = false
                // 是否正在执行动画
                running = false
                // 取消
                isCancel = false
                // 设置起始位置的触摸点
                setStartPoint(x.toFloat(), y.toFloat())
                // 如果存在动画则取消动画
                abortAnim()
            }

            MotionEvent.ACTION_MOVE -> {
                val slop = ViewConfiguration.get(mView!!.context).scaledTouchSlop
                // 判断是否移动了
                if (!isMove) {
                    isMove = kotlin.math.abs(mStartX - x) > slop ||
                        kotlin.math.abs(mStartY - y) > slop
                }

                if (isMove) {
                    // 判断是否是准备移动的状态(将要移动但是还没有移动)
                    if (mMoveX == 0 && mMoveY == 0) {
                        // 判断翻得是上一页还是下一页
                        if (x - mStartX > 0) {
                            // 上一页的参数配置
                            isNext = false
                            val hasPrev = mListener.hasPrev()
                            setDirection(Direction.PRE)
                            // 如果上一页不存在
                            if (!hasPrev) {
                                noNext = true
                                return true
                            }
                        } else {
                            // 进行下一页的配置
                            isNext = true
                            // 判断是否下一页存在
                            val hasNext = mListener.hasNext()
                            // 如果存在设置动画方向
                            setDirection(Direction.NEXT)

                            // 如果不存在表示没有下一页了
                            if (!hasNext) {
                                noNext = true
                                return true
                            }
                        }
                    } else {
                        // 判断是否取消翻页
                        isCancel = if (isNext) {
                            x - mMoveX > 0
                        } else {
                            x - mMoveX < 0
                        }
                    }

                    mMoveX = x
                    mMoveY = y
                    running = true
                    mView!!.invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isMove) {
                    if (x < mScreenWidth / 2) {
                        isNext = false
                    } else {
                        isNext = true
                    }

                    if (isNext) {
                        // 判断是否下一页存在
                        val hasNext = mListener.hasNext()
                        // 设置动画方向
                        setDirection(Direction.NEXT)
                        if (!hasNext) {
                            return true
                        }
                    } else {
                        val hasPrev = mListener.hasPrev()
                        setDirection(Direction.PRE)
                        if (!hasPrev) {
                            return true
                        }
                    }
                }

                // 是否取消翻页
                if (isCancel) {
                    mListener.pageCancel()
                }

                // 开启翻页效果
                if (!noNext) {
                    startAnim()
                    mView!!.invalidate()
                }
            }
        }
        return true
    }

    override fun draw(canvas: Canvas) {
        if (running) {
            drawMove(canvas)
        } else {
            if (isCancel) {
                mNextBitmap = mCurBitmap.copy(Bitmap.Config.RGB_565, true)
            }
            drawStatic(canvas)
        }
    }

    override fun scrollAnim() {
        if (mScroller.computeScrollOffset()) {
            val x = mScroller.currX
            val y = mScroller.currY

            setTouchPoint(x.toFloat(), y.toFloat())

            if (mScroller.finalX == x && mScroller.finalY == y) {
                running = false
            }
            mView!!.postInvalidate()
        }
    }

    override fun abortAnim() {
        if (!mScroller.isFinished) {
            mScroller.abortAnimation()
            running = false
            setTouchPoint(mScroller.finalX.toFloat(), mScroller.finalY.toFloat())
            mView!!.postInvalidate()
        }
    }

    override fun getBgBitmap(): Bitmap {
        return mNextBitmap
    }

    override fun getNextBitmap(): Bitmap {
        return mNextBitmap
    }
}
