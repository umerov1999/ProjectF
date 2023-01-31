package dev.ragnarok.fenrir.push.message

import android.annotation.SuppressLint
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
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.commentsChannelId
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getCommentsChannel
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.push.NotificationScheduler.INSTANCE
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CommentFCMMessage {
    /**
     * Идентификатор пользователя
     */
    private var from_id = 0L

    /**
     * Идентификатор комментария
     */
    private var reply_id = 0

    //private int sex;
    //public long from;
    private var text: String? = null
    private var type: String? = null
    private var item_id = 0
    private var owner_id = 0L

    @SuppressLint("CheckResult")
    fun notify(context: Context, accountId: Long) {
        if (!get()
                .notifications()
                .isCommentsNotificationsEnabled
        ) {
            return
        }
        val app = context.applicationContext
        getRx(context, accountId, from_id)
            .subscribeOn(INSTANCE)
            .subscribe({ ownerInfo: OwnerInfo -> notifyImpl(app, ownerInfo) }, RxUtils.ignore())
    }

    private fun notifyImpl(context: Context, ownerInfo: OwnerInfo) {
        var commented: Commented? = null
        var title: String? = null
        when (type) {
            "photo_comment" -> {
                title = context.getString(R.string.photo_comment_push_title)
                commented = Commented(item_id, owner_id, CommentedType.PHOTO, null)
            }

            "video_comment" -> {
                title = context.getString(R.string.video_comment_push_title)
                commented = Commented(item_id, owner_id, CommentedType.VIDEO, null)
            }

            "comment" -> {
                title = context.getString(R.string.wall_comment_push_title)
                commented = Commented(item_id, owner_id, CommentedType.POST, null)
            }
        }
        if (commented == null) {
            return
        }
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getCommentsChannel(context))
        }
        val builder = NotificationCompat.Builder(context, commentsChannelId)
            .setSmallIcon(R.drawable.comment_thread)
            .setLargeIcon(ownerInfo.avatar)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val aid = get()
            .accounts()
            .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, getCommentsPlace(aid, commented, reply_id))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            reply_id,
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        val tag = type + item_id + "_" + owner_id
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(tag, NotificationHelper.NOTIFICATION_COMMENT_ID, notification)
        }
    }

    @Serializable
    private class PushContext {
        @SerialName("reply_id")
        var reply_id = 0

        @SerialName("item_id")
        var item_id = 0

        @SerialName("owner_id")
        var owner_id = 0L

        @SerialName("type")
        var type: String? = null
    }

    companion object {
        //extras: Bundle[{google.sent_time=1477925617791, from_id=175895893, reply_id=3686, sex=2,
        // text=да, type=comment, place=wall25651989_3499, google.message_id=0:1477925617795994%8c76e97a38a5ee5f, _genSrv=833239, sandbox=0, collapse_key=comment}]
        fun fromRemoteMessage(remote: RemoteMessage): CommentFCMMessage? {
            val message = CommentFCMMessage()
            message.from_id = remote.data["from_id"]?.toLong() ?: return null
            message.text = remote.data["body"]
            val context: PushContext = kJson.decodeFromString(
                PushContext.serializer(),
                remote.data["context"] ?: return null
            )
            message.reply_id = context.reply_id
            message.type = context.type
            message.item_id = context.item_id
            message.owner_id = context.owner_id
            return message
        }
    }
}