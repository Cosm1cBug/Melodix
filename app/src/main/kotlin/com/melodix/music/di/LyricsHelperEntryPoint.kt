/*
 * Melodix Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.melodix.music.di

import com.melodix.music.lyrics.LyricsHelper
import com.melodix.music.lyrics.LyricsPreloadManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsHelperEntryPoint {
    fun lyricsHelper(): LyricsHelper
    fun lyricsPreloadManager(): LyricsPreloadManager
}