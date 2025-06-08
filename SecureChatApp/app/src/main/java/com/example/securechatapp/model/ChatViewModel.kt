package com.example.securechatapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securechatapp.network.ApiClient
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = ApiClient.getService().getMessages(
                    MessageRequest(
                        conversationId = conversationId,
                        limit = 50,
                        offset = 0
                    )
                )

                if (response.isSuccessful) {
                    _messages.value = response.body()?.messages ?: emptyList()
                } else {
                    _errorMessage.value = "Błąd ładowania wiadomości: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Błąd sieci: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(conversationId: String, encryptedContent: ByteArray) {
        viewModelScope.launch {
            try {
                // TODO: Wysyłanie przez Socket.IO
                // Tymczasowo dodajemy do lokalnej listy
                val newMessage = Message(
                    messageId = UUID.randomUUID().toString(),
                    userId = "currentUserId", // TODO: Pobierz aktualne ID użytkownika
                    conversationId = conversationId,
                    content = Content(
                        type = "Buffer",
                        data = encryptedContent.map { it.toInt() }
                    ),
                    sendAt = System.currentTimeMillis().toString()
                )


                _messages.value = _messages.value?.plus(newMessage) ?: listOf(newMessage)
            } catch (e: Exception) {
                _errorMessage.value = "Błąd wysyłania wiadomości: ${e.message}"
            }
        }
    }
}