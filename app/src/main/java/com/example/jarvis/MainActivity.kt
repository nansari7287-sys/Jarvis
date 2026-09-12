package com.example.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var prefs: PreferencesManager

    private lateinit var speech: SpeechRecognizerManager
    private lateinit var tts: TextToSpeechManager
    private lateinit var voiceSession: VoiceSessionManager
    private lateinit var voiceOverlay: VoiceOverlayManager

    // UI ELEMENTS
    private lateinit var micButton: ImageButton
    private lateinit var messageInput: EditText
    private lateinit var orbView: JarvisOrbView

    private var backgroundVoiceCommandActive = false

    companion object {

        private const val INSTAGRAM_URL =
            "https://www.instagram.com/drakoxnaeem"

        private const val FACEBOOK_URL =
            "https://www.facebook.com/share/1BsGJAatqh/"

        private const val WEBSITE_URL =
            "https://frexxy-portfolio-3dri.vercel.app/#projects"
    }

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vm =
            ViewModelProvider(this)[MainViewModel::class.java]

        vm.initializeExecutor(this)

        prefs =
            PreferencesManager(this)

        speech =
            SpeechRecognizerManager(this)

        tts =
            TextToSpeechManager(this)

        voiceOverlay =
            VoiceOverlayManager(this)

        // =====================================================
        // VIEWS BINDING
        // =====================================================

        messageInput =
            findViewById(R.id.messageInput)

        val sendButton =
            findViewById<ImageButton>(
                R.id.sendButton
            )

        micButton =
            findViewById(R.id.micButton)

        orbView = 
            findViewById(R.id.jarvisOrbView)

        val recyclerView =
            findViewById<RecyclerView>(
                R.id.messageRecyclerView
            )

        // =====================================================
        // CHAT RECYCLER VIEW SETUP
        // =====================================================

        adapter =
            ChatAdapter()

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter =
            adapter

        // =====================================================
        // MANUAL VOICE SESSION (WITH ORB STATES)
        // =====================================================

        voiceSession =
            VoiceSessionManager(
                context = this,
                speechRecognizer = speech,

                onText = { text ->

                    runOnUiThread {

                        val cleanText =
                            text.trim()

                        if (cleanText.isBlank()) {
                            return@runOnUiThread
                        }

                        messageInput.setText(
                            cleanText
                        )

                        messageInput.setSelection(
                            messageInput.length()
                        )
                        
                        orbView.setOrbState(OrbState.THINKING)

                        processUserCommand(
                            cleanText
                        )
                    }
                },

                onStateChanged = { state ->

                    runOnUiThread {

                        when (state) {

                            VoiceSessionManager.State.IDLE -> {
                                updateMicState(false)
                                voiceOverlay.updateState(OrbState.IDLE)
                                orbView.setOrbState(OrbState.IDLE)
                            }

                            VoiceSessionManager.State.LISTENING -> {
                                updateMicState(true)
                                voiceOverlay.updateState(OrbState.LISTENING)
                                orbView.setOrbState(OrbState.LISTENING)
                            }

                            VoiceSessionManager.State.PROCESSING -> {
                                updateMicState(true)
                                voiceOverlay.updateState(OrbState.THINKING)
                                orbView.setOrbState(OrbState.THINKING)
                            }

                            VoiceSessionManager.State.SPEAKING -> {
                                updateMicState(true)
                                voiceOverlay.updateState(OrbState.SPEAKING)
                                orbView.setOrbState(OrbState.SPEAKING)
                            }
                        }
                    }
                },

                onError = { error ->

                    runOnUiThread {
                        
                        orbView.setOrbState(OrbState.ERROR)

                        if (
                            error.contains(
                                "permission",
                                ignoreCase = true
                            )
                        ) {

                            Toast.makeText(
                                this,
                                error,
                                Toast.LENGTH_SHORT
                            ).show()

                        } else if (
                            !voiceSession.isActive()
                        ) {

                            Toast.makeText(
                                this,
                                error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )

        // =====================================================
        // GEMINI RESPONSE LISTENER
        // =====================================================

        vm.setResponseListener { response ->

            runOnUiThread {

                if (
                    voiceSession.isActive()
                ) {

                    speakInVoiceMode(
                        response
                    )

                    return@runOnUiThread
                }

                if (
                    backgroundVoiceCommandActive
                ) {

                    speakBackgroundResponse(
                        response
                    )

                    return@runOnUiThread
                }

                orbView.setOrbState(OrbState.SPEAKING)
                
                tts.speak(
                    response,
                    onFinished = {
                        runOnUiThread {
                            orbView.setOrbState(OrbState.IDLE)
                        }
                    }
                )
            }
        }

        // =====================================================
        // SEND BUTTON CLICK
        // =====================================================

        sendButton.setOnClickListener {

            val text =
                messageInput.text
                    .toString()
                    .trim()

            if (text.isNotBlank()) {
                
                orbView.setOrbState(OrbState.THINKING)

                processUserCommand(
                    text
                )

                messageInput.text.clear()
            }
        }

        // =====================================================
        // MICROPHONE BUTTON CLICK
        // =====================================================

        micButton.setOnClickListener {

            if (
                !PermissionHelper.hasAudioPermission(
                    this
                )
            ) {

                PermissionHelper.requestAudioPermission(
                    this
                )

                return@setOnClickListener
            }

            if (
                voiceSession.isActive()
            ) {

                stopVoiceMode()

            } else {

                startVoiceMode()
            }
        }

        // =====================================================
        // TOP NAVIGATION BAR / SETTINGS
        // =====================================================

        findViewById<View>(
            R.id.settingsButton
        )?.setOnClickListener {

            showSettings()
        }

        findViewById<View>(
            R.id.menuButton
        )?.setOnClickListener {

            showMenu()
        }

        findViewById<View>(
            R.id.searchButton
        )?.setOnClickListener {

            showSearch()
        }

        findViewById<View>(
            R.id.historyButton
        )?.setOnClickListener {

            Toast.makeText(
                this,
                "Conversation history",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // BOTTOM TABS
        // =====================================================

        findViewById<View>(
            R.id.chatTab
        )?.setOnClickListener {

            Toast.makeText(
                this,
                "JARVIS Chat",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<View>(
            R.id.toolsTab
        )?.setOnClickListener {

            showTools()
        }

        findViewById<View>(
            R.id.assistTab
        )?.setOnClickListener {

            Toast.makeText(
                this,
                "Assist mode activated",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // QUICK ACTIONS CARDS
        // =====================================================

        findViewById<View>(
            R.id.webButton
        )?.setOnClickListener {

            openUrl(
                "https://www.google.com"
            )
        }

        findViewById<View>(
            R.id.youtubeButton
        )?.setOnClickListener {

            openYouTube()
        }

        findViewById<View>(
            R.id.instagramButton
        )?.setOnClickListener {

            openInstagram()
        }

        findViewById<View>(
            R.id.whatsappButton
        )?.setOnClickListener {

            openWhatsApp()
        }

        findViewById<View>(
            R.id.appsButton
        )?.setOnClickListener {

            showApps()
        }

        findViewById<View>(
            R.id.moreButton
        )?.setOnClickListener {

            showMore()
        }

        // =====================================================
        // CHAT STATE OBSERVER
        // =====================================================

        lifecycleScope.launch {

            vm.ui.collect { state ->

                adapter.submitList(
                    state.messages
                )

                if (
                    state.messages.isNotEmpty()
                ) {

                    recyclerView.scrollToPosition(
                        state.messages.lastIndex
                    )
                }
            }
        }

        // =====================================================
        // BACKGROUND VOICE COMMAND INTENT
        // =====================================================

        handleVoiceIntent(
            intent
        )
    }

    // =========================================================
    // RECEIVE VOICE COMMAND NOTIFICATIONS
    // =========================================================

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(intent)

        setIntent(intent)

        handleVoiceIntent(
            intent
        )
    }

    private fun handleVoiceIntent(
        intent: Intent?
    ) {

        if (
            intent?.action !=
            VoiceService.ACTION_VOICE_COMMAND
        ) {
            return
        }

        val command =
            intent
                .getStringExtra(
                    VoiceService.EXTRA_COMMAND
                )
                ?.trim()
                .orEmpty()

        if (
            command.isBlank()
        ) {
            return
        }

        runOnUiThread {

            backgroundVoiceCommandActive =
                true

            messageInput.setText(
                command
            )

            messageInput.setSelection(
                messageInput.length()
            )

            orbView.setOrbState(OrbState.THINKING)
            
            processUserCommand(
                command
            )

            messageInput.text.clear()
        }

        intent.action = null

        intent.removeExtra(
            VoiceService.EXTRA_COMMAND
        )
    }

    // =========================================================
    // START MANUAL VOICE
    // =========================================================

    private fun startVoiceMode() {

        if (
            !PermissionHelper.hasAudioPermission(
                this
            )
        ) {

            PermissionHelper.requestAudioPermission(
                this
            )

            return
        }

        tts.stop()

        backgroundVoiceCommandActive =
            false

        voiceSession.start()

        updateMicState(true)
        
        orbView.setOrbState(OrbState.LISTENING)

        Toast.makeText(
            this,
            "JARVIS voice mode ON",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // STOP MANUAL VOICE
    // =========================================================

    private fun stopVoiceMode() {

        voiceSession.stop()

        speech.stop()

        tts.stop()

        updateMicState(false)

        voiceOverlay.hide()
        
        orbView.setOrbState(OrbState.IDLE)

        Toast.makeText(
            this,
            "JARVIS voice mode OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // MANUAL VOICE RESPONSE (TTS HANDLER)
    // =========================================================

    private fun speakInVoiceMode(
        text: String
    ) {

        if (
            !voiceSession.isActive()
        ) {
            return
        }

        voiceSession.setSpeaking()
        orbView.setOrbState(OrbState.SPEAKING)

        speech.stop()

        tts.speak(
            text,

            onStarted = {

                runOnUiThread {

                    if (
                        voiceSession.isActive()
                    ) {

                        voiceSession.setSpeaking()
                        orbView.setOrbState(OrbState.SPEAKING)
                    }
                }
            },

            onFinished = {

                runOnUiThread {

                    if (
                        voiceSession.isActive()
                    ) {

                        voiceSession.resumeListening()
                        orbView.setOrbState(OrbState.LISTENING)
                    } else {
                        orbView.setOrbState(OrbState.IDLE)
                    }
                }
            }
        )
    }

    // =========================================================
    // BACKGROUND VOICE RESPONSE
    // =========================================================

    private fun speakBackgroundResponse(
        text: String
    ) {

        orbView.setOrbState(OrbState.SPEAKING)

        tts.speak(

            text,

            onStarted = {

                runOnUiThread {
                    // System is speaking, maintain state
                }
            },

            onFinished = {

                runOnUiThread {

                    backgroundVoiceCommandActive =
                        false
                        
                    orbView.setOrbState(OrbState.IDLE)

                    try {

                        startService(
                            Intent(
                                this,
                                VoiceService::class.java
                            ).apply {

                                action =
                                    VoiceService
                                        .ACTION_VOICE_RESPONSE_FINISHED
                            }
                        )

                    } catch (_: Exception) {
                    }
                }
            }
        )
    }

    // =========================================================
    // MIC UI OPACITY CONTROLLER
    // =========================================================

    private fun updateMicState(
        active: Boolean
    ) {

        micButton.alpha =
            if (active) {
                1.0f
            } else {
                0.65f
            }
    }

    // =========================================================
    // COMMAND ROUTER & LOCAL INTENTS
    // =========================================================

    private fun processUserCommand(
        command: String
    ) {

        val normalized =
            command
                .lowercase(
                    Locale.getDefault()
                )
                .replace(
                    "hey jarvis",
                    ""
                )
                .replace(
                    "hey, jarvis",
                    ""
                )
                .replace(
                    "jarvis",
                    ""
                )
                .trim()

        // -----------------------------------------------------
        // WAKE ONLY
        // -----------------------------------------------------

        if (
            normalized.isBlank()
        ) {

            if (
                voiceSession.isActive()
            ) {

                speakInVoiceMode(
                    "Yes, boss. Aaj kya karna hai?"
                )
            } else {
                orbView.setOrbState(OrbState.IDLE)
            }

            return
        }

        // -----------------------------------------------------
        // INSTAGRAM
        // -----------------------------------------------------

        if (
            containsAny(
                normalized,
                "instagram open",
                "instagram kholo",
                "instagram khol",
                "open instagram"
            )
        ) {

            openInstagram()

            speakCommandResult(
                "Instagram open kar diya."
            )

            return
        }

        // -----------------------------------------------------
        // YOUTUBE
        // -----------------------------------------------------

        if (
            containsAny(
                normalized,
                "youtube open",
                "youtube kholo",
                "youtube khol",
                "open youtube"
            )
        ) {

            openYouTube()

            speakCommandResult(
                "YouTube open kar diya."
            )

            return
        }

        // -----------------------------------------------------
        // WHATSAPP
        // -----------------------------------------------------

        if (
            containsAny(
                normalized,
                "whatsapp open",
                "whatsapp kholo",
                "whatsapp khol",
                "open whatsapp"
            )
        ) {

            openWhatsApp()

            speakCommandResult(
                "WhatsApp open kar diya."
            )

            return
        }

        // -----------------------------------------------------
        // GOOGLE
        // -----------------------------------------------------

        if (
            containsAny(
                normalized,
                "google open",
                "google kholo",
                "google khol",
                "open google"
            )
        ) {

            openUrl(
                "https://www.google.com"
            )

            speakCommandResult(
                "Google open kar diya."
            )

            return
        }

        // -----------------------------------------------------
        // GEMINI AI CLOUD
        // -----------------------------------------------------

        if (
            voiceSession.isActive()
        ) {

            voiceSession.setProcessing()
        }

        vm.send(
            normalized,
            prefs.getApiKey()
        )
    }

    // =========================================================
    // COMMAND RESULT TTS DELEGATOR
    // =========================================================

    private fun speakCommandResult(
        text: String
    ) {

        if (
            voiceSession.isActive()
        ) {

            speakInVoiceMode(
                text
            )

            return
        }

        if (
            backgroundVoiceCommandActive
        ) {

            speakBackgroundResponse(
                text
            )

            return
        }
        
        orbView.setOrbState(OrbState.SPEAKING)

        tts.speak(
            text,
            onFinished = {
                runOnUiThread {
                    orbView.setOrbState(OrbState.IDLE)
                }
            }
        )
    }

    // =========================================================
    // PHRASE CHECK
    // =========================================================

    private fun containsAny(
        text: String,
        vararg phrases: String
    ): Boolean {

        return phrases.any {
            text.contains(it)
        }
    }

    // =========================================================
    // SETTINGS DIALOG
    // =========================================================

    private fun showSettings() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    35,
                    10,
                    35,
                    5
                )
            }

        // -----------------------------------------------------
        // GEMINI API KEY
        // -----------------------------------------------------

        val apiInput =
            EditText(this).apply {

                hint =
                    "Paste Gemini API key"

                setSingleLine(true)

                setText(
                    prefs.getApiKey()
                )
            }

        container.addView(
            apiInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // -----------------------------------------------------
        // VOICE WAKE
        // -----------------------------------------------------

        val wakeSwitch =
            Switch(this).apply {

                text =
                    "Voice Wake Mode"

                textSize =
                    15f

                isChecked =
                    prefs.isVoiceWakeEnabled()

                setPadding(
                    0,
                    25,
                    0,
                    10
                )
            }

        container.addView(
            wakeSwitch,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val wakeDescription =
            android.widget.TextView(this).apply {

                text =
                    "ON karne par JARVIS background voice " +
                    "wake mode ke liye ready rahega."

                textSize =
                    12f

                alpha =
                    0.7f
            }

        container.addView(
            wakeDescription,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        AlertDialog.Builder(this)

            .setTitle(
                "⚙ JARVIS SETTINGS"
            )

            .setMessage(
                "Configure Gemini and Voice Wake Mode"
            )

            .setView(
                container
            )

            .setPositiveButton(
                "SAVE"
            ) { _, _ ->

                prefs.saveApiKey(
                    apiInput.text
                        .toString()
                        .trim()
                )

                val wakeEnabled =
                    wakeSwitch.isChecked

                prefs.setVoiceWakeEnabled(
                    wakeEnabled
                )

                if (
                    wakeEnabled
                ) {

                    startBackgroundVoiceService()

                } else {

                    stopBackgroundVoiceService()
                }

                Toast.makeText(
                    this,
                    if (wakeEnabled) {
                        "Settings saved • Voice Wake ON"
                    } else {
                        "Settings saved • Voice Wake OFF"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }

            .setNegativeButton(
                "CANCEL",
                null
            )

            .show()
    }

    // =========================================================
    // START BACKGROUND WAKE SERVICE
    // =========================================================

    private fun startBackgroundVoiceService() {

        if (
            !PermissionHelper.hasAudioPermission(
                this
            )
        ) {

            PermissionHelper.requestAudioPermission(
                this
            )

            return
        }

        val intent =
            Intent(
                this,
                VoiceService::class.java
            ).apply {

                action =
                    VoiceService.ACTION_ENABLE_WAKE
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                ContextCompat.startForegroundService(
                    this,
                    intent
                )

            } else {

                startService(
                    intent
                )
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Voice Wake service start nahi ho saka.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // STOP BACKGROUND WAKE SERVICE
    // =========================================================

    private fun stopBackgroundVoiceService() {

        try {

            stopService(
                Intent(
                    this,
                    VoiceService::class.java
                )
            )

        } catch (_: Exception) {
        }

        backgroundVoiceCommandActive =
            false
    }

    // =========================================================
    // SEARCH DIALOG
    // =========================================================

    private fun showSearch() {

        val input =
            EditText(this).apply {

                hint =
                    "Search the web"

                setSingleLine(true)
            }

        AlertDialog.Builder(this)

            .setTitle(
                "⌕ JARVIS SEARCH"
            )

            .setView(
                input
            )

            .setPositiveButton(
                "SEARCH"
            ) { _, _ ->

                val query =
                    input.text
                        .toString()
                        .trim()

                if (
                    query.isNotBlank()
                ) {

                    openUrl(
                        "https://www.google.com/search?q=" +
                            Uri.encode(query)
                    )
                }
            }

            .setNegativeButton(
                "CANCEL",
                null
            )

            .show()
    }

    // =========================================================
    // MENU DIALOG
    // =========================================================

    private fun showMenu() {

        val items =
            arrayOf(
                "⚙ Settings",
                "🛠 Tools",
                "◉ Assist",
                "▣ Apps",
                "◆ About JARVIS",
                "✦ Developer"
            )

        AlertDialog.Builder(this)

            .setTitle(
                "✦ JARVIS MENU"
            )

            .setItems(
                items
            ) { _, which ->

                when (which) {

                    0 ->
                        showSettings()

                    1 ->
                        showTools()

                    2 ->
                        Toast.makeText(
                            this,
                            "Assist mode activated",
                            Toast.LENGTH_SHORT
                        ).show()

                    3 ->
                        showApps()

                    4 ->
                        showAbout()

                    5 ->
                        showDeveloper()
                }
            }

            .show()
    }

    // =========================================================
    // TOOLS DIALOG
    // =========================================================

    private fun showTools() {

        AlertDialog.Builder(this)

            .setTitle(
                "✦ JARVIS TOOLS"
            )

            .setItems(
                arrayOf(
                    "🌐 Web Search",
                    "▶ YouTube",
                    "◎ Instagram",
                    "◈ WhatsApp"
                )
            ) { _, which ->

                when (which) {

                    0 ->
                        openUrl(
                            "https://www.google.com"
                        )

                    1 ->
                        openYouTube()

                    2 ->
                        openInstagram()

                    3 ->
                        openWhatsApp()
                }
            }

            .show()
    }

    // =========================================================
    // APPS DIALOG
    // =========================================================

    private fun showApps() {

        AlertDialog.Builder(this)

            .setTitle(
                "✦ JARVIS APPS"
            )

            .setItems(
                arrayOf(
                    "◎ Instagram",
                    "▶ YouTube",
                    "◈ WhatsApp",
                    "G Google"
                )
            ) { _, which ->

                when (which) {

                    0 ->
                        openInstagram()

                    1 ->
                        openYouTube()

                    2 ->
                        openWhatsApp()

                    3 ->
                        openUrl(
                            "https://www.google.com"
                        )
                }
            }

            .show()
    }

    // =========================================================
    // MORE MENU
    // =========================================================

    private fun showMore() {

        AlertDialog.Builder(this)

            .setTitle(
                "✦ MORE"
            )

            .setItems(
                arrayOf(
                    "⚙ Settings",
                    "◷ History",
                    "◆ About JARVIS",
                    "✦ Developer"
                )
            ) { _, which ->

                when (which) {

                    0 ->
                        showSettings()

                    1 ->
                        Toast.makeText(
                            this,
                            "Conversation History",
                            Toast.LENGTH_SHORT
                        ).show()

                    2 ->
                        showAbout()

                    3 ->
                        showDeveloper()
                }
            }

            .show()
    }

    // =========================================================
    // ABOUT
    // =========================================================

    private fun showAbout() {

        AlertDialog.Builder(this)

            .setTitle(
                "◆ JARVIS"
            )

            .setMessage(
                "𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎\n\n" +
                    "Personal AI Assistant\n\n" +
                    "Voice • AI • Tools • Automation\n\n" +
                    "Developed By 𝑵𝒂𝒆𝒎"
            )

            .setPositiveButton(
                "CLOSE",
                null
            )

            .show()
    }

    // =========================================================
    // DEVELOPER INFO
    // =========================================================

    private fun showDeveloper() {

        val items =
            arrayOf(
                "◎ Instagram",
                "f Facebook",
                "⌂ Website"
            )

        AlertDialog.Builder(this)

            .setTitle(
                "✦ DEVELOPER"
            )

            .setMessage(
                "Developed By 𝑵𝒂𝒆𝒎\n\n" +
                    "𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎"
            )

            .setItems(
                items
            ) { _, which ->

                when (which) {

                    0 ->
                        openInstagram()

                    1 ->
                        openUrl(
                            FACEBOOK_URL
                        )

                    2 ->
                        openUrl(
                            WEBSITE_URL
                        )
                }
            }

            .setNegativeButton(
                "CLOSE",
                null
            )

            .show()
    }

    // =========================================================
    // QUICK APP INTENTS
    // =========================================================

    private fun openInstagram() {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "instagram://user?username=drakoxnaeem"
                    )
                )

            startActivity(
                intent
            )

        } catch (_: Exception) {

            openUrl(
                INSTAGRAM_URL
            )
        }
    }

    private fun openYouTube() {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "vnd.youtube:"
                    )
                )

            startActivity(
                intent
            )

        } catch (_: Exception) {

            openUrl(
                "https://www.youtube.com"
            )
        }
    }

    private fun openWhatsApp() {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "whatsapp://"
                    )
                )

            startActivity(
                intent
            )

        } catch (_: Exception) {

            openUrl(
                "https://web.whatsapp.com"
            )
        }
    }

    private fun openUrl(
        url: String
    ) {

        if (
            url.isBlank()
        ) {
            return
        }

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Unable to open link",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // LIFECYCLE RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (
            ::prefs.isInitialized &&
            prefs.isVoiceWakeEnabled()
        ) {

            if (
                PermissionHelper.hasAudioPermission(
                    this
                )
            ) {

                startBackgroundVoiceService()
            }
        }
    }

    // =========================================================
    // LIFECYCLE DESTROY
    // =========================================================

    override fun onDestroy() {

        try {
            voiceSession.destroy()
        } catch (_: Exception) {
        }

        try {
            speech.destroy()
        } catch (_: Exception) {
        }

        try {
            tts.shutdown()
        } catch (_: Exception) {
        }

        try {
            voiceOverlay.destroy()
        } catch (_: Exception) {
        }

        vm.setResponseListener(
            null
        )

        super.onDestroy()
    }
}
