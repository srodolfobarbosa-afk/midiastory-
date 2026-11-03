package com.rdf112.sweep.domain.permissions

import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind

/** Pure permission contract mapping Android grants to media access; no Android framework dependency. */
object MediaPermissionPolicy {
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
    const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
    const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

    fun permissionsToRequest(apiLevel: Int, kinds: Set<MediaKind>): Set<String> {
        requireSupportedApi(apiLevel)
        if (kinds.isEmpty()) return emptySet()

        return when {
            apiLevel >= 34 -> buildSet {
                if (MediaKind.IMAGE in kinds) {
                    add(READ_MEDIA_IMAGES)
                    add(READ_MEDIA_VISUAL_USER_SELECTED)
                }
                if (MediaKind.VIDEO in kinds) {
                    add(READ_MEDIA_VIDEO)
                    add(READ_MEDIA_VISUAL_USER_SELECTED)
                }
                if (MediaKind.AUDIO in kinds) add(READ_MEDIA_AUDIO)
            }
            apiLevel >= 33 -> buildSet {
                if (MediaKind.IMAGE in kinds) add(READ_MEDIA_IMAGES)
                if (MediaKind.VIDEO in kinds) add(READ_MEDIA_VIDEO)
                if (MediaKind.AUDIO in kinds) add(READ_MEDIA_AUDIO)
            }
            else -> setOf(READ_EXTERNAL_STORAGE)
        }
    }

    fun accessLevel(apiLevel: Int, kind: MediaKind, grantedPermissions: Set<String>): AccessLevel {
        requireSupportedApi(apiLevel)
        return when {
            apiLevel >= 34 -> {
                val granular = granularPermission(kind)
                if (granular in grantedPermissions) AccessLevel.FULL
                else if (kind != MediaKind.AUDIO && READ_MEDIA_VISUAL_USER_SELECTED in grantedPermissions) AccessLevel.PARTIAL
                else AccessLevel.DENIED
            }
            apiLevel >= 33 -> if (granularPermission(kind) in grantedPermissions) AccessLevel.FULL else AccessLevel.DENIED
            else -> if (READ_EXTERNAL_STORAGE in grantedPermissions) AccessLevel.FULL else AccessLevel.DENIED
        }
    }

    private fun granularPermission(kind: MediaKind): String = when (kind) {
        MediaKind.IMAGE -> READ_MEDIA_IMAGES
        MediaKind.VIDEO -> READ_MEDIA_VIDEO
        MediaKind.AUDIO -> READ_MEDIA_AUDIO
    }

    private fun requireSupportedApi(apiLevel: Int) {
        require(apiLevel >= 29) { "Android API levels below 29 are unsupported (minSdk is 29)" }
    }
}
