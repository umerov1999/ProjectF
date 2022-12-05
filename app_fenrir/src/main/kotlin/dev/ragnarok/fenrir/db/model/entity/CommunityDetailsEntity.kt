package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CommunityDetailsEntity {
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
    var isCanMessage = false
        private set
    var isSetFavorite = false
        private set
    var isSetSubscribed = false
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
    var productServicesCount = 0
        private set
    var narrativesCount = 0
        private set
    var chatsCount = 0
        private set
    var status: String? = null
        private set
    var statusAudio: AudioDboEntity? = null
        private set
    var cover: Cover? = null
        private set
    var menu: List<Menu>? = null
        private set
    var description: String? = null
        private set

    fun setProductServicesCount(productServicesCount: Int): CommunityDetailsEntity {
        this.productServicesCount = productServicesCount
        return this
    }

    fun setNarrativesCount(narrativesCount: Int): CommunityDetailsEntity {
        this.narrativesCount = narrativesCount
        return this
    }

    fun setMenu(menu: List<Menu>?): CommunityDetailsEntity {
        this.menu = menu
        return this
    }

    fun setCover(cover: Cover?): CommunityDetailsEntity {
        this.cover = cover
        return this
    }

    fun setChatsCount(chatsCount: Int): CommunityDetailsEntity {
        this.chatsCount = chatsCount
        return this
    }

    fun setDonutWallCount(donutWallCount: Int): CommunityDetailsEntity {
        this.donutWallCount = donutWallCount
        return this
    }

    fun setAllWallCount(allWallCount: Int): CommunityDetailsEntity {
        this.allWallCount = allWallCount
        return this
    }

    fun setOwnerWallCount(ownerWallCount: Int): CommunityDetailsEntity {
        this.ownerWallCount = ownerWallCount
        return this
    }

    fun setPostponedWallCount(postponedWallCount: Int): CommunityDetailsEntity {
        this.postponedWallCount = postponedWallCount
        return this
    }

    fun setSuggestedWallCount(suggestedWallCount: Int): CommunityDetailsEntity {
        this.suggestedWallCount = suggestedWallCount
        return this
    }

    fun setCanMessage(canMessage: Boolean): CommunityDetailsEntity {
        isCanMessage = canMessage
        return this
    }

    fun setFavorite(isFavorite: Boolean): CommunityDetailsEntity {
        isSetFavorite = isFavorite
        return this
    }

    fun setSubscribed(isSubscribed: Boolean): CommunityDetailsEntity {
        isSetSubscribed = isSubscribed
        return this
    }

    fun setStatusAudio(statusAudio: AudioDboEntity?): CommunityDetailsEntity {
        this.statusAudio = statusAudio
        return this
    }

    fun setStatus(status: String?): CommunityDetailsEntity {
        this.status = status
        return this
    }

    fun setDescription(description: String?): CommunityDetailsEntity {
        this.description = description
        return this
    }

    fun setTopicsCount(topicsCount: Int): CommunityDetailsEntity {
        this.topicsCount = topicsCount
        return this
    }

    fun setDocsCount(docsCount: Int): CommunityDetailsEntity {
        this.docsCount = docsCount
        return this
    }

    fun setArticlesCount(articlesCount: Int): CommunityDetailsEntity {
        this.articlesCount = articlesCount
        return this
    }

    fun setProductsCount(productsCount: Int): CommunityDetailsEntity {
        this.productsCount = productsCount
        return this
    }

    fun setPhotosCount(photosCount: Int): CommunityDetailsEntity {
        this.photosCount = photosCount
        return this
    }

    fun setAudiosCount(audiosCount: Int): CommunityDetailsEntity {
        this.audiosCount = audiosCount
        return this
    }

    fun setVideosCount(videosCount: Int): CommunityDetailsEntity {
        this.videosCount = videosCount
        return this
    }

    @Keep
    @Serializable
    class Cover {
        var isEnabled = false
            private set
        var images: ArrayList<CoverImage>? = null
            private set

        fun setImages(images: ArrayList<CoverImage>?): Cover {
            this.images = images
            return this
        }

        fun setEnabled(enabled: Boolean): Cover {
            isEnabled = enabled
            return this
        }
    }

    @Keep
    @Serializable
    class CoverImage {
        var url: String? = null
            private set
        var height = 0
            private set
        var width = 0
            private set

        operator fun set(url: String?, height: Int, width: Int): CoverImage {
            this.url = url
            this.height = height
            this.width = width
            return this
        }
    }

    @Keep
    @Serializable
    class Menu {
        var id = 0
            private set
        var url: String? = null
            private set
        var title: String? = null
            private set
        var type: String? = null
            private set
        var cover: String? = null
            private set

        operator fun set(
            id: Int,
            url: String?,
            title: String?,
            type: String?,
            cover: String?
        ): Menu {
            this.id = id
            this.url = url
            this.title = title
            this.type = type
            this.cover = cover
            return this
        }
    }
}