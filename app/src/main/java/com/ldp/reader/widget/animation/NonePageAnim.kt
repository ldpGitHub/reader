package com.ldp.reader.widget.animation

import android.graphics.Canvas
import android.util.Log
import android.view.View

/**
 * Created by ldp on 17-7-24.
 */
class NonePageAnim(w: Int, h: Int, view: View, listener: OnPageChangeListener) :
    HorizonPageAnim(w, h, view, listener) {
    override fun drawStatic(canvas: Canvas) {
        if (isCancel) {
            canvas.drawBitmap(mCurBitmap, 0f, 0f, null)
        } else {
            Log.d(TAG, "+drawStaticmNextBitmap")
            canvas.drawBitmap(mNextBitmap, 0f, 0f, null)
        }
    }

    override fun drawMove(canvas: Canvas) {
        if (isCancel) {
            canvas.drawBitmap(mCurBitmap, 0f, 0f, null)
        } else {
            Log.d(TAG, "+drawMovemNextBitmap")
            canvas.drawBitmap(mNextBitmap, 0f, 0f, null)
        }
    }

    override fun startAnim() {
    }

    companion object {
        private const val TAG = "NonePageAnim"
    }
}
