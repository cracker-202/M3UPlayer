package com.example.m3uplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.m3uplayer.ui.App
import com.example.m3uplayer.ui.M3UTheme
import com.example.m3uplayer.ui.MainViewModel
import com.example.m3uplayer.ui.ActivationScreen
import com.example.m3uplayer.security.DeviceActivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            M3UTheme {
                var status by remember {
                    mutableStateOf("checking")
                }

                var deviceId by remember {
                    mutableStateOf("Generando identidad...")
                }

                var retry by remember {
                    mutableStateOf(0)
                }

                LaunchedEffect(retry) {
                    lifecycle.repeatOnLifecycle(
                        Lifecycle.State.RESUMED
                    ) {
                        status = "checking"

                        try {
                            while (true) {
                                val result = withContext(
                                    Dispatchers.IO
                                ) {
                                    val id = runCatching {
                                        DeviceActivation.deviceId()
                                    }.getOrDefault("No disponible")

                                    val activation = DeviceActivation.checkActivation()

                                    Triple(
                                        id,
                                        activation,
                                        DeviceActivation.remoteConfig
                                    )
                                }

                                viewModel.applyRemoteConfig(
                                    if (result.second == "active")
                                        result.third
                                    else null
                                )

                                if (result.second != "active") {
                                    viewModel.clearCatalog()
                                }

                                deviceId = result.first
                                status = result.second

                                delay(30000L)
                            }
                        } finally {
                            viewModel.clearCatalog()
                            viewModel.applyRemoteConfig(null)
                            status = "checking"
                        }
                    }
                }

                if (status == "active") {
                    App(viewModel)
                } else {
                    ActivationScreen(
                        status = status,
                        deviceId = deviceId,
                        onRetry = {
                            status = "checking"
                            retry++
                        }
                    )
                }
            }
        }
    }
}
