package dev.ragnarok.filegallery.util

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
import androidx.work.*
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.NotificationHelper
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.settings.theme.ThemesController
import dev.ragnarok.filegallery.util.toast.CustomToast
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DownloadWorkUtils {
    @SuppressLint("ConstantLocale")
    private val DOWNLOAD_DATE_FORMAT: DateFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

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

    private fun toDefaultInternalDownloader(context: Context, url: String, file: DownloadInfo) {
        val downloadWork = OneTimeWorkRequest.Builder(DefaultDownloadWorker::class.java)
        val data = Data.Builder()
        data.putString(ExtraDwn.URL, url)
        data.putString(ExtraDwn.DIR, file.path)
        data.putString(ExtraDwn.FILE, file.file)
        data.putString(ExtraDwn.EXT, file.ext)
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
            CustomToast.createCustomToast(context, null)?.showToastError(R.string.exist_audio)
            return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun track_file_exist(context: Context, file: DownloadInfo): Boolean {
        file.buildFilename()
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
            return true
        }
        return false
    }

    fun TrackIsDownloaded(audio: Audio): Boolean {
        if (audio.isLocal) {
            return true
        }
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return MusicPlaybackController.tracksExist.isExistAllAudio(audioName)
    }

    fun doDownloadVideo(context: Context, video: Video, url: String, Res: String) {
        val result_filename = DownloadInfo(
            makeLegalFilename(
                optString(video.title) +
                        " - " + video.ownerId + "_" + video.id + "_" + Res + "P", null
            ), Settings.get().main().getVideoDir(), "mp4"
        )
        CheckDirectory(result_filename.path)
        if (default_file_exist(context, result_filename)) {
            return
        }
        try {
            if (!Settings.get().main().isUse_internal_downloader()) {
                toExternalDownloader(context, url, result_filename)
            } else {
                toDefaultInternalDownloader(context, url, result_filename)
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context, null)
                ?.showToastError("Video Error: " + e.message)
            return
        }
    }

    fun doDownloadPhoto(context: Context, url: String, dir: String, file: String) {
        val result_filename = DownloadInfo(file, dir, "jpg")
        if (default_file_exist(context, result_filename)) {
            return
        }
        try {
            if (!Settings.get().main().isUse_internal_downloader()) {
                toExternalDownloader(context, url, result_filename)
            } else {
                toDefaultInternalDownloader(context, url, result_filename)
            }
        } catch (e: Exception) {
            CustomToast.createCustomToast(context, null)
                ?.showToastError("Photo Error: " + e.message)
            return
        }
    }

    fun doDownloadAudio(
        context: Context,
        audio: Audio,
        Force: Boolean,
    ): Int {
        if (audio.url.nonNullNoEmpty() && (audio.url?.contains("file://") == true || audio.url?.contains(
                "content://"
            ) == true)
        )
            return 2

        val result_filename = DownloadInfo(
            makeLegalFilename(audio.artist + " - " + audio.title, null),
            Settings.get().main().getMusicDir(),
            "mp3"
        )
        CheckDirectory(result_filename.path)
        val download_status = track_file_exist(context, result_filename)
        if (download_status && !Force) {
            return 1
        }
        if (download_status) {
            result_filename.setFile(result_filename.file + ("." + DOWNLOAD_DATE_FORMAT.format(Date())))
        }
        try {
            val downloadWork = OneTimeWorkRequest.Builder(TrackDownloadWorker::class.java)
            val data = Data.Builder()
            data.putString(ExtraDwn.URL, audio.url)
            data.putString(ExtraDwn.DIR, result_filename.path)
            data.putString(ExtraDwn.FILE, result_filename.file)
            data.putString(ExtraDwn.EXT, result_filename.ext)
            downloadWork.setInputData(data.build())
            WorkManager.getInstance(context).enqueue(downloadWork.build())
        } catch (e: Exception) {
            CustomToast.createCustomToast(context, null)
                ?.showToastError("Audio Error: " + e.message)
            return 2
        }
        return 0
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
        protected fun doDownload(
            url: String?,
            file_v: DownloadInfo,
            UseMediaScanner: Boolean
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
                    val builder = Utils.createOkHttp(Constants.DOWNLOAD_TIMEOUT)
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
                            response.close()
                            File(file).delete()
                            mNotifyManager.cancel(
                                id.toString(),
                                NotificationHelper.NOTIFICATION_DOWNLOADING
                            )
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
                Utils.inMainThread(object : Utils.SafeCall {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext, null)
                            ?.showToastError(R.string.error_with_message, e.localizedMessage)
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

            val ret = doDownload(inputData.getString(ExtraDwn.URL)!!, file_v, true)
            if (ret) {
                val mBuilder = createNotification(
                    applicationContext,
                    applicationContext.getString(R.string.downloading),
                    applicationContext.getString(R.string.success)
                            + " " + file_v.buildFilename(),
                    R.drawable.save,
                    true
                )
                mBuilder.color = ThemesController.getCurrentTheme().colorDayPrimary

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

                show_notification(
                    mBuilder,
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    NotificationHelper.NOTIFICATION_DOWNLOADING
                )
                Utils.inMainThread(object : Utils.SafeCall {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext, null)
                            ?.showToastInfo(R.string.saved)
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

            val url = inputData.getString(ExtraDwn.URL)

            if (url.isNullOrEmpty()) {
                return Result.failure()
            }

            val ret = doDownload(
                url,
                file_v,
                true
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
                mBuilder.color = ThemesController.getCurrentTheme().colorDayPrimary

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

                show_notification(
                    mBuilder,
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    NotificationHelper.NOTIFICATION_DOWNLOADING
                )
                MusicPlaybackController.tracksExist.addAudio(file_v.buildFilename())
                Utils.inMainThread(object : Utils.SafeCall {
                    override fun call() {
                        CustomToast.createCustomToast(applicationContext, null)
                            ?.showToastInfo(R.string.saved)
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
