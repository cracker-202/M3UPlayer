package com.example.m3uplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.SubcomposeAsyncImage
import com.example.m3uplayer.data.Channel

/** Recuerda qué tarjeta tenía el foco, sin provocar recomposiciones. */
class FocusMemory {
    var key: String? = null
}

@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    focusKey: String,
    focusMemory: FocusMemory,
    restoreTick: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    val shape = RoundedCornerShape(14.dp)

    LaunchedEffect(restoreTick) {
        if (restoreTick > 0 && focusMemory.key == focusKey) {
            runCatching { requester.requestFocus() }
        }
    }

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .focusRequester(requester)
            .onFocusChanged { if (it.hasFocus) focusMemory.key = focusKey },
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = SurfaceDark),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Accent), shape = shape)
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(SurfaceInset)
        ) {
            ChannelLogo(channel, Modifier.fillMaxSize().padding(14.dp))
            if (isFavorite) {
                Text(
                    "★",
                    color = Gold,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun ChannelLogo(channel: Channel, modifier: Modifier = Modifier) {
    if (channel.logo == null) {
        InitialPlaceholder(channel.name, modifier)
    } else {
        SubcomposeAsyncImage(
            model = channel.logo,
            contentDescription = channel.name,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            loading = { InitialPlaceholder(channel.name, Modifier.fillMaxSize()) },
            error = { InitialPlaceholder(channel.name, Modifier.fillMaxSize()) },
        )
    }
}

@Composable
private fun InitialPlaceholder(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineLarge,
            color = Accent,
        )
    }
}

/** Campo de texto pensado para el mando: borde de color al recibir el foco. */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    onImeAction: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = TextPrimary, fontSize = 20.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = imeAction, keyboardType = keyboardType),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction() },
            onSearch = { onImeAction() },
            onGo = { onImeAction() },
        ),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceInset, RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        if (focused) Accent else Color(0xFF2A3040),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextSecondary, fontSize = 20.sp)
                }
                inner()
            }
        },
    )
}
