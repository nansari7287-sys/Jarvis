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

        const val ACTION_VOICE_RESPONSE_FINISHED =
            "com.example.jarvis.action.VOICE_RESPONSE_FINISHED"

        const val ACTION_VOICE_RESUME_LISTENING =
            "com.example.jarvis.action.VOICE_RESUME_LISTENING"

        const val ACTION_VOICE_RETURN_STANDBY =
            "com.example.jarvis.action.VOICE_RETURN_STANDBY"

        const val EXTRA_COMMAND =
            "com.example.jarvis.extra.COMMAND"

        private const val CHANNEL_ID =
            "jarvis_voice_channel"

        private const val NOTIFICATION_ID =
            1001
    }

    private var serviceActive = false
    private var wakeModeEnabled = false

    private lateinit var voiceController: BackgroundVoiceController
    private lateinit var audioCapture: AudioCaptureManager
    private lateinit var wakeWordEngine: WakeWordEngine

    // =========================================================
    // CREATE
    // =========================================================

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

                if (command.isNotBlank()) {

                    handleRecognizedCommand(
                        command
                    )
                }
            }

            ACTION_VOICE_RESPONSE_FINISHED -> {

                handleResponseFinished()
            }

            ACTION_VOICE_RESUME_LISTENING -> {

                handleResumeListening()
            }

            ACTION_VOICE_RETURN_STANDBY -> {

                handleReturnToStandby()
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

        startVoiceForeground()

        serviceActive = true
        wakeModeEnabled = true

        voiceController.start()

        startWakeWordListening()

        updateNotification(
            "Say Hey Jarvis"
        )
    }

    // =========================================================
    // START WAKE WORD LISTENING
    // =========================================================

    private fun startWakeWordListening() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        try {

            /*
             * Command SpeechRecognizer must not be active
             * while the wake-word microphone is running.
             */
            voiceController.enableWakeMode()

            wakeWordEngine.start()

            audioCapture.start()

            updateNotification(
                "Say Hey Jarvis"
            )

        } catch (_: Exception) {

            handleVoiceError(
                "Wake listener failed"
            )
        }
    }

    // =========================================================
    // RESTART WAKE WORD LISTENING
    // =========================================================

    private fun restartWakeWordListening() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        try {

            /*
             * First make sure the previous wake pipeline
             * is stopped.
             */
            stopWakeWordListening()

            /*
             * Return controller to wake-word standby.
             */
            voiceController.enableWakeMode()

            /*
             * Start the on-device wake-word pipeline again.
             */
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
    // STOP WAKE WORD LISTENING
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
         * Stop custom wake-word microphone before
         * Android SpeechRecognizer starts.
         */
        stopWakeWordListening()

        try {

            voiceController.wakeDetected()

        } catch (_: Exception) {

            restartWakeWordListening()

            return
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
    // RECOGNIZED COMMAND
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
             * MainActivity could not be opened.
             * Go back to wake-word standby.
             */
            try {

                voiceController.returnToStandby()

            } catch (_: Exception) {
            }

            restartWakeWordListening()
        }
    }

    // =========================================================
    // RESPONSE FINISHED
    // =========================================================

    private fun handleResponseFinished() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        /*
         * MainActivity tells us that TTS has completely
         * finished.
         *
         * BackgroundVoiceController will now start
         * command listening again.
         */
        try {

            voiceController.speechFinished()

        } catch (_: Exception) {

            restartWakeWordListening()

            return
        }

        updateNotification(
            "JARVIS is listening..."
        )
    }

    // =========================================================
    // RESUME LISTENING
    // =========================================================

    private fun handleResumeListening() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        try {

            voiceController.resumeListening()

        } catch (_: Exception) {
        }

        updateNotification(
            "JARVIS is listening..."
        )
    }

    // =========================================================
    // RETURN TO STANDBY
    // =========================================================

    private fun handleReturnToStandby() {

        if (
            !serviceActive ||
            !wakeModeEnabled
        ) {
            return
        }

        try {

            voiceController.returnToStandby()

        } catch (_: Exception) {
        }

        restartWakeWordListening()
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

        updateNotification(
            text
        )
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
         * Don't show raw technical error permanently.
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
    // CREATE NOTIFICATION
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

    // =========================================================
    // UPDATE NOTIFICATION
    // =========================================================

    private fun updateNotification(
        text: String
    ) {

        if (!serviceActive) {
            return
        }

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

    // =========================================================
    // PENDING INTENT FLAGS
    // =========================================================

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
    // DISABLE WAKE MODE
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
    // STOP SERVICE
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