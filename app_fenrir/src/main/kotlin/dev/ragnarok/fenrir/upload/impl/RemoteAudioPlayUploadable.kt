package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiAudioUploadServer
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLEncoder

class RemoteAudioPlayUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Audio> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Audio>> {
        var inputStream: InputStream? = null
        val local_settings = Settings.get().main().localServer
        return try {
            var server_url = firstNonEmptyString(
                local_settings.url,
                "https://debug.dev"
            ) + "/method/audio.remoteplay"
            if (local_settings.password != null) {
                server_url += "?password=" + URLEncoder.encode(local_settings.password, "utf-8")
            }
            val uri = upload.fileUri
            val file = File(uri?.path ?: throw NotFoundException("uri.path is empty"))
            inputStream = if (file.isFile) {
                FileInputStream(file)
            } else {
                context.contentResolver.openInputStream(uri)
            }
            if (inputStream == null) {
                return toFlowThrowable(
                    NotFoundException(
                        "Unable to open InputStream, URI: $uri"
                    )
                )
            }
            val filename = UploadUtils.findFileName(
                context, uri
            )
            networker.uploads()
                .remotePlayAudioRx(server_url, filename, inputStream, listener)
                .onCompletion { safelyClose(inputStream) }
                .map { dto ->
                    UploadResult(
                        VKApiAudioUploadServer(server_url), Audio().setId(
                            dto.response ?: throw NotFoundException()
                        )
                    )
                }
        } catch (e: Exception) {
            safelyClose(inputStream)
            toFlowThrowable(e)
        }
    }
}
