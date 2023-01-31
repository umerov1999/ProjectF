package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse.ConversationsPush.ConversationPushItem
import dev.ragnarok.fenrir.model.Account
import dev.ragnarok.fenrir.model.BannedPart
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.model.Owner
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IAccountsInteractor {
    fun getBanned(accountId: Long, count: Int, offset: Int): Single<BannedPart>
    fun banOwners(accountId: Long, owners: Collection<Owner>): Completable
    fun unbanOwner(accountId: Long, ownerId: Long): Completable
    fun changeStatus(accountId: Long, status: String?): Completable
    fun setOffline(accountId: Long): Single<Boolean>
    fun getProfileInfo(accountId: Long): Single<VKApiProfileInfo>
    fun getPushSettings(accountId: Long): Single<List<ConversationPushItem>>
    fun saveProfileInfo(
        accountId: Long,
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Single<Int>

    fun getAll(refresh: Boolean): Single<List<Account>>

    fun importMessagesContacts(
        accountId: Long,
        context: Context,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>>

    fun getContactList(
        accountId: Long,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>>

    fun resetMessagesContacts(
        accountId: Long,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>>

    fun refreshToken(
        accountId: Long,
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Single<RefreshToken>

    fun getExchangeToken(
        accountId: Long
    ): Single<RefreshToken>
}