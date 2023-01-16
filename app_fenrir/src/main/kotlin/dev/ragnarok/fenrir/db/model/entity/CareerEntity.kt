package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CareerEntity {
    var groupId = 0L
        private set
    var company: String? = null
        private set
    var countryId = 0
        private set
    var cityId = 0
        private set
    var from = 0
        private set
    var until = 0
        private set
    var position: String? = null
        private set

    fun setCityId(cityId: Int): CareerEntity {
        this.cityId = cityId
        return this
    }

    fun setCountryId(countryId: Int): CareerEntity {
        this.countryId = countryId
        return this
    }

    fun setFrom(from: Int): CareerEntity {
        this.from = from
        return this
    }

    fun setGroupId(groupId: Long): CareerEntity {
        this.groupId = groupId
        return this
    }

    fun setUntil(until: Int): CareerEntity {
        this.until = until
        return this
    }

    fun setCompany(company: String?): CareerEntity {
        this.company = company
        return this
    }

    fun setPosition(position: String?): CareerEntity {
        this.position = position
        return this
    }
}