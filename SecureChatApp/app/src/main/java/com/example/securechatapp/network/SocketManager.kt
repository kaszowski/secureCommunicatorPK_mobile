package com.example.securechatapp.network

import android.content.Context
import android.util.Log
import com.example.securechatapp.model.Content
import com.example.securechatapp.model.Message
import getOkHttpClientWithCert
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.CookieManager
import java.net.URI
import java.net.URISyntaxException

object SocketManager {
    private var socket: Socket? = null

    fun initialize(context: Context, cookieManager: CookieManager) {
        var token = ""
        val uri = URI.create("https://10.0.2.2:5000")
        val cookies = cookieManager.cookieStore.get(uri)
        for (cookie in cookies) {
            if (cookie.name == "token") {
                token = cookie.value
                Log.d("JWT", "Token: $token")
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
            Log.e("SocketIO", "Błąd inicjalizacji socketu", e)
        }
    }

    private fun setupBaseListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("SocketIO", "Połączono z serwerem")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d("SocketIO", "Rozłączono z serwerem")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SocketIO", "Błąd połączenia: ${args.firstOrNull()}")
        }
    }

    fun setupMessageListener(callback: (Message) -> Unit) {
        socket?.on("message") { args ->
            try {
                val messageJson = args[0] as? JSONObject ?: return@on
                val message = parseMessage(messageJson)
                callback(message)
            } catch (e: Exception) {
                Log.e("SocketIO", "Błąd parsowania wiadomości", e)
            }
        }
    }

    private fun parseMessage(json: JSONObject): Message {
        val contentJson = json.getJSONObject("content")
        return Message(
            messageId = json.getString("messageId"),
            userId = json.getString("userId"),
            conversationId = json.getString("conversationId"),
            content = Content(
                type = contentJson.getString("type"),
                data = contentJson.getJSONArray("data").run {
                    List(length()) { getInt(it) }
                }
            ),
            sendAt = json.getString("sendAt")
        )
    }

    fun sendMessage(conversationId: String, encryptedContent: ByteArray) {
        try {
            val messageData = JSONObject().apply {
                put("conversationId", conversationId)
                put("content", JSONObject().apply {
                    put("type", "Buffer")
                    put("data", encryptedContent.map { it.toInt() })
                })
            }
            socket?.emit("message", messageData)
        } catch (e: Exception) {
            Log.e("SocketIO", "Błąd wysyłania wiadomości", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}