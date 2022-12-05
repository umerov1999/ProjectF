package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse.ConversationsPush.ConversationPushItem
import dev.ragnarok.fenrir.model.Account
import dev.ragnarok.fenrir.model.BannedPart
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.model.Owner
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IAccountsInteractor {
    fun getBanned(accountId: Int, count: Int, offset: Int): Single<BannedPart>
    fun banOwners(accountId: Int, owners: Collection<Owner>): Completable
    fun unbanOwner(accountId: Int, ownerId: Int): Completable
    fun changeStatus(accountId: Int, status: String?): Completable
    fun setOffline(accountId: Int): Single<Boolean>
    fun getProfileInfo(accountId: Int): Single<VKApiProfileInfo>
    fun getPushSettings(accountId: Int): Single<List<ConversationPushItem>>
    fun saveProfileInfo(
        accountId: Int,
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
        accountId: Int,
        context: Context,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>>

    fun getContactList(accountId: Int, offset: Int?, count: Int?): Single<List<ContactConversation>>

    fun resetMessagesContacts(
        accountId: Int,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>>
}