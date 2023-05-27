package dev.ragnarok.fenrir.push.message

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
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.getLikesChannel
import dev.ragnarok.fenrir.longpoll.AppNotificationChannels.likesChannelId
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.push.NotificationUtils.configOtherPushNotification
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.makeMutablePendingIntent
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class LikeFCMMessage {
    // key: id, value: like_216143660_photo280186075_456239045, class: class java.lang.String
    // key: url, value: https://vk.com/photo280186075_456239045?access_key=d7d37c46854499dd3f, class: class java.lang.String
    // key: body, value: Umerov Artem liked your photo, class: class java.lang.String
    // key: icon, value: like_24, class: class java.lang.String
    // key: time, value: 1529682146, class: class java.lang.String
    // key: type, value: like, class: class java.lang.String
    // key: badge, value: 1, class: class java.lang.String
    // key: image, value: [{"width":200,"url":"https:\/\/pp.userapi.com\/c844520\/v844520706\/71a39\/nc5YPeh1yEI.jpg","height":200},{"width":100,"url":"https:\/\/pp.userapi.com\/c844520\/v844520706\/71a3a\/pZLtq6sleHo.jpg","height":100},{"width":50,"url":"https:\/\/pp.userapi.com\/c844520\/v844520706\/71a3b\/qoFJrYXVFdc.jpg","height":50}], class: class java.lang.String
    // key: sound, value: 0, class: class java.lang.String
    // key: title, value: Notification, class: class java.lang.String
    // key: to_id, value: 280186075, class: class java.lang.String
    // key: group_id, value: likes, class: class java.lang.String
    // key: context, value: {"feedback":true,"item_id":"456239045","owner_id":"280186075","type":"photo"}, class: class java.lang.String
    private var accountId = 0L
    private var id: String? = null
    private var title: String? = null
    private var from_id = 0L
    private var badge = 0
    private var item_id = 0
    private var owner_id = 0L
    private var like_type: String? = null
    private var reply_id = 0

    private fun notifyImpl(context: Context) {
        var place: Place? = null
        when (like_type) {
            "post" -> {
                place = getPostPreviewPlace(accountId, item_id, owner_id, null)
            }

            "photo" -> {
                val photos = singletonArrayList(
                    Photo().setId(item_id).setOwnerId(owner_id)
                )
                place = getSimpleGalleryPlace(accountId, photos, 0, true)
            }

            "video" -> {
                place = getVideoPreviewPlace(accountId, owner_id, item_id, null, null)
            }

            "post_comment" -> {
                val commented = Commented(item_id, owner_id, CommentedType.POST, null)
                place = getCommentsPlace(accountId, commented, reply_id)
            }

            "photo_comment" -> {
                val commented = Commented(item_id, owner_id, CommentedType.PHOTO, null)
                place = getCommentsPlace(accountId, commented, reply_id)
            }

            "video_comment" -> {
                val commented = Commented(item_id, owner_id, CommentedType.VIDEO, null)
                place = getCommentsPlace(accountId, commented, reply_id)
            }
        }
        if (place == null) {
            return
        }

//        VkPlace parsedPlace = VkPlace.parse(object);
//
//        if (parsedPlace == null) {
//            PersistentLogger.logThrowable("Push issues", new Exception("LikeFCMMessage, UNKNOWN OBJECT: " + object));
//            return;
//        }
//
//        String userName = (stringEmptyIfNull(firstName) + " " + stringEmptyIfNull(lastName)).trim();
//
//        Place place = null;
//        String contentText = null;
//
//        if (parsedPlace instanceof VkPlace.Photo) {
//            VkPlace.Photo photo = (VkPlace.Photo) parsedPlace;
//
//            ArrayList<Photo> photos = singletonArrayList(
//                    new Photo().setId(photo.getPhotoId()).setOwnerId(photo.getOwnerId())
//            );
//
//            place = PlaceFactory.getSimpleGalleryPlace(accountId, photos, 0, true);
//            contentText = context.getString(R.string.push_user_liked_your_photo, userName);
//        } else if (parsedPlace instanceof VkPlace.PhotoComment) {
//            VkPlace.PhotoComment photoComment = (VkPlace.PhotoComment) parsedPlace;
//            Commented commented = new Commented(photoComment.getPhotoId(), photoComment.getOwnerId(), CommentedType.PHOTO, null);
//            place = PlaceFactory.getCommentsPlace(accountId, commented, replyId);
//            contentText = context.getString(R.string.push_user_liked_your_comment_on_the_photo, userName);
//        } else if (parsedPlace instanceof VkPlace.Video) {
//            VkPlace.Video video = (VkPlace.Video) parsedPlace;
//            place = PlaceFactory.getVideoPreviewPlace(accountId, video.getOwnerId(), video.getVideoId(), null);
//            contentText = context.getString(R.string.push_user_liked_your_video, userName);
//        } else if (parsedPlace instanceof VkPlace.VideoComment) {
//            VkPlace.VideoComment videoComment = (VkPlace.VideoComment) parsedPlace;
//            Commented commented = new Commented(videoComment.getVideoId(), videoComment.getOwnerId(), CommentedType.VIDEO, null);
//            place = PlaceFactory.getCommentsPlace(accountId, commented, replyId);
//            contentText = context.getString(R.string.push_user_liked_your_comment_on_the_video, userName);
//        } else if (parsedPlace instanceof VkPlace.WallPost) {
//            VkPlace.WallPost wallPost = (VkPlace.WallPost) parsedPlace;
//            place = PlaceFactory.getPostPreviewPlace(accountId, wallPost.getPostId(), wallPost.getOwnerId(), null);
//            contentText = context.getString(R.string.push_user_liked_your_post, userName);
//        } else if (parsedPlace instanceof VkPlace.WallComment) {
//            VkPlace.WallComment wallComment = (VkPlace.WallComment) parsedPlace;
//            Commented commented = new Commented(wallComment.getPostId(), wallComment.getOwnerId(), CommentedType.POST, null);
//            place = PlaceFactory.getCommentsPlace(accountId, commented, replyId);
//            contentText = context.getString(R.string.push_user_liked_your_comment_on_the_post, userName);
//        }
//
//        if (place == null) {
//            PersistentLogger.logThrowable("Push issues", new Exception("LikeFCMMessage, UNKNOWN PLACE: " + object));
//            return;
//        }
        val nManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (hasOreo()) {
            nManager?.createNotificationChannel(getLikesChannel(context))
        }
        val builder = NotificationCompat.Builder(context, likesChannelId)
            .setSmallIcon(R.drawable.heart)
            .setContentTitle(context.getString(R.string.like_title))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setNumber(badge)
            .setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, place)
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(
            context,
            from_id.hashCode(),
            intent,
            makeMutablePendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        )
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        configOtherPushNotification(notification)
        if (AppPerms.hasNotificationPermissionSimple(context)) {
            nManager?.notify(id, NotificationHelper.NOTIFICATION_LIKE, notification)
        }
    }

    fun notifyIfNeed(context: Context) {
        if (!get()
                .notifications()
                .isLikeNotificationEnable
        ) {
            return
        }
        notifyImpl(context)
    }

    @Serializable
    internal class LikeContext {
        @SerialName("feedback")
        var feedback = 0

        @SerialName("item_id")
        var item_id = 0

        @SerialName("owner_id")
        var owner_id = 0L

        @SerialName("type")
        var type: String? = null

        @SerialName("reply_id")
        var reply_id = 0
    }

    companion object {
        fun fromRemoteMessage(accountId: Long, remote: RemoteMessage): LikeFCMMessage? {
            val message = LikeFCMMessage()
            message.accountId = accountId
            val data = remote.data
            message.id = data["id"]
            message.title = data["title"]
            message.from_id = data["from_id"]?.toLong() ?: return null
            message.badge = data["badge"]?.toInt() ?: 0
            val context: LikeContext =
                kJson.decodeFromString(LikeContext.serializer(), data["context"] ?: return null)
            message.item_id = context.item_id
            message.owner_id = context.owner_id
            message.like_type = context.type
            message.reply_id = context.reply_id
            return message
        }
    }
}
