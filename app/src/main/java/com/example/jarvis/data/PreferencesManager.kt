package com.example.jarvis.data
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.jarvis.utils.Constants
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
class PreferencesManager(c:Context){private val p=c.getSharedPreferences(Constants.PREFS_NAME,Context.MODE_PRIVATE)
private fun key():SecretKey{val ks=java.security.KeyStore.getInstance("AndroidKeyStore").apply{load(null)}; (ks.getKey("jarvis_key",null) as? SecretKey)?.let{return it}; val g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore"); g.init(KeyGenParameterSpec.Builder("jarvis_key",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()); return g.generateKey()}
fun saveApiKey(v:String){if(v.isBlank()){p.edit().remove(Constants.KEY_API).apply();return};val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());val b=c.iv+c.doFinal(v.toByteArray());p.edit().putString(Constants.KEY_API,Base64.encodeToString(b,Base64.NO_WRAP)).apply()}
fun getApiKey():String=try{val b=Base64.decode(p.getString(Constants.KEY_API,null)?:return "",Base64.NO_WRAP);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,b.copyOfRange(0,12)));String(c.doFinal(b.copyOfRange(12,b.size)))}catch(_:Exception){""}}
