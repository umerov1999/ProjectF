package dev.ragnarok.fenrir.upload.impl

import android.annotation.SuppressLint
import android.content.Context
import androidx.exifinterface.media.ExifInterface
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.interfaces.IPhotosStorage
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.ExifGeoDegree
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onCompletion
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

class Photo2AlbumUploadable(
    private val context: Context,
    private val networker: INetworker,
    private val storage: IPhotosStorage
) : IUploadable<Photo> {
    @SuppressLint("CheckResult")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Photo>> {
        val accountId = upload.accountId
        val albumId = upload.destination.id
        val groupId =
            if (upload.destination.ownerId < 0) abs(upload.destination.ownerId) else null
        val serverSingle = if (initialServer != null) {
            toFlow(initialServer)
        } else {
            networker.vkDefault(accountId)
                .photos()
                .getUploadServer(albumId, groupId)
        }
        return serverSingle.flatMapConcat { server ->
            var inputStream: InputStream? = null
            try {
                inputStream = UploadUtils.openStream(context, upload.fileUri, upload.size)
                if (inputStream == null) {
                    toFlowThrowable(
                        NotFoundException(
                            "Unable to open InputStream, URI: ${upload.fileUri}"
                        )
                    )
                } else {
                    networker.uploads()
                        .uploadPhotoToAlbumRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            inputStream,
                            listener
                        )
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            var latitude: Double? = null
                            var longitude: Double? = null
                            try {
                                val exif = UploadUtils.createStream(
                                    context, upload.fileUri
                                )?.let {
                                    ExifInterface(
                                        it
                                    )
                                }
                                val exifGeoDegree = exif?.let { ExifGeoDegree(it) }
                                exifGeoDegree?.let {
                                    if (it.isValid) {
                                        latitude = it.latitude
                                        longitude = it.longitude
                                    }
                                }
                            } catch (ignored: Exception) {
                            }
                            if (dto.photos_list.isNullOrEmpty()) {
                                toFlowThrowable(NotFoundException("VK doesn't upload this file"))
                            } else {
                                networker
                                    .vkDefault(accountId)
                                    .photos()
                                    .save(
                                        albumId,
                                        groupId,
                                        dto.server,
                                        dto.photos_list,
                                        dto.hash,
                                        latitude,
                                        longitude,
                                        null
                                    )
                                    .flatMapConcat { photos ->
                                        if (photos.isEmpty()) {
                                            toFlowThrowable(
                                                NotFoundException()
                                            )
                                        } else {
                                            val entity = Dto2Entity.mapPhoto(photos[0])
                                            val photo = Dto2Model.transform(photos[0])
                                            val result = toFlow(UploadResult(server, photo))
                                            if (upload.isAutoCommit) commit(
                                                storage,
                                                upload,
                                                entity
                                            ).andThen(
                                                result
                                            ) else result
                                        }
                                    }
                            }
                        }
                }
            } catch (e: Exception) {
                safelyClose(inputStream)
                if (e is CancellationException) {
                    throw e
                }
                toFlowThrowable(e)
            }
        }
    }

    private fun commit(
        storage: IPhotosStorage,
        upload: Upload,
        entity: PhotoDboEntity
    ): Flow<Boolean> {
        return storage.insertPhotosRx(
            upload.accountId,
            entity.ownerId,
            entity.albumId,
            listOf(entity),
            false
        )
    }
}