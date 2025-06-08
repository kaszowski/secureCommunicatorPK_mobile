package com.example.securechatapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securechatapp.databinding.ActivityChatBinding
import com.example.securechatapp.model.ChatViewModel
import com.example.securechatapp.model.Message
import com.example.securechatapp.utils.CryptoUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessagesAdapter
    private var conversationId: String = ""
    private var symmetricKey: ByteArray = ByteArray(0) // Pobierany z bazy danych

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getStringExtra("conversationId") ?: run {
            finish()
            return
        }

        // TODO: Pobierz klucz symetryczny dla tej konwersacji
        // symmetricKey = pobierzKluczDlaKonwersacji(conversationId)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        viewModel.loadMessages(conversationId)
        Log.e("ChatActivity", viewModel.loadMessages(conversationId).toString())
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter { message ->
            // TODO: Obsługa kliknięcia na wiadomość jeśli potrzebna
        }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            binding.rvMessages.scrollToPosition(messages.size - 1)
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
        binding.btnSend.setOnClickListener {
            val messageContent = binding.etMessage.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                val encryptedMessage = encryptMessage(messageContent, symmetricKey)
                viewModel.sendMessage(conversationId, encryptedMessage)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun encryptMessage(message: String, key: ByteArray): ByteArray {
        return CryptoUtils.encrypt(message.toByteArray(), key)
    }

    private fun decryptMessage(encryptedMessage: ByteArray, key: ByteArray): String {
        return String(CryptoUtils.decrypt(encryptedMessage, key))
    }
}