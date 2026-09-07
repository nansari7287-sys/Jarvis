package com.example.jarvis.data
class ConversationDao { private val messages=mutableListOf<ConversationEntity>(); fun insert(m:ConversationEntity)=messages.add(m).let{}; fun all()=messages.toList(); fun clear()=messages.clear() }
