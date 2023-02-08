package dev.ragnarok.fenrir.util

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.work.*
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.dialog.audioduplicate.AudioDuplicateDialog
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.FileUtils
import dev.ragnarok.fenrir.module.hls.TSDemuxer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.service.QuickReplyService
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.util.hls.M3U8
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import dev.ragnarok.fenrir.util.toast.CustomToast
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DownloadWorkUtils {
    @SuppressLint("ConstantLocale")
    private val DOWNLOAD_DATE_FORMAT: DateFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Utils.appLocale)

    internal fun createNotification(
        context: Context,
        Title: String?,
        Text: String?,
        icon: Int,
        fin: Boolean
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, AppNotificationChannels.DOWNLOAD_CHANNEL_ID)
            .setContentTitle(Title)
            .setContentText(Text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(fin)
            .setOngoing(!fin)
            .setCategory(if (fin) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(if (fin) "DOWNLOADING_OPERATION" else null)
            .setOnlyAlertOnce(true)
    }

    internal fun createNotificationManager(context: Context): NotificationManagerCompat {
        val mNotifyManager = NotificationManagerCompat.from(context)
        if (Utils.hasOreo()) {
            mNotifyManager.createNotificationChannel(
                AppNotificationChannels.getDownloadChannel(
                    context
                )
            )
        }
        return mNotifyManager
    }

    private val ILLEGAL_FILENAME_CHARS = charArrayOf(
        '#',
        '%',
        '&',
        '{',
        '}',
        '\\',
        '<',
        '>',
        '*',
        '?',
        '/',
        '$',
        '\'',
        '\"',
        ':',
        '@',
        '`',
        '|',
        '='
    )

    private fun makeLegalFilenameFull(filename: String): String {
        var filename_temp = filename.trim { it <= ' ' }

        var s = '\u0000'
        while (s < ' ') {
            filename_temp = filename_temp.replace(s, '_')
            s++
        }
        for (i in ILLEGAL_FILENAME_CHARS.indices) {
            filename_temp = filename_temp.replace(ILLEGAL_FILENAME_CHARS[i], '_')
        }
        return filename_temp
    }

    fun makeLegalFilename(filename: String, extension: String?): String {
        var result = makeLegalFilenameFull(filename)
        if (result.length > 90) result = result.substring(0, 90).trim { it <= ' ' }
        if (extension == null)
            return result
        return "$result.$extension"
    }

    fun fixStart(filename: String?): String? {
        if (filename.isNullOrEmpty()) {
            return filename
        }
        if (filename[0] == '.') {
            return "_" + filename.substring(1)
        }
        return filename
    }

    fun makeLegalFilenameFromArg(filename: String?, extension: String?): String? {
        filename ?: return null
        var result = makeLegalFilenameFull(filename)
        if (result.length > 90) result = result.substring(0, 90).trim { it <= ' ' }
        if (extension == null)
            return result
        return "$result.$extension"
    }

    private fun optString(value: String?): String {
        return if (value.isNullOrEmpty()) "" else value
    }

    fun CheckDirectory(Path: String): Boolean {
        val dir_final = File(Path)
        return if (!dir_final.isDirectory) {
            dir_final.mkdirs()
        } else dir_final.setLastModified(Calendar.getInstance().time.time)
    }

    @Suppress("DEPRECATION")
    private fun toExternalDownloader(context: Context, url: String, file: DownloadInfo) {
        val downloadRequest = DownloadManager.Request(Uri.parse(url))
        downloadRequest.allowScanningByMediaScanner()
        downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadRequest.setDescription(file.buildFilename())
        downloadRequest.setDestinationUri(Uri.fromFile(File(file.build())))
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        downloadManager?.enqueue(downloadRequest)
    }

    private fun toDefaultInternalDownloader(
        context: Context,
        url: String,
        file: DownloadInfo,
        type: String
    ) {
        val downloadWork = OneTimeWorkRequest.Builder(DefaultDownloadWorker::class.java)
        val data = Data.Builder()
        data.putString(ExtraDwn.URL, url)
        data.putString(ExtraDwn.DIR, file.path)
        data.putString(ExtraDwn.FILE, file.file)
        data.putString(ExtraDwn.EXT, file.ext)
        data.putString(ExtraDwn.TYPE, type)
        downloadWork.setInputData(data.build())
        WorkManager.getInstance(context).enqueue(downloadWork.build())
    }

    @Suppress("DEPRECATION")
    private fun default_file_exist(context: Context, file: DownloadInfo): Boolean {
        val Temp = File(file.build())
        if (Temp.exists()) {
            if (Temp.setLastModified(Calendar.getInstance().time.time)) {
                context.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(Temp)
                    )
                )
            }
            CustomToast.createCustomToast(context).showToastError(R.string.exist_audio)
            return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun track_file_exist(context: Context, file: DownloadInfo): Int {
        val file_name = file.buildFilename()
        val Temp = File(file.build())
        if (Temp.exists()) {
            if (Temp.setLastModified(Calendar.getInstance().time.time)) {
                context.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(Temp)
                    )
                )
            }
            return 1
        }
        if (MusicPlaybackController.tracksExist.isExistRemoteAudio(file_name)) {
            return 2
        }
        return 0
    }

    fun TrackIsDownloaded(audio: Audio): Int {
        if (audio.isLocal) {
            return 1
        }
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return MusicPlaybackController.tracksExist.isExistAllAudio(audioName)
    }


    fun GetLocalTrackLink(audio: Audio): String {
        if (audio.url?.contains("file://") == true || audio.url?.contains("content://") == true)
            return audio.url!!
        return "file://" + Settings.get()
            .other().musicDir + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
    }


    fun doSyncRemoteAudio(context: Context) {
        val url =
            Settings.get()
                .other().localServer.url + "/method/audio.dumplist?password=" + Settings.get()
                .other().localServer.password
        val result_filename = DownloadInfo(
            "local_server_audio_list",
            Settings.get().other().musicDir,
            "json"
        )
        CheckDirectory(result_filename.path)
        val Temp = File(result_filename.build())
        if (Temp.exists()) {
            Temp.delete()
        }
        try {
            if (!Settings.get().other().isUse_internal_downloader) {
                toExternalDownloader(context, url, result_filename)
            } else {
                toDefaultInternalDownloader(context, url, result_filename, "json")
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("audio.dumplist: " + e.message)
            return
        }
    }

    fun doDownloadVideo(context: Context, video: Video, url: String, Res: String) {
        val result_filename = DownloadInfo(
            makeLegalFilename(
                optString(video.title) +
                        " - " + video.ownerId + "_" + video.id + "_" + Res + "P", null
            ), Settings.get().other().videoDir, "mp4"
        )
        CheckDirectory(result_filename.path)
        if (default_file_exist(context, result_filename)) {
            return
        }
        try {
            if (!Settings.get().other().isUse_internal_downloader) {
                toExternalDownloader(context, url, result_filename)
            } else {
                toDefaultInternalDownloader(context, url, result_filename, "video")
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("Video Error: " + e.message)
            return
        }
    }

    fun doDownloadVoice(context: Context, doc: VoiceMessage) {
        if (doc.getLinkMp3().isNullOrEmpty() && doc.getLinkOgg().isNullOrEmpty())
            return
        val extDownload: String
        val urlDownload: String
        if (doc.getLinkOgg().nonNullNoEmpty() && (doc.getLinkMp3().isNullOrEmpty() || Settings.get()
                .other().isDownload_voice_ogg)
        ) {
            extDownload = "ogg"
            urlDownload = (doc.getLinkOgg() ?: return)
        } else {
            extDownload = "mp3"
            urlDownload = doc.getLinkMp3().toString()
        }
        val result_filename = DownloadInfo(
            makeLegalFilename("Голосовуха " + doc.getOwnerId() + "_" + doc.getId(), null),
            Settings.get().other().docDir,
            extDownload
        )
        CheckDirectory(result_filename.path)
        if (default_file_exist(context, result_filename)) {
            return
        }
        try {
            if (!Settings.get().other().isUse_internal_downloader) {
                toExternalDownloader(context, urlDownload, result_filename)
            } else {
                toDefaultInternalDownloader(context, urlDownload, result_filename, "voice")
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("Voice Error: " + e.message)
            return
        }
    }

    fun doDownloadSticker(context: Context, sticker: Sticker) {
        val link: String? = if (sticker.isAnimated) {
            Utils.firstNonEmptyString(
                sticker.getAnimationByType("light"),
                sticker.getAnimationByType("dark")
            )
        } else {
            sticker.getImage(256, false).url
        }
        if (link.isNullOrEmpty())
            return
        val result_filename = DownloadInfo(
            makeLegalFilename(sticker.id.toString(), null),
            Settings.get().other().stickerDir,
            if (sticker.isAnimated) "json" else "png"
        )
        CheckDirectory(result_filename.path)
        if (default_file_exist(context, result_filename)) {
            return
        }
        try {
            if (!Settings.get().other().isUse_internal_downloader) {
                toExternalDownloader(context, link, result_filename)
            } else {
                toDefaultInternalDownloader(context, link, result_filename, "sticker")
            }
            Utils.getCachedMyStickers()
                .add(0, Sticker.LocalSticker(result_filename.build(), sticker.isAnimated))
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("Sticker Error: " + e.message)
            return
        }
    }

    private fun makeDoc(title: String, dir: String, ext: String?): DownloadInfo {
        var ext_i = Utils.firstNonEmptyString(ext, "doc")
        var file = title
        val pos = file.lastIndexOf('.')
        if (pos != -1) {
            ext_i = file.substring(pos + 1)
            file = file.substring(0, pos)
        }
        return DownloadInfo(file, dir, ext_i!!)
    }

    fun doDownloadDoc(context: Context, doc: Document, force: Boolean): Int {
        val pUrl = doc.url
        if (pUrl.isNullOrEmpty())
            return 2
        val result_filename =
            makeDoc(
                makeLegalFilename(doc.title ?: "null", null),
                Settings.get().other().docDir,
                doc.ext
            )
        CheckDirectory(result_filename.path)
        if (default_file_exist(context, result_filename)) {
            if (force) {
                result_filename.setFile(
                    result_filename.file + ("." + DOWNLOAD_DATE_FORMAT.format(
                        Date()
                    ))
                )
            } else {
                return 1
            }
        }
        try {
            if (!Settings.get().other().isUse_internal_downloader) {
                toExternalDownloader(context, pUrl, result_filename)
            } else {
                toDefaultInternalDownloader(context, pUrl, result_filename, "doc")
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("Docs Error: " + e.message)
            return 2
        }
        return 0
    }


    fun doDownloadPhoto(context: Context, url: String, dir: String, file: String) {
        val result_filename = DownloadInfo(file, dir, "jpg")
        if (default_file_exist(context, result_filename)) {
            return
        }
        try {
            if (!Settings.get().other().isUse_internal_downloader) {
                toExternalDownloader(context, url, result_filename)
            } else {
                toDefaultInternalDownloader(context, url, result_filename, "photo")
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("Photo Error: " + e.message)
            return
        }
    }

    fun doDownloadAudio(
        context: Context,
        audio: Audio,
        account_id: Long,
        Force: Boolean,
        isLocal: Boolean
    ): Int {
        if (audio.url.nonNullNoEmpty() && (audio.url?.contains("file://") == true || audio.url?.contains(
                "content://"
            ) == true)
        )
            return 3

        val result_filename = DownloadInfo(
            makeLegalFilename(audio.artist + " - " + audio.title, null),
            Settings.get().other().musicDir,
            "mp3"
        )
        CheckDirectory(result_filename.path)
        val download_status = track_file_exist(context, result_filename)
        if (download_status != 0 && !Force) {
            if (context !is FragmentActivity) {
                return download_status
            }
            val dialog =
                AudioDuplicateDialog.newInstance(
                    context,
                    account_id,
                    audio,
                    result_filename.build()
                )
                    ?: return download_status
            context.supportFragmentManager.setFragmentResultListener(
                AudioDuplicateDialog.REQUEST_CODE_AUDIO_DUPLICATE,
                dialog
            ) { _, result ->
                if (!result.getBoolean(Extra.TYPE)) {
                    if (File(result_filename.build()).delete()) {
                        doDownloadAudio(context, audio, account_id, false, isLocal)
                    }
                } else {
                    doDownloadAudio(context, audio, account_id, true, isLocal)
                }
            }

            dialog.show(context.supportFragmentManager, "audio_duplicates")
            return 0
        }
        if (download_status == 1) {
            result_filename.setFile(result_filename.file + ("." + DOWNLOAD_DATE_FORMAT.format(Date())))
        }
        try {
            val downloadWork = OneTimeWorkRequest.Builder(TrackDownloadWorker::class.java)
            val data = Data.Builder()
            data.putByteArray(ExtraDwn.URL, MsgPack.encodeToByteArrayEx(Audio.serializer(), audio))
            data.putString(ExtraDwn.DIR, result_filename.path)
            data.putString(ExtraDwn.FILE, result_filename.file)
            data.putString(ExtraDwn.EXT, result_filename.ext)
            data.putLong(ExtraDwn.ACCOUNT, account_id)
            data.putBoolean(ExtraDwn.NEED_UPDATE_TAG, !isLocal)
            downloadWork.setInputData(data.build())
            WorkManager.getInstance(context).enqueue(downloadWork.build())
        } catch (e: Exception) {
            CustomToast.createCustomToast(context).showToastError("Audio Error: " + e.message)
            return 3
        }
        return 0
    }


    fun makeDownloadRequestAudio(audio: Audio, account_id: Long): OneTimeWorkRequest {
        val result_filename = DownloadInfo(
            makeLegalFilename(audio.artist + " - " + audio.title, null),
            Settings.get().other().musicDir,
            "mp3"
        )
        val downloadWork = OneTimeWorkRequest.Builder(TrackDownloadWorker::class.java)
        val data = Data.Builder()
        data.putByteArray(ExtraDwn.URL, MsgPack.encodeToByteArrayEx(Audio.serializer(), audio))
        data.putString(ExtraDwn.DIR, result_filename.path)
        data.putString(ExtraDwn.FILE, result_filename.file)
        data.putString(ExtraDwn.EXT, result_filename.ext)
        data.putLong(ExtraDwn.ACCOUNT, account_id)
        data.putBoolean(ExtraDwn.NEED_UPDATE_TAG, true)
        downloadWork.setInputData(data.build())

        return downloadWork.build()
    }

    open class DefaultDownloadWorker(val context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        @SuppressLint("MissingPermission")
        protected fun show_notification(
            notification: NotificationCompat.Builder,
            id: Int,
            cancel_id: Int?
        ) {
            if (cancel_id != null) {
                if (AppPerms.hasNotificationPermissionSimple(context)) {
                    mNotifyManager.cancel(getId().toString(), cancel_id)
                }
            }
            if (id == NotificationHelper.NOTIFICATION_DOWNLOAD) {
                createGroupNotification()
            }
            if (AppPerms.hasNotificationPermissionSimple(context)) {
                mNotifyManager.notify(getId().toString(), id, notification.build())
            }
        }

        @SuppressLint("MissingPermission")
        private fun createGroupNotification() {
            if (!Utils.hasNougat()) {
                return
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                    ?: return
            val barNotifications = notificationManager.activeNotifications
            for (notification in barNotifications) {
                if (notification.id == NotificationHelper.NOTIFICATION_DOWNLOADING_GROUP) {
                    return
                }
            }
            if (AppPerms.hasNotificationPermissionSimple(context)) {
                mNotifyManager.notify(
                    NotificationHelper.NOTIFICATION_DOWNLOADING_GROUP,
                    NotificationCompat.Builder(context, AppNotificationChannels.DOWNLOAD_CHANNEL_ID)
                        .setSmallIcon(R.drawable.save)
                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                        .setGroup("DOWNLOADING_OPERATION").setGroupSummary(true).build()
                )
            }
        }

        @Suppress("DEPRECATION")
        protected fun doHLSDownload(
            url: String,
            file_v: DownloadInfo
        ): Boolean {
            var mBuilder = createNotification(
                applicationContext,
                applicationContext.getString(R.string.downloading),
                applicationContext.getString(R.string.downloading) + " "
                        + file_v.buildFilename(),
                R.drawable.save,
                false
            )

            show_notification(mBuilder, NotificationHelper.NOTIFICATION_DOWNLOADING, null)

            val file = file_v.build()
            try {
                val file_u = DownloadInfo(file_v.file, file_v.path, "ts")
                if (!M3U8(url, file_u.build()).run()) {
                    throw Exception("M3U8 error download")
                }
                if (!TSDemuxer.unpackTS(file_u.build(), file, info = false, print_debug = false)) {
                    throw Exception("Error TSDemuxer")
                }
                File(file_u.build()).delete()
                applicationContext.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(File(file))
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()

                mBuilder = createNotification(
                    applicationContext,
                    applicationContext.getString(R.string.downloading),
                    applicationContext.getString(R.string.error)
                            + " " + e.localizedMessage + ". " + file_v.buildFilename(),
                    R.drawable.ic_error_toast_vector,
                    true
                )
                mBuilder.color = Color.parseColor("#ff0000")
                show_notification(
                    mBuilder,
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    NotificationHelper.NOTIFICATION_DOWNLOADING
                )
                val result = File(file_v.build())
                if (result.exists()) {
                    file_v.setFile(file_v.file + "." + file_v.ext)
                    result.renameTo(File(file_v.setExt("error").build()))
                }
                Utils.inMainThread(object : Utils.SafeCallInt {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext)
                            .showToastError(R.string.error_with_message, e.localizedMessage)
                    }
                })
                return false
            }
            return true
        }

        @Suppress("DEPRECATION")
        protected fun doDownload(
            url: String?,
            file_v: DownloadInfo,
            UseMediaScanner: Boolean,
            type: String
        ): Boolean {
            var mBuilder = createNotification(
                applicationContext,
                applicationContext.getString(R.string.downloading),
                applicationContext.getString(R.string.downloading) + " "
                        + file_v.buildFilename(),
                R.drawable.save,
                false
            )
            mBuilder.addAction(
                R.drawable.close,
                applicationContext.getString(R.string.cancel),
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            )

            show_notification(mBuilder, NotificationHelper.NOTIFICATION_DOWNLOADING, null)

            val file = file_v.build()
            try {
                FileOutputStream(file).use { output ->
                    if (url.isNullOrEmpty()) throw Exception(applicationContext.getString(R.string.null_image_link))
                    val builder = Utils.createOkHttp(Constants.DOWNLOAD_TIMEOUT, false)
                    val request: Request = Request.Builder()
                        .url(url)
                        .build()
                    val response = builder.build().newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception(
                            "Server return " + response.code +
                                    " " + response.message
                        )
                    }
                    val bfr = response.body.byteStream()
                    val input = BufferedInputStream(bfr)
                    val data = ByteArray(80 * 1024)
                    var bufferLength: Int
                    var downloadedSize = 0L
                    var cntlength = response.header("Content-Length")
                    if (cntlength.isNullOrEmpty()) {
                        cntlength = response.header("Compressed-Content-Length")
                    }
                    var totalSize = 1L
                    if (cntlength.nonNullNoEmpty()) totalSize = cntlength.toLong()
                    while (input.read(data).also { bufferLength = it } != -1) {
                        if (isStopped) {
                            output.flush()
                            output.close()
                            input.close()
                            File(file).delete()
                            if (AppPerms.hasNotificationPermissionSimple(context)) {
                                mNotifyManager.cancel(
                                    id.toString(),
                                    NotificationHelper.NOTIFICATION_DOWNLOADING
                                )
                            }
                            response.close()
                            return false
                        }
                        output.write(data, 0, bufferLength)
                        downloadedSize += bufferLength
                        mBuilder.setProgress(
                            100,
                            (downloadedSize.toDouble() / totalSize * 100).toInt(),
                            false
                        )
                        show_notification(
                            mBuilder,
                            NotificationHelper.NOTIFICATION_DOWNLOADING,
                            null
                        )
                    }
                    output.flush()
                    output.close()
                    input.close()
                    response.close()
                    if (UseMediaScanner) {
                        applicationContext.sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(File(file))
                            )
                        )
                    }
                    if (type == "photo") {
                        MusicPlaybackController.tracksExist.addPhoto(file_v.buildFilename())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                mBuilder = createNotification(
                    applicationContext,
                    applicationContext.getString(R.string.downloading),
                    applicationContext.getString(R.string.error)
                            + " " + e.localizedMessage + ". " + file_v.buildFilename(),
                    R.drawable.ic_error_toast_vector,
                    true
                )
                mBuilder.color = Color.parseColor("#ff0000")
                show_notification(
                    mBuilder,
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    NotificationHelper.NOTIFICATION_DOWNLOADING
                )
                val result = File(file_v.build())
                if (result.exists()) {
                    file_v.setFile(file_v.file + "." + file_v.ext)
                    result.renameTo(File(file_v.setExt("error").build()))
                }
                Utils.inMainThread(object : Utils.SafeCallInt {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext)
                            .showToastError(R.string.error_with_message, e.localizedMessage)
                    }
                })
                return false
            }
            return true
        }

        @Suppress("DEPRECATION")
        private fun createForeground() {
            val builder: NotificationCompat.Builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        "worker_channel",
                        applicationContext.getString(R.string.channel_keep_work_manager),
                        NotificationManager.IMPORTANCE_NONE
                    )
                    mNotifyManager.createNotificationChannel(channel)
                    NotificationCompat.Builder(applicationContext, channel.id)
                } else {
                    NotificationCompat.Builder(applicationContext, "worker_channel")
                        .setPriority(Notification.PRIORITY_MIN)
                }
            builder.setContentTitle(applicationContext.getString(R.string.work_manager))
                .setContentText(applicationContext.getString(R.string.foreground_downloader))
                .setSmallIcon(R.drawable.web)
                .setColor(Color.parseColor("#dd0000"))
                .setOngoing(true)

            setForegroundAsync(
                ForegroundInfo(
                    NotificationHelper.NOTIFICATION_DOWNLOAD_MANAGER,
                    builder.build()
                )
            )
        }

        override fun doWork(): Result {
            createForeground()

            val file_v = DownloadInfo(
                inputData.getString(ExtraDwn.FILE)!!,
                inputData.getString(ExtraDwn.DIR)!!, inputData.getString(ExtraDwn.EXT)!!
            )

            val ret = doDownload(
                inputData.getString(ExtraDwn.URL)!!,
                file_v,
                true,
                inputData.getString(ExtraDwn.TYPE).orEmpty()
            )
            if (ret) {
                val mBuilder = createNotification(
                    applicationContext,
                    applicationContext.getString(R.string.downloading),
                    applicationContext.getString(R.string.success)
                            + " " + file_v.buildFilename(),
                    R.drawable.save,
                    true
                )
                mBuilder.color = ThemesController.toastColor(false)

                val intent_open = Intent(Intent.ACTION_VIEW)
                intent_open.setDataAndType(
                    FileProvider.getUriForFile(
                        applicationContext,
                        Constants.FILE_PROVIDER_AUTHORITY,
                        File(file_v.build())
                    ), MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(file_v.ext)
                ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val readPendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    id.hashCode(),
                    intent_open,
                    Utils.makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                )
                mBuilder.setContentIntent(readPendingIntent)

                if (Settings.get().other().isDeveloper_mode) {
                    val deleteIntent = QuickReplyService.intentForDeleteFile(
                        applicationContext,
                        file_v.build(),
                        NotificationHelper.NOTIFICATION_DOWNLOAD,
                        id.toString()
                    )
                    val deletePendingIntent = PendingIntent.getService(
                        applicationContext,
                        id.hashCode(),
                        deleteIntent,
                        Utils.makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                    )
                    val actionDelete = NotificationCompat.Action.Builder(
                        R.drawable.ic_outline_delete,
                        applicationContext.resources.getString(R.string.delete), deletePendingIntent
                    )
                        .build()
                    mBuilder.addAction(actionDelete)
                }

                show_notification(
                    mBuilder,
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    NotificationHelper.NOTIFICATION_DOWNLOADING
                )
                Utils.inMainThread(object : Utils.SafeCallInt {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext)
                            .showToastBottom(R.string.saved)
                    }
                })
            }
            return if (ret) Result.success() else Result.failure()
        }

        private val mNotifyManager: NotificationManagerCompat =
            createNotificationManager(applicationContext)

    }

    @Suppress("DEPRECATION")
    class TrackDownloadWorker(context: Context, workerParams: WorkerParameters) :
        DefaultDownloadWorker(context, workerParams) {
        override fun doWork(): Result {
            val file_v = DownloadInfo(
                inputData.getString(ExtraDwn.FILE)!!,
                inputData.getString(ExtraDwn.DIR)!!, inputData.getString(ExtraDwn.EXT)!!
            )
            val needCover =
                inputData.getBoolean(ExtraDwn.NEED_UPDATE_TAG, true) && FenrirNative.isNativeLoaded
            val audio = MsgPack.decodeFromByteArrayEx(
                Audio.serializer(),
                inputData.getByteArray(ExtraDwn.URL)!!
            )
            val account_id =
                inputData.getLong(ExtraDwn.ACCOUNT, ISettings.IAccountsSettings.INVALID_ID)

            val mode = audio.needRefresh()
            if (mode.first) {
                val link: String? = RxUtils.blockingGetSingle(
                    InteractorFactory
                        .createAudioInteractor()
                        .getByIdOld(account_id, listOf(audio), mode.second)
                        .map { e -> e[0].url ?: audio.url ?: "" }, audio.url
                )
                if (link.nonNullNoEmpty()) {
                    audio.setUrl(link)
                }
            }

            if (audio.url.isNullOrEmpty() || !FenrirNative.isNativeLoaded && audio.isHLS) {
                return Result.failure()
            }

            val ret = if (audio.isHLS) doHLSDownload(audio.url!!, file_v) else doDownload(
                audio.url,
                file_v,
                true, "audio"
            )
            if (ret) {
                val cover =
                    Utils.firstNonEmptyString(
                        audio.thumb_image_very_big,
                        audio.thumb_image_big,
                        audio.thumb_image_little
                    )
                var updated_tag = false
                if (needCover && cover.nonNullNoEmpty()) {
                    val cover_file = DownloadInfo(file_v.file, file_v.path, "jpg")
                    if (doDownload(cover, cover_file, false, "cover")) {
                        try {
                            val pathAudio = file_v.build()
                            val pathCover = cover_file.build()
                            var ifGenre: String? = null
                            val commentText = if (audio.lyricsId != 0) {
                                val LyricString: String? = RxUtils.blockingGetSingle(
                                    InteractorFactory.createAudioInteractor()
                                        .getLyrics(account_id, audio), null
                                )
                                if (LyricString.isNullOrEmpty()) {
                                    Audio.AudioCommentTag(audio.ownerId, audio.id).toText()
                                } else {
                                    Audio.AudioCommentTag(audio.ownerId, audio.id, LyricString)
                                        .toText()
                                }
                            } else {
                                Audio.AudioCommentTag(audio.ownerId, audio.id).toText()
                            }
                            if (audio.genreByID3 != 0) {
                                ifGenre = audio.genreByID3.toString()
                            }
                            updated_tag = FileUtils.audioTagModify(
                                pathAudio,
                                pathCover,
                                "image/jpg",
                                audio.title,
                                audio.artist,
                                audio.album_title,
                                ifGenre,
                                commentText
                            )
                            File(pathCover).delete()
                            applicationContext.sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(File(file_v.build()))
                                )
                            )

                        } catch (e: Throwable) {
                            Utils.inMainThread(object : Utils.SafeCallInt {
                                override fun call() {
                                    CustomToast.createCustomToast(applicationContext)
                                        .showToastError(
                                            R.string.error_with_message,
                                            e.localizedMessage
                                        )
                                }
                            })
                            e.printStackTrace()
                        }
                    }
                }

                val mBuilder = createNotification(
                    applicationContext,
                    applicationContext.getString(if (updated_tag) R.string.tag_modified else R.string.downloading),
                    applicationContext.getString(R.string.success)
                            + " " + file_v.buildFilename(),
                    R.drawable.save,
                    true
                )
                mBuilder.color = ThemesController.toastColor(false)

                val intent_open = Intent(Intent.ACTION_VIEW)
                intent_open.setDataAndType(
                    FileProvider.getUriForFile(
                        applicationContext,
                        Constants.FILE_PROVIDER_AUTHORITY,
                        File(file_v.build())
                    ), MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(file_v.ext)
                ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val ReadPendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    id.hashCode(),
                    intent_open,
                    Utils.makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                )
                mBuilder.setContentIntent(ReadPendingIntent)

                if (Settings.get().other().isDeveloper_mode) {
                    val DeleteIntent = QuickReplyService.intentForDeleteFile(
                        applicationContext,
                        file_v.build(),
                        NotificationHelper.NOTIFICATION_DOWNLOAD,
                        id.toString()
                    )
                    val DeletePendingIntent = PendingIntent.getService(
                        applicationContext,
                        id.hashCode(),
                        DeleteIntent,
                        Utils.makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                    )
                    val actionDelete = NotificationCompat.Action.Builder(
                        R.drawable.ic_outline_delete,
                        applicationContext.resources.getString(R.string.delete), DeletePendingIntent
                    )
                        .build()
                    mBuilder.addAction(actionDelete)
                }

                show_notification(
                    mBuilder,
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    NotificationHelper.NOTIFICATION_DOWNLOADING
                )
                MusicPlaybackController.tracksExist.addAudio(file_v.buildFilename())
                Utils.inMainThread(object : Utils.SafeCallInt {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext)
                            .showToastBottom(if (updated_tag) R.string.tag_modified else R.string.saved)
                    }
                })
            }
            return if (ret) Result.success() else Result.failure()
        }
    }

    private object ExtraDwn {
        const val URL = "url"
        const val DIR = "dir"
        const val FILE = "file"
        const val EXT = "ext"
        const val TYPE = "type"
        const val ACCOUNT = "account"
        const val NEED_UPDATE_TAG = "need_update_tag"
    }

    class DownloadInfo(file: String, path: String, ext: String) {

        fun setFile(file: String): DownloadInfo {
            this.file = file
            return this
        }

        fun setExt(ext: String): DownloadInfo {
            this.ext = ext
            return this
        }

        fun buildFilename(): String {
            return "$file.$ext"
        }

        fun build(): String {
            return "$path/$file.$ext"
        }

        var file: String = file
            private set
        var path: String = path
            private set
        var ext: String = ext
            private set
    }
}
