package dev.ragnarok.fenrir.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.domain.*
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.toast.CustomToast
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

class FaveSyncWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val ownersRepository: IOwnersRepository = Repository.owners
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val wallRepository: IWallsRepository = Repository.walls
    private val communitiesInteractor: ICommunitiesInteractor =
        InteractorFactory.createCommunitiesInteractor()
    private val faves: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val shortcuts: ITempDataStorage = Includes.stores.tempStore()
    private val board: IBoardInteractor = InteractorFactory.createBoardInteractor()
    private val mNotifyManager = createNotificationManager(applicationContext)
    private val photosInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    private val videointeractor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private val docsInteractor: IDocsInteractor = InteractorFactory.createDocsInteractor()
    private fun createNotificationManager(context: Context): NotificationManagerCompat {
        val mNotifyManager = NotificationManagerCompat.from(context)
        if (hasOreo()) {
            mNotifyManager.createNotificationChannel(
                AppNotificationChannels.getDownloadChannel(
                    context
                )
            )
        }
        return mNotifyManager
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

    private val FAVE_GET_COUNT = 500
    private val PATTERN_WALL: Pattern = Pattern.compile("fenrir_wall_(-?\\d*)_aid_(-?\\d*)")

    @SuppressLint("CheckResult")
    private fun fetchInfo(id: Int, accountId: Int, log: StringBuilder) {
        log.append("###$accountId###$id\r\n")

        try {
            if (id >= 0) {
                ownersRepository.getFullUserInfo(
                    accountId,
                    id,
                    IOwnersRepository.MODE_NET
                ).blockingGet()
            } else {
                ownersRepository.getFullCommunityInfo(
                    accountId,
                    abs(id),
                    IOwnersRepository.MODE_NET
                ).blockingGet()
            }
        } catch (e: Exception) {
            log.append("+++++++++++++++FULL_OWNER_INFO++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------")
        }
        Thread.sleep(500)
        try {
            wallRepository.getWall(accountId, id, 0, 20, WallCriteria.MODE_ALL, true).blockingGet()
        } catch (e: Exception) {
            log.append("+++++++++++++++WALL++++++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
        Thread.sleep(500)
        if (Settings.get().other().isOwnerInChangesMonitor(id)) {
            if (id >= 0) {
                try {
                    relationshipInteractor.getActualFriendsList(
                        accountId,
                        id,
                        null,
                        0
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++ACTUAL_FRIENDS++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
                Thread.sleep(500)
                try {
                    relationshipInteractor.getFollowers(
                        accountId,
                        id,
                        1000,
                        0
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++FOLLOWERS++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
                Thread.sleep(500)
                try {
                    relationshipInteractor.getMutualFriends(
                        accountId,
                        id,
                        1000,
                        0
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++MUTUAL++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
                Thread.sleep(500)
                try {
                    communitiesInteractor.getActual(
                        accountId,
                        id,
                        1000,
                        0,
                        true
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++COMMUNITIES++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
            } else {
                try {
                    relationshipInteractor.getGroupMembers(
                        accountId,
                        abs(id),
                        0,
                        1000, null
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++MEMBERS++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
                Thread.sleep(500)
                try {
                    board.getActualTopics(
                        accountId,
                        id,
                        20,
                        0
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++TOPICS++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
                Thread.sleep(500)
                try {
                    faves.getOwnerPublishedArticles(
                        accountId,
                        id,
                        25,
                        0
                    ).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++ARTICLES++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------")
                }
                Thread.sleep(500)
                try {
                    docsInteractor.request(accountId, id, DocFilter.Type.ALL).blockingGet()
                } catch (e: Exception) {
                    log.append("+++++++++++++++DOCS++++++++++++++++++++++++++++\r\n")
                    log.append(
                        ErrorLocalizer.localizeThrowable(
                            applicationContext,
                            Utils.getCauseIfRuntime(e)
                        )
                    )
                    log.append("\r\n-----------------------------------------------\r\n")
                }
            }
        }
        Thread.sleep(500)
        try {
            photosInteractor.getActualAlbums(accountId, id, 50, 0).blockingGet()
        } catch (e: Exception) {
            log.append("+++++++++++++++PHOTO_ALBUMS++++++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
        Thread.sleep(500)
        try {
            photosInteractor[accountId, id, -7, 100, 0, !Settings.get()
                .other().isInvertPhotoRev].blockingGet()
        } catch (e: Exception) {
            log.append("+++++++++++++++PHOTO_FROM_WALL++++++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
        Thread.sleep(500)
        try {
            photosInteractor.getAll(
                accountId,
                id,
                1,
                1,
                0,
                100
            ).blockingGet()
        } catch (e: Exception) {
            log.append("+++++++++++++++PHOTO_ALL++++++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
        Thread.sleep(500)
        try {
            photosInteractor.getUsersPhoto(
                accountId,
                id,
                1,
                if (Settings.get().other().isInvertPhotoRev) 1 else 0,
                0,
                100
            ).blockingGet()
        } catch (_: Exception) {
        }
        Thread.sleep(500)
        try {
            videointeractor[accountId, id, -1, 50, 0].blockingGet()
        } catch (e: Exception) {
            log.append("+++++++++++++++VIDEOS-1++++++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
        Thread.sleep(500)
        try {
            videointeractor[accountId, id, -2, 50, 0].blockingGet()
        } catch (e: Exception) {
            log.append("+++++++++++++++VIDEOS-2++++++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGroupNotification() {
        if (!Utils.hasNougat()) {
            return
        }
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                ?: return
        val barNotifications = notificationManager.activeNotifications
        for (notification in barNotifications) {
            if (notification.id == NotificationHelper.NOTIFICATION_DOWNLOADING_GROUP) {
                return
            }
        }
        if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
            mNotifyManager.notify(
                NotificationHelper.NOTIFICATION_DOWNLOADING_GROUP,
                NotificationCompat.Builder(
                    applicationContext,
                    AppNotificationChannels.DOWNLOAD_CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.save)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setGroup("DOWNLOADING_OPERATION").setGroupSummary(true).build()
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun show_notification(
        notification: NotificationCompat.Builder,
        id: Int,
        cancel_id: Int?
    ) {
        if (cancel_id != null) {
            if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                mNotifyManager.cancel(getId().toString(), cancel_id)
            }
        }
        if (id == NotificationHelper.NOTIFICATION_DOWNLOAD) {
            createGroupNotification()
        }
        if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
            mNotifyManager.notify(getId().toString(), id, notification.build())
        }
    }

    @Suppress("DEPRECATION")
    override fun doWork(): Result {
        createForeground()

        var mBuilder = DownloadWorkUtils.createNotification(
            applicationContext,
            applicationContext.getString(R.string.sync),
            applicationContext.getString(R.string.bookmarks),
            R.drawable.save,
            false
        )

        show_notification(mBuilder, NotificationHelper.NOTIFICATION_DOWNLOADING, null)

        val log = StringBuilder()
        val accountId = Settings.get().accounts().current

        val favesList: ArrayList<FavePage> = ArrayList()
        val shortcutList: ArrayList<ShortcutStored> = ArrayList()
        var tmpOffset = 0

        while (true) {
            try {
                Thread.sleep(500)
                val pdg = faves.getPages(accountId, FAVE_GET_COUNT, tmpOffset, true).blockingGet()
                tmpOffset += FAVE_GET_COUNT
                favesList.addAll(pdg)
                if (Utils.safeCountOf(pdg) < FAVE_GET_COUNT) {
                    break
                }
            } catch (e: Exception) {
                log.append("+++++++++++++++FAVE_USERS++++++++++++++++++++++++++++\r\n")
                log.append(
                    ErrorLocalizer.localizeThrowable(
                        applicationContext,
                        Utils.getCauseIfRuntime(e)
                    )
                )
                log.append("\r\n-----------------------------------------------\r\n")
            }
        }
        tmpOffset = 0
        while (true) {
            try {
                Thread.sleep(500)
                val pdg = faves.getPages(accountId, FAVE_GET_COUNT, tmpOffset, false).blockingGet()
                tmpOffset += FAVE_GET_COUNT
                favesList.addAll(pdg)
                if (Utils.safeCountOf(pdg) < FAVE_GET_COUNT) {
                    break
                }
            } catch (e: Exception) {
                log.append("+++++++++++++++FAVE_GROUPS++++++++++++++++++++++++++++\r\n")
                log.append(
                    ErrorLocalizer.localizeThrowable(
                        applicationContext,
                        Utils.getCauseIfRuntime(e)
                    )
                )
                log.append("\r\n-----------------------------------------------\r\n")
            }
        }
        try {
            shortcutList.addAll(shortcuts.getShortcutAll().blockingGet())
        } catch (e: Exception) {
            log.append("+++++++++++++++SHORTCUT++++++++++++++++++++++++\r\n")
            log.append(
                ErrorLocalizer.localizeThrowable(
                    applicationContext,
                    Utils.getCauseIfRuntime(e)
                )
            )
            log.append("\r\n-----------------------------------------------\r\n")
        }
        val alls = (favesList.size + shortcutList.size).coerceAtLeast(1)
        var curr = 0

        for (i in favesList) {
            curr++
            val id = i.owner?.ownerId ?: continue
            fetchInfo(id, accountId, log)

            mBuilder.setProgress(
                100,
                (curr.toDouble() / alls * 100).toInt(),
                false
            )
            show_notification(
                mBuilder,
                NotificationHelper.NOTIFICATION_DOWNLOADING,
                null
            )
        }
        for (i in shortcutList) {
            curr++
            val matcher = PATTERN_WALL.matcher(i.action)
            var sid = 0
            var saccount_id = 0
            try {
                if (matcher.find()) {
                    sid = matcher.group(1)?.toInt() ?: continue
                    saccount_id = matcher.group(2)?.toInt() ?: continue
                }
            } catch (e: Exception) {
                log.append("+++++++++++++++REGEX_SHORTCUT++++++++++++++++++++++++\r\n")
                log.append(
                    ErrorLocalizer.localizeThrowable(
                        applicationContext,
                        Utils.getCauseIfRuntime(e)
                    )
                )
                log.append("\r\n-----------------------------------------------\r\n")
                continue
            }
            fetchInfo(sid, saccount_id, log)
            mBuilder.setProgress(
                100,
                (curr.toDouble() / alls * 100).toInt(),
                false
            )
            show_notification(
                mBuilder,
                NotificationHelper.NOTIFICATION_DOWNLOADING,
                null
            )
        }
        mBuilder = DownloadWorkUtils.createNotification(
            applicationContext,
            applicationContext.getString(R.string.sync),
            applicationContext.getString(R.string.success),
            R.drawable.save,
            true
        )
        mBuilder.color = ThemesController.toastColor(false)

        show_notification(
            mBuilder,
            NotificationHelper.NOTIFICATION_DOWNLOAD,
            NotificationHelper.NOTIFICATION_DOWNLOADING
        )
        Utils.inMainThread(object : Utils.SafeCallInt {
            override fun call() {
                CustomToast.createCustomToast(applicationContext)
                    .showToastBottom(R.string.success)
            }
        })
        try {
            val file = File(Environment.getExternalStorageDirectory(), "fenrir_fave_sync_log.txt")
            FileOutputStream(file).write(log.toString().toByteArray(Charsets.UTF_8))
            applicationContext.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)
                )
            )
        } catch (ignored: Exception) {
        }

        return Result.success()
    }
}