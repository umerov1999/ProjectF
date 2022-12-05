package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

class StickerSetEntity(val id: Int) {
    var icon: List<Img>? = null
        private set
    var title: String? = null
        private set
    var isPurchased = false
        private set
    var isPromoted = false
        private set
    var isActive = false
        private set
    var position = 0
        private set
    var stickers: List<StickerDboEntity>? = null
        private set

    fun setIcon(icon: List<Img>?): StickerSetEntity {
        this.icon = icon
        return this
    }

    fun setTitle(title: String?): StickerSetEntity {
        this.title = title
        return this
    }

    fun setPurchased(purchased: Boolean): StickerSetEntity {
        isPurchased = purchased
        return this
    }

    fun setPromoted(promoted: Boolean): StickerSetEntity {
        isPromoted = promoted
        return this
    }

    fun setPosition(position: Int): StickerSetEntity {
        this.position = position
        return this
    }

    fun setActive(active: Boolean): StickerSetEntity {
        isActive = active
        return this
    }

    fun setStickers(stickers: List<StickerDboEntity>?): StickerSetEntity {
        this.stickers = stickers
        return this
    }

    @Keep
    @Serializable
    class Img {
        var url: String? = null
            private set

        var width = 0
            private set

        var height = 0
            private set

        operator fun set(url: String?, width: Int, height: Int): Img {
            this.url = url
            this.width = width
            this.height = height
            return this
        }
    }
}