package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.CountersDto
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.api.model.response.AccountsBannedResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.ContactsResponse
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse
import dev.ragnarok.fenrir.api.model.response.VKResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class IAccountService : IServiceRest() {
    fun ban(owner_id: Long): Flow<BaseResponse<Int>> {
        return rest.request("account.ban", form("owner_id" to owner_id), baseInt)
    }

    fun unban(owner_id: Long): Flow<BaseResponse<Int>> {
        return rest.request("account.unban", form("owner_id" to owner_id), baseInt)
    }

    fun getBanned(
        count: Int?,
        offset: Int?,
        fields: String?
    ): Flow<BaseResponse<AccountsBannedResponse>> {
        return rest.request(
            "account.getBanned",
            form("count" to count, "offset" to offset, "fields" to fields),
            base(AccountsBannedResponse.serializer())
        )
    }

    //https://vk.com/dev/account.getCounters
    /**
     * @param filter friends — новые заявки в друзья;
     * friends_suggestions — предлагаемые друзья;
     * messages — новые сообщения;
     * photos — новые отметки на фотографиях;
     * videos — новые отметки на видеозаписях;
     * gifts — подарки;
     * events — события;
     * groups — сообщества;
     * notifications — ответы;
     * sdk — запросы в мобильных играх;
     * app_requests — уведомления от приложений.
     */
    fun getCounters(filter: String?): Flow<BaseResponse<CountersDto>> {
        return rest.request(
            "account.getCounters",
            form("filter" to filter),
            base(CountersDto.serializer())
        )
    }

    //https://vk.com/dev/account.unregisterDevice
    fun unregisterDevice(deviceId: String?): Flow<BaseResponse<Int>> {
        return rest.request("account.unregisterDevice", form("device_id" to deviceId), baseInt)
    }

    //https://vk.com/dev/account.registerDevice
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
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "account.registerDevice",
            form(
                "token" to token,
                "pushes_granted" to pushes_granted,
                "app_version" to app_version,
                "push_provider" to push_provider,
                "companion_apps" to companion_apps,
                "type" to type,
                "device_model" to deviceModel,
                "device_id" to deviceId,
                "system_version" to systemVersion,
                "has_google_services" to 1,
                "app_id" to app_id,
                "api_id" to api_id,
                "settings" to settings
            ), baseInt
        )
    }

    /**
     * Marks a current user as offline.
     *
     * @return In case of success returns 1.
     */
    val setOffline: Flow<BaseResponse<Int>>
        get() = rest.request("account.setOffline", null, baseInt)

    val profileInfo: Flow<BaseResponse<VKApiProfileInfo>>
        get() = rest.request("account.getProfileInfo", null, base(VKApiProfileInfo.serializer()))

    val pushSettings: Flow<BaseResponse<PushSettingsResponse>>
        get() = rest.request(
            "account.getPushSettings",
            null,
            base(PushSettingsResponse.serializer())
        )

    fun saveProfileInfo(
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Flow<BaseResponse<VKApiProfileInfoResponse>> {
        return rest.request(
            "account.saveProfileInfo",
            form(
                "first_name" to first_name,
                "last_name" to last_name,
                "maiden_name" to maiden_name,
                "screen_name" to screen_name,
                "bdate" to bdate,
                "home_town" to home_town,
                "sex" to sex
            ), base(VKApiProfileInfoResponse.serializer())
        )
    }

    fun refreshToken(
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Flow<BaseResponse<RefreshToken>> {
        return rest.request(
            "auth.refreshToken",
            form(
                "receipt" to receipt,
                "receipt2" to receipt2,
                "nonce" to nonce,
                "timestamp" to timestamp
            ),
            base(RefreshToken.serializer())
        )
    }

    fun getExchangeToken(): Flow<BaseResponse<RefreshToken>> {
        return rest.request(
            "auth.getExchangeToken",
            null,
            base(RefreshToken.serializer())
        )
    }

    val resetMessagesContacts: Flow<BaseResponse<Int>>
        get() = rest.request("account.resetMessagesContacts", null, baseInt)

    fun importMessagesContacts(
        contacts: String?
    ): Flow<VKResponse> {
        return rest.request(
            "account.importMessagesContacts",
            form("contacts" to contacts),
            VKResponse.serializer()
        )
    }

    fun processAuthCode(
        auth_code: String,
        action: Int
    ): Flow<BaseResponse<VKApiProcessAuthCode>> {
        return rest.request(
            "auth.processAuthCode",
            form("auth_code" to auth_code, "action" to action),
            base(VKApiProcessAuthCode.serializer())
        )
    }

    fun getContactList(
        offset: Int?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Flow<BaseResponse<ContactsResponse>> {
        return rest.request(
            "account.getContactList",
            form("offset" to offset, "count" to count, "extended" to extended, "fields" to fields),
            base(ContactsResponse.serializer())
        )
    }
}
