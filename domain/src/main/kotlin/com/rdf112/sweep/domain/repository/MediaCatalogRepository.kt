package com.rdf112.sweep.domain.repository

import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.ScanReport

/** Port between use/UI logic and a platform-specific media catalogue implementation. */
interface MediaCatalogRepository {
    suspend fun scan(
        kinds: Set<MediaKind>,
        accessByKind: Map<MediaKind, AccessLevel>,
    ): ScanReport
}
