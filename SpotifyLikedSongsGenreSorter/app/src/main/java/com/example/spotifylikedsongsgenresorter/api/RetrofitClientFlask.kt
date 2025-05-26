package com.example.spotifylikedsongsgenresorter.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClientFlask {
    private const val BASE_URL = "https://likedgenresorter.com" // Tu backend
    val instance: FlaskLikedSongsService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlaskLikedSongsService::class.java)
    }
}
