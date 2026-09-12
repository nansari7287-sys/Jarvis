package com.example.jarvis.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity
import java.util.Locale

/**
 * ============================================================================
 * J.A.R.V.I.S. BACKGROUND VOICE ENGINE (ULTRA-PRO EDITION)
 * ============================================================================
 * 
 * Ye service completely standalone background entity ki tarah run karti hai.
 * It features:
 * - CPU Partial WakeLocks (To bypass Doze Mode)
 * - Intelligent Audio Focus Handling
 * - Seamless Continuous Listening Loop
 * - System Beep Muting (Zero Disturbance)
 * - Exponential Backoff on Errors
 */
class VoiceService : Service(), RecognitionListener {

    // =========================================================
    // CORE DEPENDENCIES
    // =========================================================

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    // =========================================================
    // THREADING & HANDLERS
    // =========================================================

    private val mainHandler = Handler(Looper.getMainLooper())

    // =========================================================
    // STATE VARIABLES
    // =========================================================

    private var isListening = false
    private var isWakeModeActive = false
    private var isMuted = false
    private var originalVolume = 0
    private var retryCount = 0

    // =========================================================
    // AUDIO FOCUS VARIABLES
    // =========================================================

    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {

        // =====================================================
        // INTENT ACTIONS
        // =====================================================

        const val ACTION_ENABLE_WAKE = 
            "com.example.jarvis.action.ENABLE_WAKE"
            
        const val ACTION_DISABLE_WAKE = 
            "com.example.jarvis.action.DISABLE_WAKE"
            
        const val ACTION_VOICE_COMMAND = 
            "com.example.jarvis.action.VOICE_COMMAND"
            
        const val ACTION_VOICE_RESPONSE_FINISHED = 
            "com.example.jarvis.action.VOICE_RESPONSE_FINISHED"
            
        const val EXTRA_COMMAND = 
            "extra_command"

        // =====================================================
        // SYSTEM CONSTANTS
        // =====================================================

        private const val CHANNEL_ID = 
            "JarvisVoiceSystemChannel"
            
        private const val NOTIFICATION_ID = 
            4040
            
        private const val TAG = 
            "JarvisVoiceEngine"
            
        private const val WAKELOCK_TAG = 
            "JarvisApp::VoiceWakeLock"
            
        private const val MAX_RETRIES = 
            5
    }

    // =========================================================
    // SERVICE LIFECYCLE (CREATION)
    // =========================================================

    override fun onCreate() {
        
        super.onCreate()
        
        Log.d(TAG, "Initializing J.A.R.V.I.S. Voice Engine...")

        // System Services
        audioManager = 
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
        powerManager = 
            getSystemService(Context.POWER_SERVICE) as PowerManager

        // Setup Wakelock
        acquireWakeLock()

        // Setup Notification
        createNotificationChannel()

        // Setup AI Recognizer
        initializeSpeechRecognizer()
        
        Log.d(TAG, "Engine Initialization Complete.")
    }

    // =========================================================
    // SERVICE LIFECYCLE (START COMMAND)
    // =========================================================

    override fun onStartCommand(
        intent: Intent?, 
        flags: Int, 
        startId: Int
    ): Int {

        val action = intent?.action
        
        Log.d(TAG, "Received Command: \$action")

        when (action) {
            
            ACTION_ENABLE_WAKE -> {
                
                isWakeModeActive = true
                
                startForeground(
                    NOTIFICATION_ID, 
                    buildDynamicNotification("Awaiting Voice Protocol...")
                )
                
                startContinuousListening()
            }
            
            ACTION_DISABLE_WAKE -> {
                
                isWakeModeActive = false
                stopContinuousListening()
                stopSelf()
            }
            
            ACTION_VOICE_RESPONSE_FINISHED -> {
                
                // JARVIS finished speaking, resume listening
                if (isWakeModeActive) {
                    
                    Log.d(TAG, "JARVIS finished output. Resuming Wake Engine.")
                    
                    mainHandler.postDelayed({
                        startContinuousListening()
                    }, 500)
                }
            }
        }
        
        // Tells Android to recreate service if killed due to low memory
        return START_STICKY
    }

    // =========================================================
    // BINDING (NOT USED IN STANDALONE ENGINE)
    // =========================================================

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // =========================================================
    // POWER MANAGEMENT (WAKELOCKS)
    // =========================================================

