package com.example.m3uplayer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "m3u_settings")

class Prefs(context: Context) {

    private val store = context.applicationContext.dataStore

    private val keyActivated = booleanPreferencesKey("activated")
    private val keyUrl = stringPreferencesKey("playlist_url")
    private val keyFavorites = stringSetPreferencesKey("favorites")
    private val keyLast = stringPreferencesKey("last_channel")

    val activated: Flow<Boolean> = store.data.map { it[keyActivated] ?: false }
    val playlistUrl: Flow<String?> = store.data.map { it[keyUrl] }
    val favorites: Flow<Set<String>> = store.data.map { it[keyFavorites].orEmpty() }
    val lastChannel: Flow<String?> = store.data.map { it[keyLast] }

    suspend fun setActivated(value: Boolean) {
        store.edit { it[keyActivated] = value }
    }

    suspend fun setPlaylistUrl(url: String) {
        store.edit { it[keyUrl] = url }
    }

    suspend fun setLastChannel(id: String) {
        store.edit { it[keyLast] = id }
    }

    suspend fun toggleFavorite(id: String) {
        store.edit { prefs ->
            val set = prefs[keyFavorites].orEmpty().toMutableSet()
            if (!set.add(id)) set.remove(id)
            prefs[keyFavorites] = set
        }
    }
}
