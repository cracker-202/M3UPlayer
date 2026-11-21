package com.example.m3uplayer.data

object CatalogClassifier {

    fun isSeries(channel: Channel): Boolean {
        val group = channel.group.lowercase()
        return listOf(
            "serie", "series", "teleserie",
            "novela", "anime", "temporada",
            "episodio", "capitulo"
        ).any { it in group }
    }

    fun isMovie(channel: Channel): Boolean {
        val group = channel.group.lowercase()
        return !isSeries(channel) &&
            listOf(
                "vod", "pelicula", "película",
                "filme", "movie", "cine",
                "estreno"
            ).any { it in group }
    }

    fun movies(channels: List<Channel>): List<Channel> =
        channels.filter(::isMovie)

    fun series(channels: List<Channel>): List<Channel> =
        channels.filter(::isSeries)
}
