package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.feedback.CopyEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.FeedbackEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.LikeCommentEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.LikeEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.MentionCommentEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.MentionEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.NewCommentEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.PostFeedbackEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.ReplyCommentEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.UsersEntity
import dev.ragnarok.fenrir.domain.IFeedbackInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.buildFeedbackDbo
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillCommentOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillOwnerIds
import dev.ragnarok.fenrir.domain.mappers.FeedbackEntity2Model.buildFeedback
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.model.criteria.NotificationsCriteria
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class FeedbackInteractor(
    private val cache: IStorages,
    private val networker: INetworker,
    private val ownersRepository: IOwnersRepository
) : IFeedbackInteractor {
    override fun getCachedFeedbacks(accountId: Long): Flow<List<Feedback>> {
        val criteria = NotificationsCriteria(accountId)
        return cache.notifications()
            .findByCriteria(criteria)
            .flatMapConcat { dbos ->
                val ownIds = VKOwnIds()
                for (dbo in dbos) {
                    populateOwnerIds(ownIds, dbo)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    criteria.accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY
                )
                    .map {
                        val feedbacks: MutableList<Feedback> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            feedbacks.add(buildFeedback(dbo, it))
                        }
                        feedbacks
                    }
            }
    }

    override fun getCachedFeedbacksOfficial(accountId: Long): Flow<FeedbackVKOfficialList> {
        val criteria = NotificationsCriteria(accountId)
        return cache.notifications()
            .findByCriteriaOfficial(criteria)
            .map { dbos ->
                val ret = FeedbackVKOfficialList()
                ret.items = ArrayList(dbos)
                ret
            }
    }

    override fun getActualFeedbacksOfficial(
        accountId: Long,
        count: Int?,
        startFrom: Int?
    ): Flow<FeedbackVKOfficialList> {
        return networker.vkDefault(accountId)
            .notifications()
            .getOfficial(count, startFrom, null, null, null)
            .flatMapConcat { uit ->
                cache.notifications()
                    .insertOfficial(accountId, uit.items.orEmpty(), startFrom?.orZero() == 0)
                    .map {
                        uit
                    }
            }
    }

    override fun hide(accountId: Long, query: String?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .notifications()
            .hide(query)
            .ignoreElement()
    }

    override fun getActualFeedbacks(
        accountId: Long,
        count: Int,
        startFrom: String?
    ): Flow<Pair<List<Feedback>, String?>> {
        return networker.vkDefault(accountId)
            .notifications()[count, startFrom, null, null, null]
            .flatMapConcat { response ->
                val dtos = listEmptyIfNull(response.notifications)
                val dbos: MutableList<FeedbackEntity> = ArrayList(dtos.size)
                val ownIds = VKOwnIds()
                for (dto in dtos) {
                    val dbo = buildFeedbackDbo(dto)
                    populateOwnerIds(ownIds, dbo)
                    dbos.add(dbo)
                }
                val ownerEntities = mapOwners(response.profiles, response.groups)
                val owners = transformOwners(response.profiles, response.groups)
                cache.notifications()
                    .insert(accountId, dbos, ownerEntities, startFrom.isNullOrEmpty())
                    .flatMapConcat {
                        ownersRepository
                            .findBaseOwnersDataAsBundle(
                                accountId,
                                ownIds.all,
                                IOwnersRepository.MODE_ANY,
                                owners
                            )
                            .map {
                                val feedbacks: MutableList<Feedback> = ArrayList(dbos.size)
                                for (dbo in dbos) {
                                    feedbacks.add(buildFeedback(dbo, it))
                                }
                                create(feedbacks, response.nextFrom)
                            }
                    }
            }
    }

    override fun markAsViewed(accountId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .notifications()
            .markAsViewed()
            .map { it != 0 }
    }

    companion object {
        internal fun populateOwnerIds(ids: VKOwnIds, dbo: FeedbackEntity) {
            fillCommentOwnerIds(ids, dbo.reply)
            when (dbo) {
                is CopyEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is LikeCommentEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is LikeEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is MentionCommentEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is MentionEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is NewCommentEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is PostFeedbackEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is ReplyCommentEntity -> {
                    populateOwnerIds(ids, dbo)
                }

                is UsersEntity -> {
                    populateOwnerIds(ids, dbo)
                }
            }
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: UsersEntity) {
            ids.appendAll(dbo.owners)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: ReplyCommentEntity) {
            fillOwnerIds(ids, dbo.commented)
            fillOwnerIds(ids, dbo.feedbackComment)
            fillOwnerIds(ids, dbo.ownComment)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: PostFeedbackEntity) {
            fillOwnerIds(ids, dbo.post)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: NewCommentEntity) {
            fillOwnerIds(ids, dbo.comment)
            fillOwnerIds(ids, dbo.commented)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: MentionEntity) {
            fillOwnerIds(ids, dbo.where)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: MentionCommentEntity) {
            fillOwnerIds(ids, dbo.commented)
            fillOwnerIds(ids, dbo.where)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: LikeEntity) {
            fillOwnerIds(ids, dbo.liked)
            ids.appendAll(dbo.likesOwnerIds)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: LikeCommentEntity) {
            fillOwnerIds(ids, dbo.liked)
            fillOwnerIds(ids, dbo.commented)
            ids.appendAll(dbo.likesOwnerIds)
        }

        private fun populateOwnerIds(ids: VKOwnIds, dbo: CopyEntity) {
            for (i in dbo.copies?.pairDbos.orEmpty()) {
                ids.append(i.ownerId)
            }
            fillOwnerIds(ids, dbo.copied)
        }
    }
}