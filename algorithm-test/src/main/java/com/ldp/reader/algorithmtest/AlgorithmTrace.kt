package com.ldp.reader.algorithmtest

import android.util.Log
import java.io.File

interface TraceRecorder {
    fun event(name: String, key: String, value: String)
    fun state(name: String, key: String, value: String)
    fun fields(vararg pairs: Pair<String, Any?>): String
    fun clear()
    fun snapshot(): List<String>
}

object AlgorithmTrace : TraceRecorder {
    private const val TAG = "AlgorithmTrace"
    private val records = ArrayList<String>()

    override fun event(name: String, key: String, value: String) {
        write("kind=event name=$name key=${key.token()} value=${value.token()}")
    }

    override fun state(name: String, key: String, value: String) {
        write("kind=state name=$name key=${key.token()} value=${value.token()}")
    }

    override fun fields(vararg pairs: Pair<String, Any?>): String {
        return pairs.joinToString("_") { (name, value) ->
            "${name}_${value?.toString().orEmpty().token()}"
        }
    }

    @Synchronized
    override fun clear() {
        records.clear()
    }

    @Synchronized
    override fun snapshot(): List<String> {
        return records.toList()
    }

    @Synchronized
    private fun write(message: String) {
        records.add(message)
        Log.i(TAG, message)
    }

    private fun String.token(): String {
        return replace(Regex("""[\s=:/\\#]+"""), "_").take(240)
    }
}

class LocalAlgorithmTrace(
    private val tagKey: String
) : TraceRecorder {
    private val records = ArrayList<String>()

    override fun event(name: String, key: String, value: String) {
        write("kind=event name=$name key=${key.token()} value=${value.token()}")
    }

    override fun state(name: String, key: String, value: String) {
        write("kind=state name=$name key=${key.token()} value=${value.token()}")
    }

    override fun fields(vararg pairs: Pair<String, Any?>): String {
        return pairs.joinToString("_") { (name, value) ->
            "${name}_${value?.toString().orEmpty().token()}"
        }
    }

    override fun clear() {
        records.clear()
    }

    override fun snapshot(): List<String> {
        return records.toList()
    }

    private fun write(message: String) {
        records.add(message)
        Log.i("AlgorithmTrace", "scope=${tagKey.token()} $message")
    }

    private fun String.token(): String {
        return replace(Regex("""[\s=:/\\#]+"""), "_").take(240)
    }
}

class FileAlgorithmTrace(
    private val tagKey: String,
    private val file: File
) : TraceRecorder {
    private val records = ArrayList<String>()

    override fun event(name: String, key: String, value: String) {
        write("kind=event name=$name key=${key.token()} value=${value.token()}")
    }

    override fun state(name: String, key: String, value: String) {
        write("kind=state name=$name key=${key.token()} value=${value.token()}")
    }

    override fun fields(vararg pairs: Pair<String, Any?>): String {
        return pairs.joinToString("_") { (name, value) ->
            "${name}_${value?.toString().orEmpty().token()}"
        }
    }

    @Synchronized
    override fun clear() {
        records.clear()
        file.parentFile?.mkdirs()
        file.writeText("")
    }

    @Synchronized
    override fun snapshot(): List<String> {
        return records.toList()
    }

    @Synchronized
    private fun write(message: String) {
        val line = "scope=${tagKey.token()} $message"
        records.add(line)
        file.parentFile?.mkdirs()
        file.appendText(line + "\n")
        Log.i("AlgorithmTrace", line)
    }

    private fun String.token(): String {
        return replace(Regex("""[\s=:/\\#]+"""), "_").take(240)
    }
}
