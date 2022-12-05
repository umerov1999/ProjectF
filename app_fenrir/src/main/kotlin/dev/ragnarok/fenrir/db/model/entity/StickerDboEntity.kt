package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("sticker")
class StickerDboEntity : DboEntity() {
    var id = 0
        private set

    var images: List<Img>? = null
        private set

    var imagesWithBackground: List<Img>? = null
        private set

    var animations: List<AnimationEntity>? = null
        private set

    var animationUrl: String? = null
        private set

    fun setId(id: Int): StickerDboEntity {
        this.id = id
        return this
    }

    fun setAnimationUrl(animationUrl: String?): StickerDboEntity {
        this.animationUrl = animationUrl
        return this
    }

    fun setImages(images: List<Img>?): StickerDboEntity {
        this.images = images
        return this
    }

    fun setAnimations(animations: List<AnimationEntity>?): StickerDboEntity {
        this.animations = animations
        return this
    }

    fun setImagesWithBackground(imagesWithBackground: List<Img>?): StickerDboEntity {
        this.imagesWithBackground = imagesWithBackground
        return this
    }

    @Keep
    @Serializable
    class AnimationEntity {
        var type: String? = null
            private set

        var url: String? = null
            private set

        operator fun set(url: String?, type: String?): AnimationEntity {
            this.url = url
            this.type = type
            return this
        }
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