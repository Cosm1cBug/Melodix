/**
 * Melodix Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Spotify library screens: Liked Songs and Folder browsing.
 * (Playlist browsing uses the app's existing SpotifyPlaylistScreen.)
 */

package com.melodix.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.melodix.music.LocalDatabase
import com.melodix.music.LocalPlayerAwareWindowInsets
import com.melodix.music.LocalPlayerConnection
import com.melodix.music.R
import com.melodix.music.db.entities.Playlist
import com.melodix.music.db.entities.PlaylistEntity
import com.melodix.music.playback.SpotifyYouTubeMapper
import com.melodix.music.playback.queues.SpotifyLikedSongsQueue
import com.melodix.music.spotify.Spotify
import com.melodix.music.spotify.SpotifyMapper
import com.melodix.music.spotify.models.SpotifyLibraryItem
import com.melodix.music.spotify.models.SpotifyTrack
import com.melodix.music.ui.component.IconButton
import com.melodix.music.ui.component.PlaylistListItem
import com.melodix.music.ui.component.SpotifyFolderListItem
import com.melodix.music.ui.component.YouTubeListItem
import com.melodix.music.ui.utils.backToMain
import com.melodix.music.utils.SpotifyTokenManager
import com.melodix.music.utils.toSongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_TRACKS = 500

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SpotifyTrackListScaffold(
    navController: NavController,
    title: String,
    tracks: List<SpotifyTrack>?,
    loading: Boolean,
    emptyText: String,
    onTrackClick: (Int, List<SpotifyTrack>) -> Unit,
) {
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

        if (loading) {
            item(key = "loading") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (tracks.isNullOrEmpty()) {
            item(key = "empty") {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            itemsIndexed(tracks, key = { i, t -> "sp_${i}_${t.id}" }) { index, track ->
                YouTubeListItem(
                    item = track.toSongItem(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onTrackClick(index, tracks) }),
                )
            }
        }
    }

    TopAppBar(
        title = { Text(title) },
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

@Composable
fun SpotifyLikedSongsScreen(navController: NavController) {
    var tracks by remember { mutableStateOf<List<SpotifyTrack>?>(null) }
    var loading by remember { mutableStateOf(true) }
    val database = LocalDatabase.current
    val mapper = remember { SpotifyYouTubeMapper(database) }
    val playerConnection = LocalPlayerConnection.current ?: return

    LaunchedEffect(Unit) {
        if (!SpotifyTokenManager.ensureAuthenticated()) {
            loading = false
            return@LaunchedEffect
        }
        val loaded = mutableListOf<SpotifyTrack>()
        var offset = 0
        var hasMore = true
        while (hasMore && loaded.size < MAX_TRACKS) {
            val page = withContext(Dispatchers.IO) {
                Spotify.likedSongs(limit = 100, offset = offset).getOrNull()
            } ?: break
            loaded += page.items.map { it.track }.filter { !it.isLocal && it.id.isNotEmpty() }
            offset += page.items.size
            hasMore = offset < (page.total ?: 0) && page.items.isNotEmpty()
        }
        tracks = loaded
        loading = false
    }

    SpotifyTrackListScaffold(
        navController = navController,
        title = stringResource(R.string.spotify_liked_songs),
        tracks = tracks,
        loading = loading,
        emptyText = stringResource(R.string.spotify_no_tracks),
        onTrackClick = { index, list ->
            playerConnection.playQueue(
                SpotifyLikedSongsQueue(startIndex = index, mapper = mapper, tracks = list),
            )
        },
    )
}

/**
 * Folder-style Spotify library view: Liked Songs + root folders + root playlists.
 * Opened from the "Spotify" tile in Library > Playlists.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SpotifyLibraryScreen(navController: NavController) {
    var likedTotal by remember { mutableStateOf(0) }
    var folders by remember { mutableStateOf<List<com.melodix.music.spotify.models.SpotifyLibraryFolder>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<com.melodix.music.spotify.models.SpotifyPlaylist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (!SpotifyTokenManager.ensureAuthenticated()) {
            loading = false
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            likedTotal = Spotify.likedSongs(limit = 1).getOrNull()?.total ?: 0

            val root = Spotify.myLibraryNode(folderUri = null, limit = 100).getOrNull()
            if (root != null) {
                folders = root.items.filterIsInstance<SpotifyLibraryItem.Folder>().map { it.folder }
                playlists = root.items.filterIsInstance<SpotifyLibraryItem.Playlist>().map { it.playlist }
            }
            // Fallback: if the hierarchical call failed, show the flat playlist list
            if (root == null || (folders.isEmpty() && playlists.isEmpty())) {
                val flat = mutableListOf<com.melodix.music.spotify.models.SpotifyPlaylist>()
                var offset = 0
                var hasMore = true
                while (hasMore && flat.size < MAX_TRACKS) {
                    val page = Spotify.myPlaylists(limit = 50, offset = offset).getOrNull() ?: break
                    flat += page.items
                    offset += page.items.size
                    hasMore = offset < (page.total ?: 0) && page.items.isNotEmpty()
                }
                if (flat.isNotEmpty()) {
                    folders = emptyList()
                    playlists = flat
                }
            }
        }
        loading = false
    }

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

        if (loading) {
            item(key = "loading") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            if (likedTotal > 0) {
                item(key = "spotify_liked") {
                    PlaylistListItem(
                        playlist = Playlist(
                            playlist = PlaylistEntity(
                                id = "spotify_liked_songs",
                                name = stringResource(R.string.spotify_liked_songs),
                                remoteSongCount = likedTotal,
                            ),
                            songCount = likedTotal,
                            songThumbnails = emptyList(),
                        ),
                        autoPlaylist = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable { navController.navigate("spotify_liked_songs") },
                    )
                }
            }

            items(folders, key = { "folder_${it.uri}" }) { folder ->
                val encodedUri = java.net.URLEncoder.encode(folder.uri, "UTF-8")
                val encodedName = java.net.URLEncoder.encode(folder.name, "UTF-8")
                SpotifyFolderListItem(
                    folder = folder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable {
                            navController.navigate("spotify_folder/$encodedUri?name=$encodedName")
                        },
                )
            }

            items(playlists, key = { "pl_${it.id}" }) { pl ->
                val thumb = SpotifyMapper.getPlaylistThumbnail(pl)
                PlaylistListItem(
                    playlist = Playlist(
                        playlist = PlaylistEntity(
                            id = "spotify_${pl.id}",
                            name = pl.name,
                            thumbnailUrl = thumb,
                        ),
                        songCount = 0,
                        songThumbnails = listOfNotNull(thumb),
                    ),
                    autoPlaylist = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable { navController.navigate("spotify_playlist/${pl.id}") },
                )
            }

            if (likedTotal == 0 && folders.isEmpty() && playlists.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.spotify_no_playlists),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.spotify_source_spotify)) },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SpotifyFolderScreen(
    navController: NavController,
    folderUri: String,
    folderName: String,
) {
    var items by remember { mutableStateOf<List<SpotifyLibraryItem>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(folderUri) {
        if (!SpotifyTokenManager.ensureAuthenticated()) {
            loading = false
            return@LaunchedEffect
        }
        items = withContext(Dispatchers.IO) {
            Spotify.myLibraryNode(folderUri = folderUri, limit = 100).getOrNull()?.items
        }
        loading = false
    }

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

        if (loading) {
            item(key = "loading") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (items.isNullOrEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.spotify_folder_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            items(items!!, key = { it.uri }) { entry ->
                when (entry) {
                    is SpotifyLibraryItem.Folder -> {
                        val encodedUri = java.net.URLEncoder.encode(entry.folder.uri, "UTF-8")
                        val encodedName = java.net.URLEncoder.encode(entry.folder.name, "UTF-8")
                        SpotifyFolderListItem(
                            folder = entry.folder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable {
                                    navController.navigate("spotify_folder/$encodedUri?name=$encodedName")
                                },
                        )
                    }
                    is SpotifyLibraryItem.Playlist -> {
                        val pl = entry.playlist
                        val thumb = SpotifyMapper.getPlaylistThumbnail(pl)
                        PlaylistListItem(
                            playlist = Playlist(
                                playlist = PlaylistEntity(
                                    id = "spotify_${pl.id}",
                                    name = pl.name,
                                    thumbnailUrl = thumb,
                                ),
                                songCount = 0,
                                songThumbnails = listOfNotNull(thumb),
                            ),
                            autoPlaylist = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable {
                                    navController.navigate("spotify_playlist/${pl.id}")
                                },
                        )
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(folderName.ifEmpty { stringResource(R.string.spotify_playlists) }) },
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
