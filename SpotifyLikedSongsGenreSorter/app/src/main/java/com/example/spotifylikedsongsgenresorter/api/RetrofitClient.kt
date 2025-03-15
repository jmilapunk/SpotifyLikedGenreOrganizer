package com.example.spotifylikedsongsgenresorter.api
// ^ Ajusta el package a la ruta real de tu proyecto

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://likedgenresorter.com"
    // Reemplaza con tu IP o dominio de DigitalOcean si es distinto

    val instance: SpotifyService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Convierte JSON en objetos Kotlin usando Gson
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // SpotifyService es la interfaz que define tus endpoints
        retrofit.create(SpotifyService::class.java)
    }
}