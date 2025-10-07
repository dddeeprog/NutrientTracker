package com.example.nutrienttrackerv1.ai

import android.util.Base64
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// Retrofit 动态 URL
interface AiService {
    @POST
    suspend fun chat(
        @Url endpoint: String,
        @Header("Authorization") auth: String,
        @Body body: ChatRequest
    ): ChatResponse
}

private val gson = Gson()

fun buildRetrofit(): Retrofit {
    val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    val client = OkHttpClient.Builder()
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        // 附带通用头，防止个别网关挑剔
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(log)
        .build()

    // baseUrl 只占位，实际用 @Url
    return Retrofit.Builder()
        .baseUrl("https://api.moonshot.cn/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

// API 28+ 可用的压缩：把长边限制到 1280，JPEG 质量 85
fun compressImage(context: android.content.Context, uri: android.net.Uri,
                  maxW: Int = 1280, maxH: Int = 1280, quality: Int = 85): ByteArray {
    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
    var bmp = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
        decoder.isMutableRequired = false
    }
    val ratio = kotlin.math.min(maxW / bmp.width.toFloat(), maxH / bmp.height.toFloat()).coerceAtMost(1f)
    if (ratio < 1f) {
        val nw = (bmp.width * ratio).toInt()
        val nh = (bmp.height * ratio).toInt()
        bmp = android.graphics.Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }
    val out = java.io.ByteArrayOutputStream()
    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}


// 把图片 bytes 变 data:url
fun bytesToDataUrl(bytes: ByteArray, mime: String = "image/jpeg"): String {
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:$mime;base64,$b64"
}

fun buildChatRequest(model: String, dataUrl: String, userExtra: String): ChatRequest {
    val system = ROLE_PROMPT + "\n\n" + AI_JSON_REQUIREMENT
    val user = (userExtra.ifBlank { "请识别图片中的食物，估算克数并输出结构化 JSON" }) + "\n" + AI_JSON_REQUIREMENT
    val msgs = listOf(
        Msg(role = "system", content = listOf(MsgContent(type = "text", text = system))),
        Msg(
            role = "user",
            content = listOf(
                MsgContent(type = "text", text = user),
                MsgContent(type = "image_url", image_url = ImageUrl(url = dataUrl))
            )
        )
    )
    return ChatRequest(model = model, temperature = 0, messages = msgs)
}

// 把 AI 返回的字符串解析为 AiParsed
fun parseAiJson(text: String): AiParsed {
    // 兼容模型可能带非 JSON 文本的情况
    val json = text.trim().let { s ->
        if (s.startsWith("{") && s.endsWith("}")) s
        else s.substringAfter("{", missingDelimiterValue = "{")
            .substringBeforeLast("}", missingDelimiterValue = "}") + "}"
    }
    return try {
        gson.fromJson(json, AiParsed::class.java)
    } catch (_: Exception) {
        AiParsed()
    }
}
