package com.rdf112.sweep.domain.permissions

import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPermissionPolicyTest {
    private val images = MediaPermissionPolicy.READ_MEDIA_IMAGES
    private val videos = MediaPermissionPolicy.READ_MEDIA_VIDEO
    private val audio = MediaPermissionPolicy.READ_MEDIA_AUDIO
    private val selected = MediaPermissionPolicy.READ_MEDIA_VISUAL_USER_SELECTED
    private val legacy = MediaPermissionPolicy.READ_EXTERNAL_STORAGE

    @Test
    fun requestsAreGranularOnApi33AndIncludeSelectedAccessOnApi34() {
        assertEquals(setOf(images, videos, audio), MediaPermissionPolicy.permissionsToRequest(33, MediaKind.entries.toSet()))
        assertEquals(setOf(images, videos, audio, selected), MediaPermissionPolicy.permissionsToRequest(34, MediaKind.entries.toSet()))
        assertEquals(setOf(selected, images), MediaPermissionPolicy.permissionsToRequest(34, setOf(MediaKind.IMAGE)))
        assertEquals(setOf(audio), MediaPermissionPolicy.permissionsToRequest(34, setOf(MediaKind.AUDIO)))
    }

    @Test
    fun legacyApisRequestSharedStoragePermission() {
        assertEquals(setOf(legacy), MediaPermissionPolicy.permissionsToRequest(29, setOf(MediaKind.IMAGE)))
        assertEquals(setOf(legacy), MediaPermissionPolicy.permissionsToRequest(32, setOf(MediaKind.IMAGE, MediaKind.AUDIO)))
    }

    @Test
    fun accessUsesGranularGrantAndSelectedGrantIsPartialOnlyForVisualKinds() {
        assertEquals(AccessLevel.FULL, MediaPermissionPolicy.accessLevel(34, MediaKind.IMAGE, setOf(images, selected)))
        assertEquals(AccessLevel.FULL, MediaPermissionPolicy.accessLevel(34, MediaKind.AUDIO, setOf(audio)))
        assertEquals(AccessLevel.PARTIAL, MediaPermissionPolicy.accessLevel(34, MediaKind.VIDEO, setOf(selected)))
        assertEquals(AccessLevel.DENIED, MediaPermissionPolicy.accessLevel(34, MediaKind.AUDIO, setOf(selected)))
        assertEquals(AccessLevel.DENIED, MediaPermissionPolicy.accessLevel(33, MediaKind.IMAGE, setOf(selected)))
    }

    @Test
    fun api36KeepsPartialVisualAccessAndEmptyRequestsAreNoOps() {
        assertEquals(setOf(images, videos, selected), MediaPermissionPolicy.permissionsToRequest(36, setOf(MediaKind.IMAGE, MediaKind.VIDEO)))
        assertEquals(emptySet<String>(), MediaPermissionPolicy.permissionsToRequest(36, emptySet()))
        assertEquals(AccessLevel.PARTIAL, MediaPermissionPolicy.accessLevel(36, MediaKind.IMAGE, setOf(selected)))
        assertEquals(AccessLevel.DENIED, MediaPermissionPolicy.accessLevel(36, MediaKind.AUDIO, setOf(selected)))
    }

    @Test
    fun legacyAndApi33GrantsMapToFullOrDenied() {
        assertEquals(AccessLevel.FULL, MediaPermissionPolicy.accessLevel(32, MediaKind.AUDIO, setOf(legacy)))
        assertEquals(AccessLevel.DENIED, MediaPermissionPolicy.accessLevel(32, MediaKind.AUDIO, emptySet()))
        assertEquals(AccessLevel.FULL, MediaPermissionPolicy.accessLevel(33, MediaKind.VIDEO, setOf(videos)))
        assertEquals(AccessLevel.DENIED, MediaPermissionPolicy.accessLevel(33, MediaKind.VIDEO, setOf(images)))
    }

    @Test
    fun rejectsApisBelowMinimumSupportedLevel() {
        try {
            MediaPermissionPolicy.permissionsToRequest(28, setOf(MediaKind.IMAGE))
            throw AssertionError("Expected unsupported API level to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected; minSdk is 29.
        }
        try {
            MediaPermissionPolicy.accessLevel(28, MediaKind.IMAGE, emptySet())
            throw AssertionError("Expected unsupported API level to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected; minSdk is 29.
        }
    }
}
