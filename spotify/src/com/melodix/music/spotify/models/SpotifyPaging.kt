/*
 * Melodix Project (C) 2026
 * GPL-3.0 License | Contributors: see git history
 */

package com.melodix.music.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPaging<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val href: String? = null,
)
