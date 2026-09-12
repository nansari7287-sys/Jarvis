package com.example.jarvis

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jarvis.ai.JarvisCommand
import com.example.jarvis.automation.CommandExecutor
import com.example.jarvis.data.PreferencesManager
import com.example.jarvis.ui.ChatAdapter
import com.example.jarvis.ui.JarvisOrbView
import com.example.jarvis.ui.MainViewModel
import com.example.jarvis.ui.OrbState
import com.example.jarvis.ui.VoiceOverlayManager
import com.example.jarvis.utils.PermissionHelper
import com.example.jarvis.voice.SpeechRecognizerManager
import com.example.jarvis.voice.TextToSpeechManager
import com.example.jarvis.voice.VoiceSessionManager
import com.example.jarvis.voice.VoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.Executor
import kotlin.math.abs

/**
 * ============================================================================
 * J.A.R.V.I.S. MAIN NEURAL INTERFACE (ULTIMATE MONOLITHIC EDITION v9.0)
 * ============================================================================
 * Architect: Drako X Naeem
 * Lines of Code Target: Maximum Extensibility
 *
 * SUBSYSTEMS INCLUDED:
 * 1. JarvisDatabaseHelper (SQL History Logging)
 * 2. AdvancedMathParser (Offline BODMAS Engine)
 * 3. MatrixParticleView (Canvas Background Graphics)
 * 4. HardwareTelemetryManager (Battery, RAM, Network)
 * 5. BiometricSecurityVault (Keyguard Authentication)
 * 6. Dynamic Programmatic HUD (To eliminate XML Unresolved Reference errors)
 * ============================================================================
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    // =========================================================
    // ENUMS & CONSTANTS
    // =========================================================

    enum class SystemState {
        INITIALIZING,
        ONLINE,
        STANDBY,
        DIAGNOSTIC,
        CRITICAL_FAULT,
        OFFLINE // FIXED: Added OFFLINE to prevent unresolved reference error!
    }

    companion object {
        private const val CREATOR_INSTAGRAM = "https://www.instagram.com/drakoxnaeem"
        private const val CREATOR_FACEBOOK = "https://www.facebook.com/share/1BsGJAatqh/"
        private const val CREATOR_PORTFOLIO = "https://frexxy-portfolio-3dri.vercel.app/#projects"
        
        private const val REQ_CODE_OVERLAY = 9001
        private const val REQ_CODE_SECURITY = 9002
        private const val SHAKE_THRESHOLD = 15.0f
        
        private const val HUD_COLOR_CYAN = "#00E5FF"
        private const val HUD_COLOR_RED = "#FF0000"
        private const val HUD_COLOR_GREEN = "#00FF00"
    }

    // =========================================================
    // DEPENDENCY DECLARATIONS
    // =========================================================
    
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var prefs: PreferencesManager
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var localDatabase: JarvisDatabaseHelper

    // Voice & Audio Engines
    private lateinit var speechRecognizerManager: SpeechRecognizerManager
    private lateinit var textToSpeechManager: TextToSpeechManager
    private lateinit var voiceSessionManager: VoiceSessionManager
    private lateinit var voiceOverlayManager: VoiceOverlayManager
    private lateinit var audioManager: AudioManager

    // Hardware & Telemetry Managers
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magneticSensor: Sensor? = null
    
    private lateinit var cameraManager: CameraManager
    private var mainCameraId: String? = null
    private var isTorchActive = false
    private lateinit var connectivityManager: ConnectivityManager

    // UI View Bindings
    private lateinit var micToggleButton: ImageButton
    private lateinit var messageInputBox: EditText
    private lateinit var sendCommandButton: ImageButton
    private lateinit var holographicOrbView: JarvisOrbView
    private lateinit var mainRecyclerView: RecyclerView
    private lateinit var masterRootLayout: ViewGroup
    
    // Dynamic Programmatic Views
    private lateinit var dynamicTelemetryHUD: TextView
    private var matrixBackground: MatrixParticleView? = null

    // State Variables
    private var isBackgroundCommandExecuting = false
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val TAG = "JarvisTitanCore"
    private var currentSystemState = SystemState.INITIALIZING

    // Telemetry Registers
    private var currentBatteryLevel = -1
    private var currentBatteryTemp = -1f
    private var isDeviceCharging = false
    private var isNetworkAvailable = false
    private var ambientLightLux = 0f

    // Sensor Logic Registers
    private var accelLastX = 0f
    private var accelLastY = 0f
    private var accelLastZ = 0f
    private var isShakeInitialized = false

    // =========================================================
    // BROADCAST RECEIVERS
    // =========================================================

    private val batteryTelemetryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            currentBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isDeviceCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
            
            updateProgrammaticHUD()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isNetworkAvailable = true
            runOnUiThread { 
                updateProgrammaticHUD() 
            }
        }
        
        override fun onLost(network: Network) {
            isNetworkAvailable = false
            runOnUiThread { 
                updateProgrammaticHUD() 
            }
        }
    }

    // =========================================================
    // ACTIVITY LIFECYCLE: ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        
        // Securely bind the root layout
        val root = findViewById<View>(android.R.id.content)
        if (root is ViewGroup) {
            masterRootLayout = root
        } else {
            throw IllegalStateException("Root view is not a ViewGroup")
        }

        Log.i(TAG, "==================================================")
        Log.i(TAG, "SYSTEM BOOT: J.A.R.V.I.S. TITAN CORE v9.0")
        Log.i(TAG, "Architect: Drako X Naeem")
        Log.i(TAG, "==================================================")

        executeTitanInitialization()
    }

    private fun executeTitanInitialization() {
        initializeDatabasesAndLogic()
        initializeCoreManagers()
        initializeHardwareSubsystems()
        initializeNetworkSubsystem()
        
        bindUserInterface()
        injectProgrammaticHUD() // Eliminates XML Reference Errors
        
        setupChatRecyclerView()
        setupVoiceNeuralEngine()
        setupAICloudListener()
        setupInteractiveClickListeners()
        setupBottomNavigation()
        setupQuickActionDashboard()
        observeViewModelState()

        handleIncomingVoiceIntent(intent)
        
        registerReceiver(batteryTelemetryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        runStartupDiagnosticSequence()
    }

    // =========================================================
    // CORE INITIALIZATION METHODS
    // =========================================================

    private fun initializeDatabasesAndLogic() {
        Log.d(TAG, "Initializing SQLite Database...")
        localDatabase = JarvisDatabaseHelper(this)
    }

    private fun initializeCoreManagers() {
        Log.d(TAG, "Initializing Core VM and Executors...")
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initializeExecutor(this)
        
        prefs = PreferencesManager(this)
        commandExecutor = CommandExecutor(this)
        
        speechRecognizerManager = SpeechRecognizerManager(this)
        textToSpeechManager = TextToSpeechManager(this)
        voiceOverlayManager = VoiceOverlayManager(this)
        
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun initializeHardwareSubsystems() {
        Log.d(TAG, "Binding Hardware Sensors...")
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        try { 
            mainCameraId = cameraManager.cameraIdList.firstOrNull() 
        } catch (e: Exception) {
            Log.e(TAG, "Camera hardware not found", e)
        }
    }

    private fun initializeNetworkSubsystem() {
        Log.d(TAG, "Binding Network Telemetry...")
        
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun bindUserInterface() {
        Log.d(TAG, "Binding UI Components from XML...")
        
        messageInputBox = findViewById(R.id.messageInput)
        micToggleButton = findViewById(R.id.micButton)
        sendCommandButton = findViewById(R.id.sendButton)
        holographicOrbView = findViewById(R.id.jarvisOrbView)
        mainRecyclerView = findViewById(R.id.messageRecyclerView)
        
        holographicOrbView.setOrbState(OrbState.IDLE)

        // Inject Dynamic Background Matrix
        matrixBackground = MatrixParticleView(this)
        
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        masterRootLayout.addView(matrixBackground, 0, layoutParams)
    }

    // =========================================================
    // DYNAMIC HUD INJECTION (THE FIX FOR SYSTEMSTATUSBAR)
    // =========================================================

    private fun injectProgrammaticHUD() {
        dynamicTelemetryHUD = TextView(this).apply {
            text = "J.A.R.V.I.S. | INITIALIZING CORE SUBSYSTEMS..."
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
            setBackgroundColor(Color.parseColor("#33000000"))
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
            topMargin = 60 // Margin for device notch/cutout
        }

        masterRootLayout.addView(dynamicTelemetryHUD, params)
        Log.i(TAG, "Dynamic HUD successfully injected.")
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgrammaticHUD() {
        val net = if (isNetworkAvailable) "ON" else "OFF"
        val chg = if (isDeviceCharging) "AC" else "BAT"
        val mem = localDatabase.getHistoryCount()
        
        val text = "JARVIS CORE | NET:$net | PWR:$currentBatteryLevel% [$chg] | TMP:${currentBatteryTemp}°C | MEM:$mem Logs"
        
        if (::dynamicTelemetryHUD.isInitialized) {
            dynamicTelemetryHUD.text = text
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter()
        
        mainRecyclerView.layoutManager = LinearLayoutManager(this).apply { 
            stackFromEnd = true 
        }
        
        mainRecyclerView.adapter = chatAdapter
    }

    // =========================================================
    // VOICE NEURAL ENGINE (FOREGROUND LISTENER)
    // =========================================================

    private fun setupVoiceNeuralEngine() {
        voiceSessionManager = VoiceSessionManager(
            context = this,
            speechRecognizer = speechRecognizerManager,
            
            onText = { recognizedText ->
                mainThreadHandler.post {
                    val cleanText = recognizedText.trim()
                    
                    if (cleanText.isNotBlank()) {
                        messageInputBox.setText(cleanText)
                        messageInputBox.setSelection(messageInputBox.length())
                        evaluateAndExecuteMasterCommand(cleanText)
                    }
                }
            },
            
            onStateChanged = { sessionState ->
                mainThreadHandler.post {
                    when (sessionState) {
                        VoiceSessionManager.State.IDLE -> {
                            synchronizeHolographicState(OrbState.IDLE, "Standby")
                        }
                        VoiceSessionManager.State.LISTENING -> {
                            synchronizeHolographicState(OrbState.LISTENING, "Listening...")
                        }
                        VoiceSessionManager.State.PROCESSING -> {
                            synchronizeHolographicState(OrbState.THINKING, "Synthesizing...")
                        }
                        VoiceSessionManager.State.SPEAKING -> {
                            synchronizeHolographicState(OrbState.SPEAKING, "Transmitting...")
                        }
                    }
                }
            },
            
            onError = { errorCode ->
                mainThreadHandler.post {
                    synchronizeHolographicState(OrbState.ERROR, "Acoustic Error")
                    Log.w(TAG, "Acoustic Engine Error: $errorCode")
                }
            }
        )
    }

    // =========================================================
    // HOLOGRAPHIC SYNCHRONIZER (IPC & UI)
    // =========================================================

    private fun synchronizeHolographicState(state: OrbState, subText: String) {
        // Update Views
        holographicOrbView.setOrbState(state)
        voiceOverlayManager.updateState(state)
        
        // Update UI Button Transparency
        micToggleButton.alpha = if (state == OrbState.LISTENING) 1.0f else 0.7f

        // Broadcast to God Mode
        val syncIntent = Intent(VoiceService.ACTION_UPDATE_STATE).apply {
            putExtra(VoiceService.EXTRA_STATE, state.name)
        }
        sendBroadcast(syncIntent)

        // Animate Environment
        animateBackgroundTint(state)
    }

    private fun animateBackgroundTint(state: OrbState) {
        val targetColor = when(state) {
            OrbState.ERROR -> Color.parseColor("#33FF0000") // Red Tint
            OrbState.THINKING -> Color.parseColor("#2200E5FF") // Cyan Tint
            OrbState.LISTENING -> Color.parseColor("#2200FF00") // Green Tint
            else -> Color.parseColor("#000000") // Pitch Black
        }
        
        val animator = ObjectAnimator.ofArgb(
            masterRootLayout, 
            "backgroundColor", 
            targetColor
        )
        
        animator.duration = 400
        animator.start()
    }

    // =========================================================
    // GEMINI CLOUD AI INTERCEPTOR
    // =========================================================

    private fun setupAICloudListener() {
        viewModel.setResponseListener { networkResponse ->
            mainThreadHandler.post {
                val safeResponse = validateCloudResponse(networkResponse)
                
                // Write Log to Local SQLite
                localDatabase.logInteraction("Cloud Query", safeResponse, "API_RESPONSE")
                updateProgrammaticHUD()

                if (voiceSessionManager.isActive()) {
                    executeVoiceOutputInForeground(safeResponse)
                } else if (isBackgroundCommandExecuting) {
                    executeVoiceOutputInBackground(safeResponse)
                } else {
                    synchronizeHolographicState(OrbState.SPEAKING, "Speaking")
                    
                    textToSpeechManager.speak(
                        text = safeResponse, 
                        onFinished = {
                            mainThreadHandler.post { 
                                synchronizeHolographicState(OrbState.IDLE, "Standby") 
                            }
                        }
                    )
                }
            }
        }
    }

    private fun validateCloudResponse(rawResponse: String): String {
        return if (
            rawResponse.contains("Unable to resolve host", ignoreCase = true) || 
            rawResponse.contains("Failed to connect", ignoreCase = true)
        ) {
            "Sir, the neural link to the cloud is severed. Please verify network integrity."
        } else {
            rawResponse
        }
    }

    // =========================================================
    // BACKGROUND IPC INTENT HANDLER (GOD MODE OVERRIDE)
    // =========================================================

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingVoiceIntent(intent)
    }

    private fun handleIncomingVoiceIntent(intent: Intent?) {
        if (intent?.action != VoiceService.ACTION_VOICE_COMMAND) return

        val backgroundCommand = intent.getStringExtra(VoiceService.EXTRA_COMMAND)?.trim().orEmpty()
        
        if (backgroundCommand.isEmpty()) return

        Log.i(TAG, "God Mode IPC Intercepted: $backgroundCommand")
        
        mainThreadHandler.post {
            isBackgroundCommandExecuting = true
            
            messageInputBox.setText(backgroundCommand)
            messageInputBox.setSelection(messageInputBox.length())
            
            evaluateAndExecuteMasterCommand(backgroundCommand)
            
            messageInputBox.text.clear()
        }

        intent.action = null
        intent.removeExtra(VoiceService.EXTRA_COMMAND)
    }

    // =========================================================
    // MASTER NLP ROUTER (ZERO-LATENCY OFFLINE ENGINE)
    // =========================================================

    private fun evaluateAndExecuteMasterCommand(rawInput: String) {
        val normalized = rawInput.lowercase(Locale.getDefault())
            .replace("hey jarvis", "")
            .replace("ok jarvis", "")
            .replace("jarvis", "")
            .trim()

        if (normalized.isBlank()) {
            if (voiceSessionManager.isActive()) {
                speakCommandFeedback("Yes sir, awaiting protocols.")
            } else {
                synchronizeHolographicState(OrbState.IDLE, "Standby")
            }
            return
        }

        // Log the query to the local database immediately
        localDatabase.logInteraction(normalized, "Processing...", "USER_QUERY")
        updateProgrammaticHUD()
        
        synchronizeHolographicState(OrbState.THINKING, "Parsing Semantic Intent...")

        // ROUTER 1: OFFLINE MATH SOLVER
        if (handleMathProtocols(normalized)) return

        // ROUTER 2: HARDWARE & SENSORS
        if (handleHardwareProtocols(normalized)) return

        // ROUTER 3: SYSTEM DIAGNOSTICS & TELEMETRY
        if (handleTelemetryProtocols(normalized)) return

        // ROUTER 4: NATIVE APP AUTOMATION
        if (handleAppAutomationProtocols(normalized)) return

        // ROUTER 5: DEVICE SETTINGS
        if (handleSettingsProtocols(normalized)) return

        // ROUTER 6: CLOUD INFERENCE (GEMINI FALLBACK)
        if (!isNetworkAvailable) {
            speakCommandFeedback("Network is offline sir. Complex neural queries cannot be processed locally.")
            return
        }
        
        Log.i(TAG, "Routing intent to Gemini Cloud AI: $normalized")
        
        if (voiceSessionManager.isActive()) {
            voiceSessionManager.setProcessing()
        }
        
        viewModel.send(normalized, prefs.getApiKey())
    }

    // =========================================================
    // NLP SUB-ROUTER: OFFLINE MATH PARSER
    // =========================================================

    private fun handleMathProtocols(command: String): Boolean {
        if (
            command.contains("calculate") || command.contains("math") || 
            command.contains("plus") || command.contains("minus") || 
            command.contains("divided")
        ) {
            try {
                // Convert text words to symbols for the parser
                val eq = command.replace("plus", "+")
                    .replace("minus", "-")
                    .replace("times", "*")
                    .replace("multiplied by", "*")
                    .replace("divided by", "/")
                    .replace("over", "/")
                    .replace(Regex("[^0-9\\+\\-\\*\\/\\(\\)\\.]"), "")
                
                if (eq.isNotEmpty()) {
                    val parser = AdvancedMathParser()
                    val result = parser.evaluate(eq)
                    
                    val formatted = if (result % 1.0 == 0.0) {
                        result.toInt().toString() 
                    } else {
                        String.format(Locale.getDefault(), "%.2f", result)
                    }
                    
                    speakCommandFeedback("Sir, the calculation results in $formatted.")
                    return true
                }
            } catch (e: Exception) { 
                Log.e(TAG, "Math Engine Error", e) 
            }
        }
        return false
    }

    // =========================================================
    // NLP SUB-ROUTER: HARDWARE & SENSORS
    // =========================================================

    private fun handleHardwareProtocols(command: String): Boolean {
        when {
            containsAny(command, "torch on", "flashlight on", "light on", "batti jalao") -> {
                toggleFlashlight(true)
                speakCommandFeedback("Flashlight engaged.")
                return true
            }
            containsAny(command, "torch off", "flashlight off", "light off", "batti bujhao") -> {
                toggleFlashlight(false)
                speakCommandFeedback("Flashlight disengaged.")
                return true
            }
            containsAny(command, "vibrate", "test haptics", "haptic engine") -> {
                triggerHapticFeedback(800)
                speakCommandFeedback("Haptic motors tested successfully.")
                return true
            }
            containsAny(command, "check light", "how dark", "ambient light") -> {
                speakCommandFeedback("Ambient light level is currently at $ambientLightLux lux.")
                return true
            }
        }
        return false
    }

    // =========================================================
    // NLP SUB-ROUTER: SYSTEM TELEMETRY
    // =========================================================

    private fun handleTelemetryProtocols(command: String): Boolean {
        when {
            containsAny(command, "battery status", "charge kitna", "battery level") -> {
                val stateStr = if (isDeviceCharging) "charging" else "discharging"
                speakCommandFeedback("Sir, the battery is at $currentBatteryLevel percent and is currently $stateStr. Core temperature is $currentBatteryTemp degrees Celsius.")
                return true
            }
            containsAny(command, "system status", "diagnostics", "health report") -> {
                val memoryInfo = getAvailableInternalMemorySize()
                speakCommandFeedback("All systems nominal. Battery at $currentBatteryLevel percent. $memoryInfo available in internal storage. Neural link is stable.")
                return true
            }
            containsAny(command, "clear memory", "delete history", "format logs") -> {
                localDatabase.clearMemory()
                updateProgrammaticHUD()
                speakCommandFeedback("Memory logs have been completely purged from the local database.")
                return true
            }
            containsAny(command, "what time", "current time", "samay kya") -> {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                speakCommandFeedback("The current time is $time, sir.")
                return true
            }
            containsAny(command, "what date", "today date", "aaj ki date") -> {
                val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                speakCommandFeedback("Today is $date.")
                return true
            }
        }
        return false
    }

    // =========================================================
    // NLP SUB-ROUTER: NATIVE APP AUTOMATION
    // =========================================================

    private fun handleAppAutomationProtocols(command: String): Boolean {
        when {
            containsAny(command, "instagram open", "instagram kholo", "open instagram") -> {
                openInstagram()
                speakCommandFeedback("Accessing Instagram servers.")
                return true
            }
            containsAny(command, "youtube open", "youtube kholo", "open youtube") -> {
                openYouTube()
                speakCommandFeedback("Initializing YouTube protocol.")
                return true
            }
            containsAny(command, "whatsapp open", "whatsapp kholo", "open whatsapp") -> {
                openWhatsApp()
                speakCommandFeedback("WhatsApp interface loaded.")
                return true
            }
            containsAny(command, "google open", "chrome open", "browser kholo") -> {
                openUrl("https://www.google.com")
                speakCommandFeedback("Opening global web search.")
                return true
            }
            containsAny(command, "open camera", "camera kholo", "start camera") -> {
                openCamera()
                speakCommandFeedback("Camera hardware engaged.")
                return true
            }
        }
        return false
    }

    // =========================================================
    // NLP SUB-ROUTER: SETTINGS & DEVICE
    // =========================================================

    private fun handleSettingsProtocols(command: String): Boolean {
        when {
            containsAny(command, "open settings", "phone settings") -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                speakCommandFeedback("Opening device settings.")
                return true
            }
            containsAny(command, "wifi settings", "internet settings") -> {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                speakCommandFeedback("Opening Wi-Fi configuration.")
                return true
            }
            containsAny(command, "bluetooth settings", "pair bluetooth") -> {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                speakCommandFeedback("Accessing Bluetooth configuration.")
                return true
            }
        }
        return false
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean {
        return phrases.any { text.contains(it) }
    }

    // =========================================================
    // HARDWARE MOTOR FUNCTIONS
    // =========================================================

    private fun toggleFlashlight(status: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainCameraId != null) {
                cameraManager.setTorchMode(mainCameraId!!, status)
                isTorchActive = status
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Hardware Exception: Torch toggle failed.", e) 
        }
    }

    private fun triggerHapticFeedback(durationMs: Long = 200) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                vibratorManager.defaultVibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Haptic exception occurred.") 
        }
    }

    private fun getAvailableInternalMemorySize(): String {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val gb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
            "$gb Gigabytes"
        } catch (e: Exception) { 
            "Unknown amount of" 
        }
    }

    // =========================================================
    // TEXT-TO-SPEECH OUTPUT DISPATCHERS
    // =========================================================

    private fun executeVoiceOutputInForeground(speechText: String) {
        if (!voiceSessionManager.isActive()) return
        
        voiceSessionManager.setSpeaking()
        synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
        speechRecognizerManager.stop()

        textToSpeechManager.speak(
            text = speechText,
            
            onStarted = { 
                mainThreadHandler.post { 
                    synchronizeHolographicState(OrbState.SPEAKING, "Transmitting") 
                } 
            },
            
            onFinished = {
                mainThreadHandler.post {
                    if (voiceSessionManager.isActive()) {
                        voiceSessionManager.resumeListening()
                        synchronizeHolographicState(OrbState.LISTENING, "Listening...")
                    } else {
                        synchronizeHolographicState(OrbState.IDLE, "Standby")
                    }
                }
            }
        )
    }

    private fun executeVoiceOutputInBackground(speechText: String) {
        synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
        
        textToSpeechManager.speak(
            text = speechText,
            
            onStarted = { },
            
            onFinished = {
                mainThreadHandler.post {
                    isBackgroundCommandExecuting = false
                    synchronizeHolographicState(OrbState.IDLE, "Standby")
                    
                    try {
                        val resumeIntent = Intent(this, VoiceService::class.java).apply { 
                            action = VoiceService.ACTION_VOICE_RESPONSE_FINISHED 
                        }
                        startService(resumeIntent)
                    } catch (e: Exception) { 
                        Log.e(TAG, "Failed to signal background service.", e) 
                    }
                }
            }
        )
    }

    private fun speakCommandFeedback(feedbackText: String) {
        if (voiceSessionManager.isActive()) {
            executeVoiceOutputInForeground(feedbackText)
        } else if (isBackgroundCommandExecuting) {
            executeVoiceOutputInBackground(feedbackText)
        } else {
            synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
            textToSpeechManager.speak(
                text = feedbackText, 
                onFinished = { 
                    mainThreadHandler.post { 
                        synchronizeHolographicState(OrbState.IDLE, "Standby") 
                    } 
                }
            )
        }
    }

    // =========================================================
    // INTERACTIVE UI EVENT LISTENERS
    // =========================================================

    private fun setupInteractiveClickListeners() {
        sendCommandButton.setOnClickListener {
            val typedMessage = messageInputBox.text.toString().trim()
            if (typedMessage.isNotEmpty()) {
                synchronizeHolographicState(OrbState.THINKING, "Processing...")
                evaluateAndExecuteMasterCommand(typedMessage)
                messageInputBox.text.clear()
            }
        }

        micToggleButton.setOnClickListener {
            if (!PermissionHelper.hasAudioPermission(this)) {
                PermissionHelper.requestAudioPermission(this)
                return@setOnClickListener
            }
            if (voiceSessionManager.isActive()) {
                stopVoiceMode() 
            } else {
                startVoiceMode()
            }
        }

        findViewById<View>(R.id.settingsButton)?.setOnClickListener { 
            authenticateAndOpenSecurityVault() 
        }
        
        findViewById<View>(R.id.menuButton)?.setOnClickListener { 
            showNeuralOptionsMenu() 
        }
        
        findViewById<View>(R.id.searchButton)?.setOnClickListener { 
            showGlobalSearchDialog() 
        }
        
        findViewById<View>(R.id.historyButton)?.setOnClickListener { 
            showDatabaseHistory() 
        }
    }

    private fun startVoiceMode() {
        textToSpeechManager.stop()
        isBackgroundCommandExecuting = false
        
        voiceSessionManager.start()
        synchronizeHolographicState(OrbState.LISTENING, "Listening...")
        triggerHapticFeedback(100)
    }

    private fun stopVoiceMode() {
        voiceSessionManager.stop()
        speechRecognizerManager.stop()
        textToSpeechManager.stop()
        voiceOverlayManager.hide()
        
        synchronizeHolographicState(OrbState.IDLE, "Standby")
    }

    // =========================================================
    // DASHBOARD & QUICK NAVIGATION CARDS
    // =========================================================

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.chatTab)?.setOnClickListener { 
            triggerHapticFeedback(50) 
        }
        
        findViewById<View>(R.id.toolsTab)?.setOnClickListener { 
            showToolsDashboard() 
        }
        
        findViewById<View>(R.id.assistTab)?.setOnClickListener { 
            Toast.makeText(this, "Assist Active", Toast.LENGTH_SHORT).show() 
        }
    }

    private fun setupQuickActionDashboard() {
        findViewById<View>(R.id.webButton)?.setOnClickListener { 
            handleAppAutomationProtocols("google open") 
        }
        
        findViewById<View>(R.id.youtubeButton)?.setOnClickListener { 
            handleAppAutomationProtocols("youtube open") 
        }
        
        findViewById<View>(R.id.instagramButton)?.setOnClickListener { 
            handleAppAutomationProtocols("instagram open") 
        }
        
        findViewById<View>(R.id.whatsappButton)?.setOnClickListener { 
            handleAppAutomationProtocols("whatsapp open") 
        }
        
        findViewById<View>(R.id.appsButton)?.setOnClickListener { 
            showAppGrid() 
        }
        
        findViewById<View>(R.id.moreButton)?.setOnClickListener { 
            showExtendedMenu() 
        }
    }

    private fun observeViewModelState() {
        lifecycleScope.launch {
            viewModel.ui.collect { uiState ->
                chatAdapter.submitList(uiState.messages)
                if (uiState.messages.isNotEmpty()) {
                    mainRecyclerView.scrollToPosition(uiState.messages.lastIndex)
                }
            }
        }
    }

    // =========================================================
    // BIOMETRIC SECURITY PROTOCOL (NATIVE KEYGUARD)
    // =========================================================

    private fun authenticateAndOpenSecurityVault() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        
        if (!keyguardManager.isKeyguardSecure) {
            speakCommandFeedback("No device lock detected. Bypassing security.")
            showCoreSettingsDialog()
            return
        }
        
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(
            "J.A.R.V.I.S. Security Vault", 
            "Verify identity to access Core System Settings."
        )
        
        if (intent != null) {
            startActivityForResult(intent, REQ_CODE_SECURITY) 
        } else {
            showCoreSettingsDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_SECURITY) {
            if (resultCode == RESULT_OK) {
                speakCommandFeedback("Security cleared. Accessing core settings.")
                showCoreSettingsDialog()
            } else {
                speakCommandFeedback("Unauthorized user detected.")
                triggerHapticFeedback(500)
            }
        }
    }

    // =========================================================
    // DIALOGS, MENUS & DEVELOPER PROTOCOLS
    // =========================================================

    private fun showCoreSettingsDialog() {
        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }
        
        val apiKeyInputField = EditText(this).apply {
            hint = "Enter Gemini API Key"
            setSingleLine(true)
            setText(prefs.getApiKey())
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        containerLayout.addView(apiKeyInputField)

        val godModeSwitch = Switch(this).apply {
            text = "God Mode (Background Engine)"
            textSize = 14f
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            isChecked = prefs.isVoiceWakeEnabled()
            setPadding(0, 40, 0, 10)
        }
        containerLayout.addView(godModeSwitch)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("⚙ J.A.R.V.I.S. CORE CONFIGURATION")
            .setView(containerLayout)
            .setPositiveButton("SAVE & DEPLOY") { _, _ ->
                prefs.saveApiKey(apiKeyInputField.text.toString().trim())
                
                val godModeEnabled = godModeSwitch.isChecked
                prefs.setVoiceWakeEnabled(godModeEnabled)

                if (godModeEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        promptSystemOverlayPermission()
                        prefs.setVoiceWakeEnabled(false)
                        return@setPositiveButton
                    }
                    launchBackgroundVoiceService()
                } else {
                    terminateBackgroundVoiceService()
                }

                speakCommandFeedback("Settings updated and saved.")
            }
            .setNegativeButton("DISMISS", null)
            .show()
    }

    private fun promptSystemOverlayPermission() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ GOD MODE RESTRICTION")
            .setMessage("Please enable 'Display over other apps' to run God Mode.")
            .setPositiveButton("GRANT") { _, _ -> 
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), 
                    REQ_CODE_OVERLAY
                ) 
            }
            .setCancelable(false)
            .show()
    }

    private fun showDatabaseHistory() {
        val logsCount = localDatabase.getHistoryCount()
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("◷ MEMORY LOGS")
            .setMessage("JARVIS currently holds $logsCount interactions in the secure SQLite Memory Vault.\n\nSay 'Clear Memory' to wipe data.")
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun showGlobalSearchDialog() {
        val input = EditText(this).apply { 
            hint = "Enter global search query..." 
            setSingleLine(true) 
            setTextColor(Color.WHITE) 
        }
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("⌕ QUANTUM SEARCH")
            .setView(input)
            .setPositiveButton("EXECUTE") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    openUrl("https://www.google.com/search?q=" + Uri.encode(query))
                }
            }
            .show()
    }

    private fun showNeuralOptionsMenu() {
        val options = arrayOf(
            "⚙ Security Vault", 
            "🛠 Diagnostic Tools", 
            "◉ Neural Assist", 
            "▣ App Drawer", 
            "◆ Framework Status", 
            "✦ Architect Info"
        )
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ SYSTEM MENU")
            .setItems(options) { _, w ->
                when (w) {
                    0 -> authenticateAndOpenSecurityVault()
                    1 -> showToolsDashboard()
                    2 -> Toast.makeText(this, "Assist active.", Toast.LENGTH_SHORT).show()
                    3 -> showAppGrid()
                    4 -> showSystemInfo()
                    5 -> showArchitectInfo()
                }
            }
            .show()
    }

    private fun showToolsDashboard() {
        val items = arrayOf("🌐 Web Node", "▶ Media Stream", "◎ Social Feed", "◈ Comm Link")
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ PROTOCOLS")
            .setItems(items) { _, w ->
                when (w) { 
                    0 -> openUrl("https://www.google.com") 
                    1 -> openYouTube() 
                    2 -> openInstagram() 
                    3 -> openWhatsApp() 
                }
            }
            .show()
    }

    private fun showAppGrid() {
        val items = arrayOf("◎ Instagram", "▶ YouTube", "◈ WhatsApp", "G Search")
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ INTEGRATIONS")
            .setItems(items) { _, w ->
                when (w) { 
                    0 -> openInstagram() 
                    1 -> openYouTube() 
                    2 -> openWhatsApp() 
                    3 -> openUrl("https://www.google.com") 
                }
            }
            .show()
    }

    private fun showExtendedMenu() {
        val items = arrayOf("⚙ Secured Core", "◷ Telemetry Log", "◆ Specs", "✦ Creator")
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ EXTENDED MENU")
            .setItems(items) { _, w ->
                when (w) { 
                    0 -> authenticateAndOpenSecurityVault() 
                    1 -> showDatabaseHistory() 
                    2 -> showSystemInfo() 
                    3 -> showArchitectInfo() 
                }
            }
            .show()
    }

    private fun showSystemInfo() {
        val message = "Architect: 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎\n" +
                      "Type: Monolithic AI Framework\n" +
                      "Network: $isNetworkAvailable\n" +
                      "Battery: $currentBatteryLevel%\n" +
                      "Status: Fully Operational"
                      
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("◆ J.A.R.V.I.S. V9.0 (TITAN)")
            .setMessage(message)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun showArchitectInfo() {
        val options = arrayOf("◎ Instagram", "f Facebook", "⌂ Cyber-Portfolio")
        
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ ARCHITECT")
            .setMessage("Developed By 𝑵𝒂𝒆𝒎\n𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎")
            .setItems(options) { _, w ->
                when (w) { 
                    0 -> openInstagram() 
                    1 -> openUrl(CREATOR_FACEBOOK) 
                    2 -> openUrl(CREATOR_PORTFOLIO) 
                }
            }
            .setNegativeButton("DISMISS", null)
            .show()
    }

    // =========================================================
    // INTENT LAUNCHERS & ROUTERS
    // =========================================================

    private fun openInstagram() { 
        try { 
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=drakoxnaeem"))) 
        } catch (e: Exception) { 
            openUrl(CREATOR_INSTAGRAM) 
        } 
    }
    
    private fun openYouTube() { 
        try { 
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"))) 
        } catch (e: Exception) { 
            openUrl("https://www.youtube.com") 
        } 
    }
    
    private fun openWhatsApp() { 
        try { 
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://"))) 
        } catch (e: Exception) { 
            openUrl("https://web.whatsapp.com") 
        } 
    }
    
    private fun openCamera() { 
        try { 
            startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) 
        } catch (e: Exception) { 
            Toast.makeText(this, "Camera access denied.", Toast.LENGTH_SHORT).show() 
        } 
    }
    
    private fun openUrl(url: String) { 
        if (url.isNotBlank()) {
            try { 
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) 
            } catch (e: Exception) {
                Log.e(TAG, "Failed to route to URL.", e)
            } 
        }
    }

    // =========================================================
    // BACKGROUND SERVICE LIFECYCLE (GOD MODE CONTROLS)
    // =========================================================

    private fun launchBackgroundVoiceService() {
        if (!PermissionHelper.hasAudioPermission(this)) { 
            PermissionHelper.requestAudioPermission(this)
            return 
        }
        
        val serviceIntent = Intent(this, VoiceService::class.java).apply { 
            action = VoiceService.ACTION_ENABLE_WAKE 
        }
        
        try { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent) 
            } else {
                startService(serviceIntent) 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to boot God Mode.", e)
        }
    }
    
    private fun terminateBackgroundVoiceService() { 
        try { 
            stopService(Intent(this, VoiceService::class.java)) 
        } catch (e: Exception) {} 
        
        isBackgroundCommandExecuting = false 
    }

    // =========================================================
    // HARDWARE SENSOR EVENT LISTENERS (SHAKE DETECTOR)
    // =========================================================

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                if (event.values[0] < (proximitySensor?.maximumRange ?: 5f)) {
                    // Safety Override: Instantly mute if sensor is covered
                    if (voiceSessionManager.isActive() || textToSpeechManager.isSpeaking()) {
                        textToSpeechManager.stop()
                        speechRecognizerManager.stop()
                    }
                }
            }
            Sensor.TYPE_LIGHT -> {
                ambientLightLux = event.values[0]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                if (!isShakeInitialized) { 
                    accelLastX = x 
                    accelLastY = y 
                    accelLastZ = z 
                    isShakeInitialized = true 
                }
                
                val deltaX = abs(accelLastX - x)
                val deltaY = abs(accelLastY - y)
                val deltaZ = abs(accelLastZ - z)
                
                if (deltaX > SHAKE_THRESHOLD || deltaY > SHAKE_THRESHOLD || deltaZ > SHAKE_THRESHOLD) {
                    // Shake to Wake Protocol
                    if (!voiceSessionManager.isActive() && PermissionHelper.hasAudioPermission(this)) {
                        startVoiceMode() 
                    }
                }
                
                accelLastX = x 
                accelLastY = y 
                accelLastZ = z
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // =========================================================
    // STARTUP DIAGNOSTICS & THREADING
    // =========================================================

    private fun runStartupDiagnosticSequence() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            withContext(Dispatchers.Main) {
                currentSystemState = SystemState.ONLINE
                updateProgrammaticHUD()
                triggerHapticFeedback(150)
                Log.i(TAG, "DIAGNOSTIC COMPLETE: Core systems fully engaged.")
            }
        }
    }

    // =========================================================
    // MASTER LIFECYCLE MANAGEMENT & MEMORY PURGE
    // =========================================================

    override fun onResume() {
        super.onResume()
        
        proximitySensor?.let { 
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) 
        }
        lightSensor?.let { 
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) 
        }
        accelerometerSensor?.let { 
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) 
        }
        
        if (::prefs.isInitialized && prefs.isVoiceWakeEnabled() && PermissionHelper.hasAudioPermission(this)) {
            launchBackgroundVoiceService()
        }
    }
    
    override fun onPause() { 
        super.onPause()
        sensorManager.unregisterListener(this) 
    }
    
    override fun onDestroy() {
        currentSystemState = SystemState.OFFLINE
        
        try { 
            unregisterReceiver(batteryTelemetryReceiver) 
        } catch (e: Exception) {}
        
        try { 
            connectivityManager.unregisterNetworkCallback(networkCallback) 
        } catch (e: Exception) {}
        
        if (isTorchActive) {
            toggleFlashlight(false)
        }
        
        try { voiceSessionManager.destroy() } catch (e: Exception) {}
        try { speechRecognizerManager.destroy() } catch (e: Exception) {}
        try { textToSpeechManager.shutdown() } catch (e: Exception) {}
        try { voiceOverlayManager.destroy() } catch (e: Exception) {}
        
        viewModel.setResponseListener(null)
        mainThreadHandler.removeCallbacksAndMessages(null)
        
        super.onDestroy()
    }

    // =========================================================
    // INTERNAL CLASS: SQLITE DATABASE (HISTORY CACHE)
    // =========================================================

    inner class JarvisDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "JarvisMemory.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            val createTableQuery = """
                CREATE TABLE MemoryLog (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT,
                    query TEXT,
                    response TEXT,
                    intent_type TEXT
                )
            """.trimIndent()
            db.execSQL(createTableQuery)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS MemoryLog")
            onCreate(db)
        }

        fun logInteraction(query: String, response: String, intentType: String) {
            try {
                val db = this.writableDatabase
                val values = ContentValues().apply {
                    put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    put("query", query)
                    put("response", response)
                    put("intent_type", intentType)
                }
                db.insert("MemoryLog", null, values)
                db.close()
            } catch (e: Exception) {
                Log.e(TAG, "SQL Write Error.", e)
            }
        }

        fun getHistoryCount(): Int {
            var count = 0
            try {
                val db = this.readableDatabase
                val cursor = db.rawQuery("SELECT COUNT(*) FROM MemoryLog", null)
                if (cursor.moveToFirst()) count = cursor.getInt(0)
                cursor.close()
                db.close()
            } catch (e: Exception) {}
            return count
        }
        
        fun clearMemory() {
            try {
                val db = this.writableDatabase
                db.execSQL("DELETE FROM MemoryLog")
                db.close()
            } catch (e: Exception) {}
        }
    }

    // =========================================================
    // INTERNAL CLASS: ADVANCED MATH PARSER (BODMAS)
    // =========================================================

    inner class AdvancedMathParser {
        fun evaluate(expression: String): Double {
            return object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < expression.length) expression[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        when {
                            eat('+'.code) -> x += parseTerm()
                            eat('-'.code) -> x -= parseTerm()
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        when {
                            eat('*'.code) || eat('x'.code) -> x *= parseFactor()
                            eat('/'.code) -> x /= parseFactor()
                            else -> return x
                        }
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()
                    var x: Double
                    val startPos = this.pos
                    if (eat('('.code)) {
                        x = parseExpression()
                        eat(')'.code)
                    } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                        while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                        x = expression.substring(startPos, this.pos).toDouble()
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }
                    return x
                }
            }.parse()
        }
    }

    // =========================================================
    // INTERNAL CLASS: MATRIX PARTICLE CANVAS
    // =========================================================

    inner class MatrixParticleView(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.parseColor(HUD_COLOR_CYAN)
            style = Paint.Style.FILL
            alpha = 40
        }
        
        private val particles = Array(60) { Particle() }
        private val random = Random()

        inner class Particle {
            var x = random.nextFloat() * 1500f
            var y = random.nextFloat() * 2500f
            var speed = random.nextFloat() * 6f + 1f
            var radius = random.nextFloat() * 3f + 1f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val width = width.toFloat()
            val height = height.toFloat()

            for (p in particles) {
                canvas.drawCircle(p.x, p.y, p.radius, paint)
                p.y += p.speed
                if (p.y > height) {
                    p.y = 0f
                    p.x = random.nextFloat() * width
                }
            }
            invalidate() // Frame loop
        }
    }
}
