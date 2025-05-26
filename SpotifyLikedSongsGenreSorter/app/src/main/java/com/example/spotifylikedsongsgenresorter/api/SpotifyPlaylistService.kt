package com.example.spotifylikedsongsgenresorter.api

import com.example.spotifylikedsongsgenresorter.model.PlaylistRequest
import com.example.spotifylikedsongsgenresorter.model.PlaylistResponse
import com.example.spotifylikedsongsgenresorter.model.AddTracksRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SpotifyPlaylistService {
    @POST("v1/users/{user_id}/playlists")
    suspend fun crearPlaylist(
        @Path("user_id") userId: String,
        @Header("Authorization") authHeader: String,
        @Body playlistRequest: PlaylistRequest
    ): Response<PlaylistResponse>

    @POST("v1/playlists/{playlist_id}/tracks")
    suspend fun agregarCanciones(
        @Path("playlist_id") playlistId: String,
        @Header("Authorization") authHeader: String,
        @Body addTracksRequest: AddTracksRequest
    ): Response<Unit>
}
