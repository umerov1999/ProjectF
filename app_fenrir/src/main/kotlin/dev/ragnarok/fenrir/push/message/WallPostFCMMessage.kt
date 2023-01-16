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
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getNewPostChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.newPostChannelId
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.push.NotificationScheduler.INSTANCE
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class WallPostFCMMessage {
    //from_id=175895893, first_name=Руслан, from=376771982493, text=Тест push-уведомлений, type=wall_post, place=wall25651989_2509, collapse_key=wall_post, last_name=Колбаса
    private var from_id = 0L
    private var post_id = 0

    //public String first_name;
    //public String last_name;
    //public long from;
    private var body: String? = null

    //public String type;
    private var place: String? = null
    private var owner_id = 0L
    private var title: String? = null
    fun nofify(context: Context, accountId: Long) {
        if (accountId == owner_id) {
            notifyWallPost(context, accountId)
        } else {
            notifyNewPost(context, accountId)
        }
    }

    @SuppressLint("CheckResult")
    private fun notifyWallPost(context: Context, accountId: Long) {
        if (!get()
                .notifications()
                .isNewPostOnOwnWallNotifEnabled
        ) {
            return
        }
        val app = context.applicationContext
        getRx(app, accountId, from_id)
            .subscribeOn(INSTANCE)
            .subscribe({ ownerInfo: OwnerInfo ->
                notifyImpl(
                    app,
                    ownerInfo.owner,
                    ownerInfo.avatar
                )
            }, ignore())
    }

    @SuppressLint("CheckResult")
    private fun notifyNewPost(context: Context, accountId: Long) {
        if (!get()
                .notifications()
                .isNewPostsNotificationEnabled
        ) {
            return
        }
        val app = context.applicationContext
        getRx(app, accountId, owner_id)
            .subscribeOn(INSTANCE)
            .subscribe({ info: OwnerInfo ->
                val manager =
                    app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                if (hasOreo()) {
                    manager?.createNotificationChannel(getNewPostChannel(app))
                }
                val builder = NotificationCompat.Builder(app, newPostChannelId)
                    .setSmallIcon(R.drawable.client_round)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setLargeIcon(info.avatar)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                builder.priority = NotificationCompat.PRIORITY_HIGH
                val intent = Intent(app, MainActivity::class.java)
                intent.putExtra(Extra.PLACE, getPostPreviewPlace(accountId, post_id, owner_id))
                intent.action = MainActivity.ACTION_OPEN_PLACE
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                val contentIntent = PendingIntent.getActivity(
                    app,
                    owner_id.hashCode(),
                    intent,
                    makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                )
                builder.setContentIntent(contentIntent)
                val notification = builder.build()
                configOtherPushNotification(notification)
                if (AppPerms.hasNotificationPermissionSimple(context)) {
                    manager?.notify(
                        "new_post" + owner_id + "_" + post_id,
                        NotificationHelper.NOTIFICATION_NEW_POSTS_ID,
                        notification
                    )
                }
            }, ignore())
    }

    private fun notifyImpl(context: Context, owner: Owner, avatar: Bitmap?) {
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getNewPostChannel(context))
        }
        val builder = NotificationCompat.Builder(context, newPostChannelId)
            .setSmallIcon(R.drawable.pencil)
            .setLargeIcon(avatar)
            .setContentTitle(owner.fullName)
            .setContentText(context.getString(R.string.published_post_on_your_wall))
            .setSubText(body)
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val aid = get()
            .accounts()
            .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, getPostPreviewPlace(aid, post_id, owner_id))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            post_id,
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(place, NotificationHelper.NOTIFICATION_WALL_POST_ID, notification)
        }
    }

    @Serializable
    private class PushContext {
        @SerialName("item_id")
        var itemId = 0

        @SerialName("owner_id")
        var ownerId = 0L
    }

    companion object {
        /*2018-10-29 14:09:00.106 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: onMessage, from: 237327763482, pushType: post, data: {image_type=user, from_id=175895893, id=wall_post_25651989_4099, url=https://vk.com/wall25651989_4099, body=Руслан Колбаса: Дарова!, icon=write_24, time=1540814940, type=post, category=wall_posts, badge=64, image=[{"width":200,"url":"https:\/\/pp.userapi.com\/c626917\/v626917893\/f230\/KnzJyeBQr30.jpg","height":200},{"width":100,"url":"https:\/\/pp.userapi.com\/c626917\/v626917893\/f232\/A7dV0Aj_zHE.jpg","height":100},{"width":50,"url":"https:\/\/pp.userapi.com\/c626917\/v626917893\/f233\/wThqed0he9s.jpg","height":50}], sound=1, title=Новая запись на стене, to_id=25651989, group_id=posts, context={"feedback":true,"item_id":"4099","owner_id":"25651989","type":"post"}}
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: image_type, value: user, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: from_id, value: 175895893, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: id, value: wall_post_25651989_4099, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: url, value: https://vk.com/wall25651989_4099, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: body, value: Руслан Колбаса: Дарова!, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: icon, value: write_24, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: time, value: 1540814940, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: type, value: post, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: category, value: wall_posts, class: class java.lang.String
        2018-10-29 14:09:00.107 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: badge, value: 64, class: class java.lang.String
        2018-10-29 14:09:00.108 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: image, value: [{"width":200,"url":"https:\/\/pp.userapi.com\/c626917\/v626917893\/f230\/KnzJyeBQr30.jpg","height":200},{"width":100,"url":"https:\/\/pp.userapi.com\/c626917\/v626917893\/f232\/A7dV0Aj_zHE.jpg","height":100},{"width":50,"url":"https:\/\/pp.userapi.com\/c626917\/v626917893\/f233\/wThqed0he9s.jpg","height":50}], class: class java.lang.String
        2018-10-29 14:09:00.108 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: sound, value: 1, class: class java.lang.String
        2018-10-29 14:09:00.108 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: title, value: Новая запись на стене, class: class java.lang.String
        2018-10-29 14:09:00.108 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: to_id, value: 25651989, class: class java.lang.String
        2018-10-29 14:09:00.108 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: group_id, value: posts, class: class java.lang.String
        2018-10-29 14:09:00.108 18518-18893/dev.ragnarok.fenrir D/FcmListenerService: key: context, value: {"feedback":true,"item_id":"4099","owner_id":"25651989","type":"post"}, class: class java.lang.String
  */
        fun fromRemoteMessage(remote: RemoteMessage): WallPostFCMMessage? {
            val message = WallPostFCMMessage()
            val data = remote.data
            message.from_id = remote.data["from_id"]?.toLong() ?: return null
            message.body = data["body"]
            message.place = data["url"]
            message.title = data["title"]
            val context: PushContext =
                kJson.decodeFromString(PushContext.serializer(), data["context"] ?: return null)
            message.post_id = context.itemId
            message.owner_id = context.ownerId
            return message
        }
    }
}