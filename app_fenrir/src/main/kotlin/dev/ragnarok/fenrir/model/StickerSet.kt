package dev.ragnarok.fenrir.model

import kotlin.math.abs

class StickerSet(
    val icon: List<Image>?,
    val stickers: List<Sticker>?,
    val title: String?
) {
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
            icon[icon.size - 1].url
        } else result.url
    }

    class Image(val url: String?, val width: Int, val height: Int) {
        fun calcAverageSize(): Int {
            return (width + height) / 2
        }
    }
}