package dev.ragnarok.fenrir.upload.impl

import android.annotation.SuppressLint
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
import io.reactivex.rxjava3.core.Single
import java.io.InputStream

class ChatPhotoUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<String> {
    @SuppressLint("CheckResult")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<String>> {
        val accountId = upload.accountId
        val chat_id = upload.destination.ownerId
        val serverSingle: Single<UploadServer> = if (initialServer == null) {
            networker.vkDefault(accountId)
                .photos()
                .getChatUploadServer(chat_id)
                .map { s -> s }
        } else {
            Single.just(initialServer)
        }
        return serverSingle.flatMap { server ->
            var `is`: InputStream? = null
            try {
                `is` = UploadUtils.openStream(context, upload.fileUri, upload.size)
                networker.uploads()
                    .uploadChatPhotoRx(server.url, `is`!!, listener)
                    .doFinally { safelyClose(`is`) }
                    .flatMap { dto ->
                        networker.vkDefault(accountId)
                            .photos()
                            .setChatPhoto(dto.response)
                            .flatMap { response ->
                                if (response.message_id == 0 || response.chat == null) {
                                    Single.error<UploadResult<String>>(
                                        NotFoundException("message_id=0")
                                    )
                                }
                                Single.just(
                                    UploadResult(
                                        server,
                                        firstNonEmptyString(
                                            response.chat?.photo_200,
                                            response.chat?.photo_100,
                                            response.chat?.photo_50
                                        ) ?: ""
                                    )
                                )
                            }
                    }
            } catch (e: Exception) {
                safelyClose(`is`)
                Single.error(e)
            }
        }
    }
}