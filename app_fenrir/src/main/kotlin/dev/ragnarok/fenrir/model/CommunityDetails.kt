package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class CommunityDetails : Parcelable {
    var allWallCount = 0
        private set
    var ownerWallCount = 0
        private set
    var postponedWallCount = 0
        private set
    var suggestedWallCount = 0
        private set
    var donutWallCount = 0
        private set
    var canMessage = false
        private set
    var isFavorite = false
        private set
    var isSubscribed = false
        private set
    var topicsCount = 0
        private set
    var docsCount = 0
        private set
    var photosCount = 0
        private set
    var audiosCount = 0
        private set
    var videosCount = 0
        private set
    var articlesCount = 0
        private set
    var productsCount = 0
        private set
    var chatsCount = 0
        private set
    var productServicesCount = 0
        private set
    var narrativesCount = 0
        private set
    var clipsCount = 0
        private set
    var status: String? = null
        private set
    var statusAudio: Audio? = null
        private set
    var cover: Cover? = null
        private set
    var description: String? = null
        private set
    var menu: List<Menu>? = null
        private set

    constructor()
    constructor(parcel: Parcel) {
        allWallCount = parcel.readInt()
        ownerWallCount = parcel.readInt()
        postponedWallCount = parcel.readInt()
        suggestedWallCount = parcel.readInt()
        donutWallCount = parcel.readInt()
        canMessage = parcel.getBoolean()
        isFavorite = parcel.getBoolean()
        isSubscribed = parcel.getBoolean()
        topicsCount = parcel.readInt()
        docsCount = parcel.readInt()
        photosCount = parcel.readInt()
        audiosCount = parcel.readInt()
        videosCount = parcel.readInt()
        articlesCount = parcel.readInt()
        productsCount = parcel.readInt()
        chatsCount = parcel.readInt()
        productServicesCount = parcel.readInt()
        narrativesCount = parcel.readInt()
        clipsCount = parcel.readInt()
        status = parcel.readString()
        statusAudio = parcel.readTypedObjectCompat(Audio.CREATOR)
        cover = parcel.readTypedObjectCompat(Cover.CREATOR)
        description = parcel.readString()
        menu = parcel.createTypedArrayList(Menu.CREATOR)
    }

    fun setProductServicesCount(productServicesCount: Int): CommunityDetails {
        this.productServicesCount = productServicesCount
        return this
    }

    fun setClipsCount(clipsCount: Int): CommunityDetails {
        this.clipsCount = clipsCount
        return this
    }

    fun setNarrativesCount(narrativesCount: Int): CommunityDetails {
        this.narrativesCount = narrativesCount
        return this
    }

    fun setMenu(menu: List<Menu>?): CommunityDetails {
        this.menu = menu
        return this
    }

    fun setCover(cover: Cover?): CommunityDetails {
        this.cover = cover
        return this
    }

    fun setChatsCount(chatsCount: Int): CommunityDetails {
        this.chatsCount = chatsCount
        return this
    }

    fun setFavorite(isFavorite: Boolean): CommunityDetails {
        this.isFavorite = isFavorite
        return this
    }

    fun setSubscribed(isSubscribed: Boolean): CommunityDetails {
        this.isSubscribed = isSubscribed
        return this
    }

    fun setAllWallCount(allWallCount: Int): CommunityDetails {
        this.allWallCount = allWallCount
        return this
    }

    fun setOwnerWallCount(ownerWallCount: Int): CommunityDetails {
        this.ownerWallCount = ownerWallCount
        return this
    }

    fun setPostponedWallCount(postponedWallCount: Int): CommunityDetails {
        this.postponedWallCount = postponedWallCount
        return this
    }

    fun setSuggestedWallCount(suggestedWallCount: Int): CommunityDetails {
        this.suggestedWallCount = suggestedWallCount
        return this
    }

    fun setDonutWallCount(donutWallCount: Int): CommunityDetails {
        this.donutWallCount = donutWallCount
        return this
    }

    fun setCanMessage(canMessage: Boolean): CommunityDetails {
        this.canMessage = canMessage
        return this
    }

    fun setStatusAudio(statusAudio: Audio?): CommunityDetails {
        this.statusAudio = statusAudio
        return this
    }

    fun setStatus(status: String?): CommunityDetails {
        this.status = status
        return this
    }

    fun setTopicsCount(topicsCount: Int): CommunityDetails {
        this.topicsCount = topicsCount
        return this
    }

    fun setDocsCount(docsCount: Int): CommunityDetails {
        this.docsCount = docsCount
        return this
    }

    fun setPhotosCount(photosCount: Int): CommunityDetails {
        this.photosCount = photosCount
        return this
    }

    fun setArticlesCount(articlesCount: Int): CommunityDetails {
        this.articlesCount = articlesCount
        return this
    }

    fun setProductsCount(productsCount: Int): CommunityDetails {
        this.productsCount = productsCount
        return this
    }

    fun setAudiosCount(audiosCount: Int): CommunityDetails {
        this.audiosCount = audiosCount
        return this
    }

    fun setVideosCount(videosCount: Int): CommunityDetails {
        this.videosCount = videosCount
        return this
    }

    fun setDescription(description: String?): CommunityDetails {
        this.description = description
        return this
    }

    class Cover() : Parcelable {
        var enabled = false
            private set
        var images: ArrayList<CoverImage>? = null
            private set

        constructor(parcel: Parcel) : this() {
            enabled = parcel.getBoolean()
            images = parcel.createTypedArrayList(CoverImage.CREATOR)
        }

        fun setImages(images: ArrayList<CoverImage>?): Cover {
            this.images = images
            return this
        }

        fun setEnabled(enabled: Boolean): Cover {
            this.enabled = enabled
            return this
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.putBoolean(enabled)
            parcel.writeTypedList(images)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Cover> {
            override fun createFromParcel(parcel: Parcel): Cover {
                return Cover(parcel)
            }

            override fun newArray(size: Int): Array<Cover?> {
                return arrayOfNulls(size)
            }
        }
    }

    class CoverImage(val url: String?, val height: Int, val width: Int) :
        Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(url)
            parcel.writeInt(height)
            parcel.writeInt(width)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<CoverImage> {
            override fun createFromParcel(parcel: Parcel): CoverImage {
                return CoverImage(parcel)
            }

            override fun newArray(size: Int): Array<CoverImage?> {
                return arrayOfNulls(size)
            }
        }
    }

    class Menu(
        val id: Int,
        val url: String?,
        val title: String?,
        val type: String?,
        val cover: String?
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(id)
            parcel.writeString(url)
            parcel.writeString(title)
            parcel.writeString(type)
            parcel.writeString(cover)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Menu> {
            override fun createFromParcel(parcel: Parcel): Menu {
                return Menu(parcel)
            }

            override fun newArray(size: Int): Array<Menu?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(allWallCount)
        parcel.writeInt(ownerWallCount)
        parcel.writeInt(postponedWallCount)
        parcel.writeInt(suggestedWallCount)
        parcel.writeInt(donutWallCount)
        parcel.putBoolean(canMessage)
        parcel.putBoolean(isFavorite)
        parcel.putBoolean(isSubscribed)
        parcel.writeInt(topicsCount)
        parcel.writeInt(docsCount)
        parcel.writeInt(photosCount)
        parcel.writeInt(audiosCount)
        parcel.writeInt(videosCount)
        parcel.writeInt(articlesCount)
        parcel.writeInt(productsCount)
        parcel.writeInt(chatsCount)
        parcel.writeInt(productServicesCount)
        parcel.writeInt(narrativesCount)
        parcel.writeInt(clipsCount)
        parcel.writeString(status)
        parcel.writeTypedObjectCompat(statusAudio, flags)
        parcel.writeTypedObjectCompat(cover, flags)
        parcel.writeString(description)
        parcel.writeTypedList(menu)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CommunityDetails> {
        override fun createFromParcel(parcel: Parcel): CommunityDetails {
            return CommunityDetails(parcel)
        }

        override fun newArray(size: Int): Array<CommunityDetails?> {
            return arrayOfNulls(size)
        }
    }
}