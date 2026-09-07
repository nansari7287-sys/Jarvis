package com.example.jarvis
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jarvis.data.PreferencesManager
import com.example.jarvis.ui.*
import com.example.jarvis.utils.PermissionHelper
import com.example.jarvis.voice.*
import kotlinx.coroutines.launch
class MainActivity:ComponentActivity(){private lateinit var vm:MainViewModel;private lateinit var adapter:ChatAdapter;private lateinit var prefs:PreferencesManager;private lateinit var speech:SpeechRecognizerManager;private lateinit var tts:TextToSpeechManager
override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);vm=ViewModelProvider(this)[MainViewModel::class.java];prefs=PreferencesManager(this);speech=SpeechRecognizerManager(this);tts=TextToSpeechManager(this);val input=findViewById<EditText>(R.id.messageInput);val send=findViewById<ImageButton>(R.id.sendButton);val mic=findViewById<ImageButton>(R.id.micButton);val rv=findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.messageRecyclerView);adapter=ChatAdapter();rv.layoutManager=LinearLayoutManager(this);rv.adapter=adapter;send.setOnClickListener{val t=input.text.toString().trim();if(t.isNotBlank()){vm.send(t,prefs.getApiKey());input.text.clear()}};mic.setOnClickListener{if(!PermissionHelper.hasAudioPermission(this)){PermissionHelper.requestAudioPermission(this);return@setOnClickListener};speech.start({input.setText(it);input.setSelection(input.length())},{Toast.makeText(this,it,Toast.LENGTH_SHORT).show()})};lifecycleScope.launch{vm.ui.collect{adapter.submitList(it.messages);if(it.messages.isNotEmpty())rv.scrollToPosition(it.messages.lastIndex)}}}
override fun onDestroy(){speech.destroy();tts.shutdown();super.onDestroy()}}
