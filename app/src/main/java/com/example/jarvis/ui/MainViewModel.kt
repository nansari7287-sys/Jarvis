package com.example.jarvis.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.assistant.JarvisAssistant
import com.example.jarvis.models.Message
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class MainViewModel:ViewModel(){private val assistant=JarvisAssistant();private val _ui=MutableStateFlow(UiState());val ui:StateFlow<UiState>=_ui.asStateFlow();fun send(t:String,k:String){if(t.isBlank())return;_ui.value=_ui.value.copy(messages=_ui.value.messages+Message(text=t,isUser=true),isThinking=true);viewModelScope.launch{val r=assistant.respond(t,k);_ui.value=_ui.value.copy(messages=_ui.value.messages+Message(text=r.text,isUser=false),isThinking=false)}}}
