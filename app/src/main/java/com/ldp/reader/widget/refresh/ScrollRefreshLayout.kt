package com.ldp.reader.widget.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ldp.reader.R
import com.ldp.reader.databinding.LayoutScrollRefreshBinding
import com.ldp.reader.databinding.ViewRefreshTipBinding

abstract class ScrollRefreshLayout @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(mContext, attrs) {
    private lateinit var mFlContent: FrameLayout
    private lateinit var mTvTip: TextView
    private lateinit var mEmptyView: View
    private var mContentView: View? = null

    private lateinit var mTipOpenAnim: Animation
    private lateinit var mTopCloseAnim: Animation

    @LayoutRes
    private var mEmptyId = R.layout.view_empty

    init {
        initAttrs(attrs)
        initView()
    }

    abstract fun getContentView(parent: ViewGroup): View?

    fun toggleTip() {
        initAnim()
        cancelAnim()
        if (mTvTip.visibility == GONE) {
            mTvTip.visibility = VISIBLE
            mTvTip.startAnimation(mTipOpenAnim)
        } else {
            mTvTip.startAnimation(mTopCloseAnim)
            mTvTip.visibility = GONE
        }
    }

    private fun initAnim() {
        if (!::mTipOpenAnim.isInitialized || !::mTopCloseAnim.isInitialized) {
            mTipOpenAnim = AnimationUtils.loadAnimation(mContext, R.anim.slide_left_in)
            mTopCloseAnim = AnimationUtils.loadAnimation(mContext, R.anim.slide_right_out)

            mTipOpenAnim.fillAfter = true
            mTopCloseAnim.fillAfter = true
        }
    }

    fun setTip(str: String?) {
        mTvTip.text = str
    }

    fun showTip() {
        toggleTip()
        val runnable = Runnable {
            mTvTip.startAnimation(mTopCloseAnim)
            mTvTip.visibility = GONE
        }
        mTvTip.removeCallbacks(runnable)
        if (mTvTip.visibility == VISIBLE) {
            mTvTip.postDelayed(runnable, 2000)
        }
    }

    private fun cancelAnim() {
        if (mTipOpenAnim.hasStarted()) {
            mTipOpenAnim.cancel()
        }
        if (mTopCloseAnim.hasStarted()) {
            mTipOpenAnim.cancel()
        }
    }

    protected fun showEmptyView() {
        mEmptyView.visibility = VISIBLE
    }

    protected fun hideEmptyView() {
        mEmptyView.visibility = GONE
    }

    val emptyView: View
        get() = mEmptyView

    private fun initAttrs(attrs: AttributeSet?) {
        val array = mContext.obtainStyledAttributes(attrs, R.styleable.ScrollRefreshLayout)
        val emptyId = array.getResourceId(R.styleable.ScrollRefreshLayout_layout_scroll_empty, ATTR_NULL)
        if (emptyId != ATTR_NULL) {
            mEmptyId = emptyId
        }
    }

    private fun initView() {
        val binding = LayoutScrollRefreshBinding.inflate(LayoutInflater.from(mContext), this, false)
        val view = binding.root
        addView(view)
        mFlContent = binding.scrollRefreshFlContent
        mTvTip = ViewRefreshTipBinding.bind(mFlContent.getChildAt(0)).scrollRefreshTvTip

        mEmptyView = inflateId(mFlContent, mEmptyId)
        mFlContent.addView(mEmptyView)

        mContentView = getContentView(mFlContent)

        if (mContentView != null) {
            val params = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            mFlContent.addView(mContentView, params)
            mContentView!!.visibility = GONE
        }
    }

    private fun inflateId(parent: ViewGroup, @LayoutRes id: Int): View {
        return LayoutInflater.from(mContext).inflate(id, parent, false)
    }

    companion object {
        private const val ATTR_NULL = -1
    }
}
