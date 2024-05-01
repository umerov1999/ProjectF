package dev.ragnarok.fenrir.model

class CommentsBundle(val comments: ArrayList<Comment>) {
    var firstCommentId: Int? = null
        private set
    var lastCommentId: Int? = null
        private set
    var adminLevel: Int? = null
        private set
    var topicPoll: Poll? = null
        private set

    fun setFirstCommentId(firstCommentId: Int?): CommentsBundle {
        this.firstCommentId = firstCommentId
        return this
    }

    fun setLastCommentId(lastCommentId: Int?): CommentsBundle {
        this.lastCommentId = lastCommentId
        return this
    }

    fun setAdminLevel(adminLevel: Int?): CommentsBundle {
        this.adminLevel = adminLevel
        return this
    }

    fun setTopicPoll(topicPoll: Poll?): CommentsBundle {
        this.topicPoll = topicPoll
        return this
    }
}