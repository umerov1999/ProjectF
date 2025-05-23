package dev.ragnarok.fenrir.longpoll

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.Settings

object AppNotificationChannels {
    const val KEY_EXCHANGE_CHANNEL_ID = "key_exchange_channel"
    const val AUDIO_CHANNEL_ID = "audio_channel"
    const val DOWNLOAD_CHANNEL_ID = "download_channel"

    private fun getFeedbackRingtoneUri(context: Context): Uri {
        return ("android.resource://" + context.packageName + "/" + R.raw.feedback_sound).toUri()
    }

    private fun getNewPostRingtoneUri(context: Context): Uri {
        return ("android.resource://" + context.packageName + "/" + R.raw.new_post_sound).toUri()
    }

    private fun getNotificationRingtone(context: Context): Uri {
        return ("android.resource://" + context.packageName + "/" + R.raw.notification_sound).toUri()
    }

    private val ATTRIBUTES = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

    val chatMessageChannelId: String
        get() = makeChannelId("chat_message_channel")

    val groupChatMessageChannelId: String
        get() = makeChannelId("group_chat_message_channel")

    val likesChannelId: String
        get() = makeChannelId("likes_channel")

    val commentsChannelId: String
        get() = makeChannelId("comments_channel")

    val newPostChannelId: String
        get() = makeChannelId("new_post_channel")

    val mentionChannelId: String
        get() = makeChannelId("mention_channel")

    val groupInvitesChannelId: String
        get() = makeChannelId("group_invites_channel")

    val friendRequestsChannelId: String
        get() = makeChannelId("friend_requests_channel")

    val birthdaysChannelId: String
        get() = makeChannelId("birthdays_channel")

    fun getChatMessageChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.message_channel)
        val channel = NotificationChannel(
            chatMessageChannelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(getNotificationRingtone(context), ATTRIBUTES)
        channel.enableLights(true)
        channel.enableVibration(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            channel.setAllowBubbles(true)
        }
        return channel
    }

    fun getGroupChatMessageChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.group_message_channel)
        val channel = NotificationChannel(
            groupChatMessageChannelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(getNotificationRingtone(context), ATTRIBUTES)
        channel.enableLights(true)
        channel.enableVibration(true)
        return channel
    }

    fun getKeyExchangeChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.key_exchange_channel)
        val channel = NotificationChannel(
            KEY_EXCHANGE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.enableLights(false)
        channel.enableVibration(false)
        return channel
    }

    fun getLikesChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.likes_channel)
        val channel =
            NotificationChannel(likesChannelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.enableLights(true)
        channel.setSound(getFeedbackRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    fun getAudioChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.audio_channel)
        val channel =
            NotificationChannel(AUDIO_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.enableLights(false)
        channel.enableVibration(false)
        return channel
    }

    fun getCommentsChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.comment_channel)
        val channel =
            NotificationChannel(commentsChannelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(getFeedbackRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    fun getNewPostChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.new_posts_channel)
        val channel =
            NotificationChannel(newPostChannelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(getNewPostRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    fun getMentionChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.mentions)
        val channel =
            NotificationChannel(mentionChannelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(getFeedbackRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    fun getDownloadChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.downloading)
        val channel = NotificationChannel(
            DOWNLOAD_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.enableLights(true)
        channel.enableVibration(false)
        return channel
    }

    fun getGroupInvitesChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.group_invites_channel)
        val channel = NotificationChannel(
            groupInvitesChannelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(getFeedbackRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    fun getFriendRequestsChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.friend_requests_channel)
        val channel = NotificationChannel(
            friendRequestsChannelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(getFeedbackRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    fun getBirthdaysChannel(context: Context): NotificationChannel {
        val channelName = context.getString(R.string.birthdays)
        val channel = NotificationChannel(
            birthdaysChannelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setSound(getFeedbackRingtoneUri(context), ATTRIBUTES)
        return channel
    }

    private fun makeChannelId(id: String): String {
        val ch = Settings.get().main().customChannelNotif
        return if (ch == 0) {
            id
        } else id + "_" + ch
    }

    fun invalidateSoundChannels(context: Context) {
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        Settings.get().main().customChannelNotif
        nManager?.deleteNotificationChannel(chatMessageChannelId)
        nManager?.deleteNotificationChannel(groupChatMessageChannelId)
        nManager?.deleteNotificationChannel(likesChannelId)
        nManager?.deleteNotificationChannel(commentsChannelId)
        nManager?.deleteNotificationChannel(newPostChannelId)
        nManager?.deleteNotificationChannel(mentionChannelId)
        nManager?.deleteNotificationChannel(groupInvitesChannelId)
        nManager?.deleteNotificationChannel(friendRequestsChannelId)
        nManager?.deleteNotificationChannel(birthdaysChannelId)
        Settings.get().main().nextCustomChannelNotif()
    }
}
