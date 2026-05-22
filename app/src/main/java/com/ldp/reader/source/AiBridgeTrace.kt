package com.ldp.reader.source

import android.util.Log
import com.ldp.reader.BuildConfig

internal object AiBridgeTrace {
    fun state(name: String, key: String, value: String) {
        record("recordState", name, key, value)
    }

    fun event(name: String, key: String, value: String) {
        record("recordEvent", name, key, value)
    }

    fun fields(vararg fields: Pair<String, Any?>): String {
        return fields.joinToString("_") { (name, value) ->
            "${name}_${value.traceToken()}"
        }
    }

    private fun record(methodName: String, first: String, second: String, third: String) {
        val kind = if (methodName == "recordState") "state" else "event"
        Log.i(TAG, "kind=$kind name=$first key=${second.traceToken()} value=$third")
        if (!BuildConfig.DEBUG) return
        runCatching {
            val bridge = Class.forName("io.github.mobileaidev.aiappbridge.android.AiAppBridge")
            bridge
                .getMethod(methodName, String::class.java, String::class.java, String::class.java)
                .invoke(null, first, second, third)
        }
    }

    private fun Any?.traceToken(): String {
        return toString()
            .replace(Regex("""[\s=:/\\#]+"""), "_")
            .take(180)
    }

    private const val TAG = "ReaderTrace"
}
