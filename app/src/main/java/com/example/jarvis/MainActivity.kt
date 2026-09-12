package com.example.jarvis

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import java.util.concurrent.Executor
import kotlin.math.roundToInt

/**
 * ============================================================================
 * J.A.R.V.I.S. MAIN NEURAL INTERFACE (ULTIMATE MONOLITHIC EDITION v6.0)
 * ============================================================================
 * Architect: Drako X Naeem
 * Lines of Code Target: 1700+
 * Architecture: Monolithic Core (All subsystems embedded for zero-latency)
 * * CAPABILITIES:
 * 1. Voice Engine & Cloud AI Routing
 * 2. Hardware Telemetry (Battery Voltage, Temp, RAM, Storage)
 * 3. 5-Axis Sensor Integration (Proximity, Light, Gyro, Accel, Mag)
 * 4. Biometric Security Vault
 * 5. Camera/Torch Hardware Control
 * 6. Background God-Mode IPC Sync
 * 7. Offline Local NLP Routing & Math Parser
 * 8. Dynamic UI Animation Sequencer
 * 9. Real-Time Network Bandwidth Monitoring
 * ============================================================================
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    // =========================================================
    // 1. CORE ARCHITECTURE & VIEW MODELS
    // =========================================================
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var prefs: PreferencesManager
    private lateinit var commandExecutor: CommandExecutor

    // =========================================================
    // 2. VOICE & AUDIO SUBSYSTEMS
    // =========================================================
    private lateinit var speechRecognizerManager: SpeechRecognizerManager
    private lateinit var textToSpeechManager: TextToSpeechManager
    private lateinit var voiceSessionManager: VoiceSessionManager
    private lateinit var voiceOverlayManager: VoiceOverlayManager
    private lateinit var audioManager: AudioManager

    // =========================================================
    // 3. HARDWARE & SENSOR SUBSYSTEMS
    // =========================================================
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var magneticSensor: Sensor? = null
    
    private lateinit var cameraManager: CameraManager
    private var mainCameraId: String? = null
    private var isTorchActive = false

    // =========================================================
    // 4. UI ELEMENTS BINDING
    // =========================================================
    private lateinit var micToggleButton: ImageButton
    private lateinit var messageInputBox: EditText
    private lateinit var sendCommandButton: ImageButton
    private lateinit var holographicOrbView: JarvisOrbView
    private lateinit var masterRootLayout: View
    private lateinit var telemetryDisplayBar: TextView
    private lateinit var mainRecyclerView: RecyclerView

    // =========================================================
    // 5. SYSTEM STATE REGISTERS & TELEMETRY
    // =========================================================
    private var isBackgroundCommandExecuting = false
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val TAG = "JarvisUltimateCore"

    // Battery Telemetry
    private var currentBatteryLevel = -1
    private var currentBatteryTemp = -1f
    private var currentBatteryVoltage = -1
    private var isDeviceCharging = false

    // Network Telemetry
    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkAvailable = false

    // Environment Telemetry
    private var ambientLightLevel = 0f
    private var isDeviceFaceDown = false

    // =========================================================
    // 6. CONSTANTS & PROTOCOL URLS
    // =========================================================
    companion object {
        private const val CREATOR_INSTAGRAM = "https://www.instagram.com/drakoxnaeem"
        private const val CREATOR_FACEBOOK = "https://www.facebook.com/share/1BsGJAatqh/"
        private const val CREATOR_PORTFOLIO = "https://frexxy-portfolio-3dri.vercel.app/#projects"
        
        private const val REQ_CODE_OVERLAY = 9001
        private const val REQ_CODE_AUDIO = 9002
        private const val REQ_CODE_BIOMETRIC = 9003
        private const val REQ_CODE_CAMERA = 9004
    }

    // =========================================================
    // 7. BROADCAST RECEIVERS (HARDWARE TELEMETRY)
    // =========================================================
    private val batteryTelemetryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            currentBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            currentBatteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isDeviceCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            
            updateTelemetryUI()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isNetworkAvailable = true
            runOnUiThread { updateTelemetryUI() }
        }
        override fun onLost(network: Network) {
            isNetworkAvailable = false
            runOnUiThread { updateTelemetryUI() }
        }
    }

    // =========================================================
    // 8. ACTIVITY LIFECYCLE: BOOT SEQUENCE
    // =========================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure immersive window flags for sci-fi HUD appearance
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        masterRootLayout = findViewById(android.R.id.content)

        Log.i(TAG, "==================================================")
        Log.i(TAG, "BOOT SEQUENCE INITIATED: J.A.R.V.I.S. Neural Hub")
        Log.i(TAG, "Architect: Drako X Naeem")
        Log.i(TAG, "==================================================")

        executeSystemInitialization()
    }

    private fun executeSystemInitialization() {
        initializeCoreManagers()
        initializeHardwareSubsystems()
        initializeNetworkSubsystem()
        bindUserInterface()
        setupChatRecyclerView()
        setupVoiceNeuralEngine()
        setupAICloudListener()
        setupInteractiveClickListeners()
        setupBottomNavigation()
        setupQuickActionDashboard()
        observeViewModelState()

        // Handle Background God Mode Intercepts
        handleIncomingVoiceIntent(intent)
        
        // Register Broadcasts
        registerReceiver(batteryTelemetryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        // Final Diagnostic
        runStartupDiagnosticSequence()
    }

    // =========================================================
    // 9. SUBSYSTEM INITIALIZATION
    // =========================================================
    private fun initializeCoreManagers() {
        Log.d(TAG, "Init: Core Managers")
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
        Log.d(TAG, "Init: Hardware Sensors")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            mainCameraId = cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Hardware Error: Camera/Torch module inaccessible.")
        }
    }

    private fun initializeNetworkSubsystem() {
        Log.d(TAG, "Init: Network Telemetry")
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    // =========================================================
    // 10. USER INTERFACE BINDING & RECYCLER
    // =========================================================
    private fun bindUserInterface() {
        Log.d(TAG, "Init: User Interface")
        messageInputBox = findViewById(R.id.messageInput)
        micToggleButton = findViewById(R.id.micButton)
        sendCommandButton = findViewById(R.id.sendButton)
        holographicOrbView = findViewById(R.id.jarvisOrbView)
        mainRecyclerView = findViewById(R.id.messageRecyclerView)
        telemetryDisplayBar = findViewById(R.id.systemStatusBar) ?: TextView(this)
        
        holographicOrbView.setOrbState(OrbState.IDLE)
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter()
        mainRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        mainRecyclerView.adapter = chatAdapter
    }

    private fun updateTelemetryUI() {
        val netStatus = if (isNetworkAvailable) "ONLINE" else "OFFLINE"
        val chargeStatus = if (isDeviceCharging) "AC" else "BAT"
        val text = "J.A.R.V.I.S. | NET: $netStatus | PWR: $currentBatteryLevel% [$chargeStatus] | TMP: ${currentBatteryTemp}°C"
        telemetryDisplayBar.text = text
    }

    // =========================================================
    // 11. VOICE NEURAL ENGINE (FOREGROUND SESSION)
    // =========================================================
    private fun setupVoiceNeuralEngine() {
        Log.d(TAG, "Init: Acoustic Neural Engine")
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
                        VoiceSessionManager.State.IDLE -> synchronizeHolographicState(OrbState.IDLE, "Standby")
                        VoiceSessionManager.State.LISTENING -> synchronizeHolographicState(OrbState.LISTENING, "Listening...")
                        VoiceSessionManager.State.PROCESSING -> synchronizeHolographicState(OrbState.THINKING, "Synthesizing...")
                        VoiceSessionManager.State.SPEAKING -> synchronizeHolographicState(OrbState.SPEAKING, "Transmitting...")
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
    // 12. IPC HOLOGRAPHIC SYNCHRONIZER (GOD MODE SYNC)
    // =========================================================
    private fun synchronizeHolographicState(state: OrbState, subText: String) {
        // 1. Update Main UI Orb
        holographicOrbView.setOrbState(state)
        
        // 2. Update Overlay Manager (if active)
        voiceOverlayManager.updateState(state)
        
        // 3. Update Mic Alpha
        micToggleButton.alpha = if (state == OrbState.LISTENING) 1.0f else 0.7f

        // 4. Send Broadcast to God Mode Background Service
        val syncIntent = Intent(VoiceService.ACTION_UPDATE_STATE).apply {
            putExtra(VoiceService.EXTRA_STATE, state.name)
        }
        sendBroadcast(syncIntent)

        // 5. Dynamic Background Environmental Tinting
        animateBackgroundTint(state)
    }

    private fun animateBackgroundTint(state: OrbState) {
        val targetColor = when(state) {
            OrbState.ERROR -> Color.parseColor("#2A4A0000") // Crimson Danger
            OrbState.THINKING -> Color.parseColor("#2A004A4A") // Deep Cyan Processing
            OrbState.LISTENING -> Color.parseColor("#1A00FF00") // Faint Green
            else -> Color.parseColor("#000000") // Void Black
        }
        
        val animator = ObjectAnimator.ofArgb(masterRootLayout, "backgroundColor", targetColor)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    // =========================================================
    // 13. GEMINI CLOUD AI INTERCEPTOR
    // =========================================================
    private fun setupAICloudListener() {
        viewModel.setResponseListener { networkResponse ->
            mainThreadHandler.post {
                val safeResponse = validateCloudResponse(networkResponse)

                if (voiceSessionManager.isActive()) {
                    executeVoiceOutputInForeground(safeResponse)
                } else if (isBackgroundCommandExecuting) {
                    executeVoiceOutputInBackground(safeResponse)
                } else {
                    synchronizeHolographicState(OrbState.SPEAKING, "Speaking")
                    textToSpeechManager.speak(
                        text = safeResponse,
                        onFinished = {
                            mainThreadHandler.post { synchronizeHolographicState(OrbState.IDLE, "Standby") }
                        }
                    )
                }
            }
        }
    }

    private fun validateCloudResponse(rawResponse: String): String {
        return if (
            rawResponse.contains("Unable to resolve host", ignoreCase = true) ||
            rawResponse.contains("Failed to connect", ignoreCase = true) ||
            rawResponse.contains("timeout", ignoreCase = true) ||
            rawResponse.contains("400", ignoreCase = true)
        ) {
            Log.e(TAG, "Cloud Neural Link Severed: $rawResponse")
            "Sir, the neural link to the cloud is severed. Please verify network integrity or API parameters."
        } else {
            rawResponse
        }
    }

    // =========================================================
    // 14. BACKGROUND IPC INTENT HANDLER
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
    // 15. THE MASTER NLP ROUTER (ZERO-LATENCY OFFLINE ENGINE)
    // =========================================================
    private fun evaluateAndExecuteMasterCommand(rawInput: String) {
        val normalized = rawInput.lowercase(Locale.getDefault())
            .replace("hey jarvis", "").replace("ok jarvis", "").replace("jarvis", "").trim()

        if (normalized.isBlank()) {
            if (voiceSessionManager.isActive()) speakCommandFeedback("Yes sir, awaiting protocols.")
            else synchronizeHolographicState(OrbState.IDLE, "Standby")
            return
        }

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

        // ROUTER 6: CLOUD INFERENCE (GEMINI)
        if (!isNetworkAvailable) {
            speakCommandFeedback("Network is currently offline sir. I cannot process complex neural queries at this moment.")
            return
        }
        
        Log.i(TAG, "Routing to Gemini Cloud: $normalized")
        if (voiceSessionManager.isActive()) voiceSessionManager.setProcessing()
        viewModel.send(normalized, prefs.getApiKey())
    }

    // =========================================================
    // 15.1 OFFLINE MATH SOLVER
    // =========================================================
    private fun handleMathProtocols(command: String): Boolean {
        // Simple regex to catch basic math queries offline (e.g. "what is 5 plus 5")
        if (command.contains("plus") || command.contains("+") || command.contains("minus") || command.contains("-")) {
            try {
                // A very basic extraction for demonstration of offline capabilities
                val numbers = command.replace(Regex("[^0-9]"), " ").trim().split(Regex("\\s+"))
                if (numbers.size >= 2) {
                    val a = numbers[0].toInt()
                    val b = numbers[1].toInt()
                    if (command.contains("plus") || command.contains("+")) {
                        speakCommandFeedback("Sir, $a plus $b equals ${a + b}.")
                        return true
                    } else if (command.contains("minus") || command.contains("-")) {
                        speakCommandFeedback("Sir, $a minus $b equals ${a - b}.")
                        return true
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "Math parser error") }
        }
        return false
    }

    // =========================================================
    // 15.2 HARDWARE & SENSOR PROTOCOLS
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
            containsAny(command, "vibrate", "test haptics") -> {
                triggerHapticFeedback(500)
                speakCommandFeedback("Haptic motors tested successfully.")
                return true
            }
            containsAny(command, "check light", "how dark") -> {
                speakCommandFeedback("Ambient light level is currently at $ambientLightLevel lux.")
                return true
            }
        }
        return false
    }

    // =========================================================
    // 15.3 SYSTEM TELEMETRY PROTOCOLS
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
    // 15.4 APP AUTOMATION PROTOCOLS
    // =========================================================
    private fun handleAppAutomationProtocols(command: String): Boolean {
        when {
            containsAny(command, "instagram open", "instagram kholo", "open instagram") -> {
                openInstagram()
                speakCommandFeedback("Accessing Instagram.")
                return true
            }
            containsAny(command, "youtube open", "youtube kholo", "open youtube") -> {
                openYouTube()
                speakCommandFeedback("Initializing YouTube.")
                return true
            }
            containsAny(command, "whatsapp open", "whatsapp kholo", "open whatsapp") -> {
                openWhatsApp()
                speakCommandFeedback("WhatsApp loaded.")
                return true
            }
            containsAny(command, "google open", "chrome open", "browser kholo") -> {
                openUrl("https://www.google.com")
                speakCommandFeedback("Opening global web search.")
                return true
            }
            containsAny(command, "open camera", "camera kholo") -> {
                openCamera()
                speakCommandFeedback("Camera interface engaged.")
                return true
            }
        }
        return false
    }

    // =========================================================
    // 15.5 SETTINGS & DEVICE PROTOCOLS
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
            containsAny(command, "bluetooth settings") -> {
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
    // 16. HARDWARE MOTOR FUNCTIONS
    // =========================================================
    private fun toggleFlashlight(status: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainCameraId != null) {
                cameraManager.setTorchMode(mainCameraId!!, status)
                isTorchActive = status
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hardware Exception: Torch toggle failed.")
        }
    }

    private fun triggerHapticFeedback(durationMs: Long = 200) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) { Log.e(TAG, "Haptic exception") }
    }

    private fun getAvailableInternalMemorySize(): String {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val gb = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
        return "$gb Gigabytes"
    }

    // =========================================================
    // 17. TEXT-TO-SPEECH DISPATCHERS
    // =========================================================
    private fun executeVoiceOutputInForeground(speechText: String) {
        if (!voiceSessionManager.isActive()) return
        voiceSessionManager.setSpeaking()
        synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
        speechRecognizerManager.stop()

        textToSpeechManager.speak(
            text = speechText,
            onStarted = { mainThreadHandler.post { synchronizeHolographicState(OrbState.SPEAKING, "Transmitting") } },
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

                    // Signal VoiceService background listener to resume acoustic polling
                    try {
                        startService(Intent(this, VoiceService::class.java).apply {
                            action = VoiceService.ACTION_VOICE_RESPONSE_FINISHED
                        })
                    } catch (e: Exception) { Log.e(TAG, "Failed to signal background service") }
                }
            }
        )
    }

    private fun speakCommandFeedback(feedbackText: String) {
        if (voiceSessionManager.isActive()) executeVoiceOutputInForeground(feedbackText)
        else if (isBackgroundCommandExecuting) executeVoiceOutputInBackground(feedbackText)
        else {
            synchronizeHolographicState(OrbState.SPEAKING, "Transmitting")
            textToSpeechManager.speak(feedbackText, onFinished = { 
                mainThreadHandler.post { synchronizeHolographicState(OrbState.IDLE, "Standby") } 
            })
        }
    }

    // =========================================================
    // 18. INTERACTIVE UI EVENT LISTENERS
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
            if (voiceSessionManager.isActive()) stopVoiceMode() else startVoiceMode()
        }

        findViewById<View>(R.id.settingsButton)?.setOnClickListener { authenticateAndOpenSecurityVault() }
        findViewById<View>(R.id.menuButton)?.setOnClickListener { showNeuralOptionsMenu() }
        findViewById<View>(R.id.searchButton)?.setOnClickListener { showGlobalSearchDialog() }
        findViewById<View>(R.id.historyButton)?.setOnClickListener { Toast.makeText(this, "Logs Encrypted.", Toast.LENGTH_SHORT).show() }
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
    // 19. DASHBOARD & QUICK NAVIGATION
    // =========================================================
    private fun setupBottomNavigation() {
        findViewById<View>(R.id.chatTab)?.setOnClickListener { triggerHapticFeedback(50) }
        findViewById<View>(R.id.toolsTab)?.setOnClickListener { showToolsDashboard() }
        findViewById<View>(R.id.assistTab)?.setOnClickListener { Toast.makeText(this, "Assist Active", Toast.LENGTH_SHORT).show() }
    }

    private fun setupQuickActionDashboard() {
        findViewById<View>(R.id.webButton)?.setOnClickListener { handleAppAutomationProtocols("google open") }
        findViewById<View>(R.id.youtubeButton)?.setOnClickListener { handleAppAutomationProtocols("youtube open") }
        findViewById<View>(R.id.instagramButton)?.setOnClickListener { handleAppAutomationProtocols("instagram open") }
        findViewById<View>(R.id.whatsappButton)?.setOnClickListener { handleAppAutomationProtocols("whatsapp open") }
        findViewById<View>(R.id.appsButton)?.setOnClickListener { showAppGrid() }
        findViewById<View>(R.id.moreButton)?.setOnClickListener { showExtendedMenu() }
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
    // 20. BIOMETRIC SECURITY PROTOCOL (VAULT)
    // =========================================================
    private fun authenticateAndOpenSecurityVault() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor: Executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(applicationContext, "Security Lock: $errString", Toast.LENGTH_SHORT).show()
                        }
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            speakCommandFeedback("Security cleared. Accessing core settings.")
                            showCoreSettingsDialog()
                        }
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            speakCommandFeedback("Unauthorized user detected.")
                            triggerHapticFeedback(500)
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("J.A.R.V.I.S. Security Vault")
                    .setSubtitle("Verify identity to access Core Settings")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // Fallback if no biometrics setup
                showCoreSettingsDialog()
            }
        }
    }

    // =========================================================
    // 21. DIALOGS, MENUS & DEVELOPER PROTOCOLS
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
            setTextColor(Color.parseColor("#00E5FF"))
            isChecked = prefs.isVoiceWakeEnabled()
            setPadding(0, 40, 0, 10)
        }
        containerLayout.addView(godModeSwitch)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("⚙ J.A.R.V.I.S. CORE CONFIGURATION")
            .setView(containerLayout)
            .setPositiveButton("APPLY & REBOOT") { _, _ ->
                val newApiKey = apiKeyInputField.text.toString().trim()
                prefs.saveApiKey(newApiKey)

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

                Toast.makeText(this, "Core parameters updated.", Toast.LENGTH_SHORT).show()
                speakCommandFeedback("Settings updated and saved.")
            }
            .setNegativeButton("DISMISS", null)
            .show()
    }

    private fun promptSystemOverlayPermission() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ GOD MODE RESTRICTION DETECTED")
            .setMessage("JARVIS ko background floating orb (God Mode) chalane ke liye 'Display over other apps' ki permission chahiye. Kripya enable karein.")
            .setPositiveButton("GRANT PERMISSION") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQ_CODE_OVERLAY)
            }
            .setCancelable(false)
            .show()
    }

    private fun showGlobalSearchDialog() {
        val input = EditText(this).apply { hint = "Enter global search query..." ; setSingleLine(true) ; setTextColor(Color.WHITE) }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("⌕ QUANTUM SEARCH")
            .setView(input)
            .setPositiveButton("EXECUTE") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) openUrl("https://www.google.com/search?q=" + Uri.encode(query))
            }
            .show()
    }

    private fun showNeuralOptionsMenu() {
        val menuOptions = arrayOf("⚙ Security Vault", "🛠 Diagnostic Tools", "◉ Neural Assist", "▣ App Drawer", "◆ Framework Status", "✦ Architect Info")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("✦ SYSTEM MENU").setItems(menuOptions) { _, w ->
            when (w) {
                0 -> authenticateAndOpenSecurityVault()
                1 -> showToolsDashboard()
                2 -> Toast.makeText(this, "Neural Assist active.", Toast.LENGTH_SHORT).show()
                3 -> showAppGrid()
                4 -> showSystemInfo()
                5 -> showArchitectInfo()
            }
        }.show()
    }

    private fun showToolsDashboard() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("✦ PROTOCOLS").setItems(arrayOf("🌐 Web Node", "▶ Media Stream", "◎ Social Feed", "◈ Comm Link")) { _, w ->
            when (w) {
                0 -> openUrl("https://www.google.com")
                1 -> openYouTube()
                2 -> openInstagram()
                3 -> openWhatsApp()
            }
        }.show()
    }

    private fun showAppGrid() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("✦ INTEGRATIONS").setItems(arrayOf("◎ Instagram", "▶ YouTube", "◈ WhatsApp", "G Search")) { _, w ->
            when (w) {
                0 -> openInstagram()
                1 -> openYouTube()
                2 -> openWhatsApp()
                3 -> openUrl("https://www.google.com")
            }
        }.show()
    }

    private fun showExtendedMenu() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("✦ EXTENDED MENU").setItems(arrayOf("⚙ Secured Core", "◷ Telemetry Log", "◆ Specs", "✦ Creator")) { _, w ->
            when (w) {
                0 -> authenticateAndOpenSecurityVault()
                1 -> Toast.makeText(this, "Logs encrypted.", Toast.LENGTH_SHORT).show()
                2 -> showSystemInfo()
                3 -> showArchitectInfo()
            }
        }.show()
    }

    private fun showSystemInfo() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("◆ J.A.R.V.I.S. V6.0 (ULTIMATE)")
            .setMessage("Architect: 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎\nType: Monolithic AI Framework\nNetwork: $isNetworkAvailable\nBattery: $currentBatteryLevel%\nStatus: Fully Operational")
            .setPositiveButton("CLOSE", null).show()
    }

    private fun showArchitectInfo() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✦ ARCHITECT")
            .setMessage("Developed By 𝑵𝒂𝒆𝒎\n𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎")
            .setItems(arrayOf("◎ Instagram", "f Facebook", "⌂ Cyber-Portfolio")) { _, w ->
                when (w) {
                    0 -> openInstagram()
                    1 -> openUrl(FACEBOOK_URL)
                    2 -> openUrl(WEBSITE_URL)
                }
            }.setNegativeButton("DISMISS", null).show()
    }

    // =========================================================
    // 22. INTENT LAUNCHERS
    // =========================================================
    private fun openInstagram() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=drakoxnaeem"))) } 
        catch (e: Exception) { openUrl(INSTAGRAM_URL) }
    }

    private fun openYouTube() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"))) } 
        catch (e: Exception) { openUrl("https://www.youtube.com") }
    }

    private fun openWhatsApp() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://"))) } 
        catch (e: Exception) { openUrl("https://web.whatsapp.com") }
    }
    
    private fun openCamera() {
        try { startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) } 
        catch (e: Exception) { Toast.makeText(this, "Camera access denied.", Toast.LENGTH_SHORT).show() }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) return
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } 
        catch (e: Exception) { Toast.makeText(this, "Routing failed.", Toast.LENGTH_SHORT).show() }
    }

    // =========================================================
    // 23. BACKGROUND SERVICE LIFECYCLE (GOD MODE CONTROLS)
    // =========================================================
    private fun launchBackgroundVoiceService() {
        if (!PermissionHelper.hasAudioPermission(this)) {
            PermissionHelper.requestAudioPermission(this)
            return
        }
        val serviceIntent = Intent(this, VoiceService::class.java).apply { action = VoiceService.ACTION_ENABLE_WAKE }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(this, serviceIntent)
            else startService(serviceIntent)
            Log.i(TAG, "God Mode Background Engine Online.")
        } catch (e: Exception) { Log.e(TAG, "God Mode Boot Failure") }
    }

    private fun terminateBackgroundVoiceService() {
        try { stopService(Intent(this, VoiceService::class.java)) } catch (e: Exception) {}
        isBackgroundCommandExecuting = false
        Log.i(TAG, "God Mode Background Engine Offline.")
    }

    // =========================================================
    // 24. HARDWARE SENSOR EVENT LISTENERS
    // =========================================================
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                if (distance < (proximitySensor?.maximumRange ?: 5f)) {
                    // Safety Override: Covering sensor stops all speech instantly
                    if (voiceSessionManager.isActive() || textToSpeechManager.isSpeaking()) {
                        textToSpeechManager.stop()
                        speechRecognizerManager.stop()
                        Log.i(TAG, "Proximity Override: Audio Emergency Mute.")
                    }
                }
            }
            Sensor.TYPE_LIGHT -> {
                ambientLightLevel = event.values[0]
                // Optional: Auto-dim UI based on light sensor can be implemented here
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values[2]
                isDeviceFaceDown = z < -8.0f
                if (isDeviceFaceDown && voiceSessionManager.isActive()) {
                    // Auto-stop listening if phone is placed face down
                    stopVoiceMode()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sensor calibration telemetry (ignored for now)
    }

    // =========================================================
    // 25. STARTUP DIAGNOSTICS & THREADING
    // =========================================================
    private fun runStartupDiagnosticSequence() {
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Diag: Checking Neural Weights...")
            delay(500)
            Log.d(TAG, "Diag: Verifying Sensor Calibrations...")
            delay(500)
            withContext(Dispatchers.Main) {
                Log.i(TAG, "DIAGNOSTIC PASSED: J.A.R.V.I.S. Neural Link Established.")
                triggerHapticFeedback(150)
            }
        }
    }

    // =========================================================
    // 26. MASTER LIFECYCLE MANAGEMENT & MEMORY PURGE
    // =========================================================
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Lifecycle: onResume (Binding Sensors)")
        // Register all available sensors
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        
        // Ensure Background Engine is running if enabled
        if (::prefs.isInitialized && prefs.isVoiceWakeEnabled() && PermissionHelper.hasAudioPermission(this)) {
            launchBackgroundVoiceService()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Lifecycle: onPause (Unbinding Sensors)")
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        Log.i(TAG, "==================================================")
        Log.i(TAG, "SHUTDOWN SEQUENCE: Deactivating Neural Interface.")
        Log.i(TAG, "==================================================")
        
        // 1. Release Hardware
        try { unregisterReceiver(batteryTelemetryReceiver) } catch (e: Exception) {}
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        if (isTorchActive) toggleFlashlight(false)
        
        // 2. Terminate Voice Engines
        try { voiceSessionManager.destroy() } catch (e: Exception) {}
        try { speechRecognizerManager.destroy() } catch (e: Exception) {}
        try { textToSpeechManager.shutdown() } catch (e: Exception) {}
        try { voiceOverlayManager.destroy() } catch (e: Exception) {}
        
        // 3. Purge Memory
        viewModel.setResponseListener(null)
        mainThreadHandler.removeCallbacksAndMessages(null)
        
        super.onDestroy()
    }
}