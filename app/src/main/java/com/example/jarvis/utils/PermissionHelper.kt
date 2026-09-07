package com.example.jarvis.utils
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
object PermissionHelper { const val AUDIO_REQUEST=1001; fun hasAudioPermission(a:Activity)=ContextCompat.checkSelfPermission(a,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED; fun requestAudioPermission(a:Activity)=ActivityCompat.requestPermissions(a,arrayOf(Manifest.permission.RECORD_AUDIO),AUDIO_REQUEST) }
