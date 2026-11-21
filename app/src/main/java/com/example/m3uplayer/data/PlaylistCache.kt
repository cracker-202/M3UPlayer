package com.example.m3uplayer.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class PlaylistCache(context: Context) {

    private val directory = File(context.filesDir, "playlist_cache")
        .apply { mkdirs() }

    private fun cacheFile(source: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        return File(directory, "$hash.json")
    }

    fun read(source: String): List<Channel>? {
        val file = cacheFile(source)
        if (!file.exists()) return null

        return try {
            val root = JSONObject(file.readText())
            val items = root.getJSONArray("channels")

            val channels = ArrayList<Channel>(items.length())

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)

                channels.add(
                    Channel(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        url = item.getString("url"),
                        logo = if (item.isNull("logo")) null
                               else item.getString("logo"),
                        group = item.getString("group")
                    )
                )
            }

            channels.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    fun isFresh(source: String): Boolean {
        val file = cacheFile(source)
        return file.exists() &&
            System.currentTimeMillis() - file.lastModified() in 0 until TTL_MS
    }

    fun write(source: String, channels: List<Channel>) {
        if (channels.isEmpty()) return

        val array = JSONArray()

        channels.forEach { channel ->
            array.put(
                JSONObject().apply {
                    put("id", channel.id)
                    put("name", channel.name)
                    put("url", channel.url)
                    put("logo", channel.logo)
                    put("group", channel.group)
                }
            )
        }

        val data = JSONObject()
            .put("channels", array)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val atomicFile = AtomicFile(cacheFile(source))
        val stream = atomicFile.startWrite()

        try {
            stream.write(data)
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw e
        }
    }

    companion object {
        const val TTL_MS = 30 * 60 * 1000L
    }
}
