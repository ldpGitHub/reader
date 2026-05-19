package com.ldp.reader.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.ldp.reader.utils.BookCoverUrl

object BookCoverLoader {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108 Mobile Safari/537.36"

    fun load(
        context: Context,
        coverUrl: String?,
        target: ImageView,
        placeholderResId: Int
    ) {
        load(context, coverUrl, null, target, placeholderResId)
    }

    fun load(
        context: Context,
        coverUrl: String?,
        title: String?,
        target: ImageView,
        placeholderResId: Int
    ) {
        val fallback = generatedCover(context, title, placeholderResId)
        target.setImageDrawable(fallback)
        val url = BookCoverUrl.clean(coverUrl)
        if (!BookCoverUrl.isUsable(url)) return
        Glide.with(context)
            .load(glideModel(url))
            .placeholder(fallback)
            .error(fallback)
            .centerCrop()
            .into(target)
    }

    private fun generatedCover(context: Context, title: String?, placeholderResId: Int): Drawable {
        val cleanTitle = title?.trim().orEmpty()
        if (cleanTitle.isBlank()) {
            return ContextCompat.getDrawable(context, placeholderResId)
                ?: BitmapDrawable(context.resources, Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.ARGB_8888))
        }
        val palette = COVER_PALETTE
        val background = palette[(cleanTitle.hashCode().and(Int.MAX_VALUE)) % palette.size]
        val bitmap = Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = background
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, COVER_WIDTH.toFloat(), COVER_HEIGHT.toFloat(), backgroundPaint)
        val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(46, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 54f, COVER_HEIGHT.toFloat(), spinePaint)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val displayTitle = cleanTitle.take(MAX_GENERATED_TITLE_CHARS)
        val layout = StaticLayout(
            displayTitle,
            titlePaint,
            COVER_WIDTH - 96,
            Layout.Alignment.ALIGN_CENTER,
            1.15f,
            0f,
            true
        )
        canvas.save()
        canvas.translate(74f, ((COVER_HEIGHT - layout.height) / 2f).coerceAtLeast(72f))
        layout.draw(canvas)
        canvas.restore()
        val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, COVER_HEIGHT - 72f, COVER_WIDTH.toFloat(), COVER_HEIGHT.toFloat(), bottomPaint)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun glideModel(url: String): Any {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return url
        }
        return GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", refererFor(url))
                .build()
        )
    }

    private fun refererFor(url: String): String {
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: return url
        val host = uri.host ?: return url
        return "$scheme://$host/"
    }

    private const val COVER_WIDTH = 360
    private const val COVER_HEIGHT = 480
    private const val MAX_GENERATED_TITLE_CHARS = 10
    private val COVER_PALETTE = intArrayOf(
        Color.rgb(49, 92, 116),
        Color.rgb(93, 77, 124),
        Color.rgb(116, 72, 82),
        Color.rgb(60, 108, 91),
        Color.rgb(126, 93, 55),
        Color.rgb(70, 82, 116)
    )
}
