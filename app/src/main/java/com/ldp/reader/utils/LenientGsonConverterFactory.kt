package com.ldp.reader.utils

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.lang.reflect.Type
import java.nio.charset.Charset

class LenientGsonConverterFactory private constructor(
    private val gson: Gson
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val adapter = gson.getAdapter(TypeToken.get(type))
        return LenientGsonResponseBodyConverter(gson, adapter)
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val adapter = gson.getAdapter(TypeToken.get(type))
        return LenientGsonRequestBodyConverter(gson, adapter)
    }

    inner class LenientGsonResponseBodyConverter<T>(
        private val gson: Gson,
        private val adapter: TypeAdapter<T>
    ) : Converter<ResponseBody, T> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): T {
            val jsonReader: JsonReader = gson.newJsonReader(value.charStream())
            jsonReader.isLenient = true
            try {
                return adapter.read(jsonReader)
            } finally {
                value.close()
            }
        }
    }

    inner class LenientGsonRequestBodyConverter<T>(
        private val gson: Gson,
        private val adapter: TypeAdapter<T>
    ) : Converter<T, RequestBody> {
        private val mediaType: MediaType? = "application/json; charset=UTF-8".toMediaTypeOrNull()
        private val utf8 = Charset.forName("UTF-8")

        @Throws(IOException::class)
        override fun convert(value: T): RequestBody {
            val buffer = Buffer()
            val writer: Writer = OutputStreamWriter(buffer.outputStream(), utf8)
            val jsonWriter: JsonWriter = gson.newJsonWriter(writer)
            jsonWriter.isLenient = true
            adapter.write(jsonWriter, value)
            jsonWriter.close()
            return buffer.readByteString().toRequestBody(mediaType)
        }
    }

    companion object {
        @JvmStatic
        fun create(): LenientGsonConverterFactory {
            return create(Gson())
        }

        @JvmStatic
        fun create(gson: Gson?): LenientGsonConverterFactory {
            if (gson == null) {
                throw NullPointerException("gson == null")
            }
            return LenientGsonConverterFactory(gson)
        }
    }
}
