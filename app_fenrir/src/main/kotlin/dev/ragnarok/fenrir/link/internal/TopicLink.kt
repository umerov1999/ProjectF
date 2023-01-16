package dev.ragnarok.fenrir.link.internal

class TopicLink : AbsInternalLink() {
    var replyToOwner = 0L
    var topicOwnerId = 0L
    var replyToCommentId = 0
    override fun toString(): String {
        return "TopicLink{" +
                "replyToOwner=" + replyToOwner +
                ", topicOwnerId=" + topicOwnerId +
                ", replyToCommentId=" + replyToCommentId +
                "} " + super.toString()
    }
}