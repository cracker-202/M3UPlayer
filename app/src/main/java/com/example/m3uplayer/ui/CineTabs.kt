package com.example.m3uplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text

@Composable
fun CineTabs(
    selected: CineSection,
    onSelect: (CineSection) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CineSection.entries.forEach { section ->
            val active = selected == section

            Button(
                onClick = { onSelect(section) },
                shape = ButtonDefaults.shape(
                    shape = RoundedCornerShape(14.dp)
                ),
                colors = ButtonDefaults.colors(
                    containerColor = if (active) Color(0xFFD4A843)
                                     else Color(0xFF22252C),
                    contentColor = if (active) Color.Black else Color.White
                )
            ) {
                Text(
                    when (section) {
                        CineSection.MOVIES -> "🎬 Películas"
                        CineSection.SERIES -> "🍿 Series"
                        CineSection.LIVE_TV -> "📡 TV en vivo"
                    }
                )
            }
        }
    }
}
