package dev.ragnarok.fenrir.link.internal

class TopicLink : AbsInternalLink() {
    var replyToOwner = 0
    var topicOwnerId = 0
    var replyToCommentId = 0
    override fun toString(): String {
        return "TopicLink{" +
                "replyToOwner=" + replyToOwner +
                ", topicOwnerId=" + topicOwnerId +
                ", replyToCommentId=" + replyToCommentId +
                "} " + super.toString()
    }
}