/**
 * Melodix — standalone Spotify home screen (Step 3).
 * Section layout for the Spotify home integration (GPL-3.0).
 */

package com.melodix.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.melodix.music.LocalDatabase
import com.melodix.music.LocalPlayerAwareWindowInsets
import com.melodix.music.LocalPlayerConnection
import com.melodix.music.R
import com.melodix.music.innertube.YouTube
import com.melodix.music.innertube.models.ArtistItem
import com.melodix.music.playback.SpotifyProfileCache
import com.melodix.music.playback.SpotifyYouTubeMapper
import com.melodix.music.playback.queues.SpotifyLikedSongsQueue
import com.melodix.music.playback.queues.SpotifyPlaylistQueue
import com.melodix.music.playback.queues.SpotifyQueue
import com.melodix.music.ui.component.IconButton
import com.melodix.music.ui.component.NavigationTitle
import com.melodix.music.ui.component.SpotifyArtistSectionRow
import com.melodix.music.ui.component.SpotifyPlaylistSectionRow
import com.melodix.music.ui.component.SpotifyTrackSectionRow
import com.melodix.music.ui.utils.backToMain
import com.melodix.music.utils.SpotifyTokenManager
import com.melodix.music.spotify.Spotify
import com.melodix.music.spotify.models.SpotifyArtist
import com.melodix.music.spotify.models.SpotifyPlaylist
import com.melodix.music.spotify.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val mapper = remember { SpotifyYouTubeMapper(database) }

    var isLoading by remember { mutableStateOf(true) }
    var authFailed by remember { mutableStateOf(false) }
    var topTracks by remember { mutableStateOf<List<SpotifyTrack>>(emptyList()) }
    var topArtists by remember { mutableStateOf<List<SpotifyArtist>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<SpotifyPlaylist>>(emptyList()) }
    var likedSongs by remember { mutableStateOf<List<SpotifyTrack>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!SpotifyTokenManager.ensureAuthenticated()) {
            authFailed = true
            isLoading = false
            return@LaunchedEffect
        }
        try {
            coroutineScope {
                val tracksDeferred = async { SpotifyProfileCache.getTopTracks(context, database, limit = 50) }
                val artistsDeferred = async { SpotifyProfileCache.getTopArtists(context, database, limit = 20) }
                val playlistsDeferred = async {
                    Spotify.myPlaylists(limit = 50, offset = 0).getOrNull()?.items.orEmpty()
                }
                val likedDeferred = async {
                    Spotify.likedSongs(limit = 100).getOrNull()?.items.orEmpty().map { it.track }
                }
                topTracks = tracksDeferred.await()
                topArtists = artistsDeferred.await()
                playlists = playlistsDeferred.await()
                likedSongs = likedDeferred.await().filter { !it.isLocal && it.id.isNotEmpty() }
            }
        } catch (_: Exception) {
        }
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        item {
            Spacer(
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                ),
            )
        }

        if (isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (authFailed) {
            item(key = "auth_failed") {
                Text(
                    text = stringResource(R.string.spotify_session_expired),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            if (topTracks.isNotEmpty()) {
                item(key = "top_tracks_title") {
                    NavigationTitle(title = stringResource(R.string.spotify_top_tracks))
                }
                item(key = "top_tracks") {
                    SpotifyTrackSectionRow(
                        tracks = topTracks,
                        horizontalItemWidth = 280.dp,
                        isPlaying = false,
                        currentMediaId = null,
                        onTrackClick = { track ->
                            playerConnection.playQueue(
                                SpotifyQueue(
                                    initialTrack = track,
                                    mapper = mapper,
                                    context = context,
                                    database = database,
                                ),
                            )
                        },
                        onTrackLongClick = { },
                    )
                }
            }

            if (topArtists.isNotEmpty()) {
                item(key = "top_artists_title") {
                    NavigationTitle(title = stringResource(R.string.spotify_top_artists))
                }
                item(key = "top_artists") {
                    SpotifyArtistSectionRow(
                        artists = topArtists,
                        onArtistClick = { artist ->
                            scope.launch(Dispatchers.IO) {
                                val ytResult = YouTube.search(
                                    artist.name,
                                    YouTube.SearchFilter.FILTER_ARTIST,
                                ).getOrNull()
                                val ytArtist = ytResult?.items?.firstOrNull { it is ArtistItem }
                                if (ytArtist != null) {
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("artist/${ytArtist.id}")
                                    }
                                }
                            }
                        },
                    )
                }
            }

            if (likedSongs.isNotEmpty()) {
                item(key = "liked_title") {
                    NavigationTitle(title = stringResource(R.string.spotify_liked_songs))
                }
                item(key = "liked") {
                    SpotifyTrackSectionRow(
                        tracks = likedSongs,
                        horizontalItemWidth = 280.dp,
                        isPlaying = false,
                        currentMediaId = null,
                        onTrackClick = { track ->
                            val index = likedSongs.indexOf(track).coerceAtLeast(0)
                            playerConnection.playQueue(
                                SpotifyLikedSongsQueue(
                                    startIndex = index,
                                    mapper = mapper,
                                    tracks = likedSongs,
                                ),
                            )
                        },
                        onTrackLongClick = { },
                    )
                }
            }

            if (playlists.isNotEmpty()) {
                item(key = "playlists_title") {
                    NavigationTitle(title = stringResource(R.string.spotify_your_playlists))
                }
                item(key = "playlists") {
                    SpotifyPlaylistSectionRow(
                        playlists = playlists,
                        onPlaylistClick = { playlist ->
                            playerConnection.playQueue(
                                SpotifyPlaylistQueue(
                                    playlistId = playlist.id,
                                    mapper = mapper,
                                ),
                            )
                        },
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