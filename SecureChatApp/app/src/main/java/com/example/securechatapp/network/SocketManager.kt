package com.example.securechatapp.network

import android.util.Log
import com.example.securechatapp.model.Content
import com.example.securechatapp.model.Message
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {
    private var socket: Socket? = null

    fun initializeSocket(token: String) {
        try {
            val options = IO.Options().apply {
                secure = true
                reconnection = true
                //withCredentials = true
                auth = mapOf("token" to token)
            }

            socket = IO.socket("https://10.0.2.2:5000", options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Połączono")
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketIO", "Rozłączono")
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e("SocketIO", "Błąd inicjalizacji socketu", e)
        }
    }

    fun setupMessageListener(callback: (Message) -> Unit) {
        socket?.on("message") { args ->
            try {
                val messageJson = args[0] as? JSONObject
                messageJson?.let {
                    val contentJson = it.getJSONObject("content")
                    val dataArray = contentJson.getJSONArray("data")

                    val dataList = mutableListOf<Int>()
                    for (i in 0 until dataArray.length()) {
                        dataList.add(dataArray.getInt(i))
                    }

                    val content = Content(
                        type = contentJson.getString("type"),
                        data = dataList
                    )

                    val message = Message(
                        messageId = it.getString("messageId"),
                        userId = it.getString("userId"),
                        conversationId = it.getString("conversationId"),
                        content = content,
                        sendAt = it.getString("sendAt")
                    )
                    callback(message)
                }
            } catch (e: Exception) {
                Log.e("SocketIO", "Błąd parsowania wiadomości", e)
            }
        }
    }


    fun sendMessage(conversationId: String, encryptedContent: ByteArray) {
        val messageData = JSONObject().apply {
            put("conversationId", conversationId)
            put("content", String(encryptedContent))
        }
        socket?.emit("message", messageData)
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}