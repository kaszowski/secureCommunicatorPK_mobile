import android.util.Log
import com.example.securechatapp.network.ApiClient
import okhttp3.Interceptor
import okhttp3.Response
import java.net.CookieManager

class TokenInterceptor(
    private val cookieManager: CookieManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        val excludedPaths = listOf("login", "register", "refresh/token")
        if (excludedPaths.any { url.encodedPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Sprawdź token_expiry z ciasteczek
        val cookies = cookieManager.cookieStore.get(url.toUri())
        val expiryCookie = cookies.find { it.name == "token_expiry" }
        val expiryTimestamp = expiryCookie?.value?.toLongOrNull() ?: 0L
        val now = System.currentTimeMillis()

        if (expiryTimestamp - now < 60_000) {
            // Token prawie wygasł – odśwież
            try {
                val refreshCall = ApiClient.getService().refreshTokenSync()
                val refreshResponse = refreshCall.execute()
                if (!refreshResponse.isSuccessful) {
                    // Nie udało się – wyczyść ciasteczka
                    cookieManager.cookieStore.removeAll()
                }
                else{
                    Log.e("RefreshToken", "Token odswiezony")
                }
            } catch (e: Exception) {
                cookieManager.cookieStore.removeAll()
            }
        }

        // Wyślij request (ciasteczka dołączane automatycznie)
        return chain.proceed(originalRequest)
    }
}
