package com.example.spotifylikedsongsgenresorter.model

data class Track(
    val track_name: String,
    val artist_names: List<String>,
    val artist_ids: List<String>,
    val genres: List<String>
)