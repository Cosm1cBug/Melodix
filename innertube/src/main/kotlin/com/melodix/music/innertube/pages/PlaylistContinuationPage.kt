/*
 * Melodix Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.melodix.music.innertube.pages

import com.melodix.music.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
