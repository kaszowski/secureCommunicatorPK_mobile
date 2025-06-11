package com.example.securechatapp.utils

import android.util.Base64
import java.security.SecureRandom
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

object CryptoUtils {

    fun encrypt(data: String, key: ByteArray): String {
        val secretKey = SecretKeySpec(key, "AES")
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) } // Losowy IV
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Zakoduj IV + ciphertext do jednego Base64 stringa
        val combined = iv + encryptedBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, key: ByteArray): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 16)
        val ciphertext = combined.copyOfRange(16, combined.size)

        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(ciphertext)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun decryptPrivateKey(encryptedBase64: String, password: String): String {
        val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(16)
        System.arraycopy(passwordBytes, 0, keyBytes, 0, passwordBytes.size.coerceAtMost(16))

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decrypted = cipher.doFinal(encryptedBytes)
        return String(decrypted, Charsets.UTF_8)
    }
}