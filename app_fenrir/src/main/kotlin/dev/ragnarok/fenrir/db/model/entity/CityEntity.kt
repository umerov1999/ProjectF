package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CityEntity {
    var id = 0
        private set
    var title: String? = null
        private set
    var isImportant = false
        private set
    var area: String? = null
        private set
    var region: String? = null
        private set

    fun setId(id: Int): CityEntity {
        this.id = id
        return this
    }

    fun setImportant(important: Boolean): CityEntity {
        isImportant = important
        return this
    }

    fun setArea(area: String?): CityEntity {
        this.area = area
        return this
    }

    fun setRegion(region: String?): CityEntity {
        this.region = region
        return this
    }

    fun setTitle(title: String?): CityEntity {
        this.title = title
        return this
    }
}