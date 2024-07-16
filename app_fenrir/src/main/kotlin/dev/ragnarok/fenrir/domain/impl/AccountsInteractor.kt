package dev.ragnarok.fenrir.domain.impl

import android.content.Context
import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse.ConversationsPush.ConversationPushItem
import dev.ragnarok.fenrir.db.impl.ContactsUtils
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IBlacklistRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.model.Account
import dev.ragnarok.fenrir.model.BannedPart
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayedFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single

class AccountsInteractor(
    private val networker: INetworker,
    private val settings: IAccountsSettings,
    private val blacklistRepository: IBlacklistRepository,
    private val ownersRepository: IOwnersRepository
) : IAccountsInteractor {
    override fun getBanned(accountId: Long, count: Int, offset: Int): Flow<BannedPart> {
        return networker.vkDefault(accountId)
            .account()
            .getBanned(count, offset, Fields.FIELDS_BASE_OWNER)
            .map { items ->
                val owners = Dto2Model.transformOwners(items.profiles, items.groups)
                val result = ArrayList<Owner>(owners.size)
                for (i in items.items.orEmpty()) {
                    val ip = Utils.findIndexById(owners, i)
                    if (ip < 0) {
                        continue
                    }
                    result.add(owners[ip])
                }
                BannedPart(result)
            }
    }

    override fun banOwners(accountId: Long, owners: Collection<Owner>): Flow<Boolean> {
        return flow {
            for (owner in owners) {
                val s = networker.vkDefault(accountId)
                    .account()
                    .ban(owner.ownerId).checkInt().single()
                if (s) {
                    delay(1000) // чтобы не дергало UI
                    blacklistRepository.fireAdd(accountId, owner).single()
                }
            }
            emit(true)
        }
    }

    override fun unbanOwner(accountId: Long, ownerId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .account()
            .unban(ownerId)
            .delayedFlow(1000) // чтобы не дергало UI
            .ignoreElement()
            .andThen(blacklistRepository.fireRemove(accountId, ownerId))
    }

    override fun changeStatus(accountId: Long, status: String?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .status()
            .set(status, null)
            .flatMapConcat {
                ownersRepository.handleStatusChange(
                    accountId,
                    accountId,
                    status
                )
            }
    }

    override fun refreshToken(
        accountId: Long,
        receipt: String?,
        receipt2: String?,
        nonce: String?,
        timestamp: Long?
    ): Flow<RefreshToken> {
        return networker.vkDefault(accountId)
            .account()
            .refreshToken(receipt, receipt2, nonce, timestamp)
    }

    override fun getExchangeToken(
        accountId: Long
    ): Flow<RefreshToken> {
        return networker.vkDefault(accountId)
            .account()
            .getExchangeToken()
    }

    override fun setOffline(accountId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .account()
            .setOffline()
    }

    override fun getProfileInfo(accountId: Long): Flow<VKApiProfileInfo> {
        return networker.vkDefault(accountId)
            .account()
            .profileInfo
    }

    override fun getPushSettings(accountId: Long): Flow<List<ConversationPushItem>> {
        return networker.vkDefault(accountId)
            .account()
            .pushSettings.map { obj -> obj.pushSettings }
    }

    override fun saveProfileInfo(
        accountId: Long,
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Flow<Int> {
        return networker.vkDefault(accountId)
            .account()
            .saveProfileInfo(first_name, last_name, maiden_name, screen_name, bdate, home_town, sex)
            .map { t -> t.status }
    }

    override fun getAll(refresh: Boolean): Flow<List<Account>> {
        return flow {
            val tmpIds: Collection<Long> = settings.registered
            val ids: Collection<Long> = if (!Settings.get().security().IsShow_hidden_accounts) {
                val lst = ArrayList<Long>()
                for (i in tmpIds) {
                    if (!Utils.isHiddenAccount(i)) {
                        lst.add(i)
                    }
                }
                lst
            } else {
                tmpIds
            }
            val accounts: MutableList<Account> = ArrayList(ids.size)
            for (id in ids) {
                if (!isActive()) {
                    break
                }
                val owner = ownersRepository.getBaseOwnerInfo(
                    id,
                    id,
                    if (refresh) IOwnersRepository.MODE_NET else IOwnersRepository.MODE_ANY
                ).catch { emit(if (id > 0) User(id) else Community(-id)) }.single()

                val account = Account(id, owner)
                accounts.add(account)
            }
            emit(accounts)
        }
    }

    override fun importMessagesContacts(
        accountId: Long,
        context: Context,
        offset: Int?,
        count: Int?
    ): Flow<List<ContactConversation>> {
        return ContactsUtils.getAllContactsJson(context).flatMapConcat {
            networker.vkDefault(accountId)
                .account().importMessagesContacts(it)
                .andThen(getContactList(accountId, offset, count))
        }
    }

    override fun processAuthCode(
        accountId: Long,
        auth_code: String, action: Int
    ): Flow<VKApiProcessAuthCode> {
        return networker.vkDefault(accountId)
            .account().processAuthCode(auth_code, action)
    }

    override fun resetMessagesContacts(
        accountId: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<ContactConversation>> {
        return networker.vkDefault(accountId)
            .account().resetMessagesContacts().andThen(getContactList(accountId, offset, count))
    }

    private fun findProfiles(id: Long, list: List<VKApiUser>?): VKApiUser? {
        for (i in list.orEmpty()) {
            if (i.id == id) {
                return i
            }
        }
        return null
    }

    private fun findContacts(
        id: Long,
        list: List<VKApiConversation.ContactElement>?
    ): VKApiConversation.ContactElement? {
        for (i in list.orEmpty()) {
            if (Peer.fromContactId(i.id) == id) {
                return i
            }
        }
        return null
    }

    override fun getContactList(
        accountId: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<ContactConversation>> {
        return networker.vkDefault(accountId)
            .account().getContactList(offset, count).map { items ->
                val dtos = listEmptyIfNull(items.items)
                val data: MutableList<ContactConversation> = ArrayList(dtos.size)
                for (i in dtos) {
                    val temp = ContactConversation(i)
                    findContacts(i, items.contacts)?.let {
                        temp.setTitle(it.name).setPhoto(
                            Utils.firstNonEmptyString(
                                it.photo_200,
                                it.photo_100,
                                it.photo_50
                            )
                        )
                            .setPhone(it.phone)
                            .setLast_seen_status(it.last_seen_status).setLastSeen(0)
                            .setIsContact(true)
                    } ?: findProfiles(i, items.profiles)?.let {
                        temp.setTitle(it.fullName).setPhoto(
                            Utils.firstNonEmptyString(
                                it.photo_200,
                                it.photo_100,
                                it.photo_50
                            )
                        )
                            .setPhone(null)
                            .setLast_seen_status(null).setLastSeen(it.last_seen).setIsContact(false)
                    }
                    data.add(temp)
                }
                data
            }
    }
}