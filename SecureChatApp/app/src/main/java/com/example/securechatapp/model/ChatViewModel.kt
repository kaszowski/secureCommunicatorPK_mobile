package com.example.securechatapp.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securechatapp.network.ApiClient
import com.example.securechatapp.network.SocketManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _socketStatus = MutableLiveData<Boolean>(false)
    val socketStatus: LiveData<Boolean> = _socketStatus

    private val messageCallback = { message: Message ->
        val currentList = _messages.value.orEmpty()
        _messages.postValue(currentList + message)
    }

    fun initializeSocket(context: Context) {
        SocketManager.initialize(context, ApiClient.getCookieManager())
        SocketManager.setupMessageListener(messageCallback)

        SocketManager.connectionStatus.observeForever { connected ->
            _socketStatus.postValue(connected)
            if (!connected) {
                _errorMessage.postValue("Connection lost. Reconnecting...")
            }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = ApiClient.getService().getMessages(
                    MessageRequest(conversationId, 50, 0)
                )

                if (response.isSuccessful) {
                    _messages.value = response.body()?.messages ?: emptyList()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Error: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(conversationId: String, content: String) {
        if (content.isBlank()) return

        if (!SocketManager.isConnected()) {
            _errorMessage.value = "No connection to server"
            return
        }

        // Add temporary message for instant feedback
        val tempMessage = Message(
            messageId = UUID.randomUUID().toString(),
            userId = ApiClient.getUserId().toString(),
            conversationId = conversationId,
            content = Content("text", content.toByteArray().map { it.toInt() }),
            sendAt = Instant.now().toString()
        )
        _messages.value = _messages.value.orEmpty() + tempMessage

        // Send via Socket.IO
        SocketManager.sendMessage(conversationId, content)
    }

    override fun onCleared() {
        super.onCleared()
        SocketManager.removeMessageListener(messageCallback)
        SocketManager.disconnect()
    }
}