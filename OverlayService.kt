package .service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import .databinding.OverlayBubbleBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var binding: OverlayBubbleBinding
    private var params: WindowManager.LayoutParams? = null

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message") ?: return
            binding.tvStatus.text = message
            animatePulse()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlay()
        registerReceiver(statusReceiver, IntentFilter(DubbingService.ACTION_STATUS),
            RECEIVER_NOT_EXPORTED)
    }

    private fun setupOverlay() {
        binding = OverlayBubbleBinding.inflate(LayoutInflater.from(this))

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        windowManager.addView(binding.root, params)
        setupDragBehavior()
        setupControls()
    }

    private fun setupDragBehavior() {
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x; initialY = params!!.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                    params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(binding.root, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupControls() {
        binding.btnClose.setOnClickListener {
            stopSelf()
            stopService(Intent(this, DubbingService::class.java))
        }
        binding.btnToggle.setOnClickListener {
            DubbingService.isActive = !DubbingService.isActive
            binding.btnToggle.text = if (DubbingService.isActive) "Pause" else "Resume"
        }
    }

    private fun animatePulse() {
        binding.orbView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150)
            .withEndAction {
                binding.orbView.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(binding.root)
            unregisterReceiver(statusReceiver)
        } catch (_: Exception) {}
    }
}
