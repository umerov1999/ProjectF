package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

class NewsDboEntity : DboEntity() {
    var type: String? = null
        private set
    var sourceId = 0L
        private set
    var date: Long = 0
        private set
    var postId = 0
        private set
    var postType: String? = null
        private set
    var isFinalPost = false
        private set
    var copyOwnerId = 0L
        private set
    var copyPostId = 0
        private set
    var copyPostDate: Long = 0
        private set
    var text: String? = null
        private set
    var isCanEdit = false
        private set
    var isCanDelete = false
        private set
    var commentCount = 0
        private set
    var isCanPostComment = false
        private set
    var likesCount = 0
        private set
    var isUserLikes = false
        private set
    var isCanLike = false
        private set
    var isCanPublish = false
        private set
    var repostCount = 0
        private set
    var isUserReposted = false
        private set
    var friendsTags: List<Long>? = null
        private set
    var views = 0
        private set
    var attachments: List<DboEntity>? = null
        private set
    var copyHistory: List<PostDboEntity>? = null
        private set
    var copyright: CopyrightDboEntity? = null
        private set

    fun setType(type: String?): NewsDboEntity {
        this.type = type
        return this
    }

    fun setCopyright(copyright: CopyrightDboEntity?): NewsDboEntity {
        this.copyright = copyright
        return this
    }

    fun setSourceId(sourceId: Long): NewsDboEntity {
        this.sourceId = sourceId
        return this
    }

    fun setDate(date: Long): NewsDboEntity {
        this.date = date
        return this
    }

    fun setPostId(postId: Int): NewsDboEntity {
        this.postId = postId
        return this
    }

    fun setPostType(postType: String?): NewsDboEntity {
        this.postType = postType
        return this
    }

    fun setFinalPost(finalPost: Boolean): NewsDboEntity {
        isFinalPost = finalPost
        return this
    }

    fun setCopyOwnerId(copyOwnerId: Long): NewsDboEntity {
        this.copyOwnerId = copyOwnerId
        return this
    }

    fun setCopyPostId(copyPostId: Int): NewsDboEntity {
        this.copyPostId = copyPostId
        return this
    }

    fun setCopyPostDate(copyPostDate: Long): NewsDboEntity {
        this.copyPostDate = copyPostDate
        return this
    }

    fun setText(text: String?): NewsDboEntity {
        this.text = text
        return this
    }

    fun setCanEdit(canEdit: Boolean): NewsDboEntity {
        isCanEdit = canEdit
        return this
    }

    fun setCanDelete(canDelete: Boolean): NewsDboEntity {
        isCanDelete = canDelete
        return this
    }

    fun setCommentCount(commentCount: Int): NewsDboEntity {
        this.commentCount = commentCount
        return this
    }

    fun setCanPostComment(canPostComment: Boolean): NewsDboEntity {
        isCanPostComment = canPostComment
        return this
    }

    fun setLikesCount(likesCount: Int): NewsDboEntity {
        this.likesCount = likesCount
        return this
    }

    fun setUserLikes(userLikes: Boolean): NewsDboEntity {
        isUserLikes = userLikes
        return this
    }

    fun setCanLike(canLike: Boolean): NewsDboEntity {
        isCanLike = canLike
        return this
    }

    fun setCanPublish(canPublish: Boolean): NewsDboEntity {
        isCanPublish = canPublish
        return this
    }

    fun setRepostCount(repostCount: Int): NewsDboEntity {
        this.repostCount = repostCount
        return this
    }

    fun setUserReposted(userReposted: Boolean): NewsDboEntity {
        isUserReposted = userReposted
        return this
    }

    fun setFriendsTags(friendsTags: List<Long>?): NewsDboEntity {
        this.friendsTags = friendsTags
        return this
    }

    fun setViews(views: Int): NewsDboEntity {
        this.views = views
        return this
    }

    fun setAttachments(attachments: List<DboEntity>?): NewsDboEntity {
        this.attachments = attachments
        return this
    }

    fun setCopyHistory(copyHistory: List<PostDboEntity>?): NewsDboEntity {
        this.copyHistory = copyHistory
        return this
    }

    @Keep
    @Serializable
    class CopyrightDboEntity(val name: String, val link: String?)
}