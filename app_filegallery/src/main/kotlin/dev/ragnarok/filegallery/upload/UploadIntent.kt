package dev.ragnarok.filegallery.upload

import android.net.Uri

class UploadIntent(
    val destination: UploadDestination
) {

    /* Локальный путь к файлу */
    var pFileUri: Uri? = null

    /* Размер изображения (только для изображений)*/
    var size = 0

    /**
     * Дополнительные данные
     */
    var fileId: Long? = null
    var pAutoCommit = false

    fun setAutoCommit(autoCommit: Boolean): UploadIntent {
        pAutoCommit = autoCommit
        return this
    }

    fun setFileUri(fileUri: Uri?): UploadIntent {
        this.pFileUri = fileUri
        return this
    }

    fun setSize(size: Int): UploadIntent {
        this.size = size
        return this
    }

    fun setFileId(fileId: Long?): UploadIntent {
        this.fileId = fileId
        return this
    }
}