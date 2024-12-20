package dev.ragnarok.fenrir

import android.annotation.SuppressLint
import androidx.annotation.WorkerThread
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.ragnarok.fenrir.Includes.pushRegistrationResolver
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.push.PushType
import dev.ragnarok.fenrir.push.message.BirthdayFCMMessage
import dev.ragnarok.fenrir.push.message.CommentFCMMessage
import dev.ragnarok.fenrir.push.message.FCMMessage
import dev.ragnarok.fenrir.push.message.FriendAcceptedFCMMessage
import dev.ragnarok.fenrir.push.message.FriendFCMMessage
import dev.ragnarok.fenrir.push.message.GroupInviteFCMMessage
import dev.ragnarok.fenrir.push.message.LikeFCMMessage
import dev.ragnarok.fenrir.push.message.MentionMessage
import dev.ragnarok.fenrir.push.message.NewPostPushMessage
import dev.ragnarok.fenrir.push.message.ReplyFCMMessage
import dev.ragnarok.fenrir.push.message.WallPostFCMMessage
import dev.ragnarok.fenrir.push.message.WallPublishFCMMessage
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class FcmListenerService : FirebaseMessagingService() {
    @SuppressLint("CheckResult")
    @WorkerThread
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        pushRegistrationResolver
            .resolvePushRegistration(Settings.get().accounts().current, applicationContext)
            .hiddenIO()
    }

    @WorkerThread
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val context = applicationContext
        val pushType = message.data["type"]
        val accountId = Settings.get()
            .accounts()
            .current
        if (accountId == ISettings.IAccountsSettings.INVALID_ID || pushType.isNullOrEmpty() || Settings.get()
                .main().isSettings_no_push
        ) {
            return
        }
        val registrationResolver = pushRegistrationResolver
        if (!registrationResolver.canReceivePushNotification(accountId)) {
            Logger.d(TAG, "Invalid push registration on VK")
            return
        }
        if (Settings.get().main().isDump_fcm && PushType.ERASE != pushType) {
            if (Constants.IS_DEBUG) {
                Logger.d(
                    TAG,
                    "onMessage, from: " + message.from + ", pushType: " + pushType + ", data: " + kJsonPretty.encodeToString(
                        MapSerializer(String.serializer(), String.serializer()),
                        message.data
                    )
                )
            }
            PersistentLogger.logThrowable(
                "Push received",
                Exception(
                    "Key: $pushType, Dump: " + kJsonPretty.encodeToString(
                        MapSerializer(String.serializer(), String.serializer()),
                        message.data
                    )
                )
            )
        }
        try {
            when (pushType) {
                PushType.MSG, PushType.CHAT -> FCMMessage.fromRemoteMessage(message)
                    .notify(accountId)

                PushType.POST -> WallPostFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.COMMENT -> CommentFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.FRIEND -> FriendFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.NEW_POST -> NewPostPushMessage.fromRemoteMessage(accountId, message)
                    .notify(context)

                PushType.LIKE -> LikeFCMMessage.fromRemoteMessage(accountId, message)
                    ?.notify(context)

                PushType.REPLY -> ReplyFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.WALL_PUBLISH -> WallPublishFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.FRIEND_ACCEPTED -> FriendAcceptedFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.GROUP_INVITE -> GroupInviteFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.BIRTHDAY -> BirthdayFCMMessage.fromRemoteMessage(message)
                    ?.notify(context, accountId)

                PushType.SHOW_MESSAGE, PushType.VALIDATE_DEVICE -> NotificationHelper.showSimpleNotification(
                    context,
                    message.data["body"],
                    message.data["title"],
                    message.data["url"]
                )

                PushType.MENTION -> MentionMessage.fromRemoteMessage(message)
                    .notify(context, accountId)

                else -> {}
            }
        } catch (e: Exception) {
            PersistentLogger.logThrowable("Push issues", e)
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "FcmListenerService"
    }
}