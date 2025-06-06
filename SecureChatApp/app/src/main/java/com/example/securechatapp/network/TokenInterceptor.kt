package com.example.securechatapp.network

import android.content.Context
import com.example.securechatapp.model.AuthPrefs
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class TokenInterceptor(private val context: Context) : Interceptor {
    private val authPrefs = AuthPrefs(context)

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip token for login, register, refresh
        val excludedPaths = listOf("login", "register", "refresh/token")
        if (excludedPaths.any { originalRequest.url.encodedPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Add token
        val requestWithToken = originalRequest.newBuilder().apply {
            authPrefs.authToken?.let { token ->
                header("Authorization", "Bearer $token")
            }
        }.build()

        var response = chain.proceed(requestWithToken)

        // Handle 401 Unauthorized
        if (response.code == 401 && authPrefs.authToken != null) {
            response.close()  // Very important: close old response before new request

            try {
                val refreshCall = ApiClient.getService().refreshTokenSync()
                val refreshResponse = refreshCall.execute()

                if (refreshResponse.isSuccessful) {
                    val refreshedTokens = refreshResponse.body()
                    if (refreshedTokens != null) {
                        authPrefs.authToken = refreshedTokens.token
                        authPrefs.tokenExpiry = refreshedTokens.token_expiry

                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer ${refreshedTokens.token}")
                            .build()

                        return chain.proceed(newRequest)
                    }
                } else {
                    authPrefs.clear()
                }
            } catch (e: Exception) {
                authPrefs.clear()
            }
        }

        return response
    }
}
