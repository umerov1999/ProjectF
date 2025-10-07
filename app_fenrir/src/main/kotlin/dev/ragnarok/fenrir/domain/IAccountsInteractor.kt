package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse.ConversationsPush.ConversationPushItem
import dev.ragnarok.fenrir.model.Account
import dev.ragnarok.fenrir.model.BannedPart
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.model.Owner
import kotlinx.coroutines.flow.Flow

interface IAccountsInteractor {
    fun getBanned(accountId: Long, count: Int, offset: Int): Flow<BannedPart>
    fun banOwners(accountId: Long, owners: Collection<Owner>): Flow<Boolean>
    fun unbanOwner(accountId: Long, ownerId: Long): Flow<Boolean>
    fun changeStatus(accountId: Long, status: String?): Flow<Boolean>
    fun setOffline(accountId: Long): Flow<Boolean>
    fun setOnline(accountId: Long): Flow<Boolean>
    fun getProfileInfo(accountId: Long): Flow<VKApiProfileInfo>
    fun getPushSettings(accountId: Long): Flow<List<ConversationPushItem>>
    fun saveProfileInfo(
        accountId: Long,
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Flow<Int>

    fun getAll(refresh: Boolean): Flow<List<Account>>

    fun importMessagesContacts(
        accountId: Long,
        context: Context,
        offset: Int?,
        count: Int?
    ): Flow<List<ContactConversation>>

    fun processAuthCode(
        accountId: Long,
        auth_code: String, action: Int
    ): Flow<VKApiProcessAuthCode>

    fun getContactList(
        accountId: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<ContactConversation>>

    fun resetMessagesContacts(
        accountId: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<ContactConversation>>

    fun refreshToken(
        accountId: Long,
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Flow<RefreshToken>

    fun getExchangeToken(
        accountId: Long
    ): Flow<RefreshToken>
}