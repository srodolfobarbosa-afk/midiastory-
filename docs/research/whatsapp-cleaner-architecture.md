# Especificação Técnica: Limpador de WhatsApp Pro (Midiastore)
**Autor do Design:** Google Gemini 4 Argon (via OpenRouter Multi-Agent Router)  
**Alvo:** Android 11+ (API 30–34)  
**Objetivo:** Permitir que o Midiastore analise e limpe com segurança arquivos pesados de mídia do WhatsApp sem violar as políticas da Google Play Store (Scoped Storage).

---

## 1. Conformidade Google Play & Estratégia de Acesso (API 30–34)

A permissão `MANAGE_EXTERNAL_STORAGE` é **estritamente proibida** pela Google Play para utilitários secundários de limpeza sob risco de suspensão da conta. Portanto, o módulo utiliza uma **estratégia híbrida**:

1. **Mídia Indexada (Imagens, Vídeos, Áudios, GIFs):** Utiliza a API **MediaStore** com permissões granulares do Android 13+ (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`).
2. **Mídia Não-Indexada (Documentos, Voice Notes `.opus` em pastas `.nomedia`, Statuses, pasta `Sent`):** Utiliza o **Storage Access Framework (SAF)** via `ACTION_OPEN_DOCUMENT_TREE`, solicitando acesso direto à raiz `Android/media/com.whatsapp/WhatsApp/Media` na primeira execução.

---

## 2. Mapeamento de Diretórios do WhatsApp

Caminho Base: `Android/media/com.whatsapp/WhatsApp/Media/` (também compatível com `com.whatsapp.w4b` para WhatsApp Business).

| Categoria | Subpasta Alvo | Possui `.nomedia`? | Método de Leitura | Motivo Técnico |
| :--- | :--- | :---: | :--- | :--- |
| **Vídeos** | `WhatsApp Video` | Não (exceto `Sent`) | MediaStore + SAF | MediaStore lê a raiz pública; SAF lê a pasta `Sent`. |
| **Imagens** | `WhatsApp Images` | Não (exceto `Sent`) | MediaStore + SAF | Idem acima. |
| **Áudios** | `WhatsApp Audio` | Não (exceto `Sent`) | MediaStore + SAF | Idem acima. |
| **Notas de Voz** | `WhatsApp Voice Notes` | **Sim** (`.nomedia`) | **SAF Exclusivo** | Pastas por data (`YYYYWW`) contêm `.nomedia`. MediaStore ignora. |
| **GIFs** | `WhatsApp Animated Gifs` | Não | MediaStore + SAF | Vídeos `.mp4` curtos. |
| **Documentos** | `WhatsApp Documents` | Não | **SAF Exclusivo** | No Android 11+, `MediaStore.Files` não visualiza docs de terceiros. |

---

## 3. Scanner de Alta Performance (Zero ANR)

> ⚠️ **Aviso de Performance:** Nunca usar `DocumentFile.fromTreeUri()` para varrer árvores inteiras de arquivos, pois isso dispara centenas de chamadas IPC bloqueantes no Android. Usar queries diretas com `DocumentsContract` no `ContentResolver`.

### A. Leitura via MediaStore (Mídia Pública)
```kotlin
suspend fun scanViaMediaStore(context: Context): List<WhatsAppMediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<WhatsAppMediaItem>()
    val collection = MediaStore.Files.getContentUri("external")
    
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.RELATIVE_PATH,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%Android/media/com.whatsapp/WhatsApp/Media/%")

    context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val size = cursor.getLong(sizeCol)
            val path = cursor.getString(pathCol)
            val contentUri = ContentUris.withAppendedId(collection, id)
            items.add(WhatsAppMediaItem(contentUri, size, mapCategory(path), isSafUri = false))
        }
    }
    return@withContext items
}
```

### B. Leitura via SAF (Documentos, Voice Notes e Pastas Ocultas)
```kotlin
suspend fun scanViaSaf(context: Context, treeUri: Uri): List<WhatsAppMediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<WhatsAppMediaItem>()
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri)
    )

    fun traverseDirectory(dirUri: Uri) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        context.contentResolver.query(dirUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idCol)
                val mimeType = cursor.getString(mimeCol)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val nextDirUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    traverseDirectory(nextDirUri)
                } else {
                    val size = cursor.getLong(sizeCol)
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    items.add(WhatsAppMediaItem(fileUri, size, mapCategoryFromDocId(docId), isSafUri = true))
                }
            }
        }
    }

    traverseDirectory(childrenUri)
    return@withContext items
}
```

---

## 4. Exclusão Segura e Diálogo Oficial do Android (API 30+)

Para deletar arquivos de terceiros (criados pelo WhatsApp):
1. **Arquivos do SAF:** Deletados silenciosamente em background pois o usuário já concedeu a permissão persistente de árvore (`takePersistableUriPermission`).
2. **Arquivos do MediaStore:** O sistema exige consentimento explícito. O `MediaStore.createDeleteRequest` agrupa todos os arquivos em um único `IntentSenderRequest`, mostrando **apenas uma tela do Android** para confirmar a exclusão de dezenas ou centenas de mídias de uma vez só!

```kotlin
class WhatsAppDeletionManager(private val contentResolver: ContentResolver) {

    suspend fun deleteSafFiles(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        uris.forEach { uri ->
            try {
                if (DocumentsContract.deleteDocument(contentResolver, uri)) count++
            } catch (e: Exception) {
                // Log de erro protegido
            }
        }
        return@withContext count
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun createMediaStoreDeleteNotice(uris: List<Uri>): IntentSender {
        val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
        return pendingIntent.intentSender
    }
}
```

---

## 5. Garantias de Segurança
* **100% Compatível com Google Play:** Zero permissões restritas como `MANAGE_EXTERNAL_STORAGE`.
* **Proteção do Usuário:** O Android exibe o diálogo nativo do sistema para remoção do MediaStore.
* **Privacidade Total:** Nenhuma mídia ou metadado sai do aparelho.
