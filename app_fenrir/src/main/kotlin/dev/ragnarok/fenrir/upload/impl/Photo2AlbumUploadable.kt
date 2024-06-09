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
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.InputStream
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
    ): Single<UploadResult<Photo>> {
        val accountId = upload.accountId
        val albumId = upload.destination.id
        val groupId =
            if (upload.destination.ownerId < 0) abs(upload.destination.ownerId) else null
        val serverSingle: Single<UploadServer> = if (initialServer != null) {
            Single.just(initialServer)
        } else {
            networker.vkDefault(accountId)
                .photos()
                .getUploadServer(albumId, groupId)
                .map { it }
        }
        return serverSingle.flatMap { server ->
            var inputStream: InputStream? = null
            try {
                inputStream = UploadUtils.openStream(context, upload.fileUri, upload.size)
                if (inputStream == null) {
                    Single.error(
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
                        .doFinally(safelyCloseAction(inputStream))
                        .flatMap { dto ->
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
                                Single.error(NotFoundException("VK doesn't upload this file"))
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
                                    .flatMap { photos ->
                                        if (photos.isEmpty()) {
                                            Single.error(
                                                NotFoundException()
                                            )
                                        } else {
                                            val entity = Dto2Entity.mapPhoto(photos[0])
                                            val photo = Dto2Model.transform(photos[0])
                                            val result = Single.just(UploadResult(server, photo))
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
                Single.error(e)
            }
        }
    }

    private fun commit(
        storage: IPhotosStorage,
        upload: Upload,
        entity: PhotoDboEntity
    ): Completable {
        return storage.insertPhotosRx(
            upload.accountId,
            entity.ownerId,
            entity.albumId,
            listOf(entity),
            false
        )
    }
}