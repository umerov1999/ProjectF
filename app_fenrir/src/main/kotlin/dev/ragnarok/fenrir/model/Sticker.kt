package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings.get
import java.io.File
import kotlin.math.abs

class Sticker : AbsModel {
    val id: Int
    var images: List<Image>? = null
        private set
    var imagesWithBackground: List<Image>? = null
        private set
    var animationUrl: String? = null
        private set
    var animations: List<Animation>? = null
        private set

    constructor(id: Int) {
        this.id = id
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        images = `in`.createTypedArrayList(Image.CREATOR)
        imagesWithBackground = `in`.createTypedArrayList(Image.CREATOR)
        animations = `in`.createTypedArrayList(Animation.CREATOR)
        animationUrl = `in`.readString()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_STICKER
    }

    fun getImage(prefSize: Int, isNight: Boolean): Image {
        return if (isNight) getImage(prefSize, imagesWithBackground) else getImage(prefSize, images)
    }

    fun getImage(prefSize: Int, context: Context): Image {
        return getImage(
            prefSize,
            get().ui().isStickers_by_theme && get().ui().isDarkModeEnabled(context)
        )
    }

    fun getImageLight(prefSize: Int): Image {
        return getImage(prefSize, images)
    }

    private fun getImage(prefSize: Int, images: List<Image>?): Image {
        if (images.isNullOrEmpty()) {
            return Image(null, 256, 256)
        }
        var result: Image? = null
        for (image in images) {
            if (result == null) {
                result = image
                continue
            }
            if (abs(image.calcAverageSize() - prefSize) < abs(result.calcAverageSize() - prefSize)) {
                result = image
            }
        }
        return result ?: // default
        images[0]
    }

    fun setAnimationUrl(animationUrl: String?): Sticker {
        this.animationUrl = animationUrl
        return this
    }

    fun getAnimationByType(type: String): String? {
        if (animations.isNullOrEmpty()) {
            return animationUrl
        }
        for (i in animations.orEmpty()) {
            if (type == i.type) {
                return i.url
            }
        }
        return animationUrl
    }

    fun getAnimationByDayNight(context: Context): String? {
        return if (!get().ui().isStickers_by_theme) {
            getAnimationByType("light")
        } else getAnimationByType(
            if (get().ui()
                    .isDarkModeEnabled(context)
            ) "dark" else "light"
        )
    }

    val isAnimated: Boolean
        get() = if (animations.isNullOrEmpty()) {
            animationUrl.nonNullNoEmpty()
        } else true

    fun setImages(images: List<Image>?): Sticker {
        this.images = images
        return this
    }

    fun setAnimations(animations: List<Animation>?): Sticker {
        this.animations = animations
        return this
    }

    fun setImagesWithBackground(imagesWithBackground: List<Image>?): Sticker {
        this.imagesWithBackground = imagesWithBackground
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeTypedList(images)
        parcel.writeTypedList(imagesWithBackground)
        parcel.writeTypedList(animations)
        parcel.writeString(animationUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    class Image : Parcelable {
        val url: String?
        val width: Int
        val height: Int

        constructor(url: String?, width: Int, height: Int) {
            this.url = url
            this.width = width
            this.height = height
        }

        internal constructor(`in`: Parcel) {
            url = `in`.readString()
            width = `in`.readInt()
            height = `in`.readInt()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(url)
            dest.writeInt(width)
            dest.writeInt(height)
        }

        fun calcAverageSize(): Int {
            return (width + height) / 2
        }

        companion object CREATOR : Parcelable.Creator<Image> {
            override fun createFromParcel(parcel: Parcel): Image {
                return Image(parcel)
            }

            override fun newArray(size: Int): Array<Image?> {
                return arrayOfNulls(size)
            }
        }
    }

    class Animation : Parcelable {
        val url: String?
        val type: String?

        constructor(url: String?, type: String?) {
            this.url = url
            this.type = type
        }

        internal constructor(`in`: Parcel) {
            url = `in`.readString()
            type = `in`.readString()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(url)
            dest.writeString(type)
        }

        companion object CREATOR : Parcelable.Creator<Animation> {
            override fun createFromParcel(parcel: Parcel): Animation {
                return Animation(parcel)
            }

            override fun newArray(size: Int): Array<Animation?> {
                return arrayOfNulls(size)
            }
        }
    }

    class LocalSticker(val path: String, val isAnimated: Boolean) {
        val previewPath: String
            get() = "file://$path"
        val animationName: String
            get() = File(path).name.replace(".json", ".lottie")
    }

    companion object CREATOR : Parcelable.Creator<Sticker> {
        override fun createFromParcel(parcel: Parcel): Sticker {
            return Sticker(parcel)
        }

        override fun newArray(size: Int): Array<Sticker?> {
            return arrayOfNulls(size)
        }
    }
}