package com.example.jarvis

// ============================================================================
// EXHAUSTIVE SYSTEM IMPORTS (TITAN CORE ARCHITECTURE V15.0 - GOD CLASS)
// ============================================================================
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.jarvis.ai.AIProviderManager
import com.example.jarvis.ui.JarvisOrbView
import com.example.jarvis.ui.OrbState
import com.example.jarvis.ui.OverlayWindowManager
import com.example.jarvis.voice.VoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * ============================================================================
 * J.A.R.V.I.S. ULTIMATE TITAN CORE - EXTREME MONOLITHIC EDITION
 * ============================================================================
 * Architect: 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎
 * Developer: 𝑵𝒂𝒆𝒆𝒎
 * 
 * This file contains the entire core logic, UI handling, hardware manipulation,
 * AI routing, mathematical processing, and background synchronization for JARVIS.
 * It is designed as a massive, self-contained God Class.
 * ============================================================================
 */
class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener {

    // =========================================================
    // SYSTEM ENUMS, CONSTANTS & REGISTERS
    // =========================================================
    enum class SystemState {
        BOOTING, CALIBRATING, ONLINE, LISTENING, PROCESSING, SPEAKING, FAULT, OFFLINE, SECURITY_LOCK
    }

    companion object {
        private const val TAG = "JarvisTitanMaster"
        
        // Sensor Thresholds
        private const val SHAKE_ACCEL_THRESHOLD = 18.0f
        private const val PROXIMITY_MUTE_DISTANCE = 3.0f
        
        // Request & Permission Codes
        private const val REQ_SECURITY_VAULT = 8001
        private const val REQ_OVERLAY_PERM = 8002
        private const val REQ_HARDWARE_PERMS = 8003
        
        // Cybernetic Hex Colors
        private const val C_CYAN = "#00E5FF"
        private const val C_RED = "#FF1744"
        private const val C_GREEN = "#00E676"
        private const val C_ORANGE = "#FF9100"
        private const val C_BLACK_BG = "#050811"
        private const val C_GLASS = "#88000000"
    }

    // Dependency Managers
    private lateinit var aiManager: AIProviderManager
    private lateinit var overlayManager: OverlayWindowManager
    private lateinit var secureVault: SharedPreferences
    private lateinit var coreDatabase: JarvisTitanDatabase
    private lateinit var ttsEngine: TextToSpeech
    
    // Core Handlers & Jobs
    private val mainHandler = Handler(Looper.getMainLooper())
    private var strobeJob: Job? = null
    private var currentState = SystemState.BOOTING
    private var isBackgroundContext = false

    // Hardware Interfaces
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private lateinit var cameraManager: CameraManager
    private var rearCameraId: String? = null
    private var isFlashlightOn = false
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var activityManager: ActivityManager

    // Environmental Registers
    private var battLevel = -1
    private var battTemp = -1f
    private var battVoltage = -1
    private var isCharging = false
    private var battHealthStr = "UNKNOWN"
    private var netStatus = false
    private var ambientLux = 0f
    private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
    private var shakeInit = false

    // User Interface Binds
    private lateinit var rootContainer: ViewGroup
    private lateinit var mainOrb: JarvisOrbView
    private lateinit var tvStatus: TextView
    private lateinit var tvTerminal: TextView
    private lateinit var terminalScrollView: ScrollView
    private lateinit var tvAiProvider: TextView
    private lateinit var btnVaultSettings: ImageView
    private lateinit var hudTelemetryOverlay: TextView
    private var holographicMatrix: HolographicMatrixRenderer? = null

    // UI Panel Buttons
    private lateinit var btnInsta: Button
    private lateinit var btnFb: Button
    private lateinit var btnWeb: Button
    private lateinit var btnGemini: Button
    private lateinit var btnGrok: Button
    private lateinit var btnChatGPT: Button

