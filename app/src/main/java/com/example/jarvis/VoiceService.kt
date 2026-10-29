package com.example.jarvis.voice

// ============================================================================
// J.A.R.V.I.S. BACKGROUND VOICE ENGINE (TITAN CORE - SAFE MODE V52.1)
// ============================================================================
// Architect: DrakoXNaeem
// FEATURES INJECTED:
// 1. SAFE MUTE PROTOCOL: Only mutes media, never touches Ringtone or Alarms!
// 2. AUTO-RECOVERY: Automatically recreates the recognizer if it gets stuck.
// 3. ANDROID 15 READY: Foreground microphone permissions and lifecycle safety.
// ============================================================================

import android.Manifest
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
import android.view.View
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
    // SYSTEM MANAGERS & HARDWARE DEPENDENCIES
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
    // ENGINE STATE & AUDIO VARIABLES
    // =========================================================
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isWakeModeActive = false
    private var isMuted = false
    private var isManualTrigger = false 
    private var retryCount = 0
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private enum class EngineState {
        OFFLINE, STANDBY, LISTENING, PROCESSING, SPEAKING, ERROR
    }
    private var currentState = EngineState.OFFLINE

    companion object {
        const val ACTION_ENABLE_WAKE = "com.example.jarvis.action.ENABLE_WAKE"
        const val ACTION_DISABLE_WAKE = "com.example.jarvis.action.DISABLE_WAKE"
        const val ACTION_VOICE_COMMAND = "com.example.jarvis.action.VOICE_COMMAND"
        const val ACTION_UPDATE_STATE = "com.example.jarvis.action.UPDATE_STATE"
        const val ACTION_VOICE_RESPONSE_FINISHED = "com.example.jarvis.action.VOICE_RESPONSE_FINISHED"
        
        const val EXTRA_COMMAND = "extra_command"
        const val EXTRA_STATE = "extra_state"

        private const val CHANNEL_ID = "JarvisSystemCore"
        private const val NOTIFICATION_ID = 4040
        private const val TAG = "JarvisUltimateEngine"
        private const val MAX_RETRIES = 5
    }

    // =========================================================
    // IPC: BROADCAST RECEIVER FOR UI SYNCHRONIZATION
    // =========================================================
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_STATE) {
                val stateName = intent.getStringExtra(EXTRA_STATE) ?: "IDLE"
                when (stateName) {
                    "THINKING" -> setEngineState(EngineState.PROCESSING)
                    "SPEAKING" -> setEngineState(EngineState.SPEAKING)
                    "ERROR" -> setEngineState(EngineState.ERROR)
                    "LISTENING" -> setEngineState(EngineState.LISTENING)
                    "IDLE" -> setEngineState(EngineState.STANDBY)
                }
            }
        }
    }

    // =========================================================
    // 1. LIFECYCLE & CORE INITIALIZATION
    // =========================================================
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Booting J.A.R.V.I.S. Core Engine...")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        acquireWakeLock()
        createNotificationChannel()
        
        if (hasAudioPermission()) {
            initializeSpeechRecognizer()
        } else {
            Log.e(TAG, "FATAL: Audio permission missing. Engine cannot start.")
        }

        val filter = IntentFilter(ACTION_UPDATE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasAudioPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_ENABLE_WAKE -> {
                isWakeModeActive = true
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, buildSystemNotification("Sensors Online & Monitoring"), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    startForeground(NOTIFICATION_ID, buildSystemNotification("Sensors Online & Monitoring"))
                }
                
                if (hasOverlayPermission()) mountFloatingUI()
                
                routeAudioToBluetoothIfAvailable()
                startContinuousListening()
            }
            ACTION_DISABLE_WAKE -> {
                isWakeModeActive = false
                stopContinuousListening()
                stopSelf()
            }
            ACTION_VOICE_RESPONSE_FINISHED -> {
                if (isWakeModeActive) {
                    mainHandler.postDelayed({ startContinuousListening() }, 800)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // =========================================================
    // SECURITY PERMISSION CHECKS
    // =========================================================
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }

    // =========================================================
    // 2. DRAGGABLE & CLICKABLE FLOATING UI
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
            text = "System Initializing..."
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
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
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
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)
                    if (deltaX > 10 || deltaY > 10) {
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
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                            triggerHapticFeedback(200)
                            stopSelf()
                        } else {
                            triggerHapticFeedback(80)
                            triggerManualListeningOverride()
                        }
                        lastClickTime = clickTime
                    }
                    true
                }
                else -> false
            }
        }
        try { windowManager.addView(floatingLayout, windowParams); setEngineState(EngineState.STANDBY) } catch (_: Exception) {}
    }

    private fun triggerManualListeningOverride() {
        isManualTrigger = true
        setEngineState(EngineState.LISTENING)
        floatingText?.text = "Yes Sir?"
        try {
            speechRecognizer?.cancel()
            isListening = false
            mainHandler.postDelayed({ startContinuousListening() }, 100)
        } catch (_: Exception) {}
    }

    // =========================================================
    // 3. CENTRALIZED STATE MANAGER
    // =========================================================
    private fun setEngineState(state: EngineState) {
        currentState = state
        mainHandler.post {
            val uiState = when (state) {
                EngineState.STANDBY -> Pair(OrbState.IDLE, "Standby")
                EngineState.LISTENING -> Pair(OrbState.LISTENING, "Listening...")
                EngineState.PROCESSING -> Pair(OrbState.THINKING, "Processing...")
                EngineState.SPEAKING -> Pair(OrbState.SPEAKING, "Transmitting")
                EngineState.ERROR -> Pair(OrbState.ERROR, "System Fault")
                EngineState.OFFLINE -> Pair(OrbState.IDLE, "Offline")
            }
            floatingOrb?.setOrbState(uiState.first)
            floatingText?.text = uiState.second
            floatingLayout?.alpha = if (state == EngineState.STANDBY || state == EngineState.OFFLINE) 0.6f else 1.0f
        }
    }

    // =========================================================
    // 4. SPEECH RECOGNITION & BACKGROUND LOOP
    // =========================================================
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
        } else stopSelf()
    }

    private fun resetSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            initializeSpeechRecognizer()
        } catch (_: Exception) {}
    }

    private fun startContinuousListening() {
        if (isListening || !isWakeModeActive || !hasAudioPermission()) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            requestAudioFocus()
            muteSystemBeeps() // Safe Mute Initiated
            speechRecognizer?.startListening(intent)
            isListening = true
            retryCount = 0
            if (currentState != EngineState.SPEAKING && currentState != EngineState.PROCESSING) {
                if (!isManualTrigger) setEngineState(EngineState.STANDBY)
            }
        } catch (e: Exception) {
            handleRecognizerCrash()
        }
    }

    private fun stopContinuousListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            restoreSystemBeeps()
            abandonAudioFocus()
            if (!isWakeModeActive) setEngineState(EngineState.OFFLINE)
        } catch (_: Exception) {}
    }

    private fun restartListeningWithDelay(delayMs: Long = 100, requiresReset: Boolean = false) {
        isListening = false
        if (!isWakeModeActive) return
        mainHandler.postDelayed({
            try {
                if (requiresReset) resetSpeechRecognizer() else speechRecognizer?.cancel()
                startContinuousListening()
            } catch (_: Exception) {}
        }, delayMs)
    }

    private fun handleRecognizerCrash() {
        if (retryCount < MAX_RETRIES) {
            retryCount++
            restartListeningWithDelay((retryCount * 1000).toLong(), requiresReset = true)
        } else {
            setEngineState(EngineState.ERROR)
            stopSelf()
        }
    }

    // =========================================================
    // 5. WAKE WORD & COMMAND PROCESSING
    // =========================================================
    private fun processRecognizedText(text: String) {
        val normalized = text.lowercase(Locale.getDefault()).trim()
        val isAuthorizedCommand = isManualTrigger || normalized.contains("jarvis") || normalized.contains("hey jarvis")

        if (isAuthorizedCommand) {
            isManualTrigger = false 
            if (!isNetworkAvailable()) {
                setEngineState(EngineState.ERROR)
                floatingText?.text = "Offline Mode"
                triggerHapticFeedback(500)
                restartListeningWithDelay(2000)
                return
            }
            triggerHapticFeedback(100) 
            stopContinuousListening()
            setEngineState(EngineState.PROCESSING)
            
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_VOICE_COMMAND
                putExtra(EXTRA_COMMAND, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(launchIntent)
        } else {
            restartListeningWithDelay(200)
        }
    }

    // =========================================================
    // 6. RECOGNIZER CALLBACKS
    // =========================================================
    override fun onReadyForSpeech(params: Bundle?) {
        mainHandler.postDelayed({ restoreSystemBeeps() }, 400) // Unmute ASAP
    }
    
    override fun onBeginningOfSpeech() { 
        if (currentState != EngineState.PROCESSING && isManualTrigger) setEngineState(EngineState.LISTENING) 
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        if (currentState == EngineState.LISTENING || isManualTrigger) {
            val scaleFactor = 1.0f + (rmsdB / 20f).coerceIn(0f, 0.4f)
            floatingOrb?.animate()?.scaleX(scaleFactor)?.scaleY(scaleFactor)?.setDuration(50)?.start()
        }
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {}
    
    override fun onEndOfSpeech() { 
        isListening = false 
        floatingOrb?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
    }
    
    override fun onError(error: Int) {
        isListening = false
        floatingOrb?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
        restoreSystemBeeps() // Failsafe unmute
        
        when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> restartListeningWithDelay(500)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> restartListeningWithDelay(1500, requiresReset = true)
            else -> handleRecognizerCrash()
        }
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        floatingOrb?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150)?.start()
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) processRecognizedText(matches[0]) else restartListeningWithDelay(200)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0].lowercase(Locale.getDefault())
            if (partial.contains("jarvis") && !isManualTrigger) {
                speechRecognizer?.stopListening()
                processRecognizedText(partial)
            }
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {}

    // =========================================================
    // 7. SAFE AUDIO MUTING (CRITICAL FIX)
    // =========================================================
    private fun muteSystemBeeps() {
        if (isMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // NEVER ADD STREAM_ALARM OR STREAM_RING HERE!
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                isMuted = true
            }
        } catch (_: Exception) {}
    }

    private fun restoreSystemBeeps() {
        if (!isMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                isMuted = false
            }
        } catch (_: Exception) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        restoreSystemBeeps()
    }

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

    // =========================================================
    // 8. ROUTING, NOTIFICATIONS & SHUTDOWN
    // =========================================================
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focus ->
                    if (focus == AudioManager.AUDIOFOCUS_LOSS) stopContinuousListening()
                    else if (focus == AudioManager.AUDIOFOCUS_GAIN) startContinuousListening()
                }.build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    private fun routeAudioToBluetoothIfAvailable() {
        if (audioManager.isBluetoothScoAvailableOffCall) audioManager.startBluetoothSco()
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JarvisCore::CpuLock")
            wakeLock?.acquire(12 * 60 * 60 * 1000L) 
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS Subsystem", NotificationManager.IMPORTANCE_MIN)
            channel.description = "Persistent Voice Detection Core"
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
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setColor(Color.parseColor("#00E5FF"))
            .build()
    }

    override fun onDestroy() {
        isWakeModeActive = false
        stopContinuousListening()
        restoreSystemBeeps() 
        releaseWakeLock()
        if (audioManager.isBluetoothScoOn) audioManager.stopBluetoothSco()
        try {
            unregisterReceiver(stateReceiver)
            if (floatingLayout != null) {
                windowManager.removeView(floatingLayout)
                floatingLayout = null
            }
        } catch (_: Exception) {}
        speechRecognizer?.destroy()
        speechRecognizer = null
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
