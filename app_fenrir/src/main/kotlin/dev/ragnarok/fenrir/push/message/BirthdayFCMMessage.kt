package dev.ragnarok.fenrir.push.message

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.birthdaysChannelId
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getBirthdaysChannel
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.settings.theme.ThemesController.toastColor
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BirthdayFCMMessage {
    private var user_id = 0L
    private var body: String? = null
    private var title: String? = null
    fun notify(context: Context, account_id: Long) {
        if (!get()
                .notifications()
                .isBirthdayNotifyEnabled
        ) {
            return
        }
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getBirthdaysChannel(context))
        }
        val builder = NotificationCompat.Builder(context, birthdaysChannelId)
            .setSmallIcon(R.drawable.cake)
            .setContentTitle(title)
            .setContentText(body)
            .setColor(toastColor(false))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, getOwnerWallPlace(account_id, user_id, null))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            user_id.hashCode(),
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(
                user_id.toString(),
                NotificationHelper.NOTIFICATION_BIRTHDAY,
                notification
            )
        }
    }

    @Serializable
    private class BirthdayContext {
        @SerialName("user_id")
        var user_id = 0L
    }

    companion object {
        fun fromRemoteMessage(remote: RemoteMessage): BirthdayFCMMessage? {
            val message = BirthdayFCMMessage()
            val data = remote.data
            val context: BirthdayContext =
                kJson.decodeFromString(BirthdayContext.serializer(), data["context"] ?: return null)
            message.user_id = context.user_id
            if (message.user_id == 0L) {
                message.user_id = data["from_id"]?.toLong().orZero()
                if (message.user_id == 0L) {
                    return null
                }
            }
            message.body = data["body"]
            message.title = data["title"]
            return message
        }
    }
}