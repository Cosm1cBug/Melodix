/*
 * Melodix Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.melodix.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.music.innertube.YouTube
import com.melodix.music.innertube.models.filterExplicit
import com.melodix.music.innertube.models.filterVideo
import com.melodix.music.constants.EnableSpotifyKey
import com.melodix.music.constants.SpotifyAccessTokenKey
import com.melodix.music.constants.UseSpotifySearchKey
import com.melodix.music.innertube.pages.SearchSummary
import com.melodix.music.innertube.pages.SearchSummaryPage
import com.melodix.music.spotify.Spotify
import com.melodix.music.utils.SpotifyTokenManager
import com.melodix.music.utils.toAlbumItem
import com.melodix.music.utils.toArtistItem
import com.melodix.music.utils.toPlaylistItem
import com.melodix.music.utils.toSongItem
import com.melodix.music.constants.HideExplicitKey
import com.melodix.music.constants.HideVideoKey
import com.melodix.music.models.ItemsPage
import com.melodix.music.utils.dataStore
import com.melodix.music.utils.get
import com.melodix.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    val isSpotifySearch = MutableStateFlow(false)

    private suspend fun shouldUseSpotifySearch(): Boolean {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[EnableSpotifyKey] ?: false
        val useForSearch = prefs[UseSpotifySearchKey] ?: false
        val hasToken = (prefs[SpotifyAccessTokenKey] ?: "").isNotEmpty()
        return enabled && useForSearch && hasToken
    }

    private suspend fun loadYouTubeSummary() {
        YouTube
            .searchSummary(query)
            .onSuccess {
                summaryPage = it.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(context.dataStore.get(HideVideoKey, false))
            }.onFailure {
                reportException(it)
            }
    }

    private suspend fun loadSpotifySummary() {
        if (!SpotifyTokenManager.ensureAuthenticated()) {
            isSpotifySearch.value = false
            loadYouTubeSummary()
            return
        }
        val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            Spotify.search(query, types = listOf("track", "album", "artist", "playlist"), limit = 20).getOrNull()
        }
        if (res == null) {
            isSpotifySearch.value = false
            loadYouTubeSummary()
            return
        }
        val summaries = mutableListOf<SearchSummary>()
        res.tracks?.items?.takeIf { it.isNotEmpty() }?.let { tracks ->
            summaries += SearchSummary("Songs", tracks.map { it.toSongItem() })
        }
        res.albums?.items?.takeIf { it.isNotEmpty() }?.let { albums ->
            summaries += SearchSummary("Albums", albums.map { it.toAlbumItem() })
        }
        res.artists?.items?.takeIf { it.isNotEmpty() }?.let { artists ->
            summaries += SearchSummary("Artists", artists.map { it.toArtistItem() })
        }
        res.playlists?.items?.takeIf { it.isNotEmpty() }?.let { pls ->
            summaries += SearchSummary("Playlists", pls.map { it.toPlaylistItem() })
        }
        summaryPage = SearchSummaryPage(summaries)
    }

    init {
        viewModelScope.launch {
            if (shouldUseSpotifySearch()) {
                isSpotifySearch.value = true
                if (summaryPage == null) loadSpotifySummary()
            }
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        if (isSpotifySearch.value) loadSpotifySummary() else loadYouTubeSummary()
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            ).filterVideo(context.dataStore.get(HideVideoKey, false)),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
