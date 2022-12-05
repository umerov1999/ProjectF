package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("geo")
class GeoDboEntity : DboEntity() {
    var latitude: String? = null
        private set
    var longitude: String? = null
        private set
    var title: String? = null
        private set
    var address: String? = null
        private set
    var country: Int = 0
        private set
    var id: Int = 0
        private set

    fun setLatitude(latitude: String?): GeoDboEntity {
        this.latitude = latitude
        return this
    }

    fun setLongitude(longitude: String?): GeoDboEntity {
        this.longitude = longitude
        return this
    }

    fun setTitle(title: String?): GeoDboEntity {
        this.title = title
        return this
    }

    fun setAddress(address: String?): GeoDboEntity {
        this.address = address
        return this
    }

    fun setCountry(country: Int): GeoDboEntity {
        this.country = country
        return this
    }

    fun setId(id: Int): GeoDboEntity {
        this.id = id
        return this
    }
}
