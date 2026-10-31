package com.example.smsforwarder.delivery

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpSender {

    // کلاینت با timeout محدود و عدم دنبال‌کردن خودکار ریدایرکت طبق سند
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    sealed class Result {
        data class Success(val code: Int) : Result()
        data class Failure(val code: Int?, val message: String, val isRetryable: Boolean) : Result()
    }

    /**
     * ارسال POST JSON به URL مشخص‌شده
     */
    fun sendPost(url: String, jsonPayload: String): Result {
        return try {
            val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    in 200..299 -> Result.Success(response.code)
                    in 300..399 -> Result.Failure(response.code, "ریدایرکت مجاز نیست", isRetryable = false)
                    408, 429 -> Result.Failure(response.code, "محدودیت نرخ سرور یا تایم‌اوت", isRetryable = true)
                    in 400..499 -> Result.Failure(response.code, "خطای کلاینت/آدرس اشتباه", isRetryable = false)
                    in 500..599 -> Result.Failure(response.code, "خطای موقت سرور", isRetryable = true)
                    else -> Result.Failure(response.code, "پاسخ نامشخص", isRetryable = true)
                }
            }
        } catch (e: IOException) {
            // خطاهای شبکه یا قطعی اینترنت
            Result.Failure(null, e.message ?: "خطای اتصال شبکه", isRetryable = true)
        } catch (e: Exception) {
            Result.Failure(null, e.message ?: "خطای ناشناخته", isRetryable = false)
        }
    }
}