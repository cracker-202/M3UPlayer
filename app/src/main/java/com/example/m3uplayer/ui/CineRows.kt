package com.example.m3uplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.m3uplayer.data.Channel

@Composable
fun CineRows(
    channels: List<Channel>,
    onPlay: (List<Channel>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember(channels) {
        channels.groupBy { it.group }.toList()
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 48.dp,
            end = 48.dp,
            top = 16.dp,
            bottom = 48.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        itemsIndexed(
            groups,
            key = { _, group -> group.first }
        ) { _, group ->
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = group.first,
                    color = Gold,
                    style = MaterialTheme.typography.titleLarge
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    items(
                        count = group.second.size,
                        key = { index -> index }
                    ) { index ->
                        CineCard(
                            channel = group.second[index],
                            onClick = {
                                onPlay(group.second, index)
                            },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }
    }
}
