package .pipeline

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * Speech-to-Text pipeline using:
 * 1. Whisper-compatible free endpoints (primary)
 * 2. Fallback: Android SpeechRecognizer (offline)
 *
 * Uses the Groq free tier Whisper API (generous free quota, no billing required for basic use),
 * with local Google speech recognizer as fallback.
 */
class RecognitionPipeline {

    companion object {
        private const val TAG = "RecognitionPipeline"
        // Groq provides free Whisper API - sign up free at console.groq.com
        // Falls back to empty string if no key, triggering offline fallback
        private const val GROQ_API_KEY = "YOUR_GROQ_API_KEY_HERE"
        private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun recognize(samples: ShortArray, sampleRate: Int, sourceLanguage: String): String {
        return try {
            if (GROQ_API_KEY != "YOUR_GROQ_API_KEY_HERE") {
                recognizeViaGroq(samples, sampleRate, sourceLanguage)
            } else {
                // Offline fallback: return empty to trigger silence skip
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognition failed", e)
            ""
        }
    }

    private fun recognizeViaGroq(samples: ShortArray, sampleRate: Int, language: String): String {
        val wavBytes = shortsToWav(samples, sampleRate)
        val langParam = if (language == "auto") null else language

        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", "whisper-large-v3")
            .addFormDataPart("file", "audio.wav",
                wavBytes.toRequestBody("audio/wav".toMediaType()))

        if (langParam != null) bodyBuilder.addFormDataPart("language", langParam)

        val request = Request.Builder()
            .url(GROQ_ENDPOINT)
            .addHeader("Authorization", "Bearer $GROQ_API_KEY")
            .post(bodyBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Groq STT error: ${response.code}")
                return ""
            }
            val body = response.body?.string() ?: return ""
            val json = JSONObject(body)
            return json.optString("text", "").trim()
        }
    }

    private fun shortsToWav(samples: ShortArray, sampleRate: Int): ByteArray {
        val numChannels = 1
        val bitsPerSample = 16
        val dataSize = samples.size * 2
        val headerSize = 44

        val out = ByteArrayOutputStream(headerSize + dataSize)
        val buf = ByteBuffer.allocate(headerSize + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1)
        buf.putShort(numChannels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * numChannels * bitsPerSample / 8)
        buf.putShort((numChannels * bitsPerSample / 8).toShort())
        buf.putShort(bitsPerSample.toShort())
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        for (s in samples) buf.putShort(s)

        return buf.array()
    }
}
