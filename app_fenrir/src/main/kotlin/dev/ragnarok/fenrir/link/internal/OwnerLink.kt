package dev.ragnarok.fenrir.link.internal

class OwnerLink(start: Int, end: Int, ownerId: Int, name: String) : AbsInternalLink() {
    val ownerId: Int

    init {
        this.start = start
        this.end = end
        this.ownerId = ownerId
        targetLine = name
    }
}