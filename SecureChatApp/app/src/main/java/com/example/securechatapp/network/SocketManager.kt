package com.example.securechatapp.network

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.securechatapp.model.Content
import com.example.securechatapp.model.Message
import getOkHttpClientWithCert
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.CookieManager
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.util.UUID

object SocketManager {
    private var socket: Socket? = null
    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    private val _messageCallbacks = mutableListOf<(Message) -> Unit>()

    fun initialize(context: Context, cookieManager: CookieManager) {
        var token = ""
        val uri = URI.create("https://10.0.2.2:5000")
        val cookies = cookieManager.cookieStore.get(uri)
        for (cookie in cookies) {
            if (cookie.name == "token") {
                token = cookie.value
                Log.d("SocketManager", "JWT Token: $token")
            }
        }

        try {
            val options = IO.Options().apply {
                secure = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                auth = mapOf("token" to token)
                transports = arrayOf("websocket")
            }

            val okHttpClient = getOkHttpClientWithCert(context).build()
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
            IO.setDefaultOkHttpCallFactory(okHttpClient)

            socket = IO.socket("wss://10.0.2.2:5000", options)
            setupBaseListeners()
            socket?.connect()

        } catch (e: URISyntaxException) {
            Log.e("SocketManager", "URI Syntax Error", e)
            _connectionStatus.postValue(false)
        } catch (e: Exception) {
            Log.e("SocketManager", "Initialization Error", e)
            _connectionStatus.postValue(false)
        }
    }

    private fun setupBaseListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("SocketManager", "Connected to server")
            _connectionStatus.postValue(true)

            socket?.off("message")
            socket?.on("message") { args ->
                Log.d("SocketManager", "Received message: ${args.contentToString()}")
                try {
                    val messageJson = args[0] as? JSONObject ?: return@on
                    val message = parseMessage(messageJson)
                    _messageCallbacks.forEach { callback ->
                        try {
                            callback(message)
                        } catch (e: Exception) {
                            Log.e("SocketManager", "Callback error", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SocketManager", "Message parsing error", e)
                }
            }
        }

        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d("SocketManager", "Disconnected: ${args.contentToString()}")
            _connectionStatus.postValue(false)
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SocketManager", "Connection error: ${args.contentToString()}")
            _connectionStatus.postValue(false)
        }
    }

    fun setupMessageListener(callback: (Message) -> Unit) {
        _messageCallbacks.add(callback)
    }

    fun removeMessageListener(callback: (Message) -> Unit) {
        _messageCallbacks.remove(callback)
    }

    private fun parseMessage(json: JSONObject): Message {
        val content = if (json.has("content")) {
            val contentJson = json.getJSONObject("content")
            Content(
                type = contentJson.getString("type"),
                data = contentJson.getJSONArray("data").run {
                    List(length()) { getInt(it) }
                }
            )
        } else {
            Content(
                type = "text",
                data = json.optString("message", "").toByteArray().map { it.toInt() }
            )
        }

        return Message(
            messageId = json.optString("messageId", UUID.randomUUID().toString()),
            userId = json.optString("sender", "unknown"),
            conversationId = json.getString("conversationId"),
            content = content,
            sendAt = json.optString("sendAt", Instant.now().toString())
        )
    }

    fun sendMessage(conversationId: String, content: String) {
        if (!isConnected()) {
            Log.e("SocketManager", "Attempt to send message without connection")
            return
        }

        try {
            val messageData = JSONObject().apply {
                put("conversationId", conversationId)
                put("content", content)
            }
            Log.d("SocketManager", "Sending message: $messageData")
            socket?.emit("message", messageData, object : io.socket.client.Ack {
                override fun call(vararg args: Any?) {
                    if (args.isEmpty() || args[0] == null) {
                        Log.d("SocketManager", "Message acknowledged by server")
                    } else {
                        Log.e("SocketManager", "Delivery error: ${args.joinToString { it.toString() }}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("SocketManager", "Message sending error", e)
        }
    }

    fun disconnect() {
        socket?.off("message")
        socket?.disconnect()
        socket = null
        _messageCallbacks.clear()
        Log.d("SocketManager", "Socket disconnected and cleaned up")
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}