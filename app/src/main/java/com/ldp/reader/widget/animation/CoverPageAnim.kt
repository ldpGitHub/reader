package com.ldp.reader.widget.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Created by ldp on 17-7-24.
 */
class CoverPageAnim(w: Int, h: Int, view: View, listener: OnPageChangeListener) :
    HorizonPageAnim(w, h, view, listener) {
    private val mSrcRect = Rect(0, 0, mViewWidth, mViewHeight)
    private val mDestRect = Rect(0, 0, mViewWidth, mViewHeight)
    private val mBackShadowDrawableLR: GradientDrawable

    init {
        val mBackShadowColors = intArrayOf(0x66000000, 0x00000000)
        mBackShadowDrawableLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            mBackShadowColors
        )
        mBackShadowDrawableLR.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    override fun drawStatic(canvas: Canvas) {
        if (isCancel) {
            mNextBitmap = mCurBitmap.copy(Bitmap.Config.RGB_565, true)
            canvas.drawBitmap(mCurBitmap, 0f, 0f, null)
        } else {
            canvas.drawBitmap(mNextBitmap, 0f, 0f, null)
        }
    }

    override fun drawMove(canvas: Canvas) {
        when (mDirection) {
            Direction.NEXT -> {
                var dis = (mViewWidth - mStartX + mTouchX).toInt()
                if (dis > mViewWidth) {
                    dis = mViewWidth
                }
                // 计算bitmap截取的区域
                mSrcRect.left = mViewWidth - dis
                // 计算bitmap在canvas显示的区域
                mDestRect.right = dis
                canvas.drawBitmap(mNextBitmap, 0f, 0f, null)
                canvas.drawBitmap(mCurBitmap, mSrcRect, mDestRect, null)
                addShadow(dis, canvas)
            }

            else -> {
                mSrcRect.left = (mViewWidth - mTouchX).toInt()
                mDestRect.right = mTouchX.toInt()
                canvas.drawBitmap(mCurBitmap, 0f, 0f, null)
                canvas.drawBitmap(mNextBitmap, mSrcRect, mDestRect, null)
                addShadow(mTouchX.toInt(), canvas)
            }
        }
    }

    // 添加阴影
    fun addShadow(left: Int, canvas: Canvas) {
        mBackShadowDrawableLR.setBounds(left, 0, left + 30, mScreenHeight)
        mBackShadowDrawableLR.draw(canvas)
    }

    override fun startAnim() {
        super.startAnim()
        val dx = when (mDirection) {
            Direction.NEXT -> {
                if (isCancel) {
                    var dis = ((mViewWidth - mStartX) + mTouchX).toInt()
                    if (dis > mViewWidth) {
                        dis = mViewWidth
                    }
                    mViewWidth - dis
                } else {
                    -(mTouchX + (mViewWidth - mStartX)).toInt()
                }
            }

            else -> {
                if (isCancel) {
                    (-mTouchX).toInt()
                } else {
                    (mViewWidth - mTouchX).toInt()
                }
            }
        }

        // 滑动速度保持一致
        val duration = (400 * kotlin.math.abs(dx)) / mViewWidth
        mScroller.startScroll(mTouchX.toInt(), 0, dx, 0, duration)
    }
}
