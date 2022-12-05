package dev.ragnarok.fenrir.model

class School {
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

    fun setId(id: Int): School {
        this.id = id
        return this
    }

    fun setCountryId(countryId: Int): School {
        this.countryId = countryId
        return this
    }

    fun setCityId(cityId: Int): School {
        this.cityId = cityId
        return this
    }

    fun setName(name: String?): School {
        this.name = name
        return this
    }

    fun setFrom(from: Int): School {
        this.from = from
        return this
    }

    fun setTo(to: Int): School {
        this.to = to
        return this
    }

    fun setYearGraduated(yearGraduated: Int): School {
        this.yearGraduated = yearGraduated
        return this
    }

    fun setClazz(clazz: String?): School {
        this.clazz = clazz
        return this
    }
}