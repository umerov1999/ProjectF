package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCity
import dev.ragnarok.fenrir.api.model.VKApiCountry
import dev.ragnarok.fenrir.api.model.database.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IDatabaseService {
    //https://vk.com/dev/database.getCitiesById
    @FormUrlEncoded
    @POST("database.getCities")
    fun getCitiesById(@Field("city_ids") cityIds: String?): Single<BaseResponse<List<VKApiCity>>>

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
    @FormUrlEncoded
    @POST("database.getCountries")
    fun getCountries(
        @Field("need_all") needAll: Int?,
        @Field("code") code: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiCountry>>>

    /**
     * Returns a list of school classes specified for the country.
     *
     * @param countryId Country ID.
     * @return Returns an array of objects, each of them is a pair of class ID and definition.
     */
    @FormUrlEncoded
    @POST("database.getSchoolClasses")
    fun getSchoolClasses(@Field("country_id") countryId: Int?): Single<BaseResponse<List<SchoolClazzDto>>>

    /**
     * Returns list of chairs on a specified faculty.
     *
     * @param facultyId id of the faculty to get chairs from
     * @param offset    offset required to get a certain subset of chairs
     * @param count     amount of chairs to get. Default 100, maximum value 10000
     * @return the total results number in count field and an array of objects describing chairs in items field
     */
    @FormUrlEncoded
    @POST("database.getChairs")
    fun getChairs(
        @Field("faculty_id") facultyId: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<ChairDto>>>

    /**
     * Returns a list of faculties (i.e., university departments).
     *
     * @param universityId University ID.
     * @param offset       Offset needed to return a specific subset of faculties.
     * @param count        Number of faculties to return. Default 100, maximum value 10000
     * @return the total results number in count field and an array
     * of objects describing faculties in items field
     */
    @FormUrlEncoded
    @POST("database.getFaculties")
    fun getFaculties(
        @Field("university_id") universityId: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<FacultyDto>>>

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
    @FormUrlEncoded
    @POST("database.getUniversities")
    fun getUniversities(
        @Field("q") query: String?,
        @Field("country_id") countryId: Int?,
        @Field("city_id") cityId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<UniversityDto>>>

    /**
     * Returns a list of schools.
     *
     * @param query  Search query.
     * @param cityId City ID.
     * @param offset Offset needed to return a specific subset of schools.
     * @param count  Number of schools to return. Default 100, maximum value 10000
     * @return an array of objects describing schools
     */
    @FormUrlEncoded
    @POST("database.getSchools")
    fun getSchools(
        @Field("q") query: String?,
        @Field("city_id") cityId: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<SchoolDto>>>

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
    @FormUrlEncoded
    @POST("database.getCities")
    fun getCities(
        @Field("country_id") countryId: Int,
        @Field("region_id") regionId: Int?,
        @Field("q") query: String?,
        @Field("need_all") needAll: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiCity>>>
}