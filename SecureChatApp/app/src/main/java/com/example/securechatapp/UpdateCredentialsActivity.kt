package com.example.securechatapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.securechatapp.databinding.ActivityUpdateCredentialsBinding
import com.example.securechatapp.model.UpdateData
import com.example.securechatapp.model.UpdateProfileRequest
import com.example.securechatapp.network.ApiClient
import kotlinx.coroutines.launch
import java.security.MessageDigest

class UpdateCredentialsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateCredentialsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateCredentialsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnChangePassword.setOnClickListener {
            updatePassword()
        }

        binding.btnUpdateProfile.setOnClickListener {
            updateProfile()
        }

    }

    private fun updatePassword() {
        val currentPassword = binding.etCurrentPasswordUpdatePassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()

        if (currentPassword.isBlank() || newPassword.isBlank()) {
            Toast.makeText(this, "Podaj aktualne i nowe hasło", Toast.LENGTH_SHORT).show()
            return
        }

        val oldPasswordHash = hashPassword(currentPassword)
        val updateData = UpdateData(newPassword = hashPassword(newPassword), currentPassword = oldPasswordHash)

        val request = UpdateProfileRequest(
            updates = updateData
        )

        sendUpdateRequest(request)
    }

    private fun updateProfile() {
        val currentPassword = binding.etCurrentPasswordUpdateProfile.text.toString()
        if (currentPassword.isBlank()) {
            Toast.makeText(this, "Podaj aktualne hasło", Toast.LENGTH_SHORT).show()
            return
        }

        val username = binding.etUsername.text.toString().takeIf { it.isNotBlank() }
        val usernameShow = binding.etUsernameShow.text.toString().takeIf { it.isNotBlank() }
        val email = binding.etEmail.text.toString().takeIf { it.isNotBlank() }

        val oldPasswordHash = hashPassword(currentPassword)
        val updateData = UpdateData(
            username = username,
            usernameShow = usernameShow,
            email = email,
            currentPassword = oldPasswordHash
        )

        val request = UpdateProfileRequest(
            updates = updateData
        )

        sendUpdateRequest(request)
    }

    private fun sendUpdateRequest(request: UpdateProfileRequest) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val response = ApiClient.getService().updateProfile(request)
                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    Toast.makeText(this@UpdateCredentialsActivity, "Zaktualizowano", Toast.LENGTH_SHORT).show()
                    finish() // lub inne działanie po udanej aktualizacji
                } else {
                    Toast.makeText(this@UpdateCredentialsActivity, "Błąd: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@UpdateCredentialsActivity, "Błąd sieci: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
