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
import dev.ragnarok.fenrir.link.LinkHelper.findCommentedFrom
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory.withSpans
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.commentsChannelId
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getCommentsChannel
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.push.NotificationScheduler.INSTANCE
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Logger.e
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent

class ReplyFCMMessage {
    //04-14 13:02:31.114 1784-2485/dev.ragnarok.fenrir D/MyFcmListenerService: onMessage,
    // from: 652332232777, collapseKey: null, data: {image_type=user, from_id=280186075,
    // id=reply_280186075_60, url=https://vk.com/wall280186075_56?reply=60, body=Yevgeni Polkin: Emin, test,
    // icon=reply_24, time=1523700152, type=comment, badge=1,
    // image=[{"width":200,"url":"https:\/\/pp.userapi.com\/c837424\/v837424529\/5c2cb\/OkkyraBZJCY.jpg","height":200},
    // {"width":100,"url":"https:\/\/pp.userapi.com\/c837424\/v837424529\/5c2cc\/dRPyhRW_dvU.jpg","height":100},
    // {"width":50,"url":"https:\/\/pp.userapi.com\/c837424\/v837424529\/5c2cd\/BB6tk_bcJ3U.jpg","height":50}],
    // sound=1, title=Reply to your comment, to_id=216143660, group_id=reply,
    // context={"feedback":true,"reply_id":60,"user_id":280186075,"item_id":56,"owner_id":"280186075","type":"comment"}}
    private var from_id = 0L
    private var reply_id = 0

    //public String firstName;
    //private int sex;
    //public long from;
    private var text: String? = null
    private var place: String? = null

    @SuppressLint("CheckResult")
    fun notify(context: Context, accountId: Long) {
        if (!get()
                .notifications()
                .isReplyNotifEnabled
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
            }) { }
    }

    private fun notifyImpl(context: Context, owner: Owner, bitmap: Bitmap?) {
        val url = "vk.com/$place"
        val commented = findCommentedFrom(url)
        if (commented == null) {
            e(TAG, "Unknown place: $place")
            return
        }
        val snannedText = withSpans(text, owners = true, topics = false, listener = null)
        val targetText = snannedText?.toString()
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getCommentsChannel(context))
        }
        val builder = NotificationCompat.Builder(context, commentsChannelId)
            .setSmallIcon(R.drawable.channel)
            .setLargeIcon(bitmap)
            .setContentTitle(owner.fullName)
            .setContentText(targetText)
            .setSubText(context.getString(R.string.in_reply_to_your_comment))
            .setStyle(NotificationCompat.BigTextStyle().bigText(targetText))
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
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(place, NotificationHelper.NOTIFICATION_REPLY_ID, notification)
        }
    }

    companion object {
        private val TAG = ReplyFCMMessage::class.java.simpleName

        //public String lastName;
        //private String type;
        fun fromRemoteMessage(remote: RemoteMessage): ReplyFCMMessage? {
            val message = ReplyFCMMessage()
            message.from_id = remote.data["from_id"]?.toLong() ?: return null
            message.reply_id = remote.data["reply_id"]?.toInt() ?: return null
            //message.sex = optInt(bundle, "sex");
            //message.firstName = bundle.getString("first_name");
            //message.lastName = bundle.getString("last_name");
            //message.from = optLong(bundle, "from");
            message.text = remote.data["text"]
            //message.type = bundle.getString("type");
            message.place = remote.data["place"]
            return message
        }
    }
}