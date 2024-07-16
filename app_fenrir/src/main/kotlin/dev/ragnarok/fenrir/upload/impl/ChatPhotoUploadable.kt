package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

class ChatPhotoUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<String> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<String>> {
        val accountId = upload.accountId
        val chat_id = upload.destination.ownerId
        val serverSingle = if (initialServer == null) {
            networker.vkDefault(accountId)
                .photos()
                .getChatUploadServer(chat_id)
        } else {
            toFlow(initialServer)
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
                        .uploadChatPhotoRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            inputStream,
                            listener
                        )
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            if (dto.response.isNullOrEmpty()) {
                                toFlowThrowable(NotFoundException("VK doesn't upload this file"))
                            } else {
                                networker.vkDefault(accountId)
                                    .photos()
                                    .setChatPhoto(dto.response)
                                    .map { response ->
                                        if (response.message_id == 0 || response.chat == null) {
                                            throw NotFoundException("message_id=0")
                                        } else {
                                            UploadResult(
                                                server,
                                                firstNonEmptyString(
                                                    response.chat?.photo_200,
                                                    response.chat?.photo_100,
                                                    response.chat?.photo_50
                                                ) ?: ""
                                            )
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