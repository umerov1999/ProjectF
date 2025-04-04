package dev.ragnarok.fenrir.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.domain.IBoardInteractor
import dev.ragnarok.fenrir.domain.ICommunitiesInteractor
import dev.ragnarok.fenrir.domain.IDocsInteractor
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.ShortcutStored
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.inMainThread
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingle
import dev.ragnarok.fenrir.util.toast.CustomToast
import java.io.File
import java.io.FileOutputStream
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
        mNotifyManager.createNotificationChannel(
            AppNotificationChannels.getDownloadChannel(
                context
            )
        )
        return mNotifyManager
    }

    private fun createForeground() {
        val channel = NotificationChannel(
            "worker_channel",
            applicationContext.getString(R.string.channel_keep_work_manager),
            NotificationManager.IMPORTANCE_NONE
        )
        mNotifyManager.createNotificationChannel(channel)
        val builder = NotificationCompat.Builder(applicationContext, channel.id)
        builder.setContentTitle(applicationContext.getString(R.string.work_manager))
            .setContentText(applicationContext.getString(R.string.foreground_downloader))
            .setSmallIcon(R.drawable.web)
            .setColor("#dd0000".toColor())
            .setOngoing(true)
        setForegroundAsync(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NotificationHelper.NOTIFICATION_DOWNLOAD_MANAGER,
                    builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(
                    NotificationHelper.NOTIFICATION_DOWNLOAD_MANAGER,
                    builder.build()
                )
            }
        )
    }

    private val FAVE_GET_COUNT = 500
    private val PATTERN_WALL: Pattern = Pattern.compile("fenrir_wall_(-?\\d*)_aid_(-?\\d*)")

    @SuppressLint("CheckResult")
    private fun fetchInfo(id: Long, accountId: Long, log: StringBuilder) {
        log.append("###$accountId###$id\r\n")

        try {
            if (id >= 0) {
                ownersRepository.getFullUserInfo(
                    accountId,
                    id,
                    IOwnersRepository.MODE_NET
                ).syncSingle()
            } else {
                ownersRepository.getFullCommunityInfo(
                    accountId,
                    abs(id),
                    IOwnersRepository.MODE_NET
                ).syncSingle()
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
            wallRepository.getWall(accountId, id, 0, 20, WallCriteria.MODE_ALL, true).syncSingle()
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
        if (Settings.get().main().isOwnerInChangesMonitor(id)) {
            if (id >= 0) {
                try {
                    relationshipInteractor.getActualFriendsList(
                        accountId,
                        id,
                        null,
                        0
                    ).syncSingle()
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
                    ).syncSingle()
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
                    ).syncSingle()
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
                    ).syncSingle()
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
                    ).syncSingle()
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
                    ).syncSingle()
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
                    ).syncSingle()
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
                    docsInteractor.request(accountId, id, DocFilter.Type.ALL).syncSingle()
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
            photosInteractor.getActualAlbums(accountId, id, 50, 0).syncSingle()
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
                .main().isInvertPhotoRev].syncSingle()
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
            ).syncSingle()
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
                if (Settings.get().main().isInvertPhotoRev) 1 else 0,
                0,
                100
            ).syncSingle()
        } catch (_: Exception) {
        }
        Thread.sleep(500)
        try {
            videointeractor[accountId, id, -1, 50, 0].syncSingle()
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
            videointeractor[accountId, id, -2, 50, 0].syncSingle()
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
                val pdg = faves.getPages(accountId, FAVE_GET_COUNT, tmpOffset, true).syncSingle()
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
                val pdg = faves.getPages(accountId, FAVE_GET_COUNT, tmpOffset, false).syncSingle()
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
            shortcutList.addAll(shortcuts.getShortcutAll().syncSingle())
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
            mBuilder.setContentTitle(
                applicationContext.getString(R.string.sync) + " " + (curr.toDouble() / alls * 100).toInt()
                    .toString() + "%"
            )
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
            var sid = 0L
            var saccount_id = 0L
            try {
                if (matcher.find()) {
                    sid = matcher.group(1)?.toLong() ?: continue
                    saccount_id = matcher.group(2)?.toLong() ?: continue
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
            mBuilder.setContentTitle(
                applicationContext.getString(R.string.sync) + " " + (curr.toDouble() / alls * 100).toInt()
                    .toString() + "%"
            )
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
        inMainThread {
            CustomToast.createCustomToast(applicationContext)
                .showToastBottom(R.string.success)
        }
        try {
            val file = File(Environment.getExternalStorageDirectory(), "fenrir_fave_sync_log.txt")
            FileOutputStream(file).write(log.toString().toByteArray(Charsets.UTF_8))
            applicationContext.sendBroadcast(
                @Suppress("deprecation")
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)
                )
            )
        } catch (_: Exception) {
        }

        return Result.success()
    }
}