package com.example.spotifylikedsongsgenresorter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.spotifylikedsongsgenresorter.UserInterface.DataScreen
import com.example.spotifylikedsongsgenresorter.UserInterface.LoginScreen
import com.example.spotifylikedsongsgenresorter.util.generateCodeChallenge
import com.example.spotifylikedsongsgenresorter.util.generateCodeVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val CLIENT_ID = "7fb1e0a4d10844b596cce58f649d956c"
    private val REDIRECT_URI = "spotifylikedsongsgenresorter://callback"
    private val AUTH_URL = "https://accounts.spotify.com/authorize"
    private val TOKEN_URL = "https://accounts.spotify.com/api/token"

    // Usamos SharedPreferences para almacenar el codeVerifier
    private var codeVerifier: String? = null

    // Este valor se actualizará cuando se intercambie el code por token
    var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)

        // Si la Activity es invocada desde el callback, recupera el codeVerifier y el auth code
        if (intent?.data?.toString()?.startsWith(REDIRECT_URI) == true) {
            codeVerifier = sharedPrefs.getString("CODE_VERIFIER", null)
            val authCode = intent?.data?.getQueryParameter("code")
            if (authCode != null) {
                exchangeCodeForToken(authCode)
            } else {
                Log.e("MainActivity", "No se recibió auth code")
            }
        }

        setContent {
            if (accessToken == null) {
                LoginScreen(onLoginClick = { openSpotifyLogin() })
            } else {
                DataScreen(accessToken = accessToken!!)
            }
        }
    }

    private fun openSpotifyLogin() {
        // Genera el code_verifier y su code_challenge para PKCE
        val verifier = generateCodeVerifier()
        codeVerifier = verifier
        val codeChallenge = generateCodeChallenge(verifier)
        // Guarda el codeVerifier en SharedPreferences para usarlo en el callback
        val sharedPrefs = getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("CODE_VERIFIER", verifier)
            apply()
        }
        // Construye la URL de autenticación usando response_type=code
        val authUri = Uri.parse(
            "$AUTH_URL?client_id=$CLIENT_ID" +
                    "&response_type=code" +
                    "&redirect_uri=$REDIRECT_URI" +
                    "&scope=user-library-read%20playlist-modify-public%20playlist-modify-private" +
                    "&code_challenge_method=S256" +
                    "&code_challenge=$codeChallenge"
        )
        val intent = Intent(Intent.ACTION_VIEW, authUri)
        startActivity(intent)
    }

    private fun exchangeCodeForToken(authCode: String) {
        val verifier = codeVerifier
        if (verifier == null) {
            Log.e("MainActivity", "codeVerifier es nulo. No se puede intercambiar el code.")
            return
        }
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", authCode)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", CLIENT_ID)
            .add("code_verifier", verifier)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val json = JSONObject(bodyStr)
                    accessToken = json.getString("access_token")
                    Log.d("MainActivity", "Access Token: $accessToken")
                    runOnUiThread {
                        setContent {
                            DataScreen(accessToken = accessToken!!)
                        }
                    }
                } else {
                    Log.e("MainActivity", "Error en el intercambio: $bodyStr")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Excepción en el intercambio: ${e.message}", e)
            }
        }
    }
}
