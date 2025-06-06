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

    lateinit var authPrefs: AuthPrefs

    fun initPrefs(context: Context) {
        authPrefs = AuthPrefs(context)
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        authPrefs.authToken = loginResponse.token
                        authPrefs.tokenExpiry = loginResponse.token_expiry
                        _loginState.postValue(Result.success(Unit))
                    } ?: run {
                        _loginState.postValue(Result.failure(Exception("Empty response")))
                    }
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

    fun register(email: String, username: String, passwordHash: String, publicKey: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().register(
                    RegisterRequest(email, username, passwordHash, publicKey)
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
                authPrefs.clear()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}