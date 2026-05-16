package com.ldp.reader.widget.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Region
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Created by ldp on 17-7-24.
 */
class SimulationPageAnim(w: Int, h: Int, view: View, listener: OnPageChangeListener) :
    HorizonPageAnim(w, h, view, listener) {
    private var mCornerX = 1 // 拖拽点对应的页脚
    private var mCornerY = 1
    private val mPath0 = Path()
    private val mPath1 = Path()

    private val mBezierStart1 = PointF() // 贝塞尔曲线起始点
    private val mBezierControl1 = PointF() // 贝塞尔曲线控制点
    private var mBeziervertex1 = PointF() // 贝塞尔曲线顶点
    private var mBezierEnd1 = PointF() // 贝塞尔曲线结束点

    private val mBezierStart2 = PointF() // 另一条贝塞尔曲线
    private val mBezierControl2 = PointF()
    private var mBeziervertex2 = PointF()
    private var mBezierEnd2 = PointF()

    private var mMiddleX = 0f
    private var mMiddleY = 0f
    private var mDegrees = 0f
    private var mTouchToCornerDis = 0f
    private val mColorMatrixFilter: ColorMatrixColorFilter
    private val mMatrix = Matrix()
    private val mMatrixArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1.0f)

    private var mIsRTandLB = false // 是否属于右上左下
    private val mMaxLength: Float
    private lateinit var mBackShadowColors: IntArray // 背面颜色组
    private lateinit var mFrontShadowColors: IntArray // 前面颜色组
    private lateinit var mBackShadowDrawableLR: GradientDrawable // 有阴影的GradientDrawable
    private lateinit var mBackShadowDrawableRL: GradientDrawable
    private lateinit var mFolderShadowDrawableLR: GradientDrawable
    private lateinit var mFolderShadowDrawableRL: GradientDrawable

    private lateinit var mFrontShadowDrawableHBT: GradientDrawable
    private lateinit var mFrontShadowDrawableHTB: GradientDrawable
    private lateinit var mFrontShadowDrawableVLR: GradientDrawable
    private lateinit var mFrontShadowDrawableVRL: GradientDrawable

    private val mPaint = Paint()

    init {
        mMaxLength = Math.hypot(mScreenWidth.toDouble(), mScreenHeight.toDouble()).toFloat()
        mPaint.style = Paint.Style.FILL

        createDrawable()

        val cm = ColorMatrix() // 设置颜色数组
        val array = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        cm.set(array)
        mColorMatrixFilter = ColorMatrixColorFilter(cm)

        mTouchX = 0.01f // 不让x,y为0,否则在点计算时会有问题
        mTouchY = 0.01f
    }

    override fun drawMove(canvas: Canvas) {
        when (mDirection) {
            Direction.NEXT -> {
                calcPoints()
                drawCurrentPageArea(canvas, mCurBitmap, mPath0)
                drawNextPageAreaAndShadow(canvas, mNextBitmap)
                drawCurrentPageShadow(canvas)
                drawCurrentBackArea(canvas, mCurBitmap)
            }

            else -> {
                calcPoints()
                drawCurrentPageArea(canvas, mNextBitmap, mPath0)
                drawNextPageAreaAndShadow(canvas, mCurBitmap)
                drawCurrentPageShadow(canvas)
                drawCurrentBackArea(canvas, mNextBitmap)
            }
        }
    }

    override fun drawStatic(canvas: Canvas) {
        if (isCancel) {
            mNextBitmap = mCurBitmap.copy(Bitmap.Config.RGB_565, true)
            canvas.drawBitmap(mCurBitmap, 0f, 0f, null)
        } else {
            canvas.drawBitmap(mNextBitmap, 0f, 0f, null)
        }
    }

    override fun startAnim() {
        super.startAnim()
        val dx: Int
        val dy: Int
        // dx 水平方向滑动的距离，负值会使滚动向左滚动
        // dy 垂直方向滑动的距离，负值会使滚动向上滚动
        if (isCancel) {
            var localDx = if (mCornerX > 0 && mDirection == Direction.NEXT) {
                (mScreenWidth - mTouchX).toInt()
            } else {
                -mTouchX.toInt()
            }

            if (mDirection != Direction.NEXT) {
                localDx = -(mScreenWidth + mTouchX).toInt()
            }
            dx = localDx

            dy = if (mCornerY > 0) {
                (mScreenHeight - mTouchY).toInt()
            } else {
                -mTouchY.toInt() // 防止mTouchY最终变为0
            }
        } else {
            dx = if (mCornerX > 0 && mDirection == Direction.NEXT) {
                -(mScreenWidth + mTouchX).toInt()
            } else {
                (mScreenWidth - mTouchX + mScreenWidth).toInt()
            }
            dy = if (mCornerY > 0) {
                (mScreenHeight - mTouchY).toInt()
            } else {
                (1 - mTouchY).toInt() // 防止mTouchY最终变为0
            }
        }
        mScroller.startScroll(mTouchX.toInt(), mTouchY.toInt(), dx, dy, 400)
    }

    override fun setDirection(direction: Direction) {
        super.setDirection(direction)

        when (direction) {
            Direction.PRE -> {
                // 上一页滑动不出现对角
                if (mStartX > mScreenWidth / 2) {
                    calcCornerXY(mStartX, mScreenHeight.toFloat())
                } else {
                    calcCornerXY(mScreenWidth - mStartX, mScreenHeight.toFloat())
                }
            }

            Direction.NEXT -> {
                if (mScreenWidth / 2 > mStartX) {
                    calcCornerXY(mScreenWidth - mStartX, mStartY)
                }
            }

            else -> {
            }
        }
    }

    override fun setStartPoint(x: Float, y: Float) {
        super.setStartPoint(x, y)
        calcCornerXY(x, y)
    }

    override fun setTouchPoint(x: Float, y: Float) {
        super.setTouchPoint(x, y)
        // 触摸y中间位置吧y变成屏幕高度
        if ((mStartY > mScreenHeight / 3 && mStartY < mScreenHeight * 2 / 3) ||
            mDirection == Direction.PRE
        ) {
            mTouchY = mScreenHeight.toFloat()
        }

        if (mStartY > mScreenHeight / 3 && mStartY < mScreenHeight / 2 &&
            mDirection == Direction.NEXT
        ) {
            mTouchY = 1f
        }
    }

    /**
     * 创建阴影的GradientDrawable
     */
    private fun createDrawable() {
        val color = intArrayOf(0x333333, 0xb0333333.toInt())
        mFolderShadowDrawableRL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT,
            color
        )
        mFolderShadowDrawableRL.gradientType = GradientDrawable.LINEAR_GRADIENT

        mFolderShadowDrawableLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            color
        )
        mFolderShadowDrawableLR.gradientType = GradientDrawable.LINEAR_GRADIENT

        mBackShadowColors = intArrayOf(0xff111111.toInt(), 0x111111)
        mBackShadowDrawableRL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT,
            mBackShadowColors
        )
        mBackShadowDrawableRL.gradientType = GradientDrawable.LINEAR_GRADIENT

        mBackShadowDrawableLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            mBackShadowColors
        )
        mBackShadowDrawableLR.gradientType = GradientDrawable.LINEAR_GRADIENT

        mFrontShadowColors = intArrayOf(0x80111111.toInt(), 0x111111)
        mFrontShadowDrawableVLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            mFrontShadowColors
        )
        mFrontShadowDrawableVLR.gradientType = GradientDrawable.LINEAR_GRADIENT
        mFrontShadowDrawableVRL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT,
            mFrontShadowColors
        )
        mFrontShadowDrawableVRL.gradientType = GradientDrawable.LINEAR_GRADIENT

        mFrontShadowDrawableHTB = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            mFrontShadowColors
        )
        mFrontShadowDrawableHTB.gradientType = GradientDrawable.LINEAR_GRADIENT

        mFrontShadowDrawableHBT = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            mFrontShadowColors
        )
        mFrontShadowDrawableHBT.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    /**
     * 是否能够拖动过去
     */
    fun canDragOver(): Boolean {
        if (mTouchToCornerDis > mScreenWidth / 10) {
            return true
        }
        return false
    }

    fun right(): Boolean {
        if (mCornerX > -4) {
            return false
        }
        return true
    }

    /**
     * 绘制翻起页背面
     */
    private fun drawCurrentBackArea(canvas: Canvas, bitmap: Bitmap) {
        val i = ((mBezierStart1.x + mBezierControl1.x).toInt()) / 2
        val f1 = kotlin.math.abs(i - mBezierControl1.x)
        val i1 = ((mBezierStart2.y + mBezierControl2.y).toInt()) / 2
        val f2 = kotlin.math.abs(i1 - mBezierControl2.y)
        val f3 = kotlin.math.min(f1, f2)
        mPath1.reset()
        mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y)
        mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y)
        mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y)
        mPath1.lineTo(mTouchX, mTouchY)
        mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        mPath1.close()
        val mFolderShadowDrawable: GradientDrawable
        val left: Int
        val right: Int
        if (mIsRTandLB) {
            left = (mBezierStart1.x - 1).toInt()
            right = (mBezierStart1.x + f3 + 1).toInt()
            mFolderShadowDrawable = mFolderShadowDrawableLR
        } else {
            left = (mBezierStart1.x - f3 - 1).toInt()
            right = (mBezierStart1.x + 1).toInt()
            mFolderShadowDrawable = mFolderShadowDrawableRL
        }
        canvas.save()
        try {
            canvas.clipPath(mPath0)
            canvas.clipPath(mPath1, Region.Op.INTERSECT)
        } catch (e: Exception) {
        }

        mPaint.colorFilter = mColorMatrixFilter
        // 对Bitmap进行取色
        val color = bitmap.getPixel(1, 1)
        // 获取对应的三色
        val red = color and 0xff0000 shr 16
        val green = color and 0x00ff00 shr 8
        val blue = color and 0x0000ff
        // 转换成含有透明度的颜色
        val tempColor = Color.argb(200, red, green, blue)

        val dis = Math.hypot(
            (mCornerX - mBezierControl1.x).toDouble(),
            (mBezierControl2.y - mCornerY).toDouble()
        ).toFloat()
        val f8 = (mCornerX - mBezierControl1.x) / dis
        val f9 = (mBezierControl2.y - mCornerY) / dis
        mMatrixArray[0] = 1 - 2 * f9 * f9
        mMatrixArray[1] = 2 * f8 * f9
        mMatrixArray[3] = mMatrixArray[1]
        mMatrixArray[4] = 1 - 2 * f8 * f8
        mMatrix.reset()
        mMatrix.setValues(mMatrixArray)
        mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y)
        mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y)
        canvas.drawBitmap(bitmap, mMatrix, mPaint)
        // 背景叠加
        canvas.drawColor(tempColor)

        mPaint.colorFilter = null

        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y)
        mFolderShadowDrawable.setBounds(
            left,
            mBezierStart1.y.toInt(),
            right,
            (mBezierStart1.y + mMaxLength).toInt()
        )
        mFolderShadowDrawable.draw(canvas)
        canvas.restore()
    }

    /**
     * 绘制翻起页的阴影
     */
    fun drawCurrentPageShadow(canvas: Canvas) {
        val degree: Double = if (mIsRTandLB) {
            Math.PI / 4 - Math.atan2(
                (mBezierControl1.y - mTouchY).toDouble(),
                (mTouchX - mBezierControl1.x).toDouble()
            )
        } else {
            Math.PI / 4 - Math.atan2(
                (mTouchY - mBezierControl1.y).toDouble(),
                (mTouchX - mBezierControl1.x).toDouble()
            )
        }
        // 翻起页阴影顶点与touch点的距离
        val d1 = 25f * 1.414 * Math.cos(degree)
        val d2 = 25f * 1.414 * Math.sin(degree)
        val x = (mTouchX + d1).toFloat()
        val y = if (mIsRTandLB) {
            (mTouchY + d2).toFloat()
        } else {
            (mTouchY - d2).toFloat()
        }
        mPath1.reset()
        mPath1.moveTo(x, y)
        mPath1.lineTo(mTouchX, mTouchY)
        mPath1.lineTo(mBezierControl1.x, mBezierControl1.y)
        mPath1.lineTo(mBezierStart1.x, mBezierStart1.y)
        mPath1.close()
        var rotateDegrees: Float
        canvas.save()
        try {
            canvas.clipPath(mPath0, Region.Op.DIFFERENCE)
            canvas.clipPath(mPath1, Region.Op.INTERSECT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var leftx: Int
        var rightx: Int
        var mCurrentPageShadow: GradientDrawable
        if (mIsRTandLB) {
            leftx = mBezierControl1.x.toInt()
            rightx = mBezierControl1.x.toInt() + 25
            mCurrentPageShadow = mFrontShadowDrawableVLR
        } else {
            leftx = (mBezierControl1.x - 25).toInt()
            rightx = mBezierControl1.x.toInt() + 1
            mCurrentPageShadow = mFrontShadowDrawableVRL
        }

        rotateDegrees = Math.toDegrees(
            Math.atan2(
                (mTouchX - mBezierControl1.x).toDouble(),
                (mBezierControl1.y - mTouchY).toDouble()
            )
        ).toFloat()
        canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y)
        mCurrentPageShadow.setBounds(
            leftx,
            (mBezierControl1.y - mMaxLength).toInt(),
            rightx,
            mBezierControl1.y.toInt()
        )

        mCurrentPageShadow.draw(canvas)
        canvas.restore()

        mPath1.reset()
        mPath1.moveTo(x, y)
        mPath1.lineTo(mTouchX, mTouchY)
        mPath1.lineTo(mBezierControl2.x, mBezierControl2.y)
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y)
        mPath1.close()
        canvas.save()
        try {
            canvas.clipPath(mPath0, Region.Op.DIFFERENCE)
            canvas.clipPath(mPath1, Region.Op.INTERSECT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (mIsRTandLB) {
            leftx = mBezierControl2.y.toInt()
            rightx = (mBezierControl2.y + 25).toInt()
            mCurrentPageShadow = mFrontShadowDrawableHTB
        } else {
            leftx = (mBezierControl2.y - 25).toInt()
            rightx = (mBezierControl2.y + 1).toInt()
            mCurrentPageShadow = mFrontShadowDrawableHBT
        }
        rotateDegrees = Math.toDegrees(
            Math.atan2(
                (mBezierControl2.y - mTouchY).toDouble(),
                (mBezierControl2.x - mTouchX).toDouble()
            )
        ).toFloat()
        canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y)
        val temp = if (mBezierControl2.y < 0) {
            mBezierControl2.y - mScreenHeight
        } else {
            mBezierControl2.y
        }

        val hmg = Math.hypot(mBezierControl2.x.toDouble(), temp.toDouble()).toInt()
        if (hmg > mMaxLength) {
            mCurrentPageShadow.setBounds(
                (mBezierControl2.x - 25).toInt() - hmg,
                leftx,
                (mBezierControl2.x + mMaxLength).toInt() - hmg,
                rightx
            )
        } else {
            mCurrentPageShadow.setBounds(
                (mBezierControl2.x - mMaxLength).toInt(),
                leftx,
                mBezierControl2.x.toInt(),
                rightx
            )
        }

        mCurrentPageShadow.draw(canvas)
        canvas.restore()
    }

    private fun drawNextPageAreaAndShadow(canvas: Canvas, bitmap: Bitmap) {
        mPath1.reset()
        mPath1.moveTo(mBezierStart1.x, mBezierStart1.y)
        mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y)
        mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y)
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y)
        mPath1.lineTo(mCornerX.toFloat(), mCornerY.toFloat())
        mPath1.close()

        mDegrees = Math.toDegrees(
            Math.atan2(
                (mBezierControl1.x - mCornerX).toDouble(),
                (mBezierControl2.y - mCornerY).toDouble()
            )
        ).toFloat()
        val leftx: Int
        val rightx: Int
        val mBackShadowDrawable: GradientDrawable
        if (mIsRTandLB) { // 左下及右上
            leftx = mBezierStart1.x.toInt()
            rightx = (mBezierStart1.x + mTouchToCornerDis / 4).toInt()
            mBackShadowDrawable = mBackShadowDrawableLR
        } else {
            leftx = (mBezierStart1.x - mTouchToCornerDis / 4).toInt()
            rightx = mBezierStart1.x.toInt()
            mBackShadowDrawable = mBackShadowDrawableRL
        }
        canvas.save()
        try {
            canvas.clipPath(mPath0)
            canvas.clipPath(mPath1, Region.Op.INTERSECT)
        } catch (e: Exception) {
        }

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y)
        mBackShadowDrawable.setBounds(
            leftx,
            mBezierStart1.y.toInt(),
            rightx,
            (mMaxLength + mBezierStart1.y).toInt()
        )
        mBackShadowDrawable.draw(canvas)
        canvas.restore()
    }

    private fun drawCurrentPageArea(canvas: Canvas, bitmap: Bitmap, path: Path) {
        mPath0.reset()
        mPath0.moveTo(mBezierStart1.x, mBezierStart1.y)
        mPath0.quadTo(
            mBezierControl1.x,
            mBezierControl1.y,
            mBezierEnd1.x,
            mBezierEnd1.y
        )
        mPath0.lineTo(mTouchX, mTouchY)
        mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        mPath0.quadTo(
            mBezierControl2.x,
            mBezierControl2.y,
            mBezierStart2.x,
            mBezierStart2.y
        )
        mPath0.lineTo(mCornerX.toFloat(), mCornerY.toFloat())
        mPath0.close()

        canvas.save()
        canvas.clipPath(path, Region.Op.DIFFERENCE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        try {
            canvas.restore()
        } catch (e: Exception) {
        }
    }

    /**
     * 计算拖拽点对应的拖拽脚
     */
    fun calcCornerXY(x: Float, y: Float) {
        mCornerX = if (x <= mScreenWidth / 2) {
            0
        } else {
            mScreenWidth
        }
        mCornerY = if (y <= mScreenHeight / 2) {
            0
        } else {
            mScreenHeight
        }

        mIsRTandLB = (mCornerX == 0 && mCornerY == mScreenHeight) ||
            (mCornerX == mScreenWidth && mCornerY == 0)
    }

    private fun calcPoints() {
        mMiddleX = (mTouchX + mCornerX) / 2
        mMiddleY = (mTouchY + mCornerY) / 2
        mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) *
            (mCornerY - mMiddleY) / (mCornerX - mMiddleX)
        mBezierControl1.y = mCornerY.toFloat()
        mBezierControl2.x = mCornerX.toFloat()

        val f4 = mCornerY - mMiddleY
        mBezierControl2.y = if (f4 == 0f) {
            mMiddleY - (mCornerX - mMiddleX) *
                (mCornerX - mMiddleX) / 0.1f
        } else {
            mMiddleY - (mCornerX - mMiddleX) *
                (mCornerX - mMiddleX) / (mCornerY - mMiddleY)
        }
        mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2
        mBezierStart1.y = mCornerY.toFloat()

        // 当mBezierStart1.x < 0或者mBezierStart1.x > 480时
        // 如果继续翻页，会出现BUG故在此限制
        if (mTouchX > 0 && mTouchX < mScreenWidth) {
            if (mBezierStart1.x < 0 || mBezierStart1.x > mScreenWidth) {
                if (mBezierStart1.x < 0) {
                    mBezierStart1.x = mScreenWidth - mBezierStart1.x
                }

                val f1 = kotlin.math.abs(mCornerX - mTouchX)
                val f2 = mScreenWidth * f1 / mBezierStart1.x
                mTouchX = kotlin.math.abs(mCornerX - f2)

                val f3 = kotlin.math.abs(mCornerX - mTouchX) *
                    kotlin.math.abs(mCornerY - mTouchY) / f1
                mTouchY = kotlin.math.abs(mCornerY - f3)

                mMiddleX = (mTouchX + mCornerX) / 2
                mMiddleY = (mTouchY + mCornerY) / 2

                mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) *
                    (mCornerY - mMiddleY) / (mCornerX - mMiddleX)
                mBezierControl1.y = mCornerY.toFloat()

                mBezierControl2.x = mCornerX.toFloat()

                val f5 = mCornerY - mMiddleY
                mBezierControl2.y = if (f5 == 0f) {
                    mMiddleY - (mCornerX - mMiddleX) *
                        (mCornerX - mMiddleX) / 0.1f
                } else {
                    mMiddleY - (mCornerX - mMiddleX) *
                        (mCornerX - mMiddleX) / (mCornerY - mMiddleY)
                }

                mBezierStart1.x = mBezierControl1.x -
                    (mCornerX - mBezierControl1.x) / 2
            }
        }
        mBezierStart2.x = mCornerX.toFloat()
        mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y) / 2

        mTouchToCornerDis = Math.hypot(
            (mTouchX - mCornerX).toDouble(),
            (mTouchY - mCornerY).toDouble()
        ).toFloat()

        mBezierEnd1 = getCross(
            PointF(mTouchX, mTouchY),
            mBezierControl1,
            mBezierStart1,
            mBezierStart2
        )
        mBezierEnd2 = getCross(
            PointF(mTouchX, mTouchY),
            mBezierControl2,
            mBezierStart1,
            mBezierStart2
        )

        mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4
        mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4
        mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4
        mBeziervertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4
    }

    /**
     * 求解直线P1P2和直线P3P4的交点坐标
     */
    fun getCross(P1: PointF, P2: PointF, P3: PointF, P4: PointF): PointF {
        val crossP = PointF()
        // 二元函数通式： y=ax+b
        val a1 = (P2.y - P1.y) / (P2.x - P1.x)
        val b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x)

        val a2 = (P4.y - P3.y) / (P4.x - P3.x)
        val b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x)
        crossP.x = (b2 - b1) / (a1 - a2)
        crossP.y = a1 * crossP.x + b1
        return crossP
    }
}
