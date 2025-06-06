import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import com.example.securechatapp.R
import javax.net.ssl.*

fun getOkHttpClientWithCert(context: Context): OkHttpClient.Builder {
    val cf: CertificateFactory = CertificateFactory.getInstance("X.509")

    // Załaduj certyfikat z pliku res/raw/server.crt
    val certInputStream: InputStream = context.resources.openRawResource(R.raw.localhost)

    val ca = certInputStream.use {
        cf.generateCertificate(it)
    }

    // Utwórz KeyStore i dodaj certyfikat
    val keyStoreType = KeyStore.getDefaultType()
    val keyStore = KeyStore.getInstance(keyStoreType).apply {
        load(null, null)
        setCertificateEntry("ca", ca)
    }

    // Utwórz TrustManager bazujący na KeyStore
    val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
    val tmf = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
        init(keyStore)
    }

    // Utwórz SSLContext z zaufanymi certyfikatami
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.trustManagers, null)

    // Zbuduj OkHttpClient
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        .hostnameVerifier { hostname, session ->
            // Akceptuj "localhost" lub "10.0.2.2" bez weryfikacji
            hostname == "localhost" || hostname == "10.0.2.2"
        }
}