    private fun acquireWakeLock() {
        
        if (wakeLock == null) {
            
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            )
            
            // Acquire wake lock for a maximum of 12 hours to prevent permanent drain
            wakeLock?.acquire(12 * 60 * 60 * 1000L) 
            
            Log.d(TAG, "CPU Partial WakeLock Acquired.")
        }
    }

    private fun releaseWakeLock() {
        
        wakeLock?.let {
            
            if (it.isHeld) {
                
                it.release()
                Log.d(TAG, "CPU Partial WakeLock Released.")
            }
        }
        
        wakeLock = null
    }

    // =========================================================
    // SPEECH RECOGNIZER ENGINE SETUP
    // =========================================================

    private fun initializeSpeechRecognizer() {
        
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            
            speechRecognizer = 
                SpeechRecognizer.createSpeechRecognizer(this)
                
            speechRecognizer?.setRecognitionListener(this)
            
            Log.d(TAG, "Acoustic Model Linked Successfully.")
            
        } else {
            
            Log.e(TAG, "CRITICAL ERROR: Speech Recognition not available.")
            stopSelf()
        }
    }

    // =========================================================
    // LISTENING LIFECYCLE CONTROLS
    // =========================================================

    private fun startContinuousListening() {
        
        if (isListening || !isWakeModeActive) {
            return
        }

        val recognizerIntent = 
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE, 
                    Locale.getDefault().toString()
                )
                
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS, 
                    true
                )
                
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS, 
                    1
                )
                
                // Optimizations for faster detection
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1500
                )
                
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1500
                )
            }

        try {
            
            requestAudioFocus()
            muteSystemBeep()
            
            speechRecognizer?.startListening(
                recognizerIntent
            )
            
            isListening = true
            retryCount = 0
            
            updateNotification("Acoustic Sensors Active...")
            
            Log.d(TAG, "Started Listening for Wake Word...")
            
        } catch (e: Exception) {
            
            Log.e(TAG, "Engine start failed: \${e.message}")
            handleEngineFailure()
        }
    }

    private fun stopContinuousListening() {
        
        isListening = false
        
        try {
            
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            
            restoreSystemBeep()
            abandonAudioFocus()
            
            updateNotification("Engine in Standby.")
            
        } catch (e: Exception) {
            
            Log.e(TAG, "Error stopping engine: \${e.message}")
        }
    }

    private fun restartListeningWithDelay(delayMs: Long = 100) {
        
        isListening = false
        
        if (!isWakeModeActive) {
            return
        }

        mainHandler.postDelayed({
            
            speechRecognizer?.cancel()
            startContinuousListening()
            
        }, delayMs)
    }

    // =========================================================
    // ERROR RECOVERY & RETRY LOGIC
    // =========================================================

    private fun handleEngineFailure() {
        
        if (retryCount < MAX_RETRIES) {
            
            retryCount++
            
            val backoffTime = 
                (retryCount * 1000).toLong()
                
            Log.d(TAG, "Retrying engine start in \$backoffTime ms (Attempt \$retryCount/\$MAX_RETRIES)")
            
            restartListeningWithDelay(backoffTime)
            
        } else {
            
            Log.e(TAG, "Max retries reached. Shutting down Wake Engine.")
            stopSelf()
        }
    }

    // =========================================================
    // WAKE WORD NEURAL MATCHING (BRAIN)
    // =========================================================

    private fun processRecognizedText(text: String) {
        
        val normalized = 
            text.lowercase(Locale.getDefault()).trim()
        
        Log.d(TAG, "Acoustic Input: \$normalized")

        // -----------------------------------------------------
        // Wake Word Matrix Check
        // -----------------------------------------------------
        
        val isWakeWord = 
            normalized.contains("jarvis") || 
            normalized.contains("hey jarvis") ||
            normalized.contains("hello jarvis") ||
            normalized.contains("ok jarvis")

        if (isWakeWord) {
            
            Log.d(TAG, "WAKE WORD VERIFIED. Executing Main Protocol.")
            
            // Stop background loop and restore audio for JARVIS response
            stopContinuousListening()
            
            // Route command to MainActivity Engine
            val launchIntent = 
                Intent(this, MainActivity::class.java).apply {
                    
                    action = ACTION_VOICE_COMMAND
                    putExtra(EXTRA_COMMAND, text)
                    
                    // Essential flags to launch Activity from Background Service
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                
            startActivity(launchIntent)
            
        } else {
            
            // False alarm, silently restart listening
            restartListeningWithDelay(50)
        }
    }

    // =========================================================
    // RECOGNITION LISTENER CALLBACKS
    // =========================================================

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Sensors Ready. Waiting for input.")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "User input detected. Capturing audio stream...")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Required override, but logging this floods the logcat
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Raw audio buffer, not used for text processing
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech stream detected.")
        isListening = false
    }

    override fun onError(error: Int) {
        
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied"
            SpeechRecognizer.ERROR_NETWORK -> "Network unavailable"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (Silence)"
            else -> "Unknown error code: \$error"
        }
        
        Log.w(TAG, "Acoustic Error: \$errorMessage")
        
        // -----------------------------------------------------
        // Intelligent Restart Handling based on Error Type
        // -----------------------------------------------------
        
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || 
            error == SpeechRecognizer.ERROR_NO_MATCH) {
            
            // Normal silence or unreadable audio, restart immediately
            restartListeningWithDelay(50)
            
        } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            
            // Give it time to breathe if busy
            restartListeningWithDelay(1000)
            
        } else {
            
            // Network or fatal errors use backoff strategy
            handleEngineFailure()
        }
    }

    override fun onResults(results: Bundle?) {
        
        val matches = 
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            
        if (!matches.isNullOrEmpty()) {
            
            processRecognizedText(matches[0])
            
        } else {
            
            restartListeningWithDelay(50)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        
        val matches = 
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            
        if (!matches.isNullOrEmpty()) {
            
            val partialText = 
                matches[0].lowercase(Locale.getDefault())
                
            // Aggressive early detection for faster response
            if (partialText.contains("jarvis")) {
                
                speechRecognizer?.stopListening()
                processRecognizedText(partialText)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Reserved for future extensions
    }

    // =========================================================
    // AUDIO MUTE SYSTEM (HACKS TO BYPASS GOOGLE BEEP)
    // =========================================================

    private fun muteSystemBeep() {
        
        if (isMuted) return
        
        try {
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                
                originalVolume = 
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, 
                    AudioManager.ADJUST_MUTE, 
                    0
                )
                
                isMuted = true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to engage mute protocol: \${e.message}")
        }
    }

    private fun restoreSystemBeep() {
        
        if (!isMuted) return
        
        try {
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, 
                    AudioManager.ADJUST_UNMUTE, 
                    0
                )
                
                isMuted = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disengage mute protocol: \${e.message}")
        }
    }

    // =========================================================
    // AUDIO FOCUS MANAGEMENT (PREVENTS MIC CLASHES)
    // =========================================================

    private fun requestAudioFocus() {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            val playbackAttributes = 
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                    
            audioFocusRequest = 
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        
                        when (focusChange) {
                            
                            AudioManager.AUDIOFOCUS_LOSS,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                // Another app took audio, pause listening
                                Log.w(TAG, "Audio Focus Lost. Suspending Engine.")
                                stopContinuousListening()
                            }
                            
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                // We got it back, resume
                                Log.w(TAG, "Audio Focus Regained. Resuming Engine.")
                                startContinuousListening()
                            }
                        }
                    }
                    .build()

            audioFocusRequest?.let {
                audioManager.requestAudioFocus(it)
            }
            
        } else {
            
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null, 
                AudioManager.STREAM_MUSIC, 
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
            
        } else {
            
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    // =========================================================
    // SCI-FI FOREGROUND NOTIFICATION SYSTEM
    // =========================================================

    private fun createNotificationChannel() {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Background Core",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Handles J.A.R.V.I.S. persistent voice detection."
                setShowBadge(false)
            }
            
            val manager = 
                getSystemService(NotificationManager::class.java)
                
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildDynamicNotification(
        statusText: String
    ): Notification {
        
        val intent = 
            Intent(this, MainActivity::class.java)
            
        val pendingIntent = 
            PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Core Online")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) 
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setColor(android.graphics.Color.parseColor("#00E5FF")) // JARVIS Cyan
            .build()
    }

    private fun updateNotification(
        statusText: String
    ) {
        
        val notificationManager = 
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
        notificationManager.notify(
            NOTIFICATION_ID, 
            buildDynamicNotification(statusText)
        )
    }

    // =========================================================
    // SERVICE SHUTDOWN & CLEANUP
    // =========================================================

    override fun onDestroy() {
        
        Log.d(TAG, "Initiating Core Engine Shutdown Sequence...")
        
        isWakeModeActive = false
        
        stopContinuousListening()
        
        releaseWakeLock()
        
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        mainHandler.removeCallbacksAndMessages(null)
        
        Log.d(TAG, "Shutdown Sequence Complete. Goodbye.")
        
        super.onDestroy()
    }
}
