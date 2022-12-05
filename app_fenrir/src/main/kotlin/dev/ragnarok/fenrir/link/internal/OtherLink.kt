package dev.ragnarok.fenrir.link.internal

class OtherLink(start: Int, end: Int, link: String, name: String) : AbsInternalLink() {
    val Link: String

    init {
        this.start = start
        this.end = end
        targetLine = name
        Link = link
    }
}