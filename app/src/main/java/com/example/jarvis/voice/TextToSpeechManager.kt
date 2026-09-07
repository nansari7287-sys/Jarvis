package com.example.jarvis.voice
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
class TextToSpeechManager(c:Context):TextToSpeech.OnInitListener{private val tts=TextToSpeech(c,this);private var ready=false;override fun onInit(s:Int){ready=s==TextToSpeech.SUCCESS;if(ready)tts.language=Locale.getDefault()};fun speak(t:String){if(ready)tts.speak(t,TextToSpeech.QUEUE_FLUSH,null,"jarvis_response")};fun shutdown()=tts.shutdown()}
