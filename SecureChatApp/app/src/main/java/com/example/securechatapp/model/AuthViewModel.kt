package com.example.securechatapp.model

import android.content.Context
import androidx.lifecycle.*
import com.example.securechatapp.network.*
import kotlinx.coroutines.launch
import java.lang.Exception

class AuthViewModel : ViewModel() {
    private val _loginState = MutableLiveData<Result<Unit>>()
    val loginState: LiveData<Result<Unit>> = _loginState

    private val _registerState = MutableLiveData<Result<Unit>>()
    val registerState: LiveData<Result<Unit>> = _registerState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    _loginState.postValue(Result.success(Unit))
                } else {
                    _loginState.postValue(Result.failure(
                        Exception(response.errorBody()?.string() ?: "Unknown error")
                    ))
                }
            } catch (e: Exception) {
                _loginState.postValue(Result.failure(e))
            }
        }
    }

    fun register(email: String, username: String, passwordHash: String, publicKey: String, privateKey: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().register(
                    RegisterRequest(email, username, passwordHash, publicKey, privateKey)
                )
                if (response.isSuccessful) {
                    _registerState.postValue(Result.success(Unit))
                } else {
                    _registerState.postValue(Result.failure(Exception(response.errorBody()?.string())))
                }
            } catch (e: Exception) {
                _registerState.postValue(Result.failure(e))
            }
        }
    }


    fun logout() {
        viewModelScope.launch {
            try {
                ApiClient.getService().logout()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}