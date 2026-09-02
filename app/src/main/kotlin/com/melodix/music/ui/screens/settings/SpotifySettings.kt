/**
 * Spotify integration settings (GPL-3.0).
 * Qobuz options live in their own screen (QobuzSettingsScreen).
 */

package com.melodix.music.ui.screens.settings

import timber.log.Timber
import com.melodix.music.db.MusicDatabase
import androidx.compose.ui.Alignment
import android.webkit.CookieManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.melodix.music.LocalDatabase
import com.melodix.music.LocalPlayerAwareWindowInsets
import com.melodix.music.R
import com.melodix.music.constants.EnableSpotifyKey
import com.melodix.music.constants.SpotifyAccessTokenKey
import com.melodix.music.constants.HideYtmLikedSongsKey
import com.melodix.music.constants.SpotifyHomeOnlyKey
import com.melodix.music.constants.SpotifySyncLikesKey
import com.melodix.music.constants.SpotifySpDcKey
import com.melodix.music.constants.SpotifySpKeyKey
import com.melodix.music.constants.SpotifyTokenExpiryKey
import com.melodix.music.constants.SpotifyUserIdKey
import com.melodix.music.constants.SpotifyUsernameKey
import com.melodix.music.constants.UseSpotifyHomeKey
import com.melodix.music.constants.UseSpotifySearchKey
import com.melodix.music.models.MediaMetadata
import com.melodix.music.playback.SpotifyMetadataRegistry
import com.melodix.music.playback.SpotifyYouTubeMapper
import com.melodix.music.spotify.Spotify
import com.melodix.music.spotify.models.SpotifyTrack as SpotifyTrackLike
import com.melodix.music.playback.SpotifyProfileCache
import com.melodix.music.ui.component.IconButton
import com.melodix.music.ui.component.PreferenceEntry
import com.melodix.music.ui.component.PreferenceGroupTitle
import com.melodix.music.ui.component.SwitchPreference
import com.melodix.music.ui.utils.backToMain
import com.melodix.music.utils.rememberPreference
import java.time.LocalDateTime
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var spotifyUsername by rememberPreference(SpotifyUsernameKey, "")
    var spotifyAccessToken by rememberPreference(SpotifyAccessTokenKey, "")
    var spotifySpDc by rememberPreference(SpotifySpDcKey, "")
    var spotifySpKey by rememberPreference(SpotifySpKeyKey, "")
    var spotifyTokenExpiry by rememberPreference(SpotifyTokenExpiryKey, 0L)
    var spotifyUserId by rememberPreference(SpotifyUserIdKey, "")

    val (enableSpotify, onEnableSpotifyChange) = rememberPreference(
        key = EnableSpotifyKey,
        defaultValue = false,
    )

    val isLoggedIn = remember(spotifyAccessToken, spotifySpDc) {
        spotifyAccessToken.isNotEmpty() && spotifySpDc.isNotEmpty()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.account),
        )

        PreferenceEntry(
            title = {
                Text(
                    text = if (isLoggedIn) spotifyUsername else stringResource(R.string.spotify_not_logged_in),
                    modifier = Modifier.alpha(if (isLoggedIn) 1f else 0.5f),
                )
            },
            description = null,
            icon = { Icon(painterResource(R.drawable.spotify), null) },
            trailingContent = {
                if (isLoggedIn) {
                    OutlinedButton(onClick = {
                        spotifyAccessToken = ""
                        spotifySpDc = ""
                        spotifySpKey = ""
                        spotifyTokenExpiry = 0L
                        spotifyUsername = ""
                        spotifyUserId = ""
                        onEnableSpotifyChange(false)
                        CookieManager.getInstance().removeAllCookies(null)
                        // Clear all user-specific Spotify caches so a subsequent login
                        // (possibly a different account) never surfaces stale data.
                        SpotifyMetadataRegistry.clearAll()
                        coroutineScope.launch { SpotifyProfileCache.clearAllPersisted(context) }
                    }) {
                        Text(stringResource(R.string.action_logout))
                    }
                } else {
                    OutlinedButton(
                        onClick = { navController.navigate("settings/spotify/login") },
                    ) {
                        Text(stringResource(R.string.action_login))
                    }
                }
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.options),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.spotify_enable)) },
            description = stringResource(R.string.spotify_enable_description),
            checked = enableSpotify,
            onCheckedChange = onEnableSpotifyChange,
            isEnabled = isLoggedIn,
        )

        if (isLoggedIn && enableSpotify) {
            val (useSpotifySearch, onUseSpotifySearchChange) = rememberPreference(
                key = UseSpotifySearchKey,
                defaultValue = false,
            )
            val (useSpotifyHome, onUseSpotifyHomeChange) = rememberPreference(
                key = UseSpotifyHomeKey,
                defaultValue = false,
            )
            val (spotifyHomeOnly, onSpotifyHomeOnlyChange) = rememberPreference(
                key = SpotifyHomeOnlyKey,
                defaultValue = false,
            )

            SwitchPreference(
                title = { Text(stringResource(R.string.spotify_use_for_search)) },
                description = stringResource(R.string.spotify_use_for_search_description),
                checked = useSpotifySearch,
                onCheckedChange = onUseSpotifySearchChange,
            )

            SwitchPreference(
                title = { Text(stringResource(R.string.spotify_use_for_home)) },
                description = stringResource(R.string.spotify_use_for_home_description),
                checked = useSpotifyHome,
                onCheckedChange = onUseSpotifyHomeChange,
            )

            if (useSpotifyHome) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.spotify_home_only)) },
                    description = stringResource(R.string.spotify_home_only_description),
                    checked = spotifyHomeOnly,
                    onCheckedChange = onSpotifyHomeOnlyChange,
                )
            }

            val (syncLikes, onSyncLikesChange) = rememberPreference(
                key = SpotifySyncLikesKey,
                defaultValue = false,
            )

            val database = LocalDatabase.current
            val syncProgress by SpotifyLikeSyncState.progress.collectAsState()
            val isSyncing by SpotifyLikeSyncState.isSyncing.collectAsState()

            SwitchPreference(
                title = { Text(stringResource(R.string.spotify_sync_likes)) },
                description = stringResource(R.string.spotify_sync_likes_description),
                checked = syncLikes,
                onCheckedChange = { enabled ->
                    onSyncLikesChange(enabled)
                    if (enabled) {
                        coroutineScope.launch {
                            runInitialSpotifyLikeSync(
                                database = database,
                                onComplete = { count ->
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.spotify_sync_likes_complete, count),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                onError = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.spotify_sync_likes_failed),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            )
                        }
                    }
                },
                isEnabled = !isSyncing,
            )

            if (isSyncing) {
                PreferenceEntry(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.size(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.spotify_sync_likes_syncing,
                                    syncProgress.first,
                                    syncProgress.second,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    description = null,
                    icon = {},
                )
            }

            if (syncLikes) {
                val (hideYtmLikedSongs, onHideYtmLikedSongsChange) = rememberPreference(
                    key = HideYtmLikedSongsKey,
                    defaultValue = true,
                )
                SwitchPreference(
                    title = { Text(stringResource(R.string.hide_ytm_liked_songs)) },
                    description = stringResource(R.string.hide_ytm_liked_songs_description),
                    checked = hideYtmLikedSongs,
                    onCheckedChange = onHideYtmLikedSongsChange,
                )
            }

            PreferenceEntry(
                title = { Text(stringResource(R.string.spotify_source_spotify)) },
                description = stringResource(R.string.spotify_use_for_home_description),
                icon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = { navController.navigate("spotify_home") },
            )

            PreferenceEntry(
                title = { Text(stringResource(R.string.spotify_preload_songs)) },
                description = stringResource(R.string.spotify_preload_songs_description),
                icon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = { navController.navigate("settings/spotify/preload") },
            )

            PreferenceGroupTitle(
                title = stringResource(R.string.information),
            )

            PreferenceEntry(
                title = {
                    Text(
                        text = stringResource(R.string.spotify_mapping_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                description = null,
                icon = {
                    Icon(
                        painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.spotify_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

/**
 * Observable state for the initial Spotify to local like sync.
 */
internal object SpotifyLikeSyncState {
    /** (current, total) */
    val progress = MutableStateFlow(0 to 0)
    val isSyncing = MutableStateFlow(false)
}

/**
 * Pulls all liked songs from Spotify, resolves each to a YouTube equivalent
 * via [SpotifyYouTubeMapper], and marks them as liked in Room.
 */
internal suspend fun runInitialSpotifyLikeSync(
    database: MusicDatabase,
    onComplete: (Int) -> Unit,
    onError: (Throwable) -> Unit,
) {
    if (SpotifyLikeSyncState.isSyncing.value) return
    SpotifyLikeSyncState.isSyncing.value = true
    SpotifyLikeSyncState.progress.value = 0 to 0

    try {
        withContext(Dispatchers.IO) {
            val mapper = SpotifyYouTubeMapper(database)
            val allTracks = mutableListOf<SpotifyTrackLike>()

            var offset = 0
            val pageSize = 50
            while (true) {
                val page = Spotify.likedSongs(limit = pageSize, offset = offset).getOrThrow()
                allTracks.addAll(page.items.mapNotNull { it.track })
                SpotifyLikeSyncState.progress.value = allTracks.size to page.total
                if (allTracks.size >= page.total || page.items.isEmpty()) break
                offset += pageSize
            }

            Timber.d("SpotifyLikeSync: fetched ${allTracks.size} liked songs from Spotify")

            val now = LocalDateTime.now()
            var synced = 0

            data class ResolvedTrack(
                val metadata: MediaMetadata,
                val index: Int,
            )

            val resolved = mutableListOf<ResolvedTrack>()
            allTracks.forEachIndexed { index, spotifyTrack ->
                try {
                    val metadata = mapper.mapToYouTube(spotifyTrack)
                    if (metadata != null) {
                        resolved.add(ResolvedTrack(metadata, index))
                    }
                    SpotifyLikeSyncState.progress.value = (index + 1) to allTracks.size
                } catch (e: Exception) {
                    Timber.w(e, "SpotifyLikeSync: failed to resolve ${spotifyTrack.id}")
                }
            }

            Timber.d("SpotifyLikeSync: resolved ${resolved.size}/${allTracks.size} tracks, writing to DB")

            database.withTransaction {
                resolved.forEach { (metadata, index) ->
                    try {
                        val existingSong = getSongByIdBlocking(metadata.id)
                        if (existingSong == null) {
                            insert(metadata) {
                                it.copy(liked = true, likedDate = now.minusSeconds(index.toLong()))
                            }
                        } else if (!existingSong.song.liked) {
                            update(existingSong.song.copy(liked = true, likedDate = now.minusSeconds(index.toLong())))
                        }
                        synced++
                    } catch (e: Exception) {
                        Timber.w(e, "SpotifyLikeSync: failed to insert ${metadata.id}")
                    }
                }
            }

            Timber.d("SpotifyLikeSync: synced $synced/${allTracks.size} songs to local DB")
            withContext(Dispatchers.Main) { onComplete(synced) }
        }
    } catch (e: Exception) {
        Timber.e(e, "SpotifyLikeSync: initial sync failed")
        withContext(Dispatchers.Main) { onError(e) }
    } finally {
        SpotifyLikeSyncState.isSyncing.value = false
    }
}
