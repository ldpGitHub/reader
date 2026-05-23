package com.ldp.reader.sourceengine.content.v5

fun interface V5DiagnosticSink {
    fun record(line: String)

    companion object {
        val None = V5DiagnosticSink { }
    }
}

internal fun MutableList<String>.emitV5Diagnostic(
    sink: V5DiagnosticSink,
    stage: String,
    vararg fields: Pair<String, Any?>
) {
    val line = v5DiagnosticLine(stage, *fields)
    add(line)
    sink.record(line)
}

internal fun v5DiagnosticLine(stage: String, vararg fields: Pair<String, Any?>): String {
    return buildString {
        append(stage)
        fields.forEach { (name, value) ->
            append(' ')
            append(name)
            append('=')
            append(value?.toString().orEmpty().v5DiagnosticToken())
        }
    }
}

private fun String.v5DiagnosticToken(): String {
    return replace(Regex("""[\r\n\t]+"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
        .take(240)
}
