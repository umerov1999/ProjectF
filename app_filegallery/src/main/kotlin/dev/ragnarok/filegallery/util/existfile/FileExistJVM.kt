package dev.ragnarok.filegallery.util.existfile

import android.content.Context
import dev.ragnarok.filegallery.Includes.stores
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.emptyTaskFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import java.io.File
import java.util.LinkedList
import java.util.Locale

class FileExistJVM : AbsFileExist {
    private val CachedAudios: MutableList<String> = LinkedList()
    private val CachedTags: MutableList<String> = LinkedList()
    private val isBusyLock = Any()
    private var isBusy = false
    private fun setBusy(nBusy: Boolean): Boolean {
        synchronized(isBusyLock) {
            if (isBusy && nBusy) {
                return false
            }
            isBusy = nBusy
        }
        return true
    }

    override fun addAudio(file: String) {
        if (!setBusy(true)) {
            return
        }
        CachedAudios.add(file.lowercase(Locale.getDefault()))
        setBusy(false)
    }

    override fun findAllAudios(context: Context): Flow<Boolean> {
        return if (!hasReadWriteStoragePermission(context)) emptyTaskFlow() else flow {
            if (!setBusy(true)) {
                emit(false)
                return@flow
            }
            val temp = File(get().main().musicDir)
            if (!temp.exists()) {
                setBusy(false)
                emit(false)
                return@flow
            }
            val file_list = temp.listFiles()
            if (file_list == null || file_list.isEmpty()) {
                setBusy(false)
                emit(false)
                return@flow
            }
            CachedAudios.clear()
            for (u in file_list) {
                if (u.isFile) CachedAudios.add(u.name.lowercase(Locale.getDefault()))
            }
            setBusy(false)
            emit(true)
        }
    }

    override fun isExistAllAudio(file: String): Boolean {
        synchronized(isBusyLock) {
            if (isBusy) {
                return false
            }
            val res = file.lowercase(Locale.getDefault())
            if (CachedAudios.nonNullNoEmpty()) {
                for (i in CachedAudios) {
                    if (i == res) {
                        return true
                    }
                }
            }
            return false
        }
    }

    override fun addTag(path: String) {
        if (!setBusy(true)) {
            return
        }
        CachedTags.add(path)
        setBusy(false)
    }

    override fun deleteTag(path: String) {
        if (!setBusy(true)) {
            return
        }
        CachedTags.remove(path)
        setBusy(false)
    }

    override fun findAllTags(): Flow<Boolean> {
        return flow {
            if (!setBusy(true)) {
                emit(false)
            } else {
                val list = stores.searchQueriesStore().getAllTagDirs().single()
                CachedTags.clear()
                for (u in list) {
                    u.path?.let { it1 -> CachedTags.add(it1) }
                }
                setBusy(false)
                emit(true)
            }
        }
    }

    override fun isExistTag(path: String): Boolean {
        synchronized(isBusyLock) {
            if (isBusy) {
                return false
            }
            if (CachedTags.nonNullNoEmpty()) {
                for (i in CachedTags) {
                    if (i == path) {
                        return true
                    }
                }
            }
            return false
        }
    }
}