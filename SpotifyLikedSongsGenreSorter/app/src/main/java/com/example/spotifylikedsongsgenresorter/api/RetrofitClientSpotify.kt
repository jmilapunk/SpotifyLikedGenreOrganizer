package com.example.spotifylikedsongsgenresorter.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClientSpotify {
    private const val BASE_URL = "https://api.spotify.com/"
    val instance: SpotifyPlaylistService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyPlaylistService::class.java)
    }
}
