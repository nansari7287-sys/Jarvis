package com.example.jarvis.voice

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
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity
import com.example.jarvis.ui.JarvisOrbView
import com.example.jarvis.ui.OrbState
import java.util.Locale

/**
 * ============================================================================
 * J.A.R.V.I.S. BACKGROUND VOICE ENGINE (ULTIMATE GOD MODE - V3.0)
 * ============================================================================
 * 
 * Production-ready Background Service featuring:
 * - Draggable System Alert Window (Floating UI)
 * - Network Awareness (Prevents API crashes)
 * - Bluetooth SCO Audio Routing (For Earbuds/Headphones)
 * - Aggressive Audio Multi-Stream Muting
 * - Memory-safe Speech Recognition Loop
 */
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
    // FLOATING UI ELEMENTS (THE ORB)
    // =========================================================
    private var floatingLayout: LinearLayout? = null
    private var floatingOrb: JarvisOrbView? = null
    private var floatingText: TextView? = null
    private lateinit var windowParams: WindowManager.LayoutParams

    // Drag functionality variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // =========================================================
    // ENGINE STATE VARIABLES
    // =========================================================
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isWakeModeActive = false
    private var isMuted = false
    private var retryCount = 0
    private var audioFocusRequest: AudioFocusRequest? = null
    
    // Core Engine State Enum
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
    // IPC: BROADCAST RECEIVER FOR UI SYNC
    // =========================================================
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_STATE) {
                val stateName = intent.getStringExtra(EXTRA_STATE) ?: "IDLE"
                when (stateName) {
                    "THINKING" -> setEngineState(EngineState.PROCESSING)
                    "SPEAKING" -> setEngineState(EngineState.SPEAKING)
                    "ERROR" -> setEngineState(EngineState.ERROR)
                    "IDLE" -> setEngineState(EngineState.STANDBY)
                }
            }
        }
    }

    // =========================================================
    // 1. LIFECYCLE & INITIALIZATION
    // =========================================================
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Booting J.A.R.V.I.S. Core Engine...")

        // Initialize System Services
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        acquireWakeLock()
        createNotificationChannel()
        initializeSpeechRecognizer()
        
        // Register Broadcast Receiver safely
        val filter = IntentFilter(ACTION_UPDATE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE_WAKE -> {
                Log.i(TAG, "Engaging Wake Protocol...")
                isWakeModeActive = true
                startForeground(NOTIFICATION_ID, buildSystemNotification("Sensors Online & Monitoring"))
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    mountFloatingUI()
                } else {
                    Log.w(TAG, "SYSTEM_ALERT_WINDOW permission denied. Orb will not be visible.")
                }
                
                routeAudioToBluetoothIfAvailable()
                startContinuousListening()
            }
            ACTION_DISABLE_WAKE -> {
                Log.i(TAG, "Disengaging Wake Protocol...")
                isWakeModeActive = false
                stopContinuousListening()
                stopSelf()
            }
            ACTION_VOICE_RESPONSE_FINISHED -> {
                Log.i(TAG, "TTS Output complete. Restoring acoustic sensors.")
                if (isWakeModeActive) {
                    mainHandler.postDelayed({ startContinuousListening() }, 800)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // =========================================================
    // 2. DRAGGABLE FLOATING UI (SYSTEM ALERT WINDOW)
    // =========================================================
    @SuppressLint("ClickableViewAccessibility")
    private fun mountFloatingUI() {
        if (floatingLayout != null) return

        // Main Container
        floatingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }

        // The Glowing Orb
        floatingOrb = JarvisOrbView(this).apply { 
            setOrbState(OrbState.IDLE) 
        }

        // The Status Text
        floatingText = TextView(this).apply {
            text = "System Initializing..."
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            gravity = Gravity.CENTER
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#00E5FF"))
        }

        floatingLayout?.addView(floatingOrb, LinearLayout.LayoutParams(130, 130))
        floatingLayout?.addView(floatingText, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        // Window Parameters
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Implement Drag & Drop Logic
        floatingLayout?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    
                    // Visual feedback on touch
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    windowParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingLayout, windowParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(floatingLayout, windowParams)
            setEngineState(EngineState.STANDBY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mount Floating UI: \${e.message}")
        }
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
                EngineState.SPEAKING -> Pair(OrbState.SPEAKING, "System Active")
                EngineState.ERROR -> Pair(OrbState.ERROR, "Network Error")
                EngineState.OFFLINE -> Pair(OrbState.IDLE, "Offline")
            }

            floatingOrb?.setOrbState(uiState.first)
            floatingText?.text = uiState.second

            // Handle layout animations
            if (state == EngineState.STANDBY || state == EngineState.OFFLINE) {
                floatingLayout?.alpha = 0.6f
            } else {
                floatingLayout?.alpha = 1.0f
            }
        }
    }

    // =========================================================
    // 4. SPEECH RECOGNITION & BACKGROUND LOOP
    // =========================================================
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
        } else {
            Log.e(TAG, "Speech Recognition Framework missing on this device.")
            stopSelf()
        }
    }

    private fun startContinuousListening() {
        if (isListening || !isWakeModeActive) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Optimize silences for faster processing
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
        }

        try {
            requestAudioFocus()
            muteSystemBeeps() // KILL THE GOOGLE BEEP
            
            speechRecognizer?.startListening(intent)
            isListening = true
            retryCount = 0
            
            if (currentState != EngineState.SPEAKING && currentState != EngineState.PROCESSING) {
                setEngineState(EngineState.STANDBY)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer: \${e.message}")
        }
    }

    private fun restartListeningWithDelay(delayMs: Long = 100) {
        isListening = false
        if (!isWakeModeActive) return
        mainHandler.postDelayed({
            try {
                speechRecognizer?.cancel()
                startContinuousListening()
            } catch (e: Exception) {
                Log.e(TAG, "Restart failed.")
            }
        }, delayMs)
    }

    private fun handleRecognizerCrash() {
        if (retryCount < MAX_RETRIES) {
            retryCount++
            Log.w(TAG, "Recognizer crash. Backoff retry attempt \$retryCount")
            restartListeningWithDelay((retryCount * 1000).toLong())
        } else {
            Log.e(TAG, "FATAL: Recognizer max retries reached.")
            setEngineState(EngineState.ERROR)
            stopSelf()
        }
    }

    // =========================================================
    // 5. WAKE WORD & COMMAND PROCESSING
    // =========================================================
    private fun processRecognizedText(text: String) {
        val normalized = text.lowercase(Locale.getDefault()).trim()
        Log.i(TAG, "Acoustic Input: \$normalized")

        val isWakeWord = normalized.contains("jarvis") || 
                         normalized.contains("hey jarvis") || 
                         normalized.contains("ok jarvis") ||
                         normalized.contains("wake up")

        if (isWakeWord) {
            // Check Network Before Firing Intent (Prevents the technical error read-out)
            if (!isNetworkAvailable()) {
                setEngineState(EngineState.ERROR)
                floatingText?.text = "No Internet!"
                restartListeningWithDelay(2000)
                return
            }

            Log.i(TAG, "Wake Word Detected! Routing to Main Engine...")
            stopContinuousListening()
            setEngineState(EngineState.PROCESSING)
            
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_VOICE_COMMAND
                putExtra(EXTRA_COMMAND, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(launchIntent)
        } else {
            // False alarm, ignore silently
            restartListeningWithDelay(50)
        }
    }

    // =========================================================
    // 6. RECOGNIZER CALLBACKS
    // =========================================================
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() { 
        if (currentState != EngineState.PROCESSING) setEngineState(EngineState.LISTENING) 
    }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { isListening = false }
    
    override fun onError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> {
                restartListeningWithDelay(50) // Normal silence
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                restartListeningWithDelay(1500)
            }
            else -> handleRecognizerCrash()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            processRecognizedText(matches[0])
        } else {
            restartListeningWithDelay(50)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0].lowercase(Locale.getDefault())
            // Aggressive early wake word detection
            if (partial.contains("jarvis")) {
                speechRecognizer?.stopListening()
                processRecognizedText(partial)
            }
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {}

    // =========================================================
    // 7. AGGRESSIVE AUDIO MUTING (SYSTEM BEEP KILLER)
    // =========================================================
    private fun muteSystemBeeps() {
        if (isMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val streamsToMute = intArrayOf(
                    AudioManager.STREAM_MUSIC, 
                    AudioManager.STREAM_SYSTEM, 
                    AudioManager.STREAM_NOTIFICATION, 
                    AudioManager.STREAM_ALARM,
                    AudioManager.STREAM_RING
                )
                for (stream in streamsToMute) {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                }
                isMuted = true
            }
        } catch (e: Exception) { Log.e(TAG, "Mute protocol failed.") }
    }

    private fun restoreSystemBeeps() {
        if (!isMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val streamsToRestore = intArrayOf(
                    AudioManager.STREAM_MUSIC, 
                    AudioManager.STREAM_SYSTEM, 
                    AudioManager.STREAM_NOTIFICATION, 
                    AudioManager.STREAM_ALARM,
                    AudioManager.STREAM_RING
                )
                for (stream in streamsToRestore) {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                }
                isMuted = false
            }
        } catch (e: Exception) { Log.e(TAG, "Restore protocol failed.") }
    }

    // =========================================================
    // 8. HARDWARE ROUTING & CONNECTIVITY
    // =========================================================
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) stopContinuousListening()
                    else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) startContinuousListening()
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
        if (audioManager.isBluetoothScoAvailableOffCall) {
            audioManager.startBluetoothSco()
            Log.i(TAG, "Bluetooth Audio Routing Activated.")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // =========================================================
    // 9. POWER WAKELOCK & FOREGROUND NOTIFICATION
    // =========================================================
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JarvisCore::CpuLock")
            wakeLock?.acquire(12 * 60 * 60 * 1000L) // Max 12 hours
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

    // =========================================================
    // 10. SYSTEM SHUTDOWN PROTOCOL
    // =========================================================
    override fun onDestroy() {
        Log.i(TAG, "Initiating Core Shutdown Sequence...")
        isWakeModeActive = false
        
        stopContinuousListening()
        releaseWakeLock()
        
        if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
        }

        try {
            unregisterReceiver(stateReceiver)
            if (floatingLayout != null) {
                windowManager.removeView(floatingLayout)
                floatingLayout = null
            }
        } catch (e: Exception) { Log.e(TAG, "Cleanup warning: \${e.message}") }

        speechRecognizer?.destroy()
        speechRecognizer = null
        mainHandler.removeCallbacksAndMessages(null)
        
        Log.i(TAG, "Shutdown Complete. System Offline.")
        super.onDestroy()
    }
}