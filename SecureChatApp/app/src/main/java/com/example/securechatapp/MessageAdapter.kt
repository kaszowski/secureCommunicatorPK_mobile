package com.example.securechatapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securechatapp.databinding.ItemMessageBinding
import com.example.securechatapp.model.Message
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import android.util.Base64
import com.example.securechatapp.network.ApiClient
import com.example.securechatapp.utils.CryptoUtils
import java.net.URI

class MessagesAdapter(
    private val decryptedSymetricKey: ByteArray,
    private val onItemClick: (Message) -> Unit = {}
) : ListAdapter<Message, MessagesAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.apply {

                val isCurrentUser = message.userId == ApiClient.getUserId()

                if (isCurrentUser) {
                    layoutSender.visibility = View.VISIBLE
                    layoutReceiver.visibility = View.GONE

                    val bytes = message.content?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                    val text = String(bytes) // Domyślnie UTF-8


                    tvSenderContent.text = decryptMessage(text, decryptedSymetricKey)

                    tvSenderTime.text = formatTime(message.sendAt)
                } else {
                    layoutReceiver.visibility = View.VISIBLE
                    layoutSender.visibility = View.GONE

                    val bytes = message.content?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                    val text = String(bytes) // Domyślnie UTF-8

                    Log.d("Tekst przed deszyfracja", text)
                    Log.d("Tekst po deszyfracji", decryptMessage(text, decryptedSymetricKey))
                    tvReceiverContent.text = decryptMessage(text, decryptedSymetricKey)

                    tvReceiverTime.text = formatTime(message.sendAt)
                }
            }
        }

        private fun formatTime(timestamp: String): String {
            return try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val messageDate = isoFormat.parse(timestamp) ?: return ""

                val now = Calendar.getInstance()
                val messageCal = Calendar.getInstance().apply { time = messageDate }

                return when {
                    isSameDay(now, messageCal) -> {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
                    }
                    isYesterday(now, messageCal) -> {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(messageDate)
                    }
                    else -> {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(messageDate)
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }

    } // Tutaj było brakujące zamknięcie klasy ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) =
            oldItem.messageId == newItem.messageId

        override fun areContentsTheSame(oldItem: Message, newItem: Message) =
            oldItem == newItem
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, date)
    }

    private fun decryptMessage(encryptedMessage: String, key: ByteArray): String {
        return CryptoUtils.decrypt(encryptedMessage, key)
    }

}