package com.example.spotifylikedsongsgenresorter.api

import com.example.spotifylikedsongsgenresorter.model.Track
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FlaskLikedSongsService {
    @GET("/liked_songs_genres")
    suspend fun getLikedSongsGenres(
        @Query("access_token") accessToken: String
    ): Response<Map<String, List<Track>>>
}
