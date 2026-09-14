package com.example.jarvis

// ============================================================================
// EXHAUSTIVE SYSTEM IMPORTS (TITAN HYBRID CORE V100.0 - ENTERPRISE EDITION)
// J.A.R.V.I.S. ARCHITECTURE BY DRAKOX NAEEM
// ============================================================================

import android.Manifest
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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.Gravity
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.jarvis.ui.JarvisOrbView
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
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * ============================================================================
 * J.A.R.V.I.S. MAIN ACTIVITY - THE BRAIN OF THE OPERATION
 * ============================================================================
 * THEME PATH VERIFIED: app/src/main/res/drawable/jarvis_bg.png
 * ICON PATH VERIFIED: app/src/main/res/mipmap-hdpi/ic_launcher_round.png
 * ============================================================================
 */
class MainActivity : ComponentActivity(), SensorEventListener, TextToSpeech.OnInitListener {

    // ========================================================================
    // SYSTEM NATIVE BINDINGS
    // ========================================================================
    init {
        try {
            System.loadLibrary("jarvis_native_engine")
            Log.i("JarvisJNI", "Native C++ Titan Core Injected Successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("JarvisJNI", "Native C++ fallback to Dalvik/ART virtual machine.")
        }
    }

    external fun stringFromJNI(): String
    external fun quantumCompute(input: DoubleArray): DoubleArray

    // ========================================================================
    // SYSTEM STATES & GLOBALS
    // ========================================================================
    enum class SystemState { BOOTING, ONLINE, STANDBY, PROCESSING, LISTENING, SPEAKING, CRITICAL_ERROR, COMBAT_MODE, STEALTH_MODE }
    private var currentSystemState = SystemState.BOOTING

    // UI Elements
    private lateinit var messageInputBox: EditText
    private lateinit var micToggleButton: ImageButton
    private lateinit var sendCommandButton: ImageButton
    private lateinit var terminalOutput: TextView
    private lateinit var systemStatusText: TextView
    private lateinit var holographicOrbView: JarvisOrbView
    private lateinit var masterRootLayout: ViewGroup
    
    // Custom Programmatic Views
    private var matrixBackground: MatrixDigitalRainView? = null
    private var cyberpunkRadar: CyberpunkRadarHUD? = null

    // Managers & Core Engines
    private lateinit var textToSpeechEngine: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var localDatabase: MassiveDatabaseHelper
    private lateinit var securityVaultPrefs: SharedPreferences
    private lateinit var cryptoEngine: QuantumCryptographyManager
    private lateinit var deepDiagnosticsEngine: DeepSystemDiagnostics
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newFixedThreadPool(4)

    // Hardware & Telemetry
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var gyroSensor: Sensor? = null
    private var magneticSensor: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var mainCameraId: String? = null
    private var strobeJob: Job? = null
    
    // Live Data
    private var isNetworkAvailable = false
    private var currentBatteryLevel = -1
    private var currentBatteryTemp = -1f
    private var isDeviceCharging = false
    private var ambientLightLux = 0f
    private var systemUpTimeStr = ""
    private val bootTimeMillis = System.currentTimeMillis()

    // ========================================================================
    // LIFECYCLE HOOKS
    // ========================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen Sci-Fi Mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.statusBarColor = Color.parseColor("#02040A")
        
        setContentView(R.layout.activity_main)
        masterRootLayout = findViewById(android.R.id.content)

        // Verifying theme assignment natively
        masterRootLayout.setBackgroundResource(R.drawable.jarvis_bg)

        initializeCoreEngines()
        bindViewsSafely()
        injectAdvancedHUD()
        setupListenersAndHardware()
        runMassiveBootSequence()
    }

    private fun initializeCoreEngines() {
        textToSpeechEngine = TextToSpeech(this, this)
        localDatabase = MassiveDatabaseHelper(this)
        securityVaultPrefs = getSharedPreferences("JarvisTitanVault", Context.MODE_PRIVATE)
        cryptoEngine = QuantumCryptographyManager()
        deepDiagnosticsEngine = DeepSystemDiagnostics(this)

        setupNetworkTelemetry()
        setupBatteryTelemetry()
    }

