package com.example.securechatapp

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securechatapp.databinding.ItemConversationBinding
import com.example.securechatapp.model.Conversation
import java.text.SimpleDateFormat
import java.util.*

class ConversationsAdapter(
    private val onItemClick: (String, ByteArray) -> Unit
) : ListAdapter<Conversation, ConversationsAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            binding.apply {
                //Log.e("konwersacja:", conversation.toString())
                tvConversationName.text = conversation.name ?: "Chat"

                // Ustaw avatar jeśli istnieje
                conversation.avatar?.let {
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    ivAvatar.setImageBitmap(bitmap)
                } ?: run {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar) // domyślna ikona
                }

                root.setOnClickListener {
                    val key = conversation.conversationKey?.data?.map { it.toByte() }?.toByteArray() ?: byteArrayOf()
                    onItemClick(conversation.id, key)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation) =
            oldItem == newItem
    }
}
