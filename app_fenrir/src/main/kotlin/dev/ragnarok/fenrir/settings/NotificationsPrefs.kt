package dev.ragnarok.fenrir.settings

import android.content.Context
import android.net.Uri
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.settings.ISettings.INotificationSettings
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.removeFlag
import java.util.Collections

class NotificationsPrefs internal constructor(context: Context) : INotificationSettings {
    private val app: Context = context.applicationContext
    private val notification_peers: MutableSet<String> = Collections.synchronizedSet(HashSet(1))
    private val types: MutableMap<String, Int> = Collections.synchronizedMap(HashMap(1))
    override val chatsNotif: Map<String, Int>
        get() = HashMap(types)
    override val chatsNotifKeys: Set<String>
        get() = HashSet(notification_peers)

    override fun reloadNotifSettings(onlyRoot: Boolean) {
        val preferences = getPreferences(app)
        notification_peers.clear()
        notification_peers.addAll(preferences.getStringSet(KEY_PEERS_UIDS, HashSet(1)) ?: return)
        if (onlyRoot) {
            return
        }
        types.clear()
        for (i in notification_peers) {
            types[i] = preferences.getInt(i, getGlobalNotifPref(true))
        }
    }

    override fun setNotifPref(aid: Long, peerid: Long, flag: Int) {
        val preferences = getPreferences(app)
        notification_peers.add(keyFor(aid, peerid))
        types[keyFor(aid, peerid)] = flag
        preferences.edit()
            .putInt(keyFor(aid, peerid), flag)
            .putStringSet(KEY_PEERS_UIDS, notification_peers)
            .apply()
    }

    private val isOtherNotificationsEnable: Boolean
        get() = hasFlag(otherNotificationMask, INotificationSettings.FLAG_SHOW_NOTIF)
    override val otherNotificationMask: Int
        get() {
            val preferences = getPreferences(app)
            var mask = 0
            if (preferences.getBoolean("other_notifications_enable", true)) {
                mask += INotificationSettings.FLAG_SHOW_NOTIF
            }
            if (preferences.getBoolean("other_notif_sound", true)) {
                mask += INotificationSettings.FLAG_SOUND
            }
            if (preferences.getBoolean("other_notif_vibration", true)) {
                mask += INotificationSettings.FLAG_VIBRO
            }
            if (preferences.getBoolean("other_notif_led", true)) {
                mask += INotificationSettings.FLAG_LED
            }
            return mask
        }
    override val isCommentsNotificationsEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("new_comment_notification", true)
    override val isFriendRequestAcceptationNotifEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("friend_request_accepted_notification", true)
    override val isNewFollowerNotifEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("new_follower_notification", true)
    override val isWallPublishNotifEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("wall_publish_notification", true)
    override val isGroupInvitedNotifEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("group_invited_notification", true)
    override val isReplyNotifEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("reply_notification", true)
    override val isNewPostOnOwnWallNotifEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("new_wall_post_notification", true)
    override val isNewPostsNotificationEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("new_posts_notification", true)
    override val isBirthdayNotifyEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("birtday_notification", true)
    override val isMentionNotifyEnabled: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("mention_notification", true)

    override fun isSilentChat(aid: Long, peerId: Long): Boolean {
        if (types.containsKey(keyFor(aid, peerId))) {
            val v = types[keyFor(aid, peerId)]
            if (v != null) {
                return !hasFlag(v, INotificationSettings.FLAG_SHOW_NOTIF)
            }
        }
        return false
    }

    override val isLikeNotificationEnable: Boolean
        get() = isOtherNotificationsEnable && getPreferences(app)
            .getBoolean("likes_notification", true)
    override val feedbackRingtoneUri: Uri
        get() {
            val path = "android.resource://" + app.packageName + "/" + R.raw.feedback_sound
            return Uri.parse(path)
        }
    override val newPostRingtoneUri: Uri
        get() {
            val path = "android.resource://" + app.packageName + "/" + R.raw.new_post_sound
            return Uri.parse(path)
        }
    override val defNotificationRingtone: String
        get() = "android.resource://" + app.packageName + "/" + R.raw.notification_sound
    override val notificationRingtone: String
        get() = getPreferences(app)
            .getString(KEY_NOTIFICATION_RINGTONE, defNotificationRingtone)!!

