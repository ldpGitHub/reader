package com.ldp.reader.utils

import com.tencent.mmkv.MMKV

class SharedPreUtils private constructor() {
    private val mmkv: MMKV = MMKV.mmkvWithID(SHARED_NAME)

    fun getString(key: String): String {
        return mmkv.decodeString(key, "")!!
    }

    fun putString(key: String, value: String?) {
        mmkv.encode(key, value)
    }

    fun putInt(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    fun putLong(key: String, value: Long) {
        mmkv.encode(key, value)
    }

    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }

    fun getInt(key: String, def: Int): Int {
        return mmkv.decodeInt(key, def)
    }

    fun getLong(key: String, def: Long): Long {
        return mmkv.decodeLong(key, def)
    }

    fun getBoolean(key: String, def: Boolean): Boolean {
        return mmkv.decodeBool(key, def)
    }

    companion object {
        private const val SHARED_NAME = "IReader_pref"

        @Volatile
        private var sInstance: SharedPreUtils? = null

        @JvmStatic
        fun getInstance(): SharedPreUtils {
            if (sInstance == null) {
                synchronized(SharedPreUtils::class.java) {
                    if (sInstance == null) {
                        sInstance = SharedPreUtils()
                    }
                }
            }
            return sInstance!!
        }
    }
}
