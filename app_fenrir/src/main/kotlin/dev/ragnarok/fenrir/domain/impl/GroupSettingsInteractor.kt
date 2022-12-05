package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.GroupSettingsDto
import dev.ragnarok.fenrir.api.model.GroupSettingsDto.PublicCategory
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiCommunity.Contact
import dev.ragnarok.fenrir.db.interfaces.IOwnersStorage
import dev.ragnarok.fenrir.db.model.BanAction
import dev.ragnarok.fenrir.domain.IGroupSettingsInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformCommunity
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUser
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.findById
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class GroupSettingsInteractor(
    private val networker: INetworker,
    private val repository: IOwnersStorage,
    private val ownersRepository: IOwnersRepository
) : IGroupSettingsInteractor {
    override fun getGroupSettings(accountId: Int, groupId: Int): Single<GroupSettings> {
        return networker.vkDefault(accountId)
            .groups()
            .getSettings(groupId)
            .flatMap { dto -> Single.just(createFromDto(dto)) }
    }

    override fun ban(
        accountId: Int,
        groupId: Int,
        ownerId: Int,
        endDateUnixtime: Long?,
        reason: Int,
        comment: String?,
        commentVisible: Boolean
    ): Completable {
        return networker.vkDefault(accountId)
            .groups()
            .ban(groupId, ownerId, endDateUnixtime, reason, comment, commentVisible)
            .andThen(repository.fireBanAction(BanAction(groupId, ownerId, true)))
    }

    override fun edit(
        accountId: Int,
        groupId: Int,
        title: String?,
        description: String?,
        screen_name: String?,
        access: Int?,
        website: String?,
        public_category: Int?,
        public_date: String?,
        age_limits: Int?,
        obscene_filter: Int?,
        obscene_stopwords: Int?,
        obscene_words: String?
    ): Completable {
        return networker.vkDefault(accountId)
            .groups()
            .edit(
                groupId,
                title,
                description,
                screen_name,
                access,
                website,
                public_category,
                public_date,
                age_limits,
                obscene_filter,
                obscene_stopwords,
                obscene_words
            )
    }

    override fun editManager(
        accountId: Int,
        groupId: Int,
        user: User,
        role: String?,
        asContact: Boolean,
        position: String?,
        email: String?,
        phone: String?
    ): Completable {
        val targetRole = if ("creator".equals(role, ignoreCase = true)) "administrator" else role
        return networker.vkDefault(accountId)
            .groups()
            .editManager(groupId, user.getObjectId(), targetRole, asContact, position, email, phone)
            .andThen(
                Single
                    .fromCallable {
                        val info = ContactInfo(user.getObjectId())
                            .setDescription(position)
                            .setPhone(phone)
                            .setEmail(email)
                        Manager(user, role)
                            .setContactInfo(info)
                            .setDisplayAsContact(asContact)
                    }
                    .flatMapCompletable { manager: Manager ->
                        repository.fireManagementChangeAction(
                            create(groupId, manager)
                        )
                    })
    }

    override fun unban(accountId: Int, groupId: Int, ownerId: Int): Completable {
        return networker.vkDefault(accountId)
            .groups()
            .unban(groupId, ownerId)
            .andThen(repository.fireBanAction(BanAction(groupId, ownerId, false)))
    }

    override fun getBanned(
        accountId: Int,
        groupId: Int,
        startFrom: IntNextFrom,
        count: Int
    ): Single<Pair<List<Banned>, IntNextFrom>> {
        val nextFrom = IntNextFrom(startFrom.offset + count)
        return networker.vkDefault(accountId)
            .groups()
            .getBanned(groupId, startFrom.offset, count, Constants.MAIN_OWNER_FIELDS, null)
            .map { obj -> obj.items.orEmpty() }
            .flatMap { items ->
                val ids = VKOwnIds()
                for (u in items) {
                    ids.append(u.banInfo?.adminId)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY
                )
                    .map { bundle ->
                        val infos: MutableList<Banned> = ArrayList(items.size)
                        for (u in items) {
                            var admin: User
                            val banInfo = u.banInfo ?: continue
                            admin = if (banInfo.adminId != 0) {
                                bundle.getById(banInfo.adminId) as User
                            } else {
                                // ignore this
                                continue
                            }
                            val info = Banned.Info()
                                .setComment(banInfo.comment)
                                .setCommentVisible(banInfo.commentVisible)
                                .setDate(banInfo.date)
                                .setEndDate(banInfo.endDate)
                                .setReason(banInfo.reason)
                            u.profile.requireNonNull {
                                infos.add(Banned(transformUser(it), admin, info))
                            }
                            u.group.requireNonNull {
                                val jj = transformCommunity(it)
                                jj?.let { bb ->
                                    infos.add(Banned(bb, admin, info))
                                }
                            }
                        }
                        create(infos, nextFrom)
                    }
            }
    }

    override fun getContacts(accountId: Int, groupId: Int): Single<List<ContactInfo>> {
        return networker.vkDefault(accountId).groups()
            .getById(setOf(groupId), null, null, "contacts")
            .map { communities ->
                val temps = listEmptyIfNull(
                    communities[0].contacts
                )
                val managers: MutableList<ContactInfo> = ArrayList(temps.size)
                for (user in temps) {
                    managers.add(transform(user))
                }
                managers
            }
    }

    override fun getManagers(accountId: Int, groupId: Int): Single<List<Manager>> {
        return networker.vkDefault(accountId)
            .groups()
            .getMembers(
                groupId.toString(),
                null,
                null,
                null,
                Constants.MAIN_OWNER_FIELDS,
                "managers"
            )
            .flatMap { items ->
                networker.vkDefault(accountId)
                    .groups()
                    .getById(setOf(groupId), null, null, "contacts")
                    .map { communities: List<VKApiCommunity> ->
                        if (communities.isEmpty()) {
                            throw NotFoundException("Group with id $groupId not found")
                        }
                        listEmptyIfNull(communities[0].contacts)
                    }
                    .map<List<Manager>> {
                        val users = listEmptyIfNull(
                            items.items
                        )
                        val managers: MutableList<Manager> = ArrayList(users.size)
                        for (user in users) {
                            val contact = findById(it, user.id)
                            val manager = Manager(transformUser(user), user.role)
                            if (contact != null) {
                                manager.setDisplayAsContact(true).setContactInfo(transform(contact))
                            }
                            managers.add(manager)
                        }
                        managers
                    }
            }
    }

    private fun createFromDto(category: PublicCategory): IdOption {
        return IdOption(category.id, category.name,
            category.subtypes_list?.let { createFromDtos(it) })
    }

    private fun createFromDtos(dtos: List<PublicCategory>): List<IdOption> {
        if (dtos.isEmpty()) {
            return emptyList()
        }
        val categories: MutableList<IdOption> = ArrayList(dtos.size)
        for (dto in dtos) {
            categories.add(createFromDto(dto))
        }
        return categories
    }

    private fun createFromDto(dto: GroupSettingsDto): GroupSettings {
        val categories = dto.public_category_list?.let { createFromDtos(it) }
        var category: IdOption? = null
        var subcategory: IdOption? = null
        dto.public_category.requireNonNull {
            category = findById(categories, it.toInt())
            dto.public_subcategory.requireNonNull { o ->
                category.requireNonNull { p ->
                    subcategory = findById(p.childs, o.toInt())
                }
            }
        }
        return GroupSettings()
            .setTitle(dto.title)
            .setAge(dto.age_limits)
            .setAccess(dto.access)
            .setDescription(dto.description)
            .setAddress(dto.address)
            .setAvailableCategories(categories)
            .setCategory(category)
            .setSubcategory(subcategory)
            .setWebsite(dto.website)
            .setDateCreated(parseDateCreated(dto.public_date))
            .setFeedbackCommentsEnabled(dto.wall == 1)
            .setObsceneFilterEnabled(dto.obscene_filter)
            .setObsceneStopwordsEnabled(dto.obscene_stopwords)
            .setObsceneWords(
                join(
                    dto.obscene_words,
                    ",",
                    object : Utils.SimpleFunction<String, String> {
                        override fun apply(orig: String): String {
                            return orig
                        }
                    })
            )
    }

    companion object {
        internal fun transform(contact: Contact): ContactInfo {
            return ContactInfo(contact.user_id)
                .setDescription(contact.desc)
                .setEmail(contact.email)
                .setPhone(contact.phone)
        }

        internal fun parseDateCreated(text: String?): Day? {
            if (text.isNullOrEmpty()) {
                return null
            }
            val parts = text.split(Regex("\\.")).toTypedArray()
            return Day(
                parseInt(parts, 0, 0),
                parseInt(parts, 1, 0),
                parseInt(parts, 2, 0)
            )
        }

        private fun parseInt(parts: Array<String>, index: Int, ifNotExists: Int): Int {
            return if (parts.size <= index) {
                ifNotExists
            } else parts[index].toInt()
        }
    }
}