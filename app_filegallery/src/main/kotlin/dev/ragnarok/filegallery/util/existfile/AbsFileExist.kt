package dev.ragnarok.filegallery.util.existfile

import android.content.Context
import io.reactivex.rxjava3.core.Completable

interface AbsFileExist {
    fun addAudio(file: String)
    fun findAllAudios(context: Context): Completable
    fun isExistAllAudio(file: String): Boolean
    fun addTag(path: String)
    fun deleteTag(path: String)
    fun findAllTags(): Completable
    fun isExistTag(path: String): Boolean
}