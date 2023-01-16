package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.CountersDto
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.api.model.response.AccountsBannedResponse
import dev.ragnarok.fenrir.api.model.response.ContactsResponse
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IAccountApi {
    @CheckResult
    fun ban(ownerId: Long): Single<Int>

    @CheckResult
    fun unban(ownerId: Long): Single<Int>
    fun getBanned(count: Int?, offset: Int?, fields: String?): Single<AccountsBannedResponse>

    @CheckResult
    fun unregisterDevice(deviceId: String?): Single<Boolean>

    @CheckResult
    fun registerDevice(
        token: String?,
        pushes_granted: Int?,
        app_version: String?,
        push_provider: String?,
        companion_apps: String?,
        type: Int?,
        deviceModel: String?,
        deviceId: String?,
        systemVersion: String?,
        settings: String?
    ): Single<Boolean>

    @CheckResult
    fun setOffline(): Single<Boolean>

    @get:CheckResult
    val profileInfo: Single<VKApiProfileInfo>

    @get:CheckResult
    val pushSettings: Single<PushSettingsResponse>

    @CheckResult
    fun saveProfileInfo(
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Single<VKApiProfileInfoResponse>

    @CheckResult
    fun getCounters(filter: String?): Single<CountersDto>

    @CheckResult
    fun refreshToken(
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Single<RefreshToken>

    @CheckResult
    fun importMessagesContacts(contacts: String?): Completable

    @CheckResult
    fun getContactList(offset: Int?, count: Int?): Single<ContactsResponse>

    @CheckResult
    fun resetMessagesContacts(): Completable
}