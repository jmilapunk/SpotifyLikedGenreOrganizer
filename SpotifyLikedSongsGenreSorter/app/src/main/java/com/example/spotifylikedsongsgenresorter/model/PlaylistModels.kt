package com.example.spotifylikedsongsgenresorter.model

data class PlaylistRequest(
    val name: String,
    val description: String,
    val public: Boolean
)

data class PlaylistResponse(
    val id: String,
    val name: String
)

data class AddTracksRequest(
    val uris: List<String>
)
