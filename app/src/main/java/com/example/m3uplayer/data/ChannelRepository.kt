package com.example.m3uplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaylistException(message: String) : RuntimeException(message)

class ChannelRepository(private val client: OkHttpClient) {

    suspend fun load(source: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(source)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw PlaylistException("El servidor respondió con el error ${response.code}.")
            }
            val body = response.body?.string().orEmpty()
            val channels = M3uParser.parse(body)
            if (channels.isEmpty()) {
                throw PlaylistException(
                    "No se encontraron canales. Comprueba que la dirección sea una lista M3U válida."
                )
            }
            channels
        }
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 9; AFTMM) ZorrilloTV/1.1"
    }
}
