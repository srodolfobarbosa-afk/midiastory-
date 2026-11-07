package com.rdf112.midiastore.testmediaowner

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import java.util.UUID

/** Creates and removes real MediaStore content from this app's independent UID for API 29 tests. */
class MediaOwnerFixtureProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle = when (method) {
        METHOD_CREATE_IMAGE -> createImage()
        METHOD_DELETE_MEDIA -> deleteMedia(requireNotNull(arg) { "Media URI is required" })
        else -> throw IllegalArgumentException("Unsupported fixture operation: $method")
    }

    private fun createImage(): Bundle {
        val resolver = requireNotNull(context).contentResolver
        val volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "midiastore-api29-owner-${UUID.randomUUID()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MidiastoreCodexTest/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.getContentUri(volumeName), values)) {
            "MediaStore did not create the helper-owned image"
        }
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            resolver.openOutputStream(uri, "w").use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, requireNotNull(output)))
            }
            val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            check(resolver.update(uri, published, null, null) == 1) {
                "MediaStore did not publish the helper-owned image"
            }
            return Bundle().apply {
                putString(KEY_URI, uri.toString())
                putString(KEY_OWNER_PACKAGE, requireNotNull(context).packageName)
                putInt(KEY_OWNER_UID, Process.myUid())
            }
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        } finally {
            bitmap.recycle()
        }
    }

    private fun deleteMedia(rawUri: String): Bundle {
        val deletedRows = requireNotNull(context).contentResolver.delete(Uri.parse(rawUri), null, null)
        return Bundle().apply { putInt(KEY_DELETED_ROWS, deletedRows) }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException()

    companion object {
        const val AUTHORITY = "com.rdf112.midiastore.testmediaowner.fixtures"
        const val METHOD_CREATE_IMAGE = "create_image"
        const val METHOD_DELETE_MEDIA = "delete_media"
        const val KEY_URI = "media_uri"
        const val KEY_OWNER_PACKAGE = "owner_package"
        const val KEY_OWNER_UID = "owner_uid"
        const val KEY_DELETED_ROWS = "deleted_rows"
        const val PERMISSION = "com.rdf112.midiastore.testmediaowner.permission.MANAGE_FIXTURES"
    }
}
