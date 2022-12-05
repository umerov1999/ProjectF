package dev.ragnarok.fenrir.upload.impl

import android.annotation.SuppressLint
import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.interfaces.IMessagesStorage
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.InputStream

class Photo2MessageUploadable(
    private val context: Context,
    private val networker: INetworker,
    private val attachmentsRepository: IAttachmentsRepository,
    private val messagesStorage: IMessagesStorage
) : IUploadable<Photo> {
    @SuppressLint("CheckResult")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<Photo>> {
        val accountId = upload.accountId
        val messageId = upload.destination.id
        val serverSingle: Single<UploadServer> = if (initialServer != null) {
            Single.just(initialServer)
        } else {
            networker.vkDefault(accountId)
                .photos()
                .messagesUploadServer.map { s -> s }
        }
        return serverSingle.flatMap { server ->
            var `is`: InputStream? = null
            try {
                `is` = UploadUtils.openStream(context, upload.fileUri, upload.size)
                networker.uploads()
                    .uploadPhotoToMessageRx(server.url, `is`!!, listener)
                    .doFinally(safelyCloseAction(`is`))
                    .flatMap { dto ->
                        networker.vkDefault(accountId)
                            .photos()
                            .saveMessagesPhoto(dto.server, dto.photo, dto.hash)
                            .flatMap { photos ->
                                if (photos.isEmpty()) {
                                    Single.error<UploadResult<Photo>>(
                                        NotFoundException()
                                    )
                                }
                                val photo = Dto2Model.transform(photos[0])
                                val result = UploadResult(server, photo)
                                if (upload.isAutoCommit) {
                                    attachIntoDatabaseRx(
                                        attachmentsRepository,
                                        messagesStorage,
                                        accountId,
                                        messageId,
                                        photo
                                    )
                                        .andThen(Single.just(result))
                                } else {
                                    Single.just(result)
                                }
                            }
                    }
            } catch (e: Exception) {
                safelyClose(`is`)
                Single.error(e)
            }
        }
    }

    companion object {
        fun attachIntoDatabaseRx(
            repository: IAttachmentsRepository, storage: IMessagesStorage,
            accountId: Int, messageId: Int, photo: Photo
        ): Completable {
            return repository
                .attach(accountId, AttachToType.MESSAGE, messageId, listOf(photo))
                .andThen(storage.notifyMessageHasAttachments(accountId, messageId))
        }
    }
}