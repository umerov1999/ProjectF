package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.MessageMethod
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class StoryUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Story> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<Story>> {
        val accountId = upload.accountId
        val serverSingle: Single<UploadServer> = if (initialServer == null) {
            if (upload.destination.messageMethod == MessageMethod.VIDEO) networker.vkDefault(
                accountId
            ).users().stories_getVideoUploadServer()
                .map { it } else networker.vkDefault(accountId).users()
                .stories_getPhotoUploadServer().map { it }
        } else {
            Single.just(initialServer)
        }
        return serverSingle.flatMap { server ->
            var inputStream: InputStream? = null
            try {
                val uri = upload.fileUri
                val file = File(uri!!.path!!)
                inputStream = if (file.isFile) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                if (inputStream == null) {
                    return@flatMap Single.error<UploadResult<Story>>(
                        NotFoundException(
                            "Unable to open InputStream, URI: $uri"
                        )
                    )
                }
                val filename = UploadUtils.findFileName(context, uri)
                return@flatMap networker.uploads()
                    .uploadStoryRx(
                        server.url ?: throw NotFoundException("upload url empty"),
                        filename,
                        inputStream,
                        listener,
                        upload.destination.messageMethod == MessageMethod.VIDEO
                    )
                    .doFinally(safelyCloseAction(inputStream))
                    .flatMap { dto ->
                        networker
                            .vkDefault(accountId)
                            .users()
                            .stories_save(dto.response?.upload_result)
                            .map {
                                listEmptyIfNull(it.items)
                            }
                            .flatMap { tmpList ->
                                owners.findBaseOwnersDataAsBundle(
                                    accountId, listOf(
                                        Settings.get().accounts().current
                                    ), IOwnersRepository.MODE_ANY, null
                                )
                                    .flatMap {
                                        val document =
                                            Dto2Model.transformStory(tmpList[0], it)
                                        val result = UploadResult(server, document)
                                        Single.just(result)
                                    }
                            }
                    }
            } catch (e: Exception) {
                safelyClose(inputStream)
                Single.error(e)
            }
        }
    }
}