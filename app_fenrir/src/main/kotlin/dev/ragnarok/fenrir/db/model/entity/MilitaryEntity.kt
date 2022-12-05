package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class MilitaryEntity {
    var unit: String? = null
        private set
    var unitId = 0
        private set
    var countryId = 0
        private set
    var from = 0
        private set
    var until = 0
        private set

    fun setUntil(until: Int): MilitaryEntity {
        this.until = until
        return this
    }

    fun setFrom(from: Int): MilitaryEntity {
        this.from = from
        return this
    }

    fun setCountryId(countryId: Int): MilitaryEntity {
        this.countryId = countryId
        return this
    }

    fun setUnitId(unitId: Int): MilitaryEntity {
        this.unitId = unitId
        return this
    }

    fun setUnit(unit: String?): MilitaryEntity {
        this.unit = unit
        return this
    }
}