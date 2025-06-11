package com.example.securechatapp.network

import TokenInterceptor
import android.content.Context
import android.util.Base64
import android.util.Log
import getOkHttpClientWithCert
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.nio.charset.StandardCharsets

object ApiClient {
    private lateinit var apiService: ApiService
    private lateinit var cookieManager: CookieManager

    var loggedInUsername: String? = null
        private set

    var privateUserKey: String? = null
        private set



    fun init(context: Context) {
        cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }

        val client = getOkHttpClientWithCert(context)
            .cookieJar(JavaNetCookieJar(cookieManager))

            .addInterceptor(TokenInterceptor(cookieManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://10.0.2.2:5000/")
            //.baseUrl("https://lokalnyAdresKompa:5000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    fun getService(): ApiService {
        if (!::apiService.isInitialized) {
            throw IllegalStateException("ApiClient not initialized")
        }
        return apiService
    }

    fun getCookieManager(): CookieManager {
        if (!::cookieManager.isInitialized) {
            throw IllegalStateException("ApiClient not initialized")
        }
        return cookieManager
    }

    fun parseJwt(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) throw IllegalArgumentException("Invalid token format")

            val base64Url = parts[1]
            val base64 = base64Url
                .replace('-', '+')
                .replace('_', '/')
                .let {
                    // Pad with '=' to make the string length a multiple of 4
                    when (it.length % 4) {
                        2 -> "$it=="
                        3 -> "$it="
                        else -> it
                    }
                }

            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            val json = String(decodedBytes, StandardCharsets.UTF_8)

            JSONObject(json)
        } catch (e: Exception) {
            null
        }
    }

    fun getUserId(): String?
    {
        val uri = URI.create("https://10.0.2.2:5000")
        val cookies = getCookieManager().cookieStore.get(uri)
        var token = ""
        for (cookie in cookies) {
            if (cookie.name == "token") {
                token = cookie.value
            }
        }
        val payload = parseJwt(token)
        if (payload != null) {
            Log.d("payload", payload.toString(4))
        }

        return payload?.optString("userId")
    }

    fun setLoggedInUsername(username: String) {
        loggedInUsername = username
    }
    fun setPrivateKey(privateKey: String) {
        privateUserKey = privateKey
    }

    fun getPrivateKey(): String? {
        return privateUserKey
    }
}