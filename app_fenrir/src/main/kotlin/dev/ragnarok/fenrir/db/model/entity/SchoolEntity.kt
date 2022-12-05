package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class SchoolEntity {
    var id = 0
        private set
    var countryId = 0
        private set
    var cityId = 0
        private set
    var name: String? = null
        private set
    var from = 0
        private set
    var to = 0
        private set
    var yearGraduated = 0
        private set
    var clazz: String? = null
        private set

    fun setId(id: Int): SchoolEntity {
        this.id = id
        return this
    }

    fun setCountryId(countryId: Int): SchoolEntity {
        this.countryId = countryId
        return this
    }

    fun setCityId(cityId: Int): SchoolEntity {
        this.cityId = cityId
        return this
    }

    fun setName(name: String?): SchoolEntity {
        this.name = name
        return this
    }

    fun setFrom(from: Int): SchoolEntity {
        this.from = from
        return this
    }

    fun setTo(to: Int): SchoolEntity {
        this.to = to
        return this
    }

    fun setYearGraduated(yearGraduated: Int): SchoolEntity {
        this.yearGraduated = yearGraduated
        return this
    }

    fun setClazz(clazz: String?): SchoolEntity {
        this.clazz = clazz
        return this
    }
}