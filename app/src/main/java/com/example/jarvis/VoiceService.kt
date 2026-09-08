package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvis.voice.BackgroundVoiceController
import com.example.jarvis.voice.VoiceState

class VoiceService : Service() {

    companion object {

        const val ACTION_START =
            "com.example.jarvis.action.START_VOICE"

        const val ACTION_STOP =
            "com.example.jarvis.action.STOP_VOICE"

        const val ACTION_ENABLE_WAKE =
            "com.example.jarvis.action.ENABLE_WAKE"

        const val ACTION_DISABLE_WAKE =
            "com.example.jarvis.action.DISABLE_WAKE"

        const val ACTION_VOICE_COMMAND =
            "com.example.jarvis.action.VOICE_COMMAND"

        const val EXTRA_COMMAND =
            "com.example.jarvis.extra.COMMAND"

        private const val CHANNEL_ID =
            "jarvis_voice_channel"

        private const val NOTIFICATION_ID = 1001
    }

    private var serviceActive = false
    private var wakeModeEnabled = false

    private lateinit var voiceController:
            BackgroundVoiceController

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        voiceController =
            BackgroundVoiceController(

                context = applicationContext,

                onStateChanged = { state ->
                    handleVoiceState(state)
                },

                onWakeDetected = {
                    handleWakeDetected()
                },

                onCommand = { command ->
                    handleRecognizedCommand(command)
                },

                onError = { error ->
                    handleVoiceError(error)
                }
            )
    }

    // =========================================================
    // SERVICE COMMANDS
    // =========================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                startVoiceService()
            }

            ACTION_ENABLE_WAKE -> {
                enableWakeMode()
            }

            ACTION_DISABLE_WAKE -> {
                disableWakeMode()
            }

            ACTION_STOP -> {
                stopVoiceService()
            }

            ACTION_VOICE_COMMAND -> {

                val command =
                    intent
                        .getStringExtra(
                            EXTRA_COMMAND
                        )
                        ?.trim()
                        .orEmpty()

                if (command.isNotBlank()) {
                    handleRecognizedCommand(
                        command
                    )
                }
            }

            null -> {
                startVoiceService()
            }
        }

        return if (serviceActive) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    // =========================================================
    // START SERVICE
    // =========================================================

    private fun startVoiceService() {

        startVoiceForeground()

        serviceActive = true

        voiceController.start()

        updateNotification(
            if (wakeModeEnabled) {
                "Hey Jarvis wake mode is active"
            } else {
                "JARVIS voice service is active"
            }
        )
    }

    // =========================================================
    // ENABLE WAKE MODE
    // =========================================================

    private fun enableWakeMode() {

        startVoiceForeground()

        serviceActive = true
        wakeModeEnabled = true

        voiceController.start()

        voiceController.enableWakeMode()

        updateNotification(
            "Hey Jarvis wake mode is active"
        )
    }

    // =========================================================
    // DISABLE WAKE MODE
    // =========================================================

    private fun disableWakeMode() {

        wakeModeEnabled = false

        voiceController.disableWakeMode()

        stopVoiceService()
    }

    // =========================================================
    // WAKE DETECTED
    // =========================================================

    private fun handleWakeDetected() {

        if (!serviceActive) {
            return
        }

        if (!wakeModeEnabled) {
            return
        }

        updateNotification(
            "JARVIS is listening..."
        )

        /*
         * The controller now changes to LISTENING
         * and waits for the user's command.
         */
    }

    // =========================================================
    // VOICE COMMAND
    // =========================================================

    private fun handleRecognizedCommand(
        command: String
    ) {

        if (!serviceActive) {
            return
        }

        if (!wakeModeEnabled) {
            return
        }

        val cleanCommand =
            command
                .trim()

        if (cleanCommand.isBlank()) {
            return
        }

        voiceController.setProcessing()

        updateNotification(
            "JARVIS is processing..."
        )

        val commandIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                action =
                    ACTION_VOICE_COMMAND

                putExtra(
                    EXTRA_COMMAND,
                    cleanCommand
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        try {

            startActivity(
                commandIntent
            )

        } catch (_: Exception) {

            /*
             * Keep the foreground service alive
             * even if Android does not allow the
             * Activity launch at that moment.
             */

            voiceController.speechFinished()
        }
    }

    // =========================================================
    // VOICE STATE
    // =========================================================

    private fun handleVoiceState(
        state: VoiceState
    ) {

        if (!serviceActive) {
            return
        }

        val notificationText =
            when (state) {

                VoiceState.IDLE ->
                    "JARVIS voice wake is OFF"

                VoiceState.STANDBY ->
                    "Say Hey Jarvis"

                VoiceState.LISTENING ->
                    "JARVIS is listening..."

                VoiceState.THINKING ->
                    "JARVIS is thinking..."

                VoiceState.EXECUTING ->
                    "JARVIS is executing..."

                VoiceState.SPEAKING ->
                    "JARVIS is speaking..."
            }

        updateNotification(
            notificationText
        )
    }

    // =========================================================
    // VOICE ERROR
    // =========================================================

    private fun handleVoiceError(
        error: String
    ) {

        if (!serviceActive) {
            return
        }

        if (!wakeModeEnabled) {
            return
        }

        updateNotification(
            "JARVIS voice standby"
        )
    }

    // =========================================================
    // FOREGROUND SERVICE
    // =========================================================

    private fun startVoiceForeground() {

        val notification =
            createNotification()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_MICROPHONE
            )

        } else {

            @Suppress("DEPRECATION")
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private fun createNotification():
            Notification {

        val openIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent =
            android.app.PendingIntent.getActivity(
                this,
                0,
                openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    pendingIntentFlags()
            )

        val text =
            if (wakeModeEnabled) {
                "Say Hey Jarvis"
            } else {
                "JARVIS voice service is active"
            }

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "JARVIS Voice Wake"
            )
            .setContentText(
                text
            )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setContentIntent(
                pendingIntent
            )
            .setOngoing(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    private fun updateNotification(
        text: String
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        val openIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent =
            android.app.PendingIntent.getActivity(
                this,
                0,
                openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    pendingIntentFlags()
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "JARVIS Voice Wake"
                )
                .setContentText(
                    text
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setContentIntent(
                    pendingIntent
                )
                .setOngoing(true)
                .setCategory(
                    NotificationCompat.CATEGORY_SERVICE
                )
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    private fun pendingIntentFlags(): Int {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {
            android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    // =========================================================
    // STOP SERVICE
    // =========================================================

    private fun stopVoiceService() {

        serviceActive = false
        wakeModeEnabled = false

        try {
            voiceController.stop()
        } catch (_: Exception) {
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

        } else {

            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "JARVIS Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {

                description =
                    "JARVIS background voice assistant"

                setShowBadge(false)
            }

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    // =========================================================
    // BIND
    // =========================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        serviceActive = false
        wakeModeEnabled = false

        try {
            voiceController.destroy()
        } catch (_: Exception) {
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

        } else {

            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        super.onDestroy()
    }
}