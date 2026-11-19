package com.example.m3uplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    var showAdminPin by remember { mutableStateOf(false) }
    var showAdultPin by remember { mutableStateOf(false) }
    var adultsUnlocked by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.ready, state.activated) {
        if (state.ready && !state.activated) {
            showAdminPin = true
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        // La pantalla principal se mantiene debajo del reproductor para conservar
        // la posición de desplazamiento y devolver el foco al volver.
        if (state.activated) HomeScreen(
            state = state,
            restoreTick = restoreTick,
            onPlay = { list, index -> playback = PlaybackRequest(list, index) },
            onToggleFavorite = vm::toggleFavorite,
            onSearch = { showSearchDialog = true },
            onClearSearch = { vm.setQuery("") },
            onChangePlaylist = { pinError = null; showAdminPin = true },
            onRefresh = vm::refresh,
            adultsUnlocked = adultsUnlocked,
            onRequestAdultPin = { pinError = null; showAdultPin = true },
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


        if (showAdminPin) {
            PinDialog(
                title = "Acceso de administrador",
                description = pinError ?: "Introduce el PIN para cambiar o administrar la lista.",
                onDismiss = { showAdminPin = false; pinError = null },
                onConfirm = { entered ->
                    if (entered == "2001") {
                        showAdminPin = false
                        pinError = null
                        if (!state.activated) {
                            vm.activate()
                        } else {
                            showPlaylistDialog = true
                        }
                    } else pinError = "PIN incorrecto. Inténtalo de nuevo."
                },
            )
        }

        if (showAdultPin) {
            PinDialog(
                title = "Contenido para adultos",
                description = pinError ?: "Introduce el PIN para desbloquear esta sección.",
                onDismiss = { showAdultPin = false; pinError = null },
                onConfirm = { entered ->
                    if (entered == "0000") {
                        adultsUnlocked = true
                        showAdultPin = false
                        pinError = null
                    } else pinError = "PIN incorrecto. Inténtalo de nuevo."
                },
            )
        }

        if (showPlaylistDialog) {
            Dialog(
                onDismissRequest = { showPlaylistDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Column(
                    Modifier.width(600.dp)
                        .background(SurfaceDark, RoundedCornerShape(20.dp))
                        .padding(32.dp)
                ) {
                    Text("Seleccionar lista", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Text("Elige la lista integrada que quieres ver.", color = TextSecondary)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        showPlaylistDialog = false
                        vm.selectIntegratedPlaylist("1")
                    }) { Text("Lista 1 · Original" + if (state.selectedPlaylist == "1") " ✓" else "") }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        showPlaylistDialog = false
                        vm.selectIntegratedPlaylist("2")
                    }) { Text("Lista 2 · El Salvador" + if (state.selectedPlaylist == "2") " ✓" else "") }
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        showPlaylistDialog = false
                        vm.selectIntegratedPlaylist("3")
                    }) { Text("Lista 3 · Android Latinos" + if (state.selectedPlaylist == "3") " ✓" else "") }

                    Button(onClick = { showPlaylistDialog = false }) { Text("Cancelar") }
                }
            }
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
