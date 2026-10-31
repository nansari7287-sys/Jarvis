package com.example.jarvis.voice

// ============================================================================
// J.A.R.V.I.S. TITAN VOICE ENGINE - SEAMLESS AI CALL MODE (V600.0)
// ============================================================================
// Architect: DrakoXNaeem
// FEATURES INJECTED:
// 1. LIVE CALL MODE: No need to say "Hey Jarvis". Everything spoken goes to AI.
// 2. ZERO BEEP (SAFE MUTE): Kills the Google "Tudum" without touching Alarms/Ringtones.
// 3. CONTINUOUS LOOP: Instantly restarts listening if you pause.
// 4. FLOATING UI: Draggable Orb, Double-Tap to Hang Up.
// ============================================================================

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.jarvis.MainActivity
import com.example.jarvis.ui.JarvisOrbView
import com.example.jarvis.ui.OrbState
import java.util.Locale
import kotlin.math.abs

class VoiceService : Service(), RecognitionListener {

    // =========================================================
    // SYSTEM MANAGERS & HARDWARE
    // =========================================================
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var windowManager: WindowManager
    private lateinit var connectivityManager: ConnectivityManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    // =========================================================
    // FLOATING UI ELEMENTS
    // =========================================================
    private var floatingLayout: LinearLayout? = null
    private var floatingOrb: JarvisOrbView? = null
    private var floatingText: TextView? = null
    private lateinit var windowParams: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var lastClickTime: Long = 0
    private val DOUBLE_CLICK_TIME_DELTA: Long = 300 

    // =========================================================
    // ENGINE STATE
    // =========================================================
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCallActive = false
    private var isMuted = false
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        const val ACTION_START_CALL = "com.example.jarvis.action.START_CALL"
        const val ACTION_END_CALL = "com.example.jarvis.action.END_CALL"
        const val ACTION_VOICE_COMMAND = "com.example.jarvis.action.VOICE_COMMAND"
        const val ACTION_RESUME_LISTENING = "com.example.jarvis.action.RESUME_LISTENING"
        
        const val EXTRA_COMMAND = "extra_command"

