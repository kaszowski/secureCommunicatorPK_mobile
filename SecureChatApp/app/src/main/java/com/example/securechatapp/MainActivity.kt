package com.example.securechatapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securechatapp.databinding.ActivityMainBinding
import com.example.securechatapp.model.AuthViewModel
import com.example.securechatapp.model.ConversationViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: ConversationViewModel by viewModels()
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        viewModel.loadConversations()
    }

    private fun setupRecyclerView() {
        adapter = ConversationsAdapter { conversationId ->
            //openChatDetails(conversationId)
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
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            navigateToLogin()
        }
    }


    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

}