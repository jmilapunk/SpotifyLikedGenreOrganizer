package com.example.spotifylikedsongsgenresorter.UserInterface

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.spotifylikedsongsgenresorter.R

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Column {
        Text(text = stringResource(id = R.string.login_title), fontSize = 24.sp)
        Button(onClick = { onLoginClick() }) {
            Text(text = stringResource(id = R.string.login_button))
        }
    }
}
