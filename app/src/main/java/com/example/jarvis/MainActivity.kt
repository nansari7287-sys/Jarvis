package com.example.jarvis

// ══════════════════════════════════════════════════════════════════════════════════════════
// J.A.R.V.I.S. TITAN V900.0 — ULTIMATE AI COMPANION (UNIVERSAL MODEL EDITION)
// ══════════════════════════════════════════════════════════════════════════════════════════
// Architect: DrakoXNaeem
// Features: 
// - Universal AI Model Routing (Works with ANY custom model string)
// - Decoupled Intent Architecture (Zero Compilation Errors)
// - Advanced Cyberpunk HUD Engine (Matrix, Radar, Particles, Compass, Spectrum)
// - Full Deep System Automation & App Control
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
// [1] CORE DATA MODELS & ENUMS
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

        // HARDCODED INTENT ACTIONS TO PREVENT BUILD FAILURES (Decoupled from VoiceService)
        const val ACTION_START_CALL = "com.example.jarvis.action.START_CALL"
        const val ACTION_END_CALL = "com.example.jarvis.action.END_CALL"
        const val ACTION_VOICE_COMMAND = "com.example.jarvis.action.VOICE_COMMAND"
        const val ACTION_RESUME_LISTENING = "com.example.jarvis.action.RESUME_LISTENING"
        const val EXTRA_COMMAND = "extra_command"
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
            // SAFE CALL FIX: Added 'intent?' to avoid NullPointerException during runtime
            if (intent?.action == ACTION_VOICE_COMMAND) {
                val transcribedText = intent.getStringExtra(EXTRA_COMMAND) ?: ""
                Log.i(TAG, "Audio Command Received: $transcribedText")
                if (transcribedText.isNotBlank()) {
                    processUserInput(transcribedText)
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
        Log.i(TAG, "Booting Core AI Engines...")
        
        ttsEngine = TextToSpeech(this, this)
        apiPrefs = getSharedPreferences("JarvisTitanVault_V900", Context.MODE_PRIVATE)
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
            Log.e(TAG, "Camera initialization failed", e)
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
            
            val bgRes = res.getIdentifier("jarvis_bg", "drawable", pkg)
            if (bgRes != 0) rootLayout?.setBackgroundResource(bgRes)
            else rootLayout?.setBackgroundColor(Color.parseColor("#030610"))
            
        } catch (e: Exception) {
            Log.e(TAG, "UI Binding Error", e)
            rootLayout?.setBackgroundColor(Color.parseColor("#030610"))
        }
    }

    private fun injectHologramOverlays() {
        try {
            Log.i(TAG, "Injecting Holographic Arrays...")
            
            particleEmitterView = ParticleEmitterView(this)
            rootLayout?.addView(particleEmitterView, 0, FrameLayout.LayoutParams(-1, -1))

            matrixRainView = MatrixRainView(this).apply { alpha = 0.20f }
            rootLayout?.addView(matrixRainView, 1, FrameLayout.LayoutParams(-1, -1))

            radarHUDView = CyberRadarView(this)
            rootLayout?.addView(radarHUDView, 2, FrameLayout.LayoutParams(-1, -1))

            compassHUDView = HoloCompassView(this)
            rootLayout?.addView(compassHUDView, 3, FrameLayout.LayoutParams(-1, -1))

            spectrumAnalyzerView = SpectrumView(this)
            rootLayout?.addView(spectrumAnalyzerView, 4, FrameLayout.LayoutParams(-1, -1))
            
        } catch (e: Exception) {
            Log.e(TAG, "HUD injection failed", e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBackgroundReceivers() {
        val filter = IntentFilter(ACTION_VOICE_COMMAND)
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
            delay(1200)
            updateSystemState(SystemState.ONLINE)
            val provider = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")
            triggerHapticFeedback(100)
            speak("System Online. J.A.R.V.I.S. Core Initialized with $provider. Awaiting your instructions, sir.")
            currentState = SystemState.STANDBY
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [5] INTERACTION LISTENERS & CALL CONTROLS
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun setupInteractionListeners() {
        sendCommandButton?.setOnClickListener {
            val cmd = messageInputBox?.text?.toString()?.trim() ?: ""
            if (cmd.isNotEmpty()) {
                messageInputBox?.text?.clear()
                processUserInput(cmd)
            }
        }

        micToggleButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                return@setOnClickListener
            }
            if (!isCallModeActive) initiateLiveCallSession() else terminateLiveCallSession()
        }

        settingsBtn?.setOnClickListener { openAdvancedSettingsVault() }
    }

    private fun initiateLiveCallSession() {
        Log.i(TAG, "Starting Live Call Session")
        isCallModeActive = true
        updateSystemState(SystemState.CALL_ACTIVE)
        triggerHapticFeedback(150)
        Toast.makeText(this, "🎙️ Secure Audio Link Connected", Toast.LENGTH_SHORT).show()

        // Decoupled Intent calling to avoid missing class errors
        val serviceIntent = Intent().apply {
            setClassName(this@MainActivity, "com.example.jarvis.voice.VoiceService")
            action = ACTION_START_CALL
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Voice Service Not Found", e)
        }

        speak("Yes sir, I am listening. What do you need?")
    }

    private fun terminateLiveCallSession() {
        Log.i(TAG, "Terminating Call Session")
        isCallModeActive = false
        updateSystemState(SystemState.STANDBY)
        triggerHapticFeedback(80)
        
        val serviceIntent = Intent().apply {
            setClassName(this@MainActivity, "com.example.jarvis.voice.VoiceService")
            action = ACTION_END_CALL
        }
        try { startService(serviceIntent) } catch (e: Exception) {}

        speak("Call terminated. Standing by.")
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [6] AI BRAIN PROCESSING & AUTOMATION ROUTING
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun processUserInput(rawInput: String) {
        val userText = rawInput.trim()
        if (userText.isEmpty()) return

        Log.d(TAG, "USER REQUEST: $userText")
        updateSystemState(SystemState.PROCESSING)

        val lowerCaseInput = userText.lowercase(Locale.getDefault())
        if (lowerCaseInput.contains("torch on") || lowerCaseInput.contains("लाइट ऑन")) {
            setHardwareTorch(true); speak("Flashlight enabled."); resumeBackgroundListening(); return
        }
        if (lowerCaseInput.contains("torch off") || lowerCaseInput.contains("लाइट ऑफ")) {
            setHardwareTorch(false); speak("Flashlight disabled."); resumeBackgroundListening(); return
        }
        if (lowerCaseInput.contains("call disconnect") || lowerCaseInput.contains("bye jarvis")) {
            terminateLiveCallSession(); return
        }

        aiProcessingJob?.cancel()
        
        aiProcessingJob = lifecycleScope.launch {
            val providerStr = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI") ?: "GEMINI"
            val providerEnum = try { AIProvider.valueOf(providerStr) } catch (_: Exception) { AIProvider.GEMINI }

            val apiKey = when (providerEnum) {
                AIProvider.GEMINI -> crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: "")
                AIProvider.CHATGPT -> crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: "")
                AIProvider.GROK -> crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: "")
            }.trim()

            // DYNAMIC MODEL SELECTION: Accepts literally any string the user inputs in Settings.
            val rawModelPref = when (providerEnum) {
                AIProvider.GEMINI -> apiPrefs.getString("MODEL_GEMINI", "")
                AIProvider.CHATGPT -> apiPrefs.getString("MODEL_CHATGPT", "")
                AIProvider.GROK -> apiPrefs.getString("MODEL_GROK", "")
            }
            
            val modelName = if (rawModelPref.isNullOrBlank()) {
                when (providerEnum) {
                    AIProvider.GEMINI -> "gemini-2.0-flash"
                    AIProvider.CHATGPT -> "gpt-4o"
                    AIProvider.GROK -> "grok-beta"
                }
            } else {
                rawModelPref.trim()
            }

            if (apiKey.isBlank()) {
                speak("Sir, the API key for $providerStr is missing. Please update your Vault.")
                resumeBackgroundListening()
                return@launch
            }

            try {
                val decision = aiBrain.query(providerEnum, apiKey, modelName, userText)
                executeAgentProtocol(decision)
            } catch (e: Exception) {
                Log.e(TAG, "AI Brain Processing Failure", e)
                speak("I encountered an anomaly in the neural net. Please try again.")
                resumeBackgroundListening()
            }
        }
    }

    private fun executeAgentProtocol(decision: AgentDecision) {
        if (decision.speech.isNotBlank()) {
            speak(decision.speech)
        } else {
            resumeBackgroundListening()
        }

        val accessService = JarvisAccessibilityService.instance

        when (decision.action.uppercase(Locale.getDefault())) {
            "OPEN_APP" -> appAutomation.launchApplicationByName(decision.target)
            
            "WHATSAPP_SEND" -> {
                appAutomation.launchApplicationByName("whatsapp")
                val finalPayload = if (decision.target.isNotBlank()) "${decision.target}: ${decision.payload}" else decision.payload
                accessService?.automateWhatsAppSend(finalPayload)
            }
            
            "INSTAGRAM_SEARCH" -> {
                appAutomation.launchApplicationByName("instagram")
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
                    speak("I require contacts and phone permissions to execute a call.")
                } else {
                    val contactNumber = resolveContactNumber(decision.target)
                    if (contactNumber != null) {
                        speak("Initiating call to ${decision.target}.")
                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$contactNumber"))
                        startActivity(callIntent)
                    } else {
                        speak("I could not locate ${decision.target} in the directory.")
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
            Log.e(TAG, "Contact Search Error", e)
        }
        return foundNumber
    }

    private fun resumeBackgroundListening() {
        if (isCallModeActive) {
            val resumeIntent = Intent(ACTION_RESUME_LISTENING)
            sendBroadcast(resumeIntent)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [7] TEXT-TO-SPEECH (TTS ENGINE)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val langResult = ttsEngine.setLanguage(Locale("hi", "IN"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsEngine.language = Locale.US
                }
                ttsEngine.setSpeechRate(1.02f)
                ttsEngine.setPitch(1.1f)
                isTtsEngineReady = true
            } catch (e: Exception) {
                Log.e(TAG, "TTS Init Error", e)
            }
        }
    }

    private fun speak(textToSpeak: String) {
        if (!isTtsEngineReady || textToSpeak.isBlank()) {
            resumeBackgroundListening()
            return
        }
        updateSystemState(SystemState.SPEAKING)

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
            }
        } catch (_: Exception) {}

        try {
            val utteranceId = "JARVIS_VOICE_${System.currentTimeMillis()}"
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { mainHandler.post { resumeBackgroundListening() } }
                override fun onError(utteranceId: String?) { mainHandler.post { resumeBackgroundListening() } }
            })
            ttsEngine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            resumeBackgroundListening()
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {}

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [8] MULTI-LLM AI BRAIN (UNIVERSAL MODEL ROUTER)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    class MultiLLMBrain {
        private val conversationHistory = mutableListOf<Pair<String, String>>()
        private val maxHistoryContext = 8 

        private val corePersonaInstructions = """
            You are J.A.R.V.I.S., a highly intelligent, efficient, and loyal AI Assistant.
            You communicate in Hindi/Hinglish naturally.

            PROTOCOL ACTIONS:
            - Open App: action="OPEN_APP", target="app_name" 
            - Send WhatsApp: action="WHATSAPP_SEND", target="Name", payload="Message"
            - Search Instagram: action="INSTAGRAM_SEARCH", target="username"
            - Phone Call: action="CALL", target="Name"
            - System Navigation: action="SYSTEM_ACTION", target="HOME" | "BACK" | "RECENTS" | "SWIPE_UP"
            - Chat: action="CHAT"

            STRICT JSON ONLY:
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
                Log.e("JarvisLLM", "API Call Error", e)
                fallbackAgentDecision(userMessage)
            }
        }

        private fun callGeminiAPI(key: String, model: String, msg: String): AgentDecision {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            val contentsArray = JSONArray()
            for (historyItem in conversationHistory) {
                contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", historyItem.first))))
                contentsArray.put(JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", historyItem.second))))
            }
            val promptContent = if (conversationHistory.isEmpty()) "$corePersonaInstructions\n\nUser: $msg" else msg
            contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", promptContent))))

            val payloadJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payloadJson.toString()) }

            if (connection.responseCode == 200) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val rawText = JSONObject(responseString).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
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
                val rawText = JSONObject(responseString).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
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
                if (startIdx != -1 && endIdx > startIdx) JSONObject(rawStr.substring(startIdx, endIdx + 1)) else JSONObject(rawStr)
            } catch (e: Exception) { null }
        }

        private fun updateContextHistory(userStr: String, aiStr: String) {
            conversationHistory.add(Pair(userStr, aiStr))
            if (conversationHistory.size > maxHistoryContext) conversationHistory.removeAt(0)
        }

        private fun buildDecisionObject(json: JSONObject): AgentDecision {
            return AgentDecision(
                action = json.optString("action", "CHAT"),
                target = json.optString("target", ""),
                payload = json.optString("payload", ""),
                speech = json.optString("speech", "Sir, request processed.")
            )
        }

        private fun fallbackAgentDecision(msg: String): AgentDecision {
            val lower = msg.lowercase(Locale.getDefault())
            return when {
                lower.contains("instagram") -> AgentDecision("OPEN_APP", "instagram", "", "Opening Instagram.")
                lower.contains("whatsapp") -> AgentDecision("OPEN_APP", "whatsapp", "", "Accessing WhatsApp.")
                lower.contains("youtube") -> AgentDecision("OPEN_APP", "youtube", "", "Launching YouTube.")
                lower.contains("home") -> AgentDecision("SYSTEM_ACTION", "HOME", "", "Returning to Home.")
                else -> AgentDecision("CHAT", "", "", "Network issue detected. Please retry.")
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [9] HARDWARE AUTOMATION & UTILITIES
    // ══════════════════════════════════════════════════════════════════════════════════════════
    inner class AppAutomation(private val ctx: Context) {
        fun launchApplication(packageName: String) {
            try {
                ctx.packageManager.getLaunchIntentForPackage(packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(it)
                }
            } catch (_: Exception) {}
        }

        fun launchApplicationByName(appName: String) {
            val query = appName.lowercase(Locale.getDefault())
            when {
                query.contains("whatsapp") -> launchApplication("com.whatsapp")
                query.contains("instagram") -> launchApplication("com.instagram.android")
                query.contains("youtube") -> launchApplication("com.google.android.youtube")
                query.contains("facebook") -> launchApplication("com.facebook.katana")
                query.contains("telegram") -> launchApplication("org.telegram.messenger")
                query.contains("spotify") -> launchApplication("com.spotify.music")
                query.contains("chrome") -> launchApplication("com.android.chrome")
                else -> {
                    try {
                        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                        val apps = ctx.packageManager.queryIntentActivities(mainIntent, 0)
                        for (appInfo in apps) {
                            val label = appInfo.loadLabel(ctx.packageManager).toString().lowercase(Locale.getDefault())
                            if (label.contains(query)) {
                                launchApplication(appInfo.activityInfo.packageName)
                                return
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    inner class HybridCrypto {
        private val securityToken = "JARVIS_TITAN_V900_SECURE_VAULT_KEY_!".toByteArray().copyOf(32)
        
        fun encrypt(plainData: String): String = try {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(securityToken, "AES"))
            Base64.encodeToString(cipher.doFinal(plainData.toByteArray()), Base64.DEFAULT)
        } catch (_: Exception) { "" }

        fun decrypt(encryptedData: String): String {
            if (encryptedData.isBlank()) return ""
            return try {
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(securityToken, "AES"))
                String(cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT)))
            } catch (_: Exception) { "" }
        }
    }

    private fun setHardwareTorch(enable: Boolean) {
        if (mainCameraId == null) return
        try { cameraManager?.setTorchMode(mainCameraId!!, enable) } catch (_: Exception) {}
    }

    private fun triggerHapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [10] ADVANCED SETTINGS VAULT (UNIVERSAL MODEL UI)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun openAdvancedSettingsVault() {
        val rootScrollView = ScrollView(this).apply { layoutParams = ViewGroup.LayoutParams(-1, -1) }
        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 80)
            setBackgroundColor(Color.parseColor("#060B18"))
        }

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
        providerRadioGroup.addView(rbGemini); providerRadioGroup.addView(rbChatGpt); providerRadioGroup.addView(rbGrok)
        containerLayout.addView(providerRadioGroup)

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

        // Users can type ANY model string here, overriding the hints.
        val (lblGeminiKey, etGeminiKey) = buildInputField("GEMINI API KEY", "AIza...", crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: ""))
        val (lblGeminiModel, etGeminiModel) = buildInputField("GEMINI MODEL", "gemini-2.0-flash", apiPrefs.getString("MODEL_GEMINI", "") ?: "")
        
        val (lblGptKey, etGptKey) = buildInputField("OPENAI API KEY", "sk-...", crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: ""))
        val (lblGptModel, etGptModel) = buildInputField("OPENAI MODEL", "gpt-4o / gpt-4-turbo", apiPrefs.getString("MODEL_CHATGPT", "") ?: "")
        
        val (lblGrokKey, etGrokKey) = buildInputField("GROK API KEY", "xai-...", crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: ""))
        val (lblGrokModel, etGrokModel) = buildInputField("GROK MODEL", "grok-beta / grok-2", apiPrefs.getString("MODEL_GROK", "") ?: "")

        containerLayout.addView(lblGeminiKey); containerLayout.addView(etGeminiKey)
        containerLayout.addView(lblGeminiModel); containerLayout.addView(etGeminiModel)
        containerLayout.addView(lblGptKey); containerLayout.addView(etGptKey)
        containerLayout.addView(lblGptModel); containerLayout.addView(etGptModel)
        containerLayout.addView(lblGrokKey); containerLayout.addView(etGrokKey)
        containerLayout.addView(lblGrokModel); containerLayout.addView(etGrokModel)

        val saveButton = Button(this).apply {
            text = "INITIALIZE CORE OVERRIDE"
            setBackgroundColor(Color.parseColor("#00F0FF"))
            setTextColor(Color.parseColor("#020610"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 60; bottomMargin = 20
            }
        }
        containerLayout.addView(saveButton)
        rootScrollView.addView(containerLayout)

        val alertDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth)
            .setView(rootScrollView).create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        saveButton.setOnClickListener {
            val selectedEngine = when {
                rbChatGpt.isChecked -> "CHATGPT"
                rbGrok.isChecked -> "GROK"
                else -> "GEMINI"
            }
            apiPrefs.edit().apply {
                putString("ACTIVE_PROVIDER", selectedEngine)
                putString("API_GEMINI", crypto.encrypt(etGeminiKey.text.toString().trim()))
                putString("MODEL_GEMINI", etGeminiModel.text.toString().trim())
                putString("API_CHATGPT", crypto.encrypt(etGptKey.text.toString().trim()))
                putString("MODEL_CHATGPT", etGptModel.text.toString().trim())
                putString("API_GROK", crypto.encrypt(etGrokKey.text.toString().trim()))
                putString("MODEL_GROK", etGrokModel.text.toString().trim())
            }.apply()
            
            triggerHapticFeedback(200)
            alertDialog.dismiss()
            Toast.makeText(this, "Core Overwritten: $selectedEngine", Toast.LENGTH_LONG).show()
            speak("Configurations updated. The system will now use $selectedEngine.")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [11] SYSTEM STATE MANAGEMENT & THEME ORCHESTRATION
    // ══════════════════════════════════════════════════════════════════════════════════════════
    private fun updateSystemState(newState: SystemState) {
        currentState = newState
        val targetColorHex = when (newState) {
            SystemState.PROCESSING -> "#2800D4FF"
            SystemState.LISTENING -> "#2800FF88"
            SystemState.SPEAKING -> "#28FF8800"
            SystemState.CALL_ACTIVE -> "#25B000FF"
            SystemState.ERROR -> "#40FF0000"
            else -> "#00000000"
        }

        try {
            rootLayout?.let { layout ->
                ObjectAnimator.ofArgb(layout, "backgroundColor", Color.parseColor(targetColorHex)).apply {
                    duration = 600; interpolator = AccelerateDecelerateInterpolator(); start()
                }
            }
        } catch (e: Exception) {}

        try {
            matrixRainView?.applyThemeState(newState)
            radarHUDView?.applyThemeState(newState)
            particleEmitterView?.applyThemeState(newState)
            compassHUDView?.applyThemeState(newState)
            spectrumAnalyzerView?.applyThemeState(newState)
        } catch (e: Exception) {}
    }

    // ══════════════════════════════════════════════════════════════════════════════════════════
    // [12] ENTERPRISE CYBERPUNK HUD GRAPHICS (MASSIVE CUSTOM VIEWS)
    // ══════════════════════════════════════════════════════════════════════════════════════════
    inner class MatrixRainView(context: Context) : View(context) {
        private val randomGen = Random()
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textSize = 24f
        }
        private val codeDrops = Array(180) {
            floatArrayOf(
                randomGen.nextFloat() * 2500f, randomGen.nextFloat() * -4000f, 
                randomGen.nextFloat() * 18f + 8f, randomGen.nextFloat() * 10f + 5f, randomGen.nextFloat() * 255f
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
                for (i in 0..drop[3].toInt()) {
                    val yPos = drop[1] - (i * 24f)
                    val alpha = Math.max(0, drop[4].toInt() - (i * 20))
                    renderPaint.alpha = alpha
                    canvas.drawText(randomGen.nextInt(10).toString(), drop[0], yPos, renderPaint)
                }
                drop[1] += drop[2]
                if (drop[1] > height + 200f) {
                    drop[1] = -200f
                    drop[0] = randomGen.nextFloat() * width
                    drop[2] = randomGen.nextFloat() * 18f + 8f
                    drop[4] = randomGen.nextFloat() * 155f + 100f 
                }
            }
            invalidate()
        }
    }

    inner class CyberRadarView(context: Context) : View(context) {
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
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

            strokePaint.color = parsedColor; strokePaint.alpha = 50
            canvas.drawCircle(cx, cy, maxRadius, strokePaint)
            canvas.drawCircle(cx, cy, maxRadius * 0.75f, strokePaint)
            canvas.drawCircle(cx, cy, maxRadius * 0.45f, strokePaint)

            canvas.save()
            canvas.rotate(gridRotation, cx, cy)
            strokePaint.alpha = 25
            canvas.drawLine(cx - maxRadius, cy, cx + maxRadius, cy, strokePaint)
            canvas.drawLine(cx, cy - maxRadius, cx, cy + maxRadius, strokePaint)
            canvas.restore()
            gridRotation += 0.2f

            fillPaint.color = parsedColor; fillPaint.alpha = 40
            canvas.drawArc(RectF(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius), sweepAngle, 65f, true, fillPaint)
            sweepAngle = (sweepAngle + 5.5f) % 360f
            invalidate()
        }
    }

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

                particle.x += particle.velocityX
                particle.y += particle.velocityY
                particle.lifespan -= particle.fadeRate

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

    inner class HoloCompassView(context: Context) : View(context) {
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            style = Paint.Style.STROKE; strokeWidth = 4f; pathEffect = CornerPathEffect(10f)
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
            renderPaint.color = Color.parseColor(activeThemeColor); renderPaint.alpha = 35

            for (i in 0 until 36) {
                canvas.drawLine(cx, cy - radius, cx, cy - radius + 20f, renderPaint)
                canvas.rotate(10f, cx, cy)
            }

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

    inner class SpectrumView(context: Context) : View(context) {
        private val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 8f; strokeCap = Paint.Cap.ROUND }
        private val audioBars = FloatArray(45)
        private var activeThemeColor = "#00FF99"

        fun applyThemeState(state: SystemState) {
            activeThemeColor = when (state) {
                SystemState.SPEAKING -> "#FF8800"
                SystemState.ERROR -> "#FF0044"
                SystemState.CALL_ACTIVE -> "#B000FF"
                else -> "#00FF88"
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            renderPaint.color = Color.parseColor(activeThemeColor)
            renderPaint.alpha = 180
            
            val cx = width / 2f
            val cy = height - 250f
            val barSpacing = 22f
            val startX = cx - ((audioBars.size * barSpacing) / 2)

            for (i in audioBars.indices) {
                val jitter = (Math.random() * 10).toFloat()
                canvas.drawLine(startX + (i * barSpacing), cy, startX + (i * barSpacing), cy - audioBars[i] - jitter, renderPaint)
                audioBars[i] *= 0.88f
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
                if (event.values[0] < 5f && ttsEngine.isSpeaking) {
                    try { ttsEngine.stop() } catch (e: Exception) {}
                }
            }
            Sensor.TYPE_GYROSCOPE -> compassHUDView?.applyThemeState(event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onLocationChanged(location: Location) {}

    override fun onResume() {
        super.onResume()
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        proxSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isCallModeActive = false
        try { unregisterReceiver(backgroundVoiceReceiver) } catch (e: Exception) {}
        try { ttsEngine.shutdown() } catch (e: Exception) {}
        aiProcessingJob?.cancel()
    }
}
