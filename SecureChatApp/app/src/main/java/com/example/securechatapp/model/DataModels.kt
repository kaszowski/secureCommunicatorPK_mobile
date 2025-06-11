package com.example.securechatapp.model

import com.google.gson.annotations.SerializedName

// Authentication
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val token_expiry: Long)

data class RegisterRequest(
    val email: String,
    val username: String,
    val password_hash: String,
    val public_key: String,
    val private_key: String? = null
)

// Keys
data class KeysResponse(val public_key: String, val private_key: String)
data class PublicKeyRequest(val username: String)
data class PublicKeyResponse(val public_key: String)

// Conversations
data class Conversation(
    @SerializedName("ConversationId")
    val id: String,

    @SerializedName("Name")
    val name: String? = null,

    @SerializedName("Avatar")
    val avatar: ByteArray? = null,

    @SerializedName("Background")
    val background: ByteArray? = null
)


data class MessagesRequest(
    val conversationId: String,
    val limit: Int = 50,
    val offset: Int = 0
)

data class Message(
    val id: String,
    val conversation_id: String,
    val sender: String,
    val content: String,
    val timestamp: Long
)

data class CreateConversationRequest(
    val userToAdd: String,
    val keyMine: String,
    val keyOther: String
)

// Profile
data class UpdateProfileRequest(
    val updates: UpdateData
)

data class UpdateData(
    val username: String? = null,
    val usernameShow: String? = null,
    val email: String? = null,
    val newPassword: String? = null,
    val currentPassword: String
)

data class ConversationsResponse(
    @SerializedName("conversations")
    val conversations: List<Conversation>
)