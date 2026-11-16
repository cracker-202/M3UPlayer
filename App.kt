package com.example.m3uplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.m3uplayer.data.Channel

private data class PlaybackRequest(val channels: List<Channel>, val index: Int)

@Composable
fun App(vm: MainViewModel) {
    val state by vm.ui.collectAsStateWithLifecycle()

    var playback by remember { mutableStateOf<PlaybackRequest?>(null) }
    var restoreTick by remember { mutableIntStateOf(0) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        // La pantalla principal se mantiene debajo del reproductor para conservar
        // la posición de desplazamiento y devolver el foco al volver.
        HomeScreen(
            state = state,
            restoreTick = restoreTick,
            onPlay = { list, index -> playback = PlaybackRequest(list, index) },
            onToggleFavorite = vm::toggleFavorite,
            onSearch = { showSearchDialog = true },
            onClearSearch = { vm.setQuery("") },
            onChangePlaylist = { showPlaylistDialog = true },
            onRefresh = vm::refresh,
        )

        playback?.let { request ->
            PlayerScreen(
                channels = request.channels,
                startIndex = request.index,
                onBack = {
                    playback = null
                    restoreTick++
                },
                onWatched = vm::onWatched,
            )
        }

        if (showPlaylistDialog) {
            PlaylistDialog(
                initial = state.playlistUrl.orEmpty(),
                onDismiss = { showPlaylistDialog = false },
                onConfirm = { url ->
                    showPlaylistDialog = false
                    vm.loadPlaylist(url)
                },
            )
        }

        if (showSearchDialog) {
            SearchDialog(
                initial = state.query,
                onDismiss = { showSearchDialog = false },
                onSearch = { query ->
                    showSearchDialog = false
                    vm.setQuery(query)
                },
            )
        }
    }
}
