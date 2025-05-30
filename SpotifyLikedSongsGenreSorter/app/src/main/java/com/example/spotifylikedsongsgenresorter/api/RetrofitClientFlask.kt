package com.example.spotifylikedsongsgenresorter.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClientFlask {
    private const val BASE_URL = "https://likedgenresorter.com" // o tu backend real

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)   // antes 30
        .readTimeout(120, TimeUnit.SECONDS)     // antes 60
        .writeTimeout(120, TimeUnit.SECONDS)    // antes 60
        .build()

    val instance: FlaskLikedSongsService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(FlaskLikedSongsService::class.java)
    }
}
