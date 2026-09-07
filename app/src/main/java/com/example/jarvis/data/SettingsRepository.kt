package com.example.jarvis.data
class SettingsRepository(private val p:PreferencesManager){fun setApiKey(k:String)=p.saveApiKey(k.trim());fun getApiKey()=p.getApiKey()}
