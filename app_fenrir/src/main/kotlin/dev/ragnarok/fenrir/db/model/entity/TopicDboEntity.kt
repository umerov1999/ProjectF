package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("topic")
class TopicDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0
        private set
    var title: String? = null
        private set
    var createdTime: Long = 0
        private set
    var creatorId = 0
        private set
    var lastUpdateTime: Long = 0
        private set
    var updatedBy = 0
        private set
    var isClosed = false
        private set
    var isFixed = false
        private set
    var commentsCount = 0
        private set
    var firstComment: String? = null
        private set
    var lastComment: String? = null
        private set
    var poll: PollDboEntity? = null
        private set

    operator fun set(id: Int, ownerId: Int): TopicDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setTitle(title: String?): TopicDboEntity {
        this.title = title
        return this
    }

    fun setCreatedTime(createdTime: Long): TopicDboEntity {
        this.createdTime = createdTime
        return this
    }

    fun setCreatorId(creatorId: Int): TopicDboEntity {
        this.creatorId = creatorId
        return this
    }

    fun setLastUpdateTime(lastUpdateTime: Long): TopicDboEntity {
        this.lastUpdateTime = lastUpdateTime
        return this
    }

    fun setUpdatedBy(updatedBy: Int): TopicDboEntity {
        this.updatedBy = updatedBy
        return this
    }

    fun setClosed(closed: Boolean): TopicDboEntity {
        isClosed = closed
        return this
    }

    fun setFixed(fixed: Boolean): TopicDboEntity {
        isFixed = fixed
        return this
    }

    fun setCommentsCount(commentsCount: Int): TopicDboEntity {
        this.commentsCount = commentsCount
        return this
    }

    fun setFirstComment(firstComment: String?): TopicDboEntity {
        this.firstComment = firstComment
        return this
    }

    fun setLastComment(lastComment: String?): TopicDboEntity {
        this.lastComment = lastComment
        return this
    }

    fun setPoll(poll: PollDboEntity?): TopicDboEntity {
        this.poll = poll
        return this
    }
}