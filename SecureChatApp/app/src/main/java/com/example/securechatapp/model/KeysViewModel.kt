package com.example.securechatapp.model

import android.util.Log
import androidx.lifecycle.*
import com.example.securechatapp.network.ApiClient
import com.example.securechatapp.utils.CryptoUtils
import kotlinx.coroutines.launch

class KeysViewModel : ViewModel() {

    private val _ownKeys = MutableLiveData<KeysDecrypted?>()
    val ownKeys: LiveData<KeysDecrypted?> = _ownKeys

    private val _recipientPublicKey = MutableLiveData<String?>()
    val recipientPublicKey: LiveData<String?> = _recipientPublicKey

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

    fun loadAndDecryptKeys(password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getKeys()
                if (response.isSuccessful) {

                    val keys = response.body()?.keys
                    val bytesPublicKey = keys?.public_key?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                    val publicKey = String(bytesPublicKey)

                    Log.d("klucz publiczny", publicKey)

                    val bytesPrivateKey = keys?.private_key?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                    val privateKey = String(bytesPrivateKey)

                    Log.d("klucz prywatny", privateKey)

                    val decryptedPrivateKey = CryptoUtils.decryptPrivateKey(privateKey, password)
                    _ownKeys.postValue(
                        KeysDecrypted(
                            public_key = publicKey,
                            private_key = decryptedPrivateKey
                        )
                    )
                } else {
                    Log.e("KeysViewModel", "Failed to load keys: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("KeysViewModel", "Error loading/decrypting keys", e)
            }
        }
    }
}
