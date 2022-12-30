package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.CountersDto
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.api.model.response.*
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IAccountService : IServiceRest() {
    fun ban(owner_id: Int): Single<BaseResponse<Int>> {
        return rest.request("account.ban", form("owner_id" to owner_id), baseInt)
    }

    fun unban(owner_id: Int): Single<BaseResponse<Int>> {
        return rest.request("account.unban", form("owner_id" to owner_id), baseInt)
    }

    fun getBanned(
        count: Int?,
        offset: Int?,
        fields: String?
    ): Single<BaseResponse<AccountsBannedResponse>> {
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
    fun getCounters(filter: String?): Single<BaseResponse<CountersDto>> {
        return rest.request(
            "account.getCounters",
            form("filter" to filter),
            base(CountersDto.serializer())
        )
    }

    //https://vk.com/dev/account.unregisterDevice
    fun unregisterDevice(deviceId: String?): Single<BaseResponse<Int>> {
        return rest.request("account.unregisterDevice", form("device_id" to deviceId), baseInt)
    }

    //https://vk.com/dev/account.registerDevice
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
    ): Single<BaseResponse<Int>> {
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
                "settings" to settings
            ), baseInt
        )
    }

    /**
     * Marks a current user as offline.
     *
     * @return In case of success returns 1.
     */
    val setOffline: Single<BaseResponse<Int>>
        get() = rest.request("account.setOffline", null, baseInt)

    val profileInfo: Single<BaseResponse<VKApiProfileInfo>>
        get() = rest.request("account.getProfileInfo", null, base(VKApiProfileInfo.serializer()))

    val pushSettings: Single<BaseResponse<PushSettingsResponse>>
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
    ): Single<BaseResponse<VKApiProfileInfoResponse>> {
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
    ): Single<BaseResponse<RefreshToken>> {
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

    val resetMessagesContacts: Single<BaseResponse<Int>>
        get() = rest.request("account.resetMessagesContacts", null, baseInt)

    fun importMessagesContacts(
        contacts: String?
    ): Single<VkResponse> {
        return rest.request(
            "account.importMessagesContacts",
            form("contacts" to contacts),
            VkResponse.serializer()
        )
    }

    fun getContactList(
        offset: Int?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<ContactsResponse>> {
        return rest.request(
            "account.getContactList",
            form("offset" to offset, "count" to count, "extended" to extended, "fields" to fields),
            base(ContactsResponse.serializer())
        )
    }
}