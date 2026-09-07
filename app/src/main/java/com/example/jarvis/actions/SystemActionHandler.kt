package com.example.jarvis.actions
import android.content.Context
import android.content.Intent
import android.provider.Settings
class SystemActionHandler(private val c:Context){fun openAccessibilitySettings()=c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));fun openAppSettings()=c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:${c.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
