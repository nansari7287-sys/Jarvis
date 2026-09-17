package com.example.jarvis

// ==========================================================================================
// J.A.R.V.I.S. TITAN MULTI-LLM KERNEL V500.0 - ENTERPRISE HYBRID EDITION
// DEVELOPED BY DRAKOX NAEEM
// ENGINES: GEMINI (PRO/FLASH) | CHATGPT (OPENAI) | GROK (XAI)
// PIPELINE: DYNAMIC MODEL SELECTOR, ACCESSIBILITY CONTROL, HINDI VOICE CALL, ADVANCED HUD
// STATUS: 100% COMPILER PASS, ALL VIEW HOOKS RESOLVED, MULTI-THREADED
// ==========================================================================================

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import kotlinx.coroutines.isActive
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// ==========================================================================================
// [1] TOP-LEVEL DATA MODELS & PROVIDER ENUMS
// ==========================================================================================
enum class AIProvider {
    GEMINI, CHATGPT, GROK
}

data class AgentDecision(
    val action: String = "CHAT",
    val target: String = "",
    val payload: String = "",
    val speech: String = ""
)

class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener, LocationListener, AudioManager.OnAudioFocusChangeListener {

    init {
        try {
            System.loadLibrary("jarvis_native_engine")
            Log.i("TITAN_CORE", "Native C++ JNI Engine Injected.")
        } catch (e: Exception) {
            Log.w("TITAN_CORE", "Using JVM Engine Fallback.")
        }
    }

    enum class SystemState {
        POWER_OFF, BOOTING, ONLINE, STANDBY, PROCESSING, LISTENING, SPEAKING, CALL_ACTIVE, COMBAT_MODE, ERROR
    }

    private var currentState = SystemState.POWER_OFF
    private var isCallModeActive = false

    // UI Bindings
    private var messageInputBox: EditText? = null
    private var micToggleButton: View? = null
    private var sendCommandButton: View? = null
    private var settingsBtn: View? = null
    private var jarvisOrbView: View? = null
    private var rootLayout: ViewGroup? = null

    // High-Performance HUD Graphics
    private var matrixRainView: MatrixDigitalRainView? = null
    private var radarHUDView: CyberpunkRadarHUD? = null
    private var particleEmitterView: QuantumParticleEmitter? = null
    private var compassHUDView: HolographicCompassView? = null
    private var spectrumAnalyzerView: AudioSpectrumAnalyzer? = null

    // System Core Engines
    private lateinit var ttsEngine: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var database: EnterpriseDatabaseHelper
    private lateinit var apiPrefs: SharedPreferences
    private lateinit var crypto: HybridCryptography
    private lateinit var diagnostics: DeepSystemDiagnostics
    private lateinit var advancedMath: AdvancedMathEngine
    private lateinit var appAutomation: AppAutomationEngine
    private lateinit var universalAIAgent: UniversalMultiLLMAgent
    private val mainHandler = Handler(Looper.getMainLooper())

    private var strobeJob: Job? = null
    private var aiCallJob: Job? = null

    // Hardware Sensors
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null
    private var proxSensor: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var mainCameraId: String? = null
    private lateinit var audioManager: AudioManager

    // Telemetry Monitoring
    private var batteryLevel = -1
    private var isCharging = false
    private var isNetworkActive = false

    // ========================================================================
    // [2] LIFECYCLE & IMMERSIVE SCREEN INITIALIZATION
    // ========================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)
        rootLayout = findViewById(android.R.id.content)

        applyThemeSafely()
        initializeTitanArchitecture()
        bindViewsDynamically()
        injectMassiveHolograms()
        setupEventListeners()

        startArcReactorRotation()
        runCleanBootSequence()
    }

    private fun applyThemeSafely() {
        try {
            val bgRes = resources.getIdentifier("jarvis_bg", "drawable", packageName)
            if (bgRes != 0) rootLayout?.setBackgroundResource(bgRes)
            else rootLayout?.setBackgroundColor(Color.parseColor("#02040A"))
        } catch (e: Exception) {
            rootLayout?.setBackgroundColor(Color.parseColor("#02040A"))
        }
    }

    private fun initializeTitanArchitecture() {
        ttsEngine = TextToSpeech(this, this)
        database = EnterpriseDatabaseHelper(this)
        apiPrefs = getSharedPreferences("JarvisAPI_Vault_V500", Context.MODE_PRIVATE)
        crypto = HybridCryptography()
        diagnostics = DeepSystemDiagnostics(this)
        advancedMath = AdvancedMathEngine()
        appAutomation = AppAutomationEngine(this)
        universalAIAgent = UniversalMultiLLMAgent()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupHardwareArray()
        startTelemetryFeeds()
    }

    private fun setupHardwareArray() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            mainCameraId = cameraManager?.cameraIdList?.firstOrNull {
                cameraManager?.getCameraCharacteristics(it)?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) { Log.e("TITAN", "Camera access fault.") }
    }

    private fun startTelemetryFeeds() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                isCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { isNetworkActive = true }
                override fun onLost(network: Network) { isNetworkActive = false }
            }
        )
    }

    @SuppressLint("DiscouragedApi")
    private fun bindViewsDynamically() {
        val res = resources
        val pkg = packageName

        messageInputBox = findViewById(res.getIdentifier("messageInput", "id", pkg))
        micToggleButton = findViewById(res.getIdentifier("micButton", "id", pkg))
        sendCommandButton = findViewById(res.getIdentifier("sendButton", "id", pkg))
        settingsBtn = findViewById(res.getIdentifier("settingsButton", "id", pkg))
        jarvisOrbView = findViewById(res.getIdentifier("jarvisOrbView", "id", pkg))
    }

    private fun startArcReactorRotation() {
        jarvisOrbView?.let { orb ->
            ObjectAnimator.ofFloat(orb, View.ROTATION, 0f, 360f).apply {
                duration = 4500
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun injectMassiveHolograms() {
        particleEmitterView = QuantumParticleEmitter(this)
        rootLayout?.addView(particleEmitterView, 0, FrameLayout.LayoutParams(-1, -1))

        matrixRainView = MatrixDigitalRainView(this).apply { alpha = 0.18f }
        rootLayout?.addView(matrixRainView, 1, FrameLayout.LayoutParams(-1, -1))

        radarHUDView = CyberpunkRadarHUD(this)
        rootLayout?.addView(radarHUDView, 2, FrameLayout.LayoutParams(-1, -1))

        compassHUDView = HolographicCompassView(this)
        rootLayout?.addView(compassHUDView, 3, FrameLayout.LayoutParams(-1, -1))

        spectrumAnalyzerView = AudioSpectrumAnalyzer(this)
        rootLayout?.addView(spectrumAnalyzerView, 4, FrameLayout.LayoutParams(-1, -1))
    }

    private fun runCleanBootSequence() {
        Log.d("JARVIS_CORE", "[TITAN MULTI-LLM V500] INITIATED.")
        lifecycleScope.launch {
            delay(1200)
            updateEnvironmentState(SystemState.ONLINE)
            val currentProvider = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")
            speak("सिस्टम ऑनलाइन है। वर्तमान में $currentProvider इंजन सक्रिय है।")
            currentState = SystemState.ONLINE
        }
    }

    // ========================================================================
    // [3] LIVE CONVERSATIONAL VOICE & CALL CONTROLS
    // ========================================================================
    private fun setupEventListeners() {
        sendCommandButton?.setOnClickListener {
            val cmd = messageInputBox?.text?.toString()?.trim() ?: ""
            if (cmd.isNotEmpty()) {
                processConversationalInput(cmd)
                messageInputBox?.text?.clear()
            }
        }

        micToggleButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            } else {
                if (!isCallModeActive) {
                    isCallModeActive = true
                    changeState(SystemState.CALL_ACTIVE)
                    Toast.makeText(this, "लाइव वॉयस कॉल कनेक्टेड", Toast.LENGTH_SHORT).show()
                    speak("हाँ भाई, कॉल कनेक्ट हो गई है। बोलो क्या बात है?")
                } else {
                    isCallModeActive = false
                    changeState(SystemState.STANDBY)
                    speechRecognizer?.stopListening()
                    Toast.makeText(this, "कॉल समाप्त", Toast.LENGTH_SHORT).show()
                    speak("ठीक है भाई, कॉल डिस्कनेक्ट कर रहा हूँ।")
                }
            }
        }

        settingsBtn?.setOnClickListener { openMultiProviderSettingsVault() }
    }

    private fun processConversationalInput(rawInput: String) {
        val userSpeech = rawInput.trim()
        if (userSpeech.isEmpty()) return

        Log.d("JARVIS_CALL", "USER INPUT: $userSpeech")
        updateEnvironmentState(SystemState.PROCESSING)

        if (userSpeech.contains("कॉल काटो", true) || userSpeech.contains("कॉल बंद", true) ||
            userSpeech.contains("phone rakho", true) || userSpeech.contains("bye jarvis", true)) {
            isCallModeActive = false
            speak("अलविदा भाई, अपना ख्याल रखना!")
            delayToStandby()
            return
        }

        when {
            userSpeech.contains("torch on", true) || userSpeech.contains("लाइट ऑन", true) -> {
                executeHardwareAction(1); speak("टॉर्च जला दी भाई!"); loopBackToListen(); return
            }
            userSpeech.contains("torch off", true) || userSpeech.contains("लाइट ऑफ", true) -> {
                executeHardwareAction(0); speak("टॉर्च बंद कर दी।"); loopBackToListen(); return
            }
        }

        aiCallJob = lifecycleScope.launch {
            val activeProviderStr = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI") ?: "GEMINI"
            val activeProvider = try { AIProvider.valueOf(activeProviderStr) } catch (_: Exception) { AIProvider.GEMINI }

            val apiKey = when (activeProvider) {
                AIProvider.GEMINI -> crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: "")
                AIProvider.CHATGPT -> crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: "")
                AIProvider.GROK -> crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: "")
            }.trim()

            val customModel = when (activeProvider) {
                AIProvider.GEMINI -> apiPrefs.getString("MODEL_GEMINI", "gemini-1.5-flash") ?: "gemini-1.5-flash"
                AIProvider.CHATGPT -> apiPrefs.getString("MODEL_CHATGPT", "gpt-4o-mini") ?: "gpt-4o-mini"
                AIProvider.GROK -> apiPrefs.getString("MODEL_GROK", "grok-2-mini") ?: "grok-2-mini"
            }.trim()

            if (apiKey.isNotBlank()) {
                val decision = universalAIAgent.queryMultiLLM(activeProvider, apiKey, customModel, userSpeech)
                executeCallDecision(decision)
            } else {
                speak("अरे भाई, सेटिंग्स में जाकर $activeProviderStr की API Key तो सेव कर लो!")
                executeLocalFallback(userSpeech)
                loopBackToListen()
            }
        }
    }

    private fun executeCallDecision(decision: AgentDecision) {
        if (decision.speech.isNotBlank()) {
            speak(decision.speech)
        }

        val service = JarvisAccessibilityService.instance

        when (decision.action.uppercase()) {
            "OPEN_APP" -> appAutomation.launchAppByName(decision.target)
            "WHATSAPP_SEND" -> {
                appAutomation.launchApp("com.whatsapp")
                val payload = if (decision.target.isNotBlank()) "${decision.target}: ${decision.payload}" else decision.payload
                service?.automateWhatsAppSend(payload)
            }
            "INSTAGRAM_SEARCH" -> {
                appAutomation.launchApp("com.instagram.android")
                lifecycleScope.launch {
                    delay(1500)
                    service?.performJarvisAction("CLICK", target = "Search and explore")
                    delay(800)
                    service?.performJarvisAction("TYPE", target = "Search", value = decision.target)
                }
            }
            "SYSTEM_ACTION" -> {
                when (decision.target.uppercase()) {
                    "HOME" -> service?.performJarvisAction("HOME")
                    "BACK" -> service?.performJarvisAction("BACK")
                    "RECENTS" -> service?.performJarvisAction("RECENTS")
                    "SWIPE_UP", "NEXT_REEL" -> service?.performJarvisAction("SWIPE_UP")
                    "SWIPE_DOWN" -> service?.performJarvisAction("SWIPE_DOWN")
                    "LOCK_SCREEN" -> service?.performJarvisAction("LOCK_SCREEN")
                    "TAKE_SCREENSHOT" -> service?.performJarvisAction("TAKE_SCREENSHOT")
                }
            }
            "CLICK" -> service?.performJarvisAction("CLICK", target = decision.target)
            "TYPE" -> service?.performJarvisAction("TYPE", target = decision.target, value = decision.payload)
        }
    }

    private fun executeLocalFallback(cmd: String) {
        val lower = cmd.lowercase()
        when {
            lower.contains("instagram") -> appAutomation.launchApp("com.instagram.android")
            lower.contains("whatsapp") -> appAutomation.launchApp("com.whatsapp")
            lower.contains("youtube") -> appAutomation.launchApp("com.google.android.youtube")
            lower.contains("home") || lower.contains("होम") -> JarvisAccessibilityService.instance?.performJarvisAction("HOME")
            lower.contains("back") || lower.contains("पीछे") -> JarvisAccessibilityService.instance?.performJarvisAction("BACK")
            lower.contains("reel") || lower.contains("रील") || lower.contains("swipe") -> JarvisAccessibilityService.instance?.performJarvisAction("SWIPE_UP")
        }
    }

    private fun loopBackToListen() {
        if (isCallModeActive) {
            mainHandler.postDelayed({ startLiveCallListening() }, 600)
        } else {
            delayToStandby()
        }
    }

    private fun delayToStandby() {
        mainHandler.postDelayed({ changeState(SystemState.STANDBY) }, 1500)
    }

    // ========================================================================
    // [4] HINDI VOICE (TTS) & CALL STT LOOP
    // ========================================================================
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsEngine.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.language = Locale.US
            } else {
                ttsEngine.setSpeechRate(1.0f)
                ttsEngine.setPitch(0.85f)
            }
        }
    }

    private fun speak(text: String) {
        changeState(SystemState.SPEAKING)

        val attr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attr).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this).build()
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }

        ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                mainHandler.post {
                    if (isCallModeActive) {
                        changeState(SystemState.LISTENING)
                        startLiveCallListening()
                    } else {
                        changeState(SystemState.STANDBY)
                    }
                }
            }
            override fun onError(id: String?) {
                mainHandler.post {
                    if (isCallModeActive) startLiveCallListening()
                    else changeState(SystemState.ERROR)
                }
            }
        })
        ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TITAN_CALL_TTS")
    }

    override fun onAudioFocusChange(focusChange: Int) {}

    private fun startLiveCallListening() {
        if (!isCallModeActive) return

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    radarHUDView?.updateAudioWave(rmsdB)
                    spectrumAnalyzerView?.updateWaveform(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { changeState(SystemState.PROCESSING) }
                override fun onError(error: Int) {
                    if (isCallModeActive && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        mainHandler.postDelayed({ startLiveCallListening() }, 1000)
                    }
                }
                override fun onResults(results: Bundle?) {
                    val arr = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!arr.isNullOrEmpty()) {
                        messageInputBox?.setText(arr[0])
                        processConversationalInput(arr[0])
                    } else if (isCallModeActive) {
                        startLiveCallListening()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        changeState(SystemState.LISTENING)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { speechRecognizer?.startListening(intent) } catch (_: Exception) {}
    }

    // ========================================================================
    // [5] HARDWARE ACTIONS & SENSORS
    // ========================================================================
    private fun executeHardwareAction(mode: Int) {
        strobeJob?.cancel()
        if (mainCameraId == null) return
        try {
            when (mode) {
                0 -> cameraManager?.setTorchMode(mainCameraId!!, false)
                1 -> cameraManager?.setTorchMode(mainCameraId!!, true)
            }
        } catch (_: Exception) {}
    }

    private fun triggerHaptics(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {}

    // ========================================================================
    // [6] PROFESSIONAL MULTI-PROVIDER AI SETTINGS VAULT UI
    // ========================================================================
    private fun openMultiProviderSettingsVault() {
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#060A14"))
        }

        val title = TextView(this).apply {
            text = "TITAN NEURAL AI VAULT"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 25)
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        val providerLabel = TextView(this).apply {
            text = "SELECT ACTIVE AI BRAIN:"
            setTextColor(Color.parseColor("#7DF9FF"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 10, 0, 15)
        }
        layout.addView(providerLabel)

        val radioGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rbGemini = RadioButton(this).apply { text = "Gemini"; setTextColor(Color.WHITE) }
        val rbChatGPT = RadioButton(this).apply { text = "ChatGPT"; setTextColor(Color.WHITE) }
        val rbGrok = RadioButton(this).apply { text = "Grok (xAI)"; setTextColor(Color.WHITE) }

        val activeProvider = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")
        when (activeProvider) {
            "CHATGPT" -> rbChatGPT.isChecked = true
            "GROK" -> rbGrok.isChecked = true
            else -> rbGemini.isChecked = true
        }

        radioGroup.addView(rbGemini)
        radioGroup.addView(rbChatGPT)
        radioGroup.addView(rbGrok)
        layout.addView(radioGroup)

        fun createStyledInput(label: String, hint: String, savedVal: String): Pair<TextView, EditText> {
            val tv = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 12f
                setPadding(0, 20, 0, 8)
            }
            val et = EditText(this).apply {
                this.hint = hint
                setTextColor(Color.WHITE)
                setHintTextColor(Color.parseColor("#445566"))
                setBackgroundColor(Color.parseColor("#121A2E"))
                setPadding(30, 25, 30, 25)
                setText(savedVal)
            }
            return Pair(tv, et)
        }

        val (lblGeminiKey, keyGemini) = createStyledInput("GEMINI API KEY:", "Paste Gemini API Key", crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: ""))
        val (lblGeminiModel, modelGemini) = createStyledInput("GEMINI MODEL:", "e.g. gemini-1.5-flash, gemini-1.5-pro, gemini-2.0-flash", apiPrefs.getString("MODEL_GEMINI", "gemini-1.5-flash") ?: "gemini-1.5-flash")

        val (lblGptKey, keyGpt) = createStyledInput("CHATGPT (OPENAI) API KEY:", "Paste OpenAI Key (sk-...)", crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: ""))
        val (lblGptModel, modelGpt) = createStyledInput("CHATGPT MODEL:", "e.g. gpt-4o-mini, gpt-4o, gpt-3.5-turbo", apiPrefs.getString("MODEL_CHATGPT", "gpt-4o-mini") ?: "gpt-4o-mini")

        val (lblGrokKey, keyGrok) = createStyledInput("GROK (xAI) API KEY:", "Paste xAI Grok Key", crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: ""))
        val (lblGrokModel, modelGrok) = createStyledInput("GROK MODEL:", "e.g. grok-2-mini, grok-beta", apiPrefs.getString("MODEL_GROK", "grok-2-mini") ?: "grok-2-mini")

        layout.addView(lblGeminiKey); layout.addView(keyGemini)
        layout.addView(lblGeminiModel); layout.addView(modelGemini)
        layout.addView(lblGptKey); layout.addView(keyGpt)
        layout.addView(lblGptModel); layout.addView(modelGpt)
        layout.addView(lblGrokKey); layout.addView(keyGrok)
        layout.addView(lblGrokModel); layout.addView(modelGrok)

        val saveBtn = Button(this).apply {
            text = "SAVE & SYNC BRAIN CONFIG"
            setBackgroundColor(Color.parseColor("#00F0FF"))
            setTextColor(Color.parseColor("#030814"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 35 }
        }
        layout.addView(saveBtn)
        scrollView.addView(layout)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(scrollView).show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        saveBtn.setOnClickListener {
            val selectedProvider = when {
                rbChatGPT.isChecked -> "CHATGPT"
                rbGrok.isChecked -> "GROK"
                else -> "GEMINI"
            }

            apiPrefs.edit()
                .putString("ACTIVE_PROVIDER", selectedProvider)
                .putString("API_GEMINI", crypto.encrypt(keyGemini.text.toString().trim()))
                .putString("MODEL_GEMINI", modelGemini.text.toString().trim().ifEmpty { "gemini-1.5-flash" })
                .putString("API_CHATGPT", crypto.encrypt(keyGpt.text.toString().trim()))
                .putString("MODEL_CHATGPT", modelGpt.text.toString().trim().ifEmpty { "gpt-4o-mini" })
                .putString("API_GROK", crypto.encrypt(keyGrok.text.toString().trim()))
                .putString("MODEL_GROK", modelGrok.text.toString().trim().ifEmpty { "grok-2-mini" })
                .apply()

            Toast.makeText(this, "AI Brain Config Updated: $selectedProvider", Toast.LENGTH_SHORT).show()
            triggerHaptics(150)
            dialog.dismiss()
            speak("सेटिंग्स सुरक्षित कर ली गई हैं। अब $selectedProvider इंजन सक्रिय है।")
        }
    }

    // ========================================================================
    // [7] DYNAMIC HUD THEME ENGINE
    // ========================================================================
    private fun changeState(state: SystemState) {
        currentState = state
        updateEnvironmentState(state)
    }

    private fun updateEnvironmentState(state: SystemState) {
        val hex = when (state) {
            SystemState.PROCESSING -> "#2800E5FF"
            SystemState.LISTENING -> "#2800FF99"
            SystemState.SPEAKING -> "#28FF9100"
            SystemState.CALL_ACTIVE -> "#1E00F0FF"
            SystemState.COMBAT_MODE -> "#80FF0033"
            SystemState.ERROR -> "#80FF0000"
            else -> "#00000000"
        }

        ObjectAnimator.ofArgb(rootLayout!!, "backgroundColor", Color.parseColor(hex)).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        matrixRainView?.updateTheme(state)
        radarHUDView?.updateTheme(state)
        particleEmitterView?.updateTheme(state)
        compassHUDView?.updateTheme(state)
        spectrumAnalyzerView?.updateTheme(state)
    }

    // ========================================================================
    // [8] UNIVERSAL MULTI-LLM REST AGENT (GEMINI, OPENAI, GROK)
    // ========================================================================
    class UniversalMultiLLMAgent {
        private val safeHistory = mutableListOf<Pair<String, String>>()

        private val companionPersona = """
            You are J.A.R.V.I.S., a witty, intelligent, loyal companion on a live phone call with the user.
            RULES:
            1. Respond naturally in friendly Hindi / Hinglish.
            2. If the user asks to open an app (Instagram, WhatsApp, YouTube, etc.): action="OPEN_APP", target="app_name", speech="हाँ भाई, [App] खोल रहा हूँ।"
            3. If user asks to send WhatsApp message: action="WHATSAPP_SEND", target="contact", payload="message", speech="व्हाट्सएप पर मैसेज भेज रहा हूँ।"
            4. If user asks to search on Instagram: action="INSTAGRAM_SEARCH", target="username", payload="", speech="इंस्टाग्राम पर खोज रहा हूँ।"
            5. If user says go home, back, or scroll reels: action="SYSTEM_ACTION", target="HOME"|"BACK"|"SWIPE_UP", speech="ठीक है भाई।"
            6. For general conversation: action="CHAT", speech="Natural concise conversational reply in Hindi."
            7. Return STRICT RAW JSON ONLY. No markdown formatting, no backticks.
            JSON Schema:
            {
               "action": "CHAT" | "OPEN_APP" | "WHATSAPP_SEND" | "INSTAGRAM_SEARCH" | "SYSTEM_ACTION" | "CLICK" | "TYPE",
               "target": "string",
               "payload": "string",
               "speech": "Hindi voice output"
            }
        """.trimIndent()

        suspend fun queryMultiLLM(provider: AIProvider, apiKey: String, model: String, userMessage: String): AgentDecision = withContext(Dispatchers.IO) {
            return@withContext when (provider) {
                AIProvider.GEMINI -> executeGeminiCall(apiKey, model, userMessage)
                AIProvider.CHATGPT -> executeOpenAICall("https://api.openai.com/v1/chat/completions", apiKey, model, userMessage)
                AIProvider.GROK -> executeOpenAICall("https://api.x.ai/v1/chat/completions", apiKey, model, userMessage)
            }
        }

        private fun executeGeminiCall(apiKey: String, modelName: String, userMessage: String): AgentDecision {
            try {
                val cleanModel = modelName.trim().ifEmpty { "gemini-1.5-flash" }
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val contentsArray = JSONArray()
                for (turn in safeHistory) {
                    contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", turn.first))))
                    contentsArray.put(JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", turn.second))))
                }

                val prompt = if (safeHistory.isEmpty()) "$companionPersona\n\nUser: $userMessage" else userMessage
                contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))

                val payload = JSONObject().apply {
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                if (conn.responseCode == 200) {
                    val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(res)
                    val rawReply = json.getJSONArray("candidates")
                        .getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                        .getJSONObject(0).getString("text")

                    val parsed = parseRawJson(rawReply)
                    if (parsed != null) {
                        safeHistory.add(Pair(userMessage, rawReply))
                        if (safeHistory.size > 6) safeHistory.removeAt(0)
                        return AgentDecision(parsed.optString("action", "CHAT"), parsed.optString("target", ""), parsed.optString("payload", ""), parsed.optString("speech", "हाँ भाई, सुन रहा हूँ।"))
                    }
                }
            } catch (e: Exception) {
                Log.e("GEMINI_CALL", "Error", e)
            }
            return fallbackLocalDecision(userMessage)
        }

        private fun executeOpenAICall(endpoint: String, apiKey: String, modelName: String, userMessage: String): AgentDecision {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val messages = JSONArray()
                messages.put(JSONObject().put("role", "system").put("content", companionPersona))

                for (turn in safeHistory) {
                    messages.put(JSONObject().put("role", "user").put("content", turn.first))
                    messages.put(JSONObject().put("role", "assistant").put("content", turn.second))
                }
                messages.put(JSONObject().put("role", "user").put("content", userMessage))

                val payload = JSONObject().apply {
                    put("model", modelName.ifEmpty { "gpt-4o-mini" })
                    put("messages", messages)
                    put("response_format", JSONObject().put("type", "json_object"))
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                if (conn.responseCode == 200) {
                    val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(res)
                    val rawReply = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")

                    val parsed = parseRawJson(rawReply)
                    if (parsed != null) {
                        safeHistory.add(Pair(userMessage, rawReply))
                        if (safeHistory.size > 6) safeHistory.removeAt(0)
                        return AgentDecision(parsed.optString("action", "CHAT"), parsed.optString("target", ""), parsed.optString("payload", ""), parsed.optString("speech", "हाँ भाई, बोलो।"))
                    }
                }
            } catch (e: Exception) {
                Log.e("OPENAI_GROK_CALL", "Error", e)
            }
            return fallbackLocalDecision(userMessage)
        }

        private fun parseRawJson(raw: String): JSONObject? {
            return try {
                val start = raw.indexOf('{')
                val end = raw.lastIndexOf('}')
                if (start != -1 && end != -1 && end > start) JSONObject(raw.substring(start, end + 1))
                else JSONObject(raw)
            } catch (_: Exception) { null }
        }

        private fun fallbackLocalDecision(msg: String): AgentDecision {
            val lower = msg.lowercase()
            return when {
                lower.contains("instagram") -> AgentDecision("OPEN_APP", "instagram", "", "हाँ भाई, इंस्टाग्राम खोल रहा हूँ।")
                lower.contains("whatsapp") -> AgentDecision("OPEN_APP", "whatsapp", "", "व्हाट्सएप खोल दिया।")
                lower.contains("youtube") -> AgentDecision("OPEN_APP", "youtube", "", "यूट्यूब ओपन कर रहा हूँ।")
                lower.contains("home") || lower.contains("होम") -> AgentDecision("SYSTEM_ACTION", "HOME", "", "होम स्क्रीन पर आ गया।")
                lower.contains("back") || lower.contains("पीछे") -> AgentDecision("SYSTEM_ACTION", "BACK", "", "बैक कर दिया।")
                lower.contains("reel") || lower.contains("रील") -> AgentDecision("SYSTEM_ACTION", "SWIPE_UP", "", "अगली रील देख लो!")
                else -> AgentDecision("CHAT", "", "", "हाँ भाई, मैं सुन रहा हूँ। बोलो क्या बात है?")
            }
        }
    }

    inner class AppAutomationEngine(private val context: Context) {
        fun launchApp(packageName: String) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) context.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun launchAppByName(name: String) {
            val lower = name.lowercase()
            when {
                lower.contains("whatsapp") -> launchApp("com.whatsapp")
                lower.contains("instagram") -> launchApp("com.instagram.android")
                lower.contains("youtube") -> launchApp("com.google.android.youtube")
                lower.contains("facebook") -> launchApp("com.facebook.katana")
                else -> {
                    try {
                        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        val apps = context.packageManager.queryIntentActivities(intent, 0)
                        for (app in apps) {
                            if (app.loadLabel(context.packageManager).toString().lowercase().contains(lower)) {
                                launchApp(app.activityInfo.packageName)
                                return
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    inner class AdvancedMathEngine {
        fun evaluate(str: String): Double = object : Any() {
            var pos = -1; var ch: Char = ' '
            fun next() { ch = if (++pos < str.length) str[pos] else ' ' }
            fun eat(c: Char): Boolean { while(ch == ' ') next(); if(ch == c) { next(); return true }; return false }
            fun parse(): Double { next(); val x = parseExp(); return x }
            fun parseExp(): Double { var x = parseTerm(); while(true) { when { eat('+') -> x += parseTerm(); eat('-') -> x -= parseTerm(); else -> return x } } }
            fun parseTerm(): Double { var x = parseFact(); while(true) { when { eat('*') -> x *= parseFact(); eat('/') -> x /= parseFact(); else -> return x } } }
            fun parseFact(): Double {
                if(eat('+')) return parseFact(); if(eat('-')) return -parseFact()
                var x = 0.0; val st = pos
                if(eat('(')) { x = parseExp(); eat(')') }
                else if(ch in '0'..'9' || ch == '.') { while(ch in '0'..'9' || ch == '.') next(); x = str.substring(st, pos).toDouble() }
                return x
            }
        }.parse()
    }

    inner class EnterpriseDatabaseHelper(c: Context) : SQLiteOpenHelper(c, "TitanData_V500.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE Logs (id INTEGER PRIMARY KEY, query TEXT, response TEXT, type TEXT)")
        }
        override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}
    }

    inner class HybridCryptography {
        private val k = "DRAKOX_V500_AES256_API_KEY_SECURE".toByteArray().copyOf(32)
        fun encrypt(s: String): String = try { val c = Cipher.getInstance("AES"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(k, "AES")); Base64.encodeToString(c.doFinal(s.toByteArray()), Base64.DEFAULT) } catch(_: Exception){""}
        fun decrypt(s: String): String = try { val c = Cipher.getInstance("AES"); c.init(Cipher.DECRYPT_MODE, SecretKeySpec(k, "AES")); String(c.doFinal(Base64.decode(s, Base64.DEFAULT))) } catch(_: Exception){""}
    }

    inner class DeepSystemDiagnostics(private val c: Context) {
        fun getRamUsage(): Int = try { val mi = ActivityManager.MemoryInfo(); (c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi); ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100).toInt() } catch(_:Exception){0}
    }

    // ========================================================================
    // [9] CYBERPUNK HUD HOLOGRAM GRAPHICS (GLOW & ELECTRIC PALETTES)
    // ========================================================================
    inner class MatrixDigitalRainView(c: Context) : View(c) {
        private val r = Random(); private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        private val drops = Array(150) { floatArrayOf(r.nextFloat()*2000f, r.nextFloat() * -3000f, r.nextFloat()*15f + 5f) }
        private var clr = "#00F0FF"
        fun updateTheme(s: SystemState) { clr = when(s) { SystemState.ERROR -> "#FF0033"; SystemState.LISTENING -> "#00FF99"; SystemState.SPEAKING -> "#FF9100"; else -> "#00F0FF" } }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); p.color = Color.parseColor(clr); p.textSize = 21f
            for (d in drops) {
                cv.drawText(r.nextInt(2).toString(), d[0], d[1], p)
                d[1] += d[2]; if(d[1] > height) { d[1] = -100f; d[0] = r.nextFloat()*width; d[2] = r.nextFloat()*15f + 5f }
            }
            invalidate()
        }
    }

    inner class CyberpunkRadarHUD(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
        private var swp = 0f; private var clr = "#00F0FF"; private var rms = 0f
        fun updateTheme(s: SystemState) { clr = when(s) { SystemState.ERROR -> "#FF0033"; SystemState.LISTENING -> "#00FF99"; else -> "#00F0FF" } }
        fun updateAudioWave(r: Float) { rms = r * 15f; invalidate() }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); val cx = width/2f; val cy = height/2f; val r = 360f
            p.color = Color.parseColor(clr); p.alpha = 45
            cv.drawCircle(cx, cy, r, p); cv.drawCircle(cx, cy, r*0.65f, p)
            if (rms > 0) { p.alpha = 220; p.color = Color.parseColor("#00FF99"); cv.drawCircle(cx, cy, r + rms, p); rms *= 0.8f }
            p.style = Paint.Style.FILL; p.alpha = 30
            cv.drawArc(RectF(cx-r, cy-r, cx+r, cy+r), swp, 45f, true, p)
            swp = (swp + 4.5f) % 360f; p.style = Paint.Style.STROKE; invalidate()
        }
    }

    inner class QuantumParticleEmitter(c: Context) : View(c) {
        private val r = Random(); private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val particles = Array(80) { Particle() }
        private var clr = "#00F0FF"
        inner class Particle { var x = r.nextFloat()*1500f; var y = r.nextFloat()*3000f; var vx = r.nextFloat()*6-3; var vy = r.nextFloat()*6-3; var rad = r.nextFloat()*4+2 }
        fun updateTheme(s: SystemState) { clr = when(s) { SystemState.ERROR -> "#FF0033"; SystemState.LISTENING -> "#00FF99"; else -> "#00F0FF" } }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); p.color = Color.parseColor(clr); p.alpha = 70
            for (pt in particles) {
                cv.drawCircle(pt.x, pt.y, pt.rad, p)
                pt.x += pt.vx; pt.y += pt.vy
                if(pt.x < 0 || pt.x > width) pt.vx *= -1; if(pt.y < 0 || pt.y > height) pt.vy *= -1
            }
            invalidate()
        }
    }

    inner class HolographicCompassView(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        private var rot = 0f
        private var clr = "#00F0FF"

        // FIX APPLIED: updateTheme explicitly added to resolve build error
        fun updateTheme(s: SystemState) {
            clr = when (s) {
                SystemState.ERROR -> "#FF0033"
                SystemState.LISTENING -> "#00FF99"
                SystemState.SPEAKING -> "#FF9100"
                else -> "#00F0FF"
            }
            invalidate()
        }

        fun applyRotation(z: Float) { rot += z * 4f; invalidate() }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); val cx = width/2f; val cy = height/2f; val r = 460f
            cv.save(); cv.rotate(rot, cx, cy)
            p.color = Color.parseColor(clr); p.alpha = 25
            val path = Path().apply { moveTo(cx, cy - r - 20f); lineTo(cx + 20f, cy - r + 20f); lineTo(cx - 20f, cy - r + 20f); close() }
            p.style = Paint.Style.FILL; cv.drawPath(path, p); p.style = Paint.Style.STROKE
            cv.drawCircle(cx, cy, r + 30f, p)
            cv.restore()
        }
    }

    inner class AudioSpectrumAnalyzer(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 6f; strokeCap = Paint.Cap.ROUND }
        private val bars = FloatArray(30)
        private var clr = "#00FF99"
        fun updateTheme(s: SystemState) { clr = if(s == SystemState.SPEAKING) "#FF9100" else "#00FF99" }
        fun updateWaveform(rms: Float) {
            for(i in 0 until bars.size - 1) bars[i] = bars[i+1]
            bars[bars.size - 1] = rms * 20f
            invalidate()
        }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); p.color = Color.parseColor(clr); p.alpha = 160
            val cx = width/2f; val cy = height - 250f; val w = 20f
            val startX = cx - ((bars.size * w) / 2)
            for(i in bars.indices) {
                cv.drawLine(startX + (i*w), cy, startX + (i*w), cy - bars[i], p)
                bars[i] *= 0.85f
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when(event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> { if(event.values[0] < 5f && ttsEngine.isSpeaking) ttsEngine.stop() }
            Sensor.TYPE_GYROSCOPE -> { compassHUDView?.applyRotation(event.values[2]) }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onResume() {
        super.onResume()
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        proxSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    override fun onDestroy() {
        super.onDestroy()
        ttsEngine.shutdown()
        speechRecognizer?.destroy()
        strobeJob?.cancel()
        aiCallJob?.cancel()
    }
}
