package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.upload.IUploadable
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

class AudioUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Audio> {
    @Suppress("BlockingMethodInNonBlockingContext")
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Audio>> {
        val accountId = upload.accountId
        val serverSingle = if (initialServer == null) {
            networker.vkDefault(accountId)
                .audio()
                .uploadServer
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
                    var TrackName = filename?.replace(".mp3", "").orEmpty()
                    var Artist = ""
                    val arr = TrackName.split(" - ".toRegex()).toTypedArray()
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
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            if (dto.audio.isNullOrEmpty()) {
                                toFlowThrowable(NotFoundException("VK doesn't upload this file"))
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
                                    .map {
                                        val document = Dto2Model.transform(it)
                                        UploadResult(server, document)
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