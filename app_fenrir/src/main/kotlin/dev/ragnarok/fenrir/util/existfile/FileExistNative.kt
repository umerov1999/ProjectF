package dev.ragnarok.fenrir.util.existfile

import android.content.Context
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import dev.ragnarok.fenrir.module.FileUtils
import dev.ragnarok.fenrir.module.StringExist
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
import java.util.Locale
import kotlin.math.abs

class FileExistNative : AbsFileExist {
    private val CachedAudios = StringExist(true)
    private val RemoteAudios = StringExist(true)
    private val CachedPhotos = StringExist(true)

    @Throws(IOException::class)
    private fun findRemoteAudios(context: Context, requestPermission: Boolean) {
        if (requestPermission) {
            if (!hasReadStoragePermissionSimple(context)) return
        }
        RemoteAudios.clear()
        val audios = File(Settings.get().main().musicDir, "local_server_audio_list.json")
        if (!audios.exists()) {
            return
        }
        val reader = ReaderJsonLexer(
            OkioSerialReader(audios.source().buffer())
        )
        reader.consumeNextToken(WriteMode.LIST.begin)
        while (reader.canConsumeValue()) {
            RemoteAudios.insert(reader.consumeStringLenient().lowercase(Locale.getDefault()))
            reader.tryConsumeComma()
        }
        reader.consumeNextToken(WriteMode.LIST.end)
    }

    @Throws(IOException::class)
    override fun findRemoteAudios(context: Context) {
        findRemoteAudios(context, true)
    }

    private fun transform_owner(owner_id: Long): String {
        return if (owner_id < 0) "club" + abs(owner_id) else "id$owner_id"
    }

    private fun existPhoto(photo: Photo): Boolean {
        return CachedPhotos.contains(transform_owner(photo.ownerId) + "_" + photo.getObjectId())
    }

    override fun findLocalImages(photos: List<SelectablePhotoWrapper>): Flow<Boolean> {
        return flow {
            val temp = File(Settings.get().main().photoDir)
            if (!temp.exists()) {
                emit(false)
                return@flow
            }
            CachedPhotos.clear()
            CachedPhotos.lockMutex(true)
            FileUtils.listDirRecursive(temp.absolutePath, CachedPhotos.getNativePointer())
            CachedPhotos.lockMutex(false)
            for (i in photos) {
                i.setDownloaded(existPhoto(i.photo))
            }
            emit(true)
        }
    }

    override fun addAudio(file: String) {
        CachedAudios.insert(file.lowercase(Locale.getDefault()))
    }

    override fun addPhoto(file: String) {
        CachedPhotos.insert(file.lowercase(Locale.getDefault()))
    }

    override fun findAllAudios(context: Context): Flow<Boolean> {
        return if (!hasReadStoragePermissionSimple(context)) emptyTaskFlow() else flow {
            findRemoteAudios(context, false)
            val temp = File(Settings.get().main().musicDir)
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

    override fun markExistPhotos(photos: List<SelectablePhotoWrapper>) {
        if (photos.isEmpty()) {
            return
        }
        for (i in photos) {
            i.setDownloaded(existPhoto(i.photo))
        }
    }

    override fun isExistRemoteAudio(file: String): Boolean {
        return RemoteAudios.has(file.lowercase(Locale.getDefault()))
    }

    override fun isExistAllAudio(file: String): Int {
        val res = file.lowercase(Locale.getDefault())
        if (CachedAudios.has(res)) {
            return 1
        }
        return if (RemoteAudios.has(res)) {
            2
        } else 0
    }
}