        private const val CHANNEL_ID = "JarvisCallEngine"
        private const val NOTIFICATION_ID = 5050
        private const val TAG = "JarvisUltimateCall"
    }

    // Broadcast Receiver to let MainActivity tell the Service to start listening again after AI speaks
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RESUME_LISTENING && isCallActive) {
                updateUI(OrbState.IDLE, "Listening...")
                restartListeningWithDelay(100)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Booting J.A.R.V.I.S. Live Call Engine...")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        acquireWakeLock()
        createNotificationChannel()
        setupSpeechRecognizer()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, IntentFilter(ACTION_RESUME_LISTENING), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, IntentFilter(ACTION_RESUME_LISTENING))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> establishCallSession()
            ACTION_END_CALL -> endCallSession()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // =========================================================
    // 1. LIVE CALL SESSION MANAGEMENT
    // =========================================================
    private fun establishCallSession() {
        if (isCallActive) return
        isCallActive = true

        val notification = buildSystemNotification("Live Call Connected")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (Settings.canDrawOverlays(this)) mountFloatingUI()
        
        requestAudioFocus()
        startContinuousListening()
    }

    private fun endCallSession() {
        if (!isCallActive) return
        isCallActive = false
        
        stopContinuousListening()
        removeFloatingUI()
        abandonAudioFocus()
        stopForeground(true)
        stopSelf()
    }

    // =========================================================
    // 2. SEAMLESS SPEECH RECOGNITION & ZERO BEEP
    // =========================================================
    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(this)
    }

    private fun startContinuousListening() {
        if (!isCallActive) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        muteSystemBeeps() // 🔴 SAFE MUTE: Kills the beep
        
        try {
            speechRecognizer?.startListening(intent)
            updateUI(OrbState.LISTENING, "Listening...")
        } catch (e: Exception) {
            restartListeningWithDelay(500)
        }
    }

    private fun stopContinuousListening() {
        unmuteSystemBeeps()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
    }

    private fun restartListeningWithDelay(delayMs: Long = 100) {
        if (!isCallActive) return
        mainHandler.postDelayed({
            try {
                speechRecognizer?.cancel()
                startContinuousListening()
            } catch (_: Exception) {}
        }, delayMs)
    }

    // =========================================================
    // 3. RECOGNIZER CALLBACKS (FULL DUPLEX FEEL)
    // =========================================================
    override fun onReadyForSpeech(params: Bundle?) {
        // Unmute right after the beep should have played, so AI TTS isn't blocked
        mainHandler.postDelayed({ unmuteSystemBeeps() }, 400)
    }
    
    override fun onBeginningOfSpeech() {}
    
    override fun onRmsChanged(rmsdB: Float) {
        val scaleFactor = 1.0f + (rmsdB / 15f).coerceIn(0f, 0.4f)
        floatingOrb?.animate()?.scaleX(scaleFactor)?.scaleY(scaleFactor)?.setDuration(50)?.start()
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {}
    
    override fun onEndOfSpeech() { 
        floatingOrb?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
        updateUI(OrbState.THINKING, "Processing...")
    }
    
    override fun onError(error: Int) {
        unmuteSystemBeeps()
        floatingOrb?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
        
        // If silence, restart immediately to keep the call alive
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            restartListeningWithDelay(100)
        } else {
            restartListeningWithDelay(1000)
        }
    }

    override fun onResults(results: Bundle?) {
        unmuteSystemBeeps()
        floatingOrb?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
        
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0].trim()
            Log.i(TAG, "Transcribed: $spokenText")
            
            if (!isNetworkAvailable()) {
                updateUI(OrbState.ERROR, "Offline")
                triggerHapticFeedback(500)
                restartListeningWithDelay(2000)
                return
            }

            // Route speech to AI Brain
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_VOICE_COMMAND
                putExtra(EXTRA_COMMAND, spokenText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launchIntent)
            
            updateUI(OrbState.SPEAKING, "Transmitting...")
            // The mic pauses here. MainActivity must send ACTION_RESUME_LISTENING when AI is done talking.
        } else {
            restartListeningWithDelay(200)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    // =========================================================
    // 4. SAFE AUDIO MUTING (NEVER TOUCHES RINGTONE)
    // =========================================================
    private fun muteSystemBeeps() {
        if (isMuted) return
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            isMuted = true
        } catch (_: Exception) {}
    }

    private fun unmuteSystemBeeps() {
        if (!isMuted) return
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            isMuted = false
        } catch (_: Exception) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        unmuteSystemBeeps() // Failsafe
    }

    // =========================================================
    // 5. FLOATING UI (DRAGGABLE ORB)
    // =========================================================
    @SuppressLint("ClickableViewAccessibility")
    private fun mountFloatingUI() {
        if (floatingLayout != null) return

        floatingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }

        floatingOrb = JarvisOrbView(this).apply { setOrbState(OrbState.IDLE) }

        floatingText = TextView(this).apply {
            text = "Call Active"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10f
            gravity = Gravity.CENTER
            setShadowLayer(15f, 0f, 0f, Color.parseColor("#00E5FF"))
            setPadding(0, 8, 0, 0)
        }

        floatingLayout?.addView(floatingOrb, LinearLayout.LayoutParams(160, 160))
        floatingLayout?.addView(floatingText, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50; y = 250
        }

        floatingLayout?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - initialTouchX) > 10 || abs(event.rawY - initialTouchY) > 10) {
                        isDragging = true
                        windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        windowParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingLayout, windowParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).setInterpolator(OvershootInterpolator()).start()
                    if (!isDragging) {
                        val clickTime = System.currentTimeMillis()
                        // DOUBLE TAP TO HANG UP
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                            triggerHapticFeedback(200)
                            endCallSession()
                        }
                        lastClickTime = clickTime
                    }
                    true
                }
                else -> false
            }
        }
        try { windowManager.addView(floatingLayout, windowParams) } catch (_: Exception) {}
    }

    private fun removeFloatingUI() {
        try {
            floatingLayout?.let { windowManager.removeView(it) }
            floatingLayout = null
        } catch (_: Exception) {}
    }

    private fun updateUI(state: OrbState, text: String) {
        mainHandler.post {
            floatingOrb?.setOrbState(state)
            floatingText?.text = text
        }
    }

    // =========================================================
    // 6. UTILITIES (POWER, NETWORK, NOTIFICATIONS)
    // =========================================================
    private fun triggerHapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(true).build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JarvisCore::CallLock")
            wakeLock?.acquire(12 * 60 * 60 * 1000L) 
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS Call Engine", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Persistent Voice Call"
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildSystemNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Core Online")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentIntent(pIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setColor(Color.parseColor("#00E5FF"))
            .build()
    }

    override fun onDestroy() {
        endCallSession()
        unmuteSystemBeeps() // Absolute Failsafe
        releaseWakeLock()
        
        try {
            unregisterReceiver(commandReceiver)
        } catch (_: Exception) {}

        speechRecognizer?.destroy()
        speechRecognizer = null
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
