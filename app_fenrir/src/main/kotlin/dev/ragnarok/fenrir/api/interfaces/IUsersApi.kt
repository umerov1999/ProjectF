package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiGift
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.coroutines.flow.Flow

interface IUsersApi {
    @CheckResult
    fun getUserWallInfo(userId: Long, fields: String?, nameCase: String?): Flow<VKApiUser>

    @CheckResult
    fun getFollowers(
        userId: Long?, offset: Int?, count: Int?,
        fields: String?, nameCase: String?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    fun getRequests(
        offset: Int?,
        count: Int?,
        extended: Int?,
        out: Int?,
        fields: String?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    fun search(
        query: String?, sort: Int?, offset: Int?, count: Int?,
        fields: String?, city: Int?, country: Int?, hometown: String?,
        universityCountry: Int?, university: Int?, universityYear: Int?,
        universityFaculty: Int?, universityChair: Int?, sex: Int?,
        status: Int?, ageFrom: Int?, ageTo: Int?, birthDay: Int?,
        birthMonth: Int?, birthYear: Int?, online: Boolean?,
        hasPhoto: Boolean?, schoolCountry: Int?, schoolCity: Int?,
        schoolClass: Int?, school: Int?, schoolYear: Int?,
        religion: String?, interests: String?, company: String?,
        position: String?, groupId: Long?, fromList: String?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    operator fun get(
        userIds: Collection<Long>?, domains: Collection<String>?,
        fields: String?, nameCase: String?
    ): Flow<List<VKApiUser>>

    @CheckResult
    fun checkAndAddFriend(userId: Long?): Flow<Int>

    @CheckResult
    fun getGifts(user_id: Long?, count: Int?, offset: Int?): Flow<Items<VKApiGift>>

    @CheckResult
    fun report(userId: Long?, type: String?, comment: String?): Flow<Int>
}