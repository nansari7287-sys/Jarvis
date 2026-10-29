package com.example.jarvis.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.jarvis.voice.VoiceService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("JARVIS_BOOT", "BootReceiver Triggered with Action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.i("JARVIS_BOOT", "System Boot Completed. Starting Titan Core Engine...")

            // VoiceService को बैकग्राउंड में ऑटो-स्टार्ट करने के लिए इंटेंट
            val serviceIntent = Intent(context, VoiceService::class.java).apply {
                this.action = VoiceService.ACTION_ENABLE_WAKE
            }

            try {
                // Android 8.0 (Oreo) और उसके बाद के वर्शन के लिए Foreground Service स्टार्ट करना ज़रूरी है
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("JARVIS_BOOT", "Failed to auto-start VoiceService: ${e.message}")
            }
        }
    }
}
