package com.example.securechatapp

import android.content.Intent
import android.os.Bundle
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
    private val conversationViewModel: ConversationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        loadConversations()
    }

    private fun setupUI() {
        binding.rvConversations.layoutManager = LinearLayoutManager(this)
        // Tutaj należy dodać adapter gdy będzie gotowy
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            navigateToLogin()
        }

        binding.btnRefresh.setOnClickListener {
            loadConversations()
        }
    }

    private fun loadConversations() {
        lifecycleScope.launch {
            conversationViewModel.loadConversations()
            // Tutaj należy obsłużyć wyniki gdy ViewModel będzie gotowy
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

}