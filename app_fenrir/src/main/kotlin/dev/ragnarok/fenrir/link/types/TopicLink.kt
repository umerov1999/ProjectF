package dev.ragnarok.fenrir.link.types

import kotlin.math.abs

class TopicLink(val topicId: Int, ownerId: Long) : AbsLink(TOPIC) {
    val ownerId: Long = -abs(ownerId)
}