package dev.ragnarok.filegallery.util.existfile

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface AbsFileExist {
    fun addAudio(file: String)
    fun findAllAudios(context: Context): Flow<Boolean>
    fun isExistAllAudio(file: String): Boolean
    fun addTag(path: String)
    fun deleteTag(path: String)
    fun findAllTags(): Flow<Boolean>
    fun isExistTag(path: String): Boolean
}