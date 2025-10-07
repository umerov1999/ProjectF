package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.CountersDto
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.api.model.response.AccountsBannedResponse
import dev.ragnarok.fenrir.api.model.response.ContactsResponse
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse
import kotlinx.coroutines.flow.Flow

interface IAccountApi {
    @CheckResult
    fun ban(ownerId: Long): Flow<Int>

    @CheckResult
    fun unban(ownerId: Long): Flow<Int>
    fun getBanned(count: Int?, offset: Int?, fields: String?): Flow<AccountsBannedResponse>

    @CheckResult
    fun unregisterDevice(deviceId: String?): Flow<Boolean>

    @CheckResult
    fun registerDevice(
        api_id: Int?,
        app_id: Int?,
        token: String?,
        pushes_granted: Int?,
        app_version: Int?,
        push_provider: String?,
        companion_apps: String?,
        type: Int?,
        deviceModel: String?,
        deviceId: String?,
        systemVersion: String?,
        settings: String?
    ): Flow<Boolean>

    @CheckResult
    fun setOffline(): Flow<Boolean>

    @CheckResult
    fun setOnline(): Flow<Boolean>

    @get:CheckResult
    val profileInfo: Flow<VKApiProfileInfo>

    @get:CheckResult
    val pushSettings: Flow<PushSettingsResponse>

    @CheckResult
    fun saveProfileInfo(
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Flow<VKApiProfileInfoResponse>

    @CheckResult
    fun getCounters(filter: String?): Flow<CountersDto>

    @CheckResult
    fun refreshToken(
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Flow<RefreshToken>

    @CheckResult
    fun getExchangeToken(): Flow<RefreshToken>

    @CheckResult
    fun importMessagesContacts(contacts: String?): Flow<Boolean>

    @CheckResult
    fun processAuthCode(auth_code: String, action: Int): Flow<VKApiProcessAuthCode>

    @CheckResult
    fun getContactList(offset: Int?, count: Int?): Flow<ContactsResponse>

    @CheckResult
    fun resetMessagesContacts(): Flow<Boolean>
}