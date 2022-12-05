package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.City
import dev.ragnarok.fenrir.model.database.*
import io.reactivex.rxjava3.core.Single

interface IDatabaseInteractor {
    fun getChairs(accountId: Int, facultyId: Int, count: Int, offset: Int): Single<List<Chair>>
    fun getCountries(accountId: Int, ignoreCache: Boolean): Single<List<Country>>
    fun getCities(
        accountId: Int,
        countryId: Int,
        q: String?,
        needAll: Boolean,
        count: Int,
        offset: Int
    ): Single<List<City>>

    fun getFaculties(
        accountId: Int,
        universityId: Int,
        count: Int,
        offset: Int
    ): Single<List<Faculty>>

    fun getSchoolClasses(accountId: Int, countryId: Int): Single<List<SchoolClazz>>
    fun getSchools(
        accountId: Int,
        cityId: Int,
        q: String?,
        count: Int,
        offset: Int
    ): Single<List<School>>

    fun getUniversities(
        accountId: Int,
        filter: String?,
        cityId: Int?,
        countyId: Int?,
        count: Int,
        offset: Int
    ): Single<List<University>>
}