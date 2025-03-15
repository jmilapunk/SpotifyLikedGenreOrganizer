package com.example.spotifylikedsongsgenresorter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.spotifylikedsongsgenresorter.api.RetrofitClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val CLIENT_ID = "7fb1e0a4d10844b596cce58f649d956c"
    //private val REDIRECT_URI = "http://192.168.0.24:8888/callback"
   // private val REDIRECT_URI = "http://127.0.0.1:8888/callback"
    //private val REDIRECT_URI = "http://147.182.224.17:8888/callback"
    private val REDIRECT_URI = "spotifylikedsongsgenresorter://callback"
    private val AUTH_URL = "https://accounts.spotify.com/authorize"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        val accessToken = data?.let { uri ->
            val tokenFromFragment = Uri.parse("?" + uri.fragment).getQueryParameter("access_token")
            val tokenFromQuery = uri.getQueryParameter("access_token")
            tokenFromFragment ?: tokenFromQuery
        }

        // Imprime el token en la consola y Logcat
        if (accessToken != null) {
            println("Token recibido: $accessToken")
            Log.d("MainActivity", "Token recibido: $accessToken")
        } else {
            println("No se recibió token")
            Log.d("MainActivity", "No se recibió token")
        }

        setContent {
            if (accessToken != null) {
                // Ahora llamamos a DataScreen en vez de un simple Text
                DataScreen(accessToken = accessToken)
            } else {
                LoginScreen()
            }
        }
    }



    @Composable
    fun LoginScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "Login with Spotify",
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
            Button(
                onClick = { openSpotifyLogin() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Login")
            }
        }
    }

    private fun openSpotifyLogin() {
        val authIntent = Intent(Intent.ACTION_VIEW, Uri.parse(
            "$AUTH_URL?client_id=$CLIENT_ID&response_type=token&redirect_uri=$REDIRECT_URI&scope=user-library-read"
        ))
        startActivity(authIntent)
    }

    private fun fetchLikedSongsGenres(accessToken: String) {
        // Llamada asíncrona para no bloquear la UI
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getLikedSongsGenres(accessToken)
                if (response.isSuccessful) {
                    val data = response.body() // data es un Map<String, List<Track>>?
                    // Aquí puedes hacer lo que quieras con los datos, por ejemplo:
                    println("Data recibida: $data")
                    // Podrías almacenar esto en una variable global o un ViewModel
                } else {
                    println("Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
