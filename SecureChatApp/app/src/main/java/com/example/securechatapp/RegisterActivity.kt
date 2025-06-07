package com.example.securechatapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.securechatapp.databinding.ActivityRegisterBinding
import com.example.securechatapp.model.AuthViewModel
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                binding.tvError.text = "Wszystkie pola muszą być wypełnione"
                return@setOnClickListener
            }

            try {
                val passwordHash = hashPassword(password)
                val keyPair = generateKeyPair()
                val publicKeyBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
                val encryptedPrivateKeyBase64 = encryptPrivateKey(keyPair.private.encoded, password)

                // Wywołanie register z public i private key
                viewModel.register(email, username, passwordHash, publicKeyBase64, encryptedPrivateKeyBase64)

                binding.tvError.text = "Wysyłanie danych rejestracji..."
            } catch (e: Exception) {
                binding.tvError.text = "Błąd podczas przygotowania rejestracji: ${e.message}"
            }
        }

        viewModel.registerState.observe(this) { result ->
            result.onSuccess {
                binding.tvError.text = "Rejestracja zakończona sukcesem!"
            }
            result.onFailure { exception ->
                binding.tvError.text = exception.message ?: "Wystąpił błąd podczas rejestracji"
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    private fun encryptPrivateKey(privateKey: ByteArray, password: String): String {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(16)
        System.arraycopy(passwordBytes, 0, keyBytes, 0, passwordBytes.size.coerceAtMost(16))

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(privateKey)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
}
