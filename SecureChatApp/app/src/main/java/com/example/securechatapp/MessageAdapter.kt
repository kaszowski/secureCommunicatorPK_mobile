package com.example.securechatapp

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securechatapp.databinding.ItemMessageBinding
import com.example.securechatapp.model.Message
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private val onItemClick: (Message) -> Unit = {}
) : ListAdapter<Message, MessagesAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.apply {
                Log.e("TEST", message.content?.toString() ?: "content is null")
                val bytes = message.content?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                val text = String(bytes) // Domyślnie UTF-8

                tvMessageContent.text = text

                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = try {
                    isoFormat.parse(message.sendAt)
                } catch (e: Exception) {
                    null
                }

                val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvMessageTime.text = date?.let { displayFormat.format(it) } ?: ""

                root.setOnClickListener { onItemClick(message) }
            }
        }

    }

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