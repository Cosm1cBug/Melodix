/*
 * Melodix Project (C) 2026
 * GPL-3.0 License | Contributors: see git history
 */

package com.melodix.music.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyArtist(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    val genres: List<String> = emptyList(),
    val popularity: Int? = null,
    val uri: String? = null,
)
