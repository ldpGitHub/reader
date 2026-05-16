package com.ldp.reader.utils

import android.content.Context
import androidx.annotation.StringRes
import com.ldp.reader.App
import com.ldp.reader.model.local.ReadSettingManager.SHARED_READ_CONVERT_TYPE
import com.zqc.opencc.android.lib.ChineseConverter
import com.zqc.opencc.android.lib.ConversionType
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Created by ldp on 17-4-22.
 * 对文字操作的工具类
 */
object StringUtils {
    private const val TAG = "StringUtils"
    private const val HOUR_OF_DAY = 24
    private const val DAY_OF_YESTERDAY = 2
    private const val TIME_UNIT = 60

    @JvmStatic
    fun dateConvert(time: Long, pattern: String?): String {
        var result = ""
        try {
            val date = Date(time)
            val format = SimpleDateFormat(pattern!!, Locale.CHINA)
            result = format.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    @JvmStatic
    fun dateConvert(source: String?, pattern: String?): String {
        val format: DateFormat = SimpleDateFormat(pattern!!, Locale.CHINA)
        val calendar = Calendar.getInstance()
        try {
            val date = format.parse(source!!)
            val curTime = calendar.timeInMillis
            calendar.time = date!!
            val difSec = Math.abs((curTime - date.time) / 1000)
            val difMin = difSec / 60
            val difHour = difMin / 60
            val difDate = difHour / 60
            val oldHour = calendar[Calendar.HOUR]
            if (oldHour == 0) {
                if (difDate == 0L) {
                    return "今天"
                } else if (difDate < DAY_OF_YESTERDAY) {
                    return "昨天"
                } else {
                    val convertFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
                    return convertFormat.format(date)
                }
            }

            if (difSec < TIME_UNIT) {
                return difSec.toString() + "秒前"
            } else if (difMin < TIME_UNIT) {
                return difMin.toString() + "分钟前"
            } else if (difHour < HOUR_OF_DAY) {
                return difHour.toString() + "小时前"
            } else if (difDate < DAY_OF_YESTERDAY) {
                return "昨天"
            } else {
                val convertFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
                return convertFormat.format(date)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun formatBookUpdateTime(source: String?): String {
        return formatBookUpdateTime(source, System.currentTimeMillis())
    }

    @JvmStatic
    fun formatBookUpdateTime(source: String?, nowMillis: Long): String {
        val updateMillis = parseBookUpdateMillis(source)
        if (updateMillis <= 0L) {
            return ""
        }
        val diffMillis = Math.max(0L, nowMillis - updateMillis)
        val minuteMillis = 60L * 1000L
        val hourMillis = 60L * minuteMillis
        val dayMillis = 24L * hourMillis
        if (diffMillis < minuteMillis) {
            return "刚刚"
        }
        if (diffMillis < hourMillis) {
            return Math.max(1L, diffMillis / minuteMillis).toString() + "分钟前"
        }
        if (diffMillis < dayMillis) {
            return Math.max(1L, diffMillis / hourMillis).toString() + "小时前"
        }
        if (diffMillis < 30L * dayMillis) {
            return Math.max(1L, diffMillis / dayMillis).toString() + "天前"
        }
        if (diffMillis < 365L * dayMillis) {
            return Math.max(1L, diffMillis / (30L * dayMillis)).toString() + "月前"
        }
        return Math.max(1L, diffMillis / (365L * dayMillis)).toString() + "年前"
    }

    private fun parseBookUpdateMillis(source: String?): Long {
        if (source == null || source.trim { it <= ' ' }.isEmpty()) {
            return 0L
        }
        val value = source.trim { it <= ' ' }
        try {
            val numeric = value.toLong()
            if (numeric > 0L && numeric < 10_000_000_000L) {
                return numeric * 1000L
            }
            return numeric
        } catch (ignored: NumberFormatException) {
        }
        val format: DateFormat = SimpleDateFormat(Constant.FORMAT_BOOK_DATE, Locale.CHINA)
        try {
            val date = format.parse(value)
            return date?.time ?: 0L
        } catch (ignored: ParseException) {
        }
        return 0L
    }

    @JvmStatic
    fun toFirstCapital(str: String?): String {
        return str!!.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }

    @JvmStatic
    fun getString(@StringRes id: Int): String {
        return App.getContext().resources.getString(id)
    }

    @JvmStatic
    fun getString(@StringRes id: Int, vararg formatArgs: Any?): String {
        return App.getContext().resources.getString(id, *formatArgs)
    }

    /**
     * 将文本中的半角字符，转换成全角字符
     */
    @JvmStatic
    fun halfToFull(input: String?): String {
        val c = input!!.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 32) {
                c[i] = 12288.toChar()
                continue
            }
            if (c[i].code > 32 && c[i].code < 127) {
                c[i] = (c[i].code + 65248).toChar()
            }
        }
        return String(c)
    }

    @JvmStatic
    fun fullToHalf(input: String?): String {
        val c = input!!.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 12288) {
                c[i] = 32.toChar()
                continue
            }

            if (c[i].code > 65280 && c[i].code < 65375) {
                c[i] = (c[i].code - 65248).toChar()
            }
        }
        return String(c)
    }

    @JvmStatic
    fun convertCC(input: String?, context: Context?): String {
        var currentConversionType = ConversionType.S2TWP
        val convertType = SharedPreUtils.getInstance().getInt(SHARED_READ_CONVERT_TYPE, 0)

        if (input!!.isEmpty()) {
            return ""
        }

        when (convertType) {
            1 -> currentConversionType = ConversionType.TW2SP
            2 -> currentConversionType = ConversionType.S2HK
            3 -> currentConversionType = ConversionType.S2T
            4 -> currentConversionType = ConversionType.S2TW
            5 -> currentConversionType = ConversionType.S2TWP
            6 -> currentConversionType = ConversionType.T2HK
            7 -> currentConversionType = ConversionType.T2S
            8 -> currentConversionType = ConversionType.T2TW
            9 -> currentConversionType = ConversionType.TW2S
            10 -> currentConversionType = ConversionType.HK2S
        }

        return if (convertType != 0) {
            ChineseConverter.convert(input, currentConversionType, context)
        } else {
            input
        }
    }
}
