package com.keepfit.feature.assistant.access

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface OAuthCallbackServer {
    suspend fun start(
        state: String,
        preferredPort: Int? = null,
        onCallback: suspend (String) -> Unit,
        onFailure: suspend (Throwable) -> Unit = {},
    ): Result<String>

    fun stop()
}

@Singleton
class LoopbackOAuthCallbackServer @Inject constructor() : OAuthCallbackServer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    override suspend fun start(
        state: String,
        preferredPort: Int?,
        onCallback: suspend (String) -> Unit,
        onFailure: suspend (Throwable) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            stop()
            val socket = ServerSocket(
                preferredPort ?: 0,
                1,
                InetAddress.getByName("127.0.0.1"),
            ).apply { soTimeout = CALLBACK_TIMEOUT_MILLIS }
            serverSocket = socket
            val callbackUrl = "http://127.0.0.1:${socket.localPort}/oauth/callback/$state"
            serverJob = scope.launch {
                runCatching {
                    socket.accept().use { client ->
                        val requestTarget = readRequestTarget(client)
                        val fullUrl = "http://127.0.0.1:${socket.localPort}$requestTarget"
                        val accepted = runCatching { URI(fullUrl) }.isSuccess
                        writeBrowserResponse(client, accepted)
                        if (accepted) onCallback(fullUrl)
                    }
                }.onFailure { exception ->
                    if (serverSocket === socket && !socket.isClosed) {
                        onFailure(exception)
                    }
                }
                stop()
            }
            callbackUrl
        }
    }

    override fun stop() {
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun readRequestTarget(client: Socket): String {
        client.soTimeout = 5_000
        val requestLine = client.getInputStream().bufferedReader(Charsets.US_ASCII).readLine().orEmpty()
        val pieces = requestLine.split(' ')
        if (pieces.size < 2 || pieces[0] != "GET" || pieces[1].length > 8_192) {
            error("Invalid OAuth callback request.")
        }
        return pieces[1]
    }

    private fun writeBrowserResponse(client: Socket, accepted: Boolean) {
        val message = if (accepted) {
            "Authorization received. Return to Keepfit to finish connecting."
        } else {
            "Authorization could not be read. Return to Keepfit and try again."
        }
        val body = "<!doctype html><meta name=viewport content=\"width=device-width\"><title>Keepfit</title><p>$message</p>"
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.US_ASCII)
        client.getOutputStream().use { output ->
            output.write(headers)
            output.write(bytes)
            output.flush()
        }
    }

    private companion object {
        const val CALLBACK_TIMEOUT_MILLIS = 10 * 60 * 1_000
    }
}
