package com.rdf112.sweep.domain.repository

import com.rdf112.sweep.domain.model.MediaPage
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.MediaScanSession
import com.rdf112.sweep.domain.model.SessionFreshness

/** Platform-independent contract for one bounded page at a time. */
interface MediaPageRepository {
    suspend fun createSession(): MediaScanSession
    suspend fun loadPage(session: MediaScanSession, request: MediaPageRequest): MediaPage
    suspend fun assessSession(session: MediaScanSession): SessionFreshness

    /**
     * Computes a content hash from a content:// URI using bounded streaming I/O.
     * The default returns null for repositories that cannot read binary content.
     */
    suspend fun computeSha256(uri: String): String? = null

    /**
     * Computes a streaming SHA-256 while refusing to read more than maxBytes.
     * The default delegates to the legacy bounded implementation for test/fake repositories.
     */
    suspend fun computeSha256(uri: String, maxBytes: Long): String? {
        require(maxBytes > 0L) { "maxBytes must be positive" }
        return computeSha256(uri)
    }
}
