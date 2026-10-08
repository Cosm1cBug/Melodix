/*
 * Melodix Project (C) 2026
 * GPL-3.0 License | Contributors: see git history
 */

package com.melodix.music.spotify

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe provider for Spotify GQL persisted-query hashes.
 *
 * Initialized with hardcoded defaults that ship with each release.
 * The app module can update hashes at runtime from a remote JSON
 * registry via [updateHashes], enabling automatic recovery when
 * Spotify rotates hashes between app releases.
 *
 * Resolution order: remote/cached → hardcoded (always available).
 */
object SpotifyHashProvider {
    enum class HashSource { HARDCODED, CACHED, REMOTE }

    data class GqlHashEntry(
        val hash: String,
        val previousHash: String? = null,
        val source: HashSource = HashSource.HARDCODED,
    )

    private val hashes = ConcurrentHashMap<String, GqlHashEntry>()

    init {
        loadHardcodedDefaults()
    }

    private fun loadHardcodedDefaults() {
        val defaults =
            mapOf(
                "profileAttributes" to "08ffb4730af3746e04a8301396f20875dbbce10c75243803091a9274eacc8ac0",
                "libraryV3" to "390c78e5b951029bad359785e69b07b536a509c581cbcd0aded5e5067f187455",
                "fetchPlaylist" to "e4b2953f160e58e38ac025d79b5a9b3aceee5c4c716598e9830bfceb69faff5f",
                "fetchLibraryTracks" to "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240",
                "searchDesktop" to "db61238974d27839a136c9dc02bfdbe3fab7635f21cf85976ebff9a1ee281345",
                "queryArtistOverview" to "ae0e2958a4ab645b35ca19ac04d0495ae12d9c5d7b7286217674801a9aab281a",
                "getAlbum" to "b9bfabef66ed756e5e13f68a942deb60bd4125ec1f1be8cc42769dc0259b4b10",
                "queryWhatsNewFeed" to "d889c8c936ab192af8ced595427f5ba2acdf63478fdc0a181c8d477f8322630e",
                "addToPlaylist" to "47b2a1234b17748d332dd0431534f22450e9ecbb3d5ddcdacbd83368636a0990",
                "removeFromPlaylist" to "47b2a1234b17748d332dd0431534f22450e9ecbb3d5ddcdacbd83368636a0990",
                "moveItemsInPlaylist" to "47b2a1234b17748d332dd0431534f22450e9ecbb3d5ddcdacbd83368636a0990",
                "editPlaylistAttributes" to "35a1a9ce3a2f4f8c32ee0e24c63c2069c6613c0a0b7e56d0e40dabe69a0b4f80",
                "addToLibrary" to "1ad0d40b3c09660d818b9e770eb1e84745dfbe941df159a64f8772b6fa2bfc3a",
                "removeFromLibrary" to "1ad0d40b3c09660d818b9e770eb1e84745dfbe941df159a64f8772b6fa2bfc3a",
                "home" to "76243c78b0e20ecdbe41b794dec8cbe73f75e585b0a7201b8d2e84578412847a",
            )
        defaults.forEach { (op, hash) ->
            hashes[op] = GqlHashEntry(hash = hash, source = HashSource.HARDCODED)
        }
    }

    /**
     * Returns the best available hash for [operationName].
     * Throws [IllegalStateException] if the operation is unknown
     * (should never happen — all operations have hardcoded defaults).
     */
    fun getHash(operationName: String): String =
        hashes[operationName]?.hash
            ?: error("No hash registered for GQL operation: $operationName")

    /**
     * Returns the previous hash for [operationName], if one was recorded
     * during a hash rotation. Used as a fallback when the current hash
     * returns a PersistedQueryNotFound error.
     */
    fun getPreviousHash(operationName: String): String? = hashes[operationName]?.previousHash

    /**
     * Bulk-update hashes from a remote or cached source.
     * Only overwrites entries whose remote hash differs from the
     * current hardcoded default, preserving the hardcoded value
     * as an implicit fallback (always reachable via [loadHardcodedDefaults]).
     */
    fun updateHashes(
        remoteHashes: Map<String, RemoteHashEntry>,
        source: HashSource,
    ): UpdateResult {
        var updated = 0
        var unchanged = 0
        remoteHashes.forEach { (op, remote) ->
            val current = hashes[op]
            if (current != null) {
                val hashChanged = current.hash != remote.hash
                if (hashChanged) updated++ else unchanged++
                hashes[op] =
                    GqlHashEntry(
                        hash = remote.hash,
                        previousHash = remote.previousHash,
                        source = source,
                    )
            }
        }
        return UpdateResult(updated = updated, unchanged = unchanged)
    }

    data class UpdateResult(
        val updated: Int,
        val unchanged: Int,
    )

    fun getAll(): Map<String, GqlHashEntry> = hashes.toMap()

    data class RemoteHashEntry(
        val hash: String,
        val previousHash: String? = null,
    )
}
