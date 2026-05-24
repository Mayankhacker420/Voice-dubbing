package .ui

import android.app.AlertDialog
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import .R
import .databinding.ActivityMainBinding
import .service.DubbingService
import .service.OverlayService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isRunning = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Settings.canDrawOverlays(this)) {
            requestMediaProjection()
        } else {
            showError("Overlay permission is required to show the dubbing bubble.")
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startDubbingService(result.resultCode, result.data!!)
        } else {
            showError("Media projection permission denied.")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLanguageSpinners()
        setupListeners()
        updateUI()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupLanguageSpinners() {
        val targetLanguages = arrayOf("Hindi", "English", "Japanese", "Spanish", "Arabic", "French")
        val targetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, targetLanguages)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTargetLanguage.adapter = targetAdapter

        val sourceLanguages = arrayOf("Auto Detect", "English", "Hindi", "Japanese", "Spanish", "Arabic", "French")
        val sourceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sourceLanguages)
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSourceLanguage.adapter = sourceAdapter

        binding.spinnerTargetLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val codes = listOf("hi", "en", "ja", "es", "ar", "fr")
                DubbingService.targetLanguage = codes[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        binding.spinnerSourceLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val codes = listOf("auto", "en", "hi", "ja", "es", "ar", "fr")
                DubbingService.sourceLanguage = codes[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
    }

    private fun setupListeners() {
        binding.btnToggle.setOnClickListener {
            if (isRunning) {
                stopDubbing()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.sliderVolume.addOnChangeListener { _, value, _ ->
            DubbingService.outputVolume = value / 100f
            binding.tvVolume.text = "${value.toInt()}%"
        }

        binding.switchOriginalAudio.setOnCheckedChangeListener { _, checked ->
            DubbingService.muteOriginal = checked
        }
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("Hindi Dubber needs permission to draw over other apps to show the dubbing bubble.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            requestMediaProjection()
        }
    }

    private fun requestMediaProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun startDubbingService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, DubbingService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        startForegroundService(serviceIntent)

        val overlayIntent = Intent(this, OverlayService::class.java)
        startService(overlayIntent)

        isRunning = true
        updateUI()
    }

    private fun stopDubbing() {
        stopService(Intent(this, DubbingService::class.java))
        stopService(Intent(this, OverlayService::class.java))
        isRunning = false
        updateUI()
    }

    private fun updateUI() {
        if (isRunning) {
            binding.btnToggle.text = "Stop Dubbing"
            binding.btnToggle.setBackgroundColor(getColor(R.color.stop_red))
            binding.tvStatus.text = "Dubbing Active"
            binding.statusIndicator.setBackgroundResource(R.drawable.indicator_active)
        } else {
            binding.btnToggle.text = "Start Dubbing"
            binding.btnToggle.setBackgroundColor(getColor(R.color.start_cyan))
            binding.tvStatus.text = "Ready"
            binding.statusIndicator.setBackgroundResource(R.drawable.indicator_idle)
        }
    }

    private fun showError(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
}
