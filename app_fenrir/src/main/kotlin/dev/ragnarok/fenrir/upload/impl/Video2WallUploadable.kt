package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.upload.*
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.abs

class Video2WallUploadable(
    private val context: Context,
    private val networker: INetworker,
    private val attachmentsRepository: IAttachmentsRepository
) : IUploadable<Video> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<Video>> {
        val accountId = upload.accountId
        val ownerId = upload.destination.ownerId
        val groupId = if (ownerId < 0) abs(ownerId) else null
        val serverSingle = networker.vkDefault(accountId)
            .docs()
            .getVideoServer(1, groupId, UploadUtils.findFileName(context, upload.fileUri))
            .map<UploadServer> { s: VKApiVideosUploadServer -> s }
        return serverSingle.flatMap { server ->
            var `is`: InputStream? = null
            try {
                val uri = upload.fileUri
                val file = File(uri!!.path!!)
                `is` = if (file.isFile) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                if (`is` == null) {
                    return@flatMap Single.error<UploadResult<Video>>(
                        NotFoundException(
                            "Unable to open InputStream, URI: $uri"
                        )
                    )
                }
                val filename = UploadUtils.findFileName(context, uri)
                networker.uploads()
                    .uploadVideoRx(server.url, filename, `is`, listener)
                    .doFinally(safelyCloseAction(`is`))
                    .flatMap { dto ->
                        val video = Video().setId(dto.video_id).setOwnerId(dto.owner_id).setTitle(
                            UploadUtils.findFileName(
                                context, upload.fileUri
                            )
                        )
                        val result = UploadResult(server, video)
                        if (upload.isAutoCommit) {
                            commit(attachmentsRepository, upload, video).andThen(
                                Single.just(result)
                            )
                        } else {
                            Single.just(result)
                        }
                    }
            } catch (e: Exception) {
                safelyClose(`is`)
                Single.error(e)
            }
        }
    }

    private fun commit(
        repository: IAttachmentsRepository,
        upload: Upload,
        video: Video
    ): Completable {
        val accountId = upload.accountId
        val dest = upload.destination
        when (dest.method) {
            Method.TO_COMMENT -> return repository
                .attach(accountId, AttachToType.COMMENT, dest.id, listOf(video))
            Method.TO_WALL -> return repository
                .attach(accountId, AttachToType.POST, dest.id, listOf(video))
        }
        return Completable.error(UnsupportedOperationException())
    }
}