    override fun setNotificationRingtoneUri(path: String?) {
        getPreferences(app)
            .edit()
            .putString(KEY_NOTIFICATION_RINGTONE, path)
            .apply()
    }

    override val vibrationLength: LongArray
        get() = when (getPreferences(app)
            .getString(KEY_VIBRO_LENGTH, "4")) {
            "0" -> longArrayOf(0, 300)
            "1" -> longArrayOf(0, 400)
            "2" -> longArrayOf(0, 500)
            "3" -> longArrayOf(0, 300, 250, 300)
            "5" -> longArrayOf(0, 500, 250, 500)
            else -> longArrayOf(0, 400, 250, 400)
        }
    override val isQuickReplyImmediately: Boolean
        get() = getPreferences(app).getBoolean("quick_reply_immediately", false)

    override fun forceDisable(aid: Long, peerId: Long) {
        var mask = getGlobalNotifPref(Peer.isGroupChat(peerId))
        if (hasFlag(mask, INotificationSettings.FLAG_SHOW_NOTIF)) {
            mask = removeFlag(mask, INotificationSettings.FLAG_SHOW_NOTIF)
        }
        setNotifPref(aid, peerId, mask)
    }

    override fun setDefault(aid: Long, peerId: Long) {
        val preferences = getPreferences(app)
        notification_peers.remove(keyFor(aid, peerId))
        types.remove(keyFor(aid, peerId))
        preferences.edit()
            .remove(keyFor(aid, peerId))
            .putStringSet(KEY_PEERS_UIDS, notification_peers)
            .apply()
    }

    override fun resetAccount(aid: Long) {
        val preferences = getPreferences(app)
        for (i in HashSet(notification_peers)) {
            if (i.contains(keyForAccount(aid))) {
                notification_peers.remove(i)
                types.remove(i)
                preferences.edit().remove(i).apply()
            }
        }
        preferences.edit()
            .putStringSet(KEY_PEERS_UIDS, notification_peers)
            .apply()
    }

    override fun getNotifPref(aid: Long, peerid: Long): Int {
        if (types.containsKey(keyFor(aid, peerid))) {
            val v = types[keyFor(aid, peerid)]
            if (v != null) {
                return v
            }
        }
        return getGlobalNotifPref(Peer.isGroupChat(peerid))
    }

    private fun getGlobalNotifPref(isGroup: Boolean): Int {
        val sharedPreferences = getPreferences(app)
        var value = if (sharedPreferences.getBoolean(
                "high_notif_priority",
                false
            )
        ) INotificationSettings.FLAG_HIGH_PRIORITY else 0
        if (!isGroup) {
            if (sharedPreferences.getBoolean("new_dialog_message_notif_enable", true)) {
                value += INotificationSettings.FLAG_SHOW_NOTIF
            }
            if (sharedPreferences.getBoolean("new_dialog_message_notif_sound", true)) {
                value += INotificationSettings.FLAG_SOUND
            }
            if (sharedPreferences.getBoolean("new_dialog_message_notif_vibration", true)) {
                value += INotificationSettings.FLAG_VIBRO
            }
            if (sharedPreferences.getBoolean("new_dialog_message_notif_led", true)) {
                value += INotificationSettings.FLAG_LED
            }
        } else {
            if (sharedPreferences.getBoolean("new_groupchat_message_notif_enable", true)) {
                value += INotificationSettings.FLAG_SHOW_NOTIF
            }
            if (sharedPreferences.getBoolean("new_groupchat_message_notif_sound", true)) {
                value += INotificationSettings.FLAG_SOUND
            }
            if (sharedPreferences.getBoolean("new_groupchat_message_notif_vibration", true)) {
                value += INotificationSettings.FLAG_VIBRO
            }
            if (sharedPreferences.getBoolean("new_groupchat_message_notif_led", true)) {
                value += INotificationSettings.FLAG_LED
            }
        }
        return value
    }

    companion object {
        private const val KEY_NOTIFICATION_RINGTONE = "notification_ringtone"
        private const val KEY_VIBRO_LENGTH = "vibration_length"
        private const val KEY_PEERS_UIDS = "notif_peer_uids"
        internal fun keyFor(aid: Long, peerId: Long): String {
            return "notif_peer_" + aid + "_" + peerId
        }

        internal fun keyForAccount(aid: Long): String {
            return "notif_peer_$aid"
        }
    }

    init {
        reloadNotifSettings(false)
    }
}