package com.example.spotifylikedsongsgenresorter.model

data class PlaylistTracksResponse(
    val items: List<PlaylistTrackItem>,
    val total: Int
)

data class PlaylistTrackItem(
    val track: PlaylistTrack
)

data class PlaylistTrack(
    val id: String?,
    val name: String,
    val artists: List<PlaylistArtist>
)

data class PlaylistArtist(
    val name: String
)
