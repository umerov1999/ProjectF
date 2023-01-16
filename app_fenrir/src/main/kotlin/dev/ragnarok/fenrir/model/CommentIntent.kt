package dev.ragnarok.fenrir.model

class CommentIntent(private val authorId: Long) {
    private var message: String? = null
    private var replyToComment: Int? = null
    private var draftMessageId: Int? = null
    private var stickerId: Int? = null
    private var models: List<AbsModel>? = null
    fun getModels(): List<AbsModel>? {
        return models
    }

    fun setModels(models: List<AbsModel>?): CommentIntent {
        this.models = models
        return this
    }

    fun getDraftMessageId(): Int? {
        return draftMessageId
    }

    fun setDraftMessageId(draftMessageId: Int?): CommentIntent {
        this.draftMessageId = draftMessageId
        return this
    }

    fun getAuthorId(): Long {
        return authorId
    }

    fun getReplyToComment(): Int? {
        return replyToComment
    }

    fun setReplyToComment(replyToComment: Int?): CommentIntent {
        this.replyToComment = replyToComment
        return this
    }

    fun getStickerId(): Int? {
        return stickerId
    }

    fun setStickerId(stickerId: Int?): CommentIntent {
        this.stickerId = stickerId
        return this
    }

    fun getMessage(): String? {
        return message
    }

    fun setMessage(message: String?): CommentIntent {
        this.message = message
        return this
    }
}