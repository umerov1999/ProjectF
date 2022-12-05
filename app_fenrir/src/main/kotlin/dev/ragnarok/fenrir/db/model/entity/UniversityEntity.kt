package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class UniversityEntity {
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

    fun setId(id: Int): UniversityEntity {
        this.id = id
        return this
    }

    fun setCountryId(countryId: Int): UniversityEntity {
        this.countryId = countryId
        return this
    }

    fun setCityId(cityId: Int): UniversityEntity {
        this.cityId = cityId
        return this
    }

    fun setName(name: String?): UniversityEntity {
        this.name = name
        return this
    }

    fun setFacultyId(facultyId: Int): UniversityEntity {
        this.facultyId = facultyId
        return this
    }

    fun setFacultyName(facultyName: String?): UniversityEntity {
        this.facultyName = facultyName
        return this
    }

    fun setChairId(chairId: Int): UniversityEntity {
        this.chairId = chairId
        return this
    }

    fun setChairName(chairName: String?): UniversityEntity {
        this.chairName = chairName
        return this
    }

    fun setGraduationYear(graduationYear: Int): UniversityEntity {
        this.graduationYear = graduationYear
        return this
    }

    fun setForm(form: String?): UniversityEntity {
        this.form = form
        return this
    }

    fun setStatus(status: String?): UniversityEntity {
        this.status = status
        return this
    }
}