package com.example.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jarvis.data.PreferencesManager
import com.example.jarvis.ui.ChatAdapter
import com.example.jarvis.ui.MainViewModel
import com.example.jarvis.utils.PermissionHelper
import com.example.jarvis.voice.SpeechRecognizerManager
import com.example.jarvis.voice.TextToSpeechManager
import com.example.jarvis.voice.VoiceSessionManager
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var prefs: PreferencesManager

    private lateinit var speech: SpeechRecognizerManager
    private lateinit var tts: TextToSpeechManager
    private lateinit var voiceSession: VoiceSessionManager

    private lateinit var micButton: ImageButton
    private lateinit var messageInput: EditText

    companion object {

        private const val INSTAGRAM_URL =
            "https://www.instagram.com/drakoxnaeem"

        private const val FACEBOOK_URL =
            "https://www.facebook.com/share/1BsGJAatqh/"

        private const val WEBSITE_URL =
            "https://frexxy-portfolio-3dri.vercel.app/#projects"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vm = ViewModelProvider(this)[MainViewModel::class.java]

        // CommandExecutor ko Activity lifecycle se independent
        // application context ke saath initialize karte hain.
        vm.initializeExecutor(this)

        prefs = PreferencesManager(this)

        speech = SpeechRecognizerManager(this)
        tts = TextToSpeechManager(this)

        messageInput = findViewById(R.id.messageInput)

        val sendButton = findViewById<ImageButton>(
            R.id.sendButton
        )

        micButton = findViewById(
            R.id.micButton
        )

        val recyclerView = findViewById<RecyclerView>(
            R.id.messageRecyclerView
        )

        adapter = ChatAdapter()

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter = adapter

        // =====================================================
        // VOICE SESSION
        // =====================================================

        voiceSession = VoiceSessionManager(
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
                        }

                        VoiceSessionManager.State.LISTENING -> {
                            updateMicState(true)
                        }

                        VoiceSessionManager.State.PROCESSING -> {
                            updateMicState(true)
                        }

                        VoiceSessionManager.State.SPEAKING -> {
                            updateMicState(true)
                        }
                    }
                }
            },

            onError = { error ->

                runOnUiThread {

                    /*
                     * Continuous mode me temporary speech errors
                     * ke liye Toast spam nahi karna.
                     * VoiceSessionManager khud listening resume karta hai.
                     */
                    if (voiceSession.isActive()) {

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
                        }

                    } else {

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
        // GEMINI RESPONSE → TTS
        // =====================================================

        vm.setResponseListener { response ->

            runOnUiThread {

                if (voiceSession.isActive()) {

                    speakInVoiceMode(
                        response
                    )

                } else {

                    // Normal text/chat mode me bhi JARVIS response
                    // voice me suna sakta hai.
                    tts.speak(response)
                }
            }
        }

        // =====================================================
        // SEND BUTTON
        // =====================================================

        sendButton.setOnClickListener {

            val text =
                messageInput.text
                    .toString()
                    .trim()

            if (text.isNotBlank()) {

                processUserCommand(text)

                messageInput.text.clear()
            }
        }

        // =====================================================
        // MICROPHONE
        //
        // ONE TAP  = ON
        // NEXT TAP = OFF
        // =====================================================

        micButton.setOnClickListener {

            if (!PermissionHelper.hasAudioPermission(this)) {

                PermissionHelper.requestAudioPermission(
                    this
                )

                return@setOnClickListener
            }

            if (voiceSession.isActive()) {

                stopVoiceMode()

            } else {

                startVoiceMode()
            }
        }

        // =====================================================
        // SETTINGS
        // =====================================================

        findViewById<View>(
            R.id.settingsButton
        ).setOnClickListener {

            showSettings()
        }

        // =====================================================
        // MENU
        // =====================================================

        findViewById<View>(
            R.id.menuButton
        ).setOnClickListener {

            showMenu()
        }

        // =====================================================
        // SEARCH
        // =====================================================

        findViewById<View>(
            R.id.searchButton
        ).setOnClickListener {

            showSearch()
        }

        // =====================================================
        // HISTORY
        // =====================================================

        findViewById<View>(
            R.id.historyButton
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Conversation history",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // TABS
        // =====================================================

        findViewById<View>(
            R.id.chatTab
        ).setOnClickListener {

            Toast.makeText(
                this,
                "JARVIS Chat",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<View>(
            R.id.toolsTab
        ).setOnClickListener {

            showTools()
        }

        findViewById<View>(
            R.id.assistTab
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Assist mode activated",
                Toast.LENGTH_SHORT
            ).show()
        }

        // =====================================================
        // QUICK ACTIONS
        // =====================================================

        findViewById<View>(
            R.id.webButton
        ).setOnClickListener {

            openUrl(
                "https://www.google.com"
            )
        }

        findViewById<View>(
            R.id.youtubeButton
        ).setOnClickListener {

            openYouTube()
        }

        findViewById<View>(
            R.id.instagramButton
        ).setOnClickListener {

            openInstagram()
        }

        findViewById<View>(
            R.id.whatsappButton
        ).setOnClickListener {

            openWhatsApp()
        }

        findViewById<View>(
            R.id.appsButton
        ).setOnClickListener {

            showApps()
        }

        findViewById<View>(
            R.id.moreButton
        ).setOnClickListener {

            showMore()
        }

        // =====================================================
        // UI STATE
        // =====================================================

        lifecycleScope.launch {

            vm.ui.collect { state ->

                adapter.submitList(
                    state.messages
                )

                if (state.messages.isNotEmpty()) {

                    recyclerView.scrollToPosition(
                        state.messages.lastIndex
                    )
                }
            }
        }
    }

    // =========================================================
    // START VOICE MODE
    // =========================================================

    private fun startVoiceMode() {

        if (!PermissionHelper.hasAudioPermission(this)) {

            PermissionHelper.requestAudioPermission(
                this
            )

            return
        }

        /*
         * Previous TTS ko stop karke fresh voice session start.
         */
        tts.stop()

        voiceSession.start()

        updateMicState(true)

        Toast.makeText(
            this,
            "JARVIS voice mode ON",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // STOP VOICE MODE
    // =========================================================

    private fun stopVoiceMode() {

        voiceSession.stop()

        speech.stop()

        tts.stop()

        updateMicState(false)

        Toast.makeText(
            this,
            "JARVIS voice mode OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // SPEAK IN VOICE MODE
    // =========================================================

    private fun speakInVoiceMode(
        text: String
    ) {

        if (!voiceSession.isActive()) {
            return
        }

        voiceSession.setSpeaking()

        /*
         * Recognition ko TTS ke dauran active nahi rehna chahiye.
         */
        speech.stop()

        tts.speak(
            text,

            onStarted = {

                runOnUiThread {

                    if (
                        voiceSession.isActive()
                    ) {
                        voiceSession.setSpeaking()
                    }
                }
            },

            onFinished = {

                runOnUiThread {

                    if (
                        voiceSession.isActive()
                    ) {
                        /*
                         * JARVIS bolne ke baad automatically
                         * next user input ke liye listen karega.
                         */
                        voiceSession.resumeListening()
                    }
                }
            }
        )
    }

    // =========================================================
    // MIC UI
    // =========================================================

    private fun updateMicState(
        active: Boolean
    ) {

        if (active) {

            micButton.alpha = 1.0f

        } else {

            micButton.alpha = 0.65f
        }
    }

    // =========================================================
    // JARVIS COMMAND ROUTER
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
        // Only wake phrase / "Jarvis"
        // -----------------------------------------------------

        if (normalized.isBlank()) {

            if (voiceSession.isActive()) {

                speakInVoiceMode(
                    "Yes, boss. Aaj kya karna hai?"
                )
            }

            return
        }

        // =====================================================
        // FAST PATH
        // =====================================================

        when {

            containsAny(
                normalized,
                "instagram open",
                "instagram kholo",
                "instagram khol",
                "open instagram"
            ) -> {

                openInstagram()

                speakCommandResult(
                    "Instagram open kar diya."
                )

                return
            }

            containsAny(
                normalized,
                "youtube open",
                "youtube kholo",
                "youtube khol",
                "open youtube"
            ) -> {

                openYouTube()

                speakCommandResult(
                    "YouTube open kar diya."
                )

                return
            }

            containsAny(
                normalized,
                "whatsapp open",
                "whatsapp kholo",
                "whatsapp khol",
                "open whatsapp"
            ) -> {

                openWhatsApp()

                speakCommandResult(
                    "WhatsApp open kar diya."
                )

                return
            }

            containsAny(
                normalized,
                "google open",
                "google kholo",
                "google khol",
                "open google"
            ) -> {

                openUrl(
                    "https://www.google.com"
                )

                speakCommandResult(
                    "Google open kar diya."
                )

                return
            }
        }

        // =====================================================
        // COMPLEX COMMAND → GEMINI
        // =====================================================

        if (voiceSession.isActive()) {

            voiceSession.setProcessing()
        }

        vm.send(
            command,
            prefs.getApiKey()
        )
    }

    // =========================================================
    // LOCAL COMMAND RESULT → TTS
    // =========================================================

    private fun speakCommandResult(
        text: String
    ) {

        if (!voiceSession.isActive()) {

            tts.speak(text)

            return
        }

        speakInVoiceMode(
            text
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
    // SETTINGS
    // =========================================================

    private fun showSettings() {

        val input = EditText(this)

        input.hint =
            "Paste Gemini API key"

        input.setSingleLine(true)

        input.setText(
            prefs.getApiKey()
        )

        val container =
            android.widget.FrameLayout(this)

        container.setPadding(
            35,
            10,
            35,
            0
        )

        container.addView(input)

        AlertDialog.Builder(this)

            .setTitle(
                "⚙ JARVIS SETTINGS"
            )

            .setMessage(
                "Configure your Gemini API key"
            )

            .setView(container)

            .setPositiveButton(
                "SAVE"
            ) { _, _ ->

                prefs.saveApiKey(
                    input.text
                        .toString()
                        .trim()
                )

                Toast.makeText(
                    this,
                    "API key saved securely",
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
    // SEARCH
    // =========================================================

    private fun showSearch() {

        val input = EditText(this)

        input.hint =
            "Search the web"

        input.setSingleLine(true)

        AlertDialog.Builder(this)

            .setTitle(
                "⌕ JARVIS SEARCH"
            )

            .setView(input)

            .setPositiveButton(
                "SEARCH"
            ) { _, _ ->

                val query =
                    input.text
                        .toString()
                        .trim()

                if (query.isNotBlank()) {

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
    // MENU
    // =========================================================

    private fun showMenu() {

        val items = arrayOf(
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

                    0 -> showSettings()

                    1 -> showTools()

                    2 -> Toast.makeText(
                        this,
                        "Assist mode activated",
                        Toast.LENGTH_SHORT
                    ).show()

                    3 -> showApps()

                    4 -> showAbout()

                    5 -> showDeveloper()
                }
            }

            .show()
    }

    // =========================================================
    // TOOLS
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

                    0 -> openUrl(
                        "https://www.google.com"
                    )

                    1 -> openYouTube()

                    2 -> openInstagram()

                    3 -> openWhatsApp()
                }
            }

            .show()
    }

    // =========================================================
    // APPS
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

                    0 -> openInstagram()

                    1 -> openYouTube()

                    2 -> openWhatsApp()

                    3 -> openUrl(
                        "https://www.google.com"
                    )
                }
            }

            .show()
    }

    // =========================================================
    // MORE
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

                    0 -> showSettings()

                    1 -> Toast.makeText(
                        this,
                        "Conversation History",
                        Toast.LENGTH_SHORT
                    ).show()

                    2 -> showAbout()

                    3 -> showDeveloper()
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
                    "Developed By 𝑵𝒂𝒆𝒆𝒎"
            )

            .setPositiveButton(
                "CLOSE",
                null
            )

            .show()
    }

    // =========================================================
    // DEVELOPER
    // =========================================================

    private fun showDeveloper() {

        val items = arrayOf(
            "◎ Instagram",
            "f Facebook",
            "⌂ Website"
        )

        AlertDialog.Builder(this)

            .setTitle(
                "✦ DEVELOPER"
            )

            .setMessage(
                "Developed By 𝑵𝒂𝒆𝒆𝒎\n\n" +
                    "𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎"
            )

            .setItems(
                items
            ) { _, which ->

                when (which) {

                    0 -> openInstagram()

                    1 -> openUrl(
                        FACEBOOK_URL
                    )

                    2 -> openUrl(
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
    // INSTAGRAM
    // =========================================================

    private fun openInstagram() {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "instagram://user?username=drakoxnaeem"
                )
            )

            startActivity(intent)

        } catch (_: Exception) {

            openUrl(
                INSTAGRAM_URL
            )
        }
    }

    // =========================================================
    // YOUTUBE
    // =========================================================

    private fun openYouTube() {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "vnd.youtube:"
                )
            )

            startActivity(intent)

        } catch (_: Exception) {

            openUrl(
                "https://www.youtube.com"
            )
        }
    }

    // =========================================================
    // WHATSAPP
    // =========================================================

    private fun openWhatsApp() {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "whatsapp://"
                )
            )

            startActivity(intent)

        } catch (_: Exception) {

            openUrl(
                "https://web.whatsapp.com"
            )
        }
    }

    // =========================================================
    // OPEN URL
    // =========================================================

    private fun openUrl(
        url: String
    ) {

        if (url.isBlank()) {
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
    // DESTROY
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

        vm.setResponseListener(null)

        super.onDestroy()
    }
}