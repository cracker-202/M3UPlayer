package com.example.m3uplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import coil.compose.SubcomposeAsyncImage
import com.example.m3uplayer.data.Channel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CineCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    val television = isTelevision()

    val content: @Composable () -> Unit = {

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(Color(0xFF292D38)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎬")
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎬")
                        }
                    }
                )
            }

            Text(
                text = channel.name,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    if (television) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF191C24)
        ),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(
            focusedBorder = Border(
                BorderStroke(3.dp, Color(0xFFD4A843)),
                shape = shape
            )
        )
    ) {
        content()
    }
    } else {
        androidx.compose.material3.Surface(
            modifier = modifier.combinedClickable(
                onClick = onClick,
            ),
            shape = shape,
            color = Color(0xFF191C24),
            contentColor = Color.White
        ) {
            Column {
                content()
            }
        }
    }
}
