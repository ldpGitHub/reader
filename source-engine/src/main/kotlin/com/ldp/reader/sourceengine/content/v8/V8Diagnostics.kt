package com.ldp.reader.sourceengine.content.v8

fun interface V8DiagnosticSink {
    fun record(line: String)

    companion object {
        val None = V8DiagnosticSink { }
    }
}

internal fun v8DiagnosticLine(stage: String, vararg fields: Pair<String, Any?>): String {
    return buildString {
        append(stage)
        fields.forEach { (name, value) ->
            append(' ')
            append(name)
            append('=')
            append(value?.toString().orEmpty().v8DiagnosticToken())
        }
    }
}

private fun String.v8DiagnosticToken(): String {
    return replace(Regex("""[\r\n\t]+"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
        .take(240)
}
