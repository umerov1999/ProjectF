package dev.ragnarok.fenrir.model

class Career {
    var group: Community? = null
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

    fun setCityId(cityId: Int): Career {
        this.cityId = cityId
        return this
    }

    fun setCountryId(countryId: Int): Career {
        this.countryId = countryId
        return this
    }

    fun setFrom(from: Int): Career {
        this.from = from
        return this
    }

    fun setGroup(group: Community?): Career {
        this.group = group
        return this
    }

    fun setUntil(until: Int): Career {
        this.until = until
        return this
    }

    fun setCompany(company: String?): Career {
        this.company = company
        return this
    }

    fun setPosition(position: String?): Career {
        this.position = position
        return this
    }
}