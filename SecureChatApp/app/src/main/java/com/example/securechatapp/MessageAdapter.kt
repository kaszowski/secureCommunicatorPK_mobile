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
import java.net.URI

class MessagesAdapter(
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

                    tvSenderContent.text = text

                    tvSenderTime.text = formatTime(message.sendAt)
                } else {
                    layoutReceiver.visibility = View.VISIBLE
                    layoutSender.visibility = View.GONE

                    val bytes = message.content?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                    val text = String(bytes) // Domyślnie UTF-8

                    tvReceiverContent.text = text

                    tvReceiverTime.text = formatTime(message.sendAt)
                }
            }
        }

        private fun formatTime(timestamp: String): String {
            return try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = isoFormat.parse(timestamp)

                val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                date?.let { displayFormat.format(it) } ?: ""
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

}