package com.example.spotifylikedsongsgenresorter.UserInterface

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Column {
        Text(text = "Login with Spotify", fontSize = 24.sp)
        Button(onClick = { onLoginClick() }) {
            Text(text = "Login")
        }
    }
}
