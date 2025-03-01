package com.example.spotifylikedsongsgenresorter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val CLIENT_ID = "7fb1e0a4d10844b596cce58f649d956c"
    private val REDIRECT_URI = "http://192.168.0.24:8888/callback"
   // private val REDIRECT_URI = "http://127.0.0.1:8888/callback"
    private val AUTH_URL = "https://accounts.spotify.com/authorize"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen()
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
}
