package com.example.jarvis.utils
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
object NetworkUtils { fun isOnline(c:Context):Boolean { val cm=c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager; val n=cm.activeNetwork ?: return false; return cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true } }
