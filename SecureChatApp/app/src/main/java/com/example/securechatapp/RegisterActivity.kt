package com.example.securechatapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.securechatapp.databinding.ActivityRegisterBinding
import com.example.securechatapp.model.AuthViewModel
import com.example.securechatapp.piv.PivGetPublicKeyContract
import com.yubico.yubikit.piv.Slot
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val PREF_PUBLIC_KEY = "PUBLIC_KEY"



class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

     private val requestPublicKey = registerForActivityResult(PivGetPublicKeyContract()) {
         viewModel.publicKey = it
         savePrivateKeyU2F()
     }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(viewModel.publicKey == null){
            requestPublicKey.launch(Slot.KEY_MANAGEMENT)
        }

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
                if(viewModel.publicKey != null){
                    val encryptedPrivateKeyYubico = encryptPrivateKey(viewModel.publicKey!!.encoded, password)
                    viewModel.register(email, username, passwordHash, publicKeyBase64, encryptedPrivateKeyYubico)
                }else{
                    val encryptedPrivateKeyBase64 = encryptPrivateKey(keyPair.private.encoded, password)
                    viewModel.register(email, username, passwordHash, publicKeyBase64, encryptedPrivateKeyBase64)
                }


                // Wywołanie register z public i private key
                //viewModel.register(email, username, passwordHash, publicKeyBase64, encryptedPrivateKeyBase64)

                binding.tvError.text = "Wysyłanie danych rejestracji..."
            } catch (e: Exception) {
                binding.tvError.text = "Błąd podczas przygotowania rejestracji: ${e.message}"
            }
        }

        viewModel.registerState.observe(this) { result ->
            result.onSuccess {
                binding.tvError.text = "Rejestracja zakończona sukcesem!"

                // Przejście do LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
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
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()

            // Logowanie do debugowania
            Log.d("KeyGeneration", "Public Key Format: ${keyPair.public.format}")
            Log.d("KeyGeneration", "Private Key Format: ${keyPair.private.format}")

            keyPair
        } catch (e: Exception) {
            Log.e("KeyGeneration", "Error generating key pair", e)
            throw e
        }
    }


    private fun encryptPrivateKey(privateKey: ByteArray, password: String): String {
        return try {
            // 1. Przygotuj klucz AES z hasła
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            val keyBytes = ByteArray(16)
            System.arraycopy(passwordBytes, 0, keyBytes, 0, minOf(passwordBytes.size, 16))
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // 2. Zaszyfruj klucz prywatny
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(privateKey)

            // 3. Zwróć jako Base64
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("EncryptPrivateKey", "Error encrypting private key", e)
            throw RuntimeException("Błąd podczas szyfrowania klucza prywatnego", e)
        }
    }

    private fun savePrivateKeyU2F(){
        viewModel.publicKey?.let {
            getPreferences(MODE_PRIVATE).edit()
                .putString(PREF_PUBLIC_KEY, Base64.encodeToString(it.encoded, Base64.DEFAULT))
                .apply()
        }
    }

    private fun loadPublicKeyU2F(){
        getPreferences(MODE_PRIVATE).getString(PREF_PUBLIC_KEY, null)?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            val kf = KeyFactory.getInstance("RSA")
            val spec = X509EncodedKeySpec(bytes)
            viewModel.publicKey = kf.generatePublic(spec)
        }
    }


}