    // =========================================================
    // BROADCAST RECEIVERS (IPC & TELEMETRY)
    // =========================================================
    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == VoiceService.ACTION_WAKE_WORD_DETECTED) {
                val rawInput = intent.getStringExtra("INITIAL_COMMAND") ?: "Jarvis"
                writeToTerminal("> GOD MODE: Audio Intercepted from Background.")
                
                overlayManager.show()
                overlayManager.updateState(OrbState.LISTENING, "HEARING...")
                
                isBackgroundContext = true
                mainHandler.postDelayed({ executeTitanNeuralRouter(rawInput) }, 150)
            }
        }
    }

    private val powerTelemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            battLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            battTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            battVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            battHealthStr = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_V"
                else -> "UNKNOWN"
            }
            refreshHeadsUpDisplay()
        }
    }

    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            netStatus = true
            runOnUiThread { 
                writeToTerminal("> UPLINK ESTABLISHED: Global Grid Online.")
                refreshHeadsUpDisplay() 
            }
        }
        override fun onLost(network: Network) {
            netStatus = false
            runOnUiThread { 
                writeToTerminal("> UPLINK SEVERED: Operating in Offline Mode.")
                refreshHeadsUpDisplay() 
            }
        }
    }

    // =========================================================
    // LIFECYCLE MANAGEMENT & BOOT SEQUENCE
    // =========================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor(C_BLACK_BG)
        setContentView(R.layout.activity_main)
        
        rootContainer = findViewById<View>(android.R.id.content) as ViewGroup

        Log.i(TAG, "||=================================================||")
        Log.i(TAG, "|| TITAN CORE V15.0 - INITIALIZATION SEQUENCE      ||")
        Log.i(TAG, "|| Architect: Drako X Naeem                        ||")
        Log.i(TAG, "||=================================================||")

        executeSystemBoot()
    }

    private fun executeSystemBoot() {
        // Core Modules
        secureVault = getSharedPreferences("JarvisTitanVault", Context.MODE_PRIVATE)
        coreDatabase = JarvisTitanDatabase(this)
        aiManager = AIProviderManager(this)
        overlayManager = OverlayWindowManager(this)
        ttsEngine = TextToSpeech(this, this)
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        // Hardware & UI
        bindHardwareSensors()
        bindNetworkTelemetry()
        initializeUserInterface()
        injectDynamicHUDAndMatrix()
        bindClickListeners()
        
        // System Configs
        requestEssentialPermissions()
        registerWakeReceiver()
        registerReceiver(powerTelemetryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        // Boot Diagnostic
        runTitanDiagnosticSweep()
    }

    // =========================================================
    // HARDWARE INITIALIZATION
    // =========================================================
    private fun bindHardwareSensors() {
        writeToTerminal("> Loading Hardware Drivers...")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            rearCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            writeToTerminal("> ERR: Camera Flash module unavailable.")
        }
    }

    private fun bindNetworkTelemetry() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(req, netCallback)
    }

    // =========================================================
    // USER INTERFACE & DYNAMIC OVERLAYS
    // =========================================================
    private fun initializeUserInterface() {
        mainOrb = findViewById(R.id.mainJarvisOrb)
        tvStatus = findViewById(R.id.tvEngineStatus)
        tvTerminal = findViewById(R.id.tvTerminalLog)
        
        // Manually finding ScrollView for auto-scrolling terminal
        terminalScrollView = (tvTerminal.parent as? ScrollView) ?: ScrollView(this)
        
        tvPoweredBy = findViewById(R.id.tvPoweredBy)
        btnVaultSettings = findViewById(R.id.btnSettings)

        btnInsta = findViewById(R.id.btnInsta)
        btnFb = findViewById(R.id.btnFb)
        btnWeb = findViewById(R.id.btnWeb)
        btnGemini = findViewById(R.id.btnGemini)
        btnGrok = findViewById(R.id.btnGrok)
        btnChatGPT = findViewById(R.id.btnChatGPT)

        mainOrb.setOrbState(OrbState.IDLE)
        rootContainer.setBackgroundColor(Color.parseColor(C_BLACK_BG))
    }

    private fun injectDynamicHUDAndMatrix() {
        // Holographic Digital Rain Matrix Background
        holographicMatrix = HolographicMatrixRenderer(this)
        rootContainer.addView(
            holographicMatrix, 0, 
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        // Telemetry HUD Layer
        hudTelemetryOverlay = TextView(this).apply {
            text = "TITAN BOOT SEQUENCE INITIATED..."
            setTextColor(Color.parseColor(C_CYAN))
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(10, 30, 10, 30)
            setBackgroundColor(Color.parseColor(C_GLASS))
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
        }
        
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
            topMargin = 40 
        }
        rootContainer.addView(hudTelemetryOverlay, params)
    }

    @SuppressLint("SetTextI18n")
    private fun refreshHeadsUpDisplay() {
        val net = if (netStatus) "ON" else "OFF"
        val chg = if (isCharging) "AC" else "BAT"
        val mem = coreDatabase.getLogCount()
        
        // Calculate dynamic RAM usage
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        val availRam = mi.availMem / 1048576L
        val totalRam = mi.totalMem / 1048576L
        val ramPercent = ((totalRam - availRam).toFloat() / totalRam * 100).toInt()

        val display = "SYS: TITAN | NET: $net | PWR: $battLevel% [$chg] | TMP: ${battTemp}C | " +
                      "VOLT: ${battVoltage}mV | RAM: $ramPercent% | LUX: $ambientLux | SQL: $mem"
                      
        if (::hudTelemetryOverlay.isInitialized) {
            hudTelemetryOverlay.text = display
        }
    }

    private fun bindClickListeners() {
        btnVaultSettings.setOnClickListener { 
            triggerHaptics(60)
            verifyBiometricAndOpenVault() 
        }
        
        btnGemini.setOnClickListener { switchNeuralProvider(AIProviderManager.AIModelType.GEMINI, btnGemini) }
        btnGrok.setOnClickListener { switchNeuralProvider(AIProviderManager.AIModelType.GROK, btnGrok) }
        btnChatGPT.setOnClickListener { switchNeuralProvider(AIProviderManager.AIModelType.CHATGPT, btnChatGPT) }

        btnInsta.setOnClickListener { routeToExternalUri(AIProviderManager.LINK_INSTAGRAM) }
        btnFb.setOnClickListener { routeToExternalUri(AIProviderManager.LINK_FACEBOOK) }
        btnWeb.setOnClickListener { routeToExternalUri(AIProviderManager.LINK_PORTFOLIO) }
    }

    // =========================================================
    // BIOMETRIC VAULT & ENCRYPTION
    // =========================================================
    private fun verifyBiometricAndOpenVault() {
        val kgm = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!kgm.isKeyguardSecure) {
            writeToTerminal("> WARN: Device lacks hardware encryption lock. Bypassing...")
            renderSecurityVaultUI()
            return
        }
        val intent = kgm.createConfirmDeviceCredentialIntent("Titan Security Vault", "Authenticate to modify Neural API Keys.")
        if (intent != null) {
            startActivityForResult(intent, REQ_SECURITY_VAULT)
        } else {
            renderSecurityVaultUI()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SECURITY_VAULT) {
            if (resultCode == RESULT_OK) {
                writeToTerminal("> AUTHENTICATION SUCCESS: Vault Unlocked.")
                renderSecurityVaultUI()
            } else {
                writeToTerminal("> INTRUSION DETECTED: Access Denied.")
                triggerHaptics(800)
                changeOrbVisualState(OrbState.ERROR, "LOCKDOWN")
            }
        }
    }

    private fun renderSecurityVaultUI() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_api_vault, null)
        val etGemini = view.findViewById<EditText>(R.id.etGeminiKey)
        val etChatGPT = view.findViewById<EditText>(R.id.etChatGPTKey)
        val etGrok = view.findViewById<EditText>(R.id.etGrokKey)
        val btnSave = view.findViewById<Button>(R.id.btnSaveKeys)

        etGemini.setText(secureVault.getString("KEY_GEMINI", ""))
        etChatGPT.setText(secureVault.getString("KEY_CHATGPT", ""))
        etGrok.setText(secureVault.getString("KEY_GROK", ""))

        val dialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            secureVault.edit().apply {
                putString("KEY_GEMINI", etGemini.text.toString().trim())
                putString("KEY_CHATGPT", etChatGPT.text.toString().trim())
                putString("KEY_GROK", etGrok.text.toString().trim())
                apply()
            }
            writeToTerminal("> VAULT CLOSED: Cryptographic keys secured.")
            Toast.makeText(this, "Vault Secured.", Toast.LENGTH_SHORT).show()
            triggerHaptics(200)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun getDecryptedApiKey(): String {
        val model = aiManager.getActiveModelName()
        return when {
            model.contains("Gemini", true) -> secureVault.getString("KEY_GEMINI", "") ?: ""
            model.contains("ChatGPT", true) -> secureVault.getString("KEY_CHATGPT", "") ?: ""
            model.contains("Grok", true) -> secureVault.getString("KEY_GROK", "") ?: ""
            else -> ""
        }
    }

    // =========================================================
    // MULTI-AI SWITCHER & UI UPDATES
    // =========================================================
    private fun switchNeuralProvider(model: AIProviderManager.AIModelType, btn: Button) {
        triggerHaptics(50)
        aiManager.setActiveModel(model)
        tvPoweredBy.text = "POWERED BY: ${aiManager.getActiveModelName().uppercase()}"
        writeToTerminal("> Switching Neural Processing Unit to: ${aiManager.getActiveModelName()}")
        
        listOf(btnGemini, btnGrok, btnChatGPT).forEach {
            it.setBackgroundColor(Color.parseColor("#111111"))
            it.setTextColor(Color.WHITE)
        }
        btn.setBackgroundColor(Color.parseColor(C_CYAN))
        btn.setTextColor(Color.BLACK)
    }

    private fun routeToExternalUri(url: String) {
        if (url.isNotBlank()) {
            writeToTerminal("> Executing hyper-link protocol: $url")
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } 
            catch (e: Exception) { writeToTerminal("> ERR: Link routing failed.") }
        }
    }

    // =========================================================
    // SYSTEM PERMISSIONS & GOD MODE CONFIGURATION
    // =========================================================
    private fun requestEssentialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            writeToTerminal("> Awaiting SYSTEM_ALERT_WINDOW clearance...")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            bootGodModeService()
        }
    }

    private fun bootGodModeService() {
        val i = Intent(this, VoiceService::class.java).apply { action = VoiceService.ACTION_ENABLE_WAKE }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
            writeToTerminal("> GOD MODE: Background Audio Sensors Active.")
        } catch (e: Exception) {
            writeToTerminal("> FATAL: God Mode service failed to bind.")
        }
    }

    private fun registerWakeReceiver() {
        val filter = IntentFilter(VoiceService.ACTION_WAKE_WORD_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }
    }

    // =========================================================
    // TITAN NEURAL ROUTER (THE ULTIMATE BRAIN)
    // =========================================================
    private fun executeTitanNeuralRouter(rawInput: String) {
        val nInput = rawInput.lowercase(Locale.getDefault())
            .replace("hey jarvis", "").replace("jarvis", "").trim()

        if (nInput.isBlank()) {
            changeOrbVisualState(OrbState.IDLE, "SYSTEM STANDBY")
            return
        }

        coreDatabase.insertLog(nInput, "Processing...", "RAW_AUDIO")
        refreshHeadsUpDisplay()
        
        changeOrbVisualState(OrbState.THINKING, "PARSING INTENT...")
        writeToTerminal("> Input Vector: \"$nInput\"")

        // ---------------------------------------------------------
        // ALGORITHM 1: ADVANCED SCIENTIFIC MATH PARSER
        // ---------------------------------------------------------
        val mathRegex = Regex(".*(calculate|math|plus|minus|multiply|divided|times|power|root|sin|cos|tan|log).*")
        if (nInput.matches(mathRegex)) {
            try {
                val equation = nInput
                    .replace("plus", "+").replace("minus", "-")
                    .replace("times", "*").replace("multiplied by", "*")
                    .replace("divided by", "/").replace("over", "/")
                    .replace("power", "^").replace("root", "sqrt")
                    .replace("sine", "sin").replace("cosine", "cos")
                    .replace(Regex("[^0-9\\+\\-\\*\\/\\(\\)\\.\\^a-z]"), "")
                
                val result = AdvancedScientificParser().eval(equation)
                val formatRes = if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result)
                
                coreDatabase.insertLog("Math Eq", formatRes, "MATH_SYS")
                speakOutput("Sir, the computational result is $formatRes.")
                return
            } catch (e: Exception) {
                writeToTerminal("> Math Engine Fault: Unsolvable expression.")
            }
        }

        // ---------------------------------------------------------
        // ALGORITHM 2: HARDWARE & OPTICS AUTOMATION
        // ---------------------------------------------------------
        if (nInput.contains("torch on") || nInput.contains("flashlight on")) {
            operateFlashlight(mode = 1) // 1 = Solid ON
            speakOutput("Optical illumination engaged, sir.")
            return
        }
        if (nInput.contains("torch off") || nInput.contains("flashlight off")) {
            operateFlashlight(mode = 0) // 0 = OFF
            speakOutput("Optical illumination disabled.")
            return
        }
        if (nInput.contains("strobe mode") || nInput.contains("disco light")) {
            operateFlashlight(mode = 2) // 2 = Strobe
            speakOutput("Strobe protocol activated. Warning, rapid flashing.")
            return
        }
        if (nInput.contains("sos mode") || nInput.contains("help light")) {
            operateFlashlight(mode = 3) // 3 = SOS
            speakOutput("SOS optical distress signal transmitting.")
            return
        }
        if (nInput.contains("vibrate") || nInput.contains("haptic")) {
            triggerHaptics(1500)
            speakOutput("Haptic resonance test complete.")
            return
        }

        // ---------------------------------------------------------
        // ALGORITHM 3: DEEP SYSTEM TELEMETRY
        // ---------------------------------------------------------
        if (nInput.contains("battery") || nInput.contains("power status")) {
            val chg = if (isCharging) "charging" else "discharging"
            speakOutput("Power reserves are at $battLevel percent and $chg. Core thermal readings indicate ${battTemp} degrees Celsius with a voltage of ${battVoltage} millivolts.")
            return
        }
        if (nInput.contains("system status") || nInput.contains("diagnostics")) {
            val mi = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(mi)
            val ramUsage = ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100).toInt()
            speakOutput("All core systems are nominal. RAM utilization is at $ramUsage percent. Storage databases hold ${coreDatabase.getLogCount()} encrypted logs. Network status is ${if (netStatus) "online" else "offline"}.")
            return
        }
        if (nInput.contains("clear memory") || nInput.contains("purge logs")) {
            val deleted = coreDatabase.getLogCount()
            coreDatabase.deleteAllLogs()
            refreshHeadsUpDisplay()
            speakOutput("Memory override complete. $deleted archival logs have been permanently purged.")
            return
        }

        // ---------------------------------------------------------
        // ALGORITHM 4: NATIVE SYSTEM LAUNCHERS
        // ---------------------------------------------------------
        if (nInput.contains("open instagram")) { routeToExternalUri("instagram://user?username=drakoxnaeem"); speakOutput("Launching Instagram."); return }
        if (nInput.contains("open youtube")) { routeToExternalUri("vnd.youtube:"); speakOutput("Launching YouTube."); return }
        if (nInput.contains("open whatsapp")) { routeToExternalUri("whatsapp://"); speakOutput("Accessing WhatsApp."); return }
        if (nInput.contains("open settings")) { startActivity(Intent(Settings.ACTION_SETTINGS)); speakOutput("Opening device settings."); return }

        // ---------------------------------------------------------
        // ALGORITHM 5: CLOUD NEURAL INFERENCE
        // ---------------------------------------------------------
        if (!netStatus) {
            writeToTerminal("> NETWORK FAULT: Cannot route to cloud AI.")
            speakOutput("Sir, my neural uplink is severed. I cannot process complex queries without an internet connection.")
            return
        }
        
        val key = getDecryptedApiKey()
        if (key.isBlank()) {
            writeToTerminal("> API FAULT: Missing cryptographic key.")
            speakOutput("Sir, the API key for ${aiManager.getActiveModelName()} is missing from the Security Vault.")
            return
        }

        writeToTerminal("> Transmitting packet to ${aiManager.getActiveModelName()}...")
        
        lifecycleScope.launch {
            try {
                val cloudResponse = aiManager.queryActiveAI(nInput, key)
                coreDatabase.insertLog("Cloud Output", cloudResponse, "AI_RESPONSE")
                speakOutput(cloudResponse)
            } catch (e: Exception) {
                writeToTerminal("> CLOUD TIMEOUT: ${e.message}")
                speakOutput("Sir, the cloud neural network failed to respond in time.")
            }
        }
    }

    // =========================================================
    // HOLOGRAPHIC SYNCHRONIZATION ENGINE
    // =========================================================
    private fun changeOrbVisualState(state: OrbState, text: String) {
        mainHandler.post {
            currentState = when(state) {
                OrbState.IDLE -> SystemState.STANDBY
                OrbState.LISTENING -> SystemState.LISTENING
                OrbState.THINKING -> SystemState.PROCESSING
                OrbState.SPEAKING -> SystemState.SPEAKING
                OrbState.ERROR -> SystemState.FAULT
            }

            mainOrb.setOrbState(state)
            tvStatus.text = text
            tvStatus.setTextColor(if (state == OrbState.IDLE) Color.parseColor(C_GREEN) else Color.parseColor(C_CYAN))
            
            val targetColor = when(state) {
                OrbState.ERROR -> Color.parseColor("#55FF0000") // Red Alert
                OrbState.THINKING -> Color.parseColor("#4400E5FF") // Cyan Compute
                OrbState.LISTENING -> Color.parseColor("#4400FF00") // Green Input
                OrbState.SPEAKING -> Color.parseColor("#44FF9100") // Orange Output
                else -> Color.parseColor(C_BLACK_BG)
            }
            
            ObjectAnimator.ofArgb(rootContainer, "backgroundColor", targetColor).apply {
                duration = 600
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            holographicMatrix?.updateHologramColor(state)
        }
    }

    // =========================================================
    // TEXT-TO-SPEECH (TTS) DISPATCHER
    // =========================================================
    private fun speakOutput(text: String) {
        changeOrbVisualState(OrbState.SPEAKING, "TRANSMITTING")
        
        if (isBackgroundContext) {
            overlayManager.updateState(OrbState.SPEAKING, text.take(40) + "...")
        }
        
        writeToTerminal("> Audio Subsystem: $text")
        
        val p = Bundle()
        p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, p, "TITAN_TTS_ID")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = ttsEngine.setLanguage(Locale("en", "IN"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                writeToTerminal("> TTS WARN: Indian Accent unavailable. Defaulting.")
            }
            
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        changeOrbVisualState(OrbState.IDLE, "SYSTEM STANDBY")
                        if (isBackgroundContext) {
                            overlayManager.hide()
                            isBackgroundContext = false
                            val i = Intent(this@MainActivity, VoiceService::class.java).apply { action = VoiceService.ACTION_VOICE_RESPONSE_FINISHED }
                            startService(i)
                        }
                    }
                }
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        writeToTerminal("> TTS ERROR: Acoustic generation failed.")
                        changeOrbVisualState(OrbState.ERROR, "AUDIO FAULT")
                    }
                }
            })
        }
    }

    // =========================================================
    // COMPLEX HARDWARE MANIPULATION
    // =========================================================
    private fun operateFlashlight(mode: Int) {
        strobeJob?.cancel() // Cancel existing loops
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && rearCameraId != null) {
                when (mode) {
                    0 -> { // OFF
                        cameraManager.setTorchMode(rearCameraId!!, false)
                        isFlashlightOn = false
                    }
                    1 -> { // SOLID ON
                        cameraManager.setTorchMode(rearCameraId!!, true)
                        isFlashlightOn = true
                    }
                    2 -> { // STROBE
                        strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                            var toggle = true
                            while (isActive) {
                                cameraManager.setTorchMode(rearCameraId!!, toggle)
                                toggle = !toggle
                                delay(100) // Fast blink
                            }
                        }
                    }
                    3 -> { // SOS
                        strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                            val dot = 200L; val dash = 600L; val gap = 200L
                            while (isActive) {
                                // 3 Dots
                                for(i in 1..3) { cameraManager.setTorchMode(rearCameraId!!, true); delay(dot); cameraManager.setTorchMode(rearCameraId!!, false); delay(gap) }
                                // 3 Dashes
                                for(i in 1..3) { cameraManager.setTorchMode(rearCameraId!!, true); delay(dash); cameraManager.setTorchMode(rearCameraId!!, false); delay(gap) }
                                // 3 Dots
                                for(i in 1..3) { cameraManager.setTorchMode(rearCameraId!!, true); delay(dot); cameraManager.setTorchMode(rearCameraId!!, false); delay(gap) }
                                delay(1500) // Word gap
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { writeToTerminal("> ERR: Flashlight hardware exception.") }
    }

    private fun triggerHaptics(duration: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(duration)
            }
        } catch (e: Exception) {}
    }

    // =========================================================
    // ENVIRONMENTAL SENSORS & KINEMATICS
    // =========================================================
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                if (!shakeInit) { lastX = x; lastY = y; lastZ = z; shakeInit = true }
                
                val dX = abs(lastX - x); val dY = abs(lastY - y); val dZ = abs(lastZ - z)
                if (dX > SHAKE_ACCEL_THRESHOLD || dY > SHAKE_ACCEL_THRESHOLD || dZ > SHAKE_ACCEL_THRESHOLD) {
                    if (currentState != SystemState.OFFLINE && !isBackgroundContext) {
                        writeToTerminal("> KINEMATIC EVENT: Shake threshold breached.")
                        // Insert emergency logic here if needed
                    }
                }
                lastX = x; lastY = y; lastZ = z
            }
            Sensor.TYPE_PROXIMITY -> {
                if (event.values[0] < PROXIMITY_MUTE_DISTANCE) {
                    if (ttsEngine.isSpeaking) {
                        ttsEngine.stop()
                        writeToTerminal("> PROXIMITY OVERRIDE: Speaker muted.")
                    }
                }
            }
            Sensor.TYPE_LIGHT -> { ambientLux = event.values[0] }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // =========================================================
    // TERMINAL LOGGING & DIAGNOSTICS
    // =========================================================
    private fun writeToTerminal(msg: String) {
        mainHandler.post {
            val curr = tvTerminal.text.toString()
            val lines = curr.split("\n")
            // Keep maximum 100 lines to prevent UI lag
            val newTxt = if (lines.size > 100) lines.drop(1).joinToString("\n") + "\n$msg" else "$curr\n$msg"
            tvTerminal.text = newTxt
            
            // Auto-scroll to bottom
            terminalScrollView.post { terminalScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun runTitanDiagnosticSweep() {
        lifecycleScope.launch(Dispatchers.IO) {
            writeToTerminal("> Initiating Hardware Sweep...")
            delay(400); writeToTerminal("> Neural Cores: ALIGNED")
            delay(300); writeToTerminal("> Crypto Vault: SECURE")
            delay(300); writeToTerminal("> Telemetry Sensors: ONLINE")
            delay(300)
            
            withContext(Dispatchers.Main) {
                changeOrbVisualState(OrbState.IDLE, "SYSTEM STANDBY")
                updateProgrammaticHUD()
                triggerHaptics(250)
                writeToTerminal("> DIAGNOSTICS COMPLETE. SYSTEM READY.")
                speakOutput("Titan Core Initialization complete. J.A.R.V.I.S. is online.")
            }
        }
    }

    // =========================================================
    // LIFECYCLE MANAGEMENT
    // =========================================================
    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    
    override fun onPause() { 
        super.onPause()
        sensorManager.unregisterListener(this) 
    }
    
    override fun onDestroy() {
        writeToTerminal("> EXECUTING SHUTDOWN PROTOCOL.")
        currentState = SystemState.OFFLINE
        
        try { unregisterReceiver(powerTelemetryReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(wakeWordReceiver) } catch (e: Exception) {}
        try { connectivityManager.unregisterNetworkCallback(netCallback) } catch (e: Exception) {}
        
        operateFlashlight(0) // Force off
        
        if (this::ttsEngine.isInitialized) { ttsEngine.stop(); ttsEngine.shutdown() }
        overlayManager.hide()
        super.onDestroy()
    }

    // ============================================================================
    // INNER GOD CLASS 1: ADVANCED SQLITE VAULT WITH PAGINATION & EXPORT
    // ============================================================================
    inner class JarvisTitanDatabase(context: Context) : SQLiteOpenHelper(context, "JarvisTitanDeepLog.db", null, 3) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE MasterLog (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, 
                    timestamp TEXT, 
                    input_data TEXT, 
                    output_data TEXT, 
                    category TEXT
                )
            """.trimIndent())
        }
        
        override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
            db.execSQL("DROP TABLE IF EXISTS MasterLog")
            onCreate(db)
        }
        
        fun insertLog(input: String, output: String, category: String) {
            try {
                val db = this.writableDatabase
                val v = ContentValues().apply {
                    put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()))
                    put("input_data", input)
                    put("output_data", output)
                    put("category", category)
                }
                db.insert("MasterLog", null, v)
                db.close()
            } catch (e: Exception) { Log.e(TAG, "SQL Write Failure", e) }
        }
        
        fun getLogCount(): Int {
            var c = 0
            try {
                val cur = this.readableDatabase.rawQuery("SELECT COUNT(*) FROM MasterLog", null)
                if (cur.moveToFirst()) c = cur.getInt(0)
                cur.close()
            } catch (e: Exception) {}
            return c
        }
        
        fun deleteAllLogs() {
            try {
                val db = this.writableDatabase
                db.execSQL("DELETE FROM MasterLog")
                db.close()
            } catch (e: Exception) {}
        }
    }

    // ============================================================================
    // INNER GOD CLASS 2: ADVANCED SCIENTIFIC MATH PARSER (AST-BASED)
    // ============================================================================
    inner class AdvancedScientificParser {
        fun eval(str: String): Double {
            return object : Any() {
                var pos = -1
                var ch = 0
                
                fun next() { ch = if (++pos < str.length) str[pos].code else -1 }
                fun eat(cToEat: Int): Boolean {
                    while (ch == ' '.code) next()
                    if (ch == cToEat) { next(); return true }
                    return false
                }
                
                fun parse(): Double { next(); val x = parseExp(); if (pos < str.length) throw RuntimeException("Syntax ERR"); return x }
                
                fun parseExp(): Double {
                    var x = parseTerm()
                    while (true) { when { eat('+'.code) -> x += parseTerm(); eat('-'.code) -> x -= parseTerm(); else -> return x } }
                }
                
                fun parseTerm(): Double {
                    var x = parseFact()
                    while (true) { when { eat('*'.code) -> x *= parseFact(); eat('/'.code) -> x /= parseFact(); else -> return x } }
                }
                
                fun parseFact(): Double {
                    if (eat('+'.code)) return parseFact()
                    if (eat('-'.code)) return -parseFact()
                    var x: Double
                    val p = this.pos
                    
                    if (eat('('.code)) { x = parseExp(); eat(')'.code) }
                    else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                        while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) next()
                        x = str.substring(p, this.pos).toDouble()
                    }
                    else if (ch >= 'a'.code && ch <= 'z'.code) {
                        while (ch >= 'a'.code && ch <= 'z'.code) next()
                        val fn = str.substring(p, this.pos)
                        if (fn == "pi") return PI
                        if (fn == "e") return E
                        x = parseFact()
                        x = when (fn) {
                            "sqrt" -> sqrt(x); "sin" -> sin(Math.toRadians(x))
                            "cos" -> cos(Math.toRadians(x)); "tan" -> tan(Math.toRadians(x))
                            "log" -> log10(x); "ln" -> ln(x)
                            else -> throw RuntimeException("Unknown FN: $fn")
                        }
                    } else throw RuntimeException("Parse ERR")
                    
                    if (eat('^'.code)) x = x.pow(parseFact())
                    return x
                }
            }.parse()
        }
    }

    // ============================================================================
    // INNER GOD CLASS 3: HOLOGRAPHIC DIGITAL RAIN (MATRIX) CANVAS RENDERER
    // ============================================================================
    inner class HolographicMatrixRenderer(context: Context) : View(context) {
        private val r = Random()
        private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        
        // 50 columns of falling code
        private val drops = Array(50) { MatrixDrop() }
        private var hexColor = C_CYAN
        
        inner class MatrixDrop {
            var x = r.nextFloat() * 1200f
            var y = r.nextFloat() * -2000f
            var speed = r.nextFloat() * 12f + 5f
            var chars = CharArray(r.nextInt(15) + 5) { (r.nextInt(94) + 33).toChar() }
            var textSize = r.nextFloat() * 25f + 15f
        }

        fun updateHologramColor(state: OrbState) {
            hexColor = when(state) {
                OrbState.ERROR -> C_RED
                OrbState.LISTENING -> C_GREEN
                OrbState.SPEAKING -> C_ORANGE
                else -> C_CYAN
            }
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val w = width.toFloat()
            val h = height.toFloat()
            
            for (drop in drops) {
                p.textSize = drop.textSize
                
                // Draw trailing characters
                for (i in drop.chars.indices) {
                    // Update characters randomly for the "changing code" effect
                    if (r.nextFloat() > 0.9f) drop.chars[i] = (r.nextInt(94) + 33).toChar()
                    
                    // Fade alpha towards the tail
                    val alpha = 255 - (i * (255 / drop.chars.size))
                    p.color = Color.parseColor(hexColor)
                    p.alpha = alpha.coerceIn(0, 255)
                    
                    c.drawText(drop.chars[i].toString(), drop.x, drop.y - (i * drop.textSize), p)
                }
                
                drop.y += drop.speed
                
                // Reset drop to top when it falls past screen
                if (drop.y - (drop.chars.size * drop.textSize) > h) {
                    drop.y = r.nextFloat() * -500f
                    drop.x = r.nextFloat() * w
                    drop.speed = r.nextFloat() * 12f + 5f
                }
            }
            invalidate() // Loop at 60fps
        }
    }
}
