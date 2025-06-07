package com.example.securechatapp.network

import android.content.Context
import getOkHttpClientWithCert
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy

object ApiClient {
    private lateinit var apiService: ApiService

    fun init(context: Context) {
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

        val client = getOkHttpClientWithCert(context)
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor(TokenInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://10.0.2.2:5000/")
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
}