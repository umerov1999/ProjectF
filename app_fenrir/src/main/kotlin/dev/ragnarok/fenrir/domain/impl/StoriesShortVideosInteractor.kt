package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.response.StoryBlockResponce
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IStoriesShortVideosInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwner
import dev.ragnarok.fenrir.domain.mappers.MapUtil
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Single

class StoriesShortVideosInteractor(
    private val networker: INetworker,
    private val ownersRepository: IOwnersRepository
) :
    IStoriesShortVideosInteractor {
    override fun getStoriesViewers(
        accountId: Long,
        ownerId: Long,
        storyId: Int,
        count: Int,
        offset: Int
    ): Single<List<Pair<Owner, Boolean>>> {
        return networker.vkDefault(accountId)
            .stories()
            .getStoriesViewers(
                ownerId,
                storyId,
                offset,
                count
            )
            .map { response ->
                val dtos = listEmptyIfNull(response.ownersWithLikes)
                val owners: MutableList<Pair<Owner, Boolean>> = ArrayList(dtos.size)
                for ((first, second) in dtos) {
                    transformOwner(first)?.let { owners.add(Pair(it, second)) }
                }
                owners
            }
    }

    private fun parseParentStory(story: List<VKApiStory>, dtos: MutableList<VKApiStory>) {
        for (i in story) {
            i.parent_story.requireNonNull {
                dtos.add(it)
            }
        }
    }

    private fun parseStoryBlock(resp: StoryBlockResponce, dtos: MutableList<VKApiStory>) {
        resp.stories.nonNullNoEmpty {
            parseParentStory(it, dtos)
            dtos.addAll(it)
        }
        resp.grouped.nonNullNoEmpty {
            for (i in it) {
                parseStoryBlock(i, dtos)
            }
        }
    }

    override fun getNarratives(
        accountId: Long,
        owner_id: Long,
        offset: Int?,
        count: Int?
    ): Single<List<Narratives>> {
        return networker.vkDefault(accountId)
            .stories()
            .getNarratives(owner_id, offset, count)
            .flatMap { story ->
                val dtos = listEmptyIfNull(story.items)
                Single.just(MapUtil.mapAll(dtos) { transform(it) })
            }
    }

    override fun getStoryById(accountId: Long, stories: List<AccessIdPair>): Single<List<Story>> {
        return networker.vkDefault(accountId)
            .stories()
            .getStoryById(stories, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { story ->
                val dtos = listEmptyIfNull(story.items)
                val owners = Dto2Model.transformOwners(story.profiles, story.groups)
                val ownIds = VKOwnIds()
                for (news in dtos) {
                    ownIds.appendStory(news)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map<List<Story>> { owners1: IOwnersBundle ->
                        val storiesDto: MutableList<Story> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            storiesDto.add(Dto2Model.transformStory(dto, owners1))
                        }
                        storiesDto
                    }
            }
    }

    override fun stories_delete(accountId: Long, owner_id: Long, story_id: Int): Single<Int> {
        return networker.vkDefault(accountId)
            .stories().stories_delete(owner_id, story_id)
    }

    override fun getStories(accountId: Long, owner_id: Long?): Single<List<Story>> {
        return networker.vkDefault(accountId)
            .stories()
            .getStories(owner_id, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { story ->
                val dtos_multy = listEmptyIfNull(story.items)
                val dtos: MutableList<VKApiStory> = ArrayList()
                for (itst in dtos_multy) {
                    parseStoryBlock(itst, dtos)
                }
                val owners = Dto2Model.transformOwners(story.profiles, story.groups)
                val ownIds = VKOwnIds()
                for (news in dtos) {
                    ownIds.appendStory(news)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map<List<Story>> { owners1: IOwnersBundle ->
                        val blockAds = Settings.get().main().isAd_block_story_news
                        val stories: MutableList<Story> = ArrayList()
                        for (dto in dtos) {
                            if (dto.is_ads && blockAds) {
                                continue
                            }
                            stories.add(Dto2Model.transformStory(dto, owners1))
                        }
                        stories
                    }
            }
    }

    override fun searchStories(
        accountId: Long,
        q: String?,
        mentioned_id: Long?
    ): Single<List<Story>> {
        return networker.vkDefault(accountId)
            .stories()
            .searchStories(q, mentioned_id, 1000, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { story ->
                val dtos_multy = listEmptyIfNull(story.items)
                val dtos: MutableList<VKApiStory> = ArrayList()
                for (itst in dtos_multy) {
                    parseStoryBlock(itst, dtos)
                }
                val owners = Dto2Model.transformOwners(story.profiles, story.groups)
                val ownIds = VKOwnIds()
                for (news in dtos) {
                    ownIds.appendStory(news)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownIds.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map<List<Story>> { owners1: IOwnersBundle ->
                        val stories: MutableList<Story> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            stories.add(Dto2Model.transformStory(dto, owners1))
                        }
                        stories
                    }
            }
    }

    override fun getShortVideos(
        accountId: Long,
        ownerId: Long?,
        startFrom: String?,
        count: Int?
    ): Single<Pair<List<Video>, String?>> {
        return networker.vkDefault(accountId)
            .stories()
            .getShortVideos(ownerId, startFrom, count, 1, Fields.FIELDS_BASE_OWNER)
            .map { response ->
                val nextFrom = response.nextFrom
                val videos = listEmptyIfNull(response.items)
                val dbos: MutableList<Video> = ArrayList(videos.size)
                for (i in videos) {
                    dbos.add(
                        transform(i).setOptionalOwner(
                            transformOwner(
                                Utils.ownerOfApiOwner(
                                    response.profiles,
                                    response.groups,
                                    i.owner_id
                                )
                            )
                        )
                    )
                }
                Pair(dbos, nextFrom)
            }
    }
}