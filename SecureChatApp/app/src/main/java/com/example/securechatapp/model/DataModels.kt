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
    val background: ByteArray? = null,

    @SerializedName("EncryptedConversationKey")
    val conversationKey : Content
)


data class Message(
    @SerializedName("MessageId")
    val messageId: String,

    @SerializedName("UserId")
    val userId: String,

    @SerializedName("ConversationId")
    val conversationId: String,

    @SerializedName("Content")
    val content: Content,

    @SerializedName("SendAt")
    val sendAt: String
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

data class ConversationsResponse(
    @SerializedName("conversations")
    val conversations: List<Conversation>
)

data class MessageRequest(
    val conversationId: String,
    val limit: Int,
    val offset: Int
)

data class MessageResponse(
    val messages: List<Message>
)

data class Content(

    @SerializedName("type")
    val type: String,

    @SerializedName("data")
    val data: List<Int>
)
