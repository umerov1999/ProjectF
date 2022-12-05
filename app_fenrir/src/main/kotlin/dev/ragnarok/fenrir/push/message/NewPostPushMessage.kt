package dev.ragnarok.fenrir.push.message

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getNewPostChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.newPostChannelId
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.push.NotificationScheduler.INSTANCE
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.settings.theme.ThemesController.toastColor
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Logger.wtf
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import java.util.regex.Pattern

class NewPostPushMessage {
    private var accountId = 0
    private var url: String? = null
    private var body: String? = null
    private var title: String? = null

    @SuppressLint("CheckResult")
    fun notifyIfNeed(context: Context) {
        val pUrl = url
        if (pUrl == null) {
            wtf("NewPostPushMessage", "url is NULL!!!")
            return
        }
        if (!get()
                .notifications()
                .isNewPostsNotificationEnabled
        ) {
            return
        }
        val matcher = PATTERN_WALL_POST.matcher(pUrl)
        if (matcher.find()) {
            matcher.group(1)?.let {
                val app = context.applicationContext
                getRx(app, accountId, it.toInt())
                    .subscribeOn(INSTANCE)
                    .subscribe({ ownerInfo: OwnerInfo ->
                        notifyImpl(
                            app,
                            ownerInfo.avatar
                        )
                    }) { notifyImpl(app, null) }
            } ?: notifyImpl(context, null)
        } else {
            notifyImpl(context, null)
        }
    }

    private fun notifyImpl(context: Context, bitmap: Bitmap?) {
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasOreo()) {
            nManager.createNotificationChannel(getNewPostChannel(context))
        }
        val builder = NotificationCompat.Builder(context, newPostChannelId)
            .setSmallIcon(R.drawable.client_round)
            .setContentTitle(title)
            .setContentText(body)
            .setColor(toastColor(false))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val intent = Intent(context, MainActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(url)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            url.hashCode(),
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager.notify(url, NotificationHelper.NOTIFICATION_NEW_POSTS_ID, notification)
        }
    }

    companion object {
        private val PATTERN_WALL_POST =
            Pattern.compile("vk.com/(?:[\\w.\\d]+\\?(?:[\\w=&]+)?w=)?wall(-?\\d*)_(\\d*)")

        fun fromRemoteMessage(accountId: Int, remote: RemoteMessage): NewPostPushMessage {
            val message = NewPostPushMessage()
            message.accountId = accountId
            val data = remote.data
            message.url = data["url"]
            message.body = data["body"]
            message.title = data["title"]
            return message
        }
    }
}