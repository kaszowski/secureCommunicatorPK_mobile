package com.example.securechatapp

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securechatapp.databinding.ActivityMainBinding
import com.example.securechatapp.model.AuthViewModel
import com.example.securechatapp.model.ConversationViewModel
import com.example.securechatapp.model.KeysViewModel
import com.example.securechatapp.network.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

        layout.addView(usernameInput)

        AlertDialog.Builder(this)
            .setTitle("Nowa konwersacja")
            .setView(layout)
            .setPositiveButton("Utwórz") { _, _ ->
                val username = usernameInput.text.toString().trim()
                val loggedInUsername = ApiClient.loggedInUsername

                if (loggedInUsername == null) {
                    Toast.makeText(this, "Nie zalogowano użytkownika", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (username.isNotBlank()) {
                    // 1. Załaduj klucz publiczny odbiorcy
                    keysViewModel.loadRecipientPublicKey(username)
                    keysViewModel.loadOwnPublicKey(loggedInUsername)

                    // 2. Poczekaj chwilę i sprawdź, czy klucze są dostępne
                    lifecycleScope.launch {
                        repeat(10) { attempt ->
                            val ownPublicKey = keysViewModel.ownPublicKey.value
                            val recipientKey = keysViewModel.recipientPublicKey.value
                            Log.d("NewChatDialog", "Attempt $attempt: ownKey = $ownPublicKey, recipientKey = $recipientKey")

                            if (ownPublicKey != null && recipientKey != null) {
                                // 3. Utwórz konwersację
                                viewModel.createConversation(
                                    userToAdd = username,
                                    ownPublicKey = ownPublicKey,
                                    otherPublicKey = recipientKey
                                )
                                Toast.makeText(this@MainActivity, "Tworzenie konwersacji...", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            delay(400L) // Poczekaj 200 ms i sprawdź jeszcze raz
                        }

                        // Po 2 sekundach nadal brak kluczy
                        Toast.makeText(this@MainActivity, "Nie udało się pobrać kluczy", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Wprowadź dane", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

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