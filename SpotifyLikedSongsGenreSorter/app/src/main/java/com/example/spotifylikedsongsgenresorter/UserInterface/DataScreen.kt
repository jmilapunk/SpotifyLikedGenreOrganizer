package com.example.spotifylikedsongsgenresorter.UserInterface

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.spotifylikedsongsgenresorter.R
import com.example.spotifylikedsongsgenresorter.api.RetrofitClientFlask
import com.example.spotifylikedsongsgenresorter.api.RetrofitClientSpotify
import com.example.spotifylikedsongsgenresorter.model.PlaylistRequest
import com.example.spotifylikedsongsgenresorter.model.AddTracksRequest
import com.example.spotifylikedsongsgenresorter.model.PlaylistTrackItem
import com.example.spotifylikedsongsgenresorter.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            val response = RetrofitClientFlask.instance.getLikedSongsGenres(accessToken)
            if (response.isSuccessful) {
                val body = response.body()
                val genresMap = body?.genres ?: emptyMap()
                val totalExpected = body?.total_expected
                val totalReceived = body?.total_received

                // 1. Guardamos categorías para mostrar en UI
                categories = genresMap

                // 2. Ejecutamos validación de categorías (tu función ya existente)
                validarCategorias(genresMap)

                // 3. Logs de control
                Log.d("DataScreen", "DEBUG: totalExpected=$totalExpected, totalReceived=$totalReceived")
                if (totalExpected != null && totalReceived != null) {
                    if (totalExpected == 0) {
                        Log.i("DataScreen", "ℹ️ El usuario no tiene canciones liked.")
                    } else if (totalReceived < totalExpected) {
                        Log.w("DataScreen", "⚠️ Se perdieron canciones: $totalReceived de $totalExpected")
                    } else {
                        Log.d("DataScreen", "✅ Todas las canciones recibidas correctamente.")
                    }
                } else {
                    Log.e("DataScreen", "❌ total_expected o total_received no son válidos.")
                }

                genresMap.forEach { (categoria, tracks) ->
                    Log.d("DataScreen", "$categoria: ${tracks.size} tracks")
                }

                // ✅ Validación CRUCIAL: ¿Todas las canciones se clasificaron?
                val trackIdsClasificados = genresMap.values
                    .flatten()
                    .mapNotNull { it.track_id }
                    .toSet()

                Log.d("ValidaciónCrucial", "🎧 Tracks únicos clasificados: ${trackIdsClasificados.size}")
                Log.d("ValidaciónCrucial", "🎯 Total canciones reales recibidas: $totalReceived")

                if (trackIdsClasificados.size == totalReceived) {
                    Log.d("ValidaciónCrucial", "✅ Todas las canciones fueron clasificadas al menos en una categoría")
                } else {
                    val sinClasificar = (totalReceived ?: 0) - trackIdsClasificados.size
                    Log.w("ValidaciónCrucial", "⚠️ $sinClasificar canciones no se clasificaron en ningún género")
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
            Column(modifier = Modifier.padding(16.dp)) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(id = R.string.loading_liked_songs),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage)
        } else {
            CategoryButtons(accessToken, categories)
            //CategoryList(accessToken, categories)
        }
    }
}

