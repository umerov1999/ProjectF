package dev.ragnarok.fenrir.model

class TopicWithOwner(private val topic: Topic, private val owner: Owner) {
    fun getOwner(): Owner {
        return owner
    }

    fun getTopic(): Topic {
        return topic
    }
}