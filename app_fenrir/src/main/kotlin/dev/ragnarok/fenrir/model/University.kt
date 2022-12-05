package dev.ragnarok.fenrir.model

class University {
    private var id = 0
    private var countryId = 0
    private var cityId = 0
    private var name: String? = null
    private var facultyId = 0
    private var facultyName: String? = null
    private var chairId = 0
    private var chairName: String? = null
    private var graduationYear = 0
    private var form: String? = null
    private var status: String? = null
    fun getId(): Int {
        return id
    }

    fun setId(id: Int): University {
        this.id = id
        return this
    }

    fun getCountryId(): Int {
        return countryId
    }

    fun setCountryId(countryId: Int): University {
        this.countryId = countryId
        return this
    }

    fun getCityId(): Int {
        return cityId
    }

    fun setCityId(cityId: Int): University {
        this.cityId = cityId
        return this
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String?): University {
        this.name = name
        return this
    }

    fun getFacultyId(): Int {
        return facultyId
    }

    fun setFacultyId(facultyId: Int): University {
        this.facultyId = facultyId
        return this
    }

    fun getFacultyName(): String? {
        return facultyName
    }

    fun setFacultyName(facultyName: String?): University {
        this.facultyName = facultyName
        return this
    }

    fun getChairId(): Int {
        return chairId
    }

    fun setChairId(chairId: Int): University {
        this.chairId = chairId
        return this
    }

    fun getChairName(): String? {
        return chairName
    }

    fun setChairName(chairName: String?): University {
        this.chairName = chairName
        return this
    }

    fun getGraduationYear(): Int {
        return graduationYear
    }

    fun setGraduationYear(graduationYear: Int): University {
        this.graduationYear = graduationYear
        return this
    }

    fun getForm(): String? {
        return form
    }

    fun setForm(form: String?): University {
        this.form = form
        return this
    }

    fun getStatus(): String? {
        return status
    }

    fun setStatus(status: String?): University {
        this.status = status
        return this
    }
}