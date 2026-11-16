package com.example.m3uplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun PlaylistDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val clean = text.trim()
    val valid = clean.startsWith("http://", ignoreCase = true) ||
        clean.startsWith("https://", ignoreCase = true)
    val requester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }

        Column(
            Modifier
                .width(720.dp)
                .background(SurfaceDark, RoundedCornerShape(20.dp))
                .padding(32.dp)
        ) {
            Text(
                "Tu lista M3U",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Escribe o pega la dirección de tu lista (.m3u / .m3u8). " +
                    "Con la app de Fire TV en el móvil puedes escribir más rápido.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            TvTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "https://ejemplo.com/lista.m3u",
                modifier = Modifier.fillMaxWidth().focusRequester(requester),
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Uri,
                onImeAction = { if (valid) onConfirm(clean) },
            )
            if (clean.isNotEmpty() && !valid) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "La dirección debe empezar por http:// o https://",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorColor,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onConfirm(clean) }, enabled = valid) { Text("Cargar lista") }
                Button(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}

@Composable
fun SearchDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val requester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }

        Column(
            Modifier
                .width(720.dp)
                .background(SurfaceDark, RoundedCornerShape(20.dp))
                .padding(32.dp)
        ) {
            Text(
                "Buscar canales",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Busca por nombre de canal o por categoría.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            TvTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Escribe el nombre de un canal",
                modifier = Modifier.fillMaxWidth().focusRequester(requester),
                imeAction = ImeAction.Search,
                onImeAction = { onSearch(text.trim()) },
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSearch(text.trim()) }) { Text("Buscar") }
                Button(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}
