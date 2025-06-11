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
data class Keys(
    @SerializedName("PublicKey")
    val public_key: Content,

    @SerializedName("PrivateKey")
    val private_key: Content)

data class KeysDecrypted(
    val public_key: String,
    val private_key: String)

data class PublicKeyRequest(val username: String)
//data class PublicKeyResponse(val public_key: String)
data class PublicKeyResponse(
    val keys: BufferWrapper?
)

data class BufferWrapper(
    val type: String,
    val data: List<Int>
)

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

data class KeysResponse(
    @SerializedName("keys")
    val keys: Keys
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
