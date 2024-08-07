package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCity
import dev.ragnarok.fenrir.api.model.VKApiCountry
import dev.ragnarok.fenrir.api.model.database.ChairDto
import dev.ragnarok.fenrir.api.model.database.FacultyDto
import dev.ragnarok.fenrir.api.model.database.SchoolClazzDto
import dev.ragnarok.fenrir.api.model.database.SchoolDto
import dev.ragnarok.fenrir.api.model.database.UniversityDto
import kotlinx.coroutines.flow.Flow

interface IDatabaseApi {
    @CheckResult
    fun getCitiesById(cityIds: Collection<Int>): Flow<List<VKApiCity>>

    @CheckResult
    fun getCountries(
        needAll: Boolean?,
        code: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiCountry>>

    @CheckResult
    fun getSchoolClasses(countryId: Int?): Flow<List<SchoolClazzDto>>

    @CheckResult
    fun getChairs(facultyId: Int, offset: Int?, count: Int?): Flow<Items<ChairDto>>

    @CheckResult
    fun getFaculties(universityId: Int, offset: Int?, count: Int?): Flow<Items<FacultyDto>>

    @CheckResult
    fun getUniversities(
        query: String?, countryId: Int?, cityId: Int?,
        offset: Int?, count: Int?
    ): Flow<Items<UniversityDto>>

    @CheckResult
    fun getSchools(
        query: String?,
        cityId: Int,
        offset: Int?,
        count: Int?
    ): Flow<Items<SchoolDto>>

    @CheckResult
    fun getCities(
        countryId: Int,
        regionId: Int?,
        query: String?,
        needAll: Boolean?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiCity>>
}