package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

class VideoUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Video> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Video>> {
        val accountId = upload.accountId
        val ownerId = upload.destination.ownerId
        val groupId = if (ownerId >= 0) null else ownerId
        val isPrivate = upload.destination.id == 0
        val serverSingle = networker.vkDefault(accountId)
            .video()
            .getVideoServer(
                if (isPrivate) 1 else 0, groupId, UploadUtils.findFileName(
                    context, upload.fileUri
                )
            )
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
                        .map { dto ->
                            UploadResult(
                                server,
                                Video().setId(dto.video_id).setOwnerId(dto.owner_id).setTitle(
                                    UploadUtils.findFileName(
                                        context, upload.fileUri
                                    )
                                )
                            )
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
}