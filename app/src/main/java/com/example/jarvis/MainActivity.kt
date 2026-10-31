package com.example.jarvis

// ══════════════════════════════════════════════════════════════════════════════════════════
// J.A.R.V.I.S. TITAN V800.0 — ENTERPRISE AI COMPANION (FULL-DUPLEX EDITION)
// ══════════════════════════════════════════════════════════════════════════════════════════
// Architect: DrakoXNaeem
// Features: 
// - Continuous Real-Time Voice Link Integration (Zero Beep)
// - Dynamic Physics-Based Hologram HUDs
// - Multi-LLM Core (Gemini 2.0, GPT-4o, Grok Beta)
// - Deep System Automation & App Control Pipeline
// ══════════════════════════════════════════════════════════════════════════════════════════

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.jarvis.accessibility.JarvisAccessibilityService
import com.example.jarvis.voice.VoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// ══════════════════════════════════════════════════════════════════════════════════════════
// [1] CORE DATA MODELS
// ══════════════════════════════════════════════════════════════════════════════════════════
enum class AIProvider { GEMINI, CHATGPT, GROK }

data class AgentDecision(
    val action: String = "CHAT",
    val target: String = "",
    val payload: String = "",
    val speech: String = ""
)

// ══════════════════════════════════════════════════════════════════════════════════════════
// [2] MAIN ACTIVITY LIFECYCLE
// ══════════════════════════════════════════════════════════════════════════════════════════
class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener,
    LocationListener, AudioManager.OnAudioFocusChangeListener {

    init {
        try {
            System.loadLibrary("jarvis_native_engine")
            Log.i(TAG, "Native C++ Engine Booted Successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "Native Engine Unavailable. Utilizing JVM Fallback Mode.")
        }
    }

    enum class SystemState {
        POWER_OFF, ONLINE, STANDBY, PROCESSING, LISTENING, SPEAKING, CALL_ACTIVE, ERROR
    }

    companion object {
        private const val TAG = "JarvisTitanCore"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private var currentState = SystemState.POWER_OFF
    @Volatile private var isCallModeActive = false

    // UI Bindings
    private var messageInputBox: EditText? = null
    private var micToggleButton: View? = null
    private var sendCommandButton: View? = null
    private var settingsBtn: View? = null
    private var jarvisOrbView: View? = null
    private var rootLayout: ViewGroup? = null

    // Advanced Cyberpunk HUD Overlays
    private var matrixRainView: MatrixRainView? = null
    private var radarHUDView: CyberRadarView? = null
    private var particleEmitterView: ParticleEmitterView? = null
    private var compassHUDView: HoloCompassView? = null
    private var spectrumAnalyzerView: SpectrumView? = null

    // System Managers & Engines
    private lateinit var ttsEngine: TextToSpeech
    private lateinit var apiPrefs: SharedPreferences
    private lateinit var crypto: HybridCrypto
    private lateinit var appAutomation: AppAutomation
    private lateinit var aiBrain: MultiLLMBrain
    
    private lateinit var audioManager: AudioManager
    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null
    private var proxSensor: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var mainCameraId: String? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var aiProcessingJob: Job? = null
    private var isTtsEngineReady = false

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [3] BROADCAST RECEIVER (SYNC WITH VOICE SERVICE)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private val backgroundVoiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VoiceService.ACTION_VOICE_COMMAND -> {
                    val transcribedText = intent.getStringExtra(VoiceService.EXTRA_COMMAND) ?: ""
                    Log.i(TAG, "Received Audio Command from VoiceService: $transcribedText")
                    if (transcribedText.isNotBlank()) {
                        processUserInput(transcribedText)
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [4] INITIALIZATION & VIEW BINDING
    // ══════════════════════════════════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge Fullscreen Setup
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)
        rootLayout = findViewById(android.R.id.content)

        initializeCoreSystems()
        bindInterfaceViews()
        injectHologramOverlays()
        setupInteractionListeners()
        registerBackgroundReceivers()

        executeStartupSequence()
    }

    private fun initializeCoreSystems() {
        Log.i(TAG, "Initializing System Architectures...")
        
        ttsEngine = TextToSpeech(this, this)
        apiPrefs = getSharedPreferences("JarvisTitanVault_V800", Context.MODE_PRIVATE)
        crypto = HybridCrypto()
        appAutomation = AppAutomation(this)
        aiBrain = MultiLLMBrain()
        
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            mainCameraId = cameraManager?.cameraIdList?.firstOrNull {
                cameraManager?.getCameraCharacteristics(it)
                    ?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed: ${e.message}")
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun bindInterfaceViews() {
        val res = resources
        val pkg = packageName
        
        try {
            messageInputBox = findViewById(res.getIdentifier("messageInput", "id", pkg))
            micToggleButton = findViewById(res.getIdentifier("micButton", "id", pkg))
            sendCommandButton = findViewById(res.getIdentifier("sendButton", "id", pkg))
            settingsBtn = findViewById(res.getIdentifier("settingsButton", "id", pkg))
            jarvisOrbView = findViewById(res.getIdentifier("jarvisOrbView", "id", pkg))
            
            // Set Dynamic Background safely
            val bgRes = res.getIdentifier("jarvis_bg", "drawable", pkg)
            if (bgRes != 0) rootLayout?.setBackgroundResource(bgRes)
            else rootLayout?.setBackgroundColor(Color.parseColor("#040814"))
            
        } catch (e: Exception) {
            Log.e(TAG, "UI Binding Error. Ensure XML IDs match.", e)
            rootLayout?.setBackgroundColor(Color.parseColor("#040814"))
        }
    }

    private fun injectHologramOverlays() {
        try {
            Log.i(TAG, "Injecting Cyberpunk HUD Layers...")
            
            // Layer 1: Background Particles
            particleEmitterView = ParticleEmitterView(this)
            rootLayout?.addView(particleEmitterView, 0, FrameLayout.LayoutParams(-1, -1))

            // Layer 2: Matrix Rain (Low Opacity)
            matrixRainView = MatrixRainView(this).apply { alpha = 0.15f }
            rootLayout?.addView(matrixRainView, 1, FrameLayout.LayoutParams(-1, -1))

            // Layer 3: Central Radar
            radarHUDView = CyberRadarView(this)
            rootLayout?.addView(radarHUDView, 2, FrameLayout.LayoutParams(-1, -1))

            // Layer 4: Gyro Compass
            compassHUDView = HoloCompassView(this)
            rootLayout?.addView(compassHUDView, 3, FrameLayout.LayoutParams(-1, -1))

            // Layer 5: Audio Spectrum Analyzer (Bottom)
            spectrumAnalyzerView = SpectrumView(this)
            rootLayout?.addView(spectrumAnalyzerView, 4, FrameLayout.LayoutParams(-1, -1))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject HUD layers", e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBackgroundReceivers() {
        val filter = IntentFilter(VoiceService.ACTION_VOICE_COMMAND)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(backgroundVoiceReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(backgroundVoiceReceiver, filter)
        }
    }

    private fun executeStartupSequence() {
        jarvisOrbView?.let { orb ->
            ObjectAnimator.ofFloat(orb, View.ROTATION, 0f, 360f).apply {
                duration = 6000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }

        lifecycleScope.launch {
            delay(1000)
            updateSystemState(SystemState.ONLINE)
            
            val provider = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")
            triggerHapticFeedback(100)
            
            speak("System Online. J.A.R.V.I.S. Titan Engine Initialized with $provider Core. Awaiting Commands.")
            currentState = SystemState.STANDBY
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [5] INTERACTION LISTENERS & LIVE CALL CONTROLS
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun setupInteractionListeners() {
        // Text Input Processing
        sendCommandButton?.setOnClickListener {
            val cmd = messageInputBox?.text?.toString()?.trim() ?: ""
            if (cmd.isNotEmpty()) {
                messageInputBox?.text?.clear()
                processUserInput(cmd)
            }
        }

        // Live Voice Call Control (No more local STT Beeps)
        micToggleButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                return@setOnClickListener
            }

            if (!isCallModeActive) {
                initiateLiveCallSession()
            } else {
                terminateLiveCallSession()
            }
        }

        settingsBtn?.setOnClickListener { openAdvancedSettingsVault() }
    }

    private fun initiateLiveCallSession() {
        Log.i(TAG, "Starting Full-Duplex Live Call Session")
        isCallModeActive = true
        updateSystemState(SystemState.CALL_ACTIVE)
        triggerHapticFeedback(150)
        
        Toast.makeText(this, "🎙️ Neural Audio Link Connected", Toast.LENGTH_SHORT).show()

        val serviceIntent = Intent(this, VoiceService::class.java).apply {
            action = VoiceService.ACTION_START_CALL
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        speak("Yes sir, I am online and listening.")
    }

    private fun terminateLiveCallSession() {
        Log.i(TAG, "Terminating Live Call Session")
        isCallModeActive = false
        updateSystemState(SystemState.STANDBY)
        triggerHapticFeedback(80)
        
        val serviceIntent = Intent(this, VoiceService::class.java).apply {
            action = VoiceService.ACTION_END_CALL
        }
        startService(serviceIntent)

        speak("Call terminated. Standing by.")
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [6] AI BRAIN PROCESSING & AUTOMATION ROUTING
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun processUserInput(rawInput: String) {
        val userText = rawInput.trim()
        if (userText.isEmpty()) return

        Log.d(TAG, "USER INPUT: $userText")
        updateSystemState(SystemState.PROCESSING)

        // Hardcoded Fast Actions (Bypass LLM for speed)
        val lowerCaseInput = userText.lowercase(Locale.getDefault())
        if (lowerCaseInput.contains("torch on") || lowerCaseInput.contains("लाइट ऑन")) {
            setHardwareTorch(true)
            speak("Flashlight engaged.")
            resumeBackgroundListening()
            return
        }
        if (lowerCaseInput.contains("torch off") || lowerCaseInput.contains("लाइट ऑफ")) {
            setHardwareTorch(false)
            speak("Flashlight disengaged.")
            resumeBackgroundListening()
            return
        }
        if (lowerCaseInput.contains("call disconnect") || lowerCaseInput.contains("bye jarvis")) {
            terminateLiveCallSession()
            return
        }

        // Delegate to AI Engine
        aiProcessingJob?.cancel() // Cancel previous job if user interrupts
        
        aiProcessingJob = lifecycleScope.launch {
            val providerStr = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI") ?: "GEMINI"
            val providerEnum = try { AIProvider.valueOf(providerStr) } catch (_: Exception) { AIProvider.GEMINI }

            val apiKey = when (providerEnum) {
                AIProvider.GEMINI -> crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: "")
                AIProvider.CHATGPT -> crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: "")
                AIProvider.GROK -> crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: "")
            }.trim()

            val modelName = when (providerEnum) {
                AIProvider.GEMINI -> apiPrefs.getString("MODEL_GEMINI", "gemini-2.0-flash") ?: "gemini-2.0-flash"
                AIProvider.CHATGPT -> apiPrefs.getString("MODEL_CHATGPT", "gpt-4o") ?: "gpt-4o"
                AIProvider.GROK -> apiPrefs.getString("MODEL_GROK", "grok-beta") ?: "grok-beta"
            }.trim()

            if (apiKey.isBlank()) {
                speak("Sir, the API key for $providerStr is missing. Please configure it in the vault.")
                resumeBackgroundListening()
                return@launch
            }

            try {
                val decision = aiBrain.query(providerEnum, apiKey, modelName, userText)
                executeAgentProtocol(decision)
            } catch (e: Exception) {
                Log.e(TAG, "AI Processing Failure", e)
                speak("I encountered an error while processing that request.")
                resumeBackgroundListening()
            }
        }
    }

    private fun executeAgentProtocol(decision: AgentDecision) {
        // Step 1: Vocalize AI Response
        if (decision.speech.isNotBlank()) {
            speak(decision.speech)
        } else {
            resumeBackgroundListening()
        }

        // Step 2: Execute System Action
        val accessService = JarvisAccessibilityService.instance

        when (decision.action.uppercase(Locale.getDefault())) {
            "OPEN_APP" -> appAutomation.launchApplication(decision.target)
            
            "WHATSAPP_SEND" -> {
                appAutomation.launchApplication("com.whatsapp")
                val finalPayload = if (decision.target.isNotBlank()) "${decision.target}: ${decision.payload}" else decision.payload
                accessService?.automateWhatsAppSend(finalPayload)
            }
            
            "INSTAGRAM_SEARCH" -> {
                appAutomation.launchApplication("com.instagram.android")
                lifecycleScope.launch {
                    delay(2000)
                    accessService?.performJarvisAction("CLICK", target = "Search")
                    delay(1000)
                    accessService?.performJarvisAction("TYPE", target = "Search", value = decision.target)
                }
            }
            
            "CALL" -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE), 102)
                    speak("I need contacts and phone permissions to make a call.")
                } else {
                    val contactNumber = resolveContactNumber(decision.target)
                    if (contactNumber != null) {
                        speak("Dialing ${decision.target} now.")
                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$contactNumber"))
                        startActivity(callIntent)
                    } else {
                        speak("I could not find ${decision.target} in your address book.")
                    }
                }
            }
            
            "SYSTEM_ACTION" -> when (decision.target.uppercase(Locale.getDefault())) {
                "HOME" -> accessService?.performJarvisAction("HOME")
                "BACK" -> accessService?.performJarvisAction("BACK")
                "RECENTS" -> accessService?.performJarvisAction("RECENTS")
                "SWIPE_UP" -> accessService?.performJarvisAction("SWIPE_UP")
                "SWIPE_DOWN" -> accessService?.performJarvisAction("SWIPE_DOWN")
                "LOCK_SCREEN" -> accessService?.performJarvisAction("LOCK_SCREEN")
                "TAKE_SCREENSHOT" -> accessService?.performJarvisAction("TAKE_SCREENSHOT")
            }
            
            "CLICK" -> accessService?.performJarvisAction("CLICK", target = decision.target)
            "TYPE" -> accessService?.performJarvisAction("TYPE", target = decision.target, value = decision.payload)
            
            "CHAT" -> {
                // Just chat, nothing to execute visually
            }
        }
    }

    @SuppressLint("Range")
    private fun resolveContactNumber(contactName: String): String? {
        if (contactName.matches(Regex("^[0-9+ -]+$"))) return contactName
        
        var foundNumber: String? = null
        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$contactName%"),
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    foundNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contact Resolution Error", e)
        }
        return foundNumber
    }

    private fun resumeBackgroundListening() {
        if (isCallModeActive) {
            // Signal VoiceService to start listening again if we were using turn-based WebSocket or just to refresh state
            val resumeIntent = Intent(VoiceService.ACTION_RESUME_LISTENING)
            sendBroadcast(resumeIntent)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [7] TEXT-TO-SPEECH (TTS ENGINE)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                // Attempt to set Hindi language for AI Persona
                val langResult = ttsEngine.setLanguage(Locale("hi", "IN"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Hindi not supported. Falling back to English.")
                    ttsEngine.language = Locale.US
                }
                
                // Fine-tune Voice parameters for a confident, AI-like tone
                ttsEngine.setSpeechRate(1.02f)
                ttsEngine.setPitch(1.1f)
                isTtsEngineReady = true
                Log.i(TAG, "TTS Engine Initialized Successfully")
            } catch (e: Exception) {
                Log.e(TAG, "TTS Setup Exception", e)
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed")
        }
    }

    private fun speak(textToSpeak: String) {
        if (!isTtsEngineReady || textToSpeak.isBlank()) {
            resumeBackgroundListening()
            return
        }
        
        updateSystemState(SystemState.SPEAKING)

        // Request Audio Focus to ensure TTS is heard clearly
        try {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
                
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio Focus Exception", e)
        }

        // Execute Speech
        try {
            val utteranceId = "JARVIS_VOICE_OUT_${System.currentTimeMillis()}"
            
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Update Spectrum Analyzer if needed
                }
                
                override fun onDone(utteranceId: String?) {
                    mainHandler.post { resumeBackgroundListening() }
                }
                
                override fun onError(utteranceId: String?) {
                    mainHandler.post { resumeBackgroundListening() }
                }
            })
            
            ttsEngine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Speech Output Error", e)
            resumeBackgroundListening()
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // Handle audio focus changes if a phone call comes in, etc.
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [8] MULTI-LLM AI BRAIN (ENTERPRISE NETWORK PROTOCOL)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    class MultiLLMBrain {
        private val conversationHistory = mutableListOf<Pair<String, String>>()
        private val maxHistoryContext = 8 // Keeps last 4 turns

        private val corePersonaInstructions = """
            You are J.A.R.V.I.S., a highly intelligent, efficient, and loyal AI Assistant.
            You communicate in Hindi/Hinglish natively, like a smart companion.
            
            CRITICAL DIRECTIVE: You MUST control the user's Android phone using the JSON protocol below.
            If the user asks you to perform an action on their phone, set the 'action' field appropriately.

            PROTOCOL ACTIONS:
            - Open any App: action="OPEN_APP", target="app_name" (e.g., whatsapp, instagram, youtube)
            - Send WhatsApp Msg: action="WHATSAPP_SEND", target="Contact Name", payload="Message"
            - Search Instagram: action="INSTAGRAM_SEARCH", target="username"
            - Phone Call: action="CALL", target="Contact Name"
            - Android Navigation: action="SYSTEM_ACTION", target="HOME" | "BACK" | "RECENTS" | "SWIPE_UP" | "LOCK_SCREEN" | "TAKE_SCREENSHOT"
            - Normal Chat: action="CHAT"

            OUTPUT RESTRICTION: You are a machine. Output ONLY valid JSON. No markdown, no prefixes.
            {
              "action": "CHAT",
              "target": "target_name_or_empty",
              "payload": "extra_data_or_empty",
              "speech": "Your response spoken in Hindi/Hinglish"
            }
        """.trimIndent()

        suspend fun query(provider: AIProvider, apiKey: String, model: String, userMessage: String): AgentDecision = withContext(Dispatchers.IO) {
            try {
                when (provider) {
                    AIProvider.GEMINI -> callGeminiAPI(apiKey, model, userMessage)
                    AIProvider.CHATGPT -> callOpenAIFormatAPI("https://api.openai.com/v1/chat/completions", apiKey, model, userMessage)
                    AIProvider.GROK -> callOpenAIFormatAPI("https://api.x.ai/v1/chat/completions", apiKey, model, userMessage)
                }
            } catch (e: Exception) {
                Log.e("JarvisLLM", "Fatal Network Request Error", e)
                fallbackAgentDecision(userMessage)
            }
        }

        private fun callGeminiAPI(key: String, model: String, msg: String): AgentDecision {
            val targetModel = model.ifEmpty { "gemini-2.0-flash" }
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$key")
            
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000 // 20 Seconds
                readTimeout = 20000
            }

            val contentsArray = JSONArray()
            // Append History
            for (historyItem in conversationHistory) {
                contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", historyItem.first))))
                contentsArray.put(JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", historyItem.second))))
            }
            
            // Append New Message with Persona if it's the first turn, else just message
            val promptContent = if (conversationHistory.isEmpty()) "$corePersonaInstructions\n\nUser: $msg" else msg
            contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", promptContent))))

            val payloadJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payloadJson.toString()) }

            if (connection.responseCode == 200) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonRes = JSONObject(responseString)
                val rawText = jsonRes.getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                
                val parsedJson = extractJsonSafely(rawText)
                if (parsedJson != null) {
                    updateContextHistory(msg, rawText)
                    return buildDecisionObject(parsedJson)
                }
            }
            return fallbackAgentDecision(msg)
        }

        private fun callOpenAIFormatAPI(endpoint: String, key: String, model: String, msg: String): AgentDecision {
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().put("role", "system").put("content", corePersonaInstructions))
            
            for (historyItem in conversationHistory) {
                messagesArray.put(JSONObject().put("role", "user").put("content", historyItem.first))
                messagesArray.put(JSONObject().put("role", "assistant").put("content", historyItem.second))
            }
            messagesArray.put(JSONObject().put("role", "user").put("content", msg))

            val payloadJson = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("response_format", JSONObject().put("type", "json_object"))
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payloadJson.toString()) }

            if (connection.responseCode == 200) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonRes = JSONObject(responseString)
                val rawText = jsonRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                
                val parsedJson = extractJsonSafely(rawText)
                if (parsedJson != null) {
                    updateContextHistory(msg, rawText)
                    return buildDecisionObject(parsedJson)
                }
            }
            return fallbackAgentDecision(msg)
        }

        private fun extractJsonSafely(rawStr: String): JSONObject? {
            return try {
                val startIdx = rawStr.indexOf('{')
                val endIdx = rawStr.lastIndexOf('}')
                if (startIdx != -1 && endIdx > startIdx) {
                    JSONObject(rawStr.substring(startIdx, endIdx + 1))
                } else {
                    JSONObject(rawStr)
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun updateContextHistory(userStr: String, aiStr: String) {
            conversationHistory.add(Pair(userStr, aiStr))
            if (conversationHistory.size > maxHistoryContext) {
                conversationHistory.removeAt(0)
            }
        }

        private fun buildDecisionObject(json: JSONObject): AgentDecision {
            return AgentDecision(
                action = json.optString("action", "CHAT"),
                target = json.optString("target", ""),
                payload = json.optString("payload", ""),
                speech = json.optString("speech", "Sir, I have processed the request.")
            )
        }

        private fun fallbackAgentDecision(msg: String): AgentDecision {
            val lower = msg.lowercase(Locale.getDefault())
            return when {
                lower.contains("instagram") -> AgentDecision("OPEN_APP", "instagram", "", "Opening Instagram now sir.")
                lower.contains("whatsapp") -> AgentDecision("OPEN_APP", "whatsapp", "", "Accessing WhatsApp.")
                lower.contains("youtube") -> AgentDecision("OPEN_APP", "youtube", "", "Launching YouTube.")
                lower.contains("home") -> AgentDecision("SYSTEM_ACTION", "HOME", "", "Returning to Home Screen.")
                lower.contains("back") -> AgentDecision("SYSTEM_ACTION", "BACK", "", "Going back.")
                else -> AgentDecision("CHAT", "", "", "I am having trouble connecting to the neural net. Please try again.")
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [9] HARDWARE AUTOMATION & UTILITIES
    // ══════════════════════════════════════════════════════════════════════════════════════════
    inner class AppAutomation(private val ctx: Context) {
        fun launchApplication(packageName: String) {
            try {
                ctx.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch specific package", e)
            }
        }

        fun launchApplicationByName(appName: String) {
            val queryName = appName.lowercase(Locale.getDefault())
            when {
                queryName.contains("whatsapp") -> launchApplication("com.whatsapp")
                queryName.contains("instagram") -> launchApplication("com.instagram.android")
                queryName.contains("youtube") -> launchApplication("com.google.android.youtube")
                queryName.contains("facebook") -> launchApplication("com.facebook.katana")
                queryName.contains("telegram") -> launchApplication("org.telegram.messenger")
                queryName.contains("spotify") -> launchApplication("com.spotify.music")
                queryName.contains("chrome") -> launchApplication("com.android.chrome")
                else -> {
                    // Deep search through installed launcher apps
                    try {
                        val mainIntent = Intent(Intent.ACTION_MAIN, null)
                        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                        val apps = ctx.packageManager.queryIntentActivities(mainIntent, 0)
                        
                        for (appInfo in apps) {
                            val label = appInfo.loadLabel(ctx.packageManager).toString().lowercase(Locale.getDefault())
                            if (label.contains(queryName)) {
                                launchApplication(appInfo.activityInfo.packageName)
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Deep app search failed", e)
                    }
                }
            }
        }
    }

    inner class HybridCrypto {
        // Advanced AES-256 Symmetric Encryption for local API key storage
        private val securityToken = "JARVIS_TITAN_V800_SECURE_VAULT_KEY_!".toByteArray().copyOf(32)
        
        fun encrypt(plainData: String): String {
            return try {
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(securityToken, "AES"))
                val encryptedBytes = cipher.doFinal(plainData.toByteArray())
                Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "Encryption Exception", e)
                ""
            }
        }

        fun decrypt(encryptedData: String): String {
            if (encryptedData.isBlank()) return ""
            return try {
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(securityToken, "AES"))
                val decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
                val decryptedBytes = cipher.doFinal(decodedBytes)
                String(decryptedBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Decryption Exception", e)
                ""
            }
        }
    }

    private fun setHardwareTorch(enable: Boolean) {
        if (mainCameraId == null) return
        try {
            cameraManager?.setTorchMode(mainCameraId!!, enable)
        } catch (e: Exception) {
            Log.e(TAG, "Torch Toggle Error", e)
        }
    }

    private fun triggerHapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                vibratorManager.defaultVibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptics unavailable", e)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [10] ADVANCED SETTINGS VAULT (DYNAMIC UI INJECTION)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun openAdvancedSettingsVault() {
        val rootScrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
        }
        
        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 80)
            setBackgroundColor(Color.parseColor("#060B18")) // Deep Cyber Blue
        }

        // Title Header
        val vaultTitle = TextView(this).apply {
            text = "TITAN SYSTEM VAULT"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
            setShadowLayer(15f, 0f, 0f, Color.parseColor("#00F0FF"))
        }
        containerLayout.addView(vaultTitle)

        // Provider Selector
        val engineLabel = TextView(this).apply {
            text = "CORE ENGINE SELECTOR:"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 20, 0, 20)
        }
        containerLayout.addView(engineLabel)

        val providerRadioGroup = RadioGroup(this).apply { 
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        
        val rbGemini = RadioButton(this).apply { text = "GEMINI"; setTextColor(Color.WHITE); textSize = 12f }
        val rbChatGpt = RadioButton(this).apply { text = "OPENAI"; setTextColor(Color.WHITE); textSize = 12f; setPadding(20,0,20,0) }
        val rbGrok = RadioButton(this).apply { text = "GROK"; setTextColor(Color.WHITE); textSize = 12f }

        when (apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")) {
            "CHATGPT" -> rbChatGpt.isChecked = true
            "GROK" -> rbGrok.isChecked = true
            else -> rbGemini.isChecked = true
        }
        providerRadioGroup.addView(rbGemini)
        providerRadioGroup.addView(rbChatGpt)
        providerRadioGroup.addView(rbGrok)
        containerLayout.addView(providerRadioGroup)

        // Input Field Generator
        fun buildInputField(labelText: String, hintStr: String, currentValue: String): Pair<TextView, EditText> {
            val label = TextView(this).apply {
                text = labelText
                setTextColor(Color.parseColor("#667788"))
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, 30, 0, 10)
            }
            val inputbox = EditText(this).apply {
                hint = hintStr
                setTextColor(Color.WHITE)
                setHintTextColor(Color.parseColor("#334455"))
                setBackgroundColor(Color.parseColor("#0A1224"))
                setPadding(40, 35, 40, 35)
                setText(currentValue)
                textSize = 14f
            }
            return Pair(label, inputbox)
        }

        // Generate Fields
        val (lblGeminiKey, etGeminiKey) = buildInputField("GEMINI API KEY", "AIza...", crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: ""))
        val (lblGeminiModel, etGeminiModel) = buildInputField("GEMINI MODEL", "gemini-2.0-flash", apiPrefs.getString("MODEL_GEMINI", "gemini-2.0-flash") ?: "gemini-2.0-flash")
        
        val (lblGptKey, etGptKey) = buildInputField("OPENAI API KEY", "sk-...", crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: ""))
        val (lblGptModel, etGptModel) = buildInputField("OPENAI MODEL", "gpt-4o", apiPrefs.getString("MODEL_CHATGPT", "gpt-4o") ?: "gpt-4o")
        
        val (lblGrokKey, etGrokKey) = buildInputField("GROK API KEY", "xai-...", crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: ""))
        val (lblGrokModel, etGrokModel) = buildInputField("GROK MODEL", "grok-beta", apiPrefs.getString("MODEL_GROK", "grok-beta") ?: "grok-beta")

        // Add to Layout
        containerLayout.addView(lblGeminiKey); containerLayout.addView(etGeminiKey)
        containerLayout.addView(lblGeminiModel); containerLayout.addView(etGeminiModel)
        containerLayout.addView(lblGptKey); containerLayout.addView(etGptKey)
        containerLayout.addView(lblGptModel); containerLayout.addView(etGptModel)
        containerLayout.addView(lblGrokKey); containerLayout.addView(etGrokKey)
        containerLayout.addView(lblGrokModel); containerLayout.addView(etGrokModel)

        // Save Button
        val saveButton = Button(this).apply {
            text = "INITIALIZE CORE OVERRIDE"
            setBackgroundColor(Color.parseColor("#00F0FF"))
            setTextColor(Color.parseColor("#020610"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 60
                bottomMargin = 20
            }
        }
        containerLayout.addView(saveButton)
        rootScrollView.addView(containerLayout)

        // Dialog Creation
        val alertDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth)
            .setView(rootScrollView).create()
            
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        // Save Action Logic
        saveButton.setOnClickListener {
            val selectedEngine = when {
                rbChatGpt.isChecked -> "CHATGPT"
                rbGrok.isChecked -> "GROK"
                else -> "GEMINI"
            }
            
            apiPrefs.edit().apply {
                putString("ACTIVE_PROVIDER", selectedEngine)
                putString("API_GEMINI", crypto.encrypt(etGeminiKey.text.toString().trim()))
                putString("MODEL_GEMINI", etGeminiModel.text.toString().trim().ifEmpty { "gemini-2.0-flash" })
                putString("API_CHATGPT", crypto.encrypt(etGptKey.text.toString().trim()))
                putString("MODEL_CHATGPT", etGptModel.text.toString().trim().ifEmpty { "gpt-4o" })
                putString("API_GROK", crypto.encrypt(etGrokKey.text.toString().trim()))
                putString("MODEL_GROK", etGrokModel.text.toString().trim().ifEmpty { "grok-beta" })
            }.apply()
            
            triggerHapticFeedback(200)
            alertDialog.dismiss()
            Toast.makeText(this, "Core Overwritten: $selectedEngine", Toast.LENGTH_LONG).show()
            speak("System configurations updated. $selectedEngine architecture is now the primary core.")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [11] SYSTEM STATE MANAGEMENT & THEME ORCHESTRATION
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun updateSystemState(newState: SystemState) {
        currentState = newState
        Log.d(TAG, "System State Transition: ${newState.name}")
        
        val targetColorHex = when (newState) {
            SystemState.PROCESSING -> "#2800D4FF" // Deep Cyan
            SystemState.LISTENING -> "#2800FF88"  // Neon Green
            SystemState.SPEAKING -> "#28FF8800"   // Neon Orange
            SystemState.CALL_ACTIVE -> "#25B000FF"// Cyber Purple
            SystemState.ERROR -> "#40FF0000"      // Critical Red
            else -> "#00000000"                   // Transparent
        }

        try {
            rootLayout?.let { layout ->
                ObjectAnimator.ofArgb(layout, "backgroundColor", Color.parseColor(targetColorHex)).apply {
                    duration = 600
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Color Animation Error", e)
        }

        // Broadcast State to HUD Overlays
        try {
            matrixRainView?.applyThemeState(newState)
            radarHUDView?.applyThemeState(newState)
            particleEmitterView?.applyThemeState(newState)
            compassHUDView?.applyThemeState(newState)
            spectrumAnalyzerView?.applyThemeState(newState)
        } catch (e: Exception) {
            Log.w(TAG, "HUD State Update Warning", e)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [12] ENTERPRISE CYBERPUNK HUD GRAPHICS (MASSIVE CUSTOM VIEWS)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    
    // HUD 1: Advanced Matrix Rain Renderer
    inner class MatrixRainView(context: Context) : View(context) {
        private val randomGen = Random()
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) 
            textSize = 24f
        }
        
        // Complex Array structure: [x, y, speed, length, opacity]
        private val codeDrops = Array(180) {
            floatArrayOf(
                randomGen.nextFloat() * 2500f, 
                randomGen.nextFloat() * -4000f, 
                randomGen.nextFloat() * 18f + 8f,
                randomGen.nextFloat() * 10f + 5f,
                randomGen.nextFloat() * 255f
            )
        }
        private var activeThemeColor = "#00F0FF"

        fun applyThemeState(state: SystemState) {
            activeThemeColor = when (state) {
                SystemState.ERROR -> "#FF0044"
                SystemState.LISTENING -> "#00FF88"
                SystemState.SPEAKING -> "#FF8800"
                SystemState.CALL_ACTIVE -> "#B000FF"
                else -> "#00F0FF"
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val parsedColor = Color.parseColor(activeThemeColor)
            
            for (drop in codeDrops) {
                renderPaint.color = parsedColor
                renderPaint.alpha = drop[4].toInt()
                
                // Render trailing characters
                for (i in 0..drop[3].toInt()) {
                    val yPos = drop[1] - (i * 24f)
                    val alpha = Math.max(0, drop[4].toInt() - (i * 20))
                    renderPaint.alpha = alpha
                    canvas.drawText(randomGen.nextInt(10).toString(), drop[0], yPos, renderPaint)
                }

                // Update Physics
                drop[1] += drop[2]
                
                // Reset Drop if out of bounds
                if (drop[1] > height + 200f) {
                    drop[1] = -200f
                    drop[0] = randomGen.nextFloat() * width
                    drop[2] = randomGen.nextFloat() * 18f + 8f
                    drop[4] = randomGen.nextFloat() * 155f + 100f // Random opacity 100-255
                }
            }
            invalidate() // Continuous render loop
        }
    }

    // HUD 2: Cybernetic Pulse Radar
    inner class CyberRadarView(context: Context) : View(context) {
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.STROKE
            strokeWidth = 3f 
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        private var sweepAngle = 0f
        private var activeThemeColor = "#00F0FF"
        private var gridRotation = 0f

        fun applyThemeState(state: SystemState) {
            activeThemeColor = when (state) {
                SystemState.ERROR -> "#FF0044"
                SystemState.LISTENING -> "#00FF88"
                SystemState.CALL_ACTIVE -> "#B000FF"
                else -> "#00F0FF"
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val maxRadius = 380f
            val parsedColor = Color.parseColor(activeThemeColor)

            // Draw Concentric Rings
            strokePaint.color = parsedColor
            strokePaint.alpha = 50
            canvas.drawCircle(cx, cy, maxRadius, strokePaint)
            canvas.drawCircle(cx, cy, maxRadius * 0.75f, strokePaint)
            canvas.drawCircle(cx, cy, maxRadius * 0.45f, strokePaint)

            // Draw Rotating Grid Crosshairs
            canvas.save()
            canvas.rotate(gridRotation, cx, cy)
            strokePaint.alpha = 25
            canvas.drawLine(cx - maxRadius, cy, cx + maxRadius, cy, strokePaint)
            canvas.drawLine(cx, cy - maxRadius, cx, cy + maxRadius, strokePaint)
            canvas.restore()
            gridRotation += 0.2f

            // Draw Scanning Sweep Arc
            fillPaint.color = parsedColor
            fillPaint.alpha = 40
            canvas.drawArc(RectF(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius), sweepAngle, 65f, true, fillPaint)
            sweepAngle = (sweepAngle + 5.5f) % 360f
            
            invalidate()
        }
    }

    // HUD 3: Quantum Particle Emitter
    inner class ParticleEmitterView(context: Context) : View(context) {
        private val randomGen = Random()
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val particlePool = Array(120) { QuantumParticle() }
        private var activeThemeColor = "#00F0FF"

        inner class QuantumParticle {
            var x = randomGen.nextFloat() * 2000f
            var y = randomGen.nextFloat() * 4000f
            var velocityX = randomGen.nextFloat() * 8f - 4f
            var velocityY = randomGen.nextFloat() * 8f - 4f
            var radius = randomGen.nextFloat() * 5f + 2f
            var lifespan = randomGen.nextInt(255)
            var fadeRate = randomGen.nextInt(5) + 1
        }

        fun applyThemeState(state: SystemState) {
            activeThemeColor = when (state) {
                SystemState.ERROR -> "#FF0044"
                SystemState.LISTENING -> "#00FF88"
                SystemState.CALL_ACTIVE -> "#B000FF"
                else -> "#00F0FF"
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val parsedColor = Color.parseColor(activeThemeColor)

            for (particle in particlePool) {
                renderPaint.color = parsedColor
                renderPaint.alpha = particle.lifespan

                canvas.drawCircle(particle.x, particle.y, particle.radius, renderPaint)

                // Newtonian Physics
                particle.x += particle.velocityX
                particle.y += particle.velocityY
                particle.lifespan -= particle.fadeRate

                // Boundary Bounce & Reset
                if (particle.x < 0 || particle.x > width) particle.velocityX *= -1
                if (particle.y < 0 || particle.y > height) particle.velocityY *= -1
                
                if (particle.lifespan <= 0) {
                    particle.x = randomGen.nextFloat() * width
                    particle.y = randomGen.nextFloat() * height
                    particle.lifespan = 255
                }
            }
            invalidate()
        }
    }

    // HUD 4: Gyroscopic Holographic Compass
    inner class HoloCompassView(context: Context) : View(context) {
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.STROKE
            strokeWidth = 4f 
            pathEffect = CornerPathEffect(10f)
        }
        private var currentRotation = 0f
        private var activeThemeColor = "#00F0FF"

        fun applyThemeState(state: SystemState) {
            activeThemeColor = when (state) {
                SystemState.ERROR -> "#FF0044"
                SystemState.LISTENING -> "#00FF88"
                SystemState.SPEAKING -> "#FF8800"
                else -> "#00F0FF"
            }
            invalidate()
        }

        fun applyThemeState(rotationDelta: Float) {
            currentRotation += rotationDelta * 5f
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = 480f

            canvas.save()
            canvas.rotate(currentRotation, cx, cy)
            
            renderPaint.color = Color.parseColor(activeThemeColor)
            renderPaint.alpha = 35

            // Draw Outer Tick Marks
            for (i in 0 until 36) {
                canvas.drawLine(cx, cy - radius, cx, cy - radius + 20f, renderPaint)
                canvas.rotate(10f, cx, cy)
            }

            // Draw Hexagon Core Indicator
            val hexPath = Path().apply {
                moveTo(cx, cy - radius - 30f)
                lineTo(cx + 30f, cy - radius + 20f)
                lineTo(cx + 15f, cy - radius + 30f)
                lineTo(cx - 15f, cy - radius + 30f)
                lineTo(cx - 30f, cy - radius + 20f)
                close()
            }
            renderPaint.style = Paint.Style.FILL
            canvas.drawPath(hexPath, renderPaint)
            
            renderPaint.style = Paint.Style.STROKE
            canvas.drawCircle(cx, cy, radius + 40f, renderPaint)
            
            canvas.restore()
        }
    }

    // HUD 5: Dynamic Spectrum Audio Analyzer
    inner class SpectrumView(context: Context) : View(context) {
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
        }
        private val audioBars = FloatArray(45) // High density bars
        private var activeThemeColor = "#00FF99"

        fun applyThemeState(state: SystemState) {
            activeThemeColor = when (state) {
                SystemState.SPEAKING -> "#FF8800"
                SystemState.ERROR -> "#FF0044"
                SystemState.CALL_ACTIVE -> "#B000FF"
                else -> "#00FF88"
            }
        }

        // Simulate waveform purely for aesthetic idle movement when not analyzing raw mic data directly
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            renderPaint.color = Color.parseColor(activeThemeColor)
            renderPaint.alpha = 180
            
            val cx = width / 2f
            val cy = height - 250f
            val barSpacing = 22f
            val startX = cx - ((audioBars.size * barSpacing) / 2)

            for (i in audioBars.indices) {
                // Add tiny random jitter for cyberpunk glitch feel
                val jitter = (Math.random() * 10).toFloat()
                canvas.drawLine(startX + (i * barSpacing), cy, startX + (i * barSpacing), cy - audioBars[i] - jitter, renderPaint)
                
                // Decay the bars smoothly
                audioBars[i] *= 0.88f
                
                // If completely silent, give a baseline minimal wave
                if (audioBars[i] < 5f && currentState == SystemState.STANDBY) {
                    audioBars[i] = (Math.sin(System.currentTimeMillis() / 300.0 + i) * 15f).toFloat().coerceAtLeast(2f)
                }
            }
            invalidate()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [13] SENSORS, LIFECYCLE MANAGEMENT & CLEANUP
    // ══════════════════════════════════════════════════════════════════════════════════════════
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                // Stop AI Voice if device is brought to the ear suddenly (Privacy Mode)
                if (event.values[0] < 5f && ttsEngine.isSpeaking) {
                    try { ttsEngine.stop() } catch (e: Exception) { Log.e(TAG, "Proximity stop error", e) }
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                compassHUDView?.applyThemeState(event.values[2])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action required
    }

    override fun onLocationChanged(location: Location) {
        // Reserved for future Navigation API integration
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "System Resumed. Re-engaging Sensors.")
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        proxSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "System Paused. Halting Sensors to save power.")
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "Critical Teardown Initiated. Releasing all resources.")
        isCallModeActive = false
        
        try { unregisterReceiver(backgroundVoiceReceiver) } catch (e: Exception) { Log.w(TAG, "Receiver unregister error", e) }
        try { ttsEngine.shutdown() } catch (e: Exception) { Log.w(TAG, "TTS shutdown error", e) }
        
        // Terminate any active coroutines in the activity scope
        aiProcessingJob?.cancel()
    }
}
