package dev.ragnarok.fenrir.domain.impl

import android.content.Context
import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse.ConversationsPush.ConversationPushItem
import dev.ragnarok.fenrir.db.impl.ContactsUtils
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IBlacklistRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import java.util.concurrent.TimeUnit

class AccountsInteractor(
    private val networker: INetworker,
    private val settings: IAccountsSettings,
    private val blacklistRepository: IBlacklistRepository,
    private val ownersRepository: IOwnersRepository
) : IAccountsInteractor {
    override fun getBanned(accountId: Int, count: Int, offset: Int): Single<BannedPart> {
        return networker.vkDefault(accountId)
            .account()
            .getBanned(count, offset, Fields.FIELDS_BASE_OWNER)
            .map { items ->
                val owners = Dto2Model.transformOwners(items.profiles, items.groups)
                val result = ArrayList<Owner>(owners.size)
                for (i in items.items.orEmpty()) {
                    val ip = Utils.findOwnerIndexById(owners, i)
                    if (ip < 0) {
                        continue
                    }
                    result.add(owners[ip])
                }
                BannedPart(result)
            }
    }

    override fun banOwners(accountId: Int, owners: Collection<Owner>): Completable {
        var completable = Completable.complete()
        for (owner in owners) {
            completable = completable.andThen(
                networker.vkDefault(accountId)
                    .account()
                    .ban(owner.ownerId)
            )
                .delay(1, TimeUnit.SECONDS) // чтобы не дергало UI
                .ignoreElement()
                .andThen(blacklistRepository.fireAdd(accountId, owner))
        }
        return completable
    }

    override fun unbanOwner(accountId: Int, ownerId: Int): Completable {
        return networker.vkDefault(accountId)
            .account()
            .unban(ownerId)
            .delay(1, TimeUnit.SECONDS) // чтобы не дергало UI
            .ignoreElement()
            .andThen(blacklistRepository.fireRemove(accountId, ownerId))
    }

    override fun changeStatus(accountId: Int, status: String?): Completable {
        return networker.vkDefault(accountId)
            .status()
            .set(status, null)
            .flatMapCompletable {
                ownersRepository.handleStatusChange(
                    accountId,
                    accountId,
                    status
                )
            }
    }

    override fun setOffline(accountId: Int): Single<Boolean> {
        return networker.vkDefault(accountId)
            .account()
            .setOffline()
    }

    override fun getProfileInfo(accountId: Int): Single<VKApiProfileInfo> {
        return networker.vkDefault(accountId)
            .account()
            .profileInfo
    }

    override fun getPushSettings(accountId: Int): Single<List<ConversationPushItem>> {
        return networker.vkDefault(accountId)
            .account()
            .pushSettings.map { obj -> obj.pushSettings }
    }

    override fun saveProfileInfo(
        accountId: Int,
        first_name: String?,
        last_name: String?,
        maiden_name: String?,
        screen_name: String?,
        bdate: String?,
        home_town: String?,
        sex: Int?
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .account()
            .saveProfileInfo(first_name, last_name, maiden_name, screen_name, bdate, home_town, sex)
            .map { t -> t.status }
    }

    override fun getAll(refresh: Boolean): Single<List<Account>> {
        return Single.create { emitter: SingleEmitter<List<Account>> ->
            val tmpIds: Collection<Int> = settings.registered
            val ids: Collection<Int> = if (!Settings.get().security().IsShow_hidden_accounts()) {
                val lst = ArrayList<Int>()
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
                if (emitter.isDisposed) {
                    break
                }
                val owner = ownersRepository.getBaseOwnerInfo(
                    id,
                    id,
                    if (refresh) IOwnersRepository.MODE_NET else IOwnersRepository.MODE_ANY
                )
                    .onErrorReturn { if (id > 0) User(id) else Community(-id) }
                    .blockingGet()
                val account = Account(id, owner)
                accounts.add(account)
            }
            emitter.onSuccess(accounts)
        }
    }

    override fun importMessagesContacts(
        accountId: Int,
        context: Context,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>> {
        return ContactsUtils.getAllContactsJson(context).flatMap {
            networker.vkDefault(accountId)
                .account().importMessagesContacts(it)
                .andThen(getContactList(accountId, offset, count))
        }
    }

    override fun resetMessagesContacts(
        accountId: Int,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>> {
        return networker.vkDefault(accountId)
            .account().resetMessagesContacts().andThen(getContactList(accountId, offset, count))
    }

    private fun findProfiles(id: Int, list: List<VKApiUser>?): VKApiUser? {
        for (i in list.orEmpty()) {
            if (i.id == id) {
                return i
            }
        }
        return null
    }

    private fun findContacts(
        id: Int,
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
        accountId: Int,
        offset: Int?,
        count: Int?
    ): Single<List<ContactConversation>> {
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