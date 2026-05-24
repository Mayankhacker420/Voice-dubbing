package .service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.LifecycleService
import .R
import .pipeline.RecognitionPipeline
import .pipeline.TranslationPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DubbingService : LifecycleService(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "DubbingService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "dubbing_channel"
        const val ACTION_STATUS = "com.dubber.STATUS"

        var targetLanguage = "hi"
        var sourceLanguage = "auto"
        var outputVolume = 1.0f
        var muteOriginal = false
        var isActive = false
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var tts: TextToSpeech? = null
    private var captureJob: Job? = null

    private val recognitionPipeline = RecognitionPipeline()
    private val translationPipeline = TranslationPipeline()

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, buildNotification())
        setupMediaProjection(resultCode, data)
        return START_STICKY
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)
        isActive = true
        startAudioCapture()
    }

    private fun startAudioCapture() {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding) * 4

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioEncoding)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord?.startRecording()

        captureJob = serviceScope.launch {
            processAudioStream(bufferSize)
        }
    }

    private suspend fun processAudioStream(bufferSize: Int) {
        val audioBuffer = ShortArray(bufferSize / 2)
        val accumulatedSamples = mutableListOf<Short>()
        val silenceThreshold = 300
        var silentFrames = 0
        val silenceFramesNeeded = 15

        while (isActive && coroutineContext.isActive) {
            val read = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: break
            if (read <= 0) continue

            val rms = audioBuffer.take(read).map { it * it.toLong() }.sum()
                .let { Math.sqrt(it.toDouble() / read).toInt() }

            if (rms > silenceThreshold) {
                silentFrames = 0
                accumulatedSamples.addAll(audioBuffer.take(read).toList())
            } else {
                silentFrames++
            }

            if (silentFrames >= silenceFramesNeeded && accumulatedSamples.size > sampleRate) {
                val samples = accumulatedSamples.toShortArray()
                accumulatedSamples.clear()
                silentFrames = 0

                processChunk(samples)
            }
        }
    }

    private suspend fun processChunk(samples: ShortArray) {
        try {
            val transcribed = recognitionPipeline.recognize(samples, sampleRate, sourceLanguage)
            if (transcribed.isBlank()) return

            Log.d(TAG, "Recognized: $transcribed")
            broadcastStatus("Recognized: $transcribed")

            val translated = translationPipeline.translate(transcribed, sourceLanguage, targetLanguage)
            if (translated.isBlank()) return

            Log.d(TAG, "Translated: $translated")
            broadcastStatus("Speaking: $translated")

            withContext(Dispatchers.Main) {
                speakText(translated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing chunk", e)
        }
    }

    private fun speakText(text: String) {
        val locale = when (targetLanguage) {
            "hi" -> Locale("hi", "IN")
            "ja" -> Locale.JAPAN
            "es" -> Locale("es", "ES")
            "ar" -> Locale("ar", "SA")
            "fr" -> Locale.FRANCE
            "de" -> Locale.GERMANY
            else -> Locale.US
        }
        tts?.language = locale
        tts?.setSpeechRate(1.0f)
        tts?.setPitch(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utterance_${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS initialized successfully")
        }
    }

    private fun broadcastStatus(message: String) {
        val intent = Intent(ACTION_STATUS).apply {
            putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Dubbing Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live voice dubbing is active"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Hindi Dubber")
            .setContentText("Live dubbing is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
    }
}
