package com.example.securechatapp.model

import androidx.lifecycle.*
import com.example.securechatapp.network.*
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    fun updateProfile(
        oldPasswordHash: String,
        newUsername: String? = null,
        newUsernameShow: String? = null,
        newEmail: String? = null,
        newPassword: String? = null
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().updateProfile(
                    UpdateProfileRequest(
                        updates = UpdateData(
                            username = newUsername,
                            username_show = newUsernameShow,
                            email = newEmail,
                            new_password = newPassword
                        ),
                        old_password_hash = oldPasswordHash
                    )
                )
                if (response.isSuccessful) {
                    _updateResult.postValue(Result.success(Unit))
                } else {
                    _updateResult.postValue(Result.failure(Exception(response.errorBody()?.string())))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }
}