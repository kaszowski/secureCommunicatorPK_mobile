package com.example.securechatapp.utils

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
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

    fun decryptPrivateKey(encryptedPrivateKey: String, password: String): String {
        return try {
            // 1. Dekoduj z Base64
            val encryptedBytes = Base64.decode(encryptedPrivateKey, Base64.NO_WRAP)

            // 2. Przygotuj klucz AES z hasła
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            val keyBytes = ByteArray(16)
            System.arraycopy(passwordBytes, 0, keyBytes, 0, minOf(passwordBytes.size, 16))
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // 3. Odszyfruj klucz prywatny
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            // 4. Zwróć jako Base64
            Base64.encodeToString(decryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("DecryptPrivateKey", "Error decrypting private key", e)
            throw RuntimeException("Błąd odszyfrowywania klucza prywatnego", e)
        }
    }

    fun rsaDecrypt(encryptedAesKeyString: String, privateKeyString: String): ByteArray {
        return try {
            // 1. Dekoduj zaszyfrowany klucz AES
            val encryptedAesKeyBytes = Base64.decode(encryptedAesKeyString, Base64.NO_WRAP)

            // 2. Przygotuj klucz prywatny
            val privateKeyBytes = try {
                // Spróbuj najpierw jako czysty Base64
                Base64.decode(privateKeyString, Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                // Jeśli nie udało się, może to być PEM
                privateKeyString
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\n", "")
                    .trim()
                    .let { Base64.decode(it, Base64.NO_WRAP) }
            }

            // Logowanie do debugowania
            Log.d("RSADecrypt", "Private key bytes length: ${privateKeyBytes.size}")

            // 3. Załaduj klucz prywatny
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(keySpec)

            // 4. Odszyfruj dane
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            cipher.doFinal(encryptedAesKeyBytes)
        } catch (e: Exception) {
            Log.e("RSADecrypt", "Error details:", e)
            throw RuntimeException("Błąd podczas odszyfrowywania klucza AES: ${e.message}", e)
        }
    }



}