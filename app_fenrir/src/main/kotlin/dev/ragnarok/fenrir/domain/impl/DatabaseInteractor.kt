package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IDatabaseStore
import dev.ragnarok.fenrir.db.model.entity.CountryDboEntity
import dev.ragnarok.fenrir.domain.IDatabaseInteractor
import dev.ragnarok.fenrir.model.City
import dev.ragnarok.fenrir.model.database.Chair
import dev.ragnarok.fenrir.model.database.Country
import dev.ragnarok.fenrir.model.database.Faculty
import dev.ragnarok.fenrir.model.database.School
import dev.ragnarok.fenrir.model.database.SchoolClazz
import dev.ragnarok.fenrir.model.database.University
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Single

class DatabaseInteractor(private val cache: IDatabaseStore, private val networker: INetworker) :
    IDatabaseInteractor {
    override fun getChairs(
        accountId: Long,
        facultyId: Int,
        count: Int,
        offset: Int
    ): Single<List<Chair>> {
        return networker.vkDefault(accountId)
            .database()
            .getChairs(facultyId, offset, count)
            .map { items ->
                val dtos = listEmptyIfNull(items.items)
                val chairs: MutableList<Chair> = ArrayList(dtos.size)
                for (dto in dtos) {
                    chairs.add(Chair(dto.id, dto.title))
                }
                chairs
            }
    }

    override fun getCountries(accountId: Long, ignoreCache: Boolean): Single<List<Country>> {
        return if (ignoreCache) {
            networker.vkDefault(accountId)
                .database()
                .getCountries(true, null, null, 1000)
                .flatMap { items ->
                    val dtos = listEmptyIfNull(items.items)
                    val dbos: MutableList<CountryDboEntity> = ArrayList(dtos.size)
                    val countries: MutableList<Country> = ArrayList(dbos.size)
                    for (dto in dtos) {
                        dbos.add(CountryDboEntity().set(dto.id, dto.title))
                        countries.add(Country(dto.id, dto.title))
                    }
                    if (countries.isEmpty()) {
                        networker.vkDefault(accountId)
                            .database().getCountries(false, "RU,BY,RS", null, 100)
                            .flatMap { itemsg ->
                                val dtosg = listEmptyIfNull(itemsg.items)
                                val dbosg: MutableList<CountryDboEntity> = ArrayList(dtosg.size)
                                val countriesg: MutableList<Country> = ArrayList(dbosg.size)
                                for (dto in dtosg) {
                                    dbosg.add(CountryDboEntity().set(dto.id, dto.title))
                                    countriesg.add(Country(dto.id, dto.title))
                                }
                                cache.storeCountries(accountId, dbosg)
                                    .andThen(Single.just<List<Country>>(countriesg))
                            }
                    } else {
                        cache.storeCountries(accountId, dbos)
                            .andThen(Single.just<List<Country>>(countries))
                    }
                }
        } else cache.getCountries(accountId)
            .flatMap { dbos ->
                if (dbos.isNotEmpty()) {
                    val countries: MutableList<Country> = ArrayList(dbos.size)
                    for (dbo in dbos) {
                        countries.add(Country(dbo.id, dbo.title))
                    }
                    return@flatMap Single.just<List<Country>>(countries)
                }
                getCountries(accountId, true)
            }
    }

    override fun getCities(
        accountId: Long,
        countryId: Int,
        q: String?,
        needAll: Boolean,
        count: Int,
        offset: Int
    ): Single<List<City>> {
        return networker.vkDefault(accountId)
            .database()
            .getCities(countryId, null, q, needAll, offset, count)
            .map { items ->
                val dtos = listEmptyIfNull(items.items)
                val cities: MutableList<City> = ArrayList(dtos.size)
                for (dto in dtos) {
                    cities.add(
                        City(dto.id, dto.title)
                            .setArea(dto.area)
                            .setImportant(dto.important)
                            .setRegion(dto.region)
                    )
                }
                cities
            }
    }

    override fun getFaculties(
        accountId: Long,
        universityId: Int,
        count: Int,
        offset: Int
    ): Single<List<Faculty>> {
        return networker.vkDefault(accountId)
            .database()
            .getFaculties(universityId, offset, count)
            .map { items ->
                val dtos = listEmptyIfNull(items.items)
                val faculties: MutableList<Faculty> = ArrayList(dtos.size)
                for (dto in dtos) {
                    faculties.add(Faculty(dto.id, dto.title))
                }
                faculties
            }
    }

    override fun getSchoolClasses(accountId: Long, countryId: Int): Single<List<SchoolClazz>> {
        return networker.vkDefault(accountId)
            .database()
            .getSchoolClasses(countryId)
            .map { dtos ->
                val clazzes: MutableList<SchoolClazz> = ArrayList(dtos.size)
                for (dto in dtos) {
                    clazzes.add(SchoolClazz(dto.id, dto.title))
                }
                clazzes
            }
    }

    override fun getSchools(
        accountId: Long,
        cityId: Int,
        q: String?,
        count: Int,
        offset: Int
    ): Single<List<School>> {
        return networker.vkDefault(accountId)
            .database()
            .getSchools(q, cityId, offset, count)
            .map { items ->
                val dtos = listEmptyIfNull(items.items)
                val schools: MutableList<School> = ArrayList(dtos.size)
                for (dto in dtos) {
                    schools.add(School(dto.id, dto.title))
                }
                schools
            }
    }

    override fun getUniversities(
        accountId: Long,
        filter: String?,
        cityId: Int?,
        countyId: Int?,
        count: Int,
        offset: Int
    ): Single<List<University>> {
        return networker.vkDefault(accountId)
            .database()
            .getUniversities(filter, countyId, cityId, offset, count)
            .map { items ->
                val dtos = listEmptyIfNull(items.items)
                val universities: MutableList<University> = ArrayList(dtos.size)
                for (dto in dtos) {
                    universities.add(University(dto.id, dto.title))
                }
                universities
            }
    }
}