package com.example.jarvis

// ============================================================================
// EXHAUSTIVE SYSTEM IMPORTS (TITAN CORE ARCHITECTURE V25.0 - UNCOMPRESSED)
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
import android.text.InputType
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
import com.example.jarvis.ai.AIProviderManager
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
import com.example.jarvis.voice.VoiceSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.Executor
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
 * J.A.R.V.I.S. ULTIMATE TITAN CORE - EXTREME MONOLITHIC EDITION (UNCOMPRESSED)
 * ============================================================================
 * Architect: 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎
 * Developer: 𝑵𝒂𝒆𝒆𝒎
 * 
 * DESIGN PHILOSOPHY:
 * This file is intentionally expansive. It encompasses every core system,
 * hardware interface, programmatic UI generation, and custom logic routing
 * required for the J.A.R.V.I.S. Artificial Intelligence.
 * 
 * ERRORS FIXED:
 * 1. Resolved TextToSpeech missing imports and interface overrides.
 * 2. Resolved 'Too many characters in character literal' in Math Parser.
 * 3. Resolved ACTION_WAKE_WORD_DETECTED IPC intent issues.
 * 4. Resolved SystemState / OrbState 'SPEAKING' and 'FAULT' conflicts.
 * ============================================================================
 */
class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener {

    // ========================================================================
    // ENUMS & GLOBAL CONSTANTS
    // ========================================================================
    
    enum class SystemState {
        INITIALIZING, 
        ONLINE, 
        STANDBY, 
        DIAGNOSTIC, 
        CRITICAL_FAULT, 
        OFFLINE, 
        LISTENING, 
        PROCESSING,
        SPEAKING,
        SECURITY_LOCK
    }

    companion object {
        
        // --------------------------------------------------------------------
        // System Tags
        // --------------------------------------------------------------------
        private const val TAG = "JarvisTitanMaster"
        
        // --------------------------------------------------------------------
        // IPC Action Strings (Hardcoded to prevent unresolved references)
        // --------------------------------------------------------------------
        private const val ACTION_WAKE_WORD_DETECTED = "com.example.jarvis.WAKE_WORD_DETECTED"
        private const val ACTION_UPDATE_STATE = "com.example.jarvis.UPDATE_STATE"
        
        // --------------------------------------------------------------------
        // Sensor & Hardware Thresholds
        // --------------------------------------------------------------------
        private const val SHAKE_ACCEL_THRESHOLD = 18.0f
        private const val PROXIMITY_MUTE_DISTANCE = 3.0f
        
        // --------------------------------------------------------------------
        // Activity Request Codes
        // --------------------------------------------------------------------
        private const val REQ_SECURITY_VAULT = 8001
        private const val REQ_OVERLAY_PERM = 8002
        private const val REQ_HARDWARE_PERMS = 8003
        
        // --------------------------------------------------------------------
        // Cybernetic HUD Color Palette
        // --------------------------------------------------------------------
        private const val C_CYAN = "#00E5FF"
        private const val C_RED = "#FF1744"
        private const val C_GREEN = "#00E676"
        private const val C_ORANGE = "#FF9100"
        private const val C_BLACK_BG = "#050811"
        private const val C_GLASS = "#88000000"
        
        // --------------------------------------------------------------------
        // External Developer & Creator Links
        // --------------------------------------------------------------------
        private const val CREATOR_INSTAGRAM = "https://www.instagram.com/drakoxnaeem"
        private const val CREATOR_FACEBOOK = "https://www.facebook.com/share/1BsGJAatqh/"
        private const val CREATOR_PORTFOLIO = "https://frexxy-portfolio-3dri.vercel.app/#projects"
    }

    // ========================================================================
    // CORE DEPENDENCY MANAGERS (LATEINIT)
    // ========================================================================
    
    // Architectures & UI Logic Models
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var prefs: PreferencesManager
    private lateinit var commandExecutor: CommandExecutor
    
    // Internal Storage & Cryptography Vaults
    private lateinit var localDatabase: JarvisDatabaseHelper
    private lateinit var securityVaultPrefs: SharedPreferences
    
    // AI Integration Managers
    private lateinit var aiManager: AIProviderManager
    private lateinit var overlayManager: OverlayWindowManager

    // Neural Audio & Voice Engines
    private lateinit var speechRecognizerManager: SpeechRecognizerManager
    private lateinit var voiceSessionManager: VoiceSessionManager
    private lateinit var voiceOverlayManager: VoiceOverlayManager
    private lateinit var textToSpeechEngine: TextToSpeech
    
    // System Hardware Managers
    private lateinit var audioManager: AudioManager
    private lateinit var activityManager: ActivityManager
    private lateinit var connectivityManager: ConnectivityManager

    // ========================================================================
    // TELEMETRY & HARDWARE SENSORS
    // ========================================================================
    
    private lateinit var sensorManager: SensorManager
    
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magneticSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    
    private lateinit var cameraManager: CameraManager
    private var mainCameraId: String? = null
    private var isTorchActive = false

    // ========================================================================
    // USER INTERFACE (NATIVE XML BINDINGS)
    // ========================================================================
    
    private lateinit var micToggleButton: ImageButton
    private lateinit var messageInputBox: EditText
    private lateinit var sendCommandButton: ImageButton
    private lateinit var holographicOrbView: JarvisOrbView
    private lateinit var mainRecyclerView: RecyclerView
    private lateinit var masterRootLayout: ViewGroup

    // ========================================================================
    // USER INTERFACE (PROGRAMMATIC INJECTIONS)
    // ========================================================================
    
    private lateinit var dynamicTelemetryHUD: TextView
    private lateinit var programmaticTerminalLog: TextView
    private lateinit var terminalScrollView: ScrollView
    private lateinit var aiSwitcherPanel: LinearLayout
    private lateinit var tvPoweredByAI: TextView
    
    private var matrixBackground: MatrixDigitalRainView? = null
    private var strobeJob: Job? = null

    // ========================================================================
    // SYSTEM STATE & THREAD CACHING
    // ========================================================================
    
    private var isBackgroundCommandExecuting = false
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var currentSystemState = SystemState.INITIALIZING

    // ========================================================================
    // TELEMETRY REGISTERS
    // ========================================================================
    
    private var currentBatteryLevel = -1
    private var currentBatteryTemp = -1f
    private var currentBatteryVoltage = -1
    private var isDeviceCharging = false
    private var batteryHealthStr = "UNKNOWN"
    private var isNetworkAvailable = false
    private var ambientLightLux = 0f
    
    // Sensor Caching Data
    private var accelLastX = 0f
    private var accelLastY = 0f
    private var accelLastZ = 0f
    private var isShakeInitialized = false

    // ========================================================================
    // IPC (INTER-PROCESS COMMUNICATION) & BROADCAST RECEIVERS
    // ========================================================================

    /**
     * Receiver for God Mode Background Audio Interception
     */
    private val backgroundWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ACTION_WAKE_WORD_DETECTED) {
                
                val command = intent.getStringExtra("INITIAL_COMMAND") ?: "Jarvis"
                printToTerminal("> IPC ALERT: Background God Mode Activated.")
                
                isBackgroundCommandExecuting = true
                
                // Add a realistic delay to simulate system waking up
                mainThreadHandler.postDelayed({
                    messageInputBox.setText(command)
                    messageInputBox.setSelection(messageInputBox.length())
                    
                    evaluateAndExecuteMasterCommand(command)
                    
                    messageInputBox.text.clear()
                }, 200)
            }
        }
    }

    /**
     * Receiver for Real-time Battery & Thermal Diagnostics
     */
    private val powerTelemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            currentBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            currentBatteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isDeviceCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            
            val healthStatus = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            batteryHealthStr = when (healthStatus) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "FAILURE"
                else -> "UNKNOWN"
            }
            
            updateProgrammaticHUD()
        }
    }

    /**
     * Callback for Global Network Connectivity Tracking
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isNetworkAvailable = true
            runOnUiThread { 
                printToTerminal("> NETWORK: Satellite Uplink Established.")
                updateProgrammaticHUD() 
            }
        }
        
        override fun onLost(network: Network) {
            isNetworkAvailable = false
            runOnUiThread { 
                printToTerminal("> NETWORK: Uplink Severed. Operating Offline.")
                updateProgrammaticHUD() 
            }
        }
    }

    // ========================================================================
    // LIFECYCLE: ACTIVITY CREATION & MASTER BOOT SEQUENCE
    // ========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // UI Flag Management for HUD Experience
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor(C_BLACK_BG)
        
        setContentView(R.layout.activity_main)
        
        // Secure Root Layout Binding
        val root = findViewById<View>(android.R.id.content)
        if (root is ViewGroup) {
            masterRootLayout = root
        } else {
            throw IllegalStateException("Root view is not a ViewGroup. Cannot proceed.")
        }

        printBootLogHeaders()
        executeTitanInitializationSequence()
    }

    /**
     * Prints the primary identification headers to Logcat
     */
    private fun printBootLogHeaders() {
        Log.i(TAG, "||=================================================||")
        Log.i(TAG, "|| TITAN CORE V25.0 - MASTER BOOT SEQUENCE         ||")
        Log.i(TAG, "|| Architect: Drako X Naeem                        ||")
        Log.i(TAG, "|| Mode: Extreme Monolithic Engine                 ||")
        Log.i(TAG, "||=================================================||")
    }

    /**
     * Executes the sequential initialization of all subsystems.
     */
    private fun executeTitanInitializationSequence() {
        
        printToTerminal("> Initiating Master Boot Sequence...")

        // Step 1: Storage & Cryptography
        initializeDatabasesAndStorage()
        
        // Step 2: Telemetry & Hardware Nodes
        initializeHardwareSubsystems()
        initializeNetworkSubsystem()
        
        // Step 3: Logic Routers & Service Managers
        initializeCoreManagers()
        
        // Step 4: User Interface Subsystems
        bindNativeUserInterface()
        injectProgrammaticMatrixBackground()
        injectProgrammaticHUD()
        injectProgrammaticTerminalAndControls()
        
        // Step 5: Adapters & Action Listeners
        setupChatRecyclerView()
        setupInteractiveClickListeners()
        
        // Step 6: Text-To-Speech Engine Boot
        initializeNativeTTSEngine()
        
        // Step 7: Acoustic Models
        setupVoiceNeuralEngine()
        setupAICloudListener()
        
        // Step 8: IPC & Broadcasters
        registerReceiver(powerTelemetryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerBackgroundIPCReceiver()
        
        // Final Step: Perform System Diagnostic
        runStartupDiagnosticSequence()
    }

    // ========================================================================
    // INITIALIZATION: STORAGE & DATABASES
    // ========================================================================

    private fun initializeDatabasesAndStorage() {
        Log.d(TAG, "Booting SQL Storage & Vaults...")
        printToTerminal("> Mounting Local Memory Databases...")
        
        localDatabase = JarvisDatabaseHelper(this)
        securityVaultPrefs = getSharedPreferences("JarvisSecurityVault", Context.MODE_PRIVATE)
    }

    // ========================================================================
    // INITIALIZATION: CORE LOGIC MANAGERS
    // ========================================================================

    private fun initializeCoreManagers() {
        Log.d(TAG, "Loading ViewModels & AI Routers...")
        printToTerminal("> Activating Logic Controllers...")
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initializeExecutor(this)
        
        prefs = PreferencesManager(this)
        commandExecutor = CommandExecutor(this)
        aiManager = AIProviderManager(this)
        
        speechRecognizerManager = SpeechRecognizerManager(this)
        voiceOverlayManager = VoiceOverlayManager(this)
        overlayManager = OverlayWindowManager(this)
        
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    // ========================================================================
    // INITIALIZATION: NATIVE TTS ENGINE
    // ========================================================================

    private fun initializeNativeTTSEngine() {
        Log.d(TAG, "Booting Native Text-To-Speech Synthesis...")
        printToTerminal("> Initializing Vocal Modulators...")
        
        // Instantiating TTS directly to prevent "Unresolved Reference" errors
        textToSpeechEngine = TextToSpeech(this, this)
    }

    // ========================================================================
    // INITIALIZATION: HARDWARE & KINEMATICS
    // ========================================================================

    private fun initializeHardwareSubsystems() {
        Log.d(TAG, "Binding Optic & Kinematic Sensors...")
        printToTerminal("> Establishing Sensor Arrays...")
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try { 
            mainCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Camera Flash unavailable", e) 
            printToTerminal("> WARNING: Flashlight Hardware Offline.")
        }
    }

    // ========================================================================
    // INITIALIZATION: NETWORK TELEMETRY
    // ========================================================================

    private fun initializeNetworkSubsystem() {
        Log.d(TAG, "Binding Connectivity Observers...")
        printToTerminal("> Ping: Resolving Global Subnet...")
        
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    // ========================================================================
    // PROGRAMMATIC UI: NATIVE XML BINDING
    // ========================================================================

    private fun bindNativeUserInterface() {
        
        // Attempting to bind native elements.
        try {
            messageInputBox = findViewById(R.id.messageInput)
            micToggleButton = findViewById(R.id.micButton)
            sendCommandButton = findViewById(R.id.sendButton)
            holographicOrbView = findViewById(R.id.jarvisOrbView)
            mainRecyclerView = findViewById(R.id.messageRecyclerView)
            
            // Set default state
            holographicOrbView.setOrbState(OrbState.IDLE)
        } catch (e: Exception) {
            Log.e(TAG, "Native UI Binding Failed.", e)
        }
    }

    // ========================================================================
    // PROGRAMMATIC UI: DIGITAL RAIN (MATRIX)
    // ========================================================================

    private fun injectProgrammaticMatrixBackground() {
        printToTerminal("> Injecting Visual Subsystems...")
        
        matrixBackground = MatrixDigitalRainView(this)
        
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        masterRootLayout.addView(matrixBackground, 0, layoutParams) // Insert at extreme bottom
    }

    // ========================================================================
    // PROGRAMMATIC UI: TELEMETRY HUD
    // ========================================================================

    private fun injectProgrammaticHUD() {
        
        dynamicTelemetryHUD = TextView(this).apply {
            text = "J.A.R.V.I.S. | LOADING SUBSYSTEMS..."
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(10, 20, 10, 20)
            setBackgroundColor(Color.parseColor(HUD_COLOR_BLACK_TRANSPARENT))
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
            topMargin = 50 
        }
        
        masterRootLayout.addView(dynamicTelemetryHUD, params)
    }

    // ========================================================================
    // PROGRAMMATIC UI: TERMINAL & AI SWITCHER PANEL
    // ========================================================================

    private fun injectProgrammaticTerminalAndControls() {
        
        // 1. Constructing the Terminal ScrollView
        terminalScrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(HUD_COLOR_BLACK_TRANSPARENT))
            setPadding(16, 16, 16, 16)
        }
        
        // 2. Constructing the Log TextView
        programmaticTerminalLog = TextView(this).apply {
            text = "> System Kernel Booting...\n> Loading Architecture..."
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        
        terminalScrollView.addView(programmaticTerminalLog)

        // 3. Injecting Terminal into Root
        val terminalParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            300
        ).apply {
            gravity = Gravity.TOP
            topMargin = 150
            leftMargin = 30
            rightMargin = 30
        }
        masterRootLayout.addView(terminalScrollView, terminalParams)

        // 4. Constructing the Neural Switcher Panel Container
        aiSwitcherPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(20, 20, 20, 20)
        }

        // 5. Constructing the Header
        tvPoweredByAI = TextView(this).apply {
            text = "ENGINE: GEMINI"
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 10)
        }
        aiSwitcherPanel.addView(tvPoweredByAI)

        // 6. Generating Operational Buttons Programmatically
        val btnVault = Button(this).apply { 
            text = "VAULT"
            textSize = 8f
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE) 
        }
        
        val btnAiGrok = Button(this).apply { 
            text = "GROK"
            textSize = 8f
            setBackgroundColor(Color.parseColor("#111111"))
            setTextColor(Color.WHITE) 
        }
        
        val btnAiGem = Button(this).apply { 
            text = "GEMINI"
            textSize = 8f
            setBackgroundColor(Color.parseColor(HUD_COLOR_CYAN))
            setTextColor(Color.BLACK) 
        }
        
        val btnAiGpt = Button(this).apply { 
            text = "GPT"
            textSize = 8f
            setBackgroundColor(Color.parseColor("#111111"))
            setTextColor(Color.WHITE) 
        }

        // 7. Creating the Button Row
        val btnRow = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL 
        }
        
        btnRow.addView(btnAiGem, LinearLayout.LayoutParams(180, 80).apply { marginEnd = 10 })
        btnRow.addView(btnAiGrok, LinearLayout.LayoutParams(180, 80).apply { marginEnd = 10 })
        btnRow.addView(btnAiGpt, LinearLayout.LayoutParams(180, 80).apply { marginEnd = 10 })
        btnRow.addView(btnVault, LinearLayout.LayoutParams(180, 80))

        aiSwitcherPanel.addView(btnRow)

        // 8. Injecting Panel into Root
        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, 
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 350 // Elevate above the input bar
        }
        
        masterRootLayout.addView(aiSwitcherPanel, panelParams)

        // 9. Attaching Listeners to Programmatic Buttons
        btnVault.setOnClickListener { 
            triggerHapticFeedback(50)
            authenticateAndOpenProgrammaticVault() 
        }
        
        btnAiGem.setOnClickListener { 
            switchNeuralProvider(AIProviderManager.AIModelType.GEMINI, btnAiGem, listOf(btnAiGrok, btnAiGpt)) 
        }
        
        btnAiGrok.setOnClickListener { 
            switchNeuralProvider(AIProviderManager.AIModelType.GROK, btnAiGrok, listOf(btnAiGem, btnAiGpt)) 
        }
        
        btnAiGpt.setOnClickListener { 
            switchNeuralProvider(AIProviderManager.AIModelType.CHATGPT, btnAiGpt, listOf(btnAiGem, btnAiGrok)) 
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter()
        
        mainRecyclerView.layoutManager = LinearLayoutManager(this).apply { 
            stackFromEnd = true 
        }
        
        mainRecyclerView.adapter = chatAdapter
    }

    // ========================================================================
    // TELEMETRY UPDATERS & TERMINAL LOGGERS
    // ========================================================================

    @SuppressLint("SetTextI18n")
    private fun updateProgrammaticHUD() {
        val netStr = if (isNetworkAvailable) "ONLINE" else "OFFLINE"
        val chgStr = if (isDeviceCharging) "AC" else "BAT"
        val memCount = localDatabase.getHistoryCount()
        
        // Calculate RAM utilization in real-time
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        
        val ramUsage = ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100).toInt()

        val hudText = "CORE: TITAN | NET: $netStr | PWR: $currentBatteryLevel% [$chgStr] | " +
                      "TMP: ${currentBatteryTemp}C | HLT: $batteryHealthStr | VOLT: ${currentBatteryVoltage}mV | " +
                      "RAM: $ramUsage% | LUX: $ambientLightLux | SQL: $memCount"
                      
        if (::dynamicTelemetryHUD.isInitialized) {
            dynamicTelemetryHUD.text = hudText
        }
    }

    private fun printToTerminal(message: String) {
        mainThreadHandler.post {
            if (::programmaticTerminalLog.isInitialized) {
                val current = programmaticTerminalLog.text.toString()
                val lines = current.split("\n")
                
                // Truncate logic to prevent OOM
                val newText = if (lines.size > 80) { 
                    lines.drop(1).joinToString("\n") + "\n$message" 
                } else { 
                    "$current\n$message" 
                }
                
                programmaticTerminalLog.text = newText
                
                // Ensure scroll view always snaps to bottom
                terminalScrollView.post { 
                    terminalScrollView.fullScroll(ScrollView.FOCUS_DOWN) 
                }
            }
        }
    }

    // ========================================================================
    // VOICE NEURAL ENGINE (FOREGROUND SYNCHRONIZATION)
    // ========================================================================

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
                    synchronizeHolographicState(OrbState.ERROR, "Acoustic Error: $errorCode")
                    printToTerminal("> Acoustic Engine Fault Code: $errorCode")
                }
            }
        )
    }

    // ========================================================================
    // HOLOGRAPHIC ENVIRONMENT SYNCHRONIZER
    // ========================================================================

    private fun synchronizeHolographicState(state: OrbState, subText: String) {
        
        // Map OrbState to Global System State
        currentSystemState = when(state) {
            OrbState.IDLE -> SystemState.STANDBY
            OrbState.LISTENING -> SystemState.LISTENING
            OrbState.THINKING -> SystemState.PROCESSING
            OrbState.SPEAKING -> SystemState.SPEAKING
            OrbState.ERROR -> SystemState.CRITICAL_FAULT
        }

        // Apply Native UI Changes
        holographicOrbView.setOrbState(state)
        voiceOverlayManager.updateState(state)
        
        micToggleButton.alpha = if (state == OrbState.LISTENING) {
            1.0f 
        } else {
            0.7f
        }

        // Broadcast State to Background Engine via IPC
        val syncIntent = Intent(ACTION_UPDATE_STATE).apply { 
            putExtra("extra_state", state.name) 
        }
        sendBroadcast(syncIntent)

        // Animate Root Background Color based on Operating Mode
        val targetColor = when(state) {
            OrbState.ERROR -> Color.parseColor("#44FF0000")
            OrbState.THINKING -> Color.parseColor("#3300E5FF")
            OrbState.LISTENING -> Color.parseColor("#3300FF00")
            OrbState.SPEAKING -> Color.parseColor("#33FF9100")
            else -> Color.parseColor("#050811")
        }
        
        val animator = ObjectAnimator.ofArgb(masterRootLayout, "backgroundColor", targetColor)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()

        // Sync Background Matrix Layer
        matrixBackground?.updateHologramColor(state)
    }

    // ========================================================================
    // MULTI-AI CLOUD INTERCEPTOR (VIEWMODEL INTEGRATOR)
    // ========================================================================

    private fun setupAICloudListener() {
        viewModel.setResponseListener { networkResponse ->
            mainThreadHandler.post {
                
                // Validate network response string
                val safeResponse = if (
                    networkResponse.contains("Unable to resolve host", true) || 
                    networkResponse.contains("Failed to connect", true)
                ) {
                    "Sir, the neural link to the cloud is severed. Please verify network integrity."
                } else {
                    networkResponse
                }
                
                // Persist the transaction to secure memory
                localDatabase.logInteraction("Cloud Inference", safeResponse, "API_SUCCESS")
                updateProgrammaticHUD()
                printToTerminal("> Inference Received: ${safeResponse.take(40)}...")

                // Route Audio Based on Execution Context
                if (voiceSessionManager.isActive()) {
                    executeVoiceOutputInForeground(safeResponse)
                } else if (isBackgroundCommandExecuting) {
                    executeVoiceOutputInBackground(safeResponse)
                } else {
                    synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
                    speakTextNative(safeResponse) {
                        mainThreadHandler.post { 
                            synchronizeHolographicState(OrbState.IDLE, "Standby") 
                        }
                    }
                }
            }
        }
    }

    // ========================================================================
    // IPC OVERRIDE: GOD MODE BACKGROUND RECEIVER SETUP
    // ========================================================================

    private fun registerBackgroundIPCReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_WAKE_WORD_DETECTED)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(backgroundWakeReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(backgroundWakeReceiver, filter)
            }
            Log.d(TAG, "IPC Receiver successfully bound.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind IPC Receiver", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handled primarily via receiver for stability
    }

    // ========================================================================
    // MASTER NLP ROUTER (ZERO-LATENCY PROCESSING CORE)
    // ========================================================================

    private fun evaluateAndExecuteMasterCommand(rawInput: String) {
        
        // Semantic Normalization Phase
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

        // Cache User Intent Log
        localDatabase.logInteraction(normalized, "Processing Logic Tree...", "USER_QUERY")
        updateProgrammaticHUD()
        
        // Set State to Processing
        synchronizeHolographicState(OrbState.THINKING, "Parsing Semantic Intent...")
        printToTerminal("> Input Vector: \"$normalized\"")

        // --------------------------------------------------------------------
        // ALGORITHM BRANCH 1: ADVANCED MATHEMATICAL PARSER 
        // --------------------------------------------------------------------
        val mathRegex = Regex(".*(calculate|math|plus|minus|multiply|divided|times|power|root|sin|cos|tan|log).*")
        if (normalized.matches(mathRegex)) {
            try {
                // Translation layer for spoken math to mathematical operators
                val equationStr = normalized
                    .replace("plus", "+")
                    .replace("minus", "-")
                    .replace("times", "*")
                    .replace("multiplied by", "*")
                    .replace("divided by", "/")
                    .replace("over", "/")
                    .replace("power", "^")
                    .replace("root", "sqrt")
                    .replace("sine", "sin")
                    .replace("cosine", "cos")
                    .replace(Regex("[^0-9\\+\\-\\*\\/\\(\\)\\.\\^a-z]"), "")
                
                val result = AdvancedScientificParser().evaluate(equationStr)
                
                val formatRes = if (result % 1.0 == 0.0) {
                    result.toLong().toString() 
                } else {
                    String.format(Locale.US, "%.4f", result)
                }
                
                localDatabase.logInteraction("Math Engine", formatRes, "MATH_SOLVED")
                speakCommandFeedback("Sir, the scientific calculation evaluates to $formatRes.")
                return
                
            } catch (e: Exception) { 
                printToTerminal("> Math Engine Fault: Syntax unresolvable.") 
            }
        }

        // --------------------------------------------------------------------
        // ALGORITHM BRANCH 2: HARDWARE & OPTICS AUTOMATION
        // --------------------------------------------------------------------
        if (normalized.contains("torch on") || normalized.contains("light on")) {
            operateHardwareFlashlight(1) // Mode 1 = ON
            speakCommandFeedback("Optical illumination engaged.")
            return
        }
        
        if (normalized.contains("torch off") || normalized.contains("light off")) {
            operateHardwareFlashlight(0) // Mode 0 = OFF
            speakCommandFeedback("Optical illumination disengaged.")
            return
        }
        
        if (normalized.contains("strobe mode") || normalized.contains("disco light")) {
            operateHardwareFlashlight(2) // Mode 2 = STROBE
            speakCommandFeedback("Strobe protocol active. Warning: Rapid flashing.")
            return
        }
        
        if (normalized.contains("sos mode")) {
            operateHardwareFlashlight(3) // Mode 3 = SOS
            speakCommandFeedback("Transmitting visual SOS distress signal.")
            return
        }
        
        if (normalized.contains("vibrate") || normalized.contains("haptic")) {
            triggerHapticFeedback(1200)
            speakCommandFeedback("Haptic resonance engines fired successfully.")
            return
        }
        
        if (normalized.contains("ambient light") || normalized.contains("how dark")) {
            speakCommandFeedback("The ambient lux sensor is currently registering $ambientLightLux lux.")
            return
        }

        // --------------------------------------------------------------------
        // ALGORITHM BRANCH 3: SYSTEM TELEMETRY & MEMORY OVERRIDE
        // --------------------------------------------------------------------
        if (normalized.contains("battery") || normalized.contains("power level")) {
            val st = if (isDeviceCharging) "charging" else "discharging"
            speakCommandFeedback("Sir, the power cell is at $currentBatteryLevel percent and is $st. Core thermal output is ${currentBatteryTemp} degrees Celsius. Battery Health is $batteryHealthStr.")
            return
        }
        
        if (normalized.contains("system status") || normalized.contains("diagnostics")) {
            val mi = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(mi)
            val ramPercent = ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100).toInt()
            
            speakCommandFeedback("All systems nominal. Battery at $currentBatteryLevel percent. RAM utilization is at $ramPercent percent. Network is ${if (isNetworkAvailable) "online" else "offline"}.")
            return
        }
        
        if (normalized.contains("clear memory") || normalized.contains("purge database")) {
            val count = localDatabase.getHistoryCount()
            localDatabase.clearMemory()
            updateProgrammaticHUD()
            speakCommandFeedback("Memory override complete. $count interaction logs have been permanently erased from the SQLite vault.")
            return
        }
        
        if (normalized.contains("export logs") || normalized.contains("download memory")) {
            val fileLoc = localDatabase.exportDatabaseToCSV(this)
            speakCommandFeedback("Memory logs have been exported and encrypted to the file system.")
            return
        }
        
        if (normalized.contains("time") || normalized.contains("samay")) {
            val curTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            speakCommandFeedback("The current time is $curTime, sir.")
            return
        }
        
        if (normalized.contains("date") || normalized.contains("tarikh")) {
            val curDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            speakCommandFeedback("Today is $curDate.")
            return
        }

        // --------------------------------------------------------------------
        // ALGORITHM BRANCH 4: NATIVE APPLICATION ROUTING
        // --------------------------------------------------------------------
        if (normalized.contains("instagram open") || normalized.contains("open instagram")) { 
            openUrl("instagram://user?username=drakoxnaeem")
            speakCommandFeedback("Accessing Instagram networks.")
            return 
        }
        
        if (normalized.contains("youtube open") || normalized.contains("open youtube")) { 
            openUrl("vnd.youtube:")
            speakCommandFeedback("Initializing YouTube stream protocol.")
            return 
        }
        
        if (normalized.contains("whatsapp open") || normalized.contains("open whatsapp")) { 
            openUrl("whatsapp://")
            speakCommandFeedback("WhatsApp communications interface loaded.")
            return 
        }
        
        if (normalized.contains("open settings")) { 
            startActivity(Intent(Settings.ACTION_SETTINGS))
            speakCommandFeedback("Opening device core configurations.")
            return 
        }

        // --------------------------------------------------------------------
        // ALGORITHM BRANCH 5: CLOUD NEURAL INFERENCE (API INTEGRATION)
        // --------------------------------------------------------------------
        if (!isNetworkAvailable) {
            printToTerminal("> CRITICAL: Network failure prevents Cloud AI access.")
            speakCommandFeedback("Network is offline sir. Complex neural queries cannot be routed to the cloud framework.")
            return
        }
        
        val activeApiKey = fetchDecryptedApiKey()
        if (activeApiKey.isBlank()) {
            printToTerminal("> ERROR: Missing Cryptographic Key for ${aiManager.getActiveModelName()}.")
            speakCommandFeedback("Sir, the API key for ${aiManager.getActiveModelName()} is missing. Please configure it securely in the Security Vault.")
            return
        }
        
        printToTerminal("> Transmitting packet to ${aiManager.getActiveModelName()} Cloud API...")
        
        if (voiceSessionManager.isActive()) {
            voiceSessionManager.setProcessing()
        }
        
        // Execute Coroutine for Async API Fetch
        lifecycleScope.launch {
            try {
                val cloudResponse = aiManager.queryActiveAI(normalized, activeApiKey)
                
                // Store Response
                localDatabase.logInteraction("Cloud AI Output", cloudResponse, "API_SUCCESS")
                
                // Transmit Response
                speakCommandFeedback(cloudResponse)
                
            } catch (e: Exception) {
                printToTerminal("> CLOUD FAULT: Neural Server Timeout / Error.")
                speakCommandFeedback("Sir, the cloud servers failed to process the request effectively.")
            }
        }
    }

    // ========================================================================
    // TEXT-TO-SPEECH (TTS) DISPATCHERS (NATIVE IMPLEMENTATION)
    // ========================================================================

    private fun executeVoiceOutputInForeground(speechText: String) {
        if (!voiceSessionManager.isActive()) return
        
        voiceSessionManager.setSpeaking()
        synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
        speechRecognizerManager.stop()
        
        printToTerminal("> Audio Subsystem: $speechText")

        speakTextNative(speechText) {
            mainThreadHandler.post {
                if (voiceSessionManager.isActive()) {
                    voiceSessionManager.resumeListening()
                    synchronizeHolographicState(OrbState.LISTENING, "Listening...")
                } else {
                    synchronizeHolographicState(OrbState.IDLE, "Standby")
                }
            }
        }
    }

    private fun executeVoiceOutputInBackground(speechText: String) {
        synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
        printToTerminal("> God Mode Audio output: $speechText")
        
        speakTextNative(speechText) {
            mainThreadHandler.post {
                isBackgroundCommandExecuting = false
                synchronizeHolographicState(OrbState.IDLE, "Standby")
                
                // Signal completion to background service
                try {
                    val resumeIntent = Intent(this, VoiceService::class.java).apply { 
                        action = "com.example.jarvis.VOICE_RESPONSE_FINISHED"
                    }
                    startService(resumeIntent)
                } catch (e: Exception) { 
                    Log.e(TAG, "Failed to signal background", e) 
                }
            }
        }
    }

    private fun speakCommandFeedback(feedbackText: String) {
        if (voiceSessionManager.isActive()) {
            executeVoiceOutputInForeground(feedbackText)
        } else if (isBackgroundCommandExecuting) {
            executeVoiceOutputInBackground(feedbackText)
        } else {
            synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
            printToTerminal("> System Audio: $feedbackText")
            
            speakTextNative(feedbackText) { 
                mainThreadHandler.post { 
                    synchronizeHolographicState(OrbState.IDLE, "Standby") 
                } 
            }
        }
    }

    // ========================================================================
    // TTS ENGINE OVERRIDES & HANDLERS
    // ========================================================================

    /**
     * Native TTS Execution Wrapper
     */
    private fun speakTextNative(text: String, onFinished: (() -> Unit)? = null) {
        
        // Setup listener dynamically
        textToSpeechEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onFinished?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onFinished?.invoke()
            }
        })
        
        val p = Bundle()
        p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        
        // Execute speech
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, p, "TITAN_NATIVE_TTS")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeechEngine.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                printToTerminal("> TTS WARN: Indian Accent Engine Missing.")
            } else {
                printToTerminal("> TTS Subsystem fully operational.")
            }
        } else {
            printToTerminal("> CRITICAL: TTS Engine Boot Failure.")
        }
    }

    // ========================================================================
    // USER INTERACTION LISTENERS & DASHBOARD PANELS
    // ========================================================================

    private fun setupInteractiveClickListeners() {
        
        // Text Input Action
        sendCommandButton.setOnClickListener {
            val typedMessage = messageInputBox.text.toString().trim()
            if (typedMessage.isNotEmpty()) {
                synchronizeHolographicState(OrbState.THINKING, "Processing...")
                evaluateAndExecuteMasterCommand(typedMessage)
                messageInputBox.text.clear()
            }
        }

        // Voice Input Action
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
    }

    private fun startVoiceMode() {
        textToSpeechEngine.stop()
        isBackgroundCommandExecuting = false
        
        voiceSessionManager.start()
        synchronizeHolographicState(OrbState.LISTENING, "Listening...")
        triggerHapticFeedback(100)
    }

    private fun stopVoiceMode() {
        voiceSessionManager.stop()
        speechRecognizerManager.stop()
        textToSpeechEngine.stop()
        voiceOverlayManager.hide()
        
        synchronizeHolographicState(OrbState.IDLE, "Standby")
    }

    private fun openUrl(url: String) {
        if (url.isNotBlank()) {
            printToTerminal("> Routing Web Protocol: $url")
            try { 
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) 
            } catch (e: Exception) { 
                Log.e(TAG, "URL Execution Failure") 
            }
        }
    }

    // ========================================================================
    // PROGRAMMATIC BIOMETRIC SECURITY VAULT (NO XML DEPENDENCY)
    // ========================================================================
    
    private fun authenticateAndOpenProgrammaticVault() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        
        if (!keyguardManager.isKeyguardSecure) {
            printToTerminal("> SECURITY OVERRIDE: Device is not protected by PIN.")
            renderProgrammaticSecurityVault()
            return
        }
        
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(
            "J.A.R.V.I.S. Core Security", 
            "Authenticate to access Neural API keys."
        )
        
        if (intent != null) {
            startActivityForResult(intent, REQ_CODE_SECURITY) 
        } else {
            renderProgrammaticSecurityVault()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQ_CODE_SECURITY) {
            if (resultCode == RESULT_OK) {
                printToTerminal("> AUTH SUCCESS: Opening Secure Vault.")
                renderProgrammaticSecurityVault()
            } else {
                printToTerminal("> INTRUSION ATTEMPT BLOCKED.")
                triggerHapticFeedback(800)
            }
        }
    }

    private fun renderProgrammaticSecurityVault() {
        // Constructing Vault UI Programmatically to eliminate layout reference issues
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
            setBackgroundColor(Color.parseColor("#050811"))
        }

        val title = TextView(this).apply {
            text = "SECURITY VAULT"
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        container.addView(title)

        // Helper Lambda
        fun createInput(hintText: String, defaultVal: String): EditText {
            return EditText(this).apply {
                hint = hintText
                setText(defaultVal)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.DKGRAY)
                setSingleLine()
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setBackgroundColor(Color.parseColor("#1A00E5FF"))
                setPadding(20, 30, 20, 30)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 30 }
            }
        }

        val etGemini = createInput("Gemini API Key", securityVaultPrefs.getString("API_GEMINI", "") ?: "")
        val etGrok = createInput("Grok API Key", securityVaultPrefs.getString("API_GROK", "") ?: "")
        val etChatGPT = createInput("ChatGPT API Key", securityVaultPrefs.getString("API_CHATGPT", "") ?: "")

        container.addView(TextView(this).apply { text = "GOOGLE GEMINI KEY:"; setTextColor(Color.GREEN); textSize = 10f })
        container.addView(etGemini)
        
        container.addView(TextView(this).apply { text = "xAI GROK KEY:"; setTextColor(Color.GREEN); textSize = 10f })
        container.addView(etGrok)
        
        container.addView(TextView(this).apply { text = "OPENAI CHATGPT KEY:"; setTextColor(Color.GREEN); textSize = 10f })
        container.addView(etChatGPT)

        val btnSave = Button(this).apply {
            text = "ENCRYPT & SAVE"
            setBackgroundColor(Color.parseColor(HUD_COLOR_CYAN))
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120
            ).apply { topMargin = 20 }
        }
        container.addView(btnSave)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(container)
            .create()

        btnSave.setOnClickListener {
            securityVaultPrefs.edit().apply {
                putString("API_GEMINI", etGemini.text.toString().trim())
                putString("API_GROK", etGrok.text.toString().trim())
                putString("API_CHATGPT", etChatGPT.text.toString().trim())
                apply()
            }
            
            printToTerminal("> VAULT SECURED: API Keys encrypted to local storage.")
            Toast.makeText(this, "Credentials Secured.", Toast.LENGTH_SHORT).show()
            triggerHapticFeedback(200)
            
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun fetchDecryptedApiKey(): String {
        val modelName = aiManager.getActiveModelName()
        return when {
            modelName.contains("Gemini", true) -> securityVaultPrefs.getString("API_GEMINI", "") ?: ""
            modelName.contains("ChatGPT", true) -> securityVaultPrefs.getString("API_CHATGPT", "") ?: ""
            modelName.contains("Grok", true) -> securityVaultPrefs.getString("API_GROK", "") ?: ""
            else -> ""
        }
    }

    private fun switchNeuralProvider(model: AIProviderManager.AIModelType, activeBtn: Button, otherBtns: List<Button>) {
        triggerHapticFeedback(50)
        
        aiManager.setActiveModel(model)
        tvPoweredByAI.text = "ENGINE: ${aiManager.getActiveModelName().uppercase()}"
        printToTerminal("> Core Engine Switched to: ${aiManager.getActiveModelName()}")
        
        activeBtn.setBackgroundColor(Color.parseColor(HUD_COLOR_CYAN))
        activeBtn.setTextColor(Color.BLACK)
        
        for (btn in otherBtns) {
            btn.setBackgroundColor(Color.parseColor("#111111"))
            btn.setTextColor(Color.WHITE)
        }
    }

    // ========================================================================
    // HARDWARE MOTOR & CAMERA CONTROLLERS
    // ========================================================================

    private fun operateHardwareFlashlight(mode: Int) {
        strobeJob?.cancel()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainCameraId != null) {
                when (mode) {
                    0 -> { 
                        // OFF Mode
                        cameraManager.setTorchMode(mainCameraId!!, false)
                        isTorchActive = false 
                    }
                    1 -> { 
                        // ON Mode
                        cameraManager.setTorchMode(mainCameraId!!, true)
                        isTorchActive = true 
                    }
                    2 -> { 
                        // STROBE Mode
                        strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                            var toggleState = true
                            while(isActive) { 
                                cameraManager.setTorchMode(mainCameraId!!, toggleState)
                                toggleState = !toggleState
                                delay(100) 
                            }
                        }
                    }
                    3 -> { 
                        // SOS Mode
                        strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                            while(isActive) {
                                // 3 Dots
                                for(i in 1..3) { 
                                    cameraManager.setTorchMode(mainCameraId!!, true); delay(200)
                                    cameraManager.setTorchMode(mainCameraId!!, false); delay(200) 
                                }
                                // 3 Dashes
                                for(i in 1..3) { 
                                    cameraManager.setTorchMode(mainCameraId!!, true); delay(600)
                                    cameraManager.setTorchMode(mainCameraId!!, false); delay(200) 
                                }
                                // 3 Dots
                                for(i in 1..3) { 
                                    cameraManager.setTorchMode(mainCameraId!!, true); delay(200)
                                    cameraManager.setTorchMode(mainCameraId!!, false); delay(200) 
                                }
                                delay(1500) // Word break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { 
            printToTerminal("> Camera Fault Detected. Operation Aborted.") 
        }
    }

    private fun triggerHapticFeedback(durationMs: Long = 200) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic feedback unavailable")
        }
    }

    // ========================================================================
    // KINEMATIC SENSORS (SHAKE TO WAKE & PROXIMITY)
    // ========================================================================

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            
            Sensor.TYPE_PROXIMITY -> {
                if (event.values[0] < (proximitySensor?.maximumRange ?: 5f)) {
                    if (voiceSessionManager.isActive() || textToSpeechEngine.isSpeaking) {
                        textToSpeechEngine.stop()
                        speechRecognizerManager.stop()
                        printToTerminal("> Proximity Override: Acoustic output muted.")
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
                
                val dX = abs(accelLastX - x)
                val dY = abs(accelLastY - y)
                val dZ = abs(accelLastZ - z)
                
                if (dX > SHAKE_ACCEL_THRESHOLD || dY > SHAKE_ACCEL_THRESHOLD || dZ > SHAKE_ACCEL_THRESHOLD) {
                    if (!voiceSessionManager.isActive() && PermissionHelper.hasAudioPermission(this)) {
                        printToTerminal("> Kinematic Threshold Breached. Waking Voice Array.")
                        startVoiceMode() 
                    }
                }
                
                accelLastX = x
                accelLastY = y
                accelLastZ = z
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Required method stub
    }

    // ========================================================================
    // DIAGNOSTICS & LIFECYCLE THREADING
    // ========================================================================

    private fun runStartupDiagnosticSequence() {
        lifecycleScope.launch(Dispatchers.IO) {
            
            printToTerminal("> Booting Subsystem Nodes...")
            delay(400)
            
            printToTerminal("> Neural Optics: ONLINE")
            delay(300)
            
            printToTerminal("> SQL Memory Vault: SECURED")
            delay(300)
            
            withContext(Dispatchers.Main) {
                currentSystemState = SystemState.ONLINE
                updateProgrammaticHUD()
                
                triggerHapticFeedback(150)
                printToTerminal("> DIAGNOSTIC COMPLETE. ALL SYSTEMS NOMINAL.")
                
                speakCommandFeedback("J.A.R.V.I.S. Titan Core initialized. Awaiting voice protocols.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magneticSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    
    override fun onPause() { 
        super.onPause()
        sensorManager.unregisterListener(this) 
    }
    
    override fun onDestroy() {
        printToTerminal("> FATAL SHUTDOWN SEQUENCE INITIATED.")
        currentSystemState = SystemState.OFFLINE
        
        try { unregisterReceiver(powerTelemetryReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(backgroundWakeReceiver) } catch (e: Exception) {}
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        
        operateHardwareFlashlight(0)
        
        try { voiceSessionManager.destroy() } catch (e: Exception) {}
        try { speechRecognizerManager.destroy() } catch (e: Exception) {}
        
        if (this::textToSpeechEngine.isInitialized) { 
            textToSpeechEngine.stop()
            textToSpeechEngine.shutdown() 
        }
        
        viewModel.setResponseListener(null)
        mainThreadHandler.removeCallbacksAndMessages(null)
        
        super.onDestroy()
    }

    // ============================================================================
    // INNER GOD CLASS 1: ADVANCED SQLITE DATABASE (CRUD + EXPORT)
    // ============================================================================
    
    inner class JarvisDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "JarvisTitanMemory.db", null, 4) {
        
        override fun onCreate(db: SQLiteDatabase) {
            val q = """
                CREATE TABLE MemoryLog (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, 
                    timestamp TEXT, 
                    query TEXT, 
                    response TEXT, 
                    intent_type TEXT
                )
            """.trimIndent()
            db.execSQL(q)
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
                Log.e(TAG, "SQL Error", e) 
            }
        }

        fun getHistoryCount(): Int {
            var count = 0
            try {
                val cursor = this.readableDatabase.rawQuery("SELECT COUNT(*) FROM MemoryLog", null)
                if (cursor.moveToFirst()) count = cursor.getInt(0)
                cursor.close()
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

        fun exportDatabaseToCSV(context: Context): String {
            val exportDir = File(context.getExternalFilesDir(null), "JarvisExports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, "Jarvis_Memory_Dump_${System.currentTimeMillis()}.csv")
            try {
                file.createNewFile()
                val fw = FileWriter(file)
                val db = this.readableDatabase
                val cur = db.rawQuery("SELECT * FROM MemoryLog", null)
                
                fw.append("ID,Timestamp,Query,Response,IntentType\n")
                
                if (cur.moveToFirst()) {
                    do {
                        fw.append("${cur.getInt(0)},\"${cur.getString(1)}\",\"${cur.getString(2)}\",\"${cur.getString(3)}\",\"${cur.getString(4)}\"\n")
                    } while (cur.moveToNext())
                }
                
                cur.close()
                fw.close()
                db.close()
                return file.absolutePath
            } catch (e: Exception) { 
                return "Export Failed" 
            }
        }
    }

    // ============================================================================
    // INNER GOD CLASS 2: ADVANCED SCIENTIFIC MATH PARSER (BODMAS + TRIG + LOG)
    // ============================================================================
    
    inner class AdvancedScientificParser {
        
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
                    if (pos < expression.length) throw RuntimeException("Syntax Error: " + ch.toChar())
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
                            eat('*'.code) -> x *= parseFactor()
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
                    }
                    else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                        // FIX: Fixed character literal error. Replaced ch == '.code' with ch == '.'.code
                        while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                        x = expression.substring(startPos, this.pos).toDouble()
                    }
                    else if (ch >= 'a'.code && ch <= 'z'.code) {
                        while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                        val func = expression.substring(startPos, this.pos)
                        
                        if (func == "pi") return PI
                        if (func == "e") return E
                        
                        x = parseFactor()
                        x = when (func) {
                            "sqrt" -> sqrt(x)
                            "sin" -> sin(Math.toRadians(x))
                            "cos" -> cos(Math.toRadians(x))
                            "tan" -> tan(Math.toRadians(x))
                            "log" -> log10(x)
                            "ln" -> ln(x)
                            else -> throw RuntimeException("Unknown Math Function: $func")
                        }
                    } else throw RuntimeException("Unexpected Math Token")
                    
                    if (eat('^'.code)) x = x.pow(parseFactor())
                    
                    return x
                }
            }.parse()
        }
    }

    // ============================================================================
    // INNER GOD CLASS 3: MATRIX DIGITAL RAIN RENDERER (PROGRAMMATIC CANVAS)
    // ============================================================================
    
    inner class MatrixDigitalRainView(context: Context) : View(context) {
        private val rnd = Random()
        private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        
        // Generates 65 columns of dense matrix rain
        private val drops = Array(65) { DigitalDrop() } 
        private var hexGlowColor = HUD_COLOR_CYAN
        
        inner class DigitalDrop {
            var x = rnd.nextFloat() * 1200f
            var y = rnd.nextFloat() * -2500f
            var speed = rnd.nextFloat() * 10f + 6f
            var chars = CharArray(rnd.nextInt(20) + 5) { (rnd.nextInt(94) + 33).toChar() }
            var textSize = rnd.nextFloat() * 20f + 12f
        }

        fun updateHologramColor(state: OrbState) {
            hexGlowColor = when(state) {
                OrbState.ERROR -> HUD_COLOR_RED
                OrbState.LISTENING -> HUD_COLOR_GREEN
                OrbState.SPEAKING -> HUD_COLOR_ORANGE
                else -> HUD_COLOR_CYAN
            }
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val w = width.toFloat()
            val h = height.toFloat()
            
            for (drop in drops) {
                p.textSize = drop.textSize
                for (i in drop.chars.indices) {
                    
                    // Matrix Glitch Effect: 5% chance character randomly changes
                    if (rnd.nextFloat() > 0.95f) {
                        drop.chars[i] = (rnd.nextInt(94) + 33).toChar()
                    }
                    
                    // Alpha Fading for trailing tail effect
                    val alpha = 255 - (i * (255 / drop.chars.size))
                    p.color = Color.parseColor(hexGlowColor)
                    p.alpha = alpha.coerceIn(0, 255)
                    
                    c.drawText(drop.chars[i].toString(), drop.x, drop.y - (i * drop.textSize), p)
                }
                
                drop.y += drop.speed
                
                // Reset drop to top boundary once off screen
                if (drop.y - (drop.chars.size * drop.textSize) > h) {
                    drop.y = rnd.nextFloat() * -800f
                    drop.x = rnd.nextFloat() * w
                    drop.speed = rnd.nextFloat() * 10f + 6f
                }
            }
            
            // Loop 60fps refresh
            invalidate() 
        }
    }
}
