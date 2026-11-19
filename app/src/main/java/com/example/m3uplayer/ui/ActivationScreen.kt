package com.example.m3uplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActivationScreen(
    status: String,
    deviceId: String,
    onRetry: () -> Unit
) {
    val message = when (status) {
        "checking" -> "Verificando dispositivo..."
        "pending" -> "Pendiente de activacion"
        "blocked" -> "Dispositivo bloqueado"
        "expired" -> "Suscripcion vencida"
        else -> "Error de conexion o seguridad"
    }

    val accent = when (status) {
        "blocked" -> Color(0xFFFF5252)
        "expired" -> Color(0xFFFF9800)
        "pending" -> Color(0xFFFFD54F)
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZORRILLO TV",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = message,
            color = accent,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "ID DEL DISPOSITIVO",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Text(
            text = deviceId,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        if (status == "checking") {
            CircularProgressIndicator(
                color = Color.White
            )
        } else {
            Button(onClick = onRetry) {
                Text("VERIFICAR ACTIVACION")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Contacta con tu proveedor para activar o renovar.",
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "POWERED BY: skunk sv",
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}
