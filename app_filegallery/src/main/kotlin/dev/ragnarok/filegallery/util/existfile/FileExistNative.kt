package dev.ragnarok.filegallery.util.existfile

import android.content.Context
import dev.ragnarok.fenrir.module.StringExist
import dev.ragnarok.filegallery.Includes.stores
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.emptyTaskFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import java.io.File
import java.util.Locale

class FileExistNative : AbsFileExist {
    private val CachedAudios = StringExist(true)
    private val CachedTags = StringExist(true)
    override fun addAudio(file: String) {
        CachedAudios.insert(file.lowercase(Locale.getDefault()))
    }

    override fun findAllAudios(context: Context): Flow<Boolean> {
        return if (!hasReadWriteStoragePermission(context)) emptyTaskFlow() else flow {
            val temp = File(get().main().musicDir)
            if (!temp.exists()) {
                emit(false)
                return@flow
            }
            val file_list = temp.listFiles()
            if (file_list == null || file_list.isEmpty()) {
                emit(false)
                return@flow
            }
            CachedAudios.clear()
            for (u in file_list) {
                if (u.isFile) CachedAudios.insert(u.name.lowercase(Locale.getDefault()))
            }
            emit(true)
        }
    }

    override fun isExistAllAudio(file: String): Boolean {
        val res = file.lowercase(Locale.getDefault())
        return CachedAudios.has(res)
    }

    override fun addTag(path: String) {
        CachedTags.insert(path)
    }

    override fun deleteTag(path: String) {
        CachedTags.delete(path)
    }

    override fun findAllTags(): Flow<Boolean> {
        return flow {
            val list = stores.searchQueriesStore().getAllTagDirs().single()
            CachedAudios.clear()
            CachedTags.clear()
            for (u in list) {
                u.path?.let { it1 -> CachedTags.insert(it1) }
            }
        }
    }

    override fun isExistTag(path: String): Boolean {
        return CachedTags.has(path)
    }
}