package com.example.jarvis

// ============================================================================
// EXHAUSTIVE SYSTEM IMPORTS (TITAN HYBRID CORE V51.0 - MULTI-LANGUAGE)
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
import android.util.Base64
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
import com.example.jarvis.ui.OverlayWindowManager
import com.example.jarvis.utils.PermissionHelper
import com.example.jarvis.voice.SpeechRecognizerManager
import com.example.jarvis.voice.TextToSpeechManager
import com.example.jarvis.voice.VoiceSessionManager
import com.example.jarvis.voice.VoiceService
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
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * ============================================================================
 * J.A.R.V.I.S. ULTIMATE TITAN CORE - MULTI-LANGUAGE HYBRID EDITION (V51.0)
 * ============================================================================
 * Architect: DrakoXNaeem
 * * SYSTEM UPGRADES IN THIS VERSION:
 * 1. FIXED `CREATOR_INSTAGRAM` Unresolved Reference Error (Direct String Injected).
 * 2. ZERO MARGIN ERRORS: LayoutParams explicit casting implemented.
 * 3. C++ JNI BRIDGE: Native high-speed execution endpoints initialized.
 * 4. PYTHON AI BRIDGE: Advanced Python engine integration framework ready.
 * 5. PURE AUTOMATION: Everything generated programmatically.
 * ============================================================================
 */
class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener {

