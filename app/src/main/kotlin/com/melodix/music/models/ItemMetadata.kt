/*
 * Melodix Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.melodix.music.models

import androidx.compose.runtime.Immutable

@Immutable
data class ItemMetadata(
    val isLiked: Boolean = false,
    val isInLibrary: Boolean = false,
    val downloadState: Int? = null,
)
