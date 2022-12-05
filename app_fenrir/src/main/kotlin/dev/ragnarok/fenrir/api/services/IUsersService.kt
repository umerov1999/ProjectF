package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.StoryResponse
import dev.ragnarok.fenrir.api.model.response.UserWallInfoResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IUsersService {
    @FormUrlEncoded
    @POST("execute")
    fun getUserWallInfo(
        @Field("code") code: String?,
        @Field("user_id") userId: Int,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<UserWallInfoResponse>>

    //https://vk.com/dev/users.getFollowers
    @FormUrlEncoded
    @POST("users.getFollowers")
    fun getFollowers(
        @Field("user_id") userId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<Items<VKApiUser>>>

    @FormUrlEncoded
    @POST("friends.getRequests")
    fun getRequests(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("out") out: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<VKApiUser>>>

    //https://vk.com/dev/users.search
    @FormUrlEncoded
    @POST("users.search")
    fun search(
        @Field("q") query: String?,
        @Field("sort") sort: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?,
        @Field("city") city: Int?,
        @Field("country") country: Int?,
        @Field("hometown") hometown: String?,
        @Field("university_country") universityCountry: Int?,
        @Field("university") university: Int?,
        @Field("university_year") universityYear: Int?,
        @Field("university_faculty") universityFaculty: Int?,
        @Field("university_chair") universityChair: Int?,
        @Field("sex") sex: Int?,
        @Field("status") status: Int?,
        @Field("age_from") ageFrom: Int?,
        @Field("age_to") ageTo: Int?,
        @Field("birth_day") birthDay: Int?,
        @Field("birth_month") birthMonth: Int?,
        @Field("birth_year") birthYear: Int?,
        @Field("online") online: Int?,
        @Field("has_photo") hasPhoto: Int?,
        @Field("school_country") schoolCountry: Int?,
        @Field("school_city") schoolCity: Int?,
        @Field("school_class") schoolClass: Int?,
        @Field("school") school: Int?,
        @Field("school_year") schoolYear: Int?,
        @Field("religion") religion: String?,
        @Field("interests") interests: String?,
        @Field("company") company: String?,
        @Field("position") position: String?,
        @Field("group_id") groupId: Int?,
        @Field("from_list") fromList: String?
    ): Single<BaseResponse<Items<VKApiUser>>>

    /**
     * Returns detailed information on users.
     *
     * @param userIds  User IDs or screen names (screen_name). By default, current user ID.
     * List of comma-separated words, the maximum number of elements allowed is 1000
     * @param fields   Profile fields to return
     * @param nameCase Case for declension of user name and surname:
     * nom — nominative (default)
     * gen — genitive
     * dat — dative
     * acc — accusative
     * ins — instrumental
     * abl — prepositional
     * @return Returns a list of user objects.
     * A deactivated field may be returned with the value deleted or banned if a user has been suspended.
     */
    @FormUrlEncoded
    @POST("users.get")
    operator fun get(
        @Field("user_ids") userIds: String?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<List<VKApiUser>>>

    @FormUrlEncoded
    @POST("stories.getPhotoUploadServer")
    fun stories_getPhotoUploadServer(@Field("add_to_news") add_to_news: Int?): Single<BaseResponse<VKApiStoryUploadServer>>

    @FormUrlEncoded
    @POST("stories.getVideoUploadServer")
    fun stories_getVideoUploadServer(@Field("add_to_news") add_to_news: Int?): Single<BaseResponse<VKApiStoryUploadServer>>

    @FormUrlEncoded
    @POST("stories.save")
    fun stories_save(@Field("upload_results") upload_results: String?): Single<BaseResponse<Items<VKApiStory>>>

    @POST("users.report")
    @FormUrlEncoded
    fun report(
        @Field("user_id") userId: Int?,
        @Field("type") type: String?,
        @Field("comment") comment: String?
    ): Single<BaseResponse<Int>>

    @POST("stories.get")
    @FormUrlEncoded
    fun getStory(
        @Field("owner_id") owner_id: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<StoryResponse>>

    @POST("narratives.getFromOwner")
    @FormUrlEncoded
    fun getNarratives(
        @Field("owner_id") owner_id: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiNarratives>>>

    @POST("stories.getById")
    @FormUrlEncoded
    fun getStoryById(
        @Field("stories") stories: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<StoryGetResponse>>

    @POST("gifts.get")
    @FormUrlEncoded
    fun getGifts(
        @Field("user_id") user_id: Int?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?
    ): Single<BaseResponse<Items<VKApiGift>>>

    @POST("stories.search")
    @FormUrlEncoded
    fun searchStory(
        @Field("q") q: String?,
        @Field("mentioned_id") mentioned_id: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<StoryResponse>>

    @FormUrlEncoded
    @POST("execute")
    fun checkAndAddFriend(
        @Field("code") code: String?,
        @Field("user_id") user_id: Int?
    ): Single<BaseResponse<Int>>
}