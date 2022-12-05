package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("video")
class VideoDboEntity : DboEntity() {
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
    var hls: String? = null
        private set
    var live: String? = null
        private set
    var platform: String? = null
        private set
    var isRepeat = false
        private set
    var duration = 0
        private set
    var private = false
        private set
    var isFavorite = false
        private set
    var privacyView: PrivacyEntity? = null
        private set
    var privacyComment: PrivacyEntity? = null
        private set
    var isCanEdit = false
        private set
    var isCanAdd = false
        private set
    var isCanComment = false
        private set
    var isCanRepost = false
        private set

    operator fun set(id: Int, ownerId: Int): VideoDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setAlbumId(albumId: Int): VideoDboEntity {
        this.albumId = albumId
        return this
    }

    fun setHls(hls: String?): VideoDboEntity {
        this.hls = hls
        return this
    }

    fun setLive(live: String?): VideoDboEntity {
        this.live = live
        return this
    }

    fun setTitle(title: String?): VideoDboEntity {
        this.title = title
        return this
    }

    fun setDescription(description: String?): VideoDboEntity {
        this.description = description
        return this
    }

    fun setLink(link: String?): VideoDboEntity {
        this.link = link
        return this
    }

    fun setDate(date: Long): VideoDboEntity {
        this.date = date
        return this
    }

    fun setAddingDate(addingDate: Long): VideoDboEntity {
        this.addingDate = addingDate
        return this
    }

    fun setViews(views: Int): VideoDboEntity {
        this.views = views
        return this
    }

    fun setPlayer(player: String?): VideoDboEntity {
        this.player = player
        return this
    }

    fun setImage(image: String?): VideoDboEntity {
        this.image = image
        return this
    }

    fun setAccessKey(accessKey: String?): VideoDboEntity {
        this.accessKey = accessKey
        return this
    }

    fun setCommentsCount(commentsCount: Int): VideoDboEntity {
        this.commentsCount = commentsCount
        return this
    }

    fun setUserLikes(userLikes: Boolean): VideoDboEntity {
        isUserLikes = userLikes
        return this
    }

    fun setLikesCount(likesCount: Int): VideoDboEntity {
        this.likesCount = likesCount
        return this
    }

    fun setMp4link240(mp4link240: String?): VideoDboEntity {
        this.mp4link240 = mp4link240
        return this
    }

    fun setMp4link360(mp4link360: String?): VideoDboEntity {
        this.mp4link360 = mp4link360
        return this
    }

    fun setMp4link480(mp4link480: String?): VideoDboEntity {
        this.mp4link480 = mp4link480
        return this
    }

    fun setMp4link720(mp4link720: String?): VideoDboEntity {
        this.mp4link720 = mp4link720
        return this
    }

    fun setMp4link1080(mp4link1080: String?): VideoDboEntity {
        this.mp4link1080 = mp4link1080
        return this
    }

    fun setMp4link1440(mp4link1440: String?): VideoDboEntity {
        this.mp4link1440 = mp4link1440
        return this
    }

    fun setMp4link2160(mp4link2160: String?): VideoDboEntity {
        this.mp4link2160 = mp4link2160
        return this
    }

    fun setExternalLink(externalLink: String?): VideoDboEntity {
        this.externalLink = externalLink
        return this
    }

    fun setPlatform(platform: String?): VideoDboEntity {
        this.platform = platform
        return this
    }

    fun setRepeat(repeat: Boolean): VideoDboEntity {
        isRepeat = repeat
        return this
    }

    fun setDuration(duration: Int): VideoDboEntity {
        this.duration = duration
        return this
    }

    fun setPrivacyView(privacyView: PrivacyEntity?): VideoDboEntity {
        this.privacyView = privacyView
        return this
    }

    fun setPrivacyComment(privacyComment: PrivacyEntity?): VideoDboEntity {
        this.privacyComment = privacyComment
        return this
    }

    fun setCanEdit(canEdit: Boolean): VideoDboEntity {
        isCanEdit = canEdit
        return this
    }

    fun setCanAdd(canAdd: Boolean): VideoDboEntity {
        isCanAdd = canAdd
        return this
    }

    fun setPrivate(_private: Boolean): VideoDboEntity {
        private = _private
        return this
    }

    fun setCanComment(canComment: Boolean): VideoDboEntity {
        isCanComment = canComment
        return this
    }

    fun setCanRepost(canRepost: Boolean): VideoDboEntity {
        isCanRepost = canRepost
        return this
    }

    fun setFavorite(favorite: Boolean): VideoDboEntity {
        isFavorite = favorite
        return this
    }
}