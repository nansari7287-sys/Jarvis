package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvis.voice.WakeWordManager

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

    private lateinit var wakeWordManager: WakeWordManager

    private var serviceActive = false
    private var wakeModeEnabled = false

    override fun onCreate() {
        super.onCreate()

        wakeWordManager = WakeWordManager()

        createNotificationChannel()
    }

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
                    intent.getStringExtra(EXTRA_COMMAND)
                        ?.trim()
                        .orEmpty()

                if (command.isNotBlank()) {
                    handleVoiceCommand(command)
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

    /**
     * Start the foreground voice service.
     *
     * The service itself does not secretly activate the microphone.
     * Actual microphone/wake listening will be connected separately.
     */
    private fun startVoiceService() {

        if (!serviceActive) {
            startVoiceForeground()
            serviceActive = true
        } else {
            updateNotification(
                if (wakeModeEnabled) {
                    "Hey Jarvis wake mode is active"
                } else {
                    "JARVIS voice service is active"
                }
            )
        }
    }

    /**
     * Enable the user's Voice Wake Mode.
     */
    private fun enableWakeMode() {

        startVoiceForeground()

        serviceActive = true
        wakeModeEnabled = true

        wakeWordManager.setEnabled(true)

        updateNotification(
            "Hey Jarvis wake mode is active"
        )

        /*
         * Actual wake-word/audio listener will be attached
         * here in the next integration step.
         */
    }

    /**
     * Disable Voice Wake Mode.
     *
     * The service is stopped completely because there is no
     * reason to keep a microphone foreground service alive
     * when the user has disabled background wake.
     */
    private fun disableWakeMode() {

        wakeModeEnabled = false

        wakeWordManager.setEnabled(false)

        stopVoiceService()
    }

    /**
     * Receive a recognized voice command.
     *
     * MainActivity / voice controller will consume this
     * through the command-routing layer.
     */
    private fun handleVoiceCommand(command: String) {

        if (!serviceActive) {
            return
        }

        val cleanCommand = command.trim()

        if (cleanCommand.isBlank()) {
            return
        }

        val commandIntent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            action = ACTION_VOICE_COMMAND

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
            startActivity(commandIntent)
        } catch (_: Exception) {
            /*
             * Do not crash the foreground service if Android
             * refuses an activity launch.
             */
        }
    }

    /**
     * Start microphone-type foreground service.
     */
    private fun startVoiceForeground() {

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

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

    /**
     * Build persistent foreground-service notification.
     */
    private fun createNotification(): Notification {

        val openIntent = Intent(
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

        val notificationText =
            if (wakeModeEnabled) {
                "Hey Jarvis wake mode is active"
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
                notificationText
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

    /**
     * Update the existing notification.
     */
    private fun updateNotification(
        text: String
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        val openIntent = Intent(
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        ) {
            android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    /**
     * Stop everything.
     */
    private fun stopVoiceService() {

        wakeModeEnabled = false
        serviceActive = false

        wakeWordManager.setEnabled(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

        } else {

            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    /**
     * Notification channel for Android 8+.
     */
    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
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

        manager.createNotificationChannel(channel)
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    override fun onDestroy() {

        wakeModeEnabled = false
        serviceActive = false

        wakeWordManager.setEnabled(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

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