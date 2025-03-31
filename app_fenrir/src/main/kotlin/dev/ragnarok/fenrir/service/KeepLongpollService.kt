package dev.ragnarok.fenrir.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.longpoll.ILongpollManager
import dev.ragnarok.fenrir.longpoll.LongpollInstance
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain

class KeepLongpollService : Service() {
    private val compositeJob = CompositeJob()
    private lateinit var longpollManager: ILongpollManager
    private var wakeLock: PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager?
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
            wakeLock?.setReferenceCounted(false)
        }
        startWithNotification()
        longpollManager = LongpollInstance.longpollManager
        sendKeepAlive()
        compositeJob.add(
            longpollManager.observeKeepAlive()
                .sharedFlowToMain { sendKeepAlive() }
        )
        compositeJob.add(
            Settings.get().accounts()
                .observeChanges
                .sharedFlowToMain { sendKeepAlive() }
        )
    }

    private fun sendKeepAlive() {
        val accountId = Settings.get().accounts().current
        if (accountId != ISettings.IAccountsSettings.INVALID_ID) {
            longpollManager.keepAlive(accountId)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP == intent.action) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun cancelNotification() {
        if (AppPerms.hasNotificationPermissionSimple(this)) {
            NotificationManagerCompat.from(this).cancel(FOREGROUND_SERVICE)
        }
    }

    override fun onDestroy() {
        wakeLock?.release()
        compositeJob.cancel()
        cancelNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startWithNotification() {
        val notificationIntent = Intent(this, KeepLongpollService::class.java)
        notificationIntent.action = ACTION_STOP
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent =
            PendingIntent.getService(this, 0, notificationIntent, makeMutablePendingIntent(0))

        val channel = NotificationChannel(
            KEEP_LONGPOLL_CHANNEL,
            getString(R.string.channel_keep_longpoll),
            NotificationManager.IMPORTANCE_NONE
        )
        val nManager =
            NotificationManagerCompat.from(this)
        nManager.createNotificationChannel(channel)
        val builder = NotificationCompat.Builder(this, channel.id)
        val action_stop = NotificationCompat.Action.Builder(
            R.drawable.ic_arrow_down,
            getString(R.string.stop_action), pendingIntent
        )
            .build()
        builder.setContentTitle(getString(R.string.keep_longpoll_notification_title))
            .setContentText(getString(R.string.may_down_charge))
            .setSmallIcon(R.drawable.client_round)
            .addAction(action_stop)
            .setColor("#dd0000".toColor())
            .setOngoing(true)
            .build()
        val War = NotificationCompat.WearableExtender()
        War.addAction(action_stop)
        War.startScrollBottom = true
        builder.extend(War)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_SERVICE,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_SERVICE, builder.build())
        }
    }

    companion object {
        private const val ACTION_STOP = "KeepLongpollService.ACTION_STOP"
        private const val KEEP_LONGPOLL_CHANNEL = "keep_longpoll"
        private const val FOREGROUND_SERVICE = 120
        fun start(context: Context) {
            try {
                context.startService(Intent(context, KeepLongpollService::class.java))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, KeepLongpollService::class.java)
                context.stopService(intent)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}