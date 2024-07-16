package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.City
import dev.ragnarok.fenrir.model.database.Chair
import dev.ragnarok.fenrir.model.database.Country
import dev.ragnarok.fenrir.model.database.Faculty
import dev.ragnarok.fenrir.model.database.School
import dev.ragnarok.fenrir.model.database.SchoolClazz
import dev.ragnarok.fenrir.model.database.University
import kotlinx.coroutines.flow.Flow

interface IDatabaseInteractor {
    fun getChairs(accountId: Long, facultyId: Int, count: Int, offset: Int): Flow<List<Chair>>
    fun getCountries(accountId: Long, ignoreCache: Boolean): Flow<List<Country>>
    fun getCities(
        accountId: Long,
        countryId: Int,
        q: String?,
        needAll: Boolean,
        count: Int,
        offset: Int
    ): Flow<List<City>>

    fun getFaculties(
        accountId: Long,
        universityId: Int,
        count: Int,
        offset: Int
    ): Flow<List<Faculty>>

    fun getSchoolClasses(accountId: Long, countryId: Int): Flow<List<SchoolClazz>>
    fun getSchools(
        accountId: Long,
        cityId: Int,
        q: String?,
        count: Int,
        offset: Int
    ): Flow<List<School>>

    fun getUniversities(
        accountId: Long,
        filter: String?,
        cityId: Int?,
        countyId: Int?,
        count: Int,
        offset: Int
    ): Flow<List<University>>
}