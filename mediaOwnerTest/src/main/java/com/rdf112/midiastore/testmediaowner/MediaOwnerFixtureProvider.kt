package com.rdf112.midiastore.testmediaowner

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore

/**
 * Test-only provider that creates MediaStore rows under this package's UID.
 * The production app can invoke it only with the signature-level fixture permission.
 */
class MediaOwnerFixtureProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val appContext = requireNotNull(context)
        val resolver = appContext.contentResolver
        return when (method) {
            METHOD_CREATE_IMAGE -> {
                val name = arg?.takeIf(String::isNotBlank)
                    ?: "midiastore-api29-foreign-owner-${System.nanoTime()}.jpg"
                val collection = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/MidiastoreTest/",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = requireNotNull(resolver.insert(collection, values)) {
                    "Could not insert MediaStore fixture from owner app"
                }
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.MAGENTA)
                    resolver.openOutputStream(uri, "w").use { output ->
                        requireNotNull(output)
                        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
                    }
                    val publish = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    check(resolver.update(uri, publish, null, null) >= 1) {
                        "Could not publish MediaStore fixture"
                    }
                    Bundle().apply {
                        putString(EXTRA_URI, uri.toString())
                        putString(EXTRA_OWNER_PACKAGE, appContext.packageName)
                    }
                } catch (failure: Throwable) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw failure
                } finally {
                    bitmap.recycle()
                }
            }

            METHOD_DELETE_FIXTURE -> {
                val uri = Uri.parse(requireNotNull(arg) { "Fixture URI is required" })
                Bundle().apply {
                    putInt(EXTRA_DELETED_ROWS, resolver.delete(uri, null, null))
                }
            }

            else -> throw IllegalArgumentException("Unsupported fixture operation: $method")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Use call(create_image)")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Use call(delete_fixture)")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Use the fixture operations")

    companion object {
        const val METHOD_CREATE_IMAGE = "create_image"
        const val METHOD_DELETE_FIXTURE = "delete_fixture"
        const val EXTRA_URI = "fixture_uri"
        const val EXTRA_OWNER_PACKAGE = "owner_package"
        const val EXTRA_DELETED_ROWS = "deleted_rows"
    }
}
