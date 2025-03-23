package com.example.spotifylikedsongsgenresorter.api

import Track
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyService {
    @GET("/liked_songs_genres")
    suspend fun getLikedSongsGenres(
        @Query("access_token") accessToken: String
    ): Response<Map<String, List<Track>>>
}
