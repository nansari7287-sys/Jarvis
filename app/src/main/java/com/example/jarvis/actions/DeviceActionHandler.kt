package com.example.jarvis.actions
import android.content.Context
import android.content.Intent
import android.provider.Settings
class DeviceActionHandler(private val c:Context){fun openWifiSettings()=c.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));fun openBluetoothSettings()=c.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
