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
import dev.ragnarok.fenrir.link.VkLinkParser.parse
import dev.ragnarok.fenrir.link.types.WallPostLink
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getNewPostChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.newPostChannelId
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.push.NotificationScheduler.INSTANCE
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import kotlin.math.abs

class WallPublishFCMMessage {
    // collapseKey: wall_publish, extras: Bundle[{from=376771982493, name=Fenrir for VK,
    // text=Тестирование уведомлений, type=wall_publish, place=wall-72124992_4914,
    // group_id=72124992, sandbox=0, collapse_key=wall_publish}]
    //public long from;
    //public String name;
    private var text: String? = null

    //public String type;
    private var place: String? = null
    private var group_id = 0

    @SuppressLint("CheckResult")
    fun notify(context: Context, accountId: Int) {
        if (!get()
                .notifications()
                .isWallPublishNotifEnabled
        ) {
            return
        }
        val app = context.applicationContext
        getRx(app, accountId, -abs(group_id))
            .subscribeOn(INSTANCE)
            .subscribe({ ownerInfo: OwnerInfo ->
                notifyImpl(
                    app,
                    ownerInfo.community,
                    ownerInfo.avatar
                )
            }) { }
    }

    private fun notifyImpl(context: Context, community: Community, bitmap: Bitmap?) {
        val url = "vk.com/$place"
        val link = parse(url)
        if (link !is WallPostLink) {
            logThrowable("Push issues", Exception("Unknown place: $place"))
            return
        }
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getNewPostChannel(context))
        }
        val builder = NotificationCompat.Builder(context, newPostChannelId)
            .setSmallIcon(R.drawable.pencil)
            .setLargeIcon(bitmap)
            .setContentTitle(community.fullName)
            .setContentText(context.getString(R.string.postings_you_the_news))
            .setSubText(text)
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val aid = get()
            .accounts()
            .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(
            Extra.PLACE,
            getPostPreviewPlace(aid, link.postId, link.ownerId)
        )
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            link.postId,
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(place, NotificationHelper.NOTIFICATION_WALL_PUBLISH_ID, notification)
        }
    }

    companion object {
        fun fromRemoteMessage(remote: RemoteMessage): WallPublishFCMMessage? {
            val message = WallPublishFCMMessage()
            //message.name = bundle.getString("name");
            //message.from = optLong(bundle, "from");
            message.group_id = remote.data["group_id"]?.toInt() ?: return null
            message.text = remote.data["text"]
            //message.type = bundle.getString("type");
            message.place = remote.data["place"]
            return message
        }
    }
}