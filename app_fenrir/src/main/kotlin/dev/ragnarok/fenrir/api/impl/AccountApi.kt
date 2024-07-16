package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IAccountApi
import dev.ragnarok.fenrir.api.model.CountersDto
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.api.model.response.AccountsBannedResponse
import dev.ragnarok.fenrir.api.model.response.ContactsResponse
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse
import dev.ragnarok.fenrir.api.services.IAccountService
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class AccountApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IAccountApi {
    override fun ban(ownerId: Long): Flow<Int> {
        return provideService(IAccountService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.ban(ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun unban(ownerId: Long): Flow<Int> {
        return provideService(IAccountService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.unban(ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getBanned(
        count: Int?,
        offset: Int?,
        fields: String?
    ): Flow<AccountsBannedResponse> {
        return provideService(IAccountService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getBanned(count, offset, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun unregisterDevice(deviceId: String?): Flow<Boolean> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.unregisterDevice(deviceId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun registerDevice(
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
    ): Flow<Boolean> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.registerDevice(
                    token,
                    pushes_granted,
                    app_version,
                    push_provider,
                    companion_apps,
                    type,
                    deviceModel,
                    deviceId,
                    systemVersion,
                    settings
                )
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun setOffline(): Flow<Boolean> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.setOffline
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override val profileInfo: Flow<VKApiProfileInfo>
        get() = provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.profileInfo
                    .map(extractResponseWithErrorHandling())
            }
    override val pushSettings: Flow<PushSettingsResponse>
        get() = provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.pushSettings
                    .map(extractResponseWithErrorHandling())
            }

    override fun saveProfileInfo(
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Flow<VKApiProfileInfoResponse> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.saveProfileInfo(
                    first_name,
                    last_name,
                    maiden_name,
                    screen_name,
                    bdate,
                    home_town,
                    sex
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCounters(filter: String?): Flow<CountersDto> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.getCounters(filter)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun refreshToken(
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Flow<RefreshToken> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.refreshToken(receipt, receipt2, nonce, timestamp)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getExchangeToken(): Flow<RefreshToken> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.getExchangeToken()
                    .map(AuthApi.extractResponseWithErrorHandling())
            }
    }

    override fun importMessagesContacts(contacts: String?): Flow<Boolean> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.importMessagesContacts(contacts)
                    .map(checkResponseWithErrorHandling())
            }
    }

    override fun processAuthCode(auth_code: String, action: Int): Flow<VKApiProcessAuthCode> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.processAuthCode(auth_code, action)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getContactList(offset: Int?, count: Int?): Flow<ContactsResponse> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.getContactList(offset, count, 1, Fields.FIELDS_FULL_USER)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun resetMessagesContacts(): Flow<Boolean> {
        return provideService(IAccountService(), TokenType.USER)
            .flatMapConcat {
                it.resetMessagesContacts
                    .map(checkResponseWithErrorHandling())
            }
    }
}