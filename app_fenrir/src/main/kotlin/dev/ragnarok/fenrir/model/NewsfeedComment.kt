package dev.ragnarok.fenrir.model

class NewsfeedComment(val model: Any) {
    var comment: Comment? = null
        private set

    fun setComment(comment: Comment?): NewsfeedComment {
        this.comment = comment
        return this
    }
}