fun validarCategorias(genresMap: Map<String, List<Track>>) {
    val broadCategories = mapOf(
        "Rock" to listOf("rock", "garage", "latin rock", "rock urbano", "rock en español", "hard rock", "classic rock", "psychedelic rock", "grunge"),
        "Pop" to listOf("pop", "pop latino", "latin pop", "synthpop", "pop rock", "teen pop", "indie pop", "electropop"),
        "Punk" to listOf("punk", "ska punk", "hardcore punk", "skate punk", "punk rock", "melodic hardcore", "punk rap"),
        "Hip Hop / Rap" to listOf("rap", "hip hop", "trap", "latin hip hop", "cloud rap", "emo rap", "boom bap", "underground hip hop", "drill"),
        "Indie / Alternativo" to listOf("indie", "indie rock", "indie folk", "mexican indie", "latin indie", "alternative", "alt-rock", "lo-fi", "bedroom pop"),
        "Metal" to listOf("metal", "death metal", "metalcore", "thrash metal", "heavy metal", "black metal", "doom metal"),
        "Electronic" to listOf("edm", "electronic", "house", "techno", "dubstep", "synthwave", "electro", "future bass", "trance", "drum and bass"),
        "R&B / Soul" to listOf("r&b", "soul", "neo soul", "funk", "contemporary r&b", "motown", "quiet storm"),
        "Reggaetón / Urbano" to listOf("reggaeton", "latin trap", "urbano latino", "dembow", "trap latino", "reggaeton mexa"),
        "Corridos / Regional Mexicano" to listOf("corrido", "corridos tumbados", "corridos bélicos", "norteño", "mariachi", "banda", "grupera"),
        "Folk / World / Tradicional" to listOf("bolero", "folk", "latin folk", "flamenco", "música andina", "cumbia", "vallenato", "chicha", "world"),
        "Otros / Desconocido" to listOf()
    )

    val allTracks = genresMap.values.flatten()
    val errores = mutableListOf<String>()

    for (track in allTracks) {
        val categoriasEsperadas = mutableSetOf<String>()

        for (g in track.genres) {
            for ((categoria, keywords) in broadCategories) {
                if (keywords.any { kw -> g.lowercase().contains(kw.lowercase()) }) {
                    categoriasEsperadas.add(categoria)
                }
            }
        }

        val categoriasAsignadas = genresMap.filterValues { list -> list.any { it.track_id == track.track_id } }.keys.toSet()
        val faltantes = categoriasEsperadas - categoriasAsignadas

        if (faltantes.isNotEmpty()) {
            errores.add("❌ '${track.track_name}' debería estar en $categoriasEsperadas, pero está en $categoriasAsignadas")
        }
    }

    val totalVerificadosUnicos = allTracks.mapNotNull { it.track_id }.toSet().size
    Log.d("ValidaciónCrucial", "✅ Tracks verificados (apariciones totales): ${allTracks.size}")
    Log.d("ValidaciónCrucial", "🎧 Tracks verificados (únicos): $totalVerificadosUnicos")
    Log.d("ValidaciónCrucial", "⚠️ Tracks mal clasificados: ${errores.size}")
    errores.take(10).forEach { Log.d("ValidaciónCrucial", it) }
}

