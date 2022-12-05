package dev.ragnarok.fenrir.link.internal

open class AbsInternalLink {
    var start = 0
    var end = 0
    var targetLine: String? = null
    override fun toString(): String {
        return "AbsInternalLink{" +
                "start=" + start +
                ", end=" + end +
                ", targetLine='" + targetLine + '\'' +
                '}'
    }
}