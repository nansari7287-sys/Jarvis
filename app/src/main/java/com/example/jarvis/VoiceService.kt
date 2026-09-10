package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvis.voice.AudioCaptureManager
import com.example.jarvis.voice.BackgroundVoiceController
import com.example.jarvis.voice.VoiceState
import com.example.jarvis.voice.WakeWordEngine

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

        private const val NOTIFICATION_ID =
            1001
    }

    private var serviceActive = false
    private var wakeModeEnabled = false

    private lateinit var voiceController:
            BackgroundVoiceController

    private lateinit var audioCapture:
            AudioCaptureManager

    private lateinit var wakeWordEngine:
            WakeWordEngine

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        /*
         * Background voice controller.
         */
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

        /*
         * Real microphone capture.
         *
         * IMPORTANT:
         * This does NOT start automatically.
         * It starts only when Wake Mode is enabled.
         */
        audioCapture =
            AudioCaptureManager(

                context = applicationContext,

                onAudioFrame = { audioFrame ->

                    if (
                        serviceActive &&
                        wakeModeEnabled
                    ) {

                        wakeWordEngine.processAudio(
                            audioFrame
                        )
                    }
                },

                onError = { error ->

                    handleVoiceError(
                        error
                    )
                }
            )

        /*
         * Real Hey Jarvis classifier.
         */
        wakeWordEngine =
            WakeWordEngine(

                context = applicationContext,

                onWakeWordDetected = {

                    handleWakeWordDetected()
                },

                onError = { error ->

                    handleVoiceError(
                        error
                    )
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

                if (
                    command.isNotBlank()
                ) {

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
    // START
    // =========================================================

    private fun startVoiceService() {

        startVoiceForeground()

        serviceActive = true

        voiceController.start()

        if (wakeModeEnabled) {
            startWakeWordListening()
        }

        updateNotification(
            if (wakeModeEnabled) {
                "Say Hey Jarvis"
            } else {
                "JARVIS voice service is active"
            }
        )
    }

    // =========================================================
    // ENABLE WAKE MODE
    // =========================================================

    private fun enableWakeMode() {

        /*
         * Android requires microphone foreground
         * service handling for background microphone use.
         */
        startVoiceForeground()

        serviceActive = true
        wakeModeEnabled = true

        voiceController.start()

        /*
         * IMPORTANT:
         *
         * No Android SpeechRecognizer is started here.
         *
         * Only our on-device wake-word pipeline listens.
         */
        startWakeWordListening()

        updateNotification(
            "Say Hey Jarvis"
        )
    }

    // =========================================================
    // REAL WAKE LISTENER
    // =========================================================

    private fun startWakeWordListening() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        try {

            voiceController.enableWakeMode()

            wakeWordEngine.start()

            audioCapture.start()

            updateNotification(
                "Say Hey Jarvis"
            )

        } catch (e: Exception) {

            handleVoiceError(
                "Wake listener failed"
            )
        }
    }

    // =========================================================
    // STOP WAKE LISTENER
    // =========================================================

    private fun stopWakeWordListening() {

        try {
            wakeWordEngine.stop()
        } catch (_: Exception) {
        }

        try {
            audioCapture.stop()
        } catch (_: Exception) {
        }
    }

    // =========================================================
    // WAKE WORD DETECTED
    // =========================================================

    private fun handleWakeWordDetected() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        /*
         * STOP the wake-word microphone pipeline first.
         *
         * This prevents wake-word detection from
         * repeatedly triggering while the user gives
         * the actual command.
         */
        stopWakeWordListening()

        /*
         * Tell BackgroundVoiceController that
         * "Hey Jarvis" was detected.
         *
         * It then enters command-listening mode.
         */
        try {

            voiceController.wakeDetected()

        } catch (_: Exception) {

            updateNotification(
                "JARVIS voice standby"
            )
        }

        updateNotification(
            "JARVIS is listening..."
        )
    }

    // =========================================================
    // CONTROLLER WAKE CALLBACK
    // =========================================================

    private fun handleWakeDetected() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        updateNotification(
            "JARVIS is listening..."
        )
    }

    // =========================================================
    // VOICE COMMAND
    // =========================================================

    private fun handleRecognizedCommand(
        command: String
    ) {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        val cleanCommand =
            command.trim()

        if (
            cleanCommand.isBlank()
        ) {
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
             * Keep service alive.
             */
            try {
                voiceController.speechFinished()
            } catch (_: Exception) {
            }

            /*
             * Return to wake standby.
             */
            restartWakeWordListening()
        }
    }

    // =========================================================
    // RESTART WAKE LISTENER
    // =========================================================

    private fun restartWakeWordListening() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        try {

            voiceController.start()

            wakeWordEngine.start()

            audioCapture.start()

            updateNotification(
                "Say Hey Jarvis"
            )

        } catch (_: Exception) {

            updateNotification(
                "JARVIS voice standby"
            )
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

        val text =
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

        updateNotification(text)
    }

    // =========================================================
    // ERROR
    // =========================================================

    private fun handleVoiceError(
        error: String
    ) {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        /*
         * Do not expose raw technical errors
         * in the permanent notification.
         */
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
            .setContentText(text)
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
                .setContentText(text)
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
    // DISABLE
    // =========================================================

    private fun disableWakeMode() {

        wakeModeEnabled = false

        stopWakeWordListening()

        try {
            voiceController.disableWakeMode()
        } catch (_: Exception) {
        }

        stopVoiceService()
    }

    // =========================================================
    // STOP
    // =========================================================

    private fun stopVoiceService() {

        serviceActive = false
        wakeModeEnabled = false

        stopWakeWordListening()

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
            audioCapture.stop()
        } catch (_: Exception) {
        }

        try {
            wakeWordEngine.release()
        } catch (_: Exception) {
        }

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