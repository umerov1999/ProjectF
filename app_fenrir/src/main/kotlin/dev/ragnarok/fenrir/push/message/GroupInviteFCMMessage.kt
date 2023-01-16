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
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getGroupInvitesChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.groupInvitesChannelId
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Community
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
import io.reactivex.rxjava3.core.Single
import kotlin.math.abs

class GroupInviteFCMMessage {
    //collapseKey: group_invite, extras: Bundle[{from_id=175895893, from=376771982493, name=Pianoбой,
    // type=group_invite, group_id=1583008, sandbox=0, collapse_key=group_invite}]
    private var from_id = 0L

    //public long from;
    //public String name;
    //public String type;
    private var group_id = 0L

    @SuppressLint("CheckResult")
    fun notify(context: Context, accountId: Long) {
        if (!get()
                .notifications()
                .isGroupInvitedNotifEnabled
        ) {
            return
        }
        val app = context.applicationContext
        val group = getRx(app, accountId, -abs(group_id))
        val user = getRx(app, accountId, from_id)
        Single.zip(
            group,
            user
        ) { first: OwnerInfo, second: OwnerInfo ->
            Pair(
                first,
                second
            )
        }
            .subscribeOn(INSTANCE)
            .subscribe({
                val userInfo = it.second
                val groupInfo = it.first
                notifyImpl(app, userInfo.user, groupInfo.avatar, groupInfo.community)
            }) { }
    }

    private fun notifyImpl(
        context: Context,
        user: User,
        groupBitmap: Bitmap?,
        community: Community
    ) {
        val contentText = context.getString(R.string.invites_you_to_join_community, user.fullName)
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getGroupInvitesChannel(context))
        }
        val builder = NotificationCompat.Builder(context, groupInvitesChannelId)
            .setSmallIcon(R.drawable.groups)
            .setLargeIcon(groupBitmap)
            .setContentTitle(community.fullName)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val aid = get()
            .accounts()
            .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, getOwnerWallPlace(aid, community))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            group_id.hashCode(),
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(
                group_id.toString(),
                NotificationHelper.NOTIFICATION_GROUP_INVITE_ID,
                notification
            )
        }
    }

    companion object {
        fun fromRemoteMessage(remote: RemoteMessage): GroupInviteFCMMessage? {
            val message = GroupInviteFCMMessage()
            message.from_id = remote.data["from_id"]?.toLong() ?: return null
            //message.name = bundle.getString("name");
            message.group_id = remote.data["group_id"]?.toLong() ?: return null
            //message.from = FriendFCMMessage.optLong(bundle, "from");
            //message.type = bundle.getString("type");
            return message
        }
    }
}