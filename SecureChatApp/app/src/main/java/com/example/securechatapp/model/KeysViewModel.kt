package com.example.securechatapp.model

import android.util.Log
import androidx.lifecycle.*
import com.example.securechatapp.network.ApiClient
import kotlinx.coroutines.launch

class KeysViewModel : ViewModel() {

    private val _ownKeys = MutableLiveData<KeysResponse?>()
    val ownKeys: LiveData<KeysResponse?> = _ownKeys

    private val _recipientPublicKey = MutableLiveData<String?>()
    val recipientPublicKey: LiveData<String?> = _recipientPublicKey

    /**
     * Pobiera własne klucze publiczny i prywatny (np. po zalogowaniu).
     */
    fun loadOwnKeys() {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getKeys()
                if (response.isSuccessful) {
                    _ownKeys.postValue(response.body())
                } else {
                    Log.e("KeysViewModel", "Failed to load own keys: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("KeysViewModel", "Error loading own keys", e)
            }
        }
    }

    /**
     * Pobiera publiczny klucz odbiorcy po nazwie użytkownika.
     */
    fun loadRecipientPublicKey(username: String) {
        viewModelScope.launch {
            try {
                val request = PublicKeyRequest(username)
                val response = ApiClient.getService().getPublicKey(request)
                if (response.isSuccessful) {
                    _recipientPublicKey.postValue(response.body()?.public_key)
                } else {
                    Log.e("KeysViewModel", "Failed to get recipient key: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("KeysViewModel", "Error getting recipient key", e)
            }
        }
    }

    /**
     * Czy klucze zostały załadowane.
     */
    fun areKeysReady(): Boolean {
        return _ownKeys.value?.private_key != null && _recipientPublicKey.value != null
    }
}
