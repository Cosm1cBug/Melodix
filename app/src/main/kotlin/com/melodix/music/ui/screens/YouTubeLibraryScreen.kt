/**
 * Melodix Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * YouTube Music library view: liked songs + synced YouTube playlists.
 * Same folder-view pattern as the Spotify library screen.
 */

package com.melodix.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.melodix.music.LocalPlayerAwareWindowInsets
import com.melodix.music.R
import com.melodix.music.ui.component.IconButton
import com.melodix.music.ui.component.PlaylistListItem
import com.melodix.music.ui.utils.backToMain
import com.melodix.music.viewmodels.LibraryPlaylistsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YouTubeLibraryScreen(navController: NavController) {
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val playlists by viewModel.allPlaylists.collectAsState()
    val youtubePlaylists = playlists.filter { !it.playlist.isLocal }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            ),
    ) {
        item {
            Spacer(
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                ),
            )
        }

        item(key = "yt_liked") {
            PlaylistListItem(
                playlist = com.melodix.music.db.entities.Playlist(
                    playlist = com.melodix.music.db.entities.PlaylistEntity(
                        id = "AUTO_LIKED_PLAYLISTS",
                        name = stringResource(R.string.liked),
                        isEditable = false,
                    ),
                    songCount = 0,
                    songThumbnails = emptyList(),
                ),
                autoPlaylist = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable { navController.navigate("auto_playlist/liked") },
            )
        }

        items(youtubePlaylists, key = { it.id }) { playlist ->
            PlaylistListItem(
                playlist = playlist,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable {
                        navController.navigate("local_playlist/${playlist.id}")
                    },
            )
        }

        if (youtubePlaylists.isEmpty()) {
            item(key = "yt_empty_hint") {
                Text(
                    text = stringResource(R.string.youtube_library_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.spotify_source_youtube)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}
