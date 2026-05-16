package com.ldp.reader.widget.animation

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View

/**
 * Created by ldp on 17-7-24.
 */
class SlidePageAnim(w: Int, h: Int, view: View, listener: OnPageChangeListener) :
    HorizonPageAnim(w, h, view, listener) {
    private val mSrcRect = Rect(0, 0, mViewWidth, mViewHeight)
    private val mDestRect = Rect(0, 0, mViewWidth, mViewHeight)
    private val mNextSrcRect = Rect(0, 0, mViewWidth, mViewHeight)
    private val mNextDestRect = Rect(0, 0, mViewWidth, mViewHeight)

    override fun drawStatic(canvas: Canvas) {
        if (isCancel) {
            canvas.drawBitmap(mCurBitmap, 0f, 0f, null)
        } else {
            canvas.drawBitmap(mNextBitmap, 0f, 0f, null)
        }
    }

    override fun drawMove(canvas: Canvas) {
        when (mDirection) {
            Direction.NEXT -> {
                // 左半边的剩余区域
                var dis = (mScreenWidth - mStartX + mTouchX).toInt()
                if (dis > mScreenWidth) {
                    dis = mScreenWidth
                }
                // 计算bitmap截取的区域
                mSrcRect.left = mScreenWidth - dis
                // 计算bitmap在canvas显示的区域
                mDestRect.right = dis
                // 计算下一页截取的区域
                mNextSrcRect.right = mScreenWidth - dis
                // 计算下一页在canvas显示的区域
                mNextDestRect.left = dis

                canvas.drawBitmap(mNextBitmap, mNextSrcRect, mNextDestRect, null)
                canvas.drawBitmap(mCurBitmap, mSrcRect, mDestRect, null)
            }

            else -> {
                var dis = (mTouchX - mStartX).toInt()
                if (dis < 0) {
                    dis = 0
                    mStartX = mTouchX
                }
                mSrcRect.left = mScreenWidth - dis
                mDestRect.right = dis

                // 计算下一页截取的区域
                mNextSrcRect.right = mScreenWidth - dis
                // 计算下一页在canvas显示的区域
                mNextDestRect.left = dis

                canvas.drawBitmap(mCurBitmap, mNextSrcRect, mNextDestRect, null)
                canvas.drawBitmap(mNextBitmap, mSrcRect, mDestRect, null)
            }
        }
    }

    override fun startAnim() {
        super.startAnim()
        val dx = when (mDirection) {
            Direction.NEXT -> {
                if (isCancel) {
                    var dis = ((mScreenWidth - mStartX) + mTouchX).toInt()
                    if (dis > mScreenWidth) {
                        dis = mScreenWidth
                    }
                    mScreenWidth - dis
                } else {
                    -(mTouchX + (mScreenWidth - mStartX)).toInt()
                }
            }

            else -> {
                if (isCancel) {
                    -kotlin.math.abs(mTouchX - mStartX).toInt()
                } else {
                    (mScreenWidth - (mTouchX - mStartX)).toInt()
                }
            }
        }
        // 滑动速度保持一致
        val duration = (400 * kotlin.math.abs(dx)) / mScreenWidth
        mScroller.startScroll(mTouchX.toInt(), 0, dx, 0, duration)
    }
}
