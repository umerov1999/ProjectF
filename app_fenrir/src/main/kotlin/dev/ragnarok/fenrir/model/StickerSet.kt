package dev.ragnarok.fenrir.model

import kotlin.math.abs

class StickerSet(
    private val icon: List<Image>?,
    private val stickers: List<Sticker>?,
    private val title: String?
) {
    fun getStickers(): List<Sticker>? {
        return stickers
    }

    fun getTitle(): String? {
        return title
    }

    fun getIcon(): List<Image>? {
        return icon
    }

    fun getImageUrl(prefSize: Int): String? {
        if (icon.isNullOrEmpty()) {
            return null
        }
        var result: Image? = null
        for (image in icon) {
            if (result == null) {
                result = image
                continue
            }
            if (abs(image.calcAverageSize() - prefSize) < abs(result.calcAverageSize() - prefSize)) {
                result = image
            }
        }
        return if (result == null) {
            // default
            icon[0].url
        } else result.url
    }

    class Image(val url: String?, private val width: Int, private val height: Int) {
        fun getWidth(): Int {
            return width
        }

        fun getHeight(): Int {
            return height
        }

        fun calcAverageSize(): Int {
            return (width + height) / 2
        }
    }
}