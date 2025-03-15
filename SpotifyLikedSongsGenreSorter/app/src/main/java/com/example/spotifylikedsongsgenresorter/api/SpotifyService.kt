package com.example.spotifylikedsongsgenresorter.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.spotifylikedsongsgenresorter.model.Track


interface SpotifyService {
    @GET("/liked_songs_genres")
    suspend fun getLikedSongsGenres(
        @Query("access_token") accessToken: String
    ): Response<Map<String, List<Track>>>
}