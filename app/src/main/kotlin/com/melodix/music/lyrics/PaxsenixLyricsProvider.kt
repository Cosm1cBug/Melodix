/**
 * Melodix Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.melodix.music.lyrics

import android.content.Context
import com.melodix.music.App
import com.melodix.paxsenix.Paxsenix
import com.melodix.music.constants.EnablePaxsenixKey
import com.melodix.music.utils.dataStore
import com.melodix.music.utils.get
import timber.log.Timber

object PaxsenixLyricsProvider : LyricsProvider {
    private const val TAG = "PaxsenixProvider"
    
    override val name = "Paxsenix"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixKey] ?: true

    private suspend fun getLyricsCtx(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        Timber.tag(TAG).d("getLyrics called: title='$title', artist='$artist', duration=$duration")
        
        try {
            Paxsenix.init(context)
            val result = Paxsenix.getLyrics(title, artist, duration, album)
            
            result.onSuccess { lyrics ->
                Timber.tag(TAG).i("Success! Got ${lyrics.length} chars of lyrics")
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to get lyrics")
            }
            
            return result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception in getLyrics")
            return Result.failure(e)
        }
    }

    private suspend fun getAllLyricsCtx(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        Timber.tag(TAG).d("getAllLyrics called")
        try {
            Paxsenix.init(context)
            Paxsenix.getAllLyrics(title, artist, duration, album, callback)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching lyrics from Paxsenix")
            callback("")
        }
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = getLyricsCtx(App.instance, id, title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getAllLyricsCtx(App.instance, id, title, artist, duration, album, callback)
    }
}
