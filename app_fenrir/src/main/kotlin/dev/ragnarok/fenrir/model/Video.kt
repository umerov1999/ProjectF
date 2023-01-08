package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Video : AbsModel, ParcelNative.ParcelableNative {
    var id = 0
        private set
    var ownerId = 0
        private set
    var albumId = 0
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var link: String? = null
        private set
    var date: Long = 0
        private set
    var addingDate: Long = 0
        private set
    var views = 0
        private set
    var player: String? = null
        private set
    var image: String? = null
        private set
    var accessKey: String? = null
        private set
    var commentsCount = 0
        private set
    var isUserLikes = false
        private set
    var likesCount = 0
        private set
    var mp4link240: String? = null
        private set
    var mp4link360: String? = null
        private set
    var mp4link480: String? = null
        private set
    var mp4link720: String? = null
        private set
    var mp4link1080: String? = null
        private set
    var mp4link1440: String? = null
        private set
    var mp4link2160: String? = null
        private set
    var externalLink: String? = null
        private set
    var platform: String? = null
        private set
    var isRepeat = false
        private set
    var duration = 0
        private set
    var privacyView: SimplePrivacy? = null
        private set
    var privacyComment: SimplePrivacy? = null
        private set
    var isCanEdit = false
        private set
    var isCanAdd = false
        private set
    var isCanComment = false
        private set
    var private = false
        private set
    var isCanRepost = false
        private set
    var isFavorite = false
        private set
    var hls: String? = null
        private set
    var live: String? = null
        private set
    var msgId = 0
        private set
    var msgPeerId = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readInt()
        albumId = parcel.readInt()
        title = parcel.readString()
        description = parcel.readString()
        link = parcel.readString()
        date = parcel.readLong()
        addingDate = parcel.readLong()
        views = parcel.readInt()
        player = parcel.readString()
        image = parcel.readString()
        accessKey = parcel.readString()
        commentsCount = parcel.readInt()
        isCanComment = parcel.getBoolean()
        isCanRepost = parcel.getBoolean()
        isUserLikes = parcel.getBoolean()
        likesCount = parcel.readInt()
        mp4link240 = parcel.readString()
        mp4link360 = parcel.readString()
        mp4link480 = parcel.readString()
        mp4link720 = parcel.readString()
        mp4link1080 = parcel.readString()
        mp4link1440 = parcel.readString()
        mp4link2160 = parcel.readString()
        externalLink = parcel.readString()
        hls = parcel.readString()
        live = parcel.readString()
        platform = parcel.readString()
        isRepeat = parcel.getBoolean()
        duration = parcel.readInt()
        privacyView = parcel.readTypedObjectCompat(SimplePrivacy.CREATOR)
        privacyComment = parcel.readTypedObjectCompat(SimplePrivacy.CREATOR)
        isCanEdit = parcel.getBoolean()
        isCanAdd = parcel.getBoolean()
        private = parcel.getBoolean()
        isFavorite = parcel.getBoolean()
        msgId = parcel.readInt()
        msgPeerId = parcel.readInt()
    }

    internal constructor(parcel: ParcelNative) {
        id = parcel.readInt()
        ownerId = parcel.readInt()
        albumId = parcel.readInt()
        title = parcel.readString()
        description = parcel.readString()
        link = parcel.readString()
        date = parcel.readLong()
        addingDate = parcel.readLong()
        views = parcel.readInt()
        player = parcel.readString()
        image = parcel.readString()
        accessKey = parcel.readString()
        commentsCount = parcel.readInt()
        isCanComment = parcel.readBoolean()
        isCanRepost = parcel.readBoolean()
        isUserLikes = parcel.readBoolean()
        likesCount = parcel.readInt()
        mp4link240 = parcel.readString()
        mp4link360 = parcel.readString()
        mp4link480 = parcel.readString()
        mp4link720 = parcel.readString()
        mp4link1080 = parcel.readString()
        mp4link1440 = parcel.readString()
        mp4link2160 = parcel.readString()
        externalLink = parcel.readString()
        hls = parcel.readString()
        live = parcel.readString()
        platform = parcel.readString()
        isRepeat = parcel.readBoolean()
        duration = parcel.readInt()
        privacyView = parcel.readParcelable(SimplePrivacy.NativeCreator)
        privacyComment = parcel.readParcelable(SimplePrivacy.NativeCreator)
        isCanEdit = parcel.readBoolean()
        isCanAdd = parcel.readBoolean()
        private = parcel.readBoolean()
        isFavorite = parcel.readBoolean()
        msgId = parcel.readInt()
        msgPeerId = parcel.readInt()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_VIDEO
    }

    fun setPrivacyView(privacyView: SimplePrivacy?): Video {
        this.privacyView = privacyView
        return this
    }

    fun setPrivacyComment(privacyComment: SimplePrivacy?): Video {
        this.privacyComment = privacyComment
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeInt(albumId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(link)
        parcel.writeLong(date)
        parcel.writeLong(addingDate)
        parcel.writeInt(views)
        parcel.writeString(player)
        parcel.writeString(image)
        parcel.writeString(accessKey)
        parcel.writeInt(commentsCount)
        parcel.putBoolean(isCanComment)
        parcel.putBoolean(isCanRepost)
        parcel.putBoolean(isUserLikes)
        parcel.writeInt(likesCount)
        parcel.writeString(mp4link240)
        parcel.writeString(mp4link360)
        parcel.writeString(mp4link480)
        parcel.writeString(mp4link720)
        parcel.writeString(mp4link1080)
        parcel.writeString(mp4link1440)
        parcel.writeString(mp4link2160)
        parcel.writeString(externalLink)
        parcel.writeString(hls)
        parcel.writeString(live)
        parcel.writeString(platform)
        parcel.putBoolean(isRepeat)
        parcel.writeInt(duration)
        parcel.writeTypedObjectCompat(privacyView, flags)
        parcel.writeTypedObjectCompat(privacyComment, flags)
        parcel.putBoolean(isCanEdit)
        parcel.putBoolean(isCanAdd)
        parcel.putBoolean(private)
        parcel.putBoolean(isFavorite)
        parcel.writeInt(msgId)
        parcel.writeInt(msgPeerId)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeInt(albumId)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(link)
        dest.writeLong(date)
        dest.writeLong(addingDate)
        dest.writeInt(views)
        dest.writeString(player)
        dest.writeString(image)
        dest.writeString(accessKey)
        dest.writeInt(commentsCount)
        dest.writeBoolean(isCanComment)
        dest.writeBoolean(isCanRepost)
        dest.writeBoolean(isUserLikes)
        dest.writeInt(likesCount)
        dest.writeString(mp4link240)
        dest.writeString(mp4link360)
        dest.writeString(mp4link480)
        dest.writeString(mp4link720)
        dest.writeString(mp4link1080)
        dest.writeString(mp4link1440)
        dest.writeString(mp4link2160)
        dest.writeString(externalLink)
        dest.writeString(hls)
        dest.writeString(live)
        dest.writeString(platform)
        dest.writeBoolean(isRepeat)
        dest.writeInt(duration)
        dest.writeParcelable(privacyView)
        dest.writeParcelable(privacyComment)
        dest.writeBoolean(isCanEdit)
        dest.writeBoolean(isCanAdd)
        dest.writeBoolean(private)
        dest.writeBoolean(isFavorite)
        dest.writeInt(msgId)
        dest.writeInt(msgPeerId)
    }

    fun setCanAdd(canAdd: Boolean): Video {
        isCanAdd = canAdd
        return this
    }

    fun setPrivate(_private: Boolean): Video {
        private = _private
        return this
    }

    fun setCanEdit(canEdit: Boolean): Video {
        isCanEdit = canEdit
        return this
    }

    fun setFavorite(favorite: Boolean): Video {
        isFavorite = favorite
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setId(id: Int): Video {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): Video {
        this.ownerId = ownerId
        return this
    }

    fun setAlbumId(albumId: Int): Video {
        this.albumId = albumId
        return this
    }

    fun setTitle(title: String?): Video {
        this.title = title
        return this
    }

    fun setDescription(description: String?): Video {
        this.description = description
        return this
    }

    fun setLink(link: String?): Video {
        this.link = link
        return this
    }

    fun setDate(date: Long): Video {
        this.date = date
        return this
    }

    fun setAddingDate(addingDate: Long): Video {
        this.addingDate = addingDate
        return this
    }

    fun setViews(views: Int): Video {
        this.views = views
        return this
    }

    fun setPlayer(player: String?): Video {
        this.player = player
        return this
    }

    fun setImage(image: String?): Video {
        this.image = image
        return this
    }

    fun setAccessKey(accessKey: String?): Video {
        this.accessKey = accessKey
        return this
    }

    fun setCommentsCount(commentsCount: Int): Video {
        this.commentsCount = commentsCount
        return this
    }

    fun setCanComment(canComment: Boolean): Video {
        isCanComment = canComment
        return this
    }

    fun setCanRepost(canRepost: Boolean): Video {
        isCanRepost = canRepost
        return this
    }

    fun setUserLikes(userLikes: Boolean): Video {
        isUserLikes = userLikes
        return this
    }

    fun setLikesCount(likesCount: Int): Video {
        this.likesCount = likesCount
        return this
    }

    fun setMp4link240(mp4link240: String?): Video {
        this.mp4link240 = mp4link240
        return this
    }

    fun setMp4link360(mp4link360: String?): Video {
        this.mp4link360 = mp4link360
        return this
    }

    fun setMp4link480(mp4link480: String?): Video {
        this.mp4link480 = mp4link480
        return this
    }

    fun setMp4link720(mp4link720: String?): Video {
        this.mp4link720 = mp4link720
        return this
    }

    fun setMp4link1080(mp4link1080: String?): Video {
        this.mp4link1080 = mp4link1080
        return this
    }

    fun setMp4link1440(mp4link1440: String?): Video {
        this.mp4link1440 = mp4link1440
        return this
    }

    fun setMp4link2160(mp4link2160: String?): Video {
        this.mp4link2160 = mp4link2160
        return this
    }

    fun setExternalLink(externalLink: String?): Video {
        this.externalLink = externalLink
        return this
    }

    fun setLive(live: String?): Video {
        this.live = live
        return this
    }

    fun setHls(hls: String?): Video {
        this.hls = hls
        return this
    }

    fun setPlatform(platform: String?): Video {
        this.platform = platform
        return this
    }

    fun setRepeat(repeat: Boolean): Video {
        isRepeat = repeat
        return this
    }

    fun setDuration(duration: Int): Video {
        this.duration = duration
        return this
    }

    fun setMsgId(msgId: Int): Video {
        this.msgId = msgId
        return this
    }

    fun setMsgPeerId(msgPeerId: Int): Video {
        this.msgPeerId = msgPeerId
        return this
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Video> = object : Parcelable.Creator<Video> {
            override fun createFromParcel(parcel: Parcel): Video {
                return Video(parcel)
            }

            override fun newArray(size: Int): Array<Video?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Video> =
            object : ParcelNative.Creator<Video> {
                override fun readFromParcelNative(dest: ParcelNative): Video {
                    return Video(dest)
                }
            }
    }
}