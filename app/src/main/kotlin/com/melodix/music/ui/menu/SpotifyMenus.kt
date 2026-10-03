/**
 * Melodix Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Slim Spotify editing menus: playlist rename + track add/remove,
 * built on Melodix's own GridMenu primitives.
 */

package com.melodix.music.ui.menu

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.melodix.music.R
import com.melodix.music.spotify.Spotify
import com.melodix.music.spotify.models.SpotifyPlaylist
import com.melodix.music.spotify.models.SpotifyTrack
import com.melodix.music.ui.component.GridMenu
import com.melodix.music.ui.component.GridMenuItem
import com.melodix.music.ui.component.ListDialog
import com.melodix.music.ui.component.TextFieldDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Long-press menu for a Spotify playlist: play + rename.
 */
@Composable
fun SpotifyPlaylistMenu(
    playlist: SpotifyPlaylist,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onRenamed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showRename by remember { mutableStateOf(false) }

    GridMenu {
        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
            onClick = {
                onDismiss()
                onPlay()
            },
        )
        GridMenuItem(
            icon = R.drawable.edit,
            title = R.string.spotify_rename_playlist,
            onClick = { showRename = true },
        )
    }

    if (showRename) {
        TextFieldDialog(
            initialTextFieldValue = TextFieldValue(playlist.name),
            title = { Text(stringResource(R.string.spotify_rename_playlist)) },
            onDismiss = { showRename = false },
            onDone = { newName ->
                showRename = false
                onDismiss()
                scope.launch(Dispatchers.IO) {
                    Spotify.editPlaylistAttributes(playlist.id, newName = newName)
                        .onSuccess {
                            withContext(Dispatchers.Main) {
                                onRenamed()
                                Toast.makeText(context, context.getString(R.string.spotify_track_added_to_playlist, newName), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .onFailure { e ->
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, e.message ?: context.getString(R.string.spotify_add_to_playlist_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
        )
    }
}

/**
 * Long-press menu for a Spotify track: add to one of my playlists,
 * and (inside a playlist context) remove from this playlist.
 */
@Composable
fun SpotifyTrackMenu(
    track: SpotifyTrack,
    playlistId: String? = null,
    onDismiss: () -> Unit,
    onChanged: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    GridMenu {
        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.spotify_add_to_playlist,
            onClick = { showPicker = true },
        )
        if (playlistId != null) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.spotify_remove_from_playlist,
                onClick = {
                    onDismiss()
                    scope.launch(Dispatchers.IO) {
                        val result = runCatching {
                            // find the playlist item uid for this track, then remove it
                            var uid: String? = null
                            var offset = 0
                            while (uid == null) {
                                val page = Spotify.playlistTracks(playlistId, limit = 100, offset = offset).getOrNull() ?: break
                                uid = page.items.firstOrNull { it.track?.id == track.id }?.uid
                                offset += page.items.size
                                if (page.items.isEmpty() || offset >= (page.total ?: 0)) break
                            }
                            if (uid != null) {
                                Spotify.removeTracksFromPlaylist(
                                    playlistId,
                                    listOf(Spotify.PlaylistItemRef(uri = track.uri ?: "spotify:track:${track.id}", uid = uid)),
                                ).getOrThrow()
                            }
                        }
                        withContext(Dispatchers.Main) {
                            result
                                .onSuccess {
                                    Toast.makeText(context, context.getString(R.string.spotify_track_removed), Toast.LENGTH_SHORT).show()
                                    onChanged()
                                }
                                .onFailure { e ->
                                    Toast.makeText(context, e.message ?: context.getString(R.string.spotify_add_to_playlist_failed), Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                },
            )
        }
    }

    if (showPicker) {
        SpotifyPlaylistPickerDialog(
            track = track,
            onDismiss = { showPicker = false },
            onAdded = onChanged,
        )
    }
}

/**
 * Lists the user's Spotify playlists; tapping one adds [track] to it.
 */
@Composable
private fun SpotifyPlaylistPickerDialog(
    track: SpotifyTrack,
    onDismiss: () -> Unit,
    onAdded: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var playlists by remember { mutableStateOf<List<SpotifyPlaylist>?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        playlists = withContext(Dispatchers.IO) {
            Spotify.myPlaylists(limit = 50).getOrNull()?.items
        }
    }

    ListDialog(onDismiss = onDismiss) {
        val list = playlists
        if (list == null) {
            item {
                Text(
                    text = stringResource(R.string.spotify_searching),
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else if (list.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.spotify_no_playlists),
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            items(list, key = { it.id }) { playlist ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            scope.launch(Dispatchers.IO) {
                                Spotify.addTracksToPlaylist(
                                    playlist.id,
                                    listOf(track.uri ?: "spotify:track:${track.id}"),
                                ).onSuccess {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, context.getString(R.string.spotify_track_added_to_playlist, playlist.name), Toast.LENGTH_SHORT).show()
                                        onAdded()
                                    }
                                }.onFailure { e ->
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, e.message ?: context.getString(R.string.spotify_add_to_playlist_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
