package com.example.securechatapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securechatapp.databinding.ActivityMainBinding
import com.example.securechatapp.model.AuthViewModel
import com.example.securechatapp.model.ConversationViewModel
import com.example.securechatapp.model.CreateConversationRequest
import com.example.securechatapp.model.KeysDecrypted
import com.example.securechatapp.model.KeysViewModel
import com.example.securechatapp.network.ApiClient
import com.example.securechatapp.piv.PivGetPublicKeyContract
import com.yubico.yubikit.piv.Slot
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

//private const val PREF_PUBLIC_KEY = "PUBLIC_KEY"


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: ConversationViewModel by viewModels()
    private lateinit var adapter: ConversationsAdapter
    private val keysViewModel: KeysViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setSupportActionBar(binding.toolbar)



        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        viewModel.loadConversations()
    }

    /*override fun onPause() {
        savePrivateKeyU2F()
        super.onPause()
    }

    override fun onResume() {
        loadPublicKeyU2F()
        super.onResume()
    }
*/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authViewModel.logout()
                navigateToLogin()
                true
            }
            R.id.action_delete -> {
                showDeleteAccountDialog()
                true
            }
            R.id.action_update -> {
                startActivity(Intent(this, UpdateCredentialsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setupRecyclerView() {
        adapter = ConversationsAdapter { conversationId, conversationKey ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("conversationId", conversationId)
            intent.putExtra("conversationKey", conversationKey)
            startActivity(intent)
        }

        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.conversations.observe(this) { conversations ->

            adapter.submitList(conversations)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                binding.tvError.text = it
                binding.tvError.visibility = View.VISIBLE
            } ?: run {
                binding.tvError.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {



        binding.btnNewChat.setOnClickListener {
            showNewChatDialog()
            //startActivity(Intent(this, NewChatActivity::class.java))
        }
        //binding.btnLogout.setOnClickListener {
        //    authViewModel.logout()
        //    navigateToLogin()
        //}
    }

    private fun showNewChatDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val usernameInput = EditText(this).apply {
            hint = "Nazwa użytkownika"
        }

        val passwordInput = EditText(this).apply {
            hint = "Twoje hasło (do odszyfrowania klucza)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(usernameInput)
        layout.addView(passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Nowa konwersacja")
            .setView(layout)
            .setPositiveButton("Utwórz") { _, _ ->
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString()
                if (username.isNotBlank() && password.isNotBlank()) {
                    //createNewConversation(username, password)
                    //TODO
                } else {
                    Toast.makeText(this, "Wprowadź dane", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }


    //TODO: deszyfrowanie nie bardzo działa
    /*private fun createNewConversation(recipientUsername: String, password: String) {
        // 1. Załaduj i odszyfruj klucze użytkownika oraz publiczny klucz odbiorcy
        keysViewModel.loadAndDecryptKeys(password)
        keysViewModel.loadRecipientPublicKey(recipientUsername)

        val combined = MediatorLiveData<Pair<KeysDecrypted?, String?>>().apply {
            var ownKeys: KeysDecrypted? = null
            var recipientKey: String? = null

            addSource(keysViewModel.ownKeys) {
                ownKeys = it
                value = Pair(ownKeys, recipientKey)
            }
            addSource(keysViewModel.recipientPublicKey) {
                recipientKey = it
                value = Pair(ownKeys, recipientKey)
            }
        }

        combined.observe(this) { (ownKeys, recipientPublicKeyPem) ->
            if (ownKeys != null && recipientPublicKeyPem != null) {
                combined.removeObservers(this)

                try {
                    // 2. Generuj losowy klucz AES do szyfrowania rozmowy
                    val aesKey = generateAESKey()

                    // 3. Szyfruj klucz AES publicznymi kluczami (dla siebie i odbiorcy)
                    val encryptedForMe = encryptWithRSAKey(aesKey.encoded, ownKeys.public_key)
                    val encryptedForRecipient = encryptWithRSAKey(aesKey.encoded, recipientPublicKeyPem)

                    // 4. Twórz zapytanie do API
                    val request = CreateConversationRequest(
                        userToAdd = recipientUsername,
                        keyMine = android.util.Base64.encodeToString(encryptedForMe, android.util.Base64.NO_WRAP),
                        keyOther = android.util.Base64.encodeToString(encryptedForRecipient, android.util.Base64.NO_WRAP)
                    )


                    lifecycleScope.launch {
                        val response = ApiClient.getService().createConversation(request)
                        if (response.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Konwersacja utworzona", Toast.LENGTH_SHORT).show()
                            viewModel.loadConversations()
                        } else {
                            Toast.makeText(this@MainActivity, "Błąd: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Błąd podczas tworzenia konwersacji", e)
                    Toast.makeText(this, "Błąd szyfrowania", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    private fun encryptWithRSAKey(data: ByteArray, publicKeyPEM: String): ByteArray {
        val publicKeyClean = publicKeyPEM
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.decode(publicKeyClean, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }*/


    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usuń konto")
            .setMessage("Czy na pewno chcesz usunąć swoje konto? Tej operacji nie można cofnąć.")
            .setPositiveButton("Usuń") { _, _ ->
                // TODO: wyślij DELETE request
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }


    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }



    /*private fun loadPublicKeyU2F(){
        getPreferences(MODE_PRIVATE).getString(PREF_PUBLIC_KEY, null)?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            val kf = KeyFactory.getInstance("RSA")
            val spec = X509EncodedKeySpec(bytes)
            authViewModel.publicKey = kf.generatePublic(spec)
        }
    }*/

    /*private fun savePrivateKeyU2F(){
        authViewModel.publicKey?.let {
            getPreferences(MODE_PRIVATE).edit()
                .putString(PREF_PUBLIC_KEY, Base64.encodeToString(it.encoded, Base64.DEFAULT))
                .apply()
        }
    }*/


}