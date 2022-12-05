package dev.ragnarok.fenrir.view.emoji

open class AbsSection(val type: Int) {
    var active = false

    companion object {
        const val TYPE_EMOJI = 0
        const val TYPE_STICKER = 1
        const val TYPE_PHOTO_ALBUM = 3
    }
}