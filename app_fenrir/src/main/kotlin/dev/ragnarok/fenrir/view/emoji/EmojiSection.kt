package dev.ragnarok.fenrir.view.emoji

import android.graphics.drawable.Drawable

class EmojiSection(val emojiType: Int, val drawable: Drawable) : AbsSection(TYPE_EMOJI) {
    companion object {
        const val TYPE_PEOPLE = 0
        const val TYPE_NATURE = 1
        const val TYPE_FOOD = 2
        const val TYPE_SPORT = 3
        const val TYPE_CARS = 4
        const val TYPE_ELECTRONICS = 5
        const val TYPE_SYMBOLS = 6
        const val TYPE_MY_STICKERS = 7
    }
}