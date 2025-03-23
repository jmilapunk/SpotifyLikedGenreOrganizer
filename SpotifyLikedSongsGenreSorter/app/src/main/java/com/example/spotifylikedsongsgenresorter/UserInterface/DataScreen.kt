package com.example.spotifylikedsongsgenresorter.UserInterface

import Track
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.spotifylikedsongsgenresorter.api.RetrofitClient
import com.example.spotifylikedsongsgenresorter.api.RetrofitClientSpotify
import com.example.spotifylikedsongsgenresorter.model.PlaylistRequest
import com.example.spotifylikedsongsgenresorter.model.AddTracksRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun DataScreen(accessToken: String) {
    var categories by remember { mutableStateOf<Map<String, List<Track>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(accessToken) {
        try {
            val response = RetrofitClient.instance.getLikedSongsGenres(accessToken)
            if (response.isSuccessful) {
                categories = response.body() ?: emptyMap()
                Log.d("DataScreen", "Categorías recibidas:")
                categories.forEach { (categoria, tracks) ->
                    Log.d("DataScreen", "$categoria: ${tracks.size} tracks")
                }
            } else {
                errorMessage = "Error: ${response.errorBody()?.string()}"
            }
        } catch (e: Exception) {
            errorMessage = "Excepción: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column {
        if (isLoading) {
            Text(text = "Cargando...")
        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage)
        } else {
            // Muestra la lista de categorías y agrega un botón para crear playlist en cada categoría
            CategoryList(accessToken, categories)
        }
    }
}

@Composable
fun CategoryList(accessToken: String, categories: Map<String, List<Track>>) {
    LazyColumn {
        categories.forEach { (genero, tracks) ->
            item {
                Text(
                    text = genero,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                )
                Text(
                    text = "Cantidad de tracks: ${tracks.size}",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Button(
                    onClick = {
                        // Lanza una coroutine para crear la playlist para este género.
                        // Se asume que el modelo Track incluye "track_id" para construir el URI.
                        CoroutineScope(Dispatchers.IO).launch {
                            val userId = obtenerUserId(accessToken)
                            if (userId != null) {
                                val trackUris = tracks.map { "spotify:track:${it.track_id}" }
                                val exito = crearPlaylistPorGenero(accessToken, userId, genero, trackUris)
                                Log.d("CategoryList", "Playlist para $genero creada: $exito")
                            } else {
                                Log.e("CategoryList", "No se pudo obtener el userId")
                            }
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Crear Playlist para $genero")
                }
            }
            items(tracks) { track ->
                TrackItem(track)
            }
        }
    }
}

@Composable
fun TrackItem(track: Track) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "Track: ${track.track_name}")
        Text(text = "Artistas: ${track.artist_names.joinToString(", ")}")
    }
}

// Función para obtener el user_id mediante una llamada a la API de Spotify (usando OkHttp)
suspend fun obtenerUserId(accessToken: String): String? {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string()
        if (response.isSuccessful && bodyStr != null) {
            val json = JSONObject(bodyStr)
            return json.getString("id")
        }
    } catch (e: Exception) {
        Log.e("obtenerUserId", "Error: ${e.localizedMessage}", e)
    }
    return null
}

// Función para crear una playlist por género y agregarle las canciones
suspend fun crearPlaylistPorGenero(
    accessToken: String,
    userId: String,
    genero: String,
    trackUris: List<String>
): Boolean {
    val authHeader = "Bearer $accessToken"
    val playlistRequest = PlaylistRequest(
        name = "Playlist de $genero",
        description = "Playlist automática para el género $genero",
        public = true
    )
    val playlistResponse = RetrofitClientSpotify.instance.crearPlaylist(userId, authHeader, playlistRequest)
    return if (playlistResponse.isSuccessful) {
        val playlistId = playlistResponse.body()?.id
        if (playlistId != null) {
            val addResponse = RetrofitClientSpotify.instance.agregarCanciones(
                playlistId,
                authHeader,
                AddTracksRequest(trackUris)
            )
            addResponse.isSuccessful
        } else {
            false
        }
    } else {
        false
    }
}
