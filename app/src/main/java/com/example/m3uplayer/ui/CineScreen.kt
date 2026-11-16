package com.example.m3uplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private data class CineTitle(val title: String, val language: String, val video: String)
private val cineClient = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS).build()

@Composable
fun CineScreen(section: String, onSelectSection: (String) -> Unit, onPlay: (String, String) -> Unit) {
    var titles by remember(section) { mutableStateOf<List<CineTitle>>(emptyList()) }
    var loading by remember(section) { mutableStateOf(true) }
    var error by remember(section) { mutableStateOf<String?>(null) }
    var retry by remember(section) { mutableStateOf(0) }

    LaunchedEffect(section, retry) {
        loading = true
        error = null
        try {
            titles = withContext(Dispatchers.IO) { loadPublicDomainCatalog(section) }
        } catch (e: Exception) {
            error = "No se pudo consultar el catálogo. Revisa tu conexión."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 48.dp, vertical = 27.dp)) {
        Text("ZORRILLO TV", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text("POWERED BY: skunk sv.", color = Gold)
        Spacer(Modifier.height(20.dp))
        SectionTabs(section, onSelectSection)
        Spacer(Modifier.height(20.dp))
        Text(if (section == "movies") "Películas gratuitas" else "Series y episodios clásicos",
            style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("Catálogo en línea de Internet Archive · Obras identificadas como dominio público o CC0",
            color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        when {
            loading -> Row { CircularProgressIndicator(color = Accent); Text(" Buscando títulos…", color = TextPrimary) }
            error != null -> Column {
                Text(error!!, color = ErrorColor)
                Button(onClick = { retry++ }) { Text("Reintentar") }
            }
            titles.isEmpty() -> Column {
                Text("No encontramos videos verificables en esta búsqueda. Prueba más tarde.",
                    color = TextSecondary)
                Button(onClick = { retry++ }) { Text("Actualizar catálogo") }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(titles) { item ->
                    Button(onClick = { onPlay(item.title, item.video) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(item.title + "  ·  " + item.language)
                    }
                }
            }
        }
    }
}

private fun loadPublicDomainCatalog(section: String): List<CineTitle> {
    // Only items with explicit public-domain/CC0 metadata are considered for playback.
    // Search is refreshed every time this screen is opened; no APK update is needed.
    val category = if (section == "series") "television" else "feature_films"
    val query = "mediatype:movies AND collection:$category"
    val encoded = URLEncoder.encode(query, "UTF-8")
    val searchUrl = "https://archive.org/advancedsearch.php?q=$encoded&fl%5B%5D=identifier&rows=35&page=1&output=json"
    val search = requestJson(searchUrl)
    val docs = search.getJSONObject("response").getJSONArray("docs")
    val found = mutableListOf<CineTitle>()
    for (i in 0 until docs.length()) {
        if (found.size >= 16) break
        val identifier = docs.getJSONObject(i).optString("identifier")
        if (!identifier.matches(Regex("[A-Za-z0-9_.-]+"))) continue
        try {
            val meta = requestJson("https://archive.org/metadata/$identifier")
            val details = meta.optJSONObject("metadata") ?: continue
            val rights = details.optString("licenseurl") + " " + details.optString("rights")
            val normalized = rights.lowercase()
            if (!normalized.contains("publicdomain") && !normalized.contains("public domain") &&
                !normalized.contains("creativecommons.org/publicdomain/zero")) continue
            val files = meta.optJSONArray("files") ?: continue
            var video: String? = null
            for (j in 0 until files.length()) {
                val filename = files.getJSONObject(j).optString("name")
                if (filename.endsWith(".mp4", ignoreCase = true) && !filename.contains("/")) {
                    video = "https://archive.org/download/$identifier/" +
                        filename.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                    break
                }
            }
            if (video != null) {
                val language = details.optString("language").ifBlank { "Idioma no especificado" }
                found.add(CineTitle(details.optString("title", identifier), language, video))
            }
        } catch (_: Exception) {
            // A broken catalog item should not prevent loading the remaining items.
        }
    }
    return found.sortedBy { if (it.language.contains("span", true) || it.language.contains("espa", true)) 0 else 1 }
}

private fun requestJson(url: String): JSONObject {
    val request = Request.Builder().url(url).header("User-Agent", "ZorrilloTV/2.0").build()
    cineClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
        return JSONObject(response.body?.string() ?: "{}")
    }
}
