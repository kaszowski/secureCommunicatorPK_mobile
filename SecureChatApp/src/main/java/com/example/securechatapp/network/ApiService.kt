package com.example.securechatapp.network

import com.example.securechatapp.model.Conversation
import com.example.securechatapp.model.CreateConversationRequest
import com.example.securechatapp.model.KeysResponse
import com.example.securechatapp.model.LoginRequest
import com.example.securechatapp.model.LoginResponse
import com.example.securechatapp.model.Message
import com.example.securechatapp.model.MessagesRequest
import com.example.securechatapp.model.PublicKeyRequest
import com.example.securechatapp.model.PublicKeyResponse
import com.example.securechatapp.model.RegisterRequest
import com.example.securechatapp.model.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Authentication
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("logout")
    suspend fun logout(): Response<Unit>

    @POST("refresh/token")
    suspend fun refreshToken(): Response<LoginResponse>

    @POST("refresh/token")
    fun refreshTokenSync(): retrofit2.Call<LoginResponse>

    // Keys
    @GET("keys")
    suspend fun getKeys(): Response<KeysResponse>

    @POST("key/public")
    suspend fun getPublicKey(@Body request: PublicKeyRequest): Response<PublicKeyResponse>

    // Conversations
    @GET("conversations")
    suspend fun getConversations(): Response<List<Conversation>>

    @POST("messages")
    suspend fun getMessages(@Body request: MessagesRequest): Response<List<Message>>

    @POST("conversation/create")
    suspend fun createConversation(@Body request: CreateConversationRequest): Response<Unit>

    // Profile
    @POST("update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Unit>
}