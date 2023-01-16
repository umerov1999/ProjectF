package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("market")
class MarketDboEntity : DboEntity() {
    var id = 0
        private set
    var owner_id = 0L
        private set
    var access_key: String? = null
        private set
    var isIs_favorite = false
        private set
    var weight = 0
        private set
    var availability = 0
        private set
    var date: Long = 0
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var price: String? = null
        private set
    var dimensions: String? = null
        private set
    var thumb_photo: String? = null
        private set
    var sku: String? = null
        private set
    var photos: List<PhotoDboEntity>? = null
        private set

    operator fun set(id: Int, owner_id: Long): MarketDboEntity {
        this.id = id
        this.owner_id = owner_id
        return this
    }

    fun setPhotos(photos: List<PhotoDboEntity>?): MarketDboEntity {
        this.photos = photos
        return this
    }

    fun setAccess_key(access_key: String?): MarketDboEntity {
        this.access_key = access_key
        return this
    }

    fun setIs_favorite(is_favorite: Boolean): MarketDboEntity {
        isIs_favorite = is_favorite
        return this
    }

    fun setWeight(weight: Int): MarketDboEntity {
        this.weight = weight
        return this
    }

    fun setAvailability(availability: Int): MarketDboEntity {
        this.availability = availability
        return this
    }

    fun setDate(date: Long): MarketDboEntity {
        this.date = date
        return this
    }

    fun setTitle(title: String?): MarketDboEntity {
        this.title = title
        return this
    }

    fun setDescription(description: String?): MarketDboEntity {
        this.description = description
        return this
    }

    fun setPrice(price: String?): MarketDboEntity {
        this.price = price
        return this
    }

    fun setDimensions(dimensions: String?): MarketDboEntity {
        this.dimensions = dimensions
        return this
    }

    fun setThumb_photo(thumb_photo: String?): MarketDboEntity {
        this.thumb_photo = thumb_photo
        return this
    }

    fun setSku(sku: String?): MarketDboEntity {
        this.sku = sku
        return this
    }
}