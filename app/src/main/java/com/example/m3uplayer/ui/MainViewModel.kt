package com.example.m3uplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.m3uplayer.data.Channel
import com.example.m3uplayer.data.ChannelRepository
import com.example.m3uplayer.data.PlaylistException
import com.example.m3uplayer.data.Prefs
import com.example.m3uplayer.data.RemoteConfig
import com.example.m3uplayer.data.RemotePlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class UiState(
    val ready: Boolean = false,
    val activated: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val playlistUrl: String? = null,
    val selectedPlaylist: String = "",
    val playlists: List<RemotePlaylist> = emptyList(),
    val playbackRevision: Int = 0,
    val channels: List<Channel> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val lastChannelId: String? = null,
    val query: String = "",
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val repository = ChannelRepository(
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build(),
        app.applicationContext
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.favorites.collect { favs -> _ui.update { it.copy(favorites = favs) } }
        }
        viewModelScope.launch {
            prefs.lastChannel.collect { id -> _ui.update { it.copy(lastChannelId = id) } }
        }
        viewModelScope.launch {
            val activated = prefs.activated.first()
            val selected = prefs.selectedPlaylist.first()
            _ui.update {
                it.copy(
                    ready = true,
                    activated = activated,
                    selectedPlaylist = selected
                )
            }
        }
    }

    fun applyRemoteConfig(config: RemoteConfig?) {
        val old = _ui.value

        if (!old.ready) return

        if (config == null) {
            loadJob?.cancel()

            _ui.update {
                it.copy(
                    playlists = emptyList(),
                    playlistUrl = null,
                    channels = emptyList(),
                    loading = false,
                    playbackRevision = it.playbackRevision +
                        if (old.playlistUrl != null) 1 else 0,
                    error = "Configuracion remota no disponible."
                )
            }
            return
        }

        val selected = config.playlists.firstOrNull {
            it.id == old.selectedPlaylist
        } ?: config.playlists.first {
            it.id == config.defaultPlaylistId
        }

        val changed = old.playlistUrl != selected.url ||
            old.selectedPlaylist != selected.id

        if (changed) loadJob?.cancel()

        _ui.update {
            it.copy(
                playlists = config.playlists,
                selectedPlaylist = selected.id,
                playlistUrl = selected.url,
                channels = if (changed) emptyList() else it.channels,
                playbackRevision = it.playbackRevision +
                    if (changed) 1 else 0,
                error = if (changed) null else it.error
            )
        }

        if (old.selectedPlaylist != selected.id) {
            viewModelScope.launch {
                prefs.setSelectedPlaylist(selected.id)
            }
        }

        if (changed && old.activated) fetch(selected.url)
    }

    fun activate() {
        if (_ui.value.activated) return

        viewModelScope.launch {
            prefs.setActivated(true)
            _ui.update { it.copy(activated = true) }
            _ui.value.playlistUrl?.let { fetch(it) }
        }
    }

    fun selectIntegratedPlaylist(id: String) {
        val selected = _ui.value.playlists.firstOrNull {
            it.id == id
        } ?: return

        if (_ui.value.selectedPlaylist == id &&
            _ui.value.playlistUrl == selected.url) return

        loadJob?.cancel()

        viewModelScope.launch {
            prefs.setSelectedPlaylist(id)
        }

        _ui.update {
            it.copy(
                selectedPlaylist = id,
                playlistUrl = selected.url,
                query = "",
                channels = emptyList(),
                playbackRevision = it.playbackRevision + 1
            )
        }

        if (_ui.value.activated) fetch(selected.url)
    }

    fun loadPlaylist(url: String) {
        val selected = _ui.value.playlists.firstOrNull {
            it.url == url.trim()
        } ?: return

        selectIntegratedPlaylist(selected.id)
    }

    fun refresh() {
        if (!_ui.value.activated) return
        _ui.value.playlistUrl?.let {
            fetch(it, forceRefresh = true)
        }
    }

    fun setQuery(query: String) {
        _ui.update { it.copy(query = query) }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { prefs.toggleFavorite(channel.id) }
    }

    fun onWatched(channel: Channel) {
        viewModelScope.launch { prefs.setLastChannel(channel.id) }
    }

    private fun fetch(
        url: String,
        forceRefresh: Boolean = false
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val list = repository.load(url, forceRefresh)
                _ui.update { it.copy(loading = false, channels = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, channels = emptyList(), error = describe(e)) }
            }
        }
    }

    private fun describe(e: Exception): String = when (e) {
        is PlaylistException -> e.message ?: GENERIC_ERROR
        is UnknownHostException -> "No hay conexión a internet o la dirección no existe."
        is SocketTimeoutException -> "La lista tardó demasiado en responder."
        is IllegalArgumentException -> "La dirección de la lista no es válida."
        is IOException -> "No se pudo descargar la lista. Revisa tu conexión."
        else -> GENERIC_ERROR
    }

    private companion object {
        const val GENERIC_ERROR = "Ocurrió un error inesperado al cargar la lista."
    }
}
