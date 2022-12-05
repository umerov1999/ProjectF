package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IDatabaseApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCity
import dev.ragnarok.fenrir.api.model.VKApiCountry
import dev.ragnarok.fenrir.api.model.database.*
import dev.ragnarok.fenrir.api.services.IDatabaseService
import io.reactivex.rxjava3.core.Single

internal class DatabaseApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IDatabaseApi {
    override fun getCitiesById(cityIds: Collection<Int>): Single<List<VKApiCity>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getCitiesById(join(cityIds, ",") { obj: Any -> obj.toString() })
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCountries(
        needAll: Boolean?,
        code: String?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiCountry>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service.getCountries(integerFromBoolean(needAll), code, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getSchoolClasses(countryId: Int?): Single<List<SchoolClazzDto>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getSchoolClasses(countryId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getChairs(facultyId: Int, offset: Int?, count: Int?): Single<Items<ChairDto>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getChairs(facultyId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getFaculties(
        universityId: Int,
        offset: Int?,
        count: Int?
    ): Single<Items<FacultyDto>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getFaculties(universityId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getUniversities(
        query: String?,
        countryId: Int?,
        cityId: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<UniversityDto>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getUniversities(query, countryId, cityId, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getSchools(
        query: String?,
        cityId: Int,
        offset: Int?,
        count: Int?
    ): Single<Items<SchoolDto>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getSchools(query, cityId, offset, count)
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
    ): Single<Items<VKApiCity>> {
        return provideService(IDatabaseService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getCities(
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