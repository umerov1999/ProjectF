package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("post")
class PostDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0
        private set
    var dbid = NO_STORED
        private set
    var fromId = 0
        private set
    var date: Long = 0
        private set
    var text: String? = null
        private set
    var replyOwnerId = 0
        private set
    var replyPostId = 0
        private set
    var isFriendsOnly = false
        private set
    var commentsCount = 0
        private set
    var isCanPostComment = false
        private set
    var likesCount = 0
        private set
    var isUserLikes = false
        private set
    var isCanLike = false
        private set
    var isCanEdit = false
        private set
    var isFavorite = false
        private set
    var isDonut = false
        private set
    var isCanPublish = false
        private set
    var repostCount = 0
        private set
    var isUserReposted = false
        private set
    var postType = 0
        private set
    var attachmentsCount = 0
        private set
    var signedId = 0
        private set
    var createdBy = 0
        private set
    var isCanPin = false
        private set
    var isPinned = false
        private set
    var isDeleted = false
        private set
    var views = 0
        private set
    var source: SourceDbo? = null
        private set
    var copyright: CopyrightDboEntity? = null
        private set
    private var attachments: List<DboEntity>? = null
    var copyHierarchy: List<PostDboEntity>? = null
        private set

    operator fun set(id: Int, ownerId: Int): PostDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setCopyright(copyright: CopyrightDboEntity?): PostDboEntity {
        this.copyright = copyright
        return this
    }

    fun setCanPublish(canPublish: Boolean): PostDboEntity {
        isCanPublish = canPublish
        return this
    }

    fun setDbid(dbid: Int): PostDboEntity {
        this.dbid = dbid
        return this
    }

    fun setFromId(fromId: Int): PostDboEntity {
        this.fromId = fromId
        return this
    }

    fun setDate(date: Long): PostDboEntity {
        this.date = date
        return this
    }

    fun setText(text: String?): PostDboEntity {
        this.text = text
        return this
    }

    fun setReplyOwnerId(replyOwnerId: Int): PostDboEntity {
        this.replyOwnerId = replyOwnerId
        return this
    }

    fun setReplyPostId(replyPostId: Int): PostDboEntity {
        this.replyPostId = replyPostId
        return this
    }

    fun setIsDonut(isDonut: Boolean): PostDboEntity {
        this.isDonut = isDonut
        return this
    }

    fun setFriendsOnly(friendsOnly: Boolean): PostDboEntity {
        isFriendsOnly = friendsOnly
        return this
    }

    fun setCommentsCount(commentsCount: Int): PostDboEntity {
        this.commentsCount = commentsCount
        return this
    }

    fun setCanPostComment(canPostComment: Boolean): PostDboEntity {
        isCanPostComment = canPostComment
        return this
    }

    fun setLikesCount(likesCount: Int): PostDboEntity {
        this.likesCount = likesCount
        return this
    }

    fun setUserLikes(userLikes: Boolean): PostDboEntity {
        isUserLikes = userLikes
        return this
    }

    fun setCanLike(canLike: Boolean): PostDboEntity {
        isCanLike = canLike
        return this
    }

    fun setCanEdit(canEdit: Boolean): PostDboEntity {
        isCanEdit = canEdit
        return this
    }

    fun setRepostCount(repostCount: Int): PostDboEntity {
        this.repostCount = repostCount
        return this
    }

    fun setUserReposted(userReposted: Boolean): PostDboEntity {
        isUserReposted = userReposted
        return this
    }

    fun setPostType(postType: Int): PostDboEntity {
        this.postType = postType
        return this
    }

    fun setAttachmentsCount(attachmentsCount: Int): PostDboEntity {
        this.attachmentsCount = attachmentsCount
        return this
    }

    fun setSignedId(signedId: Int): PostDboEntity {
        this.signedId = signedId
        return this
    }

    fun setCreatedBy(createdBy: Int): PostDboEntity {
        this.createdBy = createdBy
        return this
    }

    fun setFavorite(favorite: Boolean): PostDboEntity {
        isFavorite = favorite
        return this
    }

    fun setCanPin(canPin: Boolean): PostDboEntity {
        isCanPin = canPin
        return this
    }

    fun setPinned(pinned: Boolean): PostDboEntity {
        isPinned = pinned
        return this
    }

    fun setDeleted(deleted: Boolean): PostDboEntity {
        isDeleted = deleted
        return this
    }

    fun setViews(views: Int): PostDboEntity {
        this.views = views
        return this
    }

    fun setSource(source: SourceDbo?): PostDboEntity {
        this.source = source
        return this
    }

    fun getAttachments(): List<DboEntity>? {
        return attachments
    }

    fun setAttachments(entities: List<DboEntity>?): PostDboEntity {
        attachments = entities
        return this
    }

    fun setCopyHierarchy(copyHierarchy: List<PostDboEntity>?): PostDboEntity {
        this.copyHierarchy = copyHierarchy
        return this
    }

    @Keep
    @Serializable
    class CopyrightDboEntity(val name: String, val link: String?)

    @Keep
    @Serializable
    class SourceDbo {
        var type = 0
            private set

        var platform: String? = null
            private set

        var data = 0
            private set

        var url: String? = null
            private set

        operator fun set(type: Int, platform: String?, data: Int, url: String?): SourceDbo {
            this.type = type
            this.platform = platform
            this.data = data
            this.url = url
            return this
        }
    }

    companion object {
        const val NO_STORED = -1
    }
}