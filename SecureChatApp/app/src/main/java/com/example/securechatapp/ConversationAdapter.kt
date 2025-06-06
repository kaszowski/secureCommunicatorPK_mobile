package com.example.securechatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securechatapp.databinding.ItemConversationBinding
import com.example.securechatapp.model.Conversation

class ConversationsAdapter : ListAdapter<Conversation, ConversationsAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.binding.apply {
            // Upewnij się, że te ID istnieją w Twoim layoutcie item_conversation.xml
            tvConversationId.text = "Conversation: ${conversation.conversationId}"
            tvParticipants.text = "Name: ${conversation.name}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation) =
            oldItem.conversationId == newItem.conversationId
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation) =
            oldItem == newItem
    }
}