package dev.ragnarok.fenrir.model

class CommentsBundle(private val comments: ArrayList<Comment>) {
    private var firstCommentId: Int? = null
    private var lastCommentId: Int? = null
    private var adminLevel: Int? = null
    private var topicPoll: Poll? = null
    fun getComments(): ArrayList<Comment> {
        return comments
    }

    fun getFirstCommentId(): Int? {
        return firstCommentId
    }

    fun setFirstCommentId(firstCommentId: Int?): CommentsBundle {
        this.firstCommentId = firstCommentId
        return this
    }

    fun getLastCommentId(): Int? {
        return lastCommentId
    }

    fun setLastCommentId(lastCommentId: Int?): CommentsBundle {
        this.lastCommentId = lastCommentId
        return this
    }

    fun getAdminLevel(): Int? {
        return adminLevel
    }

    fun setAdminLevel(adminLevel: Int?): CommentsBundle {
        this.adminLevel = adminLevel
        return this
    }

    fun getTopicPoll(): Poll? {
        return topicPoll
    }

    fun setTopicPoll(topicPoll: Poll?): CommentsBundle {
        this.topicPoll = topicPoll
        return this
    }
}