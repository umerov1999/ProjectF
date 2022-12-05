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

class FriendAcceptedFCMMessage {
    // collapseKey: friend_accepted, extras: Bundle[{first_name=Андрей, uid=320891480, from=376771982493,
    // type=friend_accepted, sandbox=0, collapse_key=friend_accepted, last_name=Боталов}]
    //private String first_name;
    //private String last_name;
    private var uid = 0

    @SuppressLint("CheckResult")
    fun notify(context: Context, accountId: Int) {
        if (!get()
                .notifications()
                .isFriendRequestAcceptationNotifEnabled
        ) {
            return
        }
        val app = context.applicationContext
        getRx(app, accountId, uid)
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
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getFriendRequestsChannel(context))
        }
        val builder = NotificationCompat.Builder(context, friendRequestsChannelId)
            .setSmallIcon(R.drawable.friends)
            .setLargeIcon(bitmap)
            .setContentTitle(user.fullName)
            .setContentText(context.getString(R.string.accepted_friend_request))
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val aid = get()
            .accounts()
            .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, getOwnerWallPlace(aid, uid, user))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            uid,
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(
                uid.toString(),
                NotificationHelper.NOTIFICATION_FRIEND_ACCEPTED_ID,
                notification
            )
        }
    }

    companion object {
        //private long from;
        //private String type;
        // FCM
        //key: image_type, value: user, class: class java.lang.String
        //key: from_id, value: 339247963, class: class java.lang.String
        //key: id, value: friend_339247963, class: class java.lang.String
        //key: url, value: https://vk.com/id339247963, class: class java.lang.String
        //key: icon, value: done_24, class: class java.lang.String
        //key: time, value: 1540738607, class: class java.lang.String
        //key: type, value: friend_accepted, class: class java.lang.String
        //key: badge, value: 69, class: class java.lang.String
        //key: image, value: [{"width":200,"url":"https:\/\/pp.userapi.com\/c844418\/v844418689\/110e79\/yMJ6_zsujQ8.jpg","height":200},{"width":100,"url":"https:\/\/pp.userapi.com\/c844418\/v844418689\/110e7a\/olNcuZZOXSU.jpg","height":100},{"width":50,"url":"https:\/\/pp.userapi.com\/c844418\/v844418689\/110e7b\/9yyASlXwnJs.jpg","height":50}], class: class java.lang.String
        //key: sound, value: 1, class: class java.lang.String
        //key: to_id, value: 25651989, class: class java.lang.String
        //key: group_id, value: friend_accepted, class: class java.lang.String
        //key: context, value: {"feedback":true,"user_id":339247963}, class: class java.lang.String
        fun fromRemoteMessage(remote: RemoteMessage): FriendAcceptedFCMMessage? {
            val message = FriendAcceptedFCMMessage()
            //message.first_name = bundle.getString("first_name");
            //message.last_name = bundle.getString("last_name");
            message.uid = remote.data["from_id"]?.toInt() ?: return null
            //message.from = FriendFCMMessage.optLong(bundle, "from");
            //message.type = bundle.getString("type");
            return message
        }
    }
}