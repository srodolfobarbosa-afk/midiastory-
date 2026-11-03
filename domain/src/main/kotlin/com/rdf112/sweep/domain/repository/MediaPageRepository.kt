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
}
