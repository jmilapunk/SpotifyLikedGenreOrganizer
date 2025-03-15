package com.example.spotifylikedsongsgenresorter

// --------- Compose y Kotlin ---------
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

// --------- Material y UI ---------
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme

// --------- Tu data class y Retrofit ---------
import com.example.spotifylikedsongsgenresorter.model.Track
import com.example.spotifylikedsongsgenresorter.api.RetrofitClient

// --------- Para manejo de excepciones, corutinas, etc. ---------
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DataScreen(accessToken: String) {
    // 1. Variables de estado
    var categories by remember { mutableStateOf<Map<String, List<Track>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // 2. Llamada al backend
    LaunchedEffect(accessToken) {
        try {
            val response = RetrofitClient.instance.getLikedSongsGenres(accessToken)
            if (response.isSuccessful) {
                categories = response.body() ?: emptyMap()
            } else {
                errorMessage = "Error: ${response.errorBody()?.string()}"
            }
        } catch (e: Exception) {
            errorMessage = "Excepción: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    // 3. Mostrar la UI según el estado
    when {
        isLoading -> {
            // Mientras esperamos la respuesta, muestra un texto o indicador
            Text(text = "Cargando...")
        }
        errorMessage.isNotEmpty() -> {
            // Si hubo un error, muéstralo
            Text(text = errorMessage)
        }
        else -> {
            // Si tenemos datos, mostramos las categorías y sus pistas
            CategoryList(categories = categories)
        }
    }
}

@Composable
fun CategoryList(categories: Map<String, List<Track>>) {
    LazyColumn {
        categories.forEach { (category, tracks) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleLarge
                )
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