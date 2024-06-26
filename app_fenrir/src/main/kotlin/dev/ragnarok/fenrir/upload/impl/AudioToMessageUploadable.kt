package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.interfaces.IMessagesStorage
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class AudioToMessageUploadable(
    private val context: Context, private val networker: INetworker,
    private val attachmentsRepository: IAttachmentsRepository,
    private val messagesStorage: IMessagesStorage
) :
    IUploadable<Audio> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Single<UploadResult<Audio>> {
        val accountId = upload.accountId
        val messageId = upload.destination.id
        val serverSingle: Single<UploadServer> = if (initialServer == null) {
            networker.vkDefault(accountId)
                .audio()
                .uploadServer
                .map { it }
        } else {
            Single.just(initialServer)
        }
        return serverSingle.flatMap { server ->
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
                    Single.error(
                        NotFoundException(
                            "Unable to open InputStream, URI: $uri"
                        )
                    )
                } else {
                    val filename = UploadUtils.findFileName(context, uri)
                    var TrackName = filename?.replace(".mp3", "").orEmpty()
                    var Artist = ""
                    val arr = TrackName.split(Regex(" - ")).toTypedArray()
                    if (arr.size > 1) {
                        Artist = arr[0]
                        TrackName = TrackName.replace("$Artist - ", "")
                    }
                    val finalArtist = Artist
                    val finalTrackName = TrackName
                    networker.uploads()
                        .uploadAudioRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            filename,
                            inputStream,
                            listener
                        )
                        .doFinally(safelyCloseAction(inputStream))
                        .flatMap { dto ->
                            if (dto.audio.isNullOrEmpty()) {
                                Single.error(NotFoundException("VK doesn't upload this file"))
                            } else {
                                networker
                                    .vkDefault(accountId)
                                    .audio()
                                    .save(
                                        dto.server,
                                        dto.audio,
                                        dto.hash,
                                        finalArtist,
                                        finalTrackName
                                    )
                                    .flatMap {
                                        val audio = Dto2Model.transform(it)
                                        val result = UploadResult(server, audio)
                                        if (upload.isAutoCommit) {
                                            attachIntoDatabaseRx(
                                                attachmentsRepository,
                                                messagesStorage,
                                                accountId,
                                                messageId,
                                                audio
                                            )
                                                .andThen(Single.just(result))
                                        } else {
                                            Single.just(result)
                                        }
                                    }
                            }
                        }
                }
            } catch (e: Exception) {
                safelyClose(inputStream)
                Single.error(e)
            }
        }
    }

    companion object {
        fun attachIntoDatabaseRx(
            repository: IAttachmentsRepository, storage: IMessagesStorage,
            accountId: Long, messageId: Int, audio: Audio
        ): Completable {
            return repository
                .attach(accountId, AttachToType.MESSAGE, messageId, listOf(audio))
                .andThen(storage.notifyMessageHasAttachments(accountId, messageId))
        }
    }
}
