package com.example.jarvis.voice

// ============================================================================
// J.A.R.V.I.S. TITAN VOICE ENGINE - REAL-TIME FULL-DUPLEX CALL MODE (V700.0)
// ============================================================================
// Architect: DrakoXNaeem
// Features: 
// 1. PURE AUDIO STREAMING: Uses AudioRecord (No SpeechRecognizer, ZERO Beeps)
// 2. WEBSOCKET READY: Ready to stream live PCM audio to Gemini/OpenAI Realtime APIs
// 3. FULL-DUPLEX: Can listen and speak simultaneously (like a real phone call)
// 4. FLOATING HOLOGRAM UI: Reactive to raw audio amplitude (RMS)
// ============================================================================

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity
import com.example.jarvis.ui.JarvisOrbView
import com.example.jarvis.ui.OrbState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.math.abs
import kotlin.math.sqrt

class VoiceService : Service() {

    // =========================================================
    // AUDIO & STREAMING COMPONENTS
    // =========================================================
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var webSocket: WebSocket? = null
    private val httpClient = OkHttpClient()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var recordingJob: Job? = null

    // Standard AI Audio Settings (16kHz, Mono, 16-bit PCM)
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // =========================================================
    // HARDWARE MANAGERS & UI
    // =========================================================
    private lateinit var audioManager: AudioManager
    private lateinit var windowManager: WindowManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var floatingLayout: LinearLayout? = null
    private var floatingOrb: JarvisOrbView? = null
    private var floatingText: TextView? = null
    private lateinit var windowParams: WindowManager.LayoutParams

    // Touch & State Variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var lastClickTime = 0L
    private val DOUBLE_CLICK_TIME_DELTA = 300L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCallActive = false

    companion object {
        const val ACTION_START_CALL = "com.example.jarvis.action.START_CALL"
        const val ACTION_END_CALL = "com.example.jarvis.action.END_CALL"
        private const val CHANNEL_ID = "JarvisLiveCallEngine"
        private const val NOTIFICATION_ID = 6060
        const val TAG = "JarvisLiveCall"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Booting J.A.R.V.I.S. Real-Time Full-Duplex Engine...")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> establishLiveCall()
            ACTION_END_CALL -> endLiveCall()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // =========================================================
    // 1. ESTABLISH FULL-DUPLEX CALL
    // =========================================================
    private fun establishLiveCall() {
        if (isCallActive) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        
        isCallActive = true
        val notification = buildSystemNotification("Live Link Connected")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        mountFloatingUI()
        
        // 1. Initialize Audio Hardware
        initAudioTrack()
        
        // 2. Open WebSocket to AI Brain (Gemini/OpenAI Realtime endpoint)
        connectToAIWebSocket()
        
        // 3. Start reading mic immediately
        startAudioStream()
    }

    private fun endLiveCall() {
        if (!isCallActive) return
        isCallActive = false
        
        updateUI(OrbState.IDLE, "Disconnecting...")
        recordingJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            webSocket?.close(1000, "Call Ended by User")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing audio/sockets: ${e.message}")
        }

        removeFloatingUI()
        stopForeground(true)
        stopSelf()
    }

