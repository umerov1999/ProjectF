package dev.ragnarok.fenrir.model

class NewsfeedComment(private val model: Any) {
    private var comment: Comment? = null

    /**
     * @return Photo, Video, Topic or Post
     */
    fun getModel(): Any {
        return model
    }

    fun getComment(): Comment? {
        return comment
    }

    fun setComment(comment: Comment?): NewsfeedComment {
        this.comment = comment
        return this
    }
}