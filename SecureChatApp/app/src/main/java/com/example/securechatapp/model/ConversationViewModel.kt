package com.example.securechatapp.model

import androidx.lifecycle.*
import com.example.securechatapp.network.*
import kotlinx.coroutines.launch

class ConversationViewModel : ViewModel() {
    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getConversations()
                if (response.isSuccessful) {
                    _conversations.postValue(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadMessages(conversationId: String, limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getMessages(
                    MessagesRequest(conversationId, limit, offset)
                )
                if (response.isSuccessful) {
                    _messages.postValue(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun createConversation(userToAdd: String, keyMine: String, keyOther: String) {
        viewModelScope.launch {
            try {
                ApiClient.getService().createConversation(
                    CreateConversationRequest(userToAdd, keyMine, keyOther)
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}