package com.xraylink.client.data

import com.xraylink.client.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class WsSyncEvent(val type: String)

@Singleton
class WebSocketSyncClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenStore: EncryptedTokenStore
) {
    private var socket: WebSocket? = null
    private val _events = MutableSharedFlow<WsSyncEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<WsSyncEvent> = _events

    fun connect() {
        val token = tokenStore.getAccessToken() ?: return
        disconnect()
        val encoded = URLEncoder.encode(token, "UTF-8")
        val req = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}?token=$encoded")
            .build()

        socket = okHttpClient.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val type = JSONObject(text).optString("type", "")
                    if (type.isNotBlank() && type != "pong" && type != "connected") {
                        _events.tryEmit(WsSyncEvent(type))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Next sync occurs on reconnect or user refresh
            }
        })
    }

    fun disconnect() {
        socket?.close(1000, "Client closing")
        socket = null
    }
}
