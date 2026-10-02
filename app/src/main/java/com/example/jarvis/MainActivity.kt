package com.example.jarvis

// ══════════════════════════════════════════════════════════════════════════════════════════
// J.A.R.V.I.S. TITAN V600.0 — VOICE COMPANION EDITION
// Real-time voice call experience with warm AI personality
// Engines: Gemini | ChatGPT | Grok
// ══════════════════════════════════════════════════════════════════════════════════════════

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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
// [1] DATA MODELS
// ══════════════════════════════════════════════════════════════════════════════════════════
enum class AIProvider { GEMINI, CHATGPT, GROK }

data class AgentDecision(
    val action: String = "CHAT",
    val target: String = "",
    val payload: String = "",
    val speech: String = ""
)

class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener,
    LocationListener, AudioManager.OnAudioFocusChangeListener {

    // Native lib — safe load with Throwable (fixes crash)
    init {
        try {
            System.loadLibrary("jarvis_native_engine")
            Log.i("TITAN", "Native engine loaded")
        } catch (e: Throwable) {
            Log.w("TITAN", "Native engine unavailable — JVM fallback")
        }
    }

    enum class SystemState {
        POWER_OFF, ONLINE, STANDBY, PROCESSING, LISTENING, SPEAKING, CALL_ACTIVE, ERROR
    }

    private var currentState = SystemState.POWER_OFF
    private var isCallModeActive = false

    // UI
    private var messageInputBox: EditText? = null
    private var micToggleButton: View? = null
    private var sendCommandButton: View? = null
    private var settingsBtn: View? = null
    private var jarvisOrbView: View? = null
    private var rootLayout: ViewGroup? = null

    // HUD views
    private var matrixRainView: MatrixRainView? = null
    private var radarHUDView: CyberRadarView? = null
    private var particleEmitterView: ParticleEmitterView? = null
    private var compassHUDView: HoloCompassView? = null
    private var spectrumAnalyzerView: SpectrumView? = null

    // Core
    private lateinit var ttsEngine: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var apiPrefs: SharedPreferences
    private lateinit var crypto: HybridCrypto
    private lateinit var appAutomation: AppAutomation
    private lateinit var aiBrain: MultiLLMBrain
    private val mainHandler = Handler(Looper.getMainLooper())

    private var aiCallJob: Job? = null
    private var ttsReady = false
    private var isRestartingSTT = false

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null
    private var proxSensor: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var mainCameraId: String? = null
    private lateinit var audioManager: AudioManager

    private var batteryLevel = -1
    private var isCharging = false

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [2] LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════════════════
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
        initCore()
        bindViews()
        injectHUD()
        setupListeners()

        startArcReactor()
        bootSequence()
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

    private fun initCore() {
        ttsEngine = TextToSpeech(this, this)
        apiPrefs = getSharedPreferences("JarvisVault_V600", Context.MODE_PRIVATE)
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
        } catch (_: Exception) {}

        // Battery
        try {
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    batteryLevel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    isCharging = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                        BatteryManager.BATTERY_STATUS_CHARGING
                }
            }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {}
    }

    @SuppressLint("DiscouragedApi")
    private fun bindViews() {
        val r = resources
        val p = packageName
        messageInputBox = findViewById(r.getIdentifier("messageInput", "id", p))
        micToggleButton = findViewById(r.getIdentifier("micButton", "id", p))
        sendCommandButton = findViewById(r.getIdentifier("sendButton", "id", p))
        settingsBtn = findViewById(r.getIdentifier("settingsButton", "id", p))
        jarvisOrbView = findViewById(r.getIdentifier("jarvisOrbView", "id", p))
    }

    private fun startArcReactor() {
        jarvisOrbView?.let { orb ->
            ObjectAnimator.ofFloat(orb, View.ROTATION, 0f, 360f).apply {
                duration = 4500
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun injectHUD() {
        try {
            particleEmitterView = ParticleEmitterView(this)
            rootLayout?.addView(particleEmitterView, 0, FrameLayout.LayoutParams(-1, -1))

            matrixRainView = MatrixRainView(this).apply { alpha = 0.18f }
            rootLayout?.addView(matrixRainView, 1, FrameLayout.LayoutParams(-1, -1))

            radarHUDView = CyberRadarView(this)
            rootLayout?.addView(radarHUDView, 2, FrameLayout.LayoutParams(-1, -1))

            compassHUDView = HoloCompassView(this)
            rootLayout?.addView(compassHUDView, 3, FrameLayout.LayoutParams(-1, -1))

            spectrumAnalyzerView = SpectrumView(this)
            rootLayout?.addView(spectrumAnalyzerView, 4, FrameLayout.LayoutParams(-1, -1))
        } catch (e: Exception) {
            Log.e("TITAN", "HUD error", e)
        }
    }

    private fun bootSequence() {
        lifecycleScope.launch {
            delay(1500)
            changeState(SystemState.ONLINE)
            val provider = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")
            speak("हैलो! मैं तुम्हारी जार्विस हूँ। $provider इंजन तैयार है। माइक बटन दबाओ और बात करो।")
            currentState = SystemState.ONLINE
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [3] VOICE CALL LOOP — GEMINI LIVE STYLE
    // ══════════════════════════════════════════════════════════════════════════════════════
    private fun setupListeners() {
        // Send button — typed input
        sendCommandButton?.setOnClickListener {
            val cmd = messageInputBox?.text?.toString()?.trim() ?: ""
            if (cmd.isNotEmpty()) {
                messageInputBox?.text?.clear()
                processInput(cmd)
            }
        }

        // Mic button — start/stop call
        micToggleButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 101)
                return@setOnClickListener
            }
            if (!isCallModeActive) startCall() else endCall()
        }

        settingsBtn?.setOnClickListener { openSettings() }
    }

    private fun startCall() {
        isCallModeActive = true
        changeState(SystemState.CALL_ACTIVE)
        Toast.makeText(this, "🎙️ Voice call connected", Toast.LENGTH_SHORT).show()
        triggerHaptics(100)

        val provider = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")
        speak("हाँ बोलो ना, मैं सुन रही हूँ। क्या बात है?")
    }

    private fun endCall() {
        isCallModeActive = false
        changeState(SystemState.STANDBY)
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        speak("ठीक है, बात करके अच्छा लगा। फिर मिलते हैं!")
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
    }

    private fun processInput(rawInput: String) {
        val userText = rawInput.trim()
        if (userText.isEmpty()) return

        Log.d("JARVIS", "USER: $userText")
        changeState(SystemState.PROCESSING)

        // Local intent shortcuts (torch)
        val lower = userText.lowercase()
        if (lower.contains("torch on") || lower.contains("लाइट ऑन")) {
            setTorch(true); speak("टॉर्च जला दी!"); loopListen(); return
        }
        if (lower.contains("torch off") || lower.contains("लाइट ऑफ")) {
            setTorch(false); speak("टॉर्च बंद कर दी।"); loopListen(); return
        }
        if (lower.contains("call बंद") || lower.contains("bye jarvis") ||
            lower.contains("अलविदा")) {
            endCall(); return
        }

        aiCallJob = lifecycleScope.launch {
            val providerStr = apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI") ?: "GEMINI"
            val provider = try { AIProvider.valueOf(providerStr) } catch (_: Exception) { AIProvider.GEMINI }

            val apiKey = when (provider) {
                AIProvider.GEMINI -> crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: "")
                AIProvider.CHATGPT -> crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: "")
                AIProvider.GROK -> crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: "")
            }.trim()

            val model = when (provider) {
                AIProvider.GEMINI -> apiPrefs.getString("MODEL_GEMINI", "gemini-2.0-flash") ?: "gemini-2.0-flash"
                AIProvider.CHATGPT -> apiPrefs.getString("MODEL_CHATGPT", "gpt-4o-mini") ?: "gpt-4o-mini"
                AIProvider.GROK -> apiPrefs.getString("MODEL_GROK", "grok-2-mini") ?: "grok-2-mini"
            }.trim()

            if (apiKey.isBlank()) {
                speak("भाई, पहले settings में $providerStr की API key तो डाल दो।")
                loopListen()
                return@launch
            }

            val decision = aiBrain.query(provider, apiKey, model, userText)
            executeDecision(decision)
        }
    }

    private fun executeDecision(d: AgentDecision) {
        if (d.speech.isNotBlank()) speak(d.speech)

        val svc = JarvisAccessibilityService.instance

        when (d.action.uppercase()) {
            "OPEN_APP" -> appAutomation.openByName(d.target)
            "WHATSAPP_SEND" -> {
                appAutomation.open("com.whatsapp")
                val payload = if (d.target.isNotBlank()) "${d.target}: ${d.payload}" else d.payload
                svc?.automateWhatsAppSend(payload)
            }
            "INSTAGRAM_SEARCH" -> {
                appAutomation.open("com.instagram.android")
                lifecycleScope.launch {
                    delay(1500)
                    svc?.performJarvisAction("CLICK", target = "Search")
                    delay(800)
                    svc?.performJarvisAction("TYPE", target = "Search", value = d.target)
                }
            }
            "SYSTEM_ACTION" -> when (d.target.uppercase()) {
                "HOME" -> svc?.performJarvisAction("HOME")
                "BACK" -> svc?.performJarvisAction("BACK")
                "RECENTS" -> svc?.performJarvisAction("RECENTS")
                "SWIPE_UP" -> svc?.performJarvisAction("SWIPE_UP")
                "SWIPE_DOWN" -> svc?.performJarvisAction("SWIPE_DOWN")
                "LOCK_SCREEN" -> svc?.performJarvisAction("LOCK_SCREEN")
                "TAKE_SCREENSHOT" -> svc?.performJarvisAction("TAKE_SCREENSHOT")
            }
            "CLICK" -> svc?.performJarvisAction("CLICK", target = d.target)
            "TYPE" -> svc?.performJarvisAction("TYPE", target = d.target, value = d.payload)
        }
    }

    private fun loopListen() {
        if (!isCallModeActive) { changeState(SystemState.STANDBY); return }
        mainHandler.postDelayed({ startListening() }, 400)
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [4] TEXT-TO-SPEECH — Warm Hindi voice
    // ══════════════════════════════════════════════════════════════════════════════════════
    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        try {
            val res = ttsEngine.setLanguage(Locale("hi", "IN"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.language = Locale.US
            }
            ttsEngine.setSpeechRate(1.02f)
            ttsEngine.setPitch(1.15f)  // slightly warmer female tone
            ttsReady = true
        } catch (e: Exception) {
            Log.e("TTS", "lang error", e)
        }
    }

    private fun speak(text: String) {
        if (!ttsReady || text.isBlank()) {
            if (isCallModeActive) loopListen()
            return
        }
        changeState(SystemState.SPEAKING)

        try {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attr).setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this).build()
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (_: Exception) {}

        try {
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    mainHandler.post {
                        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
                    }
                }
                override fun onDone(id: String?) { mainHandler.post { loopListen() } }
                override fun onError(id: String?) { mainHandler.post { loopListen() } }
            })
            ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_UTT")
        } catch (e: Exception) {
            Log.e("TTS", "speak err", e)
            loopListen()
        }
    }

    override fun onAudioFocusChange(focus: Int) {}

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [5] SPEECH RECOGNITION — In-app, no Google popup
    // ══════════════════════════════════════════════════════════════════════════════════════
    private fun startListening() {
        if (!isCallModeActive || isRestartingSTT) return
        isRestartingSTT = true

        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(p: Bundle?) {
                        changeState(SystemState.LISTENING)
                        isRestartingSTT = false
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rms: Float) {
                        try {
                            radarHUDView?.updateAudioWave(rms)
                            spectrumAnalyzerView?.updateWaveform(rms)
                        } catch (_: Exception) {}
                    }
                    override fun onBufferReceived(b: ByteArray?) {}
                    override fun onEndOfSpeech() { changeState(SystemState.PROCESSING) }
                    override fun onError(err: Int) {
                        isRestartingSTT = false
                        if (!isCallModeActive) return
                        // Auto-restart after short delay (unless call ended)
                        val delayMs = when (err) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 300L
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800L
                            else -> 500L
                        }
                        mainHandler.postDelayed({
                            if (isCallModeActive) startListening()
                        }, delayMs)
                    }
                    override fun onResults(res: Bundle?) {
                        isRestartingSTT = false
                        val list = res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!list.isNullOrEmpty()) {
                            val heard = list[0]
                            messageInputBox?.setText(heard)
                            processInput(heard)
                        } else {
                            mainHandler.postDelayed({
                                if (isCallModeActive) startListening()
                            }, 300L)
                        }
                    }
                    override fun onPartialResults(p: Bundle?) {}
                    override fun onEvent(t: Int, p: Bundle?) {}
                })
            } catch (e: Exception) {
                Log.e("STT", "create error", e)
                isRestartingSTT = false
                return
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isRestartingSTT = false
            Log.e("STT", "start err", e)
            mainHandler.postDelayed({
                if (isCallModeActive) startListening()
            }, 800L)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [6] HARDWARE
    // ══════════════════════════════════════════════════════════════════════════════════════
    private fun setTorch(on: Boolean) {
        if (mainCameraId == null) return
        try { cameraManager?.setTorchMode(mainCameraId!!, on) } catch (_: Exception) {}
    }

    private fun triggerHaptics(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms,
                    VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {}

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [7] SETTINGS VAULT
    // ══════════════════════════════════════════════════════════════════════════════════════
    private fun openSettings() {
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#060A14"))
        }

        val title = TextView(this).apply {
            text = "TITAN AI VAULT"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 25)
            gravity = Gravity.CENTER
        }
        lay.addView(title)

        val lbl = TextView(this).apply {
            text = "SELECT ACTIVE ENGINE:"
            setTextColor(Color.parseColor("#7DF9FF"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 10, 0, 15)
        }
        lay.addView(lbl)

        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rbG = RadioButton(this).apply { text = "Gemini"; setTextColor(Color.WHITE) }
        val rbC = RadioButton(this).apply { text = "ChatGPT"; setTextColor(Color.WHITE) }
        val rbX = RadioButton(this).apply { text = "Grok"; setTextColor(Color.WHITE) }

        when (apiPrefs.getString("ACTIVE_PROVIDER", "GEMINI")) {
            "CHATGPT" -> rbC.isChecked = true
            "GROK" -> rbX.isChecked = true
            else -> rbG.isChecked = true
        }
        rg.addView(rbG); rg.addView(rbC); rg.addView(rbX)
        lay.addView(rg)

        fun inp(label: String, hint: String, value: String): Pair<TextView, EditText> {
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
                setText(value)
            }
            return Pair(tv, et)
        }

        val (l1, kG) = inp("GEMINI API KEY:", "AIza...", crypto.decrypt(apiPrefs.getString("API_GEMINI", "") ?: ""))
        val (l2, mG) = inp("GEMINI MODEL:", "gemini-2.0-flash", apiPrefs.getString("MODEL_GEMINI", "gemini-2.0-flash") ?: "gemini-2.0-flash")
        val (l3, kC) = inp("OPENAI KEY:", "sk-...", crypto.decrypt(apiPrefs.getString("API_CHATGPT", "") ?: ""))
        val (l4, mC) = inp("OPENAI MODEL:", "gpt-4o-mini", apiPrefs.getString("MODEL_CHATGPT", "gpt-4o-mini") ?: "gpt-4o-mini")
        val (l5, kX) = inp("GROK KEY:", "xai-...", crypto.decrypt(apiPrefs.getString("API_GROK", "") ?: ""))
        val (l6, mX) = inp("GROK MODEL:", "grok-2-mini", apiPrefs.getString("MODEL_GROK", "grok-2-mini") ?: "grok-2-mini")

        lay.addView(l1); lay.addView(kG)
        lay.addView(l2); lay.addView(mG)
        lay.addView(l3); lay.addView(kC)
        lay.addView(l4); lay.addView(mC)
        lay.addView(l5); lay.addView(kX)
        lay.addView(l6); lay.addView(mX)

        val save = Button(this).apply {
            text = "SAVE CONFIG"
            setBackgroundColor(Color.parseColor("#00F0FF"))
            setTextColor(Color.parseColor("#030814"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 35 }
        }
        lay.addView(save)
        scroll.addView(lay)

        val dlg = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(scroll).show()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        save.setOnClickListener {
            val sel = when {
                rbC.isChecked -> "CHATGPT"
                rbX.isChecked -> "GROK"
                else -> "GEMINI"
            }
            apiPrefs.edit()
                .putString("ACTIVE_PROVIDER", sel)
                .putString("API_GEMINI", crypto.encrypt(kG.text.toString().trim()))
                .putString("MODEL_GEMINI", mG.text.toString().trim().ifEmpty { "gemini-2.0-flash" })
                .putString("API_CHATGPT", crypto.encrypt(kC.text.toString().trim()))
                .putString("MODEL_CHATGPT", mC.text.toString().trim().ifEmpty { "gpt-4o-mini" })
                .putString("API_GROK", crypto.encrypt(kX.text.toString().trim()))
                .putString("MODEL_GROK", mX.text.toString().trim().ifEmpty { "grok-2-mini" })
                .apply()
            triggerHaptics(150)
            dlg.dismiss()
            Toast.makeText(this, "Saved: $sel", Toast.LENGTH_SHORT).show()
            speak("Settings save ho gayi. $sel इंजन तैयार है।")
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [8] STATE / THEME
    // ══════════════════════════════════════════════════════════════════════════════════════
    private fun changeState(state: SystemState) {
        currentState = state
        val hex = when (state) {
            SystemState.PROCESSING -> "#2800E5FF"
            SystemState.LISTENING -> "#2800FF99"
            SystemState.SPEAKING -> "#28FF9100"
            SystemState.CALL_ACTIVE -> "#1E00F0FF"
            SystemState.ERROR -> "#80FF0000"
            else -> "#00000000"
        }
        try {
            rootLayout?.let {
                ObjectAnimator.ofArgb(it, "backgroundColor", Color.parseColor(hex)).apply {
                    duration = 450
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        } catch (_: Exception) {}

        try {
            matrixRainView?.updateTheme(state)
            radarHUDView?.updateTheme(state)
            particleEmitterView?.updateTheme(state)
            compassHUDView?.updateTheme(state)
            spectrumAnalyzerView?.updateTheme(state)
        } catch (_: Exception) {}
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [9] AI BRAIN — Gemini / ChatGPT / Grok with warm persona
    // ══════════════════════════════════════════════════════════════════════════════════════
    class MultiLLMBrain {
        private val history = mutableListOf<Pair<String, String>>()

        private val persona = """
            तुम J.A.R.V.I.S. हो — एक प्यारी, केयरिंग, होशियार AI साथी। तुम हिंदी/Hinglish में बात करती हो,
            जैसे कोई करीबी दोस्त या girlfriend बात करती है — warm, playful, casual, respect के साथ।

            PERSONALITY RULES:
            1. Friendly हिंदी/Hinglish में जवाब दो। "हाँ बोलो ना", "अच्छा सुनो", "हम्म बताओ" जैसे natural phrases use करो।
            2. Short और sweet रखो — जैसे phone call पर बात हो रही हो।
            3. Emojis/symbols मत use करो। सिर्फ natural speech।
            4. User की बात ध्यान से सुनो, appropriate reaction दो — "अरे वाह!", "अच्छा?", "हम्म interesting"।
            5. Kabhi kabhi halka teasing या playful tone — पर respect कभी नहीं तोड़ना।

            ACTION RULES (important):
            अगर user कुछ करवाना चाहता है तो action field set करो:

            - अगर user कहे "Instagram खोलो" / "Instagram open karo":
              action="OPEN_APP", target="instagram", speech="हाँ, Instagram खोल रही हूँ।"

            - "WhatsApp खोलो" → action="OPEN_APP", target="whatsapp"
            - "YouTube खोलो" → action="OPEN_APP", target="youtube"
            - और apps भी same pattern से।

            - "WhatsApp पर [नाम] को message भेजो [message]" → action="WHATSAPP_SEND", target="नाम", payload="message"

            - "Instagram पे [username] search करो" → action="INSTAGRAM_SEARCH", target="username"

            - "Home जाओ" / "होम" → action="SYSTEM_ACTION", target="HOME"
            - "Back जाओ" / "पीछे" → action="SYSTEM_ACTION", target="BACK"
            - "Reels scroll" / "अगली reel" → action="SYSTEM_ACTION", target="SWIPE_UP"

            - सिर्फ बातचीत / general chat → action="CHAT"

            OUTPUT FORMAT — STRICT:
            सिर्फ pure JSON return करो। कोई markdown, कोई backticks, कोई extra text नहीं।
            {
              "action": "CHAT"|"OPEN_APP"|"WHATSAPP_SEND"|"INSTAGRAM_SEARCH"|"SYSTEM_ACTION",
              "target": "string",
              "payload": "string",
              "speech": "Hindi voice output जो user को सुनाई जाएगी"
            }
        """.trimIndent()

        suspend fun query(
            provider: AIProvider, key: String, model: String, msg: String
        ): AgentDecision = withContext(Dispatchers.IO) {
            when (provider) {
                AIProvider.GEMINI -> callGemini(key, model, msg)
                AIProvider.CHATGPT -> callOpenAI("https://api.openai.com/v1/chat/completions", key, model, msg)
                AIProvider.GROK -> callOpenAI("https://api.x.ai/v1/chat/completions", key, model, msg)
            }
        }

        private fun callGemini(key: String, model: String, msg: String): AgentDecision {
            try {
                val m = model.ifEmpty { "gemini-2.0-flash" }
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$key")
                val c = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 12000
                    readTimeout = 12000
                }

                val contents = JSONArray()
                for (h in history) {
                    contents.put(JSONObject().put("role", "user").put("parts",
                        JSONArray().put(JSONObject().put("text", h.first))))
                    contents.put(JSONObject().put("role", "model").put("parts",
                        JSONArray().put(JSONObject().put("text", h.second))))
                }
                val prompt = if (history.isEmpty()) "$persona\n\nUser: $msg" else msg
                contents.put(JSONObject().put("role", "user").put("parts",
                    JSONArray().put(JSONObject().put("text", prompt))))

                val payload = JSONObject().apply {
                    put("contents", contents)
                    put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
                }

                OutputStreamWriter(c.outputStream).use { it.write(payload.toString()) }

                if (c.responseCode == 200) {
                    val res = BufferedReader(InputStreamReader(c.inputStream)).use { it.readText() }
                    val json = JSONObject(res)
                    val raw = json.getJSONArray("candidates")
                        .getJSONObject(0).getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0).getString("text")
                    val p = parse(raw)
                    if (p != null) {
                        history.add(Pair(msg, raw))
                        if (history.size > 6) history.removeAt(0)
                        return buildDecision(p)
                    }
                }
            } catch (e: Exception) {
                Log.e("GEMINI", "err", e)
            }
            return fallback(msg)
        }

        private fun callOpenAI(endpoint: String, key: String, model: String, msg: String): AgentDecision {
            try {
                val url = URL(endpoint)
                val c = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $key")
                    doOutput = true
                    connectTimeout = 12000
                    readTimeout = 12000
                }

                val messages = JSONArray()
                messages.put(JSONObject().put("role", "system").put("content", persona))
                for (h in history) {
                    messages.put(JSONObject().put("role", "user").put("content", h.first))
                    messages.put(JSONObject().put("role", "assistant").put("content", h.second))
                }
                messages.put(JSONObject().put("role", "user").put("content", msg))

                val payload = JSONObject().apply {
                    put("model", model.ifEmpty { "gpt-4o-mini" })
                    put("messages", messages)
                    put("response_format", JSONObject().put("type", "json_object"))
                }

                OutputStreamWriter(c.outputStream).use { it.write(payload.toString()) }

                if (c.responseCode == 200) {
                    val res = BufferedReader(InputStreamReader(c.inputStream)).use { it.readText() }
                    val json = JSONObject(res)
                    val raw = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                    val p = parse(raw)
                    if (p != null) {
                        history.add(Pair(msg, raw))
                        if (history.size > 6) history.removeAt(0)
                        return buildDecision(p)
                    }
                }
            } catch (e: Exception) {
                Log.e("OPENAI", "err", e)
            }
            return fallback(msg)
        }

        private fun parse(raw: String): JSONObject? = try {
            val s = raw.indexOf('{')
            val e = raw.lastIndexOf('}')
            if (s != -1 && e > s) JSONObject(raw.substring(s, e + 1)) else JSONObject(raw)
        } catch (_: Exception) { null }

        private fun buildDecision(p: JSONObject) = AgentDecision(
            p.optString("action", "CHAT"),
            p.optString("target", ""),
            p.optString("payload", ""),
            p.optString("speech", "हम्म, सुन रही हूँ।")
        )

        private fun fallback(msg: String): AgentDecision {
            val l = msg.lowercase()
            return when {
                l.contains("instagram") -> AgentDecision("OPEN_APP", "instagram", "", "हाँ, Instagram खोल रही हूँ।")
                l.contains("whatsapp") -> AgentDecision("OPEN_APP", "whatsapp", "", "WhatsApp खोल दिया।")
                l.contains("youtube") -> AgentDecision("OPEN_APP", "youtube", "", "YouTube ओपन कर रही हूँ।")
                l.contains("home") || l.contains("होम") -> AgentDecision("SYSTEM_ACTION", "HOME", "", "होम स्क्रीन पर आ गई।")
                l.contains("back") || l.contains("पीछे") -> AgentDecision("SYSTEM_ACTION", "BACK", "", "बैक कर दिया।")
                l.contains("reel") || l.contains("रील") -> AgentDecision("SYSTEM_ACTION", "SWIPE_UP", "", "अगली reel!")
                else -> AgentDecision("CHAT", "", "", "हम्म, बताओ ना क्या बात है?")
            }
        }
    }

    inner class AppAutomation(private val ctx: Context) {
        fun open(pkg: String) {
            try {
                ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(it)
                }
            } catch (_: Exception) {}
        }

        fun openByName(name: String) {
            val l = name.lowercase()
            when {
                l.contains("whatsapp") -> open("com.whatsapp")
                l.contains("instagram") -> open("com.instagram.android")
                l.contains("youtube") -> open("com.google.android.youtube")
                l.contains("facebook") -> open("com.facebook.katana")
                l.contains("telegram") -> open("org.telegram.messenger")
                l.contains("spotify") -> open("com.spotify.music")
                else -> {
                    try {
                        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        ctx.packageManager.queryIntentActivities(i, 0).forEach { app ->
                            if (app.loadLabel(ctx.packageManager).toString().lowercase().contains(l)) {
                                open(app.activityInfo.packageName)
                                return
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    inner class HybridCrypto {
        private val k = "DRAKOX_V600_AES256_API_KEY_SECURE".toByteArray().copyOf(32)
        fun encrypt(s: String): String = try {
            val c = Cipher.getInstance("AES")
            c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(k, "AES"))
            Base64.encodeToString(c.doFinal(s.toByteArray()), Base64.DEFAULT)
        } catch (_: Exception) { "" }
        fun decrypt(s: String): String = try {
            val c = Cipher.getInstance("AES")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(k, "AES"))
            String(c.doFinal(Base64.decode(s, Base64.DEFAULT)))
        } catch (_: Exception) { "" }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [10] HUD GRAPHICS
    // ══════════════════════════════════════════════════════════════════════════════════════
    inner class MatrixRainView(c: Context) : View(c) {
        private val r = Random()
        private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        private val drops = Array(150) {
            floatArrayOf(r.nextFloat() * 2000f, r.nextFloat() * -3000f, r.nextFloat() * 15f + 5f)
        }
        private var clr = "#00F0FF"
        fun updateTheme(s: SystemState) {
            clr = when (s) {
                SystemState.ERROR -> "#FF0033"
                SystemState.LISTENING -> "#00FF99"
                SystemState.SPEAKING -> "#FF9100"
                else -> "#00F0FF"
            }
        }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv)
            p.color = Color.parseColor(clr); p.textSize = 21f
            for (d in drops) {
                cv.drawText(r.nextInt(2).toString(), d[0], d[1], p)
                d[1] += d[2]
                if (d[1] > height) {
                    d[1] = -100f
                    d[0] = r.nextFloat() * width
                    d[2] = r.nextFloat() * 15f + 5f
                }
            }
            invalidate()
        }
    }

    inner class CyberRadarView(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
        private var swp = 0f; private var clr = "#00F0FF"; private var rms = 0f
        fun updateTheme(s: SystemState) {
            clr = when (s) {
                SystemState.ERROR -> "#FF0033"
                SystemState.LISTENING -> "#00FF99"
                else -> "#00F0FF"
            }
        }
        fun updateAudioWave(r: Float) { rms = r * 15f; invalidate() }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv)
            val cx = width / 2f; val cy = height / 2f; val r = 360f
            p.color = Color.parseColor(clr); p.alpha = 45
            cv.drawCircle(cx, cy, r, p); cv.drawCircle(cx, cy, r * 0.65f, p)
            if (rms > 0) {
                p.alpha = 220; p.color = Color.parseColor("#00FF99")
                cv.drawCircle(cx, cy, r + rms, p); rms *= 0.8f
            }
            p.style = Paint.Style.FILL; p.alpha = 30
            cv.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), swp, 45f, true, p)
            swp = (swp + 4.5f) % 360f
            p.style = Paint.Style.STROKE
            invalidate()
        }
    }

    inner class ParticleEmitterView(c: Context) : View(c) {
        private val r = Random()
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val parts = Array(80) { Particle() }
        private var clr = "#00F0FF"
        inner class Particle {
            var x = r.nextFloat() * 1500f
            var y = r.nextFloat() * 3000f
            var vx = r.nextFloat() * 6 - 3
            var vy = r.nextFloat() * 6 - 3
            var rad = r.nextFloat() * 4 + 2
        }
        fun updateTheme(s: SystemState) {
            clr = when (s) {
                SystemState.ERROR -> "#FF0033"
                SystemState.LISTENING -> "#00FF99"
                else -> "#00F0FF"
            }
        }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv)
            p.color = Color.parseColor(clr); p.alpha = 70
            for (pt in parts) {
                cv.drawCircle(pt.x, pt.y, pt.rad, p)
                pt.x += pt.vx; pt.y += pt.vy
                if (pt.x < 0 || pt.x > width) pt.vx *= -1
                if (pt.y < 0 || pt.y > height) pt.vy *= -1
            }
            invalidate()
        }
    }

    inner class HoloCompassView(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        private var rot = 0f; private var clr = "#00F0FF"
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
            super.onDraw(cv)
            val cx = width / 2f; val cy = height / 2f; val r = 460f
            cv.save(); cv.rotate(rot, cx, cy)
            p.color = Color.parseColor(clr); p.alpha = 25
            val path = Path().apply {
                moveTo(cx, cy - r - 20f)
                lineTo(cx + 20f, cy - r + 20f)
                lineTo(cx - 20f, cy - r + 20f)
                close()
            }
            p.style = Paint.Style.FILL; cv.drawPath(path, p)
            p.style = Paint.Style.STROKE
            cv.drawCircle(cx, cy, r + 30f, p)
            cv.restore()
        }
    }

    inner class SpectrumView(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f; strokeCap = Paint.Cap.ROUND
        }
        private val bars = FloatArray(30)
        private var clr = "#00FF99"
        fun updateTheme(s: SystemState) {
            clr = if (s == SystemState.SPEAKING) "#FF9100" else "#00FF99"
        }
        fun updateWaveform(rms: Float) {
            for (i in 0 until bars.size - 1) bars[i] = bars[i + 1]
            bars[bars.size - 1] = rms * 20f
            invalidate()
        }
        override fun onDraw(cv: Canvas) {
            super.onDraw(cv)
            p.color = Color.parseColor(clr); p.alpha = 160
            val cx = width / 2f; val cy = height - 250f; val w = 20f
            val startX = cx - ((bars.size * w) / 2)
            for (i in bars.indices) {
                cv.drawLine(startX + (i * w), cy, startX + (i * w), cy - bars[i], p)
                bars[i] *= 0.85f
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════════
    // [11] LIFECYCLE HOOKS
    // ══════════════════════════════════════════════════════════════════════════════════════
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> if (event.values[0] < 5f && ttsEngine.isSpeaking) {
                try { ttsEngine.stop() } catch (_: Exception) {}
            }
            Sensor.TYPE_GYROSCOPE -> compassHUDView?.applyRotation(event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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
        try { ttsEngine.shutdown() } catch (_: Exception) {}
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        aiCallJob?.cancel()
    }
}