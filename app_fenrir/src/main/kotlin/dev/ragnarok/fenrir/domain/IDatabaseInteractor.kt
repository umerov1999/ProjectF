package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.City
import dev.ragnarok.fenrir.model.database.*
import io.reactivex.rxjava3.core.Single

interface IDatabaseInteractor {
    fun getChairs(accountId: Long, facultyId: Int, count: Int, offset: Int): Single<List<Chair>>
    fun getCountries(accountId: Long, ignoreCache: Boolean): Single<List<Country>>
    fun getCities(
        accountId: Long,
        countryId: Int,
        q: String?,
        needAll: Boolean,
        count: Int,
        offset: Int
    ): Single<List<City>>

    fun getFaculties(
        accountId: Long,
        universityId: Int,
        count: Int,
        offset: Int
    ): Single<List<Faculty>>

    fun getSchoolClasses(accountId: Long, countryId: Int): Single<List<SchoolClazz>>
    fun getSchools(
        accountId: Long,
        cityId: Int,
        q: String?,
        count: Int,
        offset: Int
    ): Single<List<School>>

    fun getUniversities(
        accountId: Long,
        filter: String?,
        cityId: Int?,
        countyId: Int?,
        count: Int,
        offset: Int
    ): Single<List<University>>
}