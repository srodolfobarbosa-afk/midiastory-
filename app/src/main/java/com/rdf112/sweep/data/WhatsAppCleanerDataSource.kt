package com.rdf112.sweep.data

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** A WhatsApp media file found through MediaStore or a user-granted SAF tree. */
data class WhatsAppMediaItem(
    val uri: Uri,
    val sizeBytes: Long,
    val category: WhatsAppMediaCategory,
    val isSafUri: Boolean,
)

enum class WhatsAppMediaCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE_NOTE,
    GIF,
    DOCUMENT,
    OTHER,
}

/** Reads WhatsApp media exposed by MediaStore and by a user-selected Storage Access Framework tree. */
class WhatsAppCleanerDataSource(
    context: Context,
    private val resolver: ContentResolver = context.applicationContext.contentResolver,
) {
    private val appContext = context.applicationContext

    suspend fun scanViaMediaStore(context: Context = appContext): List<WhatsAppMediaItem> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
            )
            val pathColumn = MediaStore.Files.FileColumns.RELATIVE_PATH
            val selection = "$pathColumn LIKE ? OR $pathColumn LIKE ?"
            val args = arrayOf(
                "%Android/media/com.whatsapp/WhatsApp/Media/%",
                "%Android/media/com.whatsapp.w4b/WhatsApp Business/Media/%",
            )
            val sortOrder = "${MediaStore.Files.FileColumns.SIZE} DESC"
            val items = mutableListOf<WhatsAppMediaItem>()
            val cursor = resolver.query(collection, projection, selection, args, sortOrder)
                ?: throw IllegalStateException("MediaStore did not return a cursor")

            cursor.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val pathIndex = it.getColumnIndexOrThrow(pathColumn)
                val typeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val path = it.getString(pathIndex).orEmpty()
                    val category = categoryForPath(path)
                    val mediaType = it.getInt(typeColumn)
                    // Files with no indexed media type (such as documents) belong to the SAF scan.
                    if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_NONE) continue
                    items += WhatsAppMediaItem(
                        uri = Uri.withAppendedPath(collection, it.getLong(idColumn).toString()),
                        sizeBytes = it.getLong(sizeColumn).coerceAtLeast(0L),
                        category = category,
                        isSafUri = false,
                    )
                }
            }
            items
        }

    suspend fun scanViaSaf(treeUri: Uri): List<WhatsAppMediaItem> = withContext(Dispatchers.IO) {
        require(DocumentsContract.isTreeUri(treeUri)) { "A SAF tree URI is required" }
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val pending = ArrayDeque<PendingDirectory>()
        pending.add(PendingDirectory(rootDocumentId, WhatsAppMediaCategory.OTHER, 0))
        val visited = HashSet<String>()
        val items = ArrayList<WhatsAppMediaItem>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val directory = pending.removeLast()
            if (directory.depth >= MAX_DIRECTORY_DEPTH || !visited.add(directory.documentId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directory.documentId)
            val cursor = resolver.query(childrenUri, projection, null, null, null)
                ?: throw IllegalStateException("SAF provider did not return a cursor")
            cursor.use {
                val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val documentId = it.getString(idColumn) ?: continue
                    val name = it.getString(nameColumn).orEmpty()
                    val mime = it.getString(mimeColumn)
                    val category = categoryForName(name, directory.category, mime)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.add(PendingDirectory(documentId, category, directory.depth + 1))
                    } else {
                        if (items.size >= MAX_SAF_ITEMS) {
                            throw IllegalStateException("SAF scan exceeded its $MAX_SAF_ITEMS item safety limit")
                        }
                        items += WhatsAppMediaItem(
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                            sizeBytes = if (it.isNull(sizeColumn)) 0L else it.getLong(sizeColumn).coerceAtLeast(0L),
                            category = category,
                            isSafUri = true,
                        )
                    }
                }
            }
        }
        items
    }

    private data class PendingDirectory(
        val documentId: String,
        val category: WhatsAppMediaCategory,
        val depth: Int,
    )

    private fun categoryForPath(path: String): WhatsAppMediaCategory =
        categoryForName(path.substringBeforeLast('/').substringAfterLast('/'), WhatsAppMediaCategory.OTHER)

    private fun categoryForName(
        name: String,
        inherited: WhatsAppMediaCategory,
        mimeType: String? = null,
    ): WhatsAppMediaCategory {
        val normalized = name.lowercase()
        return when {
            "voice notes" in normalized || "voicenotes" in normalized -> WhatsAppMediaCategory.VOICE_NOTE
            "animated gifs" in normalized || normalized.endsWith(".gif") -> WhatsAppMediaCategory.GIF
            "images" in normalized || mimeType?.startsWith("image/") == true -> WhatsAppMediaCategory.IMAGE
            "video" in normalized || mimeType?.startsWith("video/") == true -> WhatsAppMediaCategory.VIDEO
            "audio" in normalized || mimeType?.startsWith("audio/") == true -> WhatsAppMediaCategory.AUDIO
            "document" in normalized || mimeType == "application/pdf" -> WhatsAppMediaCategory.DOCUMENT
            inherited != WhatsAppMediaCategory.OTHER -> inherited
            else -> WhatsAppMediaCategory.OTHER
        }
    }

    private companion object {
        const val MAX_SAF_ITEMS = 100_000
        const val MAX_DIRECTORY_DEPTH = 32
    }
}

class WhatsAppDeletionManager(
    private val contentResolver: ContentResolver,
) {
    /** Returns the Android system consent request; callers must present it to the user. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createMediaStoreDeleteNotice(uris: List<Uri>): IntentSender {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "MediaStore delete consent requests require Android 11 or later"
        }
        val safeUris = uris.asSequence()
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            .distinct()
            .toList()
        require(safeUris.isNotEmpty()) { "At least one MediaStore content URI is required" }
        return MediaStore.createDeleteRequest(contentResolver, safeUris).intentSender
    }
}
