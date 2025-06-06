package com.example.securechatapp.model

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
    val conversationId: String,
    val name: String? = null,
    val avatar: ByteArray? = null,
    val background: ByteArray? = null,
//    val status: Int = 0,
//    val statusTimestamp: Long
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
    val updates: UpdateData,
    val old_password_hash: String
)

data class UpdateData(
    val username: String? = null,
    val username_show: String? = null,
    val email: String? = null,
    val new_password: String? = null
)