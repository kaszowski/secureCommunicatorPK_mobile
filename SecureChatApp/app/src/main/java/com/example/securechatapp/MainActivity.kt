package com.example.securechatapp

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securechatapp.databinding.ActivityMainBinding
import com.example.securechatapp.model.AuthViewModel
import com.example.securechatapp.model.ConversationViewModel
import com.example.securechatapp.piv.PivGetPublicKeyContract
import com.yubico.yubikit.piv.Slot
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

//private const val PREF_PUBLIC_KEY = "PUBLIC_KEY"


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: ConversationViewModel by viewModels()
    private lateinit var adapter: ConversationsAdapter


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
            //startActivity(Intent(this, NewChatActivity::class.java))
        }
        //binding.btnLogout.setOnClickListener {
        //    authViewModel.logout()
        //    navigateToLogin()
        //}
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