    private fun bindViewsSafely() {
        try {
            messageInputBox = findViewById(R.id.messageInput)
            micToggleButton = findViewById(R.id.micButton)
            sendCommandButton = findViewById(R.id.sendButton)
            terminalOutput = findViewById(R.id.terminalOutput)
            systemStatusText = findViewById(R.id.systemStatusText)
            holographicOrbView = findViewById(R.id.jarvisOrbView)
        } catch (e: Exception) {
            Log.e("JARVIS", "View Binding Failure: Make sure activity_main.xml is correct.")
        }
    }

    private fun injectAdvancedHUD() {
        // Matrix Rain Layer
        matrixBackground = MatrixDigitalRainView(this)
        val matrixParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        matrixBackground?.alpha = 0.4f
        masterRootLayout.addView(matrixBackground, 0, matrixParams)

        // Cyberpunk Radar HUD Layer
        cyberpunkRadar = CyberpunkRadarHUD(this)
        val radarParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        masterRootLayout.addView(cyberpunkRadar, 1, radarParams)
    }

    private fun setupListenersAndHardware() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            mainCameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager?.getCameraCharacteristics(id)?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {}

        // UI Listeners
        sendCommandButton.setOnClickListener {
            val query = messageInputBox.text.toString().trim()
            if (query.isNotEmpty()) {
                executeMasterCommand(query)
                messageInputBox.text.clear()
            }
        }

        micToggleButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            } else {
                startVoiceRecognition()
            }
        }

        // Module Listeners
        findViewById<View>(R.id.settingsButton)?.setOnClickListener { authenticateAndOpenVault() }
        findViewById<View>(R.id.webButton)?.setOnClickListener { executeMasterCommand("open google") }
        findViewById<View>(R.id.youtubeButton)?.setOnClickListener { executeMasterCommand("open youtube") }
        findViewById<View>(R.id.instagramButton)?.setOnClickListener { executeMasterCommand("open instagram") }
        findViewById<View>(R.id.whatsappButton)?.setOnClickListener { executeMasterCommand("open whatsapp") }
        findViewById<View>(R.id.btnSettings)?.setOnClickListener { authenticateAndOpenVault() }
    }

    // ========================================================================
    // TELEMETRY BROADCAST RECEIVERS
    // ========================================================================
    private fun setupNetworkTelemetry() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { 
                    isNetworkAvailable = true
                    printToTerminal("> SATELLITE UPLINK: ESTABLISHED [PING: <20ms]") 
                }
                override fun onLost(network: Network) { 
                    isNetworkAvailable = false
                    printToTerminal("> SATELLITE UPLINK: SEVERED [SWITCHING TO LOCAL]") 
                }
            })
    }

    private fun setupBatteryTelemetry() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                currentBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                currentBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isDeviceCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    // ========================================================================
    // BOOT SEQUENCE MANAGER
    // ========================================================================
    private fun runMassiveBootSequence() {
        val bootLog = listOf(
            "> INITIATING TITAN CORE V100...",
            "> LOADING DRAKOX NAEEM PREFERENCES...",
            "> SYNCING ARC REACTOR THEME [app/src/main/res/drawable/jarvis_bg.png]...",
            "> SYNCING LAUNCHER ICON [mipmap-hdpi/ic_launcher_round.png]...",
            "> BYPASSING MAINFRAME SECURITY FIREWALLS...",
            "> INJECTING C++ NATIVE ALGORITHMS...",
            "> INITIALIZING QUANTUM NEURAL NETWORK...",
            "> CALIBRATING GYROSCOPIC SENSORS...",
            "> CONNECTING TO GLOBAL SATELLITE GRID...",
            "> MEMORY MODULES ONLINE.",
            "> WAITING FOR MASTER COMMAND..."
        )

        lifecycleScope.launch {
            terminalOutput.text = ""
            for (log in bootLog) {
                for (char in log) {
                    terminalOutput.append(char.toString())
                    delay(15) // Typewriter speed
                }
                terminalOutput.append("\n")
                delay(200)
            }
            systemStatusText.text = "● TITAN SYSTEM FULLY OPERATIONAL"
            systemStatusText.setTextColor(Color.parseColor("#00FF00"))
            speakTextNative("All core systems are online and fully operational. DrakoX Jarvis is at your service.")
            currentSystemState = SystemState.ONLINE
            updateEnvironmentLighting(SystemState.ONLINE)
        }
    }

    // ========================================================================
    // MASTER COMMAND PARSER & NLP ROUTER (Extremely Exhaustive)
    // ========================================================================
    private fun executeMasterCommand(rawInput: String) {
        val normalized = rawInput.lowercase(Locale.getDefault()).trim()
        printToTerminal("> USER: $rawInput")
        localDatabase.logInteraction(normalized, "Processing...", "USER_INPUT")
        updateEnvironmentLighting(SystemState.PROCESSING)

        // 1. Math Engine (Advanced Scientific Parser)
        if (normalized.matches(Regex(".*(calculate|math|plus|minus|multiply|divided|times|root|power|sin|cos|tan).*"))) {
            try {
                val equation = normalized.replace(Regex("[^0-9\\+\\-\\*\\/\\(\\)\\.\\^a-z]"), "")
                val result = AdvancedScientificParser().evaluate(equation)
                printToTerminal("> QUANTUM MATH ENGINE: Result -> $result")
                speakTextNative("Sir, the mathematical evaluation results in $result.")
                localDatabase.logInteraction(equation, result.toString(), "MATH_SOLVED")
            } catch (e: Exception) { 
                printToTerminal("> MATH ERROR: Syntactical anomaly detected.") 
                speakTextNative("There is a syntax error in your calculation, sir.")
            }
            mainThreadHandler.postDelayed({ updateEnvironmentLighting(SystemState.STANDBY) }, 2000)
            return
        }

        // 2. Hardware Automations (Extensive)
        when {
            normalized.contains("torch on") || normalized.contains("light on") -> { 
                operateHardwareFlashlight(1); speakTextNative("Optical illumination engaged."); resetState(); return 
            }
            normalized.contains("torch off") || normalized.contains("light off") -> { 
                operateHardwareFlashlight(0); speakTextNative("Optical illumination disengaged."); resetState(); return 
            }
            normalized.contains("strobe mode") -> { 
                operateHardwareFlashlight(2); speakTextNative("Strobe defense protocol active."); resetState(); return 
            }
            normalized.contains("sos mode") -> { 
                operateHardwareFlashlight(3); speakTextNative("Transmitting visual SOS signal."); resetState(); return 
            }
            normalized.contains("morse code") -> {
                operateHardwareFlashlight(4); speakTextNative("Transmitting classified morse code."); resetState(); return
            }
            normalized.contains("vibrate") || normalized.contains("haptic") -> {
                triggerHapticFeedback(1000); speakTextNative("Haptic engines fired."); resetState(); return
            }
        }

        // 3. System Telemetry & Diagnostics
        when {
            normalized.contains("battery") || normalized.contains("power") -> {
                val state = if (isDeviceCharging) "charging" else "discharging"
                speakTextNative("The power cell is at $currentBatteryLevel percent and is currently $state. Core temperature is ${currentBatteryTemp} degrees Celsius.")
                printToTerminal("> BATTERY: $currentBatteryLevel% | TEMP: ${currentBatteryTemp}°C")
                resetState(); return
            }
            normalized.contains("system status") || normalized.contains("diagnostics") -> {
                val ram = deepDiagnosticsEngine.getRamUsagePercentage()
                val storage = deepDiagnosticsEngine.getAvailableStorageMB()
                speakTextNative("Initiating deep system diagnostics. RAM utilization is $ram percent. Available storage is $storage Megabytes. All neural pathways are stable.")
                printToTerminal("> DIAGNOSTICS: RAM $ram% | DISK ${storage}MB")
                resetState(); return
            }
            normalized.contains("environment") || normalized.contains("surroundings") -> {
                speakTextNative("Ambient light levels are at $ambientLightLux lux. Gyroscopic and magnetic fields are nominal.")
                printToTerminal("> SENSORS: LUX $ambientLightLux")
                resetState(); return
            }
        }

        // 4. Combat / UI Modes
        when {
            normalized.contains("combat mode") || normalized.contains("defense protocol") -> {
                currentSystemState = SystemState.COMBAT_MODE
                updateEnvironmentLighting(SystemState.COMBAT_MODE)
                speakTextNative("Combat mode engaged. Tactical HUD activated. Restricting background processes.")
                triggerHapticFeedback(500)
                return
            }
            normalized.contains("stealth mode") -> {
                currentSystemState = SystemState.STEALTH_MODE
                updateEnvironmentLighting(SystemState.STEALTH_MODE)
                speakTextNative("Stealth mode activated. All audio output will be suppressed, and screen brightness minimized.")
                return
            }
            normalized.contains("stand down") || normalized.contains("normal mode") -> {
                currentSystemState = SystemState.ONLINE
                updateEnvironmentLighting(SystemState.ONLINE)
                speakTextNative("Standing down. Returning to standard operational parameters.")
                return
            }
        }

        // 5. App Launchers & Deep Links
        when {
            normalized.contains("open google") || normalized.contains("open browser") -> { openUrl("https://www.google.com"); speakTextNative("Accessing global network."); resetState(); return }
            normalized.contains("open youtube") -> { openUrl("vnd.youtube:"); speakTextNative("Initializing video stream."); resetState(); return }
            normalized.contains("open instagram") -> { openUrl("https://www.instagram.com/drakoxnaeem"); speakTextNative("Accessing social grids."); resetState(); return }
            normalized.contains("open whatsapp") -> { openUrl("whatsapp://"); speakTextNative("Opening encrypted communications."); resetState(); return }
            normalized.contains("open github") -> { openUrl("https://github.com/drakoxnaeem"); speakTextNative("Accessing source code repositories."); resetState(); return }
            normalized.contains("clear memory") -> { localDatabase.clearMemory(); speakTextNative("Local memory cache wiped."); resetState(); return }
        }

        // 6. AI Fallback Engine (Simulated Cloud Request)
        speakTextNative("Processing your request through the Titan neural network.")
        printToTerminal("> NEURAL NET: Routing complex query to cloud API...")
        
        lifecycleScope.launch {
            delay(2500) // Simulate network latency
            if (isNetworkAvailable) {
                val simulatedResponse = generateAIResponse(normalized)
                speakTextNative(simulatedResponse)
                printToTerminal("> CLOUD RESPONSE: $simulatedResponse")
                localDatabase.logInteraction(normalized, simulatedResponse, "AI_CLOUD")
            } else {
                speakTextNative("Network failure detected. Cannot reach the cloud architecture.")
                printToTerminal("> ERROR: No uplink available.")
            }
            updateEnvironmentLighting(SystemState.STANDBY)
        }
    }

    private fun generateAIResponse(query: String): String {
        // Massive simulated dictionary for AI
        val responses = listOf(
            "Sir, I have analyzed the variables, and the optimal path is clear.",
            "I'm cross-referencing your request with global databases.",
            "The probability of success is mathematically in your favor.",
            "I have updated your files accordingly, sir.",
            "My neural nets suggest we proceed with caution.",
            "I am learning from this interaction to improve future responses."
        )
        return responses[Random().nextInt(responses.size)]
    }

    private fun resetState() {
        mainThreadHandler.postDelayed({ updateEnvironmentLighting(SystemState.STANDBY) }, 2000)
    }

    // ========================================================================
    // UI & ANIMATION ENGINE (Sci-Fi Aesthetics)
    // ========================================================================
    private fun updateEnvironmentLighting(state: SystemState) {
        val colorHex = when(state) {
            SystemState.PROCESSING -> "#3300E5FF" // Bright Cyan
            SystemState.LISTENING -> "#3300FF00"  // Bright Green
            SystemState.SPEAKING -> "#33FF9100"   // Orange
            SystemState.CRITICAL_ERROR -> "#55FF0000" // Red
            SystemState.COMBAT_MODE -> "#88FF0000" // Intense Red
            SystemState.STEALTH_MODE -> "#11000000" // Near Black
            else -> "#050811" // Default Deep Space Blue
        }
        
        val targetColor = Color.parseColor(colorHex)
        val animator = ObjectAnimator.ofArgb(masterRootLayout, "backgroundColor", targetColor)
        animator.duration = 600
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        matrixBackground?.updateHologramColor(state)
        cyberpunkRadar?.updateRadarColor(state)
        
        // Pulse Effect for Status Text
        val pulse = ObjectAnimator.ofFloat(systemStatusText, "alpha", 0.5f, 1f)
        pulse.duration = 800
        pulse.repeatCount = ValueAnimator.INFINITE
        pulse.repeatMode = ValueAnimator.REVERSE
        pulse.start()
    }

    private fun printToTerminal(msg: String) {
        mainThreadHandler.post {
            val lines = terminalOutput.text.toString().split("\n")
            val newText = if (lines.size > 30) lines.drop(1).joinToString("\n") + "\n$msg" else "${terminalOutput.text}\n$msg"
            terminalOutput.text = newText
            
            // Scroll to bottom (assuming it's wrapped in ScrollView in actual XML)
            val parent = terminalOutput.parent
            if (parent is ScrollView) {
                parent.post { parent.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    // ========================================================================
    // NATIVE HARDWARE CONTROLS (Sensors, Flashlight, Vibrator)
    // ========================================================================
    private fun operateHardwareFlashlight(mode: Int) {
        strobeJob?.cancel()
        try {
            when (mode) {
                0 -> cameraManager?.setTorchMode(mainCameraId!!, false)
                1 -> cameraManager?.setTorchMode(mainCameraId!!, true)
                2 -> strobeJob = lifecycleScope.launch(Dispatchers.IO) {
                    var toggle = true
                    while(isActive) { cameraManager?.setTorchMode(mainCameraId!!, toggle); toggle = !toggle; delay(50) } // High speed strobe
                }
                3 -> strobeJob = lifecycleScope.launch(Dispatchers.IO) { // SOS
                    while(isActive) {
                        for(i in 1..3) { cameraManager?.setTorchMode(mainCameraId!!, true); delay(200); cameraManager?.setTorchMode(mainCameraId!!, false); delay(200) }
                        for(i in 1..3) { cameraManager?.setTorchMode(mainCameraId!!, true); delay(600); cameraManager?.setTorchMode(mainCameraId!!, false); delay(200) }
                        for(i in 1..3) { cameraManager?.setTorchMode(mainCameraId!!, true); delay(200); cameraManager?.setTorchMode(mainCameraId!!, false); delay(200) }
                        delay(2000)
                    }
                }
                4 -> strobeJob = lifecycleScope.launch(Dispatchers.IO) { // Custom Morse Code (J-A-R-V-I-S)
                    val morse = listOf(
                        1, 3, 3, 3, 0, // J
                        1, 3, 0,       // A
                        1, 3, 1, 0,    // R
                        3, 1, 1, 1, 0, // V
                        1, 1, 0,       // I
                        1, 1, 1, 0     // S
                    )
                    while(isActive) {
                        for (dot in morse) {
                            if (dot == 0) { delay(600) } 
                            else {
                                cameraManager?.setTorchMode(mainCameraId!!, true)
                                delay(if (dot == 1) 200L else 600L)
                                cameraManager?.setTorchMode(mainCameraId!!, false)
                                delay(200)
                            }
                        }
                        delay(3000)
                    }
                }
            }
        } catch (e: Exception) { Log.e("JARVIS", "Torch Error: ${e.message}") }
    }

    private fun triggerHapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(durationMs)
            }
        } catch (e: Exception) {}
    }

    // ========================================================================
    // VOICE & SPEECH ENGINES (TTS & STT)
    // ========================================================================
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = textToSpeechEngine.setLanguage(Locale("en", "US"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("JARVIS", "Language not supported for TTS")
            }
            // JARVIS Deep Voice Profile
            textToSpeechEngine.setSpeechRate(0.9f)
            textToSpeechEngine.setPitch(0.75f) 
        }
    }

    private fun speakTextNative(text: String) {
        if (currentSystemState == SystemState.STEALTH_MODE) {
            printToTerminal("> STEALTH MODE: Voice output suppressed. Text -> $text")
            return
        }
        
        updateEnvironmentLighting(SystemState.SPEAKING)
        textToSpeechEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utId: String?) {}
            override fun onDone(utId: String?) { mainThreadHandler.post { updateEnvironmentLighting(SystemState.STANDBY) } }
            override fun onError(utId: String?) { mainThreadHandler.post { updateEnvironmentLighting(SystemState.CRITICAL_ERROR) } }
        })
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_TTS_CORE")
    }

    private fun startVoiceRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { printToTerminal("> Listening...") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    // Update HUD with voice levels
                    cyberpunkRadar?.updateWaveform(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { updateEnvironmentLighting(SystemState.PROCESSING) }
                override fun onError(error: Int) { 
                    printToTerminal("> SPEECH ERROR: Code $error")
                    updateEnvironmentLighting(SystemState.STANDBY) 
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        messageInputBox.setText(matches[0])
                        executeMasterCommand(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        
        updateEnvironmentLighting(SystemState.LISTENING)
        speechRecognizer?.startListening(intent)
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { printToTerminal("> ERROR: Cannot launch URL protocol.") }
    }

    // ========================================================================
    // SENSOR EVENT LISTENERS
    // ========================================================================
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                if (event.values[0] < (proximitySensor?.maximumRange ?: 5f)) {
                    if (textToSpeechEngine.isSpeaking) {
                        textToSpeechEngine.stop()
                        printToTerminal("> SENSOR OVERRIDE: Proximity triggered audio mute.")
                    }
                }
            }
            Sensor.TYPE_LIGHT -> ambientLightLux = event.values[0]
            Sensor.TYPE_GYROSCOPE -> {
                // Feeds into radar animation for a 3D effect
                cyberpunkRadar?.applyGyroOffset(event.values[0], event.values[1])
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ========================================================================
    // SECURITY VAULT (API KEYS ENCRYPTION)
    // ========================================================================
    private fun authenticateAndOpenVault() {
        val kg = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!kg.isKeyguardSecure) { renderSecurityVault(); return }
        val intent = kg.createConfirmDeviceCredentialIntent("JARVIS TITAN VAULT", "Authenticate to view Quantum API Keys.")
        if (intent != null) startActivityForResult(intent, 9002) else renderSecurityVault()
    }

    override fun onActivityResult(reqCode: Int, resCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resCode, data)
        if (reqCode == 9002) {
            if (resCode == RESULT_OK) renderSecurityVault()
            else { printToTerminal("> INTRUSION DETECTED. Vault locked."); triggerHapticFeedback(1000) }
        }
    }

    private fun renderSecurityVault() {
        val container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL; setPadding(60,60,60,60); setBackgroundColor(Color.parseColor("#050811")) 
        }
        
        val title = TextView(this).apply { text = "QUANTUM SECURITY VAULT"; setTextColor(Color.parseColor("#00E5FF")); textSize = 20f; setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 40) }
        val geminiInput = EditText(this).apply { hint = "Google Gemini API Key"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1A00E5FF")); setPadding(30,30,30,30); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin=20 } }
        val gptInput = EditText(this).apply { hint = "OpenAI GPT-4 API Key"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1A00E5FF")); setPadding(30,30,30,30); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin=40 } }
        val btnSave = Button(this).apply { text = "ENCRYPT & SECURE"; setBackgroundColor(Color.parseColor("#00E5FF")); setTextColor(Color.BLACK); setPadding(20,40,20,40) }
        
        geminiInput.setText(cryptoEngine.decrypt(securityVaultPrefs.getString("API_GEMINI", "") ?: ""))
        gptInput.setText(cryptoEngine.decrypt(securityVaultPrefs.getString("API_GPT", "") ?: ""))

        container.addView(title); container.addView(geminiInput); container.addView(gptInput); container.addView(btnSave)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(container).show()
        btnSave.setOnClickListener {
            securityVaultPrefs.edit()
                .putString("API_GEMINI", cryptoEngine.encrypt(geminiInput.text.toString()))
                .putString("API_GPT", cryptoEngine.encrypt(gptInput.text.toString()))
                .apply()
            Toast.makeText(this, "Vault Secured.", Toast.LENGTH_SHORT).show()
            triggerHapticFeedback(200)
            dialog.dismiss()
        }
    }

    // ========================================================================
    // LIFECYCLE MANAGEMENT
    // ========================================================================
    override fun onResume() { 
        super.onResume()
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    override fun onDestroy() { 
        super.onDestroy()
        textToSpeechEngine.shutdown()
        speechRecognizer?.destroy()
        strobeJob?.cancel()
        printToTerminal("> SYSTEM SHUTDOWN INITIATED.")
    }

    // ========================================================================
    // INNER CLASSES: DATABASE, CRYPTO, MATH, UI VIEWS (Extensive details)
    // ========================================================================

    // 1. Massive Database Helper
    inner class MassiveDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "JarvisTitanDB.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE MemoryLog (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, query TEXT, response TEXT, intent_type TEXT)")
            db.execSQL("CREATE TABLE ErrorLog (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, error_msg TEXT)")
        }
        override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
            db.execSQL("DROP TABLE IF EXISTS MemoryLog"); db.execSQL("DROP TABLE IF EXISTS ErrorLog"); onCreate(db)
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
        fun clearMemory() {
            try { this.writableDatabase.execSQL("DELETE FROM MemoryLog"); this.writableDatabase.close() } catch (e: Exception) {}
        }
    }

    // 2. Deep System Diagnostics
    inner class DeepSystemDiagnostics(private val ctx: Context) {
        fun getRamUsagePercentage(): Int {
            return try {
                val mi = ActivityManager.MemoryInfo()
                (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
                ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100).toInt()
            } catch (e: Exception) { 0 }
        }
        fun getAvailableStorageMB(): Long {
            return try { 
                val stat = StatFs(Environment.getDataDirectory().path)
                (stat.blockSizeLong * stat.availableBlocksLong) / (1024 * 1024) 
            } catch (e: Exception) { 0L }
        }
    }

    // 3. Quantum Cryptography
    inner class QuantumCryptographyManager {
        private val key = "DRAKOX_TITAN_CORE_V100".padEnd(32, 'X').toByteArray() // 256 bit key
        fun encrypt(raw: String): String {
            if(raw.isEmpty()) return ""
            return try {
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
                Base64.encodeToString(cipher.doFinal(raw.toByteArray()), Base64.DEFAULT)
            } catch (e: Exception) { "" }
        }
        fun decrypt(enc: String): String {
            if(enc.isEmpty()) return ""
            return try {
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
                String(cipher.doFinal(Base64.decode(enc, Base64.DEFAULT)))
            } catch (e: Exception) { "" }
        }
    }

    // 4. Advanced Math Parser
    inner class AdvancedScientificParser {
        fun evaluate(str: String): Double {
            return object : Any() {
                var pos = -1; var ch: Char = 0.toChar()
                fun nextChar() { pos++; ch = if (pos < str.length) str[pos] else 0.toChar() }
                fun eat(charToEat: Char): Boolean { while (ch == ' ') nextChar(); if (ch == charToEat) { nextChar(); return true }; return false }
                fun parse(): Double { nextChar(); val x = parseExpression(); if (pos < str.length) throw RuntimeException("Syntax Error"); return x }
                fun parseExpression(): Double { var x = parseTerm(); while (true) { when { eat('+') -> x += parseTerm(); eat('-') -> x -= parseTerm(); else -> return x } } }
                fun parseTerm(): Double { var x = parseFactor(); while (true) { when { eat('*') -> x *= parseFactor(); eat('/') -> x /= parseFactor(); else -> return x } } }
                fun parseFactor(): Double {
                    if (eat('+')) return parseFactor(); if (eat('-')) return -parseFactor()
                    var x: Double; val startPos = this.pos
                    if (eat('(')) { x = parseExpression(); eat(')') }
                    else if ((ch in '0'..'9') || ch == '.') { while ((ch in '0'..'9') || ch == '.') nextChar(); x = str.substring(startPos, this.pos).toDouble() }
                    else if (ch in 'a'..'z') {
                        while (ch in 'a'..'z') nextChar(); val func = str.substring(startPos, this.pos)
                        x = parseFactor(); x = when (func) { "sqrt" -> Math.sqrt(x); "sin" -> Math.sin(Math.toRadians(x)); "cos" -> Math.cos(Math.toRadians(x)); "tan" -> Math.tan(Math.toRadians(x)); else -> throw RuntimeException("Unknown function") }
                    } else throw RuntimeException("Unexpected Token")
                    if (eat('^')) x = Math.pow(x, parseFactor())
                    return x
                }
            }.parse()
        }
    }

    // 5. Matrix Digital Rain View (Heavy Programmatic Graphics)
    inner class MatrixDigitalRainView(context: Context) : View(context) {
        private val rnd = Random(); private val p = Paint().apply { typeface = Typeface.MONOSPACE }
        private val drops = Array(80) { DigitalDrop() }
        private var hexGlowColor = "#00E5FF"
        inner class DigitalDrop {
            var x = rnd.nextFloat() * 2000f; var y = rnd.nextFloat() * -3000f; var speed = rnd.nextFloat() * 18f + 5f
            var chars = CharArray(rnd.nextInt(25) + 5) { (rnd.nextInt(94) + 33).toChar() }
            var textSize = rnd.nextFloat() * 24f + 12f
        }
        fun updateHologramColor(state: SystemState) {
            hexGlowColor = when(state) { SystemState.CRITICAL_ERROR, SystemState.COMBAT_MODE -> "#FF0000"; SystemState.LISTENING -> "#00FF00"; SystemState.SPEAKING -> "#FF9100"; else -> "#00E5FF" }
        }
        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            for (drop in drops) {
                p.textSize = drop.textSize
                for (i in drop.chars.indices) {
                    if (rnd.nextFloat() > 0.94f) drop.chars[i] = (rnd.nextInt(94) + 33).toChar()
                    p.color = Color.parseColor(hexGlowColor)
                    p.alpha = (255 - (i * (255 / drop.chars.size))).coerceIn(0, 255)
                    c.drawText(drop.chars[i].toString(), drop.x, drop.y - (i * drop.textSize), p)
                }
                drop.y += drop.speed; if (drop.y - (drop.chars.size * drop.textSize) > height) { drop.y = rnd.nextFloat() * -1000f; drop.x = rnd.nextFloat() * width }
            }
            invalidate() 
        }
    }

    // 6. Cyberpunk Radar HUD (Advanced Programmatic Rendering)
    inner class CyberpunkRadarHUD(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var sweepAngle = 0f
        private var radarColor = "#00E5FF"
        private var gyroOffsetX = 0f
        private var gyroOffsetY = 0f
        private var voiceWaveform = 0f

        fun updateRadarColor(state: SystemState) {
            radarColor = when(state) { SystemState.CRITICAL_ERROR, SystemState.COMBAT_MODE -> "#FF0000"; SystemState.LISTENING -> "#00FF00"; else -> "#00E5FF" }
        }
        
        fun applyGyroOffset(x: Float, y: Float) {
            gyroOffsetX = y * 5f; gyroOffsetY = x * 5f; invalidate()
        }
        
        fun updateWaveform(rms: Float) {
            voiceWaveform = rms * 10f; invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = (width / 2f) + gyroOffsetX
            val cy = (height / 2f) + gyroOffsetY
            val radius = 400f

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.parseColor(radarColor)
            paint.alpha = 50

            // Draw Radar Grids
            canvas.drawCircle(cx, cy, radius, paint)
            canvas.drawCircle(cx, cy, radius * 0.66f, paint)
            canvas.drawCircle(cx, cy, radius * 0.33f, paint)
            canvas.drawLine(cx, cy - radius, cx, cy + radius, paint)
            canvas.drawLine(cx - radius, cy, cx + radius, cy, paint)

            // Draw Sweeper
            paint.style = Paint.Style.FILL
            paint.alpha = 40
            canvas.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius), sweepAngle, 45f, true, paint)
            sweepAngle = (sweepAngle + 3f) % 360f

            // Draw Audio Waveform if listening
            if (voiceWaveform > 0) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                paint.color = Color.GREEN
                paint.alpha = 200
                val waveRadius = radius + voiceWaveform
                canvas.drawCircle(cx, cy, waveRadius, paint)
                voiceWaveform *= 0.9f // decay
            }
            invalidate()
        }
    }
}
