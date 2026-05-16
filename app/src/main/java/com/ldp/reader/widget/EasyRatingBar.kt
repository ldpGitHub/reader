package com.ldp.reader.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.ldp.reader.R
import com.ldp.reader.utils.ScreenUtils
import java.lang.ref.WeakReference

class EasyRatingBar @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(mContext, attrs, defStyleAttr) {
    private var mRateCount = 0
    private var mCurrentRate = 0
    private var mNormalRes = 0
    private var mSelectRes = 0
    private var mInterval = 0
    private var isIndicator = false

    private var mRoomWidth = 0
    private var mRoomHeight = 0

    private var mNormalWeak: WeakReference<Drawable>? = null
    private var mSelectWeak: WeakReference<Drawable>? = null
    private lateinit var mPaint: Paint

    init {
        initAttrs(attrs)
        init()
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val a = mContext.obtainStyledAttributes(attrs, R.styleable.EasyRatingBar)
        mRateCount = a.getInteger(R.styleable.EasyRatingBar_rateNum, 5)
        mNormalRes = a.getResourceId(R.styleable.EasyRatingBar_rateNormal, R.drawable.rating_star_nor)
        mSelectRes = a.getResourceId(R.styleable.EasyRatingBar_rateSelect, R.drawable.rating_star_sel)
        mInterval = a.getDimension(R.styleable.EasyRatingBar_rateInterval, ScreenUtils.dpToPx(4).toFloat()).toInt()
        isIndicator = a.getBoolean(R.styleable.EasyRatingBar_isIndicator, true)

        val currentRate = a.getInteger(R.styleable.EasyRatingBar_rating, 0)
        if (currentRate < mRateCount) {
            mCurrentRate = currentRate
        }
        a.recycle()
    }

    private fun init() {
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRoomHeight = h
        mRoomWidth = w / mRateCount
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            val viewHeight = initRoomHeight
            val viewWidth = viewHeight * mRateCount
            widthSize = viewWidth
            heightSize = viewHeight
        } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY) {
            val viewWidth = heightSize * mRateCount
            widthSize = viewWidth
        } else if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST) {
            heightSize = widthSize / mRateCount
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    private val initRoomHeight: Int
        get() {
            val normal = getDrawable(mNormalWeak, mNormalRes)
            val select = getDrawable(mSelectWeak, mSelectRes)
            val normalMin = normal.intrinsicWidth.coerceAtMost(normal.intrinsicHeight)
            val selectMin = select.intrinsicWidth.coerceAtMost(select.intrinsicHeight)
            val drawableMin = normalMin.coerceAtMost(selectMin)
            return ScreenUtils.dpToPx(DEFAULT_MAX_HEIGHT).coerceAtMost(drawableMin)
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val normalDrawable = getDrawable(mNormalWeak, mNormalRes)
        val selectDrawable = getDrawable(mSelectWeak, mSelectRes)
        val radius = mRoomWidth.coerceAtMost(mRoomHeight) / 2 - mInterval
        for (i in 0 until mRateCount) {
            val roomWidthCenter = if (i == 0) {
                mRoomWidth / 2 - mInterval
            } else if (i == mRateCount - 1) {
                mRoomWidth / 2 + mRoomWidth * i + mInterval
            } else {
                mRoomWidth / 2 + mRoomWidth * i
            }
            val roomHeightCenter = mRoomHeight / 2
            canvas.save()
            canvas.translate(roomWidthCenter.toFloat(), roomHeightCenter.toFloat())
            normalDrawable.setBounds(-radius, -radius, radius, radius)
            normalDrawable.draw(canvas)
            if (i < mCurrentRate) {
                selectDrawable.setBounds(-radius, -radius, radius, radius)
                selectDrawable.draw(canvas)
            }
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    private fun getDrawable(weak: WeakReference<Drawable>?, res: Int): Drawable {
        var localWeak = weak
        val drawable: Drawable? = if (localWeak == null || localWeak.get() == null) {
            val loaded = ContextCompat.getDrawable(mContext, res)
            localWeak = WeakReference(loaded)
            loaded
        } else {
            localWeak.get()
        }
        return drawable!!
    }

    fun setRating(currentRate: Int) {
        mCurrentRate = currentRate
        invalidate()
    }

    fun setRateCount(rateCount: Int) {
        mRateCount = rateCount
        invalidate()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superParcel = super.onSaveInstanceState()
        val savedState = SavedState(superParcel)
        savedState.rateCount = mRateCount
        savedState.currentRate = mCurrentRate
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        mRateCount = savedState.rateCount
        mCurrentRate = savedState.currentRate
    }

    internal class SavedState : BaseSavedState {
        var rateCount = 0
        var currentRate = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            rateCount = parcel.readInt()
            currentRate = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(rateCount)
            out.writeInt(currentRate)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_MAX_HEIGHT = 48
    }
}
