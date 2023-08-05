package dev.ragnarok.fenrir.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.longpoll.ILongpollManager
import dev.ragnarok.fenrir.longpoll.LongpollInstance
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class KeepLongpollService : Service() {
    private val compositeDisposable = CompositeDisposable()
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
        compositeDisposable.add(
            longpollManager.observeKeepAlive()
                .observeOn(provideMainThreadScheduler())
                .subscribe({ sendKeepAlive() }, ignore())
        )
        compositeDisposable.add(
            Settings.get().accounts()
                .observeChanges
                .observeOn(provideMainThreadScheduler())
                .subscribe({ sendKeepAlive() }, ignore())
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
        compositeDisposable.dispose()
        cancelNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @Suppress("DEPRECATION")
    private fun startWithNotification() {
        val notificationIntent = Intent(this, KeepLongpollService::class.java)
        notificationIntent.action = ACTION_STOP
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent =
            PendingIntent.getService(this, 0, notificationIntent, makeMutablePendingIntent(0))
        val builder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    KEEP_LONGPOLL_CHANNEL,
                    getString(R.string.channel_keep_longpoll),
                    NotificationManager.IMPORTANCE_NONE
                )
                val nManager =
                    NotificationManagerCompat.from(this)
                nManager.createNotificationChannel(channel)
                NotificationCompat.Builder(this, channel.id)
            } else {
                NotificationCompat.Builder(
                    this,
                    KEEP_LONGPOLL_CHANNEL
                ).setPriority(Notification.PRIORITY_MIN)
            }
        val action_stop = NotificationCompat.Action.Builder(
            R.drawable.ic_arrow_down,
            getString(R.string.stop_action), pendingIntent
        )
            .build()
        builder.setContentTitle(getString(R.string.keep_longpoll_notification_title))
            .setContentText(getString(R.string.may_down_charge))
            .setSmallIcon(R.drawable.client_round)
            .addAction(action_stop)
            .setColor(Color.parseColor("#dd0000"))
            .setOngoing(true)
            .build()
        val War = NotificationCompat.WearableExtender()
        War.addAction(action_stop)
        War.startScrollBottom = true
        builder.extend(War)
        if (Utils.hasUpsideDownCake()) {
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
                context.stopService(Intent(context, KeepLongpollService::class.java))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}