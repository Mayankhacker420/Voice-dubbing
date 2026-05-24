package .pipeline

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Translation pipeline using LibreTranslate (free, open-source).
 *
 * Free public endpoints:
 * - https://libretranslate.com (free tier, rate limited)
 * - https://translate.terraprint.co (community instance)
 * - https://lt.vern.cc (community instance)
 *
 * No API key required for basic usage on community instances.
 */
class TranslationPipeline {

    companion object {
        private const val TAG = "TranslationPipeline"
        private val FREE_ENDPOINTS = listOf(
            "https://translate.terraprint.co/translate",
            "https://lt.vern.cc/translate",
            "https://libretranslate.com/translate"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (text.isBlank()) return ""
        if (sourceLang == targetLang) return text

        for (endpoint in FREE_ENDPOINTS) {
            try {
                val result = callLibreTranslate(endpoint, text, sourceLang, targetLang)
                if (result.isNotBlank()) return result
            } catch (e: Exception) {
                Log.w(TAG, "Endpoint $endpoint failed: ${e.message}")
            }
        }
        return text
    }

    private fun callLibreTranslate(endpoint: String, text: String, source: String, target: String): String {
        val jsonBody = JSONObject().apply {
            put("q", text)
            put("source", if (source == "auto") "auto" else source)
            put("target", target)
            put("format", "text")
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val body = response.body?.string() ?: return ""
            val json = JSONObject(body)
            return json.optString("translatedText", "").trim()
        }
    }
}
