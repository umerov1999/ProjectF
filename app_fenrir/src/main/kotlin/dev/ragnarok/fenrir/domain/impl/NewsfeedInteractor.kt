package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.CommentsDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.Dto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.PhotoDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.PostDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.TopicDto
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.VideoDto
import dev.ragnarok.fenrir.domain.INewsfeedInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.buildComment
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.model.PhotoWithOwner
import dev.ragnarok.fenrir.model.TopicWithOwner
import dev.ragnarok.fenrir.model.VideoWithOwner
import dev.ragnarok.fenrir.nonNullNoEmptyOrNullable
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class NewsfeedInteractor(
    private val networker: INetworker,
    private val ownersRepository: IOwnersRepository
) : INewsfeedInteractor {
    override fun getMentions(
        accountId: Long,
        owner_id: Long?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Flow<Pair<List<NewsfeedComment>, String?>> {
        return networker.vkDefault(accountId)
            .newsfeed()
            .getMentions(owner_id, count, offset, startTime, endTime)
            .flatMapConcat { response ->
                val owners = transformOwners(response.profiles, response.groups)
                val ownIds = VKOwnIds()
                val dtos = listEmptyIfNull(response.items)
                for (dto in dtos) {
                    if (dto is PostDto) {
                        val post = dto.post ?: continue
                        ownIds.append(post)
                        ownIds.append(post.comments)
                    }
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map { bundle ->
                        val comments: MutableList<NewsfeedComment> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            val comment = createFrom(dto, bundle)
                            if (comment != null) {
                                comments.add(comment)
                            }
                        }
                        create(comments, response.nextFrom)
                    }
            }
    }

    override fun getNewsfeedComments(
        accountId: Long,
        count: Int,
        startFrom: String?,
        filter: String?
    ): Flow<Pair<List<NewsfeedComment>, String?>> {
        return networker.vkDefault(accountId)
            .newsfeed()
            .getComments(
                count, filter, null, null, null,
                1, startFrom, Fields.FIELDS_BASE_OWNER
            )
            .flatMapConcat { response ->
                val owners = transformOwners(response.profiles, response.groups)
                val ownIds = VKOwnIds()
                val dtos = listEmptyIfNull(response.items)
                for (dto in dtos) {
                    when (dto) {
                        is PostDto -> {
                            val post = dto.post ?: continue
                            ownIds.append(post)
                            ownIds.append(post.comments)
                        }

                        is PhotoDto -> {
                            val photo = dto.photo ?: continue
                            ownIds.append(photo.owner_id)
                            ownIds.append(photo.comments)
                        }

                        is TopicDto -> {
                            val topic = dto.topic ?: continue
                            ownIds.append(topic.owner_id)
                            ownIds.append(topic.comments)
                        }

                        is VideoDto -> {
                            val video = dto.video ?: continue
                            ownIds.append(video.owner_id)
                            ownIds.append(video.comments)
                        }
                    }
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map { bundle ->
                        val comments: MutableList<NewsfeedComment> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            val comment = createFrom(dto, bundle)
                            if (comment != null) {
                                comments.add(comment)
                            }
                        }
                        create(comments, response.nextFrom)
                    }
            }
    }

    companion object {
        private fun oneCommentFrom(
            commented: Commented,
            dto: CommentsDto?,
            bundle: IOwnersBundle
        ): Comment? {
            return dto?.list.nonNullNoEmptyOrNullable {
                buildComment(commented, it[it.size - 1], bundle)
            }
        }

        internal fun createFrom(dto: Dto, bundle: IOwnersBundle): NewsfeedComment? {
            if (dto is PhotoDto) {
                val photoDto = dto.photo
                val photo = transform(photoDto ?: return null)
                val commented = Commented.from(photo)
                val photoOwner = bundle.getById(photo.ownerId)
                return NewsfeedComment(PhotoWithOwner(photo, photoOwner))
                    .setComment(oneCommentFrom(commented, photoDto.comments, bundle))
            }
            if (dto is VideoDto) {
                val videoDto = dto.video
                val video = transform(videoDto ?: return null)
                val commented = Commented.from(video)
                val videoOwner = bundle.getById(video.ownerId)
                return NewsfeedComment(VideoWithOwner(video, videoOwner))
                    .setComment(oneCommentFrom(commented, videoDto.comments, bundle))
            }
            if (dto is PostDto) {
                val postDto = dto.post
                val post = transform(postDto ?: return null, bundle)
                val commented = Commented.from(post)
                return NewsfeedComment(post).setComment(
                    oneCommentFrom(
                        commented,
                        postDto.comments,
                        bundle
                    )
                )
            }
            if (dto is TopicDto) {
                val topicDto = dto.topic
                val topic = transform(topicDto ?: return null, bundle)
                topicDto.comments.requireNonNull {
                    topic.setCommentsCount(it.count)
                }
                val commented = Commented.from(topic)
                val owner = bundle.getById(topic.ownerId)
                return NewsfeedComment(TopicWithOwner(topic, owner)).setComment(
                    oneCommentFrom(
                        commented,
                        topicDto.comments,
                        bundle
                    )
                )
            }
            return null
        }
    }
}