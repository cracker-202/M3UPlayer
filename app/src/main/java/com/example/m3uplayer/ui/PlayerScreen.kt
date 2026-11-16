package com.example.m3uplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.m3uplayer.data.Channel
import com.example.m3uplayer.data.ChannelRepository
import kotlinx.coroutines.delay

private fun mediaItem(url: String, forceHls: Boolean): MediaItem =
    MediaItem.Builder()
        .setUri(url)
        .apply { if (forceHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .build()

/**
 * Reproductor a pantalla completa.
 *  - Arriba / Canal+  : canal siguiente
 *  - Abajo / Canal-   : canal anterior
 *  - OK               : mostrar información del canal
 *  - Play/Pausa       : pausar o reanudar
 *  - Atrás            : volver a la lista
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channels: List<Channel>,
    startIndex: Int,
    onBack: () -> Unit,
    onWatched: (Channel) -> Unit,
) {
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, channels.lastIndex)) }
    val channel = channels[index]

    var buffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoTick by remember { mutableIntStateOf(0) }
    var infoVisible by remember { mutableStateOf(true) }
    var userPaused by remember { mutableStateOf(false) }

    val retriedAsHls = remember { BooleanArray(1) }
    val currentChannel by rememberUpdatedState(channel)
    val focusRequester = remember { FocusRequester() }

    val exo = remember {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent(ChannelRepository.USER_AGENT)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http))
            )
            .build()
    }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!retriedAsHls[0]) {
                    // Muchas listas usan URLs sin extensión .m3u8: se reintenta como HLS.
                    retriedAsHls[0] = true
                    exo.setMediaItem(mediaItem(currentChannel.url, forceHls = true))
                    exo.prepare()
                    exo.playWhenReady = true
                } else {
                    buffering = false
                    errorMessage = "No se pudo reproducir este canal."
                }
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }

    LaunchedEffect(channel.url) {
        errorMessage = null
        buffering = true
        delay(150) // evita saturar la red si se cambia de canal muy rápido
        val hls = channel.url.contains(".m3u8", ignoreCase = true)
        retriedAsHls[0] = hls
        exo.setMediaItem(mediaItem(channel.url, forceHls = hls))
        exo.prepare()
        exo.playWhenReady = true
        userPaused = false
        onWatched(channel)
        infoTick++
    }

    LaunchedEffect(infoTick) {
        infoVisible = true
        delay(4_000)
        infoVisible = false
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { exo.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (!userPaused) exo.play() }

    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.ChannelUp -> {
                        index = (index + 1) % channels.size
                        true
                    }
                    Key.DirectionDown, Key.ChannelDown -> {
                        index = (index - 1 + channels.size) % channels.size
                        true
                    }
                    Key.DirectionLeft, Key.DirectionRight -> true
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        infoTick++
                        true
                    }
                    Key.MediaPlayPause -> {
                        if (exo.isPlaying) {
                            exo.pause(); userPaused = true
                        } else {
                            exo.play(); userPaused = false
                        }
                        infoTick++
                        true
                    }
                    Key.MediaPlay -> {
                        exo.play(); userPaused = false
                        true
                    }
                    Key.MediaPause -> {
                        exo.pause(); userPaused = true
                        infoTick++
                        true
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exo
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    isFocusable = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (buffering && errorMessage == null) {
            CircularProgressIndicator(
                color = Accent,
                modifier = Modifier.align(Alignment.Center).size(56.dp),
            )
        }

        errorMessage?.let { message ->
            Column(
                Modifier.align(Alignment.Center).padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ErrorColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Usa arriba o abajo para cambiar de canal, o Atrás para volver.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        AnimatedVisibility(
            visible = infoVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)))
                    )
                    .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(width = 112.dp, height = 63.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                ) {
                    ChannelLogo(channel, Modifier.fillMaxSize().padding(8.dp))
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        channel.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        channel.group,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${index + 1} / ${channels.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        "Arriba / Abajo: cambiar canal",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
