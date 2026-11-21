package com.example.m3uplayer.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class CatalogCache(
    context: Context,
    private val client: OkHttpClient
) {
    private val file = File(
        context.filesDir,
        "zorrillo_catalog.m3u"
    )

    private val ttl = 24 * 60 * 60 * 1000L
    // El ViewModel conserva las películas y series clasificadas.
    // Evitamos duplicar el catálogo completo en memoria.

    suspend fun load(source: String): List<Channel> =
        withContext(Dispatchers.IO) {

            val fresh = file.exists() &&
                System.currentTimeMillis() -
                file.lastModified() in 0 until ttl

            if (!fresh) {
                try {
                    val request = Request.Builder()
                        .url(source)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw PlaylistException(
                                "Error HTTP ${response.code}"
                            )
                        }

                        val body = response.body
                            ?: throw PlaylistException("Catalogo vacio")

                        val temp = File(
                            file.parentFile,
                            "${file.name}.tmp"
                        )

                        try {
                            body.byteStream().use { input ->
                                temp.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            val valid = temp.bufferedReader().use { reader ->
                                reader.readLine()
                                    ?.trim()
                                    ?.trimStart('\\uFEFF')
                                    ?.startsWith("#EXTM3U", ignoreCase = true) == true
                            }

                            if (!valid) {
                                throw PlaylistException("Formato M3U invalido")
                            }

                            if (!temp.renameTo(file)) {
                                throw PlaylistException("No se pudo guardar el catalogo")
                            }
                        } finally {
                            if (temp.exists()) temp.delete()
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    if (!file.exists()) throw e
                }
            }

            file.bufferedReader().use { reader ->
                M3uParser.parseLines(reader.lineSequence())
            }
        }
}
