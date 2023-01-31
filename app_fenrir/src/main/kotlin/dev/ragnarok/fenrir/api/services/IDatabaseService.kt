package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCity
import dev.ragnarok.fenrir.api.model.VKApiCountry
import dev.ragnarok.fenrir.api.model.database.ChairDto
import dev.ragnarok.fenrir.api.model.database.FacultyDto
import dev.ragnarok.fenrir.api.model.database.SchoolClazzDto
import dev.ragnarok.fenrir.api.model.database.SchoolDto
import dev.ragnarok.fenrir.api.model.database.UniversityDto
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IDatabaseService : IServiceRest() {
    //https://vk.com/dev/database.getCitiesById
    fun getCitiesById(cityIds: String?): Single<BaseResponse<List<VKApiCity>>> {
        return rest.request(
            "database.getCities",
            form("city_ids" to cityIds),
            baseList(VKApiCity.serializer())
        )
    }

    /**
     * Returns a list of countries.
     *
     * @param needAll 1 — to return a full list of all countries
     * 0 — to return a list of countries near the current user's country (default).
     * @param code    Country codes in ISO 3166-1 alpha-2 standard.
     * @param offset  Offset needed to return a specific subset of countries.
     * @param count   Number of countries to return. Default 100, maximum value 1000
     * @return Returns the total results number in count field and an array of objects describing countries in items field
     */
    fun getCountries(
        needAll: Int?,
        code: String?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiCountry>>> {
        return rest.request(
            "database.getCountries",
            form("need_all" to needAll, "code" to code, "offset" to offset, "count" to count),
            items(VKApiCountry.serializer())
        )
    }

    /**
     * Returns a list of school classes specified for the country.
     *
     * @param countryId Country ID.
     * @return Returns an array of objects, each of them is a pair of class ID and definition.
     */
    fun getSchoolClasses(countryId: Int?): Single<BaseResponse<List<SchoolClazzDto>>> {
        return rest.request(
            "database.getSchoolClasses",
            form("country_id" to countryId),
            baseList(SchoolClazzDto.serializer())
        )
    }

    /**
     * Returns list of chairs on a specified faculty.
     *
     * @param facultyId id of the faculty to get chairs from
     * @param offset    offset required to get a certain subset of chairs
     * @param count     amount of chairs to get. Default 100, maximum value 10000
     * @return the total results number in count field and an array of objects describing chairs in items field
     */
    fun getChairs(
        facultyId: Int,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<ChairDto>>> {
        return rest.request(
            "database.getChairs",
            form("faculty_id" to facultyId, "offset" to offset, "count" to count),
            items(ChairDto.serializer())
        )
    }

    /**
     * Returns a list of faculties (i.e., university departments).
     *
     * @param universityId University ID.
     * @param offset       Offset needed to return a specific subset of faculties.
     * @param count        Number of faculties to return. Default 100, maximum value 10000
     * @return the total results number in count field and an array
     * of objects describing faculties in items field
     */
    fun getFaculties(
        universityId: Int,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<FacultyDto>>> {
        return rest.request(
            "database.getFaculties",
            form("university_id" to universityId, "offset" to offset, "count" to count),
            items(FacultyDto.serializer())
        )
    }

    /**
     * Returns a list of higher education institutions.
     *
     * @param query     Search query.
     * @param countryId Country ID.
     * @param cityId    City ID.
     * @param offset    Offset needed to return a specific subset of universities.
     * @param count     Number of universities to return. Default 100, maximum value 10000
     * @return an array of objects describing universities
     */
    fun getUniversities(
        query: String?,
        countryId: Int?,
        cityId: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<UniversityDto>>> {
        return rest.request(
            "database.getUniversities", form(
                "q" to query,
                "country_id" to countryId,
                "city_id" to cityId,
                "offset" to offset,
                "count" to count
            ), items(UniversityDto.serializer())
        )
    }

    /**
     * Returns a list of schools.
     *
     * @param query  Search query.
     * @param cityId City ID.
     * @param offset Offset needed to return a specific subset of schools.
     * @param count  Number of schools to return. Default 100, maximum value 10000
     * @return an array of objects describing schools
     */
    fun getSchools(
        query: String?,
        cityId: Int,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<SchoolDto>>> {
        return rest.request(
            "database.getSchools", form(
                "q" to query,
                "city_id" to cityId,
                "offset" to offset,
                "count" to count
            ), items(SchoolDto.serializer())
        )
    }

    /**
     * Returns a list of cities.
     *
     * @param countryId Country ID.
     * @param regionId  Region ID.
     * @param query     Search query.
     * @param needAll   1 — to return all cities in the country
     * 0 — to return major cities in the country (default)
     * @param offset    Offset needed to return a specific subset of cities.
     * @param count     Number of cities to return. Default 100, maximum value 1000
     * @return the total results number in count field and an array of objects describing cities in items field
     */
    fun getCities(
        countryId: Int,
        regionId: Int?,
        query: String?,
        needAll: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiCity>>> {
        return rest.request(
            "database.getCities", form(
                "country_id" to countryId,
                "region_id" to regionId,
                "q" to query,
                "need_all" to needAll,
                "offset" to offset,
                "count" to count
            ), items(VKApiCity.serializer())
        )
    }
}