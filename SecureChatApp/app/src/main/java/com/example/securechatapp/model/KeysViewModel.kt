package com.example.securechatapp.model

import androidx.lifecycle.*
import com.example.securechatapp.network.*
import kotlinx.coroutines.launch

class KeysViewModel : ViewModel() {
    private val _keys = MutableLiveData<KeysResponse?>()
    val keys: LiveData<KeysResponse?> = _keys

    private val _publicKey = MutableLiveData<String?>()
    val publicKey: LiveData<String?> = _publicKey

    fun loadKeys() {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getKeys()
                if (response.isSuccessful) {
                    _keys.postValue(response.body())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getPublicKey(username: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getPublicKey(PublicKeyRequest(username))
                if (response.isSuccessful) {
                    _publicKey.postValue(response.body()?.public_key)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}