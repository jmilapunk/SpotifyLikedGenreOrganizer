package com.example.spotifylikedsongsgenresorter.api

import com.example.spotifylikedsongsgenresorter.model.PlaylistRequest
import com.example.spotifylikedsongsgenresorter.model.PlaylistResponse
import com.example.spotifylikedsongsgenresorter.model.AddTracksRequest
import com.example.spotifylikedsongsgenresorter.model.PlaylistTracksResponse
import retrofit2.Response
import retrofit2.http.*

interface SpotifyPlaylistService {

    // 🎯 Crear una playlist nueva para el usuario
    @POST("v1/users/{user_id}/playlists")
    suspend fun crearPlaylist(
        @Path("user_id") userId: String,
        @Header("Authorization") authHeader: String,
        @Body playlistRequest: PlaylistRequest
    ): Response<PlaylistResponse>

    // ➕ Agregar canciones a una playlist existente
    @POST("v1/playlists/{playlist_id}/tracks")
    suspend fun agregarCanciones(
        @Path("playlist_id") playlistId: String,
        @Header("Authorization") authHeader: String,
        @Body addTracksRequest: AddTracksRequest
    ): Response<Unit>

    // 👀 Ver cuántas canciones hay actualmente en la playlist
    @GET("v1/playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("playlist_id") playlistId: String,
        @Header("Authorization") authHeader: String
    ): Response<PlaylistTracksResponse>
}