    // =========================================================
    // 2. RAW PCM AUDIO STREAMING (NO MORE SPEECHRECOGNIZER)
    // =========================================================
    @SuppressLint("MissingPermission")
    private fun startAudioStream() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // Optimized for VOIP/Calling
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed!")
            endLiveCall()
            return
        }

        audioRecord?.startRecording()
        updateUI(OrbState.LISTENING, "Live Stream Active")

        recordingJob = serviceScope.launch {
            val audioBuffer = ByteArray(bufferSize)
            while (isActive && isCallActive) {
                val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readResult > 0) {
                    
                    // 1. Calculate Volume for UI (RMS)
                    val rms = calculateRMS(audioBuffer, readResult)
                    updateOrbVisuals(rms)

                    // 2. Stream raw audio bytes to AI via WebSocket
                    // For Gemini/OpenAI Realtime APIs, you usually send Base64 JSON or raw binary
                    if (rms > 10) { // Voice Activity Detection Threshold (VAD)
                        val base64Audio = android.util.Base64.encodeToString(audioBuffer.copyOf(readResult), android.util.Base64.NO_WRAP)
                        sendAudioToAI(base64Audio)
                    }
                }
            }
        }
    }

    private fun calculateRMS(buffer: ByteArray, readSize: Int): Float {
        var sum = 0.0
        for (i in 0 until readSize step 2) {
            // Convert 2 bytes to 16-bit short
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            sum += sample * sample
        }
        val mean = sum / (readSize / 2)
        return sqrt(mean).toFloat()
    }

    private suspend fun updateOrbVisuals(rms: Float) {
        withContext(Dispatchers.Main) {
            val scale = 1.0f + (rms / 2000f).coerceIn(0f, 0.4f)
            floatingOrb?.animate()?.scaleX(scale)?.scaleY(scale)?.setDuration(50)?.start()
        }
    }

    // =========================================================
    // 3. WEBSOCKET REAL-TIME CONNECTION (THE AI BRIDGE)
    // =========================================================
    private fun connectToAIWebSocket() {
        // NOTE: Replace this URL with your actual API endpoint (Gemini Multimodal Live / OpenAI Realtime)
        // If connecting to your own backend server that handles the LLM, put that IP here.
        val request = Request.Builder().url("wss://your-realtime-ai-endpoint.com/stream").build()
        
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "AI WebSocket Channel Opened!")
                updateUI(OrbState.LISTENING, "Call Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Handle JSON responses (transcripts, function calls, etc.)
                Log.d(TAG, "AI JSON Response: $text")
                updateUI(OrbState.SPEAKING, "AI is speaking...")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // RECEIVE LIVE AI AUDIO IN BYTES AND PLAY IMMEDIATELY (FULL DUPLEX)
                updateUI(OrbState.SPEAKING, "AI is speaking...")
                audioTrack?.play()
                audioTrack?.write(bytes.toByteArray(), 0, bytes.size())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "AI WebSocket Closed: $reason")
                if (isCallActive) endLiveCall()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "AI WebSocket Error: ${t.message}")
                updateUI(OrbState.ERROR, "Connection Lost")
            }
        })
    }

    private fun sendAudioToAI(base64PCM: String) {
        // Format depends on specific API. Example for JSON Realtime API:
        val jsonPayload = """{"type": "input_audio", "audio": "$base64PCM"}"""
        webSocket?.send(jsonPayload)
    }

    // =========================================================
    // 4. AUDIO TRACK (SPEAKER) SETUP FOR AI VOICE
    // =========================================================
    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    // =========================================================
    // 5. FLOATING UI (DRAGGABLE HOLOGRAM)
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
            text = "Connecting Stream..."
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10f
            gravity = Gravity.CENTER
            setShadowLayer(15f, 0f, 0f, Color.parseColor("#00E5FF"))
            setPadding(0, 8, 0, 0)
        }

        floatingLayout?.addView(floatingOrb, LinearLayout.LayoutParams(160, 160))
        floatingLayout?.addView(floatingText, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
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
                        val currentTime = System.currentTimeMillis()
                        // Double Tap to End Call
                        if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                            triggerHapticFeedback(200)
                            endLiveCall()
                        }
                        lastClickTime = currentTime
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
    // 6. UTILITIES (POWER, NOTIFICATIONS)
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

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JarvisCore::StreamLock")
            wakeLock?.acquire(12 * 60 * 60 * 1000L) 
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS Live Stream", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildSystemNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Direct Link")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentIntent(pIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setColor(Color.parseColor("#00E5FF"))
            .build()
    }

    override fun onDestroy() {
        endLiveCall()
        releaseWakeLock()
        super.onDestroy()
    }
}
