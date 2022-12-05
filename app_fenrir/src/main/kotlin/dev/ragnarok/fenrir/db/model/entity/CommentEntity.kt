package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import dev.ragnarok.fenrir.model.CommentedType
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CommentEntity {
    var sourceId = 0
        private set

    var sourceOwnerId = 0
        private set

    @CommentedType
    var sourceType = 0
        private set

    var sourceAccessKey: String? = null
        private set

    var id = 0
        private set

    var fromId = 0
        private set

    var date: Long = 0
        private set

    var text: String? = null
        private set

    var replyToUserId = 0
        private set

    var replyToComment = 0
        private set

    var likesCount = 0
        private set

    var isUserLikes = false
        private set

    var isCanLike = false
        private set

    var isCanEdit = false
        private set

    var isDeleted = false
        private set

    var attachmentsCount = 0
        private set

    var threadsCount = 0
        private set

    var pid = 0
        private set

    private var attachments: List<DboEntity>? = null

    var threads: List<CommentEntity>? = null
        private set

    operator fun set(
        sourceId: Int,
        sourceOwnerId: Int,
        @CommentedType sourceType: Int,
        sourceAccessKey: String?,
        id: Int
    ): CommentEntity {
        this.sourceId = sourceId
        this.sourceOwnerId = sourceOwnerId
        this.sourceType = sourceType
        this.id = id
        this.sourceAccessKey = sourceAccessKey
        return this
    }

    fun setFromId(fromId: Int): CommentEntity {
        this.fromId = fromId
        return this
    }

    fun setThreads(threads: List<CommentEntity>?): CommentEntity {
        this.threads = threads
        return this
    }

    fun setThreadsCount(threads_count: Int): CommentEntity {
        threadsCount = threads_count
        return this
    }

    fun setDate(date: Long): CommentEntity {
        this.date = date
        return this
    }

    fun setText(text: String?): CommentEntity {
        this.text = text
        return this
    }

    fun setReplyToUserId(replyToUserId: Int): CommentEntity {
        this.replyToUserId = replyToUserId
        return this
    }

    fun setReplyToComment(replyToComment: Int): CommentEntity {
        this.replyToComment = replyToComment
        return this
    }

    fun setLikesCount(likesCount: Int): CommentEntity {
        this.likesCount = likesCount
        return this
    }

    fun setUserLikes(userLikes: Boolean): CommentEntity {
        isUserLikes = userLikes
        return this
    }

    fun setCanLike(canLike: Boolean): CommentEntity {
        isCanLike = canLike
        return this
    }

    fun setCanEdit(canEdit: Boolean): CommentEntity {
        isCanEdit = canEdit
        return this
    }

    fun setDeleted(deleted: Boolean): CommentEntity {
        isDeleted = deleted
        return this
    }

    fun setAttachmentsCount(attachmentsCount: Int): CommentEntity {
        this.attachmentsCount = attachmentsCount
        return this
    }

    fun getAttachments(): List<DboEntity>? {
        return attachments
    }

    fun setAttachments(entities: List<DboEntity>?): CommentEntity {
        attachments = entities
        return this
    }

    fun setPid(pid: Int): CommentEntity {
        this.pid = pid
        return this
    }
}