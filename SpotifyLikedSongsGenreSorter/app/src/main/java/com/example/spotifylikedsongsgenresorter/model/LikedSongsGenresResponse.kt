package com.example.spotifylikedsongsgenresorter.model

import com.example.spotifylikedsongsgenresorter.model.Track

data class LikedSongsGenresResponse(
    val genres: Map<String, List<Track>>,
    val total_expected: Int,
    val total_received: Int
)