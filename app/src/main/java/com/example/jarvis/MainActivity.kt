package com.example.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jarvis.data.PreferencesManager
import com.example.jarvis.ui.ChatAdapter
import com.example.jarvis.ui.MainViewModel
import com.example.jarvis.utils.PermissionHelper
import com.example.jarvis.voice.SpeechRecognizerManager
import com.example.jarvis.voice.TextToSpeechManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var prefs: PreferencesManager
    private lateinit var speech: SpeechRecognizerManager
    private lateinit var tts: TextToSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vm = ViewModelProvider(this)[MainViewModel::class.java]

        prefs = PreferencesManager(this)

        speech = SpeechRecognizerManager(this)

        tts = TextToSpeechManager(this)

        val input = findViewById<EditText>(R.id.messageInput)

        val send = findViewById<ImageButton>(R.id.sendButton)

        val mic = findViewById<ImageButton>(R.id.micButton)

        val rv =
            findViewById<androidx.recyclerview.widget.RecyclerView>(
                R.id.messageRecyclerView
            )

        adapter = ChatAdapter()

        rv.layoutManager = LinearLayoutManager(this)

        rv.adapter = adapter


        // SEND
        send.setOnClickListener {

            val text = input.text.toString().trim()

            if (text.isNotBlank()) {

                vm.send(
                    text,
                    prefs.getApiKey()
                )

                input.text.clear()
            }
        }


        // MICROPHONE
        mic.setOnClickListener {

            if (!PermissionHelper.hasAudioPermission(this)) {

                PermissionHelper.requestAudioPermission(this)

                return@setOnClickListener
            }

            speech.start(
                { text ->

                    input.setText(text)

                    input.setSelection(input.length())

                },
                { error ->

                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }


        // SETTINGS
        findViewById<TextView>(
            R.id.settingsButton
        ).setOnClickListener {

            showSettings()
        }


        // MENU
        findViewById<TextView>(
            R.id.menuButton
        ).setOnClickListener {

            showMenu()
        }


        // SEARCH
        findViewById<TextView>(
            R.id.searchButton
        ).setOnClickListener {

            Toast.makeText(
                this,
                "JARVIS search ready",
                Toast.LENGTH_SHORT
            ).show()
        }


        // HISTORY
        findViewById<TextView>(
            R.id.historyButton
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Conversation history",
                Toast.LENGTH_SHORT
            ).show()
        }


        // TABS
        findViewById<TextView>(
            R.id.chatTab
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Chat",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<TextView>(
            R.id.toolsTab
        ).setOnClickListener {

            showTools()
        }

        findViewById<TextView>(
            R.id.assistTab
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Assist mode activated",
                Toast.LENGTH_SHORT
            ).show()
        }


        // QUICK ACTIONS

        findViewById<TextView>(
            R.id.webButton
        ).setOnClickListener {

            openUrl("https://www.google.com")
        }

        findViewById<TextView>(
            R.id.youtubeButton
        ).setOnClickListener {

            openUrl("https://www.youtube.com")
        }

        findViewById<TextView>(
            R.id.instagramButton
        ).setOnClickListener {

            openUrl("https://www.instagram.com")
        }

        findViewById<TextView>(
            R.id.whatsappButton
        ).setOnClickListener {

            openUrl("https://web.whatsapp.com")
        }

        findViewById<TextView>(
            R.id.appsButton
        ).setOnClickListener {

            showApps()
        }

        findViewById<TextView>(
            R.id.moreButton
        ).setOnClickListener {

            showMore()
        }


        // AI UI UPDATE
        lifecycleScope.launch {

            vm.ui.collect { state ->

                adapter.submitList(state.messages)

                if (state.messages.isNotEmpty()) {

                    rv.scrollToPosition(
                        state.messages.lastIndex
                    )
                }
            }
        }
    }


    private fun showSettings() {

        val input = EditText(this)

        input.hint = "Paste Gemini API key"

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

            .setTitle("JARVIS Settings")

            .setMessage(
                "Configure your Gemini API key"
            )

            .setView(container)

            .setPositiveButton(
                "SAVE"
            ) { _, _ ->

                prefs.saveApiKey(
                    input.text.toString().trim()
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


    private fun showMenu() {

        val items = arrayOf(
            "Settings",
            "Tools",
            "Assist",
            "Apps",
            "About JARVIS"
        )

        AlertDialog.Builder(this)

            .setTitle("JARVIS MENU")

            .setItems(items) { _, which ->

                when (which) {

                    0 -> showSettings()

                    1 -> showTools()

                    2 ->
                        Toast.makeText(
                            this,
                            "Assist mode",
                            Toast.LENGTH_SHORT
                        ).show()

                    3 -> showApps()

                    4 ->
                        Toast.makeText(
                            this,
                            "DrakoXNaeem • JARVIS AI Assistant",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }

            .show()
    }


    private fun showTools() {

        AlertDialog.Builder(this)

            .setTitle("JARVIS TOOLS")

            .setItems(
                arrayOf(
                    "Web Search",
                    "YouTube",
                    "Instagram",
                    "WhatsApp"
                )
            ) { _, which ->

                when (which) {

                    0 -> openUrl(
                        "https://www.google.com"
                    )

                    1 -> openUrl(
                        "https://www.youtube.com"
                    )

                    2 -> openUrl(
                        "https://www.instagram.com"
                    )

                    3 -> openUrl(
                        "https://web.whatsapp.com"
                    )
                }
            }

            .show()
    }


    private fun showApps() {

        AlertDialog.Builder(this)

            .setTitle("JARVIS APPS")

            .setItems(
                arrayOf(
                    "Instagram",
                    "YouTube",
                    "WhatsApp",
                    "Google"
                )
            ) { _, which ->

                when (which) {

                    0 -> openUrl(
                        "https://www.instagram.com"
                    )

                    1 -> openUrl(
                        "https://www.youtube.com"
                    )

                    2 -> openUrl(
                        "https://web.whatsapp.com"
                    )

                    3 -> openUrl(
                        "https://www.google.com"
                    )
                }
            }

            .show()
    }


    private fun showMore() {

        AlertDialog.Builder(this)

            .setTitle("MORE")

            .setItems(
                arrayOf(
                    "Settings",
                    "History",
                    "About JARVIS"
                )
            ) { _, which ->

                when (which) {

                    0 -> showSettings()

                    1 ->
                        Toast.makeText(
                            this,
                            "History",
                            Toast.LENGTH_SHORT
                        ).show()

                    2 ->
                        Toast.makeText(
                            this,
                            "DrakoXNaeem JARVIS",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }

            .show()
    }


    private fun openUrl(url: String) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onDestroy() {

        speech.destroy()

        tts.shutdown()

        super.onDestroy()
    }
}