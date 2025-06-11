package com.example.securechatapp.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securechatapp.network.ApiClient
import com.example.securechatapp.network.SocketManager
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _socketStatus = MutableLiveData<Boolean>()
    val socketStatus: LiveData<Boolean> = _socketStatus

    private val connectionStatusObserver = Observer<Boolean> { connected ->
        _socketStatus.postValue(connected)
    }

    fun initializeSocket(context: Context) {
        SocketManager.initialize(context, ApiClient.getCookieManager())
        SocketManager.setupMessageListener { message ->
            _messages.postValue(_messages.value.orEmpty() + message)
        }
        SocketManager.connectionStatus.observeForever(connectionStatusObserver)
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
                } else {
                    _errorMessage.value = "Błąd: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Błąd sieci: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(conversationId: String, content: String) {
        if (!SocketManager.isConnected()) {
            _errorMessage.value = "Brak połączenia z serwerem"
            return
        }

        // Najpierw dodaj lokalnie dla natychmiastowego feedbacku
        val tempMessage = Message(
            messageId = UUID.randomUUID().toString(),
            userId = ApiClient.getUserId().toString(),
            conversationId = conversationId,
            content = Content("Buffer", content.toByteArray().map { it.toInt() }),
            sendAt = System.currentTimeMillis().toString()
        )
        _messages.value = _messages.value.orEmpty() + tempMessage

        // Wyślij przez Socket.IO
        SocketManager.sendMessage(conversationId, content)
    }


    override fun onCleared() {
        super.onCleared()
        SocketManager.connectionStatus.removeObserver(connectionStatusObserver)
        SocketManager.disconnect()
    }

}