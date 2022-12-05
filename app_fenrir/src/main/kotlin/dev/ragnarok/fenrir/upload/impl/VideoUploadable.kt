package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class VideoUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Video> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<Video>> {
        val accountId = upload.accountId
        val ownerId = upload.destination.ownerId
        val groupId = if (ownerId >= 0) null else ownerId
        val isPrivate = upload.destination.id == 0
        val serverSingle = networker.vkDefault(accountId)
            .docs()
            .getVideoServer(
                if (isPrivate) 1 else 0, groupId, UploadUtils.findFileName(
                    context, upload.fileUri
                )
            )
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
                return@flatMap networker.uploads()
                    .uploadVideoRx(server.url, filename, `is`, listener)
                    .doFinally(safelyCloseAction(`is`))
                    .flatMap { dto ->
                        val result = UploadResult(
                            server, Video().setId(dto.video_id).setOwnerId(dto.owner_id).setTitle(
                                UploadUtils.findFileName(
                                    context, upload.fileUri
                                )
                            )
                        )
                        Single.just(result)
                    }
            } catch (e: Exception) {
                safelyClose(`is`)
                Single.error(e)
            }
        }
    }
}