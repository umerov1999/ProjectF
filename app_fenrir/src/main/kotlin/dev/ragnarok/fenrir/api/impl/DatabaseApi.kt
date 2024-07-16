package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IDatabaseApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCity
import dev.ragnarok.fenrir.api.model.VKApiCountry
import dev.ragnarok.fenrir.api.model.database.ChairDto
import dev.ragnarok.fenrir.api.model.database.FacultyDto
import dev.ragnarok.fenrir.api.model.database.SchoolClazzDto
import dev.ragnarok.fenrir.api.model.database.SchoolDto
import dev.ragnarok.fenrir.api.model.database.UniversityDto
import dev.ragnarok.fenrir.api.services.IDatabaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class DatabaseApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IDatabaseApi {
    override fun getCitiesById(cityIds: Collection<Int>): Flow<List<VKApiCity>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat { s ->
                s.getCitiesById(join(cityIds, ",") { it.toString() })
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCountries(
        needAll: Boolean?,
        code: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiCountry>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getCountries(integerFromBoolean(needAll), code, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getSchoolClasses(countryId: Int?): Flow<List<SchoolClazzDto>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getSchoolClasses(countryId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getChairs(facultyId: Int, offset: Int?, count: Int?): Flow<Items<ChairDto>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getChairs(facultyId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getFaculties(
        universityId: Int,
        offset: Int?,
        count: Int?
    ): Flow<Items<FacultyDto>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getFaculties(universityId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getUniversities(
        query: String?,
        countryId: Int?,
        cityId: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<UniversityDto>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getUniversities(query, countryId, cityId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getSchools(
        query: String?,
        cityId: Int,
        offset: Int?,
        count: Int?
    ): Flow<Items<SchoolDto>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getSchools(query, cityId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCities(
        countryId: Int,
        regionId: Int?,
        query: String?,
        needAll: Boolean?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiCity>> {
        return provideService(IDatabaseService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getCities(
                    countryId,
                    regionId,
                    query,
                    integerFromBoolean(needAll),
                    offset,
                    count
                )
                    .map(extractResponseWithErrorHandling())
            }
    }
}