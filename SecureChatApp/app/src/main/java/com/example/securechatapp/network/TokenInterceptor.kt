package com.example.securechatapp.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class TokenInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Nie przechwytujemy login/register/refresh
        val excludedPaths = listOf("login", "register", "refresh/token")
        if (excludedPaths.any { originalRequest.url.encodedPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Token JWT jest w ciasteczku – nie trzeba nic dodawać
        var response = chain.proceed(originalRequest)

        // Jeśli odpowiedź to 401 – próbujemy odświeżyć token
        if (response.code == 401) {
            response.close() // zamykamy poprzednią odpowiedź

            try {
                val refreshCall = ApiClient.getService().refreshTokenSync()
                val refreshResponse = refreshCall.execute()

                if (refreshResponse.isSuccessful) {
                    // Udało się odświeżyć token – ponów żądanie
                    val newRequest = originalRequest.newBuilder().build()
                    return chain.proceed(newRequest)
                } else {
                    // Nie udało się odświeżyć – wyczyść cookies (opcjonalnie)
                    clearCookies()
                }
            } catch (e: Exception) {
                clearCookies()
            }
        }

        return response
    }

    private fun clearCookies() {
        // Wyczyść cookies globalnie – tylko jeśli naprawdę potrzebne
        val cookieHandler = java.net.CookieHandler.getDefault()
        if (cookieHandler is java.net.CookieManager) {
            cookieHandler.cookieStore.removeAll()
        }
    }
}
