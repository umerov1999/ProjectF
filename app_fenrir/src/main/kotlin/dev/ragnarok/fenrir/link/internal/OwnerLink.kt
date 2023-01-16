package dev.ragnarok.fenrir.link.internal

class OwnerLink(start: Int, end: Int, ownerId: Long, name: String) : AbsInternalLink() {
    val ownerId: Long

    init {
        this.start = start
        this.end = end
        this.ownerId = ownerId
        targetLine = name
    }
}