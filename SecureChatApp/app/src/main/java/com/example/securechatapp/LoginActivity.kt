package com.example.securechatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.securechatapp.databinding.ActivityLoginBinding
import com.example.securechatapp.model.AuthViewModel
import com.example.securechatapp.model.KeysViewModel
import com.example.securechatapp.network.ApiClient
import kotlinx.coroutines.launch
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private val keysViewModel: KeysViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicjalizacja
        ApiClient.init(applicationContext)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.apply {
            btnLogin.setOnClickListener {
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()

                when {
                    username.isEmpty() -> etUsername.error = "Username required"
                    password.isEmpty() -> etPassword.error = "Password required"
                    else -> {
                        val passwordHash = hashPassword(password)
                        viewModel.login(username, passwordHash)
                    }
                }
            }

        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { result ->
            result.onSuccess {
                val username = binding.etUsername.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                keysViewModel.loadAndDecryptKeys(password)
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            }.onFailure { error ->
                binding.tvError.text = error.message ?: "Login failed"
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}