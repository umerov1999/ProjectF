package dev.ragnarok.fenrir.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.CryptStatus
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTime
import dev.ragnarok.fenrir.util.DownloadWorkUtils.CheckDirectory
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeLegalFilename
import dev.ragnarok.fenrir.util.Utils.appLocale
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.inMainThread
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingle
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingleSafe
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs

class ChatDownloadWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val DOWNLOAD_DATE_FORMAT: DateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", appLocale)
    private val Avatars: MutableMap<Long, String> = HashMap()
    private val avatars_styles = StringBuilder()
    private val messagesRepository: IMessagesRepository = messages
    private val mNotifyManager = createNotificationManager(applicationContext)
    private fun createNotificationManager(context: Context): NotificationManagerCompat {
        val mNotifyManager = NotificationManagerCompat.from(context)
        mNotifyManager.createNotificationChannel(
            AppNotificationChannels.getDownloadChannel(
                context
            )
        )
        return mNotifyManager
    }

    private fun readBase64(value: String): String {
        return String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
    }

    private fun getAvatarUrl(owner: Owner?, owner_id: Long): String {
        if (owner_id >= VKApiMessage.CHAT_PEER) {
            return "https://vk.com/images/icons/im_multichat_200.png"
        }
        val AVATAR_USER_DEFAULT = "https://vk.com/images/camera_200.png?ava=1"
        return owner?.maxSquareAvatar ?: AVATAR_USER_DEFAULT
    }

    private fun Apply(Param: String, Replace: String?, res: String): String {
        return if (res.contains(Param)) {
            if (Replace == null) res.replace(Param, "") else res.replace(Param, Replace)
        } else res
    }

    private fun getTitle(owner: Owner?, owner_id: Long, chat_title: String?): String? {
        return if (owner_id < VKApiMessage.CHAT_PEER) {
            if (owner == null || owner.fullName.isNullOrEmpty()) "dialog_$owner_id" else owner.fullName
        } else chat_title
    }

    private fun getAvatarTag(peerId: Long): String {
        return "avatar_id_" + if (peerId < 0) "club" else "" + abs(peerId)
    }

    private fun Build_Message(i: Message, isSub: Boolean): String {
        i.sender?.let {
            it.maxSquareAvatar.nonNullNoEmpty { kk ->
                if (!Avatars.containsKey(it.ownerId)) {
                    Avatars[it.ownerId] = kk
                    var avatar_data = ".chat ul li a.user .<#AVATAR_NAME#> {\n" +
                            "        background-image: url(\"<#AVATAR_URL#>\");\n" +
                            "        width: 70px;\n" +
                            "        height: 70px;\n" +
                            "        border-radius: 50%;\n" +
                            "        background-color: #f3f3f3;\n" +
                            "        background-size: cover;\n" +
                            "        background-position: center center;\n" +
                            "        background-repeat: no-repeat;\n" +
                            "        display: inline-block;\n" +
                            "        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);\n" +
                            "    }"
                    avatar_data =
                        Apply("<#AVATAR_NAME#>", getAvatarTag(it.ownerId), avatar_data)
                    avatar_data = Apply("<#AVATAR_URL#>", kk, avatar_data)
                    avatars_styles.append(avatar_data)
                }
            }
        }
        var msg_html = StringBuilder(
            "<li class=\"<#MSG_TYPE#>\">\n" +
                    "    <a class=\"user\" href=\"<#PAGE_LINK#>\"><div class=\"<#AVATAR_NAME#>\"></div></a>\n" +
                    "    <div class=\"<#CLASS_USERNAME#>\"><#USER_NAME#></div>\n" +
                    "    <div class=\"<#CLASS_DATE#>\"><#DATE_TIME#></div>\n" +
                    "    <div class=\"message\">\n" +
                    "        <p class=\"text_message\"><#MESSAGE_CONTENT#></p>\n" +
                    "\t<#SUB_CONTENT#>\n" +
                    "    </div>\n" +
                    "</li>"
        )
        msg_html = StringBuilder(
            Apply(
                "<#MSG_TYPE#>",
                if (i.isOut) "you" else "other",
                msg_html.toString()
            )
        )
        i.sender?.ownerId?.let {
            msg_html = StringBuilder(
                Apply(
                    "<#AVATAR_NAME#>",
                    getAvatarTag(it),
                    msg_html.toString()
                )
            )
        }
        msg_html = StringBuilder(
            Apply(
                "<#PAGE_LINK#>",
                "https://vk.com/" + (if (i.sender?.ownerId.orZero() < 0) "club" else "id") + abs(i.sender?.ownerId.orZero()),
                msg_html.toString()
            )
        )
        msg_html = StringBuilder(Apply("<#USER_NAME#>", i.sender?.fullName, msg_html.toString()))
        msg_html =
            StringBuilder(Apply("<#DATE_TIME#>", getDateFromUnixTime(i.date), msg_html.toString()))
        msg_html = StringBuilder(
            Apply(
                "<#CLASS_USERNAME#>",
                if (isSub) "resendusername" else "username",
                msg_html.toString()
            )
        )
        msg_html = StringBuilder(
            Apply(
                "<#CLASS_DATE#>",
                if (isSub) "resenddate" else "date",
                msg_html.toString()
            )
        )
        val message =
            StringBuilder(if (i.cryptStatus != CryptStatus.DECRYPTED) i.text.orEmpty() else i.decryptedText.orEmpty())
        if (!isSub && i.forwardMessagesCount > 0) {
            for (s in i.fwd.orEmpty()) {
                val fwd = Build_Message(s, true)
                var result_msgs = "<ul><#MESSAGE_LIST#></ul>"
                result_msgs = Apply("<#MESSAGE_LIST#>", fwd, result_msgs)
                message.append(result_msgs)
            }
        }
        var AttacmentHeader = "<p class=\"attachments\">Вложения: <#ATTACHMENT_TYPE#></p>"
        if (i.isHasAttachments && i.attachments != null) {
            var HasPhotos = false
            var HasVideos = false
            var HasStory = false
            var HasAlbum = false
            var HasArticle = false
            var HasDocs = false
            var HasLinks = false
            var HasPosts = false
            var HasNarative = false
            if (i.attachments?.photos.nonNullNoEmpty()) HasPhotos = true
            if (i.attachments?.videos.nonNullNoEmpty()) HasVideos = true
            if (i.attachments?.docs.nonNullNoEmpty()) HasDocs = true
            if (i.attachments?.stories.nonNullNoEmpty()) HasStory = true
            if (i.attachments?.photoAlbums.nonNullNoEmpty()) HasAlbum = true
            if (i.attachments?.articles.nonNullNoEmpty()) HasArticle = true
            if (i.attachments?.links.nonNullNoEmpty()) HasLinks = true
            if (i.attachments?.posts.nonNullNoEmpty()) HasPosts = true
            if (i.attachments?.narratives.nonNullNoEmpty()) HasNarative = true
            AttacmentHeader =
                if (!HasPhotos && !HasVideos && !HasStory && !HasAlbum && !HasArticle && !HasDocs && !HasLinks && !HasPosts && !HasNarative) Apply(
                    "<#ATTACHMENT_TYPE#>",
                    " !ИНОЕ!",
                    AttacmentHeader
                ) else {
                    val Type = StringBuilder()
                    if (HasDocs) Type.append(" !ДОКУМЕНТ!")
                    if (HasStory) Type.append(" !ИСТОРИЯ!")
                    if (HasAlbum) Type.append(" !АЛЬБОМ!")
                    if (HasArticle) Type.append(" !СТАТЬЯ!")
                    if (HasPhotos) Type.append(" !ФОТО!")
                    if (HasVideos) Type.append(" !ВИДЕО!")
                    if (HasLinks) Type.append(" !ССЫЛКА!")
                    if (HasPosts) Type.append(" !ПОСТ!")
                    if (HasNarative) Type.append(" !СЮЖЕТ!")
                    Apply("<#ATTACHMENT_TYPE#>", Type.toString(), AttacmentHeader)
                }
            message.append(AttacmentHeader)
        }
        msg_html =
            StringBuilder(Apply("<#MESSAGE_CONTENT#>", message.toString(), msg_html.toString()))
        val Attachments = StringBuilder()
        if (i.isHasAttachments) {
            val Image =
                "\"<a href=\"<#ORIGINAL_IMAGE_LINK#>\"><div data-href=\"<#IMAGE_LINK#>\" class=\"progressive replace\"><img class=\"preview\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAVSURBVBhXY/z//z8DAwMTEDMwMAAAJAYDAbrboo8AAAAASUVORK5CYII=\"/></div></a>\""
            i.attachments?.links.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply("<#ORIGINAL_IMAGE_LINK#>", att.url, atcontent)
                    atcontent = Apply(
                        "<#IMAGE_LINK#>",
                        att.photo?.getUrlForSize(
                            PhotoSize.Y,
                            false
                        ),
                        atcontent
                    )
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.articles.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply("<#ORIGINAL_IMAGE_LINK#>", att.uRL, atcontent)
                    atcontent = Apply(
                        "<#IMAGE_LINK#>",
                        att.photo?.getUrlForSize(
                            PhotoSize.Y,
                            false
                        ),
                        atcontent
                    )
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.photoAlbums.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply(
                        "<#ORIGINAL_IMAGE_LINK#>",
                        "https://vk.com/album" + att.ownerId + "_" + att.getObjectId(),
                        atcontent
                    )
                    atcontent = Apply(
                        "<#IMAGE_LINK#>",
                        if (att.sizes == null) null else att.sizes?.getUrlForSize(
                            PhotoSize.Y,
                            false
                        ),
                        atcontent
                    )
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.stories.nonNullNoEmpty {
                for (att in it) {
                    if (att.photo == null && att.video == null) continue
                    var atcontent = Image
                    if (att.photo != null) {
                        atcontent = Apply(
                            "<#ORIGINAL_IMAGE_LINK#>",
                            att.photo?.getUrlForSize(PhotoSize.W, false),
                            atcontent
                        )
                        atcontent = Apply(
                            "<#IMAGE_LINK#>",
                            att.photo?.getUrlForSize(PhotoSize.Y, false),
                            atcontent
                        )
                    } else if (att.video != null) {
                        atcontent = Apply(
                            "<#ORIGINAL_IMAGE_LINK#>",
                            "https://vk.com/video" + att.video?.ownerId + "_" + att.video?.id,
                            atcontent
                        )
                        atcontent = Apply("<#IMAGE_LINK#>", att.video?.image, atcontent)
                    }
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.posts.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply(
                        "<#ORIGINAL_IMAGE_LINK#>",
                        "https://vk.com/wall" + att.ownerId + "_" + att.vkid,
                        atcontent
                    )
                    atcontent = Apply(
                        "<#IMAGE_LINK#>",
                        att.author?.maxSquareAvatar,
                        atcontent
                    )
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.photos.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply(
                        "<#ORIGINAL_IMAGE_LINK#>",
                        att.getUrlForSize(PhotoSize.W, false),
                        atcontent
                    )
                    atcontent =
                        Apply("<#IMAGE_LINK#>", att.getUrlForSize(PhotoSize.Y, false), atcontent)
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.docs.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply("<#ORIGINAL_IMAGE_LINK#>", att.url, atcontent)
                    atcontent = Apply(
                        "<#IMAGE_LINK#>",
                        att.getPreviewWithSize(PhotoSize.Y, false),
                        atcontent
                    )
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.videos.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply(
                        "<#ORIGINAL_IMAGE_LINK#>",
                        "https://vk.com/video" + att.ownerId + "_" + att.id,
                        atcontent
                    )
                    atcontent = Apply("<#IMAGE_LINK#>", att.image, atcontent)
                    Attachments.append(atcontent)
                }
            }
            i.attachments?.narratives.nonNullNoEmpty {
                for (att in it) {
                    var atcontent = Image
                    atcontent = Apply(
                        "<#ORIGINAL_IMAGE_LINK#>",
                        "https://vk.com/narrative" + att.owner_id + "_" + att.id,
                        atcontent
                    )
                    atcontent = Apply("<#IMAGE_LINK#>", att.cover, atcontent)
                    Attachments.append(atcontent)
                }
            }
        }
        msg_html =
            StringBuilder(Apply("<#SUB_CONTENT#>", Attachments.toString(), msg_html.toString()))
        if (isSub && i.forwardMessagesCount > 0) {
            for (s in i.fwd.orEmpty()) {
                val fwd = Build_Message(s, true)
                msg_html.append(fwd)
            }
        }
        return msg_html.toString()
    }

    @SuppressLint("MissingPermission")
    private fun doDownloadAsHTML(chat_title: String?, account_id: Long, owner_id: Long) {
        try {
            var owner: Owner? = null
            if (owner_id < VKApiMessage.CHAT_PEER) {
                owner = OwnerInfo.getRx(applicationContext, account_id, owner_id)
                    .syncSingleSafe()?.owner
            }
            val peer_title = getTitle(owner, owner_id, chat_title)
            val mBuilder = NotificationCompat.Builder(
                applicationContext,
                AppNotificationChannels.DOWNLOAD_CHANNEL_ID
            )
            mBuilder.setContentTitle(applicationContext.getString(R.string.downloading))
                .setContentText(
                    applicationContext.getString(R.string.downloading) + " " + applicationContext.getString(
                        R.string.chat
                    ) + " " + peer_title
                )
                .setSmallIcon(R.drawable.save)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
            mBuilder.addAction(
                R.drawable.close,
                applicationContext.getString(R.string.cancel),
                WorkManager.getInstance(
                    applicationContext
                ).createCancelPendingIntent(id)
            )
            if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                mNotifyManager.notify(
                    id.toString(),
                    NotificationHelper.NOTIFICATION_DOWNLOADING,
                    mBuilder.build()
                )
            }
            val HTML_MAIN =
                "PCFET0NUWVBFIGh0bWw+DQo8aHRtbD4NCjxoZWFkPg0KICAgIDxtZXRhIGNoYXJzZXQ9InV0Zi04Ij4NCiAgICA8bWV0YSBuYW1lPSJyb2JvdHMiIGNvbnRlbnQ9Im5vaW5kZXgsIG5vZm9sbG93Ij4NCiAgICA8bWV0YSBuYW1lPSJ0aGVtZS1jb2xvciIgY29udGVudD0iIzUxODFiOCI+DQogICAgPGxpbmsgcmVsPSJzaG9ydGN1dCBpY29uIiBocmVmPSJkYXRhOmltYWdlL3BuZztiYXNlNjQsaVZCT1J3MEtHZ29BQUFBTlNVaEVVZ0FBQUxRQUFBQzBDQU1BQUFBS0UvWUFBQUFDK2xCTVZFVnBwT2hvbytkbm91Wm5vdWRrb2Vaa29lZGpvT2RpbitaaG4rWmhudVZnbnVabW9lVmxvT1Zsb2VWaW4rVmduZVJtb2Vaa29PVmZuZVJrb09Sam4rUmxvT1JrbitSaW4rUmVuT05tb2VSaW51TmpudU5obmVOZG0rTmRtK0pnbk9ObG9PTmpudUpobmVGZm0rSmVtK0Znbk9GY211SmRtdUZqbmVKY211RmZuT0ZkbXVCZm0rRmJtZUZlbXVCYm1PQmNtZDlpbmVGaG5PQmdtOTlkbXQ5YW1OOWRtZDlobStCZm10OWVtZDlkbWQ1Wmw5NWFtTjVjbU41Ymw5MWZtdDVjbU4xWmw5MVlsdDFmbTk1WGxkeGVtdDFlbWQxYWw5eFpsdHhZbGR4V2xOdGNsOXhlbU54ZG1OeGFsdHRZbGR0Ymx0dGNsOXRabGRwWWxOcFhsTnBXazlwVWt0bGNsdHBibHRwYWx0cFlsTmxXa3RoWGs5aGFsZGxZbE5oVmt0aFVrZGlLdE9QYTV2WDMrdjMvLy8vNi9QN3A4Zm1hdmVYazdmajkvdjd6OS95NTBleC9xOTZ3eStwV2t0ZDRwOTYvMWUvZjZ2YWp3K2RabE5pU3QrSlRrTmRTa05kVmtkYnU5UHJVNHZOWWs5ZHNvTnRVa05aUmo5WlJrTmJQMy9GWGt0WllrOVpUa05WV2tkVlNqOVJRanRXSnNOM0cydkJtbWRWUmp0UlZrTlJUajlSUGpkUmVsdFpUajlOUWpkTk9qTk9xeCtkM3BOaFZrTk5VajlOU2p0SlJqZEZQak5KTmk5Sk5pdEpQaTlGTml0Rk1pZEZNaWRCT2lzOUxpTTlSamRCUmpOQk1pYzllazlGUWpNOVFpODlMaU01S2g4NU5pYzFLaDgxUGlzNU1pTXhKaHMxT2ljMUloY3hOaU14dm45VkloY3RIaE10TWg4dEhoTXBHZzhwTGhzdEtoY3BKaGNsSWhNbEZnOGxGZ3NsSGc4aEloTWhGZ3NoRWdjaERnY2RGZ3NaQ2dNWkhnc1pJZzhkRWdjVkhnc2RGZ2NaRWdNVkNmOFpCZjhSQmZzVkdnY1ZFZ01SQmZzUkRmOFJEZjhOQ2ZzTkFmY05BZmNJL2ZNSkNmc0krZThFL2ZNRStlOEpCZmNGQWZNRStlOEE5ZXNBL2U4RkNmY0kvZThBOGViOUFmTUErZXI4N2VMNDllcjgvZTc4OGViNDZkNzArZWI0K2VyNDllTDA4ZUwwNmQ3dzdlTDA1ZHJ3OGVMdzVkcnM0ZGJzOGQ3dzdkN3M0ZGJvN2RyczZkcnM0ZExvM2RMbzZkcm8yYzdrNWRibzJjN2cwY3JpRWxqT1NBQUFkZ1VsRVFWUjRBWlNUQ1hhRklBd0FGUkNNZmtDOS8yRTdueFFSN1RxZFY1T3U0emJBT0l3NFZKaS94MWlEdHNOWmg3Wm4raG8zZVQvNXV2bEM4QUY5VHppWnc5eVFXV1FlV25BcjVuK09ka1E5VnN6WWdrdThld2F6RktoRFBTcjk3dC94WWJvSGx4MFdQbkJCdGhhTURGUmlDMlBwWWhrTTBQdU1OczY0R2w1cEo5SkN1NVB3TFZvYmE2aWl1MFpEQ1JmUjJCWk4yRDNjZHBodm9ibkFVT2ZmOFlyekRydlFLOHNWdVRPTWVoWC9rN3lhRlV1MmU3a1hhazkwRVg5dlRqNmhQcmdTQkxVMGhZVDNhbm1RQjczdGVMNW9tOTBzYnFqUm05a0tEQ2FhaUtXOUJuZmh1OXZSdWVRUzZwNzJoUHIxTXhqRWEzQUxyL01TOHBLUjVFWHlrUS9NV2JMSUljY2h3K1VsQTVyUFlOUlFEVy9SYTF3eFJrTHIxV1hjUWNQNDlBN0dGc3VNR3MzWE1ERzJxMHNwbkNmeERrWU5QV01QUFhLbGIyd2FXT24ycUZDTzhYVmwveVR1TWFaSWtWSURPN2pTeUNCUGtxUVB1c2xvMlZVUUNHRG5sUkZWQ3d1b3RQLy9tVGV5RjFGYk14blk0U2xGaW1hc21GRnYrb3k1UlUvZDFLRTJ0cmxHWC9sVjdXYm5acSt4ZnZaK1lIN0NQRkppVWNOZmQvNDYvV1BCUFJhdG5leUUydFc3dnBkZWNDL3ViYkFCTFliQ0hEVGNENkhFeGlIaXdPaDk5RHJYTXpEakpVNW5hTEhrN2t0NnBRSkRtV3MwWHVJMTNPcHV1eExQTTE0V3R6aEhQQVpvMFM0NDU1MUh2V2xDOFJMTUhMMkp4eE9ncmtaZmQ2WVRHczBDTmJvRmYyTTFXSEhXWFFoQmdtQkFEUTQrSUpIL29SYmJIaW0rY0F1dTFEZk1nT21idis2WnhZcFlxYzBpVGxveXJTYzhITkZINUJwWFBQOEFqVjdOZmo3RzhZa1dEcitpNVJUWkxZc3NnbVV2bjE1cFQ0QUZXV0FMRzdib3pXL2J1cTBIR3F6a21OR1liREtXOEp4eVNtTnFvWG5NNVN6cjI4WFcvRTV2VE9tVFBwOFNMWjJBeHU1UUQ1WkxycDkvMy8reFVTNDRiZ0pCRU9VUzZ4RERZcy9Qdzg5Z0hOLy9iS21lMm02QjJMZWw2UVpGMGt0cE1LVU5rNVo1aFd1am1LdzAzVjViaFBNUFpRMlZ4bEhmNnR1dHZiVUZsa3pvYTlMYXRLRU5GMFFWbUxEZVYxeGVwRGtCYTRUekFJV04zWE90UUJpUmh0bjBBUXFUN2h0Rm8rMEtrdmV2Ty9KMTRLL0tLbzNjV2JZcjl3Qmc1Yk5ybkxzNk9wNnNZZWRiajJBNTdtaTJITGR3QzZFTzloL1kvN3lCNG9vVTZ5NTJNWFlWaFEwOENDWWtFNmluQ050dU9HdTV1UHVyUjhwTStJTWtaZkdRMnNUZEI0cmlRTFJ0UG9lNlk2T1kzYU1JSTFoanpsMkdlSFducUhDNVh4Q3V4TVJOVnFZNHQwMkw4R2IwMTc3Z2UrOGRaUGZBMHNSbHgxRUliZGVGenNRTGVJR0Vlcy9qKy9GUThVZEg2YTY2S3lZT1JQVDRvUUZyRTdJSEhPamRqM2hCbTZZd0RpSDQ0SkVRVXNEN2NNTEVnVFZkUXhqV2hUaEVaQmh5ZFhIdTR0aXd1Ly9zRkRZdUtzdlp1cmJ2Mjc0Vk1DaUptcEc5c0FlcFlPSkJFc0lZeGpGRXl1R09JbXc5QnRsTkdOQzJNL0djaHp3TVUrVk0xc1R4WlRWOTB5TU4xdElpd284TkI5N3plc0JjZHBXbE9CRkpoTXhwUmhKY0taekdsTHBBWVpnaVovSDhjeFdHeHpBOGh5Y3lUUGlUWThxVm96Q09FeXBMVEJwdkRSTldReXdLMWlLcm1MUXd4NXhqanNBa2diNGplTWlRTlZSNnFrNm1sQ1dVOXM0am5MQThDcWNkSTZWSHp6YlRUR2x0ZWhtWFpWN21lWnpIRVVJSHBqd2hSWnIzZHNETDUxT2tyZWwxV3RmWCtucjlJcjF0L1ViWnpaWDlKSXhGZGs2N3M5UVdWRjVrQWNYSEdPYzRDL0NlcGppeFNRRUxjcGJIUDFxZnF3aFRITUtGOVNqTmpqZUtjKzd1YUo4b2FoVGJNN0FXNWowTGdDMnlDRVVNeHc3S3FqaXZ3bHFrY1JRZ2pXRFpTN05iRXk1NHcrU1IzVHpxS2lkcDFCeVhXSVF4Y2xUaDMrVHQ3cFlMUVdoT0lMM0JsTHJ1dlcxcFM4aW0rTGQvSTVSOHB6ZXkrK0RzbDRGei9BZFRjMld6bi9tRHNPSFA4a0ZLMFVkZWt0ZC93c2tBTjNFWWlLSzVEMmhMWXljRTJ6RUE5Ny9UdnNuMFcwNldWWisrbUo5cEpiME1ndGowaXhwbVBIMU5oTkwzb2ZtWi9KN3hlelFRZFVuc3JaUDJJdWNPdkRkcHduUkpjRkVlSlUzbStUUTNZM0JoUi9Jc2tUU08wdE13N2lXdjQ1V01RcEs3cnZsbjlHdUtjQTVoQ1FzSmhvU2IvQ21lQ0R1RXNkNlFvT1owbWNqbGgwMlNOSGtYTjJtVC9aYm9VZmIvTzBrVG02YmM0RUd5b24rQldlRHVza2UrTHBKdE5Ha3VqZkNvQy92c3NWMFlBOW4vVDdnR2t6QWt6b1g5MHNidGgzaUxNY1ZFVUkwemNlVTBKMkt0Vi9WTEg4bFRKaFF4WUF3NFNISXZCeVpJMmd1b242SDdnYm1zeEFPeUdHK2ttd3Npakx0M1lkTGxVc2ltakRPZm1rZHA5V0UwWEVTQ1IxaVNRNDh4TG9qeVlVaGF6NmtEWjc4c01OUkZNVndhMVRLVlJuWk1tSFI5NEhUaXMram5QYWZjV0c0dXFxdFNERVQ5d2tja3ZhYlZxZXRhMWxKcXFVWXUyWVd4bityRW9tWkRROUlmMmI3bWU3Z1QvN3J2Uyt2RUw0MmhKSVhrY1VycGtSN0VCUjlyNnlRWjNtdlpnZldHbFdkK0VuOSs1UmZ4anZSN0k3d0Q4WW1VUklsTFNsYjdCUDAxWTRyRWQzRnRvc1FsSmF1OVFhblAraVMxRnVTZnhTVkxkbmxKSDNrTkx0dUwzOE83Q2RzVWVpYTNSVDh3eVhZdlFacXdCQ1dwUFhqSGxMaDRyazJVK0dYNUVPMDVEKzkvdWYrT0x2Y1JTZFpISlk4aitwdEZsN2E1NTJWeFdSWHhsMUZ5MFcwVWlHSW8zNFdrMENVd1Q0WUpwUDMvWDFuSHR4WkRhYlI3WkdIcjZuV0VadXJtWWJnUDkvdDhuNUhmamZ0WDhIR0RjNzNyd1dCeXZmMjE5djIra096UDloL2UzenczOFg4Q1FrVlVRT1E3SWxmaUdKRnhpaDFFRFJPZm5ZTWJYVkhjc2lhVU4wR0pDa3lUNmIyMUpDVU9KSSt3SlNweHd0c2hlU0YxODhFd0g3SUdwZkZ6YldOd04rK3pSWCtkVXRsbmhOS0lKOWNkVG95QlQ4SGtJNEUwNWFjWDR6UWlVMnFsZ2ZzZi92VmVKV3o5aHVBRDhwN1lBdWM0Y2FBb3ZTenpNaVBMNGhhSHNBVm1LYTdZeGtET2h2a2pJMjFMUzlMcjZsZFBWbTJKZ3hvcWN1NVlvOENzZGFvVFNmQkZURnBJVnVCUWxuS1JscUNrQlcrM1ExcWVGMm0xTjJsQjZSZ2lnalpoRWkvU1JYK1hVZ1NxU0dGdzVGM29uZVpienMxRzlGN1BTTElsckFGWlZ4TXlPYlZJTlNIZmQ1aWU2Q2lZUzhtU1pST01DM3FyTDdRZitmSHdEOXMvbGQ5VEtkMVlYcEI4ZzZSTmNsczJ4THBzQmRtMnZPVmNNcmR1OEtNa01GRWdjU1NmbmtEWVE0WGJ1cTg3b2piZHZlN1AranprUHVzbm9rN3BLMzBoOXJlMTFhbGJXaW0ybnNndmR3OVBDVk1XNFExQm03REFQTXNDYmhqWDlrZkQvbmtJNDVDK1pldFpGdGorU3loNTRFZ0l3d0NRRnlKYVF1cGwvLytLbThReU1tTExhRTYyb2piblpTSUlybGlXQnhvK0x6TXViOUdyVTMyeENScCtaNzhoc2NBQ3pxTGhndWR2TW5IbmVpTExXNVpsTmRFc0hZYnU5cUwzNnZjY2cvM1lVWFozT0dTKzVYMjBpUTloRFN2SzdJUTV4Q1V1R09NY05mYjVUNWo0TFcwSkpWSDM2MDFpUnpUYmpld3lPcFFwclI1bCttazlRUzVzTDAwdG1PZ2w5RmlNRi9abmYvZnRKbmdYM1Nsd2xBTlpuRnhZTGkxUUM0OW9QNWdrMW9hSFB1TWFVZU5qaUFhSjE5ajM0V20ya1JaNUg3RWFMcVVhVHVnVnllb3RHaTJFTStDWWJBS3BLT3VEN1FmcEs4VVZoeEp1THRuSmlzLytQVk9Wd0JvcUVoNTFOek5XakZnSExGdmM1SW8xMWJyVjdjMzNhd3BUUVpsLzVROGxPdE5YY2ltK2VXell3M051dWFITS92YnlMNVRaSVZvdTNLbm5GUldEeEErb2pob09qSVQzVUkwRmVUT3hjTi9MaUVYNVpvdDNnK1phYS85ODFGRjdtakFVeG5FKzRHNTJPOVdpaTlWYURWb0syWkN3Q0xDdDMzcHZPRHNDaGNOdmVXSjg1c1hmWTJnWVB2bkFCZUxEQmQ2RXczQXZERlpEUEdGdnMvMnVCbmJQMnoxOXNYYkMyRHI3dzR1YTlISjgvYm9FbmpKaXNUZ2FubnJRVGJHRW9yRU5JVnF5VVZPMnFFYmordHNhcTJzK0lWbDBmZ1FUbE9xRjF0Z2d1aHcreitiNnh0bkQrSEYwSEs5aTdIajFjTGlxU1JjMGorL3JYczA0VUt0ZWFxemxrdStzOS9RKytXM1AzSmVFQ1ZZSWFaaENtT0lVUEpJNW1sNWhKL3pjMGVlNzZwM1VqRjB2R3RNMUM0TkYwUkdhSi96Z1pxQm95dTVGeDVPT2F0cHIvNmZtOC9xbm1xRTdwdjNudDlaV3lkRWNqTTFMSHdMTU5Zc3pyRkgwVlUwN3JwYU1vdlVhRTd3ZDU2TTdwbWVqcGwxNTBKMjBIMDBvZlBCNlVZTE5Nc3QwUnZjend4a2Jqbk9Qb3VIcFlwSGM1RmhHR3N3Wi8ya0xXeFJKa1NRMnNWaEpXcVFGVnBvV3dUaVk3WlhnbDZaUWhucU5iM0dlaTNiR09lMThlQnY3MzdzU1JCZ3plcnZvM0ZLd1RhMHRnb3hRTElXVCtLWUV1NXR1SVJUekppaGZIWlRFeC9LQTI2MWxzbWMxYlp0N2xxQVo0WWlGZHZxV29zazl1Mk5sRDg5SzhLWXhOSnBjMzM1bTBveWp5N3dzOHpmeEFpSzVRSjlsRkZ1TW96a2NLUDVWQ1E1NlVueFVBcjdERkl2TnE3NUluNy9pTTNWdVdXM3J1ckVObGlYQjNibTdvMGlYOFpuaWZ5dEo1TzlvQ3dlUEhyUS9jalNoYUNZLzZmNXpDSzBickJyTmRxQUozRENXejlpY1BMa0xCdy8rNXNyM0k0ZDJ4SDNSaTNTajY3SnNTcG9zTnZaNGJ3UFhSWTZJbzlqOU5SUHlhRGFhcDh5MlNuQ3FBWkVmelFmV09CcVRKcFdyc056QS9hUWtad09sS2N1cXJDcFQwVG4veDFoOUtMZXFBMkVjNTFGUDRmVGVlOE9rSUJtSmhHckp1RDNwL1ZpeVJHRm1tZnYzanRlM25sOWtJT0x6UmlXRE9FM1NWS2RhSnpwQjR1UHVBbVRDTXByMytteTl6dGI0RERRM3gyTjlqWVdlNThrWFdGZUkwUFJVVUxsODBnbWtHbUVSUHNtbC8vZkR5elRMc2pSQU1oeU4rR2cxUWhGakdZNlJmMSs4eHduam1ERzBLYVZpb1lTa09HVk1TdWtYNHY4NWhaaXdlT05OOGVjQXpURjRoWDQ4aTRVZS8xVXFVUUdhRXRFb0FBTXYzb1ZQRmN3aW1BSjZzVCt4MU1lRWdoeERYejVHUnB2RVVLbEpNV2IxVHJ3TE0yU3B0VjFqc0l0eDg5OHJsdEJLYWZsUi9VNFJtTk5vQ1owaVlBbU5vN3dTNzhKc2FFUVd0c0RZQW1qS3JobXRBaVNZZDlKS1BaV1BXZzhwVGVJeEVXMU1aakxNemJiWHo4UWIzS0tDQXBlUTRRK0FkeFNWcTFXNUtrdFZxaW5vRVN6UWZJemxxOXBvWTVSUkdodXpoTlozMEpsNko1NEV5SVJEV0JOK2ppWXdadHFvUW5oRDBsTVB2VGRqQkI3aHNWQmRaM1dHR1hjalhoeC9MR3JEaXJiQVRMc3J1cUxvSWlEaHc1UVV3WFZacVVyclNrUDlQcGFmMVhyRTFxYkdHQ1NpRzd3SWJEUGJpaytPaDVjV2hVaXFhenNNL1QyWU41c3VDc0FJSHcya3hsUlladGo1YzFGOVZZZmdSVFJsYTR0cDhzZXgwQk1jcyswb0lESFRaOFp2OE9xNkNHQ29iek1sc0NNYVllRVhqSGd3QkNiNFdDelVBTXZvK2lJVyt1YWNhNTNsazZWODYzM25neDlpUkljWnpMejhvYWkrcUZFSWw5RzNQWkhQd0xXdDY5d1FWb2ZCQmhqcUlhek54bSs4MzBSVlhWYzFvd2t3RDBjdGRUNWNySmdhL1UvMDlTUHg0b0RWNDhWZzVDbUdNM29iQVF3MXhsQjhqUVo3NGFwK3ZBSjRnc3ZvbHNJRmErVUwrcHNiOHdUM3RHZHRoOWx1UFY4ZXdGRXpNUFUrRm50VlV3Ukg0a25EUytqeWdmZ3N1blNJd2J3RmRCMFdZaWROWGNyUDZtYy9taW5YdUZob3VNRmFqSHNiUzMwY3lBSjJiTnR2ZTJxR0RyOXEvb3pYeDFqc1FSV29SVFNCMFh2NW5yNDU1OTczR0UveFo2N25vb1p3YUk3bGplcDNzdnFLd1VoRVY0NlNmL2l2LzNZVGo3SGhSdnQ5djhjN05xTVppVUl3aG5vcG81KzlhY2FXMERtWjd6OFQveWN2M2M0NVBzczVHUEZpOU9GUUgyck00ZEFjR3N6aHB1T3hPVFlZMmhleSttcysvSHR1RWYwRy8yejM4Wmw4UDhOOE92V25Ibk9hOGllUDRiKy92eTA2Y0l6bWZRUjc2dktoclA0Mi9JdkxsOGRiOXgvdGROT2JOaENFY2R5ZnRWblNVb2hhVUE3MDFCdmxuUFNVQTQ3QjlxN2ZZL01GK3l5UFJzdTB4UWRML211a1dkekx6MXZuN2J3eWQ5c2t4K1BwZEJUMDMzaWZmQnBFcHdIdG1XbVNwdTlaa21SSmhnbnFyYm5maW1MYzF2Mi8xNThqYi8xd2dCa1RuK0k0ai9QOGxKOHczSktOTGVZR25iNm5QaXpDT1V6d3Y3K01xTGZIYTIrSm1kUVcxK3h5SWdISENEWGc0OXNpWUhHOUdLSXp2RUFXQXBqdzdKc1o2Wm5vMTBubVhlSng0WVpWZkdhbDYyM2JDTmhRbGlLRkRhM00vWmFmcityREpQU0tGNnJEZDRMSkpYdFRZU05sSnJqTVNrelk3THd6SXoybFFLK25valc0eUF1TWJKbzFPaE9yQndwU1V2QzlHV3V6THMrZkpxRWZ5N3dxR0pFMnQ1andBcmEyUHF3YVk0c2lnZ2JxbEQ0aWtaelZzMTltdE1YU1RPdkpFb2M3eEJEUE0xL0NZeEVXNFhWMFZRbExnZi81alE5a25wNExsWHdhN0FZclJmQTBaWU1wVmZKTW5mY3pvWmY3dXFoSnhBRVR0aiswZFl1aDE1K0JEakNFSmNqL3diL1BwRjZzWVVNYXpNMEVUblNqRXl4elRlbEtqSE00WWw0ZjUxTnJ0RlFRcWdKYVFSV2VXQUU3Mzh0eUp2WERpekFWWG51bHFDT3lhenFNY3J2T2RSam53K0svYjgxYzZvKys3alVjRC9wMmFIMVl3K0RQZll0blJHczR6eG9zNStiSFhPcXZCNDhVT005dExXQ0Zqd2pGaE0wSXJWeFZkVlhudzhMNXZKdEx2VHYzeEVyMVFHUS85SmdoRkFXam9EbEVJb1hHZkN6bVVtOUtoVlpTQlk4dW5lcUNxa3VGNFlZVUk3K3Zlei9wUC84UHFYYXptN1lYUkFHY1p5MGxCQ0trSm1KQlYrd0lXK2dMUktLN1Zpb08vc1FYRzMrQUNXbmZxY2N6ZFFZYzU5cFdEMGZNNE9Rdi9YVGhqeGZwemVCemJSNE53elJNbE9lekpZZHJQcHNvVDBKdlFNMkRCVHhVY29uRnozbU9XcE43TS92blpubFgreDR0ak12QUNyZUZsdEdNUldrU1NpSmd4TTd6dzhicmFWdjB5SEFNeDNGV3RUZW5QazY0aUlVQXpIQkVYbmVBYlJRYjZDTGZibHVpMTRSR2xuWC9HNDlOam9HSEJYZGxPbm9uS3FmczJBNmFMNnVXNnBVRHRlRWFydnYwV0hkck5FM1A5RHh6YTFtKzVhTmkzVnBiTkVmYkFrUXdLZzdZdVVZanEzYjNjNXRPMm5WYzlLSHVKc05vajZFQ3IwYWpsM2k1NWxTa25kcWp1SjZMZXJOZXpkZTFVc3JmK2x2VWY1dUlYT3Z3NlRGdUk3c3RoNHY4cjVxd0tNOWxWL3ZMdDkrVjV4VlF3Y3IwTzY1dHU3YUwyb2pzK1hSY05tT2hZRUY1T25ZYjlZN2k3VHdVY3ozVS92YlU5VlhnQnlpanNWQ3dvSmg4MG93VTJCVWNFYXhNNytsclkzU09GZlJXelNmNmIwZ0JDbHl1aFIxWEl1anExK1ZybTd1bWFLVjJhb2VxSWpYcVdjQmhhT2lIWVJBR2FNZ1JkSnNVbjgvK29CbGF3QmkwcmJUcTdqeTRpSUFadjY5QzcvTHlVekVSMmZFZVU3QXNodzFQV2hKRktvSmNyNzUzOThBRmV6UUk5K0dlZ2lYZlk2QWJneW5GWjFONS9KYXZwdTNRRWFOUnZYckJZRlRBQ1BpRUZ0eS9weUtDTHFMb2dUSVl3UmdQbTZBRGhiYzVDaUpKTXRmOWg0UDVOVm9TeDNGSFNJeldoYkdvVEtUQllTdUtvRGxyM2ZmMWRGK1ZHR2FnVXlHbHU1U0NCY1VzL1V5bENwV0owSzVta3hwMDJjczVMSFgzeG5WWmkySXl1aGFMYXRCSGhicmpTWHMwb3Z2bnkzZFhXSjVzQmxwUzRON25lRXlQZU1aazVKRkNGeWhKNnZRMTdHNldSVm1FeWtST2VNdytWdC9FeUFzZStkUDFGTFF1YjhnMFNaUGttUENPNjhTT2poRzZHWC82OEk4WEdjTGdRM1E0WkFmc0xOZW9CMW04Rnl4Qyt6aytueXZSZ0tIRmhJNWE0TWdQTi9CUjZkcDhjVk45ZytQVGhSYk5FT3luRTRyTWVoK2U5T3ZMSy9vR3h3cndHZm9PNDRvUTlqSTVLb240ZEFYSU80SWhldzViUHJ6N2sxQzNuMU1QSlhRR01HVjlXNDFlUUFvb3c1RnJORVhnZ3MvU0RFMlNMTWtvOHJrVWJEbS84VUdkajBkZnB2ZVRRYTg3dkorT1pyOU9ETVRSSGxDYU1CZjVrNDZuazBHWlBGbWtmd2twRjl3R2tTQ0krdjYzMmtRb2hNeVhqNTNqN0tOTVN1MVpTL3VvME0xZ1NVOHpUU3g4VmloTFdVZ3B5eTJhQnVHelhxTGFhY1F0bmI1UzhvNXA5enl2MHNDYnpPNi9Kc3VxNXBwekRUVCs1cjk1WlNmbjc1a2dleUh4cFpDbmRJL1NrZWx6d29HZCt5ZWxLVTBrNFNwaGxqeWovQ1orWEpJbEx6WC9NV2VFZVp5b2RhNkNSdjEzOVlkR1BqN0tSeWsvNVljVTBVc256K3FkVHArSlhQMFhTWktLc3E3bktzUTVKZHJGTitRcmx0WTJMM1VoOVE4MmVCRTBoQnFFSlIrayt5MEZac1FuQ3NGQUpEM1BSSlgxVWRMSFhya0VqWHM1NWtwQ1hTclJVUXNhb2lwb1ZpN0R3OXJYVGxaUmtOWU9XM3ZPYWNwUDJaeTBPMUVZUEordTRwS0ZLTis0b3JBNEJXdXBSS0w0Vy9aNS9MM1FTbGFVSUN6Q1R1ZEJscmdLM2hPdFFYd3ZweWg3MFNUTlRlU1djMXNhWHYrZFUwQjJFVFRyd0hheWJqUzBsa2FVaUxmQy85ZlhOd1JoeS9mZW9tanNUUm1WZy9BSjBqV2YxRFFlbzRqUHNZL3ZSYWt2Unc3c0tlQkltc0JuNVFwb2dieldkYmN1N2I1dit5Wm9DUFdXNjJraVJxR1d0VU9EdE5hSjEwNWJnYkgvSzJDOEhPMDQraEdrVzE4YllsdmZ5Q1Z0TEIyRnhTQzkzMi90VlNZU1A2TG0rWlN3ZjV0NzlwZlBMQnAwTGN6dGZEaldRMGNzWVc2Q0JjT0R6Q0tZYStteFB3ajE5M0hmYjVmSUtBdmpQTHBHYXE5eEpqMm5wVXRVMEVUcGlHZlY3QU1QWFpibWRtZW5oV2R2clBHZDhWRzN0N09KM2JFZEd6bkFmVER6VVkvUCsrLytTM2JYQjl6eDVDYnVJR2xtajBTNWlOL0gvdklXbnRGTC9GL0N5UVM3WVJBR29yNy83ZnF3SVJMRVczS1F6b2lvMVNQYjc2U2FkUDBQNUdWRUZwRkZrNkZKRVpSQVNRV2hlQmVOVUx1V2loU2o1dGJ5MVA5SGxJcFg2dDlEaG83RGkxZExhWUFoa29TdmhJZ0JhM1FsRkViVThHc25sTGFIYk03R0pWK1FYQW1GS1E1YWJqbTNLY2hGS0UzR3ZhUTB0Mkg1ZWIyVHB0eWxTUmRYMFZsbmhOSjBZeTlHRk9jZU9KVG1xM1p2akl4Z2R1bDN2TnpMQitrTmdqam1pbGpUcTE0UjFhSUZVU3NEZWFUVmhsU0QzWUcweUNycnVxeTJlOGdyNlFqVlZIaHlhZTVYSGJ2b0tLMG9UbmZkeXJieDVFcnUxKzRkZDQ4RTZXelNnV2JzazFEV3hGTVVqOU94OTBrVTF2R2hjbkVLSXpyT2JVTUliQkcrcGJvSkkzM0N6b1JkM0lYRENlLzQ0S2U5VFdzL2FjaThKVXJyb283dmFaOXYyUnpJS3JKdGRSdjJsTE45eDZYM1NjVTQ1RURrSTRkdWlEcStwNFF5eUh0bGNtNG5BdWtIWVdjcGczeFd2dTAzaE5JU2lmS3hBMFUwQ29hSEtoRGxVVXh5NUt6M2U3M1hBT1U3VVQ1MjFEOXUwL0hQS0J2bnNSMSt2U1IySjhyYS9wN0ZUcGJteGkvaDVJTGpKaEFFVWU1L3lXUlpNMy9BN0FsU3RaMVMwd2labDVLclBVaktvejNMbWNLRTB2Z1FkTG5Lbmh1bXRtVnVHOUltNWJqb21TajhKSzh0QzVkL3ZXWVhsanlsSElsR0pFeDViWHI1V3BBb3F2TS9TN3lUVis2Zm1XeWFFK0x6SzcwdS9PV2EvYjRHZEc3azc0eHcwNStGQVdTaXRQcGVXSmdrY09ueUtwUkd6dktmaFlrR1NlZHBrYlFrSFI1VDJPK2tCRCtlQVpPdHFkYTV1cnlFMVVRL3UvcE0rc2J6eE91Y0NZVk5YTktPQzg5ZnMrT0NUMmNKb1RDU0hKZU9aMEUyQ0NPMllDRnBiUHFXWUZGbjI5alRtVnVuUUV1dHBaNENQWFVFVms5QWMrU0JaSWZTZGFtSUM5cy9wOHdGaWZORkV2R3ZJZ2l1aWFCV1JFOHVmMkQzNUMxdlNDWTJUMUY0S1V1cHRaUlNXUndBSkpIVDdIY1c4YnRMK3R4L2FiMGhOcWR1NG1zM1lYd1ltY21aL2IzNkN4QTlrekFiMkR4QkdOYUcrUkpLRTRuckpTZ05QOHBHY1NGWk51RU1QZmhLV2k5QUpaZHpYSnBzWXlPb2daajBtQ3B4dTJkU1NRQmxrOCtVQ2V4OVIvcXY5Yjd1eUNyZy9Nekk3N0c5TjRKNkk1c3gxZFpxc3h2UlNrTStHK3VPY3BPcHhYbjBnVnlsdWVYSVdBZlhOdkt3bi8wMEkwRmFvb0ZqYXJXMDZySW1IbHJBTHhWS29pV1BzSDJyRWxmL3JEOUR3QlpoODBKSThsYWNmZndLYm9mQWFOKzNLY2hwdmpuak92MitxZ1dFVFd3Zk82SytQNE5ndUtmcTY5bnhQZ2pxalhnZjAvNy9mMjA3Z3VHZVRrVnRVOUw2UHBCL2haT042dnNxRUVUei9vOTZmMEtOMzhZRTdteW4wNXBhK0o4dTdpZ0JUdUkydW9oeUxyaUFjai9STXhLYjUzVEtZdjhtN1FrRmVVbExQRHhDK0M5d2o4RDhkMGV5Nm4rVFZJNDV1OHc5UW5IRkxVaFFYZGgrOXh3RFU2NTdSZTBMYVFzVU5XWlpIUElGYkM3LytQVTBxdzluc3E5Y0hrL1Jrc3RiRUtIRUVxT1AzanZQdlMvTUVJTVhaV0duSzlmTUdqeHZLYlhVMnQ0bzM0QmxMRzJXRmhKV2h6QkZ3VEt2a2RLQTRsaXdqWEQyY0xaT1ljdkdQSnVHc3RBWmhGRXAxUVRoeW13Z21IU01qL2hBeFJoaVFMR2Jxc25adVl2M21SU2hoTnh6NzdGSFZFZk16MzNwM2t2VXpqMzJSdDg3Wm5LZFU1ODhLaUZ5RHpFRFRaa2M3VUFoUUZwUVZzQUpobUNSUnZGWmpHeUJvWUF2djdiaEFXekJLbzFGOHBhRnprRDdpYVNQTFgrK2JnNzVTU3dSVlZDUzFGNS90T0tHeThNb2crSllFTWVUbmpvcW9TaU14ZmE2Ym96b2EwNHI1MVF2b2JOMFNQQ3RlNk50bVNKR1pMWXVabG1VZWtlOUJFZEh6WmxkU0g3ME9sS2x0THBtZFI0SG5FK3lsRi9ZTXBsbnMzUmR1K1pVWjN4bXhMRWdhWk9jR1dtZ0lFWWtPUVAzend1YzdUeVA4MENkUVBuZXI0M0NxRklrWitSeWswWHhiT1JSOGZ1TkdZNjc3SnVYM0ZIdDJwa05oQzhPc2NnYTEzRmR4L2E2UzhtT3NseTF4QkZYWGJ3QlNzNjhkb0RHTDRvRmZQMzd4WHBtZ20rdTgwSkovTE0vTjhxSlpTN25UTWVQTEVCVGJpak5KYVVORTVlVUJKVU5CR1hEcENZa09tY3MyeVQ2RDg1NlZ0U0ptbVdaWWZtZVRadkwrZG4xcXUreXl2cWFBRTJTUC9nZmlhWmgyZUtZVUdnQUFBQUFTVVZPUks1Q1lJST0iIHR5cGU9ImltYWdlL3BuZyI+DQogICAgPHRpdGxlPjwjRElBTE9HX05BTUUjPjwvdGl0bGU+DQoNCiAgICA8c3R5bGU+DQogICAgICAgIEBmb250LWZhY2Ugew0KICAgICAgICAgICAgZm9udC1mYW1pbHk6ICdNb250c2VycmF0JzsNCiAgICAgICAgICAgIHNyYzogdXJsKCJkYXRhOmFwcGxpY2F0aW9uL3gtZm9udC13b2ZmMjtjaGFyc2V0PXV0Zi04O2Jhc2U2NCw8I0ZPTlQjPiIpIGZvcm1hdCgnd29mZjInKTsNCiAgICAgICAgfQ0KDQogICAgICAgIGJvZHkgew0KICAgICAgICAgICAgYmFja2dyb3VuZC1jb2xvcjogI2YzZjNmMzsNCiAgICAgICAgICAgIGZvbnQtZmFtaWx5OiAnTW9udHNlcnJhdCc7DQogICAgICAgICAgICBmb250LXdlaWdodDogMzAwOw0KICAgICAgICB9DQoNCiAgICAgICAgc2VjdGlvbiB7DQogICAgICAgICAgICBvdmVyZmxvdzogaGlkZGVuOw0KICAgICAgICAgICAgbWFyZ2luOiA2MHB4IGF1dG87DQogICAgICAgICAgICBiYWNrZ3JvdW5kLWNvbG9yOiAjZmZmOw0KICAgICAgICAgICAgcGFkZGluZzogMTBweCAxNXB4IDIwcHggMTVweDsNCiAgICAgICAgfQ0KDQogICAgICAgIEBtZWRpYSBzY3JlZW4gYW5kIChvcmllbnRhdGlvbjpwb3J0cmFpdCkgew0KICAgICAgICAgICAgc2VjdGlvbiB7DQogICAgICAgICAgICAgICAgd2lkdGg6IDg1JTsNCiAgICAgICAgICAgIH0NCiAgICAgICAgfQ0KDQogICAgICAgIEBtZWRpYSBzY3JlZW4gYW5kIChvcmllbnRhdGlvbjpsYW5kc2NhcGUpIHsNCiAgICAgICAgICAgIHNlY3Rpb24gew0KICAgICAgICAgICAgICAgIHdpZHRoOiA1NSU7DQogICAgICAgICAgICB9DQogICAgICAgIH0NCg0KICAgICAgICAuY2hhdCB1bCB7DQogICAgICAgICAgICBsaXN0LXN0eWxlOiBub25lOw0KICAgICAgICAgICAgcGFkZGluZzogMDsNCiAgICAgICAgICAgIG1hcmdpbjogMDsNCiAgICAgICAgfQ0KDQogICAgICAgICAgICAuY2hhdCB1bCBsaSB7DQogICAgICAgICAgICAgICAgbWFyZ2luOiA0NXB4IDAgMCAwOw0KICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkgYS51c2VyIHsNCiAgICAgICAgICAgICAgICAgICAgbWFyZ2luOiAtMzBweCAwIDAgMDsNCiAgICAgICAgICAgICAgICAgICAgZGlzcGxheTogYmxvY2s7DQogICAgICAgICAgICAgICAgICAgIGNvbG9yOiAjMzMzOw0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpIC5kYXRlIHsNCiAgICAgICAgICAgICAgICAgICAgZm9udC1zaXplOiAxNHB4Ow0KICAgICAgICAgICAgICAgICAgICBjb2xvcjogI2E2YTZhNjsNCiAgICAgICAgICAgICAgICB9DQoNCiAgICAgICAgICAgICAgICAuY2hhdCB1bCBsaSAudXNlcm5hbWUgew0KICAgICAgICAgICAgICAgICAgICBmb250LXNpemU6IDE3cHg7DQogICAgICAgICAgICAgICAgICAgIGNvbG9yOiAjNjY2Ow0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpIC5tZXNzYWdlIHsNCiAgICAgICAgICAgICAgICAgICAgZGlzcGxheTogYmxvY2s7DQogICAgICAgICAgICAgICAgICAgIHBhZGRpbmc6IDEwcHg7DQogICAgICAgICAgICAgICAgICAgIHBvc2l0aW9uOiByZWxhdGl2ZTsNCiAgICAgICAgICAgICAgICAgICAgZm9udC1zaXplOiAxNXB4Ow0KICAgICAgICAgICAgICAgICAgICBib3JkZXItcmFkaXVzOiAzcHg7DQogICAgICAgICAgICAgICAgICAgIGJveC1zaGFkb3c6IDAgMnB4IDZweCByZ2JhKDAsIDAsIDAsIDAuMyk7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpIC5tZXNzYWdlOmJlZm9yZSB7DQogICAgICAgICAgICAgICAgICAgICAgICBjb250ZW50OiAnJzsNCiAgICAgICAgICAgICAgICAgICAgICAgIHBvc2l0aW9uOiBhYnNvbHV0ZTsNCiAgICAgICAgICAgICAgICAgICAgICAgIGJvcmRlci10b3A6IDE2cHggc29saWQgcmdiYSgwLCAwLCAwLCAwLjE1KTsNCiAgICAgICAgICAgICAgICAgICAgICAgIGJvcmRlci1sZWZ0OiAxNnB4IHNvbGlkIHRyYW5zcGFyZW50Ow0KICAgICAgICAgICAgICAgICAgICAgICAgYm9yZGVyLXJpZ2h0OiAxNnB4IHNvbGlkIHRyYW5zcGFyZW50Ow0KICAgICAgICAgICAgICAgICAgICB9DQoNCiAgICAgICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkgLm1lc3NhZ2U6YWZ0ZXIgew0KICAgICAgICAgICAgICAgICAgICAgICAgY29udGVudDogJyc7DQogICAgICAgICAgICAgICAgICAgICAgICBwb3NpdGlvbjogYWJzb2x1dGU7DQogICAgICAgICAgICAgICAgICAgICAgICB0b3A6IDA7DQogICAgICAgICAgICAgICAgICAgICAgICBib3JkZXItbGVmdDogMTdweCBzb2xpZCB0cmFuc3BhcmVudDsNCiAgICAgICAgICAgICAgICAgICAgICAgIGJvcmRlci1yaWdodDogMTdweCBzb2xpZCB0cmFuc3BhcmVudDsNCiAgICAgICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpIC5tZXNzYWdlIC50ZXh0X21lc3NhZ2Ugew0KICAgICAgICAgICAgICAgICAgICAgICAgbWFyZ2luOiAwOw0KICAgICAgICAgICAgICAgICAgICAgICAgcGFkZGluZzogMDsNCiAgICAgICAgICAgICAgICAgICAgICAgIHRyYW5zaXRpb246IGFsbCAwLjFzOw0KICAgICAgICAgICAgICAgICAgICB9DQoNCiAgICAgICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkgLm1lc3NhZ2UgLmF0dGFjaG1lbnRzIHsNCiAgICAgICAgICAgICAgICAgICAgICAgIG1hcmdpbjogOXB4IDE2cHggMCAwOw0KICAgICAgICAgICAgICAgICAgICAgICAgcGFkZGluZzogMDsNCiAgICAgICAgICAgICAgICAgICAgICAgIHRyYW5zaXRpb246IGFsbCAwLjFzOw0KICAgICAgICAgICAgICAgICAgICAgICAgZm9udC1zaXplOiAxOXB4Ow0KICAgICAgICAgICAgICAgICAgICAgICAgYm94LXNoYWRvdzogMCA1cHggMTVweCByZ2JhKDAsMCwwLDAuNCk7DQogICAgICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpLnlvdSBhLnVzZXIgew0KICAgICAgICAgICAgICAgICAgICBmbG9hdDogcmlnaHQ7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkueW91IC5kYXRlIHsNCiAgICAgICAgICAgICAgICAgICAgZmxvYXQ6IHJpZ2h0Ow0KICAgICAgICAgICAgICAgICAgICBtYXJnaW46IC0yMHB4IDEwcHggMCAwOw0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpLnlvdSAudXNlcm5hbWUgew0KICAgICAgICAgICAgICAgICAgICBmbG9hdDogcmlnaHQ7DQogICAgICAgICAgICAgICAgICAgIG1hcmdpbjogLTQwcHggMTBweCAwIDA7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkueW91IC5yZXNlbmRkYXRlIHsNCiAgICAgICAgICAgICAgICAgICAgZmxvYXQ6IHJpZ2h0Ow0KICAgICAgICAgICAgICAgICAgICBtYXJnaW46IC0yMHB4IDEwcHggMCAwOw0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpLnlvdSAucmVzZW5kdXNlcm5hbWUgew0KICAgICAgICAgICAgICAgICAgICBmbG9hdDogcmlnaHQ7DQogICAgICAgICAgICAgICAgICAgIG1hcmdpbjogLTQwcHggMTBweCAwIDA7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkueW91IC5tZXNzYWdlIHsNCiAgICAgICAgICAgICAgICAgICAgYmFja2dyb3VuZC1jb2xvcjogIzUxODFiODsNCiAgICAgICAgICAgICAgICAgICAgY29sb3I6ICNmZmY7DQogICAgICAgICAgICAgICAgICAgIG1hcmdpbjogMCA5MHB4IDAgMDsNCiAgICAgICAgICAgICAgICB9DQoNCiAgICAgICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkueW91IC5tZXNzYWdlOmJlZm9yZSB7DQogICAgICAgICAgICAgICAgICAgICAgICBtYXJnaW46IC05cHggLTE2cHggMCAwOw0KICAgICAgICAgICAgICAgICAgICAgICAgcmlnaHQ6IDA7DQogICAgICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgICAgICAuY2hhdCB1bCBsaS55b3UgLm1lc3NhZ2U6YWZ0ZXIgew0KICAgICAgICAgICAgICAgICAgICAgICAgYm9yZGVyLXRvcDogMTdweCBzb2xpZCAjNTE4MWI4Ow0KICAgICAgICAgICAgICAgICAgICAgICAgY29udGVudDogJyc7DQogICAgICAgICAgICAgICAgICAgICAgICByaWdodDogMDsNCiAgICAgICAgICAgICAgICAgICAgICAgIG1hcmdpbjogMCAtMTVweCAwIDA7DQogICAgICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpLm90aGVyIGEudXNlciB7DQogICAgICAgICAgICAgICAgICAgIGZsb2F0OiBsZWZ0Ow0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5jaGF0IHVsIGxpLm90aGVyIC5kYXRlIHsNCiAgICAgICAgICAgICAgICAgICAgZmxvYXQ6IGxlZnQ7DQogICAgICAgICAgICAgICAgICAgIG1hcmdpbjogLTIwcHggMCAwIDEwcHg7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkub3RoZXIgLnVzZXJuYW1lIHsNCiAgICAgICAgICAgICAgICAgICAgZmxvYXQ6IGxlZnQ7DQogICAgICAgICAgICAgICAgICAgIG1hcmdpbjogLTQwcHggMCAwIDEwcHg7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkub3RoZXIgLm1lc3NhZ2Ugew0KICAgICAgICAgICAgICAgICAgICBjb2xvcjogIzY2NjE1YjsNCiAgICAgICAgICAgICAgICAgICAgYmFja2dyb3VuZC1jb2xvcjogI2ZhZmFmYTsNCiAgICAgICAgICAgICAgICAgICAgbWFyZ2luOiAwIDAgMCA5MHB4Ow0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgICAgICAuY2hhdCB1bCBsaS5vdGhlciAubWVzc2FnZTpiZWZvcmUgew0KICAgICAgICAgICAgICAgICAgICAgICAgbWFyZ2luOiAtOXB4IDAgMCAtMTZweDsNCiAgICAgICAgICAgICAgICAgICAgICAgIGxlZnQ6IDA7DQogICAgICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgICAgICAuY2hhdCB1bCBsaS5vdGhlciAubWVzc2FnZTphZnRlciB7DQogICAgICAgICAgICAgICAgICAgICAgICBib3JkZXItdG9wOiAxN3B4IHNvbGlkICNmYWZhZmE7DQogICAgICAgICAgICAgICAgICAgICAgICBjb250ZW50OiAnJzsNCiAgICAgICAgICAgICAgICAgICAgICAgIGxlZnQ6IDA7DQogICAgICAgICAgICAgICAgICAgICAgICBtYXJnaW46IDAgMCAwIC0xNXB4Ow0KICAgICAgICAgICAgICAgICAgICB9DQoNCiAgICAgICAgICAgICAgICAuY2hhdCB1bCBsaSAubWVzc2FnZSAubGluayB7DQogICAgICAgICAgICAgICAgICAgIGRpc3BsYXk6IGlubGluZS1ibG9jazsNCiAgICAgICAgICAgICAgICAgICAgcGFkZGluZzogNXB4IDhweDsNCiAgICAgICAgICAgICAgICAgICAgdGV4dC1kZWNvcmF0aW9uOiBub25lOw0KICAgICAgICAgICAgICAgICAgICBwYWRkaW5nLXRvcDogMXB4Ow0KICAgICAgICAgICAgICAgICAgICBwYWRkaW5nLWJvdHRvbTogMXB4Ow0KICAgICAgICAgICAgICAgICAgICBmb250LXdlaWdodDogYm9sZDsNCiAgICAgICAgICAgICAgICAgICAgYm94LXNoYWRvdzogMCA1cHggMTJweCByZ2JhKDAsMCwwLDAuNik7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkueW91IC5tZXNzYWdlIC5saW5rIHsNCiAgICAgICAgICAgICAgICAgICAgYm9yZGVyLWJvdHRvbTogM3B4IHNvbGlkIHdoaXRlOw0KICAgICAgICAgICAgICAgICAgICBjb2xvcjogd2hpdGU7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkub3RoZXIgLm1lc3NhZ2UgLmxpbmsgew0KICAgICAgICAgICAgICAgICAgICBib3JkZXItYm90dG9tOiAzcHggc29saWQgIzU5QTZGMzsNCiAgICAgICAgICAgICAgICAgICAgY29sb3I6ICM1OUE2RjM7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgICAgICAgICAgLmNoYXQgdWwgbGkgLm1lc3NhZ2UgLmF0dGFjaG1lbnRfdmlkZW8gew0KICAgICAgICAgICAgICAgICAgICB3aWR0aDogMjAwcHg7DQogICAgICAgICAgICAgICAgICAgIGhlaWdodDogMjAwcHg7DQogICAgICAgICAgICAgICAgICAgIGRpc3BsYXk6IGlubGluZS1ibG9jazsNCiAgICAgICAgICAgICAgICB9DQoNCiAgICAgICAgLnByb2dyZXNzaXZlIHsNCiAgICAgICAgICAgIGJvcmRlci1yYWRpdXM6IDQlOw0KICAgICAgICAgICAgYm94LXNoYWRvdzogMCA1cHggMTVweCByZ2JhKDAsMCwwLDAuNik7DQogICAgICAgICAgICBwb3NpdGlvbjogcmVsYXRpdmU7DQogICAgICAgICAgICBkaXNwbGF5OiBpbmxpbmUtYmxvY2s7DQogICAgICAgICAgICBvdmVyZmxvdzogaGlkZGVuOw0KICAgICAgICAgICAgb3V0bGluZTogbm9uZTsNCiAgICAgICAgfQ0KDQogICAgICAgICAgICAucHJvZ3Jlc3NpdmUgLnJlcGxhY2Ugew0KICAgICAgICAgICAgICAgIGJvcmRlci1yYWRpdXM6IDQlOw0KICAgICAgICAgICAgICAgIHBvc2l0aW9uOiByZWxhdGl2ZTsNCiAgICAgICAgICAgICAgICBkaXNwbGF5OiBpbmxpbmUtYmxvY2s7DQogICAgICAgICAgICAgICAgb3ZlcmZsb3c6IGhpZGRlbjsNCiAgICAgICAgICAgICAgICBvdXRsaW5lOiBub25lOw0KICAgICAgICAgICAgfQ0KDQoNCiAgICAgICAgICAgIC5wcm9ncmVzc2l2ZSBpbWcgew0KICAgICAgICAgICAgICAgIHdpZHRoOiAyMDBweDsNCiAgICAgICAgICAgICAgICBoZWlnaHQ6IDIwMHB4Ow0KICAgICAgICAgICAgICAgIGJvcmRlci1yYWRpdXM6IDQlOw0KICAgICAgICAgICAgICAgIG9iamVjdC1maXQ6IGNvdmVyOw0KICAgICAgICAgICAgICAgIGRpc3BsYXk6IGlubGluZS1ibG9jazsNCiAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5wcm9ncmVzc2l2ZSBpbWcucHJldmlldyB7DQogICAgICAgICAgICAgICAgICAgIGZpbHRlcjogYmx1cigydncpOw0KICAgICAgICAgICAgICAgICAgICB0cmFuc2Zvcm06IHNjYWxlKDEuMDUpOw0KICAgICAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgICAgIC5wcm9ncmVzc2l2ZSBpbWcucmV2ZWFsIHsNCiAgICAgICAgICAgICAgICAgICAgcG9zaXRpb246IGFic29sdXRlOw0KICAgICAgICAgICAgICAgICAgICBsZWZ0OiAwOw0KICAgICAgICAgICAgICAgICAgICB0b3A6IDA7DQogICAgICAgICAgICAgICAgICAgIHdpbGwtY2hhbmdlOiB0cmFuc2Zvcm0sIG9wYWNpdHk7DQogICAgICAgICAgICAgICAgICAgIGFuaW1hdGlvbjogcHJvZ3Jlc3NpdmVSZXZlYWwgMXMgZWFzZS1vdXQ7DQogICAgICAgICAgICAgICAgfQ0KDQogICAgICAgIEBrZXlmcmFtZXMgcHJvZ3Jlc3NpdmVSZXZlYWwgew0KICAgICAgICAgICAgMCUgew0KICAgICAgICAgICAgICAgIHRyYW5zZm9ybTogc2NhbGUoMS4wNSk7DQogICAgICAgICAgICAgICAgb3BhY2l0eTogMDsNCiAgICAgICAgICAgIH0NCg0KICAgICAgICAgICAgMTAwJSB7DQogICAgICAgICAgICAgICAgdHJhbnNmb3JtOiBzY2FsZSgxKTsNCiAgICAgICAgICAgICAgICBvcGFjaXR5OiAxOw0KICAgICAgICAgICAgfQ0KICAgICAgICB9DQoNCiAgICAgICAgc2VjdGlvbiB7DQogICAgICAgICAgICBib3gtc2hhZG93OiAwIDVweCAxNXB4IHJnYmEoMCwwLDAsMC42KTsNCiAgICAgICAgfQ0KDQogICAgICAgIC5hdmF0YXJfbWFpbl9pbWFnZSB7DQogICAgICAgICAgICB3aWR0aDogMjAwcHg7DQogICAgICAgICAgICBoZWlnaHQ6IDIwMHB4Ow0KICAgICAgICAgICAgYmFja2dyb3VuZC1pbWFnZTogdXJsKCI8I0FWQVRBUl9VUkwjPiIpOw0KICAgICAgICAgICAgYmFja2dyb3VuZC1zaXplOiBjb3ZlcjsNCiAgICAgICAgICAgIGJhY2tncm91bmQtcG9zaXRpb246IGNlbnRlciBjZW50ZXI7DQogICAgICAgICAgICBiYWNrZ3JvdW5kLXJlcGVhdDogbm8tcmVwZWF0Ow0KICAgICAgICAgICAgYm9yZGVyLXJhZGl1czogNTAlOw0KICAgICAgICAgICAgZGlzcGxheTogaW5saW5lLWJsb2NrOw0KICAgICAgICAgICAgYm94LXNoYWRvdzogN3B4IDdweCA1cHggcmdiYSgwLDAsMCwwLjYpOw0KICAgICAgICB9DQoNCiAgICAgICAgLmF2YXRhcl9tYWluIHsNCiAgICAgICAgICAgIG92ZXJmbG93OiBoaWRkZW47DQogICAgICAgICAgICBtYXJnaW46IDYwcHggYXV0bzsNCiAgICAgICAgICAgIHBhZGRpbmc6IDEwcHggMTVweCAyMHB4IDE1cHg7DQogICAgICAgICAgICBiYWNrZ3JvdW5kLWNvbG9yOiAjNTE4MWI4Ow0KICAgICAgICAgICAgY29sb3I6ICNmZmZmZmY7DQogICAgICAgICAgICB0ZXh0LWFsaWduOiBjZW50ZXI7DQogICAgICAgICAgICB0ZXh0LXNoYWRvdzogM3B4IDFweCA3cHggIzAwMDAwMDsNCiAgICAgICAgfQ0KDQoJLmF2YXRhcl9tYWluIC5wYWdlX25hbWUgew0KCSAgICBmb250LXNpemU6IDMwcHg7DQoJfQ0KDQoJLmF2YXRhcl9tYWluIC5wYWdlX2luZm8gew0KCSAgICBmb250LXNpemU6IDE0cHg7DQoJfQ0KDQogICAgICAgIEBtZWRpYSBzY3JlZW4gYW5kIChvcmllbnRhdGlvbjpwb3J0cmFpdCkgew0KICAgICAgICAgICAgLmF2YXRhcl9tYWluIHsNCiAgICAgICAgICAgICAgICB3aWR0aDogODUlOw0KICAgICAgICAgICAgfQ0KICAgICAgICB9DQoNCiAgICAgICAgQG1lZGlhIHNjcmVlbiBhbmQgKG9yaWVudGF0aW9uOmxhbmRzY2FwZSkgew0KICAgICAgICAgICAgLmF2YXRhcl9tYWluIHsNCiAgICAgICAgICAgICAgICB3aWR0aDogNTUlOw0KICAgICAgICAgICAgfQ0KICAgICAgICB9DQoNCiAgICAgICAgPCNBVkFUQVJTX1NUWUxFIz4NCiAgICA8L3N0eWxlPg0KPC9oZWFkPg0KPGJvZHk+DQogICAgPHNlY3Rpb24gY2xhc3M9ImF2YXRhcl9tYWluIj4NCiAgICAgICAgPGEgaHJlZj0iPCNQQUdFX0xJTksjPiI+PGRpdiBjbGFzcz0iYXZhdGFyX21haW5faW1hZ2UiPjwvZGl2PjwvYT4NCiAgICAgICAgPHAgY2xhc3M9InBhZ2VfbmFtZSI+PCNESUFMT0dfTkFNRSM+PC9wPg0KCTwjUEFHRV9JTkZPIz4NCiAgICA8L3NlY3Rpb24+DQogICAgPHNlY3Rpb24+DQogICAgICAgIDxkaXYgY2xhc3M9ImNoYXQiPg0KICAgICAgICAgICAgPCNNRVNTQUdFUyM+DQogICAgICAgIDwvZGl2Pg0KICAgIDwvc2VjdGlvbj4NCiAgICA8c2NyaXB0Pg0KICAgICAgICB3aW5kb3cuYWRkRXZlbnRMaXN0ZW5lciYmd2luZG93LnJlcXVlc3RBbmltYXRpb25GcmFtZSYmZG9jdW1lbnQuZ2V0RWxlbWVudHNCeUNsYXNzTmFtZSYmd2luZG93LmFkZEV2ZW50TGlzdGVuZXIoJ2xvYWQnLGZ1bmN0aW9uKCl7J3VzZSBzdHJpY3QnO3ZhciBlLHQsbj1kb2N1bWVudC5nZXRFbGVtZW50c0J5Q2xhc3NOYW1lKCdwcm9ncmVzc2l2ZSByZXBsYWNlJyk7ZnVuY3Rpb24gaSgpe3Q9dHx8c2V0VGltZW91dChmdW5jdGlvbigpe3Q9bnVsbCxyKCl9LDMwMCl9ZnVuY3Rpb24gcigpe24ubGVuZ3RoJiZyZXF1ZXN0QW5pbWF0aW9uRnJhbWUoZnVuY3Rpb24oKXtmb3IodmFyIHQsaSxyPXdpbmRvdy5pbm5lckhlaWdodCxhPTA7YTxuLmxlbmd0aDspMDwoaT0odD1uW2FdLmdldEJvdW5kaW5nQ2xpZW50UmVjdCgpKS50b3ApK3QuaGVpZ2h0JiZyPmk/KHMoblthXSksblthXS5jbGFzc0xpc3QucmVtb3ZlKCdyZXBsYWNlJykpOmErKztlPW4ubGVuZ3RofSl9ZnVuY3Rpb24gcyhlLHQpe3ZhciBuPWUmJihlLmdldEF0dHJpYnV0ZSgnZGF0YS1ocmVmJyl8fGUuaHJlZik7aWYobil7dmFyIGk9bmV3IEltYWdlLHI9ZS5kYXRhc2V0O3ImJihyLnNyY3NldCYmKGkuc3Jjc2V0PXIuc3Jjc2V0KSxyLnNpemVzJiYoaS5zaXplcz1yLnNpemVzKSksaS5vbmxvYWQ9ZnVuY3Rpb24oKXtyZXF1ZXN0QW5pbWF0aW9uRnJhbWUoZnVuY3Rpb24oKXtuPT09ZS5ocmVmJiYoZS5zdHlsZS5jdXJzb3I9J2RlZmF1bHQnLGUuYWRkRXZlbnRMaXN0ZW5lcignY2xpY2snLGZ1bmN0aW9uKGUpe2UucHJldmVudERlZmF1bHQoKX0sITEpKTt2YXIgdD1lLnF1ZXJ5U2VsZWN0b3ImJmUucXVlcnlTZWxlY3RvcignaW1nLnByZXZpZXcnKTtlLmluc2VydEJlZm9yZShpLHQmJnQubmV4dFNpYmxpbmcpLmFkZEV2ZW50TGlzdGVuZXIoJ2FuaW1hdGlvbmVuZCcsZnVuY3Rpb24oKXt0JiYodC5hbHQmJihpLmFsdD10LmFsdCksZS5yZW1vdmVDaGlsZCh0KSksaS5jbGFzc0xpc3QucmVtb3ZlKCdyZXZlYWwnKX0pfSl9LCh0PTErKHR8fDApKTwzJiYoaS5vbmVycm9yPWZ1bmN0aW9uKCl7c2V0VGltZW91dChmdW5jdGlvbigpe3MoZSx0KX0sM2UzKnQpfSksaS5jbGFzc05hbWU9J3JldmVhbCcsaS5zcmM9bn19d2luZG93LmFkZEV2ZW50TGlzdGVuZXIoJ3Njcm9sbCcsaSwhMSksd2luZG93LmFkZEV2ZW50TGlzdGVuZXIoJ3Jlc2l6ZScsaSwhMSksTXV0YXRpb25PYnNlcnZlciYmbmV3IE11dGF0aW9uT2JzZXJ2ZXIoZnVuY3Rpb24oKXtuLmxlbmd0aCE9PWUmJnIoKX0pLm9ic2VydmUoZG9jdW1lbnQuYm9keSx7c3VidHJlZTohMCxjaGlsZExpc3Q6ITAsYXR0cmlidXRlczohMCxjaGFyYWN0ZXJEYXRhOiEwfSkscigpfSwhMSk7DQogICAgPC9zY3JpcHQ+DQo8L2JvZHk+DQo8L2h0bWw+"
            val FontPart1 =
                "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"
            val FontPart2 =
                "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"
            var main = readBase64(HTML_MAIN)
            main = Apply("<#DIALOG_NAME#>", peer_title, main)
            main = Apply("<#AVATAR_URL#>", getAvatarUrl(owner, owner_id), main)
            main = Apply(
                "<#PAGE_LINK#>",
                if (owner_id < VKApiMessage.CHAT_PEER) "https://vk.com/" + (if (owner_id < 0) "club" else "id") + abs(
                    owner_id
                ) else "",
                main
            )
            main = Apply(
                "<#PAGE_INFO#>",
                "Created By " + applicationContext.getString(R.string.app_name),
                main
            )
            main = Apply("<#FONT#>", FontPart1 + FontPart2, main)
            val msgs = StringBuilder()
            var messages: List<Message>
            var offset = 0
            while (true) {
                if (isStopped) {
                    if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                        mNotifyManager.cancel(
                            id.toString(),
                            NotificationHelper.NOTIFICATION_DOWNLOADING
                        )
                    }
                    return
                }
                try {
                    messages = messagesRepository.getPeerMessages(
                        account_id,
                        owner_id,
                        200,
                        offset,
                        null,
                        cacheData = false,
                        rev = false
                    ).syncSingle()
                    if (messages.isEmpty()) break
                    mBuilder.setContentTitle(applicationContext.getString(R.string.downloading) + " " + offset)
                    if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                        mNotifyManager.notify(
                            id.toString(),
                            NotificationHelper.NOTIFICATION_DOWNLOADING,
                            mBuilder.build()
                        )
                    }
                    for (i in messages) {
                        if (i.sender == null) continue
                        msgs.append(Build_Message(i, false))
                    }
                    offset += messages.size
                } catch (e: Throwable) {
                    msgs.append(e.localizedMessage)
                    break
                }
            }
            main = Apply("<#AVATARS_STYLE#>", avatars_styles.toString(), main)
            var result_msgs = "<ul><#MESSAGE_LIST#></ul>"
            result_msgs = Apply("<#MESSAGE_LIST#>", msgs.toString(), result_msgs)
            main = Apply("<#MESSAGES#>", result_msgs, main)
            CheckDirectory(Settings.get().main().docDir)
            val html = File(
                Settings.get().main().docDir, makeLegalFilename(
                    peer_title + "_" + DOWNLOAD_DATE_FORMAT.format(
                        Date()
                    ), "html"
                )
            )
            val output: OutputStream = FileOutputStream(html)
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            output.write(bom)
            output.write(main.toByteArray(Charsets.UTF_8))
            output.flush()
            output.close()
            applicationContext.sendBroadcast(
                @Suppress("deprecation")
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(html)
                )
            )
            val intent_open = Intent(Intent.ACTION_VIEW)
            intent_open.setDataAndType(
                FileProvider.getUriForFile(
                    applicationContext, Constants.FILE_PROVIDER_AUTHORITY, html
                ), MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(getFileExtension(html))
            ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val ReadPendingIntent = PendingIntent.getActivity(
                applicationContext,
                peer_title.hashCode(),
                intent_open,
                makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
            )
            mBuilder.setContentIntent(ReadPendingIntent)
            mBuilder.setContentText(
                applicationContext.getString(R.string.success) + " " + applicationContext.getString(
                    R.string.chat
                ) + " " + peer_title
            )
                .setContentTitle(html.absolutePath)
                .setAutoCancel(true)
                .setOngoing(false)
            if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                mNotifyManager.cancel(id.toString(), NotificationHelper.NOTIFICATION_DOWNLOADING)
                mNotifyManager.notify(
                    id.toString(),
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    mBuilder.build()
                )
            }
            inMainThread {
                createCustomToast(
                    applicationContext
                ).showToastSuccessBottom(
                    applicationContext.getString(R.string.success) + " " + applicationContext.getString(
                        R.string.chat
                    ) + " " + peer_title
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            inMainThread {
                createCustomToast(
                    applicationContext
                ).showToastError(e.localizedMessage)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun doJsonDownload(chat_title: String?, account_id: Long, owner_id: Long) {
        try {
            var owner: Owner? = null
            if (owner_id < VKApiMessage.CHAT_PEER) {
                owner = OwnerInfo.getRx(applicationContext, account_id, owner_id)
                    .syncSingleSafe()?.owner
            }
            val peer_title = getTitle(owner, owner_id, chat_title)
            val mBuilder = NotificationCompat.Builder(
                applicationContext,
                AppNotificationChannels.DOWNLOAD_CHANNEL_ID
            )
            mBuilder.setContentTitle(applicationContext.getString(R.string.downloading))
                .setContentText(
                    applicationContext.getString(R.string.downloading) + " " + applicationContext.getString(
                        R.string.chat
                    ) + " " + peer_title
                )
                .setSmallIcon(R.drawable.save)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
            mBuilder.addAction(
                R.drawable.close,
                applicationContext.getString(R.string.cancel),
                WorkManager.getInstance(
                    applicationContext
                ).createCancelPendingIntent(id)
            )
            if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                mNotifyManager.notify(
                    id.toString(),
                    NotificationHelper.NOTIFICATION_DOWNLOADING,
                    mBuilder.build()
                )
            }
            CheckDirectory(Settings.get().main().docDir)
            val html = File(
                Settings.get().main().docDir, makeLegalFilename(
                    peer_title + "_" + DOWNLOAD_DATE_FORMAT.format(
                        Date()
                    ), "json"
                )
            )
            val output = OutputStreamWriter(FileOutputStream(html), Charsets.UTF_8)
            val bom = charArrayOf('\ufeff')
            output.write(bom)
            var offset = 0
            var isFirst = true
            if (owner_id >= VKApiMessage.CHAT_PEER) {
                output.write("{ \"type\": \"chat\", \"chat\": [")
            } else {
                output.write("{ \"type\": \"dialog\", \"dialog\": [")
            }
            while (true) {
                if (isStopped) {
                    if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                        mNotifyManager.cancel(
                            id.toString(),
                            NotificationHelper.NOTIFICATION_DOWNLOADING
                        )
                    }
                    output.flush()
                    output.close()
                    html.delete()
                    return
                }
                try {
                    val messages =
                        messagesRepository.getJsonHistory(account_id, offset, 200, owner_id)
                            .syncSingle()
                    if (messages.isEmpty()) break
                    mBuilder.setContentTitle(applicationContext.getString(R.string.downloading) + " " + offset)
                    if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                        mNotifyManager.notify(
                            id.toString(),
                            NotificationHelper.NOTIFICATION_DOWNLOADING,
                            mBuilder.build()
                        )
                    }
                    for (i in messages) {
                        try {
                            if (isFirst) {
                                isFirst = false
                            } else {
                                output.write(','.code)
                            }
                            output.write(i)
                        } catch (_: Exception) {
                        }
                    }
                    offset += messages.size
                } catch (_: Throwable) {
                    break
                }
            }
            output.write("], \"vk_api_version\": { \"string\": \"" + Constants.API_VERSION + "\", \"float\": " + Constants.API_VERSION + " }, ")
            output.write(
                "\"page_id\": $owner_id, \"page_title\": \"" + makeLegalFilename(
                    peer_title ?: return, null
                ) + "\""
            )
            if (owner_id < VKApiMessage.CHAT_PEER && owner_id >= 0) {
                val own = owner as User?
                output.write(", \"page_avatar\": \"" + (own ?: return).maxSquareAvatar + "\"")
            }
            output.write(" }")
            output.flush()
            output.close()
            applicationContext.sendBroadcast(
                @Suppress("deprecation")
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(html)
                )
            )
            val intent_open = Intent(Intent.ACTION_VIEW)
            intent_open.setDataAndType(
                FileProvider.getUriForFile(
                    applicationContext, Constants.FILE_PROVIDER_AUTHORITY, html
                ), MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(getFileExtension(html))
            ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val ReadPendingIntent = PendingIntent.getActivity(
                applicationContext,
                peer_title.hashCode(),
                intent_open,
                makeMutablePendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
            )
            mBuilder.setContentIntent(ReadPendingIntent)
            mBuilder.setContentText(
                applicationContext.getString(R.string.success) + " " + applicationContext.getString(
                    R.string.chat
                ) + " " + peer_title
            )
                .setContentTitle(html.absolutePath)
                .setAutoCancel(true)
                .setOngoing(false)
            if (AppPerms.hasNotificationPermissionSimple(applicationContext)) {
                mNotifyManager.cancel(id.toString(), NotificationHelper.NOTIFICATION_DOWNLOADING)
                mNotifyManager.notify(
                    id.toString(),
                    NotificationHelper.NOTIFICATION_DOWNLOAD,
                    mBuilder.build()
                )
            }
            inMainThread {
                createCustomToast(
                    applicationContext
                ).showToastSuccessBottom(
                    applicationContext.getString(R.string.success) + " " + applicationContext.getString(
                        R.string.chat
                    ) + " " + peer_title
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            inMainThread {
                createCustomToast(
                    applicationContext
                ).showToastError(e.localizedMessage)
            }
        }
    }

    private fun createForeground() {
        val channel = NotificationChannel(
            "worker_channel",
            applicationContext.getString(R.string.channel_keep_work_manager),
            NotificationManager.IMPORTANCE_NONE
        )
        mNotifyManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(applicationContext, channel.id)
        builder.setContentTitle(applicationContext.getString(R.string.work_manager))
            .setContentText(applicationContext.getString(R.string.foreground_downloader))
            .setSmallIcon(R.drawable.web)
            .setColor("#dd0000".toColor())
            .setOngoing(true)
        setForegroundAsync(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(
                    NotificationHelper.NOTIFICATION_DOWNLOAD_MANAGER,
                    builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(
                    NotificationHelper.NOTIFICATION_DOWNLOAD_MANAGER,
                    builder.build()
                )
            }
        )
    }

    override fun doWork(): Result {
        val owner_id = inputData.getLong(Extra.OWNER_ID, 0)
        val account_id = inputData.getLong(Extra.ACCOUNT_ID, 0)
        var chat_title = inputData.getString(Extra.TITLE)
        val action = inputData.getString(Extra.ACTION)
        if (chat_title.isNullOrEmpty()) chat_title =
            applicationContext.getString(R.string.chat) + " " + owner_id
        if (owner_id == 0L || account_id == 0L) return Result.failure()
        createForeground()
        inMainThread {
            createCustomToast(
                applicationContext
            ).showToastBottom(applicationContext.getString(R.string.do_chat_download))
        }
        if ("html" == action) {
            doDownloadAsHTML(chat_title, account_id, owner_id)
        } else {
            doJsonDownload(chat_title, account_id, owner_id)
        }
        return Result.success()
    }

    companion object {
        internal fun getFileExtension(file: File?): String {
            var extension = ""
            try {
                if (file != null && file.exists()) {
                    val name = file.name
                    extension = name.substring(name.lastIndexOf('.') + 1)
                }
            } catch (_: Exception) {
                extension = ""
            }
            return extension
        }
    }

}