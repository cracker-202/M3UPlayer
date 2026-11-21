package com.example.m3uplayer.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaylistException(message: String) : RuntimeException(message)

class ChannelRepository(
    private val client: OkHttpClient,
    context: Context
) {
    private val cache = PlaylistCache(context)

    suspend fun load(
        source: String,
        forceRefresh: Boolean = false
    ): List<Channel> = withContext(Dispatchers.IO) {

        val cached = cache.read(source)

        if (!forceRefresh && cached != null && cache.isFresh(source)) {
            return@withContext cached
        }

        try {
            val request = Request.Builder()
                .url(source)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw PlaylistException(
                        "El servidor respondió con el error ${response.code}."
                    )
                }

                val body = response.body?.string().orEmpty()
                val channels = M3uParser.parse(body)

                if (channels.isEmpty()) {
                    throw PlaylistException(
                        "No se encontraron canales en la lista M3U."
                    )
                }

                try {
                    cache.write(source, channels)
                } catch (_: Exception) {
                    // Si falla la escritura, conservamos la lista descargada.
                }

                channels
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            cached ?: throw e
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 9; AFTMM) ZorrilloTV/1.1"
    }
}
