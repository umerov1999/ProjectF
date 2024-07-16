package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Video
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
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

class Video2WallUploadable(
    private val context: Context,
    private val networker: INetworker,
    private val attachmentsRepository: IAttachmentsRepository
) : IUploadable<Video> {
    @Suppress("BlockingMethodInNonBlockingContext")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Video>> {
        val accountId = upload.accountId
        val ownerId = upload.destination.ownerId
        val groupId = if (ownerId < 0) abs(ownerId) else null
        val serverSingle = networker.vkDefault(accountId)
            .video()
            .getVideoServer(1, groupId, UploadUtils.findFileName(context, upload.fileUri))
        return serverSingle.flatMapConcat { server ->
            var inputStream: InputStream? = null
            try {
                val uri = upload.fileUri
                val file = File(uri?.path ?: throw NotFoundException("uri.path is empty"))
                inputStream = if (file.isFile) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                if (inputStream == null) {
                    toFlowThrowable(
                        NotFoundException(
                            "Unable to open InputStream, URI: $uri"
                        )
                    )
                } else {
                    val filename = UploadUtils.findFileName(context, uri)
                    networker.uploads()
                        .uploadVideoRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            filename,
                            inputStream,
                            listener
                        )
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            val video =
                                Video().setId(dto.video_id).setOwnerId(dto.owner_id).setTitle(
                                    UploadUtils.findFileName(
                                        context, upload.fileUri
                                    )
                                )
                            val result = UploadResult(server, video)
                            if (upload.isAutoCommit) {
                                commit(attachmentsRepository, upload, video).map {
                                    result
                                }
                            } else {
                                toFlow(result)
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
        video: Video
    ): Flow<Boolean> {
        val accountId = upload.accountId
        val dest = upload.destination
        when (dest.method) {
            Method.TO_COMMENT -> return repository
                .attach(accountId, AttachToType.COMMENT, dest.id, listOf(video))

            Method.TO_WALL -> return repository
                .attach(accountId, AttachToType.POST, dest.id, listOf(video))
        }
        return toFlowThrowable(UnsupportedOperationException())
    }
}