@Composable
fun CategoryButtons(accessToken: String, categories: Map<String, List<Track>>) {
    val context = LocalContext.current
    val loadingMap = remember { mutableStateMapOf<String, Boolean>() }

    Column {
        categories.forEach { (genero, tracks) ->
            val isLoading = loadingMap[genero] ?: false

            if (isLoading) {
                Column(modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(id = R.string.creating_playlist, genero),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Button(
                    onClick = {
                        loadingMap[genero] = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val userId = obtenerUserId(accessToken)
                            val exito = if (userId != null) {
                                crearPlaylistPorGenero(accessToken, userId, genero, tracks)
                            } else false

                            withContext(Dispatchers.Main) {
                                loadingMap[genero] = false
                                val mensaje = if (exito) {
                                    context.getString(R.string.playlist_created, genero)
                                } else {
                                    context.getString(R.string.playlist_failed, genero)
                                }
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = stringResource(id = R.string.create_playlist, genero))
                }
            }
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

suspend fun crearPlaylistPorGenero(
    accessToken: String,
    userId: String,
    genero: String,
    tracks: List<Track>  // ⬅️ Cambiamos de List<String> (URIs) a List<Track>
): Boolean {
    val authHeader = "Bearer $accessToken"
    val trackUris = tracks.map { "spotify:track:${it.track_id}" }

    // 🧱 Paso 1: Crear la playlist
    val playlistRequest = PlaylistRequest(
        name = "Playlist de $genero",
        description = "Playlist automática para el género $genero",
        public = true
    )
    val playlistResponse = RetrofitClientSpotify.instance.crearPlaylist(userId, authHeader, playlistRequest)
    if (!playlistResponse.isSuccessful) {
        Log.e("crearPlaylistPorGenero", "❌ Error creando playlist: ${playlistResponse.errorBody()?.string()}")
        return false
    }

    val playlistId = playlistResponse.body()?.id
    if (playlistId == null) {
        Log.e("crearPlaylistPorGenero", "❌ Playlist creada, pero no se obtuvo el ID")
        return false
    }

    // 🎵 Paso 2: Agregar canciones en bloques de 100
    val chunks = trackUris.chunked(100)
    for (chunk in chunks) {
        val addResponse = RetrofitClientSpotify.instance.agregarCanciones(
            playlistId,
            authHeader,
            AddTracksRequest(chunk)
        )
        if (!addResponse.isSuccessful) {
            Log.e("crearPlaylistPorGenero", "❌ Error agregando canciones: ${addResponse.errorBody()?.string()}")
            return false
        }
    }

    // ✅ Paso 3a: Validación por cantidad
    val verifyResponse = RetrofitClientSpotify.instance.getPlaylistTracks(playlistId, authHeader)
    if (!verifyResponse.isSuccessful) {
        Log.e("crearPlaylistPorGenero", "❌ No se pudo verificar la playlist: ${verifyResponse.errorBody()?.string()}")
        return false
    }

    val responseBody = verifyResponse.body()
    val totalEnSpotify = responseBody?.total ?: -1
    val totalEsperado = trackUris.size

    if (totalEnSpotify == totalEsperado) {
        Log.d("crearPlaylistPorGenero", "✅ Playlist '$genero' completa: $totalEnSpotify canciones.")
    } else {
        Log.w("crearPlaylistPorGenero", "⚠️ Playlist '$genero' incompleta: $totalEnSpotify de $totalEsperado canciones.")
    }

    // ✅ Paso 3b: Validación por nombre + artista
    val items = obtenerTodasLasCancionesPlaylist(playlistId, authHeader)

    if (items != null) {
        val spotifyTracks = items.map {
            val name = it.track.name
            val artists = it.track.artists.joinToString(", ") { artist -> artist.name }
            "$name - $artists"
        }

        val esperadas = tracks.map {
            "${it.track_name} - ${it.artist_names.joinToString(", ")}"
        }

        val faltantes = esperadas.toSet() - spotifyTracks.toSet()
        val extras = spotifyTracks.toSet() - esperadas.toSet()

        Log.d("crearPlaylistPorGenero", "🎯 Validación detallada: Esperadas=${esperadas.size}, En Spotify=${spotifyTracks.size}")
        faltantes.take(5).forEach { Log.w("crearPlaylistPorGenero", "❌ Faltante en Spotify: $it") }
        extras.take(5).forEach { Log.w("crearPlaylistPorGenero", "⚠️ Extra no esperada en Spotify: $it") }

        if (faltantes.isEmpty() && extras.isEmpty()) {
            Log.d("crearPlaylistPorGenero", "✅ Todas las canciones coinciden perfectamente (nombre + artistas).")
            esperadas.forEach {
                Log.d("crearPlaylistPorGenero", "✅ Confirmado en Spotify: $it")
            }
        }
    }

    return true
}

suspend fun obtenerTodasLasCancionesPlaylist(
    playlistId: String,
    authHeader: String
): List<PlaylistTrackItem> {
    val allItems = mutableListOf<PlaylistTrackItem>()
    var offset = 0
    val limit = 100

    while (true) {
        val response = RetrofitClientSpotify.instance.getPlaylistTracksPaged(
            playlistId,
            authHeader,
            limit,
            offset
        )

        if (!response.isSuccessful) break

        val items = response.body()?.items ?: break
        allItems.addAll(items)

        if (items.size < limit) break
        offset += limit
    }

    return allItems
}




