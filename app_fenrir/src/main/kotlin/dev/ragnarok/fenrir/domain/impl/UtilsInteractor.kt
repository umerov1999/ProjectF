package dev.ragnarok.fenrir.domain.impl

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommunityFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.model.FriendList
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Privacy
import dev.ragnarok.fenrir.model.ShortLink
import dev.ragnarok.fenrir.model.SimplePrivacy
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyMapFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class UtilsInteractor(
    private val networker: INetworker,
    private val stores: IStorages,
    private val ownersRepository: IOwnersRepository
) : IUtilsInteractor {
    @SuppressLint("UseSparseArrays")
    override fun createFullPrivacies(
        accountId: Long,
        orig: Map<Int, SimplePrivacy>
    ): Flow<Map<Int, Privacy>> {
        return toFlow(orig)
            .flatMapConcat {
                val uids: MutableSet<Long> = HashSet()
                val listsIds: MutableSet<Long> = HashSet()
                for ((_, privacy) in orig) {
                    if (privacy.entries.isNullOrEmpty()) {
                        continue
                    }
                    for (entry in privacy.entries) {
                        when (entry.type) {
                            SimplePrivacy.Entry.TYPE_FRIENDS_LIST -> listsIds.add(entry.id)
                            SimplePrivacy.Entry.TYPE_USER -> uids.add(entry.id)
                        }
                    }
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    uids,
                    IOwnersRepository.MODE_ANY
                )
                    .flatMapConcat { owners ->
                        findFriendListsByIds(accountId, accountId, listsIds)
                            .map { lists ->
                                val privacies: MutableMap<Int, Privacy> = HashMap(
                                    safeCountOf(orig)
                                )
                                for ((key, value) in orig) {
                                    val full: Privacy = transform(value, owners, lists)
                                    privacies[key] = full
                                }
                                privacies
                            }
                    }
            }
    }

    override fun resolveDomain(accountId: Long, domain: String?): Flow<Optional<Owner>> {
        return stores.owners()
            .findUserByDomain(accountId, domain)
            .flatMapConcat { optionalUserEntity ->
                if (optionalUserEntity.nonEmpty()) {
                    val user = map(optionalUserEntity.get())
                    toFlow(wrap<Owner>(user))
                } else {
                    stores.owners()
                        .findCommunityByDomain(accountId, domain)
                        .map { optionalCommunityEntity ->
                            if (optionalCommunityEntity.nonEmpty()) {
                                val community =
                                    buildCommunityFromDbo(optionalCommunityEntity.requireNonEmpty())
                                wrap<Owner>(community)
                            } else {
                                empty()
                            }
                        }
                }
            }
            .flatMapConcat { optionalOwner ->
                if (optionalOwner.nonEmpty()) {
                    toFlow(optionalOwner)
                } else {
                    networker.vkDefault(accountId)
                        .utils()
                        .resolveScreenName(domain)
                        .flatMapConcat { response ->
                            response.object_id?.let {
                                when (response.type) {
                                    "user" -> {
                                        val userId =
                                            it.toLong()
                                        ownersRepository.getBaseOwnerInfo(
                                            accountId,
                                            userId,
                                            IOwnersRepository.MODE_ANY
                                        )
                                            .map { pp -> wrap(pp) }
                                    }

                                    "group" -> {
                                        val ownerId = -abs(
                                            it.toLong()
                                        )
                                        ownersRepository.getBaseOwnerInfo(
                                            accountId,
                                            ownerId,
                                            IOwnersRepository.MODE_ANY
                                        )
                                            .map { pp -> wrap(pp) }
                                    }

                                    else -> toFlow(empty())
                                }
                            } ?: toFlow(empty())
                        }
                }
            }
    }

    @SuppressLint("UseSparseArrays")
    private fun findFriendListsByIds(
        accountId: Long,
        userId: Long,
        ids: Collection<Long>
    ): Flow<Map<Long, FriendList>> {
        return if (ids.isEmpty()) {
            emptyMapFlow()
        } else stores.owners()
            .findFriendsListsByIds(accountId, userId, ids)
            .flatMapConcat { mp ->
                if (mp.size == ids.size) {
                    val data: MutableMap<Long, FriendList> = HashMap(mp.size)
                    for (id in ids) {
                        val dbo = mp[id] ?: continue
                        data[id] = FriendList(dbo.id, dbo.name)
                    }
                    toFlow(data)
                } else {
                    networker.vkDefault(accountId)
                        .friends()
                        .getLists(userId, true)
                        .map { items ->
                            listEmptyIfNull(
                                items.items
                            )
                        }
                        .flatMapConcat { dtos ->
                            val dbos: MutableList<FriendListEntity> = ArrayList(dtos.size)
                            val data: MutableMap<Long, FriendList> = HashMap(mp.size)
                            for (dto in dtos) {
                                dbos.add(FriendListEntity(dto.id, dto.name))
                            }
                            for (id in ids) {
                                var found = false
                                for (dto in dtos) {
                                    if (dto.id == id) {
                                        data[id] = transform(dto)
                                        found = true
                                        break
                                    }
                                }
                                if (!found) {
                                    mp[id] = FriendListEntity(id, "UNKNOWN")
                                }
                            }
                            stores.relativeship()
                                .storeFriendsList(accountId, userId, dbos)
                                .map {
                                    data
                                }
                        }
                }
            }
    }

    override fun getLastShortenedLinks(
        accountId: Long,
        count: Int?,
        offset: Int?
    ): Flow<List<ShortLink>> {
        return networker.vkDefault(accountId)
            .utils()
            .getLastShortenedLinks(count, offset)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { out ->
                val ret: MutableList<ShortLink> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getShortLink(accountId: Long, url: String?, t_private: Int?): Flow<ShortLink> {
        return networker.vkDefault(accountId)
            .utils()
            .getShortLink(url, t_private)
            .map { obj -> transform(obj) }
    }

    override fun deleteFromLastShortened(accountId: Long, key: String?): Flow<Int> {
        return networker.vkDefault(accountId)
            .utils()
            .deleteFromLastShortened(key)
    }

    override fun checkLink(accountId: Long, url: String?): Flow<VKApiCheckedLink> {
        return networker.vkDefault(accountId)
            .utils()
            .checkLink(url)
    }

    override fun joinChatByInviteLink(accountId: Long, link: String?): Flow<VKApiChatResponse> {
        return networker.vkDefault(accountId)
            .utils()
            .joinChatByInviteLink(link)
    }

    override fun getInviteLink(
        accountId: Long,
        peer_id: Long?,
        reset: Int?
    ): Flow<VKApiLinkResponse> {
        return networker.vkDefault(accountId)
            .utils()
            .getInviteLink(peer_id, reset)
    }

    override fun customScript(accountId: Long, code: String?): Flow<Int> {
        return networker.vkDefault(accountId)
            .utils()
            .customScript(code)
    }

    override fun getServerTime(accountId: Long): Flow<Long> {
        return networker.vkDefault(accountId)
            .utils()
            .getServerTime()
    }
}