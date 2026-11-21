package com.example.m3uplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.m3uplayer.data.Channel

@Composable
fun HomeScreen(
    state: UiState,
    restoreTick: Int,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onChangePlaylist: () -> Unit,
    onRefresh: () -> Unit,
    adultsUnlocked: Boolean,
    onRequestAdultPin: () -> Unit,
) {
    val visibleChannels = remember(state.channels, adultsUnlocked) {
        if (adultsUnlocked) state.channels else state.channels.filterNot { isAdultChannel(it) }
    }
    val visibleState = remember(state, visibleChannels) { state.copy(channels = visibleChannels) }
    val focusMemory = remember { FocusMemory() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        Header(
            hasPlaylist = state.playlistUrl != null,
            channelCount = state.channels.size,
            onSearch = onSearch,
            onRefresh = onRefresh,
            onChangePlaylist = onChangePlaylist,
            onRequestAdultPin = onRequestAdultPin,
            adultsUnlocked = adultsUnlocked,
        )
        Spacer(Modifier.height(16.dp))

        when {
            !state.ready || state.loading -> CenteredLoading("Cargando canales…")
            state.error != null -> ErrorState(state.error, onRefresh, onChangePlaylist)
            state.playlistUrl == null -> EmptyState(onChangePlaylist)
            visibleState.channels.isEmpty() -> CenteredLoading("Cargando canales…")
            visibleState.query.isNotBlank() -> SearchResults(
                visibleState, focusMemory, restoreTick, onPlay, onToggleFavorite, onClearSearch
            )
            else -> ChannelRows(visibleState, focusMemory, restoreTick, onPlay, onToggleFavorite)
        }
    }
}

@Composable
private fun Header(
    hasPlaylist: Boolean,
    channelCount: Int,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onChangePlaylist: () -> Unit,
    onRequestAdultPin: () -> Unit,
    adultsUnlocked: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 6.dp, height = 32.dp)
                        .background(Accent, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "ZORRILLO TV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
            Text(
                "POWERED BY: skunk sv.",
                style = MaterialTheme.typography.bodySmall,
                color = Gold,
                modifier = Modifier.padding(start = 18.dp, top = 2.dp),
            )
            if (channelCount > 0) {
                Text(
                    "$channelCount canales · Mantén pulsado OK sobre un canal para marcarlo como favorito",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 18.dp, top = 4.dp),
                )
            }
        }
        Button(onClick = onRequestAdultPin) {
            Text(if (adultsUnlocked) "Adultos desbloqueados" else "Adultos · PIN")
        }
        Spacer(Modifier.width(12.dp))
        if (hasPlaylist) {
            Button(onClick = onSearch) { Text("Buscar") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onRefresh) { Text("Actualizar") }
            Spacer(Modifier.width(12.dp))
        }
        Button(onClick = onChangePlaylist) {
            Text(if (hasPlaylist) "Cambiar lista" else "Añadir lista")
        }
    }
}

internal fun isAdultChannel(channel: Channel): Boolean {
    val text = (channel.name + " " + channel.group).lowercase()
    val markers = listOf("adult", "xxx", "18+", "18 plus", "erotic", "erot", "porn", "sex", "hot")
    return markers.any { it in text }
}

@Composable
private fun ChannelRows(
    state: UiState,
    focusMemory: FocusMemory,
    restoreTick: Int,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
) {
    val groups = remember(state.channels) { state.channels.groupBy { it.group }.toList() }
    val favorites = remember(state.channels, state.favorites) {
        state.channels.filter { it.id in state.favorites }
    }
    val last = remember(state.channels, state.lastChannelId) {
        state.channels.firstOrNull { it.id == state.lastChannelId }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (last != null) {
            item(key = "row_last") {
                ChannelRow("Continuar viendo", "last", listOf(last), state.favorites,
                    focusMemory, restoreTick, onPlay, onToggleFavorite)
            }
        }
        if (favorites.isNotEmpty()) {
            item(key = "row_favs") {
                ChannelRow("Favoritos", "favs", favorites, state.favorites,
                    focusMemory, restoreTick, onPlay, onToggleFavorite)
            }
        }
        itemsIndexed(groups, key = { _, g -> "row_g_${g.first}" }) { _, (name, list) ->
            ChannelRow(name, "g_$name", list, state.favorites,
                focusMemory, restoreTick, onPlay, onToggleFavorite)
        }
    }
}

@Composable
private fun ChannelRow(
    title: String,
    rowKey: String,
    channels: List<Channel>,
    favorites: Set<String>,
    focusMemory: FocusMemory,
    restoreTick: Int,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(start = 12.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
        ) {
            itemsIndexed(channels, key = { index, c -> "${index}_${c.id}" }) { index, channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = channel.id in favorites,
                    focusKey = "$rowKey|$index",
                    focusMemory = focusMemory,
                    restoreTick = restoreTick,
                    onClick = { onPlay(channels, index) },
                    onLongClick = { onToggleFavorite(channel) },
                    modifier = Modifier.width(200.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: UiState,
    focusMemory: FocusMemory,
    restoreTick: Int,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onClearSearch: () -> Unit,
) {
    val query = state.query.trim()
    val results = remember(state.channels, query) {
        state.channels.filter {
            it.name.contains(query, ignoreCase = true) || it.group.contains(query, ignoreCase = true)
        }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Resultados para «$query» · ${results.size}",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onClearSearch) { Text("Limpiar búsqueda") }
    }
    Spacer(Modifier.height(8.dp))

    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No hay canales que coincidan con tu búsqueda.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(204.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(results, key = { index, c -> "${index}_${c.id}" }) { index, channel ->
            ChannelCard(
                channel = channel,
                isFavorite = channel.id in state.favorites,
                focusKey = "search|$index",
                focusMemory = focusMemory,
                restoreTick = restoreTick,
                onClick = { onPlay(results, index) },
                onLongClick = { onToggleFavorite(channel) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CenteredLoading(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
    }
}

@Composable
private fun EmptyState(onAddPlaylist: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Añade tu lista M3U",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pega la dirección (URL) de tu lista .m3u o .m3u8 para ver tus canales.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAddPlaylist) { Text("Añadir lista") }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onChangePlaylist: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No se pudo cargar la lista",
                style = MaterialTheme.typography.headlineSmall,
                color = ErrorColor,
            )
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRetry) { Text("Reintentar") }
                Button(onClick = onChangePlaylist) { Text("Cambiar lista") }
            }
        }
    }
}
