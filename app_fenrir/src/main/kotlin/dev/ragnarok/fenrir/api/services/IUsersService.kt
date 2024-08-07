package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiGift
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.UserWallInfoResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class IUsersService : IServiceRest() {
    fun getUserWallInfo(
        code: String?,
        userId: Long,
        fields: String?,
        nameCase: String?
    ): Flow<BaseResponse<UserWallInfoResponse>> {
        return rest.request(
            "execute", form(
                "code" to code,
                "user_id" to userId,
                "fields" to fields,
                "name_case" to nameCase
            ), base(UserWallInfoResponse.serializer())
        )
    }

    //https://vk.com/dev/users.getFollowers
    fun getFollowers(
        userId: Long?,
        offset: Int?,
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Flow<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "users.getFollowers", form(
                "user_id" to userId,
                "offset" to offset,
                "count" to count,
                "fields" to fields,
                "name_case" to nameCase
            ), items(VKApiUser.serializer())
        )
    }

    fun getRequests(
        offset: Int?,
        count: Int?,
        extended: Int?,
        out: Int?,
        fields: String?
    ): Flow<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "friends.getRequests", form(
                "offset" to offset,
                "count" to count,
                "extended" to extended,
                "out" to out,
                "fields" to fields
            ), items(VKApiUser.serializer())
        )
    }

    //https://vk.com/dev/users.search
    fun search(
        query: String?,
        sort: Int?,
        offset: Int?,
        count: Int?,
        fields: String?,
        city: Int?,
        country: Int?,
        hometown: String?,
        universityCountry: Int?,
        university: Int?,
        universityYear: Int?,
        universityFaculty: Int?,
        universityChair: Int?,
        sex: Int?,
        status: Int?,
        ageFrom: Int?,
        ageTo: Int?,
        birthDay: Int?,
        birthMonth: Int?,
        birthYear: Int?,
        online: Int?,
        hasPhoto: Int?,
        schoolCountry: Int?,
        schoolCity: Int?,
        schoolClass: Int?,
        school: Int?,
        schoolYear: Int?,
        religion: String?,
        interests: String?,
        company: String?,
        position: String?,
        groupId: Long?,
        fromList: String?
    ): Flow<BaseResponse<Items<VKApiUser>>> {
        return rest.request(
            "users.search", form(
                "q" to query,
                "sort" to sort,
                "offset" to offset,
                "count" to count,
                "fields" to fields,
                "city" to city,
                "country" to country,
                "hometown" to hometown,
                "university_country" to universityCountry,
                "university" to university,
                "university_year" to universityYear,
                "university_faculty" to universityFaculty,
                "university_chair" to universityChair,
                "sex" to sex,
                "status" to status,
                "age_from" to ageFrom,
                "age_to" to ageTo,
                "birth_day" to birthDay,
                "birth_month" to birthMonth,
                "birth_year" to birthYear,
                "online" to online,
                "has_photo" to hasPhoto,
                "school_country" to schoolCountry,
                "school_city" to schoolCity,
                "school_class" to schoolClass,
                "school" to school,
                "school_year" to schoolYear,
                "religion" to religion,
                "interests" to interests,
                "company" to company,
                "position" to position,
                "group_id" to groupId,
                "from_list" to fromList
            ), items(VKApiUser.serializer())
        )
    }

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
    operator fun get(
        userIds: String?,
        fields: String?,
        nameCase: String?
    ): Flow<BaseResponse<List<VKApiUser>>> {
        return rest.request(
            "users.get", form(
                "user_ids" to userIds,
                "fields" to fields,
                "name_case" to nameCase
            ), baseList(VKApiUser.serializer())
        )
    }

    fun report(
        userId: Long?,
        type: String?,
        comment: String?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "users.report", form(
                "user_id" to userId,
                "type" to type,
                "comment" to comment
            ), baseInt
        )
    }

    fun getGifts(
        user_id: Long?,
        count: Int?,
        offset: Int?
    ): Flow<BaseResponse<Items<VKApiGift>>> {
        return rest.request(
            "gifts.get", form(
                "user_id" to user_id,
                "count" to count,
                "offset" to offset
            ), items(VKApiGift.serializer())
        )
    }

    fun checkAndAddFriend(
        code: String?,
        user_id: Long?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "execute", form(
                "code" to code,
                "user_id" to user_id
            ), baseInt
        )
    }
}