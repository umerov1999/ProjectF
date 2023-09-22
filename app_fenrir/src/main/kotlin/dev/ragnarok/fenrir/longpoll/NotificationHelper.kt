package dev.ragnarok.fenrir.longpoll

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ChatActivityBubbles.Companion.forStart
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.activity.QuickAnswerActivity
import dev.ragnarok.fenrir.activity.QuickAnswerActivity.Companion.forStart
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory.withSpans
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.chatMessageChannelId
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getChatMessageChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getGroupChatMessageChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.groupChatMessageChannelId
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.place.PlaceFactory.getChatPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getExternalLinkPlace
import dev.ragnarok.fenrir.push.ChatEntryFetcher
import dev.ragnarok.fenrir.push.ChatEntryFetcher.DialogInfo
import dev.ragnarok.fenrir.push.NotificationScheduler
import dev.ragnarok.fenrir.service.QuickReplyService.Companion.intentForAddMessage
import dev.ragnarok.fenrir.service.QuickReplyService.Companion.intentForReadMessage
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.toastColor
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.ShortcutUtils.chatOpenIntent
import dev.ragnarok.fenrir.util.Utils.createOkHttp
import dev.ragnarok.fenrir.util.Utils.declOfNum
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.hasNougat
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object NotificationHelper {
    private const val NOTIFICATION_MESSAGE = 62
    const val NOTIFICATION_WALL_POST_ID = 63
    const val NOTIFICATION_REPLY_ID = 64
    const val NOTIFICATION_COMMENT_ID = 65
    const val NOTIFICATION_WALL_PUBLISH_ID = 66
    const val NOTIFICATION_FRIEND_ID = 67
    const val NOTIFICATION_FRIEND_ACCEPTED_ID = 68
    const val NOTIFICATION_GROUP_INVITE_ID = 69
    const val NOTIFICATION_NEW_POSTS_ID = 70
    const val NOTIFICATION_LIKE = 71
    const val NOTIFICATION_BIRTHDAY = 72
    const val NOTIFICATION_UPLOAD = 73
    const val NOTIFICATION_DOWNLOADING = 74
    const val NOTIFICATION_DOWNLOAD = 75
    const val NOTIFICATION_DOWNLOAD_MANAGER = 76
    const val NOTIFICATION_MENTION = 77
    const val NOTIFICATION_DOWNLOADING_GROUP = 78
    private val bubbleLock = Any()
    private var bubbleOpened: String? = null
    private fun getBubbleOpened(): String? {
        synchronized(bubbleLock) { return bubbleOpened }
    }

    fun setBubbleOpened(accountId: Long, peerId: Long) {
        synchronized(bubbleLock) { bubbleOpened = createPeerTagFor(accountId, peerId) }
    }

    fun resetBubbleOpened(context: Context, force: Boolean) {
        synchronized(bubbleLock) {
            if (!force) {
                if (AppPerms.hasNotificationPermissionSimple(context)) {
                    getService(context).cancel(bubbleOpened, NOTIFICATION_MESSAGE)
                }
            }
            bubbleOpened = null
        }
    }

    /**
     * Отображение уведомления в statusbar о новом сообщении.
     * Этот метод сначала в отдельном потоке получает всю необходимую информацию для отображения
     *
     * @param context контекст
     */
    @SuppressLint("CheckResult")
    fun notifyNewMessage(context: Context, accountId: Long, message: Message) {
        if (Settings.get().main().isDisable_notifications) return
        ChatEntryFetcher.getRx(context, accountId, accountId)
            .subscribeOn(NotificationScheduler.INSTANCE)
            .subscribe({ account ->
                ChatEntryFetcher.getRx(context, accountId, message.peerId)
                    .subscribeOn(NotificationScheduler.INSTANCE)
                    .subscribe({ info ->
                        if (Settings.get().main().isLoad_history_notif) {
                            messages.getPeerMessages(
                                accountId,
                                message.peerId,
                                10,
                                1,
                                null,
                                cacheData = false,
                                rev = false
                            )
                                .subscribeOn(NotificationScheduler.INSTANCE)
                                .subscribe({
                                    doShowNotification(
                                        accountId,
                                        context,
                                        account,
                                        info,
                                        message,
                                        it
                                    )
                                }) {
                                    doShowNotification(
                                        accountId,
                                        context,
                                        account,
                                        info,
                                        message,
                                        null
                                    )
                                }
                        } else {
                            doShowNotification(accountId, context, account, info, message, null)
                        }
                    }) {
                        doShowNotification(
                            accountId,
                            context,
                            account,
                            onErrorChat(context),
                            message,
                            null
                        )
                    }
            }) {
                doShowNotification(
                    accountId,
                    context,
                    onErrorChat(context),
                    onErrorChat(context),
                    message,
                    null
                )
            }
    }

    private fun onErrorChat(context: Context): DialogInfo {
        val ret = DialogInfo()
        ret.icon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_avatar_unknown)
        ret.title = context.getString(R.string.error)
        return ret
    }

    private fun doShowNotification(
        accountId: Long, context: Context, account: DialogInfo,
        info: DialogInfo, message: Message, history: List<Message>?
    ) {
        val account_peer = Peer(accountId).setTitle(account.title).setAvaUrl(account.img)
        val peer = Peer(message.peerId).setTitle(info.title).setAvaUrl(info.img)
        showNotification(
            context,
            accountId,
            peer,
            message,
            info.icon ?: BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_avatar_unknown
            ),
            history,
            account_peer,
            account.icon ?: BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_avatar_unknown
            )
        )
    }

    private fun getSenderName(own: Owner?, ctx: Context): String {
        val name = own?.fullName
        return if (name.isNullOrEmpty()) ctx.getString(R.string.error) else name
    }

    private fun getMessageContent(
        hideBody: Boolean,
        message: Message,
        context: Context
    ): CharSequence? {
        var messageText =
            if (message.decryptedBody.isNullOrEmpty()) message.body else message.decryptedBody
        if (messageText == null) messageText = ""
        if (message.forwardMessagesCount > 0) {
            messageText += " " + context.getString(
                R.string.notif_forward,
                message.forwardMessagesCount
            )
        }
        if (message.attachments?.hasAttachments == true) {
            messageText += if (!message.attachments?.stickers.isNullOrEmpty()) " " + context.getString(
                R.string.notif_sticker
            ) else if (!message.attachments?.voiceMessages
                    .isNullOrEmpty()
            ) " " + context.getString(R.string.notif_voice) else if (!message.attachments?.photos
                    .isNullOrEmpty()
            ) " " + context.getString(
                R.string.notif_photos,
                message.attachments?.photos?.size.orZero()
            ) else if (!message.attachments?.videos
                    .isNullOrEmpty()
            ) " " + context.getString(
                R.string.notif_videos,
                message.attachments?.videos?.size.orZero()
            ) else " " + context.getString(
                R.string.notif_attach, message.attachments?.size().orZero(), context.getString(
                    declOfNum(
                        message.attachments?.size().orZero(),
                        intArrayOf(
                            R.string.attachment_notif,
                            R.string.attacment_sec_notif,
                            R.string.attachments_notif
                        )
                    )
                )
            )
        }
        val text =
            if (hideBody) context.getString(R.string.message_text_is_not_available) else messageText
        return withSpans(text, owners = true, topics = false, listener = null)
    }

    private fun MakeMedia(
        context: Context,
        msgs: NotificationCompat.MessagingStyle,
        message: Message,
        hideBody: Boolean,
        accountId: Long,
        acc_avatar: Bitmap,
        avatar: Bitmap
    ) {
        if (hideBody || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val h_inbox = NotificationCompat.MessagingStyle.Message(
            getMessageContent(hideBody, message, context),
            message.date * 1000,
            Person.Builder()
                .setName(getSenderName(message.sender, context))
                .setIcon(IconCompat.createWithBitmap(if (message.senderId == accountId) acc_avatar else avatar))
                .setKey(message.senderId.toString()).build()
        )
        if (message.isHasAttachments && message.attachments != null) {
            if (message.attachments?.stickers.nonNullNoEmpty()) {
                val url = message.attachments?.stickers?.get(0)?.getImageLight(256)?.url
                val cont = doDownloadDataNotification(
                    context,
                    url,
                    "notif_" + accountId + "_" + message.getObjectId()
                )
                if (cont != null) {
                    h_inbox.setData(cont.mime, cont.uri_data)
                    msgs.addMessage(h_inbox)
                }
            } else if (message.attachments?.photos.nonNullNoEmpty()) {
                val url = message.attachments?.photos?.get(0)?.getUrlForSize(PhotoSize.X, false)
                val cont = doDownloadDataNotification(
                    context,
                    url,
                    "notif_" + accountId + "_" + message.getObjectId()
                )
                if (cont != null) {
                    h_inbox.setData(cont.mime, cont.uri_data)
                    msgs.addMessage(h_inbox)
                }
            } else if (message.attachments?.videos.nonNullNoEmpty()) {
                val url = message.attachments?.videos?.get(0)?.image
                val cont = doDownloadDataNotification(
                    context,
                    url,
                    "notif_" + accountId + "_" + message.getObjectId()
                )
                if (cont != null) {
                    h_inbox.setData(cont.mime, cont.uri_data)
                    msgs.addMessage(h_inbox)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun showNotification(
        context: Context,
        accountId: Long,
        peer: Peer,
        message: Message,
        avatar: Bitmap,
        History: List<Message>?,
        ich: Peer,
        acc_avatar: Bitmap
    ) {
        val hideBody = Settings.get()
            .security()
            .needHideMessagesBodyForNotif
        val text = getMessageContent(hideBody, message, context)
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                ?: return
        if (hasOreo()) {
            nManager.createNotificationChannel(getChatMessageChannel(context))
            nManager.createNotificationChannel(getGroupChatMessageChannel(context))
        }
        val channelId =
            if (Peer.isGroupChat(message.peerId)) groupChatMessageChannelId else chatMessageChannelId
        val msgs = NotificationCompat.MessagingStyle(
            Person.Builder()
                .setName(ich.getTitle()).setIcon(IconCompat.createWithBitmap(acc_avatar))
                .setKey(accountId.toString()).build()
        )
        val inbox = NotificationCompat.MessagingStyle.Message(
            text,
            message.date * 1000,
            Person.Builder()
                .setName(getSenderName(message.sender, context))
                .setIcon(IconCompat.createWithBitmap(if (message.senderId == accountId) acc_avatar else avatar))
                .setKey(message.senderId.toString()).build()
        )
        if (History != null) {
            for (i in History.reversed()) {
                val h_inbox = NotificationCompat.MessagingStyle.Message(
                    getMessageContent(hideBody, i, context),
                    i.date * 1000,
                    Person.Builder()
                        .setName(getSenderName(i.sender, context))
                        .setIcon(IconCompat.createWithBitmap(if (i.senderId == accountId) acc_avatar else avatar))
                        .setKey(i.senderId.toString()).build()
                )
                MakeMedia(context, msgs, i, hideBody, accountId, acc_avatar, avatar)
                msgs.addMessage(h_inbox)
            }
        }
        MakeMedia(context, msgs, message, hideBody, accountId, acc_avatar, avatar)
        msgs.addMessage(inbox)
        if (message.peerId > VKApiMessage.CHAT_PEER) {
            msgs.conversationTitle = peer.getTitle()
            msgs.isGroupConversation = Build.VERSION.SDK_INT >= 28
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.client_round)
            .setLargeIcon(avatar)
            .setContentText(text)
            .setStyle(msgs)
            .setColor(toastColor(false))
            .setWhen(message.date * 1000)
            .setShowWhen(true)
            .setSortKey("" + (Long.MAX_VALUE - message.date * 1000))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        //Our quickreply
        val intentQuick = forStart(
            context,
            accountId,
            message,
            text?.toString() ?: context.getString(R.string.error),
            peer.avaUrl,
            peer.getTitle()
        )
        val quickPendingIntent = PendingIntent.getActivity(
            context,
            message.getObjectId(),
            intentQuick,
            makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val actionCustomReply = NotificationCompat.Action(
            R.drawable.reply,
            context.getString(R.string.quick_answer_title),
            quickPendingIntent
        )

        //System reply. Works only on Wear (20 Api) and N+
        val remoteInput = RemoteInput.Builder(Extra.BODY)
            .setLabel(context.resources.getString(R.string.reply))
            .build()
        val ReadIntent =
            intentForReadMessage(context, accountId, message.peerId, message.getObjectId())
        val directIntent = intentForAddMessage(context, accountId, message.peerId, message)
        val ReadPendingIntent = PendingIntent.getService(
            context,
            message.getObjectId(),
            ReadIntent,
            makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val directPendingIntent = PendingIntent.getService(
            context,
            message.getObjectId(),
            directIntent,
            makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val actionDirectReply =
            NotificationCompat.Action.Builder( /*may be missing in some cases*/R.drawable.reply,
                context.resources.getString(R.string.reply), directPendingIntent
            )
                .addRemoteInput(remoteInput)
                .build()
        val actionRead =
            NotificationCompat.Action.Builder( /*may be missing in some cases*/R.drawable.view,
                context.resources.getString(R.string.read), ReadPendingIntent
            )
                .build()
        val intent = Intent(context, MainActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_PLACE
        val chatPlace = getChatPlace(accountId, accountId, peer)
        intent.putExtra(Extra.PLACE, chatPlace)
        val contentIntent = PendingIntent.getActivity(
            context,
            message.getObjectId(),
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        if (!hasNougat()) {
            builder.addAction(actionCustomReply)
        } else {
            builder.addAction(actionDirectReply)
        }
        builder.addAction(actionRead)

        //Для часов игнорирует все остальные action, тем самым убирает QuickReply
        val War = NotificationCompat.WearableExtender()
        War.addAction(actionDirectReply)
        War.addAction(actionRead)
        War.startScrollBottom = true
        builder.extend(War)
        if (!hasOreo()) {
            val notificationMask = Settings.get()
                .notifications()
                .getNotifPref(accountId, message.peerId)
            if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_HIGH_PRIORITY)) {
                builder.priority = NotificationCompat.PRIORITY_HIGH
            }
            if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_LED)) {
                builder.setLights(-0xffff01, 100, 1000)
            }
            if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_VIBRO)) {
                builder.setVibrate(
                    Settings.get()
                        .notifications()
                        .vibrationLength
                )
            }
            if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_SOUND)) {
                builder.setSound(findNotificationSound())
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Peer.isGroupChat(peer.id) && Settings.get()
                .main().isNotification_bubbles_enabled
        ) {
            createNotificationShortcut(
                context, builder, Person.Builder()
                    .setName(getSenderName(message.sender, context))
                    .setIcon(IconCompat.createWithBitmap(if (message.senderId == accountId) acc_avatar else avatar))
                    .setKey(message.senderId.toString()).build(), peer, accountId
            )
        }
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager.notify(
                createPeerTagFor(accountId, message.peerId),
                NOTIFICATION_MESSAGE,
                builder.build()
            )
        }
        if (Settings.get().notifications().isQuickReplyImmediately) {
            val startQuickReply = forStart(
                context,
                accountId,
                message,
                text?.toString() ?: context.getString(R.string.error),
                peer.avaUrl,
                peer.getTitle()
            )
            startQuickReply.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startQuickReply.putExtra(QuickAnswerActivity.EXTRA_FOCUS_TO_FIELD, false)
            startQuickReply.putExtra(QuickAnswerActivity.EXTRA_LIVE_DELAY, true)
            context.startActivity(startQuickReply)
        }
    }

    fun showSimpleNotification(
        context: Context,
        body: String?,
        pTitle: String?,
        type: String?,
        url: String?
    ) {
        var title = pTitle
        val hideBody = Settings.get()
            .security()
            .needHideMessagesBodyForNotif
        val text =
            if (hideBody) context.getString(R.string.message_text_is_not_available) else body
        val nManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (hasOreo()) {
            nManager.createNotificationChannel(getChatMessageChannel(context))
            nManager.createNotificationChannel(getGroupChatMessageChannel(context))
        }
        if (!type.isNullOrEmpty()) {
            title += ", Type: $type"
        }
        val builder = NotificationCompat.Builder(context, chatMessageChannelId)
            .setSmallIcon(R.drawable.client_round)
            .setContentText(text)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        if (!url.isNullOrEmpty()) {
            val aid = Settings.get()
                .accounts()
                .current
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(Extra.PLACE, getExternalLinkPlace(aid, url))
            intent.action = MainActivity.ACTION_OPEN_PLACE
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val contentIntent = PendingIntent.getActivity(
                context,
                url.hashCode(),
                intent,
                makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
            )
            builder.setContentIntent(contentIntent)
        }
        val notification = builder.build()
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager.notify(
                "simple " + Settings.get().accounts().current,
                NOTIFICATION_MESSAGE,
                notification
            )
        }
    }

    private fun createPeerTagFor(aid: Long, peerId: Long): String {
        return aid.toString() + "_" + peerId
    }

    fun findNotificationSound(): Uri {
        return try {
            Uri.parse(
                Settings.get()
                    .notifications()
                    .notificationRingtone
            )
        } catch (ignored: Exception) {
            Uri.parse(
                Settings.get()
                    .notifications()
                    .defNotificationRingtone
            )
        }
    }

    private fun getService(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }


    fun tryCancelNotificationForPeer(context: Context, accountId: Long, peerId: Long) {
        //int mask = Settings.get()
        //        .notifications()
        //        .getNotifPref(accountId, peerId);

        //if (hasFlag(mask, ISettings.INotificationSettings.FLAG_SHOW_NOTIF)) {
        val peer = createPeerTagFor(accountId, peerId)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || peer != getBubbleOpened()) {
            if (AppPerms.hasNotificationPermissionSimple(context)) {
                getService(context).cancel(peer, NOTIFICATION_MESSAGE)
            }
        }
        //}
    }

    private fun doDownloadDataNotification(
        mContext: Context,
        url: String?,
        prefix: String
    ): Content? {
        val cache = File(mContext.cacheDir, "notif-cache")
        val MimeType = "image/jpeg"
        val urit: Uri?
        if (!cache.exists()) {
            cache.mkdirs()
        }
        val file = File(cache.absolutePath, prefix)
        try {
            if (!file.exists()) {
                if (url.isNullOrEmpty()) throw Exception(mContext.getString(R.string.null_image_link))
                val output: OutputStream = FileOutputStream(file)
                val builder: OkHttpClient.Builder = createOkHttp(Constants.GIF_TIMEOUT, false)
                val request: Request = Request.Builder()
                    .url(url)
                    .build()
                val response: Response = builder.build().newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception(
                        "Server return " + response.code +
                                " " + response.message
                    )
                }
                val inputStream = response.body.byteStream()
                val input = BufferedInputStream(inputStream)
                val data = ByteArray(80 * 1024)
                var bufferLength: Int
                while (input.read(data).also { bufferLength = it } != -1) {
                    output.write(data, 0, bufferLength)
                }
                //MimeType = response.header("Content-Type", "image/jpeg");
                output.flush()
                input.close()
                response.close()
            }
            urit = FileProvider.getUriForFile(mContext, Constants.FILE_PROVIDER_AUTHORITY, file)
            mContext.grantUriPermission(
                "com.android.systemui",
                urit,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return if (urit != null) Content(MimeType, urit) else null
    }

    @SuppressLint("RestrictedApi", "ReportShortcutUsage")
    private fun createNotificationShortcut(
        context: Context,
        builder: NotificationCompat.Builder,
        person: Person,
        peer: Peer,
        accountId: Long
    ) {
        try {
            val person_name =
                if (person.name != null) person.name.toString() else context.getString(R.string.error)
            val id = "fenrir_peer_" + peer.id + "_aid_" + accountId
            val shortcutBuilder = ShortcutInfoCompat.Builder(context, id)
                .setShortLabel(person_name.trim { it <= ' ' })
                .setLongLabel(person_name)
                .setIntent(
                    chatOpenIntent(
                        context,
                        peer.avaUrl,
                        accountId,
                        peer.id,
                        peer.getTitle()
                    )
                )
                .setLongLived(true)
            shortcutBuilder.setPerson(person)
            shortcutBuilder.setIcon(person.icon)
            val avatar: Bitmap = person.icon?.bitmap ?: BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_avatar_unknown
            )
            val arrayList = ArrayList<ShortcutInfoCompat>(1)
            arrayList.add(shortcutBuilder.build())
            ShortcutManagerCompat.addDynamicShortcuts(context, arrayList)
            builder.setShortcutId(id)
            val intentQuick = forStart(context, accountId, peer)
            val bubbleIntent = PendingIntent.getActivity(
                context,
                0,
                intentQuick,
                makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
            )
            val bubbleBuilder = NotificationCompat.BubbleMetadata.Builder(
                bubbleIntent,
                IconCompat.createWithAdaptiveBitmap(avatar)
            )
            bubbleBuilder.setSuppressNotification(true)
            bubbleBuilder.setAutoExpandBubble(false)
            bubbleBuilder.setDesiredHeight(640)
            builder.bubbleMetadata = bubbleBuilder.build()
        } catch (e: Exception) {
            //FileLog.e(e);
        }
    }

    private class Content(val mime: String, val uri_data: Uri)
}