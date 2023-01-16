package dev.ragnarok.fenrir.push.message

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.friendRequestsChannelId
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getFriendRequestsChannel
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.push.NotificationScheduler.INSTANCE
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent

class FriendFCMMessage {
    //collapseKey: friend, extras: Bundle[{first_name=Андрей, uid=320891480, from=376771982493,
    // type=friend, badge=1, common_count=0, sandbox=0, collapse_key=friend, last_name=Боталов}]
    //private String first_name;
    //private String last_name;
    private var from_id = 0L

    @SuppressLint("CheckResult")
    fun notify(context: Context, accountId: Long) {
        if (!get()
                .notifications()
                .isNewFollowerNotifEnabled
        ) {
            return
        }
        val app = context.applicationContext
        getRx(app, accountId, from_id)
            .subscribeOn(INSTANCE)
            .subscribe({ ownerInfo: OwnerInfo ->
                notifyImpl(
                    app,
                    ownerInfo.user,
                    ownerInfo.avatar
                )
            }) { }
    }

    private fun notifyImpl(context: Context, user: User, bitmap: Bitmap?) {
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasOreo()) {
            nManager.createNotificationChannel(getFriendRequestsChannel(context))
        }
        val builder = NotificationCompat.Builder(context, friendRequestsChannelId)
            .setSmallIcon(R.drawable.friends)
            .setLargeIcon(bitmap)
            .setContentTitle(user.fullName)
            .setContentText(context.getString(R.string.subscribed_to_your_updates))
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val aid = get()
            .accounts()
            .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, getOwnerWallPlace(aid, from_id, user))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            from_id.hashCode(),
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager.notify(
                from_id.toString(),
                NotificationHelper.NOTIFICATION_FRIEND_ID,
                notification
            )
        }
    }

    companion object {
        //private long from;
        //private String type;
        //private int badge;
        //private int common_count;
        fun fromRemoteMessage(remote: RemoteMessage): FriendFCMMessage? {
            val message = FriendFCMMessage()
            //message.first_name = bundle.getString("first_name");
            //message.last_name = bundle.getString("last_name");
            message.from_id = remote.data["from_id"]?.toLong() ?: return null
            //message.from = optLong(bundle, "from");
            //message.type = bundle.getString("type");
            //message.badge = optInt(bundle, "badge");
            //message.common_count = optInt(bundle, "common_count");
            return message
        }
    }
}