/**
 * Melodix Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Minimal YouTube options screen (Settings > Integrations > YouTube Music):
 * Login, Login with token, Select playlists to sync. Nothing else.
 */

package com.melodix.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.melodix.music.LocalPlayerAwareWindowInsets
import com.melodix.music.R
import com.melodix.music.constants.AccountChannelHandleKey
import com.melodix.music.constants.AccountEmailKey
import com.melodix.music.constants.AccountNameKey
import com.melodix.music.constants.DataSyncIdKey
import com.melodix.music.constants.InnerTubeCookieKey
import com.melodix.music.constants.PoTokenKey
import com.melodix.music.constants.VisitorDataKey
import com.melodix.music.ui.screens.buildLoginRoute
import com.melodix.music.ui.component.IconButton
import com.melodix.music.ui.component.PreferenceEntry
import com.melodix.music.ui.utils.backToMain
import com.melodix.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeAccountScreen(navController: NavController) {
    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (poToken, onPoTokenChange) = rememberPreference(PoTokenKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in innerTubeCookie }
    val connectedLabel = accountNamePref.ifEmpty { accountEmail.ifEmpty { "YouTube" } }

    var showTokenEditor by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

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

        // 1) Login / connected state
        PreferenceEntry(
            title = {
                Text(
                    if (isLoggedIn) {
                        stringResource(R.string.youtube_logged_in_as, connectedLabel)
                    } else {
                        stringResource(R.string.login)
                    }
                )
            },
            description = stringResource(R.string.youtube_integration_description),
            icon = {
                Icon(
                    painterResource(if (isLoggedIn) R.drawable.account else R.drawable.login),
                    null,
                )
            },
            onClick = {
                if (isLoggedIn) {
                    navController.navigate("account")
                } else {
                    navController.navigate(buildLoginRoute())
                }
            },
        )

        // 2) Login with token
        PreferenceEntry(
            title = { Text(stringResource(R.string.advanced_login)) },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showTokenEditor = true },
        )

        // 3) Select playlists to sync
        PreferenceEntry(
            title = { Text(stringResource(R.string.select_playlist_to_sync)) },
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            onClick = { showPlaylistDialog = true },
        )
    }

    if (showTokenEditor) {
        TokenEditorDialog(
            innerTubeCookie = innerTubeCookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            accountNamePref = accountNamePref,
            accountEmail = accountEmail,
            accountChannelHandle = accountChannelHandle,
            onInnerTubeCookieChange = onInnerTubeCookieChange,
            onPoTokenChange = onPoTokenChange,
            onVisitorDataChange = onVisitorDataChange,
            onDataSyncIdChange = onDataSyncIdChange,
            onAccountNameChange = onAccountNameChange,
            onAccountEmailChange = onAccountEmailChange,
            onAccountChannelHandleChange = onAccountChannelHandleChange,
            onDismiss = { showTokenEditor = false },
        )
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(onDismiss = { showPlaylistDialog = false })
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
