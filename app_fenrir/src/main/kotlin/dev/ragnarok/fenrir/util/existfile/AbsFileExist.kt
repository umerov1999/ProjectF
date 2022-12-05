package dev.ragnarok.fenrir.util.existfile

import android.content.Context
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import io.reactivex.rxjava3.core.Completable
import java.io.IOException

interface AbsFileExist {
    @Throws(IOException::class)
    fun findRemoteAudios(context: Context)
    fun findLocalImages(photos: List<SelectablePhotoWrapper>): Completable
    fun addAudio(file: String)
    fun addPhoto(file: String)
    fun findAllAudios(context: Context): Completable
    fun markExistPhotos(photos: List<SelectablePhotoWrapper>)
    fun isExistRemoteAudio(file: String): Boolean
    fun isExistAllAudio(file: String): Int
}