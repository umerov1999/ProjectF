package dev.ragnarok.fenrir.upload.impl

import android.annotation.SuppressLint
import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Method
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

class Photo2WallUploadable(
    private val context: Context,
    private val networker: INetworker,
    private val attachmentsRepository: IAttachmentsRepository
) : IUploadable<Photo> {
    @SuppressLint("CheckResult")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Photo>> {
        val subjectOwnerId = upload.destination.ownerId
        val userId = if (subjectOwnerId > 0) subjectOwnerId else null
        val groupId = if (subjectOwnerId < 0) abs(subjectOwnerId) else null
        val accountId = upload.accountId
        val serverSingle = if (initialServer != null) {
            toFlow(initialServer)
        } else {
            networker.vkDefault(accountId)
                .photos()
                .getWallUploadServer(groupId)
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
                        .uploadPhotoToWallRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            inputStream,
                            listener
                        )
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            if (dto.photo.isNullOrEmpty()) {
                                toFlowThrowable(NotFoundException("VK doesn't upload this file"))
                            } else {
                                networker.vkDefault(accountId)
                                    .photos()
                                    .saveWallPhoto(
                                        userId,
                                        groupId,
                                        dto.photo,
                                        dto.server,
                                        dto.hash,
                                        null,
                                        null,
                                        null
                                    )
                                    .flatMapConcat {
                                        if (it.isEmpty()) {
                                            toFlowThrowable(
                                                NotFoundException()
                                            )
                                        } else {
                                            val photo = Dto2Model.transform(it[0])
                                            val result = UploadResult(server, photo)
                                            if (upload.isAutoCommit) {
                                                commit(
                                                    attachmentsRepository,
                                                    upload,
                                                    photo
                                                ).map {
                                                    result
                                                }
                                            } else {
                                                toFlow(result)
                                            }
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
        repository: IAttachmentsRepository,
        upload: Upload,
        photo: Photo
    ): Flow<Boolean> {
        val accountId = upload.accountId
        val dest = upload.destination
        return when (dest.method) {
            Method.TO_COMMENT -> repository
                .attach(accountId, AttachToType.COMMENT, dest.id, listOf(photo))

            Method.TO_WALL -> repository
                .attach(accountId, AttachToType.POST, dest.id, listOf(photo))

            else -> toFlowThrowable(UnsupportedOperationException())
        }
    }
}