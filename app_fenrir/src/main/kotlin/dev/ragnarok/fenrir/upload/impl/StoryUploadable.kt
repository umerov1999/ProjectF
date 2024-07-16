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
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.MessageMethod
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
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

class StoryUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Story> {
    @Suppress("BlockingMethodInNonBlockingContext")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Story>> {
        val accountId = upload.accountId
        val serverSingle = if (initialServer == null) {
            if (upload.destination.messageMethod == MessageMethod.VIDEO) networker.vkDefault(
                accountId
            ).stories().stories_getVideoUploadServer(null, upload.destination.ref)
            else {
                networker.vkDefault(accountId).stories()
                    .stories_getPhotoUploadServer(null, upload.destination.ref)
            }
        } else {
            toFlow(initialServer)
        }
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
                        .uploadStoryRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            filename,
                            inputStream,
                            listener,
                            upload.destination.messageMethod == MessageMethod.VIDEO
                        )
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            if (dto.error.nonNullNoEmpty()) {
                                toFlowThrowable(Exception(dto.error))
                            } else {
                                networker
                                    .vkDefault(accountId)
                                    .stories()
                                    .stories_save(dto.response?.upload_result)
                                    .map {
                                        listEmptyIfNull(it.items)
                                    }
                                    .flatMapConcat { tmpList ->
                                        if (tmpList.isEmpty()) {
                                            toFlowThrowable(NotFoundException("[stories_save] returned empty list"))
                                        } else {
                                            owners.findBaseOwnersDataAsBundle(
                                                accountId, listOf(
                                                    Settings.get().accounts().current
                                                ), IOwnersRepository.MODE_ANY, null
                                            ).map {
                                                val document =
                                                    Dto2Model.transformStory(tmpList[0], it)
                                                UploadResult(server, document)
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
}