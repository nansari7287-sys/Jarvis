package com.example.jarvis

// ==========================================================================================
// J.A.R.V.I.S. TITAN CORE ARCHITECTURE - LIVE VOICE CALL & NEURAL AGENT V500.0
// DEVELOPED BY DRAKOX NAEEM
// FEATURES: CONTINUOUS AI VOICE CALL, CONVERSATION MEMORY, HINDI/HINGLISH FRIEND PERSONA,
// FULL PHONE CONTROL & APP AUTOMATION PIPELINE VIA ACCESSIBILITY
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
// TOP-LEVEL DATA MODELS (ZERO NESTED CLASS COMPILER ERRORS)
// ==========================================================================================
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
            Log.i("TITAN_CORE", "Native C++ Engine Injected.")
        } catch (e: Exception) {
            Log.w("TITAN_CORE", "Using JVM Engine Fallback.")
        }
    }

    enum class SystemState {
        POWER_OFF, BOOTING, ONLINE, STANDBY, PROCESSING, LISTENING, SPEAKING, COMBAT_MODE, CALL_ACTIVE, ERROR
    }

    private var currentState = SystemState.POWER_OFF
    private var isCallModeActive = false // TRUE = Live Phone Call with AI is Active

    // UI Bindings
    private var messageInputBox: EditText? = null
    private var micToggleButton: View? = null
    private var sendCommandButton: View? = null
    private var settingsBtn: View? = null
    private var jarvisOrbView: View? = null
    private var rootLayout: ViewGroup? = null
    
    // HUD Visuals
    private var matrixRainView: MatrixDigitalRainView? = null
    private var radarHUDView: CyberpunkRadarHUD? = null
    private var particleEmitterView: QuantumParticleEmitter? = null
    private var compassHUDView: HolographicCompassView? = null
    private var spectrumAnalyzerView: AudioSpectrumAnalyzer? = null

    // Managers & Engines
    private lateinit var ttsEngine: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var database: EnterpriseDatabaseHelper
    private lateinit var apiPrefs: SharedPreferences
    private lateinit var crypto: HybridCryptography
    private lateinit var diagnostics: DeepSystemDiagnostics
    private lateinit var advancedMath: AdvancedMathEngine
    private lateinit var appAutomation: AppAutomationEngine
    private lateinit var aiBrainAgent: TitanConversationalCallAgent
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

    // Telemetry Values
    private var batteryLevel = -1
    private var isCharging = false
    private var isNetworkActive = false

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
            else rootLayout?.setBackgroundColor(Color.parseColor("#03050A"))
        } catch (e: Exception) {
            rootLayout?.setBackgroundColor(Color.parseColor("#03050A"))
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
        aiBrainAgent = TitanConversationalCallAgent()
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
        } catch (e: Exception) { Log.e("TITAN", "Camera hardware exception.") }
    }

    private fun startTelemetryFeeds() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                isCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { isNetworkActive = true }
                override fun onLost(network: Network) { isNetworkActive = false }
            })
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
                duration = 5000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun injectMassiveHolograms() {
        particleEmitterView = QuantumParticleEmitter(this)
        rootLayout?.addView(particleEmitterView, 0, FrameLayout.LayoutParams(-1, -1))

        matrixRainView = MatrixDigitalRainView(this).apply { alpha = 0.15f }
        rootLayout?.addView(matrixRainView, 1, FrameLayout.LayoutParams(-1, -1))

        radarHUDView = CyberpunkRadarHUD(this)
        rootLayout?.addView(radarHUDView, 2, FrameLayout.LayoutParams(-1, -1))
        
        compassHUDView = HolographicCompassView(this)
        rootLayout?.addView(compassHUDView, 3, FrameLayout.LayoutParams(-1, -1))
        
        spectrumAnalyzerView = AudioSpectrumAnalyzer(this)
        rootLayout?.addView(spectrumAnalyzerView, 4, FrameLayout.LayoutParams(-1, -1))
    }

    private fun runCleanBootSequence() {
        Log.d("JARVIS_CORE", "[TITAN CORE V500.0] ENTERPRISE KERNEL ONLINE.")
        lifecycleScope.launch {
            delay(1200)
            updateEnvironmentState(SystemState.ONLINE)
            speak("सिस्टम ऑनलाइन है बॉस। माइक ऑन करके आप मुझसे सीधे कॉल की तरह बात कर सकते हैं।")
            currentState = SystemState.ONLINE
        }
    }

    // ========================================================================
    // [5] EVENT LISTENERS & CALL ACTIVATION
    // ========================================================================
    private fun setupEventListeners() {
        sendCommandButton?.setOnClickListener {
            val cmd = messageInputBox?.text?.toString()?.trim() ?: ""
            if (cmd.isNotEmpty()) {
                processConversationalInput(cmd)
                messageInputBox?.text?.clear()
            }
        }

        // MIC BUTTON = TOGGLE LIVE AI CALL (कॉल शुरू / बंद करें)
        micToggleButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            } else {
                if (!isCallModeActive) {
                    isCallModeActive = true
                    changeState(SystemState.CALL_ACTIVE)
                    Toast.makeText(this, "AI Voice Call Connected (Speak freely)", Toast.LENGTH_LONG).show()
                    speak("कॉल कनेक्ट हो गया है भाई, बोलो क्या हाल-चाल?")
                } else {
                    isCallModeActive = false
                    changeState(SystemState.STANDBY)
                    speechRecognizer?.stopListening()
                    Toast.makeText(this, "Call Disconnected", Toast.LENGTH_SHORT).show()
                    speak("ठीक है भाई, कॉल डिस्कनेक्ट कर रहा हूँ। जब भी ज़रूरत हो बस माइक दबा देना।")
                }
            }
        }
        
        settingsBtn?.setOnClickListener { openApiSettingsVault() }
    }

    // ========================================================================
    // [6] CONVERSATIONAL AI ENGINE (CALL LOGIC & MEMORY)
    // ========================================================================
    private fun processConversationalInput(rawInput: String) {
        val userSpeech = rawInput.trim()
        if (userSpeech.isEmpty()) return

        Log.d("JARVIS_CALL", "USER SAID: $userSpeech")
        updateEnvironmentState(SystemState.PROCESSING)

        // 1. अगर यूजर कॉल काटने को कहे
        if (userSpeech.contains("कॉल काटो", true) || userSpeech.contains("कॉल बंद", true) ||
            userSpeech.contains("phone rakho", true) || userSpeech.contains("bye jarvis", true)) {
            isCallModeActive = false
            speak("चलो ठीक है भाई, बाद में बात करते हैं। टेक केयर!")
            delayToStandby()
            return
        }

        // 2. तुरंत हार्डवेयर कमांड्स (बिना किसी देरी के)
        when {
            userSpeech.contains("torch on", true) || userSpeech.contains("लाइट ऑन", true) -> {
                executeHardwareAction(1); speak("टॉर्च जला दी भाई!"); loopBackToListen(); return
            }
            userSpeech.contains("torch off", true) || userSpeech.contains("लाइट ऑफ", true) -> {
                executeHardwareAction(0); speak("टॉर्च बंद कर दी।"); loopBackToListen(); return
            }
        }

        // 3. AI ब्रेन से बातचीत और ऑटोमेशन डिसीजन
        aiCallJob = lifecycleScope.launch {
            val geminiKey = crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: "")

            if (geminiKey.isNotBlank()) {
                val decision = aiBrainAgent.queryConversationalCall(geminiKey, userSpeech)
                executeCallDecision(decision)
            } else {
                speak("अरे भाई, तुमने सेटिंग्स में Gemini API Key नहीं डाली है। सेटिंग्स वाले आइकन पर क्लिक करके Key डाल दो, फिर मस्त बातें करेंगे!")
                loopBackToListen()
            }
        }
    }

    private fun executeCallDecision(decision: AgentDecision) {
        // AI दोस्त की तरह अपनी आवाज़ में बोलेगा
        if (decision.speech.isNotBlank()) {
            speak(decision.speech)
        }

        val service = JarvisAccessibilityService.instance

        // अगर बात-बात में कोई फोन का काम बोला गया है, तो बैकग्राउंड में वो भी करो
        when (decision.action.uppercase()) {
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
            "OPEN_APP" -> {
                appAutomation.launchAppByName(decision.target)
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
    // [7] VOICE & CONTINUOUS LISTENING PIPELINE
    // ========================================================================
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsEngine.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.language = Locale.US
            } else {
                ttsEngine.setSpeechRate(1.0f) // बिल्कुल इंसानी बोलने की नेचुरल स्पीड
                ttsEngine.setPitch(0.85f)     // फ्रेंडली और क्लियर टोन
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
                    // जैसे ही AI बोलना बंद करेगा, वॉयस कॉल में माइक खुद तुरंत चालू हो जाएगा!
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
                    // अगर यूजर थोड़ी देर चुप रहा (Timeout), तो कॉल कटेगी नहीं, दोबारा सुनना शुरू करेगा!
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
    // [8] HARDWARE AUTOMATIONS
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
    // [9] API SETTINGS VAULT
    // ========================================================================
    private fun openApiSettingsVault() {
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(60,60,60,60)
            setBackgroundColor(Color.parseColor("#050814")) 
        }
        
        val title = TextView(this).apply { 
            text = "TITAN AI CALL VAULT"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setPadding(0,0,0,40)
            gravity = Gravity.CENTER
        }
        
        fun createApiInput(hintText: String, savedVal: String): EditText {
            return EditText(this).apply {
                hint = hintText
                setTextColor(Color.WHITE)
                setHintTextColor(Color.parseColor("#444444"))
                setBackgroundColor(Color.parseColor("#1A00E5FF"))
                setPadding(30,30,30,30)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ bottomMargin=25 }
                setText(savedVal)
            }
        }

        val keyGemini = createApiInput("Enter Gemini API Key", crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: ""))

        val saveBtn = Button(this).apply { 
            text = "SAVE & CONNECT CALL BRAIN"
            setBackgroundColor(Color.parseColor("#00E5FF"))
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ topMargin=20 }
        }
        
        layout.addView(title); layout.addView(keyGemini); layout.addView(saveBtn)
        
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(layout).show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        saveBtn.setOnClickListener {
            val enteredKey = keyGemini.text.toString().trim()
            apiPrefs.edit().putString("API_GEMINI", crypto.encrypt(enteredKey)).apply()
            Toast.makeText(this, "Gemini Key Saved!", Toast.LENGTH_SHORT).show()
            triggerHaptics(150)
            dialog.dismiss()
            speak("एपीआई की सेव हो गई है भाई! अब माइक दबाओ और कॉल पर बात शुरू करो।")
        }
    }

    // ========================================================================
    // [10] UI THEMES
    // ========================================================================
    private fun changeState(state: SystemState) {
        currentState = state
        updateEnvironmentState(state)
    }

    private fun updateEnvironmentState(state: SystemState) {
        val hex = when (state) {
            SystemState.PROCESSING -> "#3300E5FF" 
            SystemState.LISTENING -> "#3300FF00"  
            SystemState.SPEAKING -> "#33FF9100"   
            SystemState.CALL_ACTIVE -> "#2200E5FF"
            SystemState.COMBAT_MODE -> "#88FF0000" 
            SystemState.ERROR -> "#88FF0000" 
            else -> "#00000000" 
        }
        
        ObjectAnimator.ofArgb(rootLayout!!, "backgroundColor", Color.parseColor(hex)).apply { 
            duration = 500
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
    // [11] TITAN CONVERSATIONAL AI AGENT (CALL BRAIN WITH MEMORY)
    // ========================================================================
    class TitanConversationalCallAgent {
        // बातचीत की याददाश्त (Conversation History for real-time call feel)
        private val conversationHistory = JSONArray()

        private val companionPersona = """
            You are J.A.R.V.I.S., a witty, highly intelligent, loyal friend and personal AI assistant to the user.
            You are currently on a LIVE VOICE CALL with the user.
            
            RULES FOR RESPONSE:
            1. Speak naturally in conversational Hindi/Hinglish (like two close friends chatting on a call). Keep sentences punchy, lively, and natural.
            2. You can talk about ANYTHING (life, tech, coding, jokes, advice).
            3. You ALSO have full control over the user's Android phone. If the user asks you to do something on their phone (send WhatsApp, search Instagram, go home, scroll reels), you must execute it while naturally acknowledging it on the call.
            4. ALWAYS return strictly a valid RAW JSON object with NO MARKDOWN, NO BACKTICKS.
            
            JSON Schema:
            {
               "action": "CHAT" | "WHATSAPP_SEND" | "INSTAGRAM_SEARCH" | "OPEN_APP" | "SYSTEM_ACTION" | "CLICK" | "TYPE",
               "target": "string or empty",
               "payload": "string or empty",
               "speech": "What you speak to the user in Hindi on the call"
            }

            Examples:
            - User: "aur bhai kaisa hai kya chal raha hai"
              -> {"action":"CHAT","target":"","payload":"","speech":"Sab badhiya mere bhai! Bas tumhare phone me baithke tumhare agle order ka wait kar raha tha. Batao aaj kya plan hai?"}
            - User: "Instagram kholo aur rohit search karo"
              -> {"action":"INSTAGRAM_SEARCH","target":"rohit","payload":"","speech":"Haan bhai, Instagram khol ke Rohit ko dhoondh raha hoon."}
            - User: "WhatsApp pe Rahul ko bolo kal milte hain"
              -> {"action":"WHATSAPP_SEND","target":"Rahul","payload":"Kal milte hain","speech":"Theek hai bhai, Rahul ko WhatsApp pe message bhej diya."}
            - User: "Reel scroll karo"
              -> {"action":"SYSTEM_ACTION","target":"SWIPE_UP","payload":"","speech":"Ye lo agla reel dekh lo!"}
        """.trimIndent()

        suspend fun queryConversationalCall(apiKey: String, userMessage: String): AgentDecision = withContext(Dispatchers.IO) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                // Add user turn to conversation history
                val userPart = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                userPart.put("role", "user")
                conversationHistory.put(userPart)

                // Keep memory from growing infinitely (last 10 turns)
                while (conversationHistory.length() > 10) {
                    conversationHistory.remove(0)
                }

                val payloadJson = JSONObject().apply {
                    val systemInstruction = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", companionPersona)))
                    put("system_instruction", systemInstruction)
                    put("contents", conversationHistory)
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payloadJson.toString()) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val res = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(res)
                    val rawReply = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleaned = rawReply.replace("```json", "").replace("```", "").trim()
                    val parsed = JSONObject(cleaned)

                    // Add assistant turn to history
                    val modelPart = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", cleaned)))
                    modelPart.put("role", "model")
                    conversationHistory.put(modelPart)

                    return@withContext AgentDecision(
                        action = parsed.optString("action", "CHAT"),
                        target = parsed.optString("target", ""),
                        payload = parsed.optString("payload", ""),
                        speech = parsed.optString("speech", "Haan bhai, sun raha hoon.")
                    )
                } else {
                    val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    Log.e("CALL_BRAIN", "Gemini HTTP $responseCode: $errStream")
                }
            } catch (e: Exception) {
                Log.e("CALL_BRAIN", "Call reasoning exception", e)
            }
            return@withContext AgentDecision("CHAT", speech = "Bhai awaaz thodi kat gayi thi, ek baar dobara bolna?")
        }
    }

    inner class AppAutomationEngine(private val context: Context) {
        fun launchApp(packageName: String) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) context.startActivity(launchIntent)
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
    // [12] HUD HOLOGRAMS
    // ========================================================================
    inner class MatrixDigitalRainView(c: Context) : View(c) {
        private val r = Random(); private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        private val drops = Array(150) { floatArrayOf(r.nextFloat()*2000f, r.nextFloat() * -3000f, r.nextFloat()*15f + 5f) }
        private var clr = "#00E5FF"
        fun updateTheme(s: SystemState) { clr = when(s) { SystemState.ERROR -> "#FF0000"; SystemState.LISTENING -> "#00FF00"; SystemState.SPEAKING -> "#FF9100"; else -> "#00E5FF" } }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); p.color = Color.parseColor(clr); p.textSize = 20f
            for (d in drops) {
                cv.drawText(r.nextInt(2).toString(), d[0], d[1], p)
                d[1] += d[2]; if(d[1] > height) { d[1] = -100f; d[0] = r.nextFloat()*width; d[2] = r.nextFloat()*15f + 5f }
            }
            invalidate()
        }
    }

    inner class CyberpunkRadarHUD(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
        private var swp = 0f; private var clr = "#00E5FF"; private var rms = 0f
        fun updateTheme(s: SystemState) { clr = when(s) { SystemState.ERROR -> "#FF0000"; SystemState.LISTENING -> "#00FF00"; else -> "#00E5FF" } }
        fun updateAudioWave(r: Float) { rms = r * 15f; invalidate() }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); val cx = width/2f; val cy = height/2f; val r = 350f
            p.color = Color.parseColor(clr); p.alpha = 40
            cv.drawCircle(cx, cy, r, p); cv.drawCircle(cx, cy, r*0.6f, p)
            if (rms > 0) { p.alpha = 200; p.color = Color.GREEN; cv.drawCircle(cx, cy, r + rms, p); rms *= 0.8f }
            p.style = Paint.Style.FILL; p.alpha = 25
            cv.drawArc(RectF(cx-r, cy-r, cx+r, cy+r), swp, 40f, true, p)
            swp = (swp + 4f) % 360f; p.style = Paint.Style.STROKE; invalidate()
        }
    }

    inner class QuantumParticleEmitter(c: Context) : View(c) {
        private val r = Random(); private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val particles = Array(80) { Particle() }
        private var clr = "#00E5FF"
        inner class Particle { var x = r.nextFloat()*1500f; var y = r.nextFloat()*3000f; var vx = r.nextFloat()*6-3; var vy = r.nextFloat()*6-3; var rad = r.nextFloat()*4+2 }
        fun updateTheme(s: SystemState) { clr = when(s) { SystemState.ERROR -> "#FF0000"; SystemState.LISTENING -> "#00FF00"; else -> "#00E5FF" } }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); p.color = Color.parseColor(clr); p.alpha = 60
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
        private var rot = 0f; private var clr = "#00E5FF"
        fun updateTheme(s: SystemState) { clr = "#00E5FF" }
        fun applyRotation(z: Float) { rot += z * 4f; invalidate() }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); val cx = width/2f; val cy = height/2f; val r = 450f
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
        private var clr = "#00FF00"
        fun updateTheme(s: SystemState) { clr = if(s == SystemState.SPEAKING) "#FF9100" else "#00FF00" }
        fun updateWaveform(rms: Float) {
            for(i in 0 until bars.size - 1) bars[i] = bars[i+1]
            bars[bars.size - 1] = rms * 20f
            invalidate()
        }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv); p.color = Color.parseColor(clr); p.alpha = 150
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
