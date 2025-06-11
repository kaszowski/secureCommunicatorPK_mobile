package com.example.securechatapp.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.securechatapp.network.ApiClient
import kotlinx.coroutines.launch
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class ConversationViewModel : ViewModel() {
    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = ApiClient.getService().getConversations()
                if (response.isSuccessful) {
                    _conversations.value = response.body()?.conversations ?: emptyList()

                } else {
                    _errorMessage.value = "Failed to load conversations: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createConversation(userToAdd: String, ownPublicKey: String, otherPublicKey: String) {
        viewModelScope.launch {
            try {
                // 1. Generujemy AES key
                val aesKey = generateAESKey()
                val aesKeyBytes = aesKey.encoded

                // 2. Szyfrujemy klucz AES kluczami publicznymi (Twoim i drugiego użytkownika)
                val keyMineEncrypted = rsaEncrypt(aesKeyBytes, ownPublicKey)
                val keyOtherEncrypted = rsaEncrypt(aesKeyBytes, otherPublicKey)

                // 3. Przygotowanie zapytania (Base64 zaszyfrowanych kluczy)
                val request = CreateConversationRequest(
                    userToAdd = userToAdd,
                    keyMine = Base64.encodeToString(keyMineEncrypted, Base64.NO_WRAP),
                    keyOther = Base64.encodeToString(keyOtherEncrypted, Base64.NO_WRAP)
                )

                // 4. Wywołanie API
                val response = ApiClient.getService().createConversation(request)

                if (response.isSuccessful) {
                    Log.d("ConversationViewModel", "Konwersacja utworzona pomyślnie")
                    loadConversations()
                } else {
                    Log.e("ConversationViewModel", "Błąd utworzenia konwersacji: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ConversationViewModel", "Wyjątek przy tworzeniu konwersacji", e)
            }
        }
    }

    private fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128)
        return keyGen.generateKey()
    }

    fun rsaEncrypt(data: ByteArray, publicKeyPEM: String): ByteArray {
        val cleanKey = publicKeyPEM
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.decode(cleanKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }



}