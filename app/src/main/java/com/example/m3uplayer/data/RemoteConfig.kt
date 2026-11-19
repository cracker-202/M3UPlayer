package com.example.m3uplayer.data

import org.json.JSONObject
import java.net.URI

data class RemotePlaylist(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean
)

data class RemoteConfig(
    val playlists: List<RemotePlaylist>,
    val defaultPlaylistId: String
) {
    companion object {
        fun parse(json: JSONObject): RemoteConfig {
            val array = json.getJSONArray("playlists")
            require(array.length() in 1..30)

            val lists = (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)

                RemotePlaylist(
                    id = item.getString("id"),
                    name = item.getString("name").trim(),
                    url = item.getString("url").trim(),
                    enabled = item.getBoolean("enabled")
                )
            }

            require(lists.map { it.id }.distinct().size == lists.size)

            lists.forEach {
                require(it.id.matches(Regex("[a-zA-Z0-9_-]{1,64}")))
                require(it.name.isNotBlank() && it.name.length <= 100)
                require(it.url.length <= 4096)

                val uri = URI(it.url.replace(" ", "%20"))
                require(uri.scheme in listOf("http", "https"))
                require(!uri.host.isNullOrBlank())
            }

            val defaultId = json.getString("defaultPlaylistId")

            require(lists.any {
                it.id == defaultId && it.enabled
            })

            return RemoteConfig(
                playlists = lists.filter { it.enabled },
                defaultPlaylistId = defaultId
            )
        }
    }
}
