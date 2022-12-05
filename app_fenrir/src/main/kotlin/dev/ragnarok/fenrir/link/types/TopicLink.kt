package dev.ragnarok.fenrir.link.types

import kotlin.math.abs

class TopicLink(val topicId: Int, ownerId: Int) : AbsLink(TOPIC) {
    val ownerId: Int = -abs(ownerId)
}