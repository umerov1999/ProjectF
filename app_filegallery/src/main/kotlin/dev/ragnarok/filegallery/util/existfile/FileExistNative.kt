package dev.ragnarok.filegallery.util.existfile

import android.content.Context
import dev.ragnarok.fenrir.module.StringExist
import dev.ragnarok.filegallery.Includes.stores
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.AppPerms.hasReadWriteStoragePermission
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import java.io.File
import java.util.Locale

class FileExistNative : AbsFileExist {
    private val CachedAudios = StringExist(true)
    private val CachedTags = StringExist(true)
    override fun addAudio(file: String) {
        CachedAudios.insert(file.lowercase(Locale.getDefault()))
    }

    override fun findAllAudios(context: Context): Completable {
        return if (!hasReadWriteStoragePermission(context)) Completable.complete() else Completable.create { t: CompletableEmitter ->
            val temp = File(get().main().musicDir)
            if (!temp.exists()) {
                t.onComplete()
                return@create
            }
            val file_list = temp.listFiles()
            if (file_list == null || file_list.isEmpty()) {
                t.onComplete()
                return@create
            }
            CachedAudios.clear()
            for (u in file_list) {
                if (u.isFile) CachedAudios.insert(u.name.lowercase(Locale.getDefault()))
            }
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

    override fun findAllTags(): Completable {
        return Completable.create {
            val list = stores.searchQueriesStore().getAllTagDirs().blockingGet()
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