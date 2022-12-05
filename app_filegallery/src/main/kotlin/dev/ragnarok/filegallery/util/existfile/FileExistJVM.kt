package dev.ragnarok.filegallery.util.existfile

import android.content.Context
import dev.ragnarok.filegallery.Includes.stores
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.AppPerms.hasReadWriteStoragePermission
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import java.io.File
import java.util.*

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

    override fun findAllAudios(context: Context): Completable {
        return if (!hasReadWriteStoragePermission(context)) Completable.complete() else Completable.create { t: CompletableEmitter ->
            if (!setBusy(true)) {
                return@create
            }
            val temp = File(get().main().getMusicDir())
            if (!temp.exists()) {
                setBusy(false)
                t.onComplete()
                return@create
            }
            val file_list = temp.listFiles()
            if (file_list == null || file_list.isEmpty()) {
                setBusy(false)
                t.onComplete()
                return@create
            }
            CachedAudios.clear()
            for (u in file_list) {
                if (u.isFile) CachedAudios.add(u.name.lowercase(Locale.getDefault()))
            }
            setBusy(false)
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

    override fun findAllTags(): Completable {
        return Completable.create {
            if (!setBusy(true)) {
                return@create
            }
            val list = stores.searchQueriesStore().getAllTagDirs().blockingGet()
            CachedTags.clear()
            for (u in list) {
                u.path?.let { it1 -> CachedTags.add(it1) }
            }
            setBusy(false)
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