package dev.ragnarok.fenrir.model

class CommunityDetails {
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

    class Cover {
        private var enabled = false
        private var images: ArrayList<CoverImage>? = null
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
    }

    class CoverImage(private val url: String?, private val height: Int, private val width: Int) {
        fun getHeight(): Int {
            return height
        }

        fun getWidth(): Int {
            return width
        }

        fun getUrl(): String? {
            return url
        }
    }

    class Menu(
        val id: Int,
        val url: String?,
        val title: String?,
        val type: String?,
        val cover: String?
    )
}