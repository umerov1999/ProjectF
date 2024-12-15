package dev.ragnarok.fenrir.util.existfile

import android.content.Context
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermissionSimple
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.internal.OkioSerialReader
import kotlinx.serialization.json.internal.WriteMode
import kotlinx.serialization.json.internal.lexer.ReaderJsonLexer
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.Locale
import kotlin.math.abs

class FileExistJVM : AbsFileExist {
    private val CachedAudios: MutableList<String> = LinkedList()
    private val RemoteAudios: MutableList<String> = LinkedList()
    private val CachedPhotos: MutableList<String> = LinkedList()
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

    @Throws(IOException::class)
    private fun findRemoteAudios(context: Context, needLock: Boolean) {
        if (needLock) {
            if (!hasReadStoragePermissionSimple(context)) return
            if (!setBusy(true)) {
                return
            }
        }
        RemoteAudios.clear()
        val audios = File(Settings.get().main().musicDir, "local_server_audio_list.json")
        if (!audios.exists()) {
            if (needLock) {
                setBusy(false)
            }
            return
        }
        try {
            val reader = ReaderJsonLexer(
                OkioSerialReader(audios.source().buffer())
            )
            reader.consumeNextToken(WriteMode.LIST.begin)
            while (reader.canConsumeValue()) {
                RemoteAudios.add(reader.consumeStringLenient().lowercase(Locale.getDefault()))
                reader.tryConsumeComma()
            }
            reader.consumeNextToken(WriteMode.LIST.end)
            if (needLock) {
                setBusy(false)
            }
        } catch (e: Exception) {
            if (needLock) {
                setBusy(false)
            }
            throw e
        }
    }

    @Throws(IOException::class)
    override fun findRemoteAudios(context: Context) {
        findRemoteAudios(context, true)
    }

    private fun transform_owner(owner_id: Long): String {
        return if (owner_id < 0) "club" + abs(owner_id) else "id$owner_id"
    }

    private fun existPhoto(photo: Photo): Boolean {
        for (i in CachedPhotos) {
            if (i.contains(transform_owner(photo.ownerId) + "_" + photo.getObjectId())) {
                return true
            }
        }
        return false
    }

    private fun loadDownloadPath(Path: String) {
        val temp = File(Path)
        if (!temp.exists()) return
        val file_list = temp.listFiles()
        if (file_list == null || file_list.isEmpty()) return
        for (u in file_list) {
            if (u.isFile) CachedPhotos.add(u.name) else if (u.isDirectory) {
                loadDownloadPath(u.absolutePath)
            }
        }
    }

    override fun findLocalImages(photos: List<SelectablePhotoWrapper>): Flow<Boolean> {
        return flow {
            if (!setBusy(true)) {
                emit(false)
                return@flow
            }
            val temp = File(Settings.get().main().photoDir)
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
            CachedPhotos.clear()
            for (u in file_list) {
                if (u.isFile) CachedPhotos.add(u.name) else if (u.isDirectory) {
                    loadDownloadPath(u.absolutePath)
                }
            }
            for (i in photos) {
                i.setDownloaded(existPhoto(i.photo))
            }
            setBusy(false)
            emit(true)
        }
    }

    override fun addAudio(file: String) {
        if (!setBusy(true)) {
            return
        }
        CachedAudios.add(file.lowercase(Locale.getDefault()))
        setBusy(false)
    }

    override fun addPhoto(file: String) {
        if (!setBusy(true)) {
            return
        }
        CachedPhotos.add(file.lowercase(Locale.getDefault()))
        setBusy(false)
    }

    override fun findAllAudios(context: Context): Flow<Boolean> {
        return if (!hasReadStoragePermissionSimple(context)) emptyTaskFlow() else flow {
            if (!setBusy(true)) {
                emit(false)
                return@flow
            }
            findRemoteAudios(context, false)
            val temp = File(Settings.get().main().musicDir)
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

    override fun markExistPhotos(photos: List<SelectablePhotoWrapper>) {
        synchronized(isBusyLock) {
            if (isBusy) {
                return
            }
            if (photos.isEmpty()) {
                return
            }
            for (i in photos) {
                i.setDownloaded(existPhoto(i.photo))
            }
        }
    }

    override fun isExistRemoteAudio(file: String): Boolean {
        synchronized(isBusyLock) {
            if (isBusy) {
                return false
            }
            val res = file.lowercase(Locale.getDefault())
            if (RemoteAudios.nonNullNoEmpty()) {
                for (i in RemoteAudios) {
                    if (i == res) {
                        return true
                    }
                }
            }
            return false
        }
    }

    override fun isExistAllAudio(file: String): Int {
        synchronized(isBusyLock) {
            if (isBusy) {
                return 0
            }
            val res = file.lowercase(Locale.getDefault())
            if (CachedAudios.nonNullNoEmpty()) {
                for (i in CachedAudios) {
                    if (i == res) {
                        return 1
                    }
                }
            }
            if (RemoteAudios.nonNullNoEmpty()) {
                for (i in RemoteAudios) {
                    if (i == res) {
                        return 2
                    }
                }
            }
            return 0
        }
    }
}