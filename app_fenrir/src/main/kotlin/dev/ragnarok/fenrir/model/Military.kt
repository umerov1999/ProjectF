package dev.ragnarok.fenrir.model

class Military {
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

    fun setUntil(until: Int): Military {
        this.until = until
        return this
    }

    fun setFrom(from: Int): Military {
        this.from = from
        return this
    }

    fun setCountryId(countryId: Int): Military {
        this.countryId = countryId
        return this
    }

    fun setUnitId(unitId: Int): Military {
        this.unitId = unitId
        return this
    }

    fun setUnit(unit: String?): Military {
        this.unit = unit
        return this
    }
}