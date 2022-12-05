package dev.ragnarok.filegallery.upload.impl

import android.content.Context
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.upload.IUploadable
import dev.ragnarok.filegallery.upload.Upload
import dev.ragnarok.filegallery.upload.UploadResult
import dev.ragnarok.filegallery.upload.UploadUtils
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import dev.ragnarok.filegallery.util.Utils.safelyClose
import dev.ragnarok.filegallery.util.rxutils.RxUtils.safelyCloseAction
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLEncoder

class RemoteAudioPlayUploadable(private val context: Context, private val networker: INetworker) :
    IUploadable<Audio> {
    override fun doUpload(
        upload: Upload,
        listener: PercentagePublisher?
    ): Single<UploadResult<Audio>> {
        var `is`: InputStream? = null
        val local_settings = Settings.get().main().getLocalServer()
        return try {
            var server_url = firstNonEmptyString(
                local_settings.url,
                "https://debug.dev"
            ) + "/method/audio.remoteplay"
            if (local_settings.password != null) {
                server_url += "?password=" + URLEncoder.encode(local_settings.password, "utf-8")
            }
            val uri = upload.fileUri
            val file = File(uri!!.path!!)
            `is` = if (file.isFile) {
                FileInputStream(file)
            } else {
                context.contentResolver.openInputStream(uri)
            }
            if (`is` == null) {
                return Single.error(
                    Exception(
                        "Unable to open InputStream, URI: $uri"
                    )
                )
            }
            val filename = UploadUtils.findFileName(
                context, uri
            )
            networker.localServerApi()
                .remotePlayAudioRx(server_url, filename, `is`, listener)
                .doFinally(safelyCloseAction(`is`))
                .flatMap { dto ->
                    Single.just(
                        UploadResult(
                            Audio().setId(
                                dto.response ?: throw Exception()
                            )
                        )
                    )
                }
        } catch (e: Exception) {
            safelyClose(`is`)
            Single.error(e)
        }
    }
}