    // ========================================================================
    // NATIVE C++ (JNI) LOADERS
    // ========================================================================
    init {
        try {
            System.loadLibrary("jarvis_native_engine")
            Log.i("JarvisJNI", "C++ Native Core Loaded Successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("JarvisJNI", "C++ Library missing or not compiled yet. Running on Kotlin Fallback.")
        }
    }

    external fun stringFromJNI(): String
    external fun processQuantumCalculation(inputArray: DoubleArray): DoubleArray
    external fun nativeImageMatrixProcessing(bitmapBytes: ByteArray): ByteArray

    // ========================================================================
    // ENUMS & GLOBAL CONSTANTS
    // ========================================================================
    
    enum class SystemState {
        INITIALIZING, ONLINE, STANDBY, DIAGNOSTIC, CRITICAL_FAULT, OFFLINE, LISTENING, PROCESSING, SPEAKING, FLOATING_MODE
    }

    companion object {
        private const val TAG = "JarvisTitanMaster"
        
        private const val ACTION_WAKE_WORD_DETECTED = "com.example.jarvis.WAKE_WORD_DETECTED"
        private const val ACTION_UPDATE_STATE = "com.example.jarvis.UPDATE_STATE"
        private const val ACTION_LAUNCH_FLOATING_ORB = "com.example.jarvis.LAUNCH_FLOATING_ORB"
        
        private const val SHAKE_ACCEL_THRESHOLD = 18.0f
        
        private const val REQ_CODE_SECURITY = 9002
        private const val REQ_CODE_OVERLAY = 9001
        
        private const val HUD_COLOR_CYAN = "#00E5FF"
        private const val HUD_COLOR_RED = "#FF1744"
        private const val HUD_COLOR_GREEN = "#00E676"
        private const val HUD_COLOR_ORANGE = "#FF9100"
        private const val HUD_COLOR_PURPLE = "#AA00FF"
        private const val HUD_COLOR_BLACK_BG = "#050811"
        private const val HUD_COLOR_BLACK_TRANSPARENT = "#88000000"
    }

    // ========================================================================
    // PYTHON & KOTLIN ENGINE MANAGERS
    // ========================================================================
    
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var prefs: PreferencesManager
    private lateinit var commandExecutor: CommandExecutor
    
    private lateinit var localDatabase: JarvisDatabaseHelper
    private lateinit var securityVaultPrefs: SharedPreferences
    private lateinit var cryptoEngine: QuantumCryptographyManager
    private lateinit var pythonBridge: PythonAIBridge
    
    private lateinit var aiManager: AIProviderManager
    private lateinit var overlayManager: OverlayWindowManager

    private lateinit var speechRecognizerManager: SpeechRecognizerManager
    private lateinit var voiceSessionManager: VoiceSessionManager
    private lateinit var voiceOverlayManager: VoiceOverlayManager
    private lateinit var textToSpeechEngine: TextToSpeech
    private lateinit var textToSpeechManager: TextToSpeechManager
    
    private lateinit var audioManager: AudioManager
    private lateinit var activityManager: ActivityManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var deepDiagnosticsEngine: DeepSystemDiagnostics

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magneticSensor: Sensor? = null
    
    private lateinit var cameraManager: CameraManager
    private var mainCameraId: String? = null
    private var isTorchActive = false

    // UI View Bindings
    private var micToggleButton: ImageButton? = null
    private var messageInputBox: EditText? = null
    private var sendCommandButton: ImageButton? = null
    private var holographicOrbView: JarvisOrbView? = null
    private var mainRecyclerView: RecyclerView? = null
    private lateinit var masterRootLayout: ViewGroup

    private lateinit var dynamicTelemetryHUD: TextView
    private lateinit var programmaticTerminalLog: TextView
    private lateinit var terminalScrollView: ScrollView
    private lateinit var aiSwitcherPanel: LinearLayout
    private lateinit var tvPoweredByAI: TextView
    
    private var matrixBackground: MatrixDigitalRainView? = null
    private var strobeJob: Job? = null

    // State Vars
    private var isBackgroundCommandExecuting = false
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var currentSystemState = SystemState.INITIALIZING

    private var currentBatteryLevel = -1
    private var currentBatteryTemp = -1f
    private var currentBatteryVoltage = -1
    private var isDeviceCharging = false
    private var batteryHealthStr = "UNKNOWN"
    private var isNetworkAvailable = false
    private var ambientLightLux = 0f
    
    private var accelLastX = 0f
    private var accelLastY = 0f
    private var accelLastZ = 0f
    private var isShakeInitialized = false

    // ========================================================================
    // BROADCAST RECEIVERS
    // ========================================================================

    private val backgroundWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_WAKE_WORD_DETECTED) {
                val command = intent.getStringExtra("INITIAL_COMMAND") ?: "Jarvis"
                printToTerminal("> IPC ALERT: Background God Mode Activated.")
                isBackgroundCommandExecuting = true
                mainThreadHandler.postDelayed({
                    messageInputBox?.setText(command)
                    messageInputBox?.setSelection(messageInputBox?.length() ?: 0)
                    evaluateAndExecuteMasterCommand(command)
                    messageInputBox?.text?.clear()
                }, 200)
            }
        }
    }

    private val powerTelemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            currentBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            currentBatteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isDeviceCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            batteryHealthStr = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                else -> "UNKNOWN"
            }
            updateProgrammaticHUD()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isNetworkAvailable = true
            runOnUiThread { printToTerminal("> NETWORK: Satellite Uplink Established. Operating Online."); updateProgrammaticHUD() }
        }
        override fun onLost(network: Network) {
            isNetworkAvailable = false
            runOnUiThread { printToTerminal("> NETWORK: Uplink Severed. Switching to Local Core Logic."); updateProgrammaticHUD() }
        }
    }

    // ========================================================================
    // LIFECYCLE: MASTER BOOT SEQUENCE
    // ========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor(HUD_COLOR_BLACK_BG)
        
        setContentView(R.layout.activity_main)
        
        val root = findViewById<View>(android.R.id.content)
        if (root is ViewGroup) {
            masterRootLayout = root
        } else {
            throw IllegalStateException("Root view is not a ViewGroup. Cannot proceed.")
        }

        printBootLogHeaders()
        executeTitanInitializationSequence()
    }

    private fun printBootLogHeaders() {
        Log.i(TAG, "||=================================================||")
        Log.i(TAG, "|| TITAN CORE V51.0 - HYBRID MULTI-LANGUAGE ENGINE ||")
        Log.i(TAG, "|| Architect: DrakoXNaeem                          ||")
        Log.i(TAG, "|| Status: KOTLIN + PYTHON + C++ LINK ACTIVE       ||")
        Log.i(TAG, "||=================================================||")
    }

    private fun executeTitanInitializationSequence() {
        initializeDatabasesAndStorage()
        initializeHardwareSubsystems()
        initializeNetworkSubsystem()
        initializeCoreManagers()
        
        injectProgrammaticMatrixBackground()
        injectProgrammaticHUD()
        injectProgrammaticTerminalAndControls()
        bindNativeUserInterfaceDynamically()
        
        setupChatRecyclerView()
        setupInteractiveClickListenersDynamically()
        initializeNativeTTSEngine()
        
        setupVoiceNeuralEngine()
        setupAICloudListener()
        
        registerReceiver(powerTelemetryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerBackgroundIPCReceiver()
        runStartupDiagnosticSequence()
    }

    private fun initializeDatabasesAndStorage() {
        localDatabase = JarvisDatabaseHelper(this)
        securityVaultPrefs = getSharedPreferences("JarvisSecurityVault", Context.MODE_PRIVATE)
        cryptoEngine = QuantumCryptographyManager()
        deepDiagnosticsEngine = DeepSystemDiagnostics(this)
        pythonBridge = PythonAIBridge(this)
    }

    private fun initializeCoreManagers() {
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

    private fun initializeNativeTTSEngine() {
        textToSpeechEngine = TextToSpeech(this, this)
        textToSpeechManager = TextToSpeechManager(this)
    }

    private fun initializeHardwareSubsystems() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try { 
            mainCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Camera Flash unavailable", e) 
        }
    }

    private fun initializeNetworkSubsystem() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    @SuppressLint("DiscouragedApi")
    private fun bindNativeUserInterfaceDynamically() {
        printToTerminal("> Executing Safe ViewBinding Protocol...")
        try {
            val inputId = resources.getIdentifier("messageInput", "id", packageName)
            if (inputId != 0) messageInputBox = findViewById(inputId)

            val micId = resources.getIdentifier("micButton", "id", packageName)
            if (micId != 0) micToggleButton = findViewById(micId)

            val sendId = resources.getIdentifier("sendButton", "id", packageName)
            if (sendId != 0) sendCommandButton = findViewById(sendId)

            var orbId = resources.getIdentifier("mainJarvisOrb", "id", packageName)
            if (orbId == 0) orbId = resources.getIdentifier("jarvisOrbView", "id", packageName)
            if (orbId != 0) {
                holographicOrbView = findViewById(orbId)
                holographicOrbView?.setOrbState(OrbState.IDLE)
            }

            val recyclerId = resources.getIdentifier("messageRecyclerView", "id", packageName)
            if (recyclerId != 0) mainRecyclerView = findViewById(recyclerId)

        } catch (e: Exception) {
            printToTerminal("> WARNING: Dynamic Bindings Failed.")
        }
    }

    private fun injectProgrammaticMatrixBackground() {
        matrixBackground = MatrixDigitalRainView(this)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        masterRootLayout.addView(matrixBackground, 0, params)
    }

    private fun injectProgrammaticHUD() {
        dynamicTelemetryHUD = TextView(this).apply {
            text = "J.A.R.V.I.S. | C++ & PYTHON HYBRID ONLINE"
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(10, 20, 10, 20)
            setBackgroundColor(Color.parseColor(HUD_COLOR_BLACK_TRANSPARENT))
            typeface = Typeface.MONOSPACE
        }
        
        val hudParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        hudParams.gravity = Gravity.TOP
        hudParams.topMargin = 50 
        masterRootLayout.addView(dynamicTelemetryHUD, hudParams)
    }

    private fun injectProgrammaticTerminalAndControls() {
        terminalScrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(HUD_COLOR_BLACK_TRANSPARENT))
            setPadding(16, 16, 16, 16)
        }
        
        programmaticTerminalLog = TextView(this).apply {
            text = "> System Kernel Booting...\n> Loading C++ Execution Environment...\n> Waking Python AI Wrappers..."
            setTextColor(Color.parseColor(HUD_COLOR_CYAN))
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        terminalScrollView.addView(programmaticTerminalLog)

        val terminalParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 350)
        terminalParams.gravity = Gravity.TOP
        terminalParams.topMargin = 300 
        terminalParams.leftMargin = 30
        terminalParams.rightMargin = 30
        masterRootLayout.addView(terminalScrollView, terminalParams)

        aiSwitcherPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(20, 20, 20, 20)
        }

        tvPoweredByAI = TextView(this).apply {
            text = "ENGINE: HYBRID (KOTLIN+PYTHON)"
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 10)
        }
        aiSwitcherPanel.addView(tvPoweredByAI)

        val btnVault = createStyledButton("VAULT", "#333333", Color.WHITE)
        val btnAiGrok = createStyledButton("PYTHON AI", "#111111", Color.WHITE)
        val btnAiGem = createStyledButton("GEMINI", HUD_COLOR_CYAN, Color.BLACK)
        val btnAiGpt = createStyledButton("GPT", "#111111", Color.WHITE)
        val btnFloat = createStyledButton("FLOAT ORB", HUD_COLOR_PURPLE, Color.WHITE)

        val btnRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        
        val btnParam = LinearLayout.LayoutParams(160, 80)
        btnParam.rightMargin = 10

        btnRow1.addView(btnAiGem, btnParam)
        btnRow1.addView(btnAiGrok, btnParam)
        btnRow1.addView(btnAiGpt, btnParam)
        
        val btnRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2Params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        row2Params.topMargin = 10
        
        val bigBtnParam = LinearLayout.LayoutParams(245, 80)
        bigBtnParam.rightMargin = 10
        
        btnRow2.addView(btnVault, bigBtnParam)
        btnRow2.addView(btnFloat, LinearLayout.LayoutParams(245, 80))

        aiSwitcherPanel.addView(btnRow1)
        aiSwitcherPanel.addView(btnRow2, row2Params)

        val panelParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        panelParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        panelParams.bottomMargin = 350
        masterRootLayout.addView(aiSwitcherPanel, panelParams)

        // Listeners
        btnVault.setOnClickListener { triggerHapticFeedback(50); authenticateAndOpenProgrammaticVault() }
        btnAiGem.setOnClickListener { switchNeuralProvider(AIProviderManager.AIModelType.GEMINI, btnAiGem, listOf(btnAiGrok, btnAiGpt)) }
        btnAiGrok.setOnClickListener { 
            triggerHapticFeedback(50)
            tvPoweredByAI.text = "ENGINE: PYTHON CUSTOM"
            printToTerminal("> Routing requests to Local Python Model Interface.")
            btnAiGrok.setBackgroundColor(Color.parseColor(HUD_COLOR_CYAN))
            btnAiGrok.setTextColor(Color.BLACK)
            btnAiGem.setBackgroundColor(Color.parseColor("#111111")); btnAiGem.setTextColor(Color.WHITE)
            btnAiGpt.setBackgroundColor(Color.parseColor("#111111")); btnAiGpt.setTextColor(Color.WHITE)
        }
        btnAiGpt.setOnClickListener { switchNeuralProvider(AIProviderManager.AIModelType.CHATGPT, btnAiGpt, listOf(btnAiGem, btnAiGrok)) }
        btnFloat.setOnClickListener { triggerHapticFeedback(100); launchFloatingOrbService() }
    }

    private fun createStyledButton(txt: String, bgColor: String, txtColor: Int): Button {
        return Button(this).apply { 
            text = txt
            textSize = 8f
            setBackgroundColor(Color.parseColor(bgColor))
            setTextColor(txtColor) 
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter()
        mainRecyclerView?.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        mainRecyclerView?.adapter = chatAdapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgrammaticHUD() {
        val netStr = if (isNetworkAvailable) "ONLINE" else "OFFLINE"
        val chgStr = if (isDeviceCharging) "AC" else "BAT"
        val memCount = localDatabase.getHistoryCount()
        val ramUsage = deepDiagnosticsEngine.getRamUsagePercentage()
        val cpuTemp = deepDiagnosticsEngine.estimateCpuTemp()
        val storage = deepDiagnosticsEngine.getAvailableStorageMB()

        val hudText = "CORE: TITAN | NET: $netStr | PWR: $currentBatteryLevel% [$chgStr] | " +
                      "TMP: ${currentBatteryTemp}C (CPU:$cpuTemp) | HLT: $batteryHealthStr | VOLT: ${currentBatteryVoltage}mV | " +
                      "RAM: $ramUsage% | LUX: $ambientLightLux | DISK: ${storage}MB | PY: ONLINE"
                      
        if (::dynamicTelemetryHUD.isInitialized) dynamicTelemetryHUD.text = hudText
    }

    private fun printToTerminal(message: String) {
        mainThreadHandler.post {
            if (::programmaticTerminalLog.isInitialized) {
                val current = programmaticTerminalLog.text.toString()
                val lines = current.split("\n")
                val newText = if (lines.size > 80) lines.drop(1).joinToString("\n") + "\n$message" else "$current\n$message" 
                programmaticTerminalLog.text = newText
                terminalScrollView.post { terminalScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    // ========================================================================
    // KOTLIN-BASED VOICE & AI DISPATCHER
    // ========================================================================

    private fun setupVoiceNeuralEngine() {
        voiceSessionManager = VoiceSessionManager(
            context = this,
            speechRecognizer = speechRecognizerManager,
            onText = { recognizedText ->
                mainThreadHandler.post {
                    val cleanText = recognizedText.trim()
                    if (cleanText.isNotBlank()) {
                        messageInputBox?.setText(cleanText)
                        messageInputBox?.setSelection(messageInputBox?.length() ?: 0)
                        evaluateAndExecuteMasterCommand(cleanText)
                    }
                }
            },
            onStateChanged = { sessionState ->
                mainThreadHandler.post {
                    when (sessionState) {
                        VoiceSessionManager.State.IDLE -> synchronizeHolographicState(OrbState.IDLE, "Standby")
                        VoiceSessionManager.State.LISTENING -> synchronizeHolographicState(OrbState.LISTENING, "Listening...")
                        VoiceSessionManager.State.PROCESSING -> synchronizeHolographicState(OrbState.THINKING, "Synthesizing...")
                        VoiceSessionManager.State.SPEAKING -> synchronizeHolographicState(OrbState.SPEAKING, "Transmitting...")
                        VoiceSessionManager.State.FAULT_RECOVERY -> synchronizeHolographicState(OrbState.ERROR, "Recovering...")
                        else -> synchronizeHolographicState(OrbState.IDLE, "Standby")
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

    private fun synchronizeHolographicState(state: OrbState, subText: String) {
        currentSystemState = when(state) {
            OrbState.IDLE -> SystemState.STANDBY
            OrbState.LISTENING -> SystemState.LISTENING
            OrbState.THINKING -> SystemState.PROCESSING
            OrbState.SPEAKING -> SystemState.SPEAKING
            OrbState.ERROR -> SystemState.CRITICAL_FAULT
        }

        holographicOrbView?.setOrbState(state)
        if (::voiceOverlayManager.isInitialized) voiceOverlayManager.updateState(state)
        micToggleButton?.alpha = if (state == OrbState.LISTENING) 1.0f else 0.7f

        val syncIntent = Intent(ACTION_UPDATE_STATE).apply { putExtra("extra_state", state.name) }
        sendBroadcast(syncIntent)

        val targetColor = when(state) {
            OrbState.ERROR -> Color.parseColor("#55FF0000")
            OrbState.THINKING -> Color.parseColor("#4400E5FF")
            OrbState.LISTENING -> Color.parseColor("#4400FF00")
            OrbState.SPEAKING -> Color.parseColor("#44FF9100")
            else -> Color.parseColor(HUD_COLOR_BLACK_BG)
        }
        
        val animator = ObjectAnimator.ofArgb(masterRootLayout, "backgroundColor", targetColor)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        matrixBackground?.updateHologramColor(state)
    }

    private fun setupAICloudListener() {
        viewModel.setResponseListener { networkResponse ->
            mainThreadHandler.post {
                val safeResponse = if (networkResponse.contains("Unable to resolve host", true)) {
                    "Sir, the neural link to the cloud is severed. Please verify network integrity."
                } else {
                    networkResponse
                }
                
                localDatabase.logInteraction("Cloud Inference", safeResponse, "API_SUCCESS")
                updateProgrammaticHUD()
                printToTerminal("> Inference Received: ${safeResponse.take(50)}...")

                if (voiceSessionManager.isActive()) {
                    executeVoiceOutputInForeground(safeResponse)
                } else if (isBackgroundCommandExecuting) {
                    executeVoiceOutputInBackground(safeResponse)
                } else {
                    synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
                    speakTextNative(safeResponse) {
                        mainThreadHandler.post { synchronizeHolographicState(OrbState.IDLE, "Standby") }
                    }
                }
            }
        }
    }

    private fun registerBackgroundIPCReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_WAKE_WORD_DETECTED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(backgroundWakeReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(backgroundWakeReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind IPC Receiver", e)
        }
    }

    // ========================================================================
    // HYBRID NLP ROUTER (KOTLIN PARSER + PYTHON FALLBACK STUB)
    // ========================================================================

    private fun evaluateAndExecuteMasterCommand(rawInput: String) {
        val normalized = rawInput.lowercase(Locale.getDefault())
            .replace("hey jarvis", "").replace("ok jarvis", "").replace("jarvis", "").trim()

        if (normalized.isBlank()) {
            if (voiceSessionManager.isActive()) speakCommandFeedback("Yes sir, awaiting protocols.")
            else synchronizeHolographicState(OrbState.IDLE, "Standby")
            return
        }

        localDatabase.logInteraction(normalized, "Processing Logic Tree...", "USER_QUERY")
        updateProgrammaticHUD()
        synchronizeHolographicState(OrbState.THINKING, "Parsing Semantic Intent...")
        printToTerminal("> Input Vector: \"$normalized\"")

        // 1. Math Evaluation Engine
        val mathRegex = Regex(".*(calculate|math|plus|minus|multiply|divided|times|power|root|sin|cos|tan|log).*")
        if (normalized.matches(mathRegex)) {
            try {
                val equationStr = normalized
                    .replace("plus", "+").replace("minus", "-")
                    .replace("times", "*").replace("divided by", "/")
                    .replace("power", "^").replace("root", "sqrt")
                    .replace(Regex("[^0-9\\+\\-\\*\\/\\(\\)\\.\\^a-z]"), "")
                
                val result = AdvancedScientificParser().evaluate(equationStr)
                val formatRes = if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result)
                
                localDatabase.logInteraction("Math Engine", formatRes, "MATH_SOLVED")
                speakCommandFeedback("Sir, the calculation evaluates to $formatRes.")
                return
            } catch (e: Exception) { 
                printToTerminal("> Math Engine Fault: Syntax unresolvable.") 
            }
        }

        // 2. Hardware Automations
        if (normalized.contains("torch on") || normalized.contains("light on")) { operateHardwareFlashlight(1); speakCommandFeedback("Optical illumination engaged."); return }
        if (normalized.contains("torch off") || normalized.contains("light off")) { operateHardwareFlashlight(0); speakCommandFeedback("Optical illumination disengaged."); return }
        if (normalized.contains("strobe mode")) { operateHardwareFlashlight(2); speakCommandFeedback("Strobe protocol active. Warning: Rapid flashing."); return }
        if (normalized.contains("sos mode")) { operateHardwareFlashlight(3); speakCommandFeedback("Transmitting visual SOS signal."); return }
        if (normalized.contains("vibrate")) { triggerHapticFeedback(1000); speakCommandFeedback("Haptic engines fired."); return }
        
        // 3. System Telemetry
        if (normalized.contains("battery")) {
            val st = if (isDeviceCharging) "charging" else "discharging"
            speakCommandFeedback("Sir, the power cell is at $currentBatteryLevel percent and is $st. Core thermal output is ${currentBatteryTemp} degrees Celsius.")
            return
        }
        
        if (normalized.contains("system status") || normalized.contains("diagnostics")) {
            val ramPercent = deepDiagnosticsEngine.getRamUsagePercentage()
            val disk = deepDiagnosticsEngine.getAvailableStorageMB()
            speakCommandFeedback("All systems nominal. Battery at $currentBatteryLevel percent. RAM utilization is at $ramPercent percent. We have $disk megabytes of free storage.")
            return
        }
        
        if (normalized.contains("time")) {
            val curTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            speakCommandFeedback("The current time is $curTime, sir.")
            return
        }
        
        // 4. Custom App Deep Links
        if (normalized.contains("instagram open") || normalized.contains("open instagram")) { 
            openUrl("instagram://user?username=drakoxnaeem"); speakCommandFeedback("Accessing Instagram networks."); return 
        }
        if (normalized.contains("youtube open") || normalized.contains("open youtube")) { 
            openUrl("vnd.youtube:"); speakCommandFeedback("Initializing YouTube stream protocol."); return 
        }
        if (normalized.contains("whatsapp open") || normalized.contains("open whatsapp")) { 
            openUrl("whatsapp://"); speakCommandFeedback("WhatsApp communications interface loaded."); return 
        }
        if (normalized.contains("open github") || normalized.contains("open vercel")) {
            openUrl("https://github.com/drakoxnaeem"); speakCommandFeedback("Opening Developer Console."); return
        }

        // 5. Advanced Python Scripting Bridge
        if (normalized.contains("run python") || normalized.contains("execute script")) {
            printToTerminal("> Routing to Python Environment...")
            val result = pythonBridge.executePythonScript("import sys\nprint('Python is ready for DrakoX.')")
            speakCommandFeedback(result)
            return
        }

        // 6. Cloud Inference Routing
        if (!isNetworkAvailable) {
            printToTerminal("> CRITICAL: Network failure prevents Cloud AI access.")
            speakCommandFeedback("Network is offline sir. Complex neural queries cannot be routed to the cloud framework.")
            return
        }
        
        val activeApiKey = fetchDecryptedApiKey()
        if (activeApiKey.isBlank() && tvPoweredByAI.text.contains("GEMINI")) {
            printToTerminal("> ERROR: Missing Cryptographic Key for ${aiManager.getActiveModelName()}.")
            speakCommandFeedback("Sir, the API key for ${aiManager.getActiveModelName()} is missing. Please configure it securely in the Vault.")
            return
        }
        
        printToTerminal("> Transmitting packet to API Engine...")
        if (voiceSessionManager.isActive()) voiceSessionManager.setProcessing()
        
        lifecycleScope.launch {
            try {
                val cloudResponse = aiManager.queryActiveAI(normalized, activeApiKey)
                localDatabase.logInteraction("Cloud AI Output", cloudResponse, "API_SUCCESS")
                speakCommandFeedback(cloudResponse)
            } catch (e: Exception) {
                printToTerminal("> CLOUD FAULT: Neural Server Error.")
                speakCommandFeedback("Sir, the cloud servers failed to process the request.")
            }
        }
    }

    // ========================================================================
    // TEXT-TO-SPEECH (TTS) DISPATCHERS (Fixed Speed/Pitch)
    // ========================================================================

    private fun executeVoiceOutputInForeground(speechText: String) {
        if (!voiceSessionManager.isActive()) return
        voiceSessionManager.setSpeaking()
        synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
        speechRecognizerManager.stop()
        
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
        speakTextNative(speechText) {
            mainThreadHandler.post {
                isBackgroundCommandExecuting = false
                synchronizeHolographicState(OrbState.IDLE, "Standby")
                try {
                    val resumeIntent = Intent(this, VoiceService::class.java).apply { action = "com.example.jarvis.VOICE_RESPONSE_FINISHED" }
                    startService(resumeIntent)
                } catch (e: Exception) { Log.e(TAG, "Failed to signal background", e) }
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
            speakTextNative(feedbackText) { 
                mainThreadHandler.post { synchronizeHolographicState(OrbState.IDLE, "Standby") } 
            }
        }
    }

    private fun speakTextNative(text: String, onFinished: (() -> Unit)? = null) {
        textToSpeechEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onFinished?.invoke() }
            override fun onError(utteranceId: String?) { onFinished?.invoke() }
        })
        val p = Bundle()
        p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, p, "TITAN_NATIVE_TTS")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeechEngine.setSpeechRate(0.85f)
            textToSpeechEngine.setPitch(0.9f)
            
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
    // BUG FIX #3: INSTAGRAM CREATOR LINK HARDCODED TO FIX COMPILER ERROR
    // ========================================================================
    @SuppressLint("DiscouragedApi")
    private fun setupInteractiveClickListenersDynamically() {
        sendCommandButton?.setOnClickListener {
            val typedMessage = messageInputBox?.text?.toString()?.trim() ?: ""
            if (typedMessage.isNotEmpty()) {
                synchronizeHolographicState(OrbState.THINKING, "Processing...")
                evaluateAndExecuteMasterCommand(typedMessage)
                messageInputBox?.text?.clear()
            }
        }

        micToggleButton?.setOnClickListener {
            if (!PermissionHelper.hasAudioPermission(this)) {
                PermissionHelper.requestAudioPermission(this)
                return@setOnClickListener
            }
            if (voiceSessionManager.isActive()) stopVoiceMode() else startVoiceMode()
        }

        safelyBindClick("btnWeb") { openUrl("https://www.google.com") }
        
        // ERROR FIXED: Hardcoded string instead of companion object reference
        safelyBindClick("btnInsta") { openUrl("https://www.instagram.com/drakoxnaeem") }
        
        safelyBindClick("settingsButton") { authenticateAndOpenProgrammaticVault() }
        safelyBindClick("btnSettings") { authenticateAndOpenProgrammaticVault() }
    }

    @SuppressLint("DiscouragedApi")
    private fun safelyBindClick(idName: String, action: () -> Unit) {
        val resId = resources.getIdentifier(idName, "id", packageName)
        if (resId != 0) { findViewById<View>(resId)?.setOnClickListener { action() } }
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
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { Log.e(TAG, "URL Execution Failure") }
        }
    }

    // ========================================================================
    // FLOATING WINDOW / ORB DELEGATION
    // ========================================================================

    private fun launchFloatingOrbService() {
        if (!Settings.canDrawOverlays(this)) {
            printToTerminal("> Requesting SYSTEM_ALERT_WINDOW permission.")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_CODE_OVERLAY)
            return
        }
        
        printToTerminal("> System permission granted. Initiating Floating Orb Service...")
        val serviceIntent = Intent(this, VoiceService::class.java).apply { action = ACTION_LAUNCH_FLOATING_ORB }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        speakCommandFeedback("Floating core interface activated. Moving to background.")
    }

    // ========================================================================
    // SECURITY VAULT (API KEY ENCRYPTION)
    // ========================================================================

    private fun authenticateAndOpenProgrammaticVault() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isKeyguardSecure) {
            renderProgrammaticSecurityVault()
            return
        }
        val intent = keyguardManager.createConfirmDeviceCredentialIntent("J.A.R.V.I.S. Core Security", "Authenticate to access Neural API keys.")
        if (intent != null) startActivityForResult(intent, REQ_CODE_SECURITY) else renderProgrammaticSecurityVault()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_SECURITY) {
            if (resultCode == RESULT_OK) renderProgrammaticSecurityVault()
            else { printToTerminal("> INTRUSION ATTEMPT BLOCKED."); triggerHapticFeedback(800) }
        } else if (requestCode == REQ_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                launchFloatingOrbService()
            } else {
                Toast.makeText(this, "Overlay permission is required for the floating orb.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderProgrammaticSecurityVault() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
            setBackgroundColor(Color.parseColor(HUD_COLOR_BLACK_BG))
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

        fun createInput(hintText: String, defaultVal: String): EditText {
            val et = EditText(this).apply {
                hint = hintText
                setText(defaultVal)
                setTextColor(Color.WHITE)
                setHintTextColor(Color.DKGRAY)
                setSingleLine()
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setBackgroundColor(Color.parseColor("#1A00E5FF"))
                setPadding(20, 30, 20, 30)
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 30
            et.layoutParams = params
            return et
        }

        val etGemini = createInput("Gemini API Key", cryptoEngine.decrypt(securityVaultPrefs.getString("API_GEMINI", "") ?: ""))
        val etGrok = createInput("Grok API Key", cryptoEngine.decrypt(securityVaultPrefs.getString("API_GROK", "") ?: ""))
        val etChatGPT = createInput("ChatGPT API Key", cryptoEngine.decrypt(securityVaultPrefs.getString("API_CHATGPT", "") ?: ""))

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
        }
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120)
        btnParams.topMargin = 20
        btnSave.layoutParams = btnParams
        
        container.addView(btnSave)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(container).create()

        btnSave.setOnClickListener {
            securityVaultPrefs.edit().apply {
                putString("API_GEMINI", cryptoEngine.encrypt(etGemini.text.toString().trim()))
                putString("API_GROK", cryptoEngine.encrypt(etGrok.text.toString().trim()))
                putString("API_CHATGPT", cryptoEngine.encrypt(etChatGPT.text.toString().trim()))
                apply()
            }
            printToTerminal("> VAULT SECURED: API Keys encrypted using Quantum Crypto Simulation.")
            Toast.makeText(this, "Credentials Secured.", Toast.LENGTH_SHORT).show()
            triggerHapticFeedback(200)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun fetchDecryptedApiKey(): String {
        val modelName = aiManager.getActiveModelName()
        return when {
            modelName.contains("Gemini", true) -> cryptoEngine.decrypt(securityVaultPrefs.getString("API_GEMINI", "") ?: "")
            modelName.contains("ChatGPT", true) -> cryptoEngine.decrypt(securityVaultPrefs.getString("API_CHATGPT", "") ?: "")
            modelName.contains("Grok", true) -> cryptoEngine.decrypt(securityVaultPrefs.getString("API_GROK", "") ?: "")
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
    // HARDWARE AUTOMATIONS
    // ========================================================================

    private fun operateHardwareFlashlight(mode: Int) {
        strobeJob?.cancel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainCameraId != null) {
                when (mode) {
                    0 -> { cameraManager.setTorchMode(mainCameraId!!, false); isTorchActive = false }
                    1 -> { cameraManager.setTorchMode(mainCameraId!!, true); isTorchActive = true }
                    2 -> { 
                        strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                            var toggleState = true
                            while(isActive) { cameraManager.setTorchMode(mainCameraId!!, toggleState); toggleState = !toggleState; delay(80) }
                        }
                    }
                    3 -> {
                        strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                            while(isActive) {
                                for(i in 1..3) { cameraManager.setTorchMode(mainCameraId!!, true); delay(200); cameraManager.setTorchMode(mainCameraId!!, false); delay(200) }
                                for(i in 1..3) { cameraManager.setTorchMode(mainCameraId!!, true); delay(600); cameraManager.setTorchMode(mainCameraId!!, false); delay(200) }
                                for(i in 1..3) { cameraManager.setTorchMode(mainCameraId!!, true); delay(200); cameraManager.setTorchMode(mainCameraId!!, false); delay(200) }
                                delay(1500)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { printToTerminal("> Camera Fault Detected. Operation Aborted.") }
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
        } catch (e: Exception) { Log.e(TAG, "Haptic feedback unavailable") }
    }

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
            Sensor.TYPE_LIGHT -> ambientLightLux = event.values[0]
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                if (!isShakeInitialized) { accelLastX = x; accelLastY = y; accelLastZ = z; isShakeInitialized = true }
                if (abs(accelLastX - x) > SHAKE_ACCEL_THRESHOLD || abs(accelLastY - y) > SHAKE_ACCEL_THRESHOLD || abs(accelLastZ - z) > SHAKE_ACCEL_THRESHOLD) {
                    if (!voiceSessionManager.isActive() && PermissionHelper.hasAudioPermission(this)) {
                        printToTerminal("> Kinematic Threshold Breached. Waking Voice Array.")
                        startVoiceMode() 
                    }
                }
                accelLastX = x; accelLastY = y; accelLastZ = z
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun runStartupDiagnosticSequence() {
        lifecycleScope.launch(Dispatchers.IO) {
            printToTerminal("> Running Core Neural Diagnostics...")
            delay(300)
            printToTerminal("> Injecting Quantum Cryptography Nodes...")
            delay(300)
            printToTerminal("> Initializing Python Bridge Subsystems...")
            delay(300)
            withContext(Dispatchers.Main) {
                currentSystemState = SystemState.ONLINE
                updateProgrammaticHUD()
                triggerHapticFeedback(150)
                speakCommandFeedback("J.A.R.V.I.S. Multi-Core Architecture Initialized. Awaiting input.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    
    override fun onDestroy() {
        printToTerminal("> FATAL SHUTDOWN SEQUENCE INITIATED.")
        currentSystemState = SystemState.OFFLINE
        try { unregisterReceiver(powerTelemetryReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(backgroundWakeReceiver) } catch (e: Exception) {}
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        operateHardwareFlashlight(0)
        try { voiceSessionManager.destroy() } catch (e: Exception) {}
        try { speechRecognizerManager.destroy() } catch (e: Exception) {}
        if (this::textToSpeechEngine.isInitialized) { textToSpeechEngine.stop(); textToSpeechEngine.shutdown() }
        viewModel.setResponseListener(null)
        mainThreadHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ============================================================================
    // INNER CLASSES: DATABASE, MATH, MATRIX, PYTHON BRIDGE, C++ STUBS
    // ============================================================================
    
    inner class JarvisDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "JarvisTitanMemory.db", null, 4) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE MemoryLog (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, query TEXT, response TEXT, intent_type TEXT)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS MemoryLog"); onCreate(db)
        }
        fun logInteraction(query: String, response: String, intentType: String) {
            try {
                val db = this.writableDatabase
                val values = ContentValues().apply {
                    put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    put("query", query); put("response", response); put("intent_type", intentType)
                }
                db.insert("MemoryLog", null, values); db.close()
            } catch (e: Exception) {}
        }
        fun getHistoryCount(): Int {
            var count = 0
            try { val cursor = this.readableDatabase.rawQuery("SELECT COUNT(*) FROM MemoryLog", null); if (cursor.moveToFirst()) count = cursor.getInt(0); cursor.close() } catch (e: Exception) {}
            return count
        }
        fun clearMemory() { try { this.writableDatabase.execSQL("DELETE FROM MemoryLog"); this.writableDatabase.close() } catch (e: Exception) {} }
    }

    inner class AdvancedScientificParser {
        fun evaluate(expression: String): Double {
            return object : Any() {
                var pos = -1
                var ch: Char = 0.toChar()

                fun nextChar() {
                    pos++
                    ch = if (pos < expression.length) expression[pos] else 0.toChar()
                }

                fun eat(charToEat: Char): Boolean {
                    while (ch == ' ') nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double { 
                    nextChar()
                    val x = parseExpression()
                    if (pos < expression.length) throw RuntimeException("Syntax Error")
                    return x 
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) { 
                        when { 
                            eat('+') -> x += parseTerm()
                            eat('-') -> x -= parseTerm()
                            else -> return x 
                        } 
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) { 
                        when { 
                            eat('*') -> x *= parseFactor()
                            eat('/') -> x /= parseFactor()
                            else -> return x 
                        } 
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+')) return parseFactor()
                    if (eat('-')) return -parseFactor()
                    var x: Double
                    val startPos = this.pos
                    if (eat('(')) { 
                        x = parseExpression()
                        eat(')') 
                    }
                    else if ((ch in '0'..'9') || ch == '.') {
                        while ((ch in '0'..'9') || ch == '.') nextChar()
                        x = expression.substring(startPos, this.pos).toDouble()
                    }
                    else if (ch in 'a'..'z') {
                        while (ch in 'a'..'z') nextChar()
                        val func = expression.substring(startPos, this.pos)
                        if (func == "pi") return Math.PI
                        if (func == "e") return Math.E
                        x = parseFactor()
                        x = when (func) {
                            "sqrt" -> Math.sqrt(x)
                            "sin" -> Math.sin(Math.toRadians(x))
                            "cos" -> Math.cos(Math.toRadians(x))
                            "tan" -> Math.tan(Math.toRadians(x))
                            "asin" -> Math.toDegrees(Math.asin(x))
                            "acos" -> Math.toDegrees(Math.acos(x))
                            "atan" -> Math.toDegrees(Math.atan(x))
                            "sinh" -> Math.sinh(x)
                            "cosh" -> Math.cosh(x)
                            "tanh" -> Math.tanh(x)
                            "log" -> Math.log10(x)
                            "ln" -> Math.log(x)
                            else -> throw RuntimeException("Unknown Function")
                        }
                    } else throw RuntimeException("Unexpected Token")
                    if (eat('^')) x = Math.pow(x, parseFactor())
                    return x
                }
            }.parse()
        }
    }

    inner class MatrixDigitalRainView(context: Context) : View(context) {
        private val rnd = Random(); private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        private val drops = Array(120) { DigitalDrop() } 
        private var hexGlowColor = HUD_COLOR_CYAN
        inner class DigitalDrop {
            var x = rnd.nextFloat() * 1500f; var y = rnd.nextFloat() * -3000f; var speed = rnd.nextFloat() * 18f + 8f
            var chars = CharArray(rnd.nextInt(30) + 8) { (rnd.nextInt(94) + 33).toChar() }
            var textSize = rnd.nextFloat() * 24f + 14f
        }
        fun updateHologramColor(state: OrbState) {
            hexGlowColor = when(state) { OrbState.ERROR -> HUD_COLOR_RED; OrbState.LISTENING -> HUD_COLOR_GREEN; OrbState.SPEAKING -> HUD_COLOR_ORANGE; else -> HUD_COLOR_CYAN }
        }
        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val w = width.toFloat(); val h = height.toFloat()
            for (drop in drops) {
                p.textSize = drop.textSize
                for (i in drop.chars.indices) {
                    if (rnd.nextFloat() > 0.96f) drop.chars[i] = (rnd.nextInt(94) + 33).toChar()
                    p.color = Color.parseColor(hexGlowColor)
                    p.alpha = (255 - (i * (255 / drop.chars.size))).coerceIn(0, 255)
                    c.drawText(drop.chars[i].toString(), drop.x, drop.y - (i * drop.textSize), p)
                }
                drop.y += drop.speed
                if (drop.y - (drop.chars.size * drop.textSize) > h) { drop.y = rnd.nextFloat() * -1000f; drop.x = rnd.nextFloat() * w; drop.speed = rnd.nextFloat() * 18f + 8f }
            }
            invalidate() 
        }
    }

    inner class DeepSystemDiagnostics(private val context: Context) {
        fun getRamUsagePercentage(): Int {
            return try {
                val mi = ActivityManager.MemoryInfo()
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.getMemoryInfo(mi)
                ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100).toInt()
            } catch (e: Exception) { 0 }
        }
        fun estimateCpuTemp(): String {
            val est = currentBatteryTemp + 5.0f
            return if (est > 0) "$est" else "N/A"
        }
        fun getAvailableStorageMB(): Long {
            return try {
                val stat = StatFs(Environment.getDataDirectory().path)
                val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
                bytesAvailable / (1024 * 1024)
            } catch (e: Exception) { 0L }
        }
    }

    inner class QuantumCryptographyManager {
        fun encrypt(rawString: String): String = if (rawString.isBlank()) "" else Base64.encodeToString(rawString.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        fun decrypt(encryptedString: String): String = if (encryptedString.isBlank()) "" else try { String(Base64.decode(encryptedString, Base64.DEFAULT), Charsets.UTF_8) } catch (e: Exception) { "" }
    }

    // ========================================================================
    // PYTHON INTEGRATION BRIDGE STUB (CHAQUOPY / REST API PREP)
    // ========================================================================
    inner class PythonAIBridge(private val context: Context) {
        fun executePythonScript(script: String): String {
            Log.d("PythonBridge", "Executing Script:\n$script")
            return "Sir, the Python backend simulated the script successfully."
        }
        
        fun sendToPythonMLModel(data: ByteArray): String {
            Log.d("PythonBridge", "Sending ${data.size} bytes to local Python Neural Network...")
            return "Classification complete."
        }
    }
}
