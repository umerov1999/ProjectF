package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class CommunityDetails : Parcelable {
    private var allWallCount = 0
    private var ownerWallCount = 0
    private var postponedWallCount = 0
    private var suggestedWallCount = 0
    private var donutWallCount = 0
    private var canMessage = false
    private var isFavorite = false
    private var isSubscribed = false
    private var topicsCount = 0
    private var docsCount = 0
    private var photosCount = 0
    private var audiosCount = 0
    private var videosCount = 0
    private var articlesCount = 0
    private var productsCount = 0
    private var chatsCount = 0
    private var productServicesCount = 0
    private var narrativesCount = 0
    private var status: String? = null
    private var statusAudio: Audio? = null
    private var cover: Cover? = null
    private var description: String? = null
    private var menu: List<Menu>? = null

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

    fun setMenu(menu: List<Menu>?): CommunityDetails {
        this.menu = menu
        return this
    }

    fun setNarrativesCount(narrativesCount: Int): CommunityDetails {
        this.narrativesCount = narrativesCount
        return this
    }

    fun getProductServicesCount(): Int {
        return productServicesCount
    }

    fun getNarrativesCount(): Int {
        return narrativesCount
    }

    fun getCover(): Cover? {
        return cover
    }

    fun getMenu(): List<Menu>? {
        return menu
    }

    fun setCover(cover: Cover?): CommunityDetails {
        this.cover = cover
        return this
    }

    fun getChatsCount(): Int {
        return chatsCount
    }

    fun setChatsCount(chatsCount: Int): CommunityDetails {
        this.chatsCount = chatsCount
        return this
    }

    fun isSetFavorite(): Boolean {
        return isFavorite
    }

    fun setFavorite(isFavorite: Boolean): CommunityDetails {
        this.isFavorite = isFavorite
        return this
    }

    fun isSetSubscribed(): Boolean {
        return isSubscribed
    }

    fun setSubscribed(isSubscribed: Boolean): CommunityDetails {
        this.isSubscribed = isSubscribed
        return this
    }

    fun getAllWallCount(): Int {
        return allWallCount
    }

    fun setAllWallCount(allWallCount: Int): CommunityDetails {
        this.allWallCount = allWallCount
        return this
    }

    fun getOwnerWallCount(): Int {
        return ownerWallCount
    }

    fun setOwnerWallCount(ownerWallCount: Int): CommunityDetails {
        this.ownerWallCount = ownerWallCount
        return this
    }

    fun getPostponedWallCount(): Int {
        return postponedWallCount
    }

    fun setPostponedWallCount(postponedWallCount: Int): CommunityDetails {
        this.postponedWallCount = postponedWallCount
        return this
    }

    fun getSuggestedWallCount(): Int {
        return suggestedWallCount
    }

    fun getDonutWallCount(): Int {
        return donutWallCount
    }

    fun setSuggestedWallCount(suggestedWallCount: Int): CommunityDetails {
        this.suggestedWallCount = suggestedWallCount
        return this
    }

    fun setDonutWallCount(donutWallCount: Int): CommunityDetails {
        this.donutWallCount = donutWallCount
        return this
    }

    fun isCanMessage(): Boolean {
        return canMessage
    }

    fun setCanMessage(canMessage: Boolean): CommunityDetails {
        this.canMessage = canMessage
        return this
    }

    fun getStatusAudio(): Audio? {
        return statusAudio
    }

    fun setStatusAudio(statusAudio: Audio?): CommunityDetails {
        this.statusAudio = statusAudio
        return this
    }

    fun getStatus(): String? {
        return status
    }

    fun setStatus(status: String?): CommunityDetails {
        this.status = status
        return this
    }

    fun getTopicsCount(): Int {
        return topicsCount
    }

    fun setTopicsCount(topicsCount: Int): CommunityDetails {
        this.topicsCount = topicsCount
        return this
    }

    fun getDocsCount(): Int {
        return docsCount
    }

    fun setDocsCount(docsCount: Int): CommunityDetails {
        this.docsCount = docsCount
        return this
    }

    fun getPhotosCount(): Int {
        return photosCount
    }

    fun setPhotosCount(photosCount: Int): CommunityDetails {
        this.photosCount = photosCount
        return this
    }

    fun getArticlesCount(): Int {
        return articlesCount
    }

    fun setArticlesCount(articlesCount: Int): CommunityDetails {
        this.articlesCount = articlesCount
        return this
    }

    fun getProductsCount(): Int {
        return productsCount
    }

    fun setProductsCount(productsCount: Int): CommunityDetails {
        this.productsCount = productsCount
        return this
    }

    fun getAudiosCount(): Int {
        return audiosCount
    }

    fun setAudiosCount(audiosCount: Int): CommunityDetails {
        this.audiosCount = audiosCount
        return this
    }

    fun getVideosCount(): Int {
        return videosCount
    }

    fun setVideosCount(videosCount: Int): CommunityDetails {
        this.videosCount = videosCount
        return this
    }

    fun getDescription(): String? {
        return description
    }

    fun setDescription(description: String?): CommunityDetails {
        this.description = description
        return this
    }

    class Cover() : Parcelable {
        private var enabled = false
        private var images: ArrayList<CoverImage>? = null

        constructor(parcel: Parcel) : this() {
            enabled = parcel.getBoolean()
            images = parcel.createTypedArrayList(CoverImage.CREATOR)
        }

        fun getImages(): ArrayList<CoverImage>? {
            return images
        }

        fun setImages(images: ArrayList<CoverImage>?): Cover {
            this.images = images
            return this
        }

        fun isEnabled(): Boolean {
            return enabled
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

    class CoverImage(private val url: String?, private val height: Int, private val width: Int) :
        Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt()
        )

        fun getHeight(): Int {
            return height
        }

        fun getWidth(): Int {
            return width
        }

        fun getUrl(): String? {
            return url
        }

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