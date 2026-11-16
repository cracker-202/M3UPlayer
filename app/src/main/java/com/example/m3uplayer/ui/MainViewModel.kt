package com.example.m3uplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.m3uplayer.data.Channel
import com.example.m3uplayer.data.ChannelRepository
import com.example.m3uplayer.data.PlaylistException
import com.example.m3uplayer.data.Prefs
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
            .build()
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
            val url = DEFAULT_PLAYLIST_URL.trim()
            _ui.update {
                it.copy(ready = true, activated = activated, playlistUrl = url)
            }
            if (activated && url.isNotEmpty()) fetch(url)
        }
    }

    fun activate() {
        if (_ui.value.activated) return
        viewModelScope.launch {
            prefs.setActivated(true)
            _ui.update { it.copy(activated = true) }
            val url = _ui.value.playlistUrl.orEmpty()
            if (url.isNotEmpty()) fetch(url)
        }
    }

    fun loadPlaylist(url: String) {
        val clean = url.trim()
        viewModelScope.launch { prefs.setPlaylistUrl(clean) }
        _ui.update { it.copy(playlistUrl = clean, query = "") }
        fetch(clean)
    }

    fun refresh() {
        _ui.value.playlistUrl?.let { fetch(it) }
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

    private fun fetch(url: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val list = repository.load(url)
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
        // Lista proporcionada por el usuario para su instalación.
        const val DEFAULT_PLAYLIST_URL = "http://amazing-prada.xyz:80/get.php?username= TanekaG&password=63937363&type=m3u_plus"
    }
}
