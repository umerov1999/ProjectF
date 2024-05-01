package dev.ragnarok.fenrir.model

class University {
    var id = 0
        private set
    var countryId = 0
        private set
    var cityId = 0
        private set
    var name: String? = null
        private set
    var facultyId = 0
        private set
    var facultyName: String? = null
        private set
    var chairId = 0
        private set
    var chairName: String? = null
        private set
    var graduationYear = 0
        private set
    var form: String? = null
        private set
    var status: String? = null
        private set

    fun setId(id: Int): University {
        this.id = id
        return this
    }

    fun setCountryId(countryId: Int): University {
        this.countryId = countryId
        return this
    }

    fun setCityId(cityId: Int): University {
        this.cityId = cityId
        return this
    }

    fun setName(name: String?): University {
        this.name = name
        return this
    }

    fun setFacultyId(facultyId: Int): University {
        this.facultyId = facultyId
        return this
    }

    fun setFacultyName(facultyName: String?): University {
        this.facultyName = facultyName
        return this
    }

    fun setChairId(chairId: Int): University {
        this.chairId = chairId
        return this
    }

    fun setChairName(chairName: String?): University {
        this.chairName = chairName
        return this
    }

    fun setGraduationYear(graduationYear: Int): University {
        this.graduationYear = graduationYear
        return this
    }

    fun setForm(form: String?): University {
        this.form = form
        return this
    }

    fun setStatus(status: String?): University {
        this.status = status
        return this
    }
}