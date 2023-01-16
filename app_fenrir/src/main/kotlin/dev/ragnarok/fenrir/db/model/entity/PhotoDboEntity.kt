package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("photo")
class PhotoDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var albumId = 0
        private set
    var width = 0
        private set
    var height = 0
        private set
    var text: String? = null
        private set
    var date: Long = 0
        private set
    var isUserLikes = false
        private set
    var likesCount = 0
        private set
    var repostsCount = 0
        private set
    var isCanComment = false
        private set
    var commentsCount = 0
        private set
    var tagsCount = 0
        private set
    var accessKey: String? = null
        private set
    var postId = 0
        private set
    var isDeleted = false
        private set
    var sizes: PhotoSizeEntity? = null
        private set

    operator fun set(id: Int, ownerId: Long): PhotoDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setSizes(sizes: PhotoSizeEntity?): PhotoDboEntity {
        this.sizes = sizes
        return this
    }

    fun setAlbumId(albumId: Int): PhotoDboEntity {
        this.albumId = albumId
        return this
    }

    fun setCanComment(canComment: Boolean): PhotoDboEntity {
        isCanComment = canComment
        return this
    }

    fun setWidth(width: Int): PhotoDboEntity {
        this.width = width
        return this
    }

    fun setHeight(height: Int): PhotoDboEntity {
        this.height = height
        return this
    }

    fun setText(text: String?): PhotoDboEntity {
        this.text = text
        return this
    }

    fun setDate(date: Long): PhotoDboEntity {
        this.date = date
        return this
    }

    fun setUserLikes(userLikes: Boolean): PhotoDboEntity {
        isUserLikes = userLikes
        return this
    }

    fun setLikesCount(likesCount: Int): PhotoDboEntity {
        this.likesCount = likesCount
        return this
    }

    fun setRepostsCount(repostsCount: Int): PhotoDboEntity {
        this.repostsCount = repostsCount
        return this
    }

    fun setCommentsCount(commentsCount: Int): PhotoDboEntity {
        this.commentsCount = commentsCount
        return this
    }

    fun setTagsCount(tagsCount: Int): PhotoDboEntity {
        this.tagsCount = tagsCount
        return this
    }

    fun setAccessKey(accessKey: String?): PhotoDboEntity {
        this.accessKey = accessKey
        return this
    }

    fun setPostId(postId: Int): PhotoDboEntity {
        this.postId = postId
        return this
    }

    fun setDeleted(deleted: Boolean): PhotoDboEntity {
        isDeleted = deleted
        return this
    }
}