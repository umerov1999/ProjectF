package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.VKApiConversation.CurrentKeyboard
import dev.ragnarok.fenrir.api.model.VKApiSticker.VKApiAnimation
import dev.ragnarok.fenrir.api.model.VKApiStickerSet.Product
import dev.ragnarok.fenrir.api.model.feedback.*
import dev.ragnarok.fenrir.api.model.interfaces.Commentable
import dev.ragnarok.fenrir.api.model.interfaces.Likeable
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.crypt.CryptHelper.analizeMessageBody
import dev.ragnarok.fenrir.crypt.MessageType
import dev.ragnarok.fenrir.db.model.IdPairEntity
import dev.ragnarok.fenrir.db.model.entity.*
import dev.ragnarok.fenrir.db.model.entity.AudioArtistDboEntity.AudioArtistImageEntity
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.GraffitiDbo
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.VideoPreviewDbo
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity.ButtonEntity
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity.SourceDbo
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity.AnimationEntity
import dev.ragnarok.fenrir.db.model.entity.UserDetailsEntity.RelativeEntity
import dev.ragnarok.fenrir.db.model.entity.feedback.*
import dev.ragnarok.fenrir.domain.mappers.MapUtil.calculateConversationAcl
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.MessageStatus
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.transformNonNullNullable
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf

object Dto2Entity {
    fun buildFeedbackDbo(feedback: VKApiBaseFeedback): FeedbackEntity {
        when (@FeedbackType val type = FeedbackEntity2Model.transformType(feedback.type)) {
            FeedbackType.FOLLOW, FeedbackType.FRIEND_ACCEPTED -> {
                val usersNotification = feedback as VKApiUsersFeedback
                val usersDbo = UsersEntity(type)
                usersDbo.setOwners(usersNotification.users?.ids)
                usersDbo.setDate(feedback.date)
                return usersDbo
            }

            FeedbackType.MENTION -> {
                val mentionWallFeedback = feedback as VKApiMentionWallFeedback
                val mentionDbo = MentionEntity(type)
                val post =
                    mapPost(mentionWallFeedback.post ?: throw NullPointerException("Feedback"))
                mentionDbo.setWhere(post)
                feedback.reply.requireNonNull {
                    mentionDbo.setReply(
                        mapComment(
                            post.id,
                            post.ownerId,
                            CommentedType.POST,
                            null,
                            it
                        )
                    )
                }
                mentionDbo.setDate(feedback.date)
                return mentionDbo
            }

            FeedbackType.MENTION_COMMENT_POST, FeedbackType.MENTION_COMMENT_PHOTO, FeedbackType.MENTION_COMMENT_VIDEO -> {
                val mentionCommentFeedback = feedback as VKApiMentionCommentFeedback
                val entity = createFromCommentable(
                    mentionCommentFeedback.comment_of ?: throw NullPointerException("Feedback")
                )
                val mentionCommentDbo = MentionCommentEntity(type)
                mentionCommentDbo.setDate(feedback.date)
                mentionCommentDbo.setCommented(entity.dboEntity)
                mentionCommentDbo.setWhere(
                    mapComment(
                        entity.id,
                        entity.ownerId,
                        entity.type,
                        entity.accessKey,
                        mentionCommentFeedback.where ?: throw NullPointerException("Feedback")
                    )
                )
                feedback.reply.requireNonNull {
                    mentionCommentDbo.setReply(
                        mapComment(
                            entity.id,
                            entity.ownerId,
                            entity.type,
                            entity.accessKey,
                            it
                        )
                    )
                }
                return mentionCommentDbo
            }

            FeedbackType.WALL, FeedbackType.WALL_PUBLISH -> {
                val wallFeedback = feedback as VKApiWallFeedback
                val postEntity =
                    mapPost(wallFeedback.post ?: throw NullPointerException("Feedback"))
                val postFeedbackEntity = PostFeedbackEntity(type)
                postFeedbackEntity.setDate(feedback.date)
                postFeedbackEntity.setPost(postEntity)
                feedback.reply.requireNonNull {
                    postFeedbackEntity.setReply(
                        mapComment(
                            postEntity.id,
                            postEntity.ownerId,
                            CommentedType.POST,
                            null,
                            it
                        )
                    )
                }
                return postFeedbackEntity
            }

            FeedbackType.COMMENT_POST, FeedbackType.COMMENT_PHOTO, FeedbackType.COMMENT_VIDEO -> {
                val commentFeedback = feedback as VKApiCommentFeedback
                val commented = createFromCommentable(
                    commentFeedback.comment_of ?: throw NullPointerException("Feedback")
                )
                val commentEntity = NewCommentEntity(type)
                commentEntity.setComment(
                    mapComment(
                        commented.id,
                        commented.ownerId,
                        commented.type,
                        commented.accessKey,
                        commentFeedback.comment ?: throw NullPointerException("Feedback")
                    )
                )
                commentEntity.setCommented(commented.dboEntity)
                commentEntity.setDate(feedback.date)
                feedback.reply.requireNonNull {
                    commentEntity.setReply(
                        mapComment(
                            commented.id,
                            commented.ownerId,
                            commented.type,
                            commented.accessKey,
                            it
                        )
                    )
                }
                return commentEntity
            }

            FeedbackType.REPLY_COMMENT, FeedbackType.REPLY_COMMENT_PHOTO, FeedbackType.REPLY_COMMENT_VIDEO, FeedbackType.REPLY_TOPIC -> {
                val replyCommentFeedback = feedback as VKApiReplyCommentFeedback
                val c = createFromCommentable(
                    replyCommentFeedback.comments_of ?: throw NullPointerException("Feedback")
                )
                val replyCommentEntity = ReplyCommentEntity(type)
                replyCommentEntity.setDate(feedback.date)
                replyCommentEntity.setCommented(c.dboEntity)
                replyCommentEntity.setFeedbackComment(
                    mapComment(
                        c.id,
                        c.ownerId,
                        c.type,
                        c.accessKey,
                        replyCommentFeedback.feedback_comment
                            ?: throw NullPointerException("Feedback")
                    )
                )
                replyCommentFeedback.own_comment.requireNonNull {
                    replyCommentEntity.setOwnComment(
                        mapComment(
                            c.id,
                            c.ownerId,
                            c.type,
                            c.accessKey,
                            it
                        )
                    )
                }
                feedback.reply.requireNonNull {
                    replyCommentEntity.setReply(
                        mapComment(
                            c.id,
                            c.ownerId,
                            c.type,
                            c.accessKey,
                            it
                        )
                    )
                }
                return replyCommentEntity
            }

            FeedbackType.LIKE_POST, FeedbackType.LIKE_PHOTO, FeedbackType.LIKE_VIDEO -> {
                val likeFeedback = feedback as VKApiLikeFeedback
                val likeEntity = LikeEntity(type)
                likeEntity.setLiked(
                    createFromLikeable(
                        likeFeedback.liked ?: throw NullPointerException("Feedback")
                    )
                )
                likeEntity.setLikesOwnerIds(likeFeedback.users?.ids)
                likeEntity.setDate(feedback.date)
                return likeEntity
            }

            FeedbackType.LIKE_COMMENT_POST, FeedbackType.LIKE_COMMENT_PHOTO, FeedbackType.LIKE_COMMENT_VIDEO, FeedbackType.LIKE_COMMENT_TOPIC -> {
                val likeCommentFeedback = feedback as VKApiLikeCommentFeedback
                val ce = createFromCommentable(
                    likeCommentFeedback.commented ?: throw NullPointerException("Feedback")
                )
                val likeCommentEntity = LikeCommentEntity(type)
                likeCommentEntity.setCommented(ce.dboEntity)
                likeCommentEntity.setLiked(
                    mapComment(
                        ce.id,
                        ce.ownerId,
                        ce.type,
                        ce.accessKey,
                        likeCommentFeedback.comment ?: throw NullPointerException("Feedback")
                    )
                )
                likeCommentEntity.setDate(feedback.date)
                likeCommentEntity.setLikesOwnerIds(likeCommentFeedback.users?.ids)
                return likeCommentEntity
            }

            FeedbackType.COPY_POST, FeedbackType.COPY_PHOTO, FeedbackType.COPY_VIDEO -> {
                val copyFeedback = feedback as VKApiCopyFeedback
                val copyEntity = CopyEntity(type)
                copyEntity.setDate(feedback.date)
                when (type) {
                    FeedbackType.COPY_POST -> {
                        copyEntity.setCopied(mapPost(copyFeedback.what as VKApiPost))
                    }

                    FeedbackType.COPY_PHOTO -> {
                        copyEntity.setCopied(mapPhoto(copyFeedback.what as VKApiPhoto))
                    }

                    else -> {
                        copyEntity.setCopied(mapVideo(copyFeedback.what as VKApiVideo))
                    }
                }
                val copyPairs = listEmptyIfNull(copyFeedback.copies?.pairs)
                val copiesEntity = CopiesEntity()
                copiesEntity.setPairDbos(ArrayList(copyPairs.size))
                for (idPair in copyPairs) {
                    copiesEntity.pairDbos?.add(IdPairEntity().set(idPair.id, idPair.owner_id))
                }
                copyEntity.setCopies(copiesEntity)
                return copyEntity
            }

            FeedbackType.NULL -> {}
        }
        throw UnsupportedOperationException("Unsupported feedback type: " + feedback.type)
    }

    private fun createFromLikeable(likeable: Likeable): DboEntity {
        if (likeable is VKApiPost) {
            return mapPost(likeable)
        }
        if (likeable is VKApiPhoto) {
            return mapPhoto(likeable)
        }
        if (likeable is VKApiVideo) {
            return mapVideo(likeable)
        }
        throw UnsupportedOperationException("Unsupported commentable type: $likeable")
    }

    private fun createFromCommentable(commentable: Commentable): CEntity {
        if (commentable is VKApiPost) {
            val entity = mapPost(commentable)
            return CEntity(entity.id, entity.ownerId, CommentedType.POST, null, entity)
        }
        if (commentable is VKApiPhoto) {
            val entity = mapPhoto(commentable)
            return CEntity(entity.id, entity.ownerId, CommentedType.PHOTO, entity.accessKey, entity)
        }
        if (commentable is VKApiVideo) {
            val entity = mapVideo(commentable)
            return CEntity(entity.id, entity.ownerId, CommentedType.VIDEO, entity.accessKey, entity)
        }
        if (commentable is VKApiTopic) {
            val entity = buildTopicDbo(commentable)
            return CEntity(entity.id, entity.ownerId, CommentedType.TOPIC, null, entity)
        }
        throw UnsupportedOperationException("Unsupported commentable type: $commentable")
    }


    fun buildVideoAlbumDbo(dto: VKApiVideoAlbum): VideoAlbumDboEntity {
        return VideoAlbumDboEntity(dto.id, dto.owner_id)
            .setUpdateTime(dto.updated_time)
            .setCount(dto.count)
            .setImage(dto.image)
            .setTitle(dto.title)
            .setPrivacy(dto.privacy?.let { mapPrivacy(it) })
    }


    fun buildTopicDbo(dto: VKApiTopic): TopicDboEntity {
        return TopicDboEntity().set(dto.id, dto.owner_id)
            .setTitle(dto.title)
            .setCreatedTime(dto.created)
            .setCreatorId(dto.created_by)
            .setLastUpdateTime(dto.updated)
            .setUpdatedBy(dto.updated_by)
            .setClosed(dto.is_closed)
            .setFixed(dto.is_fixed)
            .setCommentsCount(dto.comments?.count.orZero())
            .setFirstComment(dto.first_comment)
            .setLastComment(dto.last_comment)
            .setPoll(null)
    }


    fun buildPhotoAlbumDbo(dto: VKApiPhotoAlbum): PhotoAlbumDboEntity {
        return PhotoAlbumDboEntity().set(dto.id, dto.owner_id)
            .setTitle(dto.title)
            .setSize(dto.size)
            .setDescription(dto.description)
            .setCanUpload(dto.can_upload)
            .setUpdatedTime(dto.updated)
            .setCreatedTime(dto.created)
            .setSizes(if (dto.photo != null) mapPhotoSizes(dto.photo) else null)
            .setCommentsDisabled(dto.comments_disabled)
            .setUploadByAdminsOnly(dto.upload_by_admins_only)
            .setPrivacyView(dto.privacy_view?.let { mapPrivacy(it) })
            .setPrivacyComment(dto.privacy_comment?.let { mapPrivacy(it) })
    }


    fun mapOwners(users: List<VKApiUser>?, communities: List<VKApiCommunity>?): OwnerEntities {
        return OwnerEntities(mapUsers(users), mapCommunities(communities))
    }


    fun mapCommunities(communities: List<VKApiCommunity>?): List<CommunityEntity> {
        return mapAll(
            communities
        ) {
            mapCommunity(it)
        }
    }


    fun mapUsers(users: List<VKApiUser>?): List<UserEntity> {
        return mapAll(
            users
        ) {
            mapUser(it)
        }
    }


    fun mapCommunity(community: VKApiCommunity): CommunityEntity {
        return CommunityEntity(community.id)
            .setName(community.fullName)
            .setScreenName(community.screen_name)
            .setClosed(community.is_closed)
            .setBlacklisted(community.blacklisted)
            .setVerified(community.verified)
            .setAdmin(community.is_admin)
            .setAdminLevel(community.admin_level)
            .setMember(community.is_member)
            .setMemberStatus(community.member_status)
            .setType(community.type)
            .setPhoto50(community.photo_50)
            .setPhoto100(community.photo_100)
            .setPhoto200(community.photo_200)
            .setMembersCount(community.members_count)
            .setHasUnseenStories(community.has_unseen_stories)
    }


    fun mapFavePage(favePage: FavePageResponse): FavePageEntity {
        var id = 0L
        favePage.user.requireNonNull {
            id = it.id
        }
        favePage.group.requireNonNull {
            id = -it.id
        }
        return FavePageEntity(id)
            .setDescription(favePage.description)
            .setUpdateDate(favePage.updated_date)
            .setFaveType(favePage.type)
            .setGroup(favePage.group?.let { mapCommunity(it) })
            .setUser(favePage.user?.let { mapUser(it) })
    }


    fun mapUser(user: VKApiUser): UserEntity {
        return UserEntity(user.id)
            .setFirstName(user.first_name)
            .setLastName(user.last_name)
            .setOnline(user.online)
            .setOnlineMobile(user.online_mobile)
            .setOnlineApp(user.online_app)
            .setPhoto50(user.photo_50)
            .setPhoto100(user.photo_100)
            .setPhoto200(user.photo_200)
            .setPhotoMax(user.photo_max_orig)
            .setLastSeen(user.last_seen)
            .setPlatform(user.platform)
            .setStatus(user.status)
            .setSex(user.sex)
            .setDomain(user.domain)
            .setFriend(user.is_friend)
            .setFriendStatus(user.friend_status)
            .setCanWritePrivateMessage(user.can_write_private_message)
            .setBlacklisted_by_me(user.blacklisted_by_me)
            .setBlacklisted(user.blacklisted)
            .setCan_access_closed(user.can_access_closed)
            .setVerified(user.verified)
            .setMaiden_name(user.maiden_name)
            .setBdate(user.bdate)
            .setHasUnseenStories(user.has_unseen_stories)
    }


    fun mapCommunityDetails(dto: VKApiCommunity): CommunityDetailsEntity {
        val details = CommunityDetailsEntity()
            .setCanMessage(dto.can_message)
            .setStatus(dto.status)
            .setStatusAudio(dto.status_audio?.let { mapAudio(it) })
            .setFavorite(dto.is_favorite)
            .setSubscribed(dto.is_subscribed)
        dto.menu?.nonNullNoEmpty {
            val o = ArrayList<CommunityDetailsEntity.Menu>(it.size)
            for (i in it) {
                o.add(CommunityDetailsEntity.Menu().set(i.id, i.url, i.title, i.type, i.cover))
            }
            details.setMenu(o)
        }
        dto.counters.requireNonNull {
            details.setAllWallCount(it.all_wall)
                .setOwnerWallCount(it.owner_wall)
                .setPostponedWallCount(it.postponed_wall)
                .setSuggestedWallCount(it.suggest_wall)
                .setTopicsCount(it.topics)
                .setDocsCount(it.docs)
                .setDonutWallCount(it.donuts)
                .setPhotosCount(it.photos)
                .setAudiosCount(it.audios)
                .setVideosCount(it.videos)
                .setProductsCount(it.market)
                .setProductServicesCount(it.market_services)
                .setNarrativesCount(it.narratives)
                .setArticlesCount(it.articles).setChatsCount(it.chats)
        }
        details.setCover(dto.cover.requireNonNull({
            val cover = CommunityDetailsEntity.Cover()
                .setEnabled(it.enabled)
                .setImages(ArrayList(safeCountOf(it.images)))
            it.images.requireNonNull { pit ->
                for (imageDto in pit) {
                    cover.images?.add(
                        CommunityDetailsEntity.CoverImage()
                            .set(imageDto.url, imageDto.height, imageDto.width)
                    )
                }
            }
            cover
        }, {
            CommunityDetailsEntity.Cover().setEnabled(false)
        }))
        details.setDescription(dto.description)
        return details
    }


    fun mapUserDetails(user: VKApiUser): UserDetailsEntity {
        val dbo = UserDetailsEntity()
        try {
            user.photo_id.nonNullNoEmpty {
                val dividerIndex = it.indexOf('_')
                if (dividerIndex != -1) {
                    val photoId = it.substring(dividerIndex + 1).toInt()
                    dbo.setPhotoId(IdPairEntity().set(photoId, user.id))
                }
            }
        } catch (ignored: Exception) {
        }
        dbo.setStatusAudio(user.status_audio?.let { mapAudio(it) })
        dbo.setClosed(user.is_closed)
        dbo.setCity(user.city?.let { mapCity(it) })
        dbo.setCountry(user.country?.let { mapCountry(it) })
        dbo.setHomeTown(user.home_town)
        dbo.setPhone(user.mobile_phone)
        dbo.setHomePhone(user.home_phone)
        dbo.setSkype(user.skype)
        dbo.setInstagram(user.instagram)
        dbo.setFacebook(user.facebook)
        dbo.setTwitter(user.twitter)
        val counters = user.counters
        if (counters != null) {
            dbo.setFriendsCount(counters.friends)
                .setOnlineFriendsCount(counters.online_friends)
                .setMutualFriendsCount(counters.mutual_friends)
                .setFollowersCount(counters.followers)
                .setGroupsCount(counters.groups.coerceAtLeast(counters.pages + counters.subscriptions))
                .setPhotosCount(counters.photos)
                .setAudiosCount(counters.audios)
                .setVideosCount(counters.videos)
                .setArticlesCount(counters.articles)
                .setProductsCount(counters.market)
                .setProductServicesCount(counters.market_services)
                .setNarrativesCount(counters.narratives)
                .setGiftCount(counters.gifts)
                .setAllWallCount(counters.all_wall)
                .setOwnWallCount(counters.owner_wall).setPostponedWallCount(counters.postponed_wall)
        }
        dbo.setMilitaries(mapAll(
            user.militaries
        ) {
            mapMilitary(
                it
            )
        })
        dbo.setCareers(mapAll(
            user.careers
        ) {
            mapCareer(it)
        })
        dbo.setUniversities(mapAll(
            user.universities
        ) {
            mapUniversity(
                it
            )
        })
        dbo.setSchools(mapAll(
            user.schools
        ) {
            mapSchool(it)
        })
        dbo.setRelatives(mapAll(
            user.relatives
        ) {
            mapUserRelative(
                it
            )
        })
        dbo.setRelation(user.relation)
        dbo.setRelationPartnerId(user.relation_partner?.id.orZero())
        dbo.setLanguages(user.langs)
        dbo.setPolitical(user.political)
        dbo.setPeopleMain(user.people_main)
        dbo.setLifeMain(user.life_main)
        dbo.setSmoking(user.smoking)
        dbo.setAlcohol(user.alcohol)
        dbo.setInspiredBy(user.inspired_by)
        dbo.setReligion(user.religion)
        dbo.setSite(user.site)
        dbo.setInterests(user.interests)
        dbo.setMusic(user.music)
        dbo.setActivities(user.activities)
        dbo.setMovies(user.movies)
        dbo.setTv(user.tv)
        dbo.setGames(user.games)
        dbo.setQuotes(user.quotes)
        dbo.setAbout(user.about)
        dbo.setBooks(user.books)
        dbo.setFavorite(user.is_favorite)
        dbo.setSubscribed(user.is_subscribed)
        dbo.setCover(user.cover.requireNonNull({
            val cover = UserDetailsEntity.Cover()
                .setEnabled(it.enabled)
                .setImages(ArrayList(safeCountOf(it.images)))
            it.images.requireNonNull { pit ->
                for (imageDto in pit) {
                    cover.images?.add(
                        UserDetailsEntity.CoverImage()
                            .set(imageDto.url, imageDto.height, imageDto.width)
                    )
                }
            }
            cover
        }, {
            UserDetailsEntity.Cover().setEnabled(false)
        }))
        return dbo
    }

    private fun mapUserRelative(relative: VKApiUser.Relative): RelativeEntity {
        return RelativeEntity()
            .setId(relative.id)
            .setType(relative.type)
            .setName(relative.name)
    }

    private fun mapSchool(dto: VKApiSchool): SchoolEntity {
        return SchoolEntity()
            .setCityId(dto.city_id)
            .setClazz(dto.clazz)
            .setCountryId(dto.country_id)
            .setFrom(dto.year_from)
            .setTo(dto.year_to)
            .setYearGraduated(dto.year_graduated)
            .setId(dto.id)
            .setName(dto.name)
    }

    private fun mapUniversity(dto: VKApiUniversity): UniversityEntity {
        return UniversityEntity()
            .setId(dto.id)
            .setCityId(dto.city_id)
            .setCountryId(dto.country_id)
            .setName(dto.name)
            .setStatus(dto.education_status)
            .setForm(dto.education_form)
            .setFacultyId(dto.faculty)
            .setFacultyName(dto.faculty_name)
            .setChairId(dto.chair)
            .setChairName(dto.chair_name)
            .setGraduationYear(dto.graduation)
    }

    private fun mapMilitary(dto: VKApiMilitary): MilitaryEntity {
        return MilitaryEntity()
            .setCountryId(dto.country_id)
            .setFrom(dto.from)
            .setUnit(dto.unit)
            .setUnitId(dto.unit_id)
            .setUntil(dto.until)
    }

    private fun mapCareer(dto: VKApiCareer): CareerEntity {
        return CareerEntity()
            .setCityId(dto.city_id)
            .setCompany(dto.company)
            .setCountryId(dto.country_id)
            .setFrom(dto.from)
            .setUntil(dto.until)
            .setPosition(dto.position)
            .setGroupId(dto.group_id)
    }

    private fun mapCountry(dto: VKApiCountry): CountryDboEntity {
        return CountryDboEntity().set(dto.id, dto.title)
    }

    private fun mapCity(dto: VKApiCity): CityEntity {
        return CityEntity()
            .setArea(dto.area)
            .setId(dto.id)
            .setImportant(dto.important)
            .setTitle(dto.title)
            .setRegion(dto.region)
    }


    fun mapNews(news: VKApiNews): NewsDboEntity {
        val entity = NewsDboEntity()
            .setType(news.type)
            .setSourceId(news.source_id)
            .setDate(news.date)
            .setPostId(news.post_id)
            .setPostType(news.post_type)
            .setFinalPost(news.final_post)
            .setCopyOwnerId(news.copy_owner_id)
            .setCopyPostId(news.copy_post_id)
            .setCopyPostDate(news.copy_post_date)
            .setText(news.text)
            .setCanEdit(news.can_edit)
            .setCanDelete(news.can_delete)
            .setCommentCount(news.comment_count)
            .setCanPostComment(news.comment_can_post)
            .setLikesCount(news.like_count)
            .setUserLikes(news.user_like)
            .setCanLike(news.can_like)
            .setCanPublish(news.can_publish)
            .setRepostCount(news.reposts_count)
            .setUserReposted(news.user_reposted)
            .setFriendsTags(news.friends)
            .setViews(news.views)
            .setDonut(news.is_donut)
            .setCopyright(news.copyright?.let {
                NewsDboEntity.CopyrightDboEntity(
                    it.name,
                    it.link
                )
            })
        if (news.hasAttachments()) {
            entity.setAttachments(news.attachments?.let { mapAttachmentsList(it) })
        } else {
            entity.setAttachments(null)
        }
        entity.setCopyHistory(mapAll(
            news.copy_history
        ) {
            mapPost(it)
        })
        return entity
    }


    fun mapComment(
        sourceId: Int,
        sourceOwnerId: Long,
        sourceType: Int,
        sourceAccessKey: String?,
        comment: VKApiComment
    ): CommentEntity {
        var attachmentsEntities: List<DboEntity>? = null
        comment.attachments.requireNonNull {
            attachmentsEntities = mapAttachmentsList(it)
        }
        return CommentEntity().set(sourceId, sourceOwnerId, sourceType, sourceAccessKey, comment.id)
            .setFromId(comment.from_id)
            .setDate(comment.date)
            .setText(comment.text)
            .setReplyToUserId(comment.reply_to_user)
            .setReplyToComment(comment.reply_to_comment)
            .setLikesCount(comment.likes)
            .setUserLikes(comment.user_likes)
            .setCanLike(comment.can_like)
            .setCanEdit(comment.can_edit)
            .setDeleted(false)
            .setAttachmentsCount(comment.attachmentsCount)
            .setAttachments(attachmentsEntities)
            .setThreadsCount(comment.threads_count)
            .setThreads(
                mapComments(
                    sourceId,
                    sourceOwnerId,
                    sourceType,
                    sourceAccessKey,
                    comment.threads
                )
            )
            .setPid(comment.pid)
    }

    private fun mapComments(
        sourceId: Int,
        sourceOwnerId: Long,
        sourceType: Int,
        sourceAccessKey: String?,
        comments: List<VKApiComment>?
    ): List<CommentEntity>? {
        if (comments.isNullOrEmpty()) {
            return null
        }
        val o: MutableList<CommentEntity> = ArrayList(comments.size)
        for (i in comments) {
            o.add(mapComment(sourceId, sourceOwnerId, sourceType, sourceAccessKey, i))
        }
        return o
    }


    fun mapConversation(
        dto: VKApiConversation,
        contacts: List<VKApiConversation.ContactElement>?
    ): SimpleDialogEntity? {
        val entity = SimpleDialogEntity(dto.peer?.id ?: return null)
            .setInRead(dto.inRead)
            .setOutRead(dto.outRead)
            .setUnreadCount(dto.unreadCount)
            .setLastMessageId(dto.lastMessageId)
            .setAcl(calculateConversationAcl(dto))
        dto.settings.requireNonNull {
            entity.setTitle(it.title)
            it.pinnedMesage.requireNonNull { pit ->
                entity.setPinned(mapMessage(pit))
            }
            it.photo.requireNonNull { pit ->
                entity.setPhoto50(pit.photo50)
                    .setPhoto100(pit.photo100).setPhoto200(pit.photo200)
            }
        }
        if ("contact" == dto.peer?.type && contacts.nonNullNoEmpty()) {
            for (i in contacts) {
                if (i.id == dto.peer?.local_id) {
                    entity.setTitle(i.name)
                    entity.setPhoto50(i.photo_50)
                        .setPhoto100(i.photo_100).setPhoto200(i.photo_200)
                    break
                }
            }
        }
        dto.sort_id.requireNonNull {
            entity.setMajor_id(it.major_id)
            entity.setMinor_id(it.minor_id)
        }
        entity.setCurrentKeyboard(mapKeyboard(dto.current_keyboard))
        return entity
    }


    fun mapDialog(
        dto: VKApiDialog,
        contacts: List<VKApiConversation.ContactElement>?
    ): DialogDboEntity? {
        val messageEntity = dto.lastMessage?.let { mapMessage(it) }
        val entity = DialogDboEntity(messageEntity?.peerId ?: return null)
            .setLastMessageId(messageEntity.id)
            .setMessage(messageEntity)
            .setInRead(dto.conversation?.inRead.orZero())
            .setOutRead(dto.conversation?.outRead.orZero())
            .setUnreadCount(dto.conversation?.unreadCount.orZero())
            .setAcl(calculateConversationAcl(dto.conversation))
        dto.conversation?.settings.requireNonNull {
            entity.setTitle(it.title)
            entity.setGroupChannel(it.is_group_channel)
            it.pinnedMesage.requireNonNull { pit ->
                entity.setPinned(mapMessage(pit))
            }
            dto.conversation?.settings?.photo.requireNonNull { pit ->
                entity.setPhoto50(pit.photo50)
                    .setPhoto100(pit.photo100).setPhoto200(pit.photo200)
            }
        }
        if ("contact" == dto.conversation?.peer?.type && contacts.nonNullNoEmpty()) {
            for (i in contacts) {
                if (i.id == dto.conversation?.peer?.local_id) {
                    entity.setTitle(i.name)
                    entity.setPhoto50(i.photo_50)
                        .setPhoto100(i.photo_100).setPhoto200(i.photo_200)
                    break
                }
            }
        }
        dto.conversation?.sort_id.requireNonNull {
            entity.setMajor_id(it.major_id)
            entity.setMinor_id(it.minor_id)
        }
        entity.setCurrentKeyboard(mapKeyboard(dto.conversation?.current_keyboard))
        return entity
    }

    private fun mapKeyboard(keyboard: CurrentKeyboard?): KeyboardEntity? {
        if (keyboard == null || keyboard.buttons.isNullOrEmpty()) {
            return null
        }
        val buttons: MutableList<List<ButtonEntity>> = ArrayList()
        for (i in keyboard.buttons.orEmpty()) {
            val v: MutableList<ButtonEntity> = ArrayList()
            for (s in i) {
                if (s.action == null || "text" != s.action?.type && "open_link" != s.action?.type) {
                    continue
                }
                s.action.requireNonNull {
                    v.add(
                        ButtonEntity().setType(it.type).setColor(s.color)
                            .setLabel(it.label)
                            .setLink(it.link).setPayload(it.payload)
                    )
                }
            }
            if (v.isNotEmpty()) {
                buttons.add(v)
            }
        }
        buttons.nonNullNoEmpty {
            return KeyboardEntity().setAuthor_id(keyboard.author_id)
                .setInline(keyboard.inline)
                .setOne_time(keyboard.one_time)
                .setButtons(buttons)
        }
        return null
    }

    private fun mapAttachmentsList(attachments: VKApiAttachments): List<DboEntity>? {
        val entries = attachments.entryList()
        if (entries.isEmpty()) {
            return null
        }
        if (entries.size == 1) {
            return listOf(mapAttachment(entries[0].attachment))
        }
        val entities: MutableList<DboEntity> = ArrayList(entries.size)
        for (entry in entries) {
            entities.add(mapAttachment(entry.attachment))
        }
        return entities
    }

    private fun mapAttachment(dto: VKApiAttachment): DboEntity {
        when (dto.getType()) {
            VKApiAttachment.TYPE_PHOTO -> {
                return mapPhoto(dto as VKApiPhoto)
            }

            VKApiAttachment.TYPE_VIDEO -> {
                return mapVideo(dto as VKApiVideo)
            }

            VKApiAttachment.TYPE_DOC -> {
                return mapDoc(dto as VKApiDoc)
            }

            VKApiAttachment.TYPE_LINK -> {
                return mapLink(dto as VKApiLink)
            }

            VKApiAttachment.TYPE_ARTICLE -> {
                return mapArticle(dto as VKApiArticle)
            }

            VKApiAttachment.TYPE_AUDIO_PLAYLIST -> {
                return mapAudioPlaylist(dto as VKApiAudioPlaylist)
            }

            VKApiAttachment.TYPE_STORY -> {
                return mapStory(dto as VKApiStory)
            }

            VKApiAttachment.TYPE_GRAFFITI -> {
                return mapGraffiti(dto as VKApiGraffiti)
            }

            VKApiAttachment.TYPE_ALBUM -> {
                return buildPhotoAlbumDbo(dto as VKApiPhotoAlbum)
            }

            VKApiAttachment.TYPE_CALL -> {
                return mapCall(dto as VKApiCall)
            }

            VKApiAttachment.TYPE_GEO -> {
                return mapGeo(dto as VKApiGeo)
            }

            VKApiAttachment.TYPE_WALL_REPLY -> {
                return mapWallReply(dto as VKApiWallReply)
            }

            VKApiAttachment.TYPE_NOT_SUPPORT -> {
                return mapNotSupported(dto as VKApiNotSupported)
            }

            VKApiAttachment.TYPE_EVENT -> {
                return mapEvent(dto as VKApiEvent)
            }

            VKApiAttachment.TYPE_MARKET -> {
                return mapMarket(dto as VKApiMarket)
            }

            VKApiAttachment.TYPE_MARKET_ALBUM -> {
                return mapMarketAlbum(dto as VKApiMarketAlbum)
            }

            VKApiAttachment.TYPE_ARTIST -> {
                return mapAudioArtist(dto as VKApiAudioArtist)
            }

            VKApiAttachment.TYPE_WIKI_PAGE -> {
                return mapWikiPage(dto as VKApiWikiPage)
            }

            VKApiAttachment.TYPE_STICKER -> {
                return mapSticker(dto as VKApiSticker)
            }

            VKApiAttachment.TYPE_POST -> {
                return mapPost(dto as VKApiPost)
            }

            VKApiAttachment.TYPE_POLL -> {
                return buildPollEntity(dto as VKApiPoll)
            }

            VKApiAttachment.TYPE_AUDIO -> {
                return mapAudio(dto as VKApiAudio)
            }

            VKApiAttachment.TYPE_AUDIO_MESSAGE -> {
                return mapAudioMessage(dto as VKApiAudioMessage)
            }

            VKApiAttachment.TYPE_GIFT -> {
                return mapGiftItem(dto as VKApiGiftItem)
            }

            else -> throw UnsupportedOperationException("Unsupported attachment, class: " + dto.javaClass)
        }
    }

    private fun mapAudio(dto: VKApiAudio): AudioDboEntity {
        return AudioDboEntity().set(dto.id, dto.owner_id)
            .setArtist(dto.artist)
            .setTitle(dto.title)
            .setDuration(dto.duration)
            .setUrl(dto.url)
            .setLyricsId(dto.lyrics_id)
            .setDate(dto.date)
            .setAlbumId(dto.album_id)
            .setAlbum_owner_id(dto.album_owner_id)
            .setAlbum_access_key(dto.album_access_key)
            .setGenre(dto.genre_id)
            .setAccessKey(dto.access_key)
            .setAlbum_title(dto.album_title)
            .setThumb_image_big(dto.thumb_image_big)
            .setThumb_image_little(dto.thumb_image_little)
            .setThumb_image_very_big(dto.thumb_image_very_big)
            .setIsHq(dto.isHq)
            .setMain_artists(dto.main_artists)
    }

    private fun mapPollAnswer(dto: VKApiPoll.Answer): PollDboEntity.Answer {
        return PollDboEntity.Answer().set(dto.id, dto.text, dto.votes, dto.rate)
    }

    private fun buildPollPhoto(photo: VKApiPoll.Photo?): String? {
        var url: String? = null
        photo?.images.nonNullNoEmpty {
            var def = 0
            for (i in it) {
                if (i.width * i.height > def) {
                    def = i.width * i.height
                    url = i.url
                }
            }
        }
        return url
    }

    private fun buildPollEntity(dto: VKApiPoll): PollDboEntity {
        return PollDboEntity().set(dto.id, dto.owner_id)
            .setAnonymous(dto.anonymous)
            .setAnswers(
                mapAll(
                    dto.answers
                ) {
                    mapPollAnswer(it)
                }
            )
            .setBoard(dto.is_board)
            .setCreationTime(dto.created)
            .setMyAnswerIds(dto.answer_ids)
            .setVoteCount(dto.votes)
            .setQuestion(dto.question)
            .setClosed(dto.closed)
            .setAuthorId(dto.author_id)
            .setCanVote(dto.can_vote)
            .setCanEdit(dto.can_edit)
            .setCanReport(dto.can_report)
            .setCanShare(dto.can_share)
            .setEndDate(dto.end_date)
            .setMultiple(dto.multiple)
            .setPhoto(buildPollPhoto(dto.photo))
    }


    fun mapPost(dto: VKApiPost): PostDboEntity {
        val dbo = PostDboEntity().set(dto.id, dto.owner_id)
            .setFromId(dto.from_id)
            .setDate(dto.date)
            .setText(dto.text)
            .setReplyOwnerId(dto.reply_owner_id)
            .setReplyPostId(dto.reply_post_id)
            .setFriendsOnly(dto.friends_only)
            .setCommentsCount(dto.comments?.count.orZero())
            .setCanPostComment(dto.comments?.canPost == true)
            .setLikesCount(dto.likes_count)
            .setUserLikes(dto.user_likes)
            .setCanLike(dto.can_like)
            .setCanEdit(dto.can_edit)
            .setFavorite(dto.is_favorite)
            .setCanPublish(dto.can_publish)
            .setRepostCount(dto.reposts_count)
            .setUserReposted(dto.user_reposted)
            .setPostType(dto.post_type)
            .setAttachmentsCount(dto.attachmentsCount)
            .setSignedId(dto.signer_id)
            .setCreatedBy(dto.created_by)
            .setCanPin(dto.can_pin)
            .setPinned(dto.is_pinned)
            .setIsDonut(dto.is_donut)
            .setDeleted(false) // cant be deleted
            .setViews(dto.views)
            .setCopyright(dto.copyright?.let { PostDboEntity.CopyrightDboEntity(it.name, it.link) })
        val source = dto.post_source
        if (source != null) {
            dbo.setSource(SourceDbo().set(source.type, source.platform, source.data, source.url))
        }
        if (dto.hasAttachments()) {
            dbo.setAttachments(dto.attachments?.let { mapAttachmentsList(it) })
        } else {
            dbo.setAttachments(null)
        }
        if (dto.hasCopyHistory()) {
            dbo.setCopyHierarchy(mapAll(
                dto.copy_history
            ) {
                mapPost(it)
            })
        } else {
            dbo.setCopyHierarchy(null)
        }
        return dbo
    }


    fun mapSticker(sticker: VKApiSticker): StickerDboEntity {
        return StickerDboEntity().setId(sticker.sticker_id)
            .setImages(
                mapAll(
                    sticker.images
                ) {
                    mapStickerImage(it)
                }
            )
            .setImagesWithBackground(
                mapAll(
                    sticker.images_with_background
                ) {
                    mapStickerImage(it)
                }
            )
            .setAnimations(
                mapAll(
                    sticker.animations
                ) {
                    mapStickerAnimation(it)
                }
            )
            .setAnimationUrl(sticker.animation_url)
    }


    fun mapStickerSet(dto: Product): StickerSetEntity {
        return StickerSetEntity(dto.id)
            .setTitle(dto.title)
            .setPromoted(dto.promoted)
            .setActive(dto.active)
            .setPurchased(dto.purchased)
            .setIcon(
                mapAll(
                    dto.icon
                ) {
                    map(it)
                }
            )
            .setStickers(
                mapAll(
                    dto.stickers
                ) {
                    mapSticker(it)
                }
            )
    }

    private fun mapStickerImage(dto: VKApiSticker.Image): StickerDboEntity.Img {
        return StickerDboEntity.Img().set(dto.url, dto.width, dto.height)
    }

    private fun mapArtistImage(dto: VKApiAudioArtist.Image): AudioArtistImageEntity {
        return AudioArtistImageEntity().set(dto.url, dto.width, dto.height)
    }

    fun map(dto: VKApiStickerSet.Image): StickerSetEntity.Img {
        return StickerSetEntity.Img().set(dto.url, dto.width, dto.height)
    }

    private fun mapStickerAnimation(dto: VKApiAnimation): AnimationEntity {
        return AnimationEntity().set(dto.url, dto.type)
    }

    private fun mapWikiPage(dto: VKApiWikiPage): PageDboEntity {
        return PageDboEntity().set(dto.id, dto.owner_id)
            .setCreatorId(dto.creator_id)
            .setTitle(dto.title)
            .setSource(dto.source)
            .setEditionTime(dto.edited)
            .setCreationTime(dto.created)
            .setParent(dto.parent)
            .setParent2(dto.parent2)
            .setViews(dto.views)
            .setViewUrl(dto.view_url)
    }

    private fun mapLink(link: VKApiLink): LinkDboEntity {
        return LinkDboEntity().setUrl(link.url)
            .setCaption(link.caption)
            .setDescription(link.description)
            .setTitle(link.title)
            .setPreviewPhoto(link.preview_photo)
            .setPhoto(link.photo?.let { mapPhoto(it) })
    }


    fun mapArticle(article: VKApiArticle): ArticleDboEntity {
        return ArticleDboEntity().set(article.id, article.owner_id)
            .setAccessKey(article.access_key)
            .setOwnerName(article.owner_name)
            .setPhoto(article.photo?.let { mapPhoto(it) })
            .setTitle(article.title)
            .setSubTitle(article.subtitle)
            .setURL(article.url)
            .setIsFavorite(article.is_favorite)
    }

    private fun mapStory(dto: VKApiStory): StoryDboEntity {
        return StoryDboEntity().setId(dto.id)
            .setOwnerId(dto.owner_id)
            .setDate(dto.date)
            .setExpires(dto.expires_at)
            .setIs_expired(dto.is_expired)
            .setAccessKey(dto.access_key)
            .setTarget_url(dto.target_url)
            .setPhoto(dto.photo?.let { mapPhoto(it) })
            .setVideo(dto.video?.let { mapVideo(it) })
    }

    private fun mapWallReply(dto: VKApiWallReply): WallReplyDboEntity {
        val attachmentsEntities: List<DboEntity>? = dto.attachments?.let { mapAttachmentsList(it) }
        return WallReplyDboEntity().setId(dto.id)
            .setOwnerId(dto.owner_id)
            .setAttachments(attachmentsEntities)
            .setFromId(dto.from_id)
            .setPostId(dto.post_id)
            .setText(dto.text)
    }

    private fun mapGraffiti(dto: VKApiGraffiti): GraffitiDboEntity {
        return GraffitiDboEntity().setId(dto.id)
            .setOwner_id(dto.owner_id)
            .setAccess_key(dto.access_key)
            .setHeight(dto.height)
            .setWidth(dto.width)
            .setUrl(dto.url)
    }

    private fun mapCall(dto: VKApiCall): CallDboEntity {
        return CallDboEntity().setInitiator_id(dto.initiator_id)
            .setReceiver_id(dto.receiver_id)
            .setState(dto.state)
            .setTime(dto.time)
    }

    private fun mapGeo(dto: VKApiGeo): GeoDboEntity {
        return GeoDboEntity().setLatitude(dto.latitude)
            .setLongitude(dto.longitude)
            .setTitle(dto.title)
            .setAddress(dto.address)
            .setCountry(dto.country)
            .setId(dto.id)
    }

    private fun mapNotSupported(dto: VKApiNotSupported): NotSupportedDboEntity {
        return NotSupportedDboEntity().setType(dto.attachmentType).setBody(dto.bodyJson)
    }

    private fun mapEvent(dto: VKApiEvent): EventDboEntity {
        return EventDboEntity().setId(dto.id).setButton_text(dto.button_text).setText(dto.text)
    }


    fun mapMarket(dto: VKApiMarket): MarketDboEntity {
        return MarketDboEntity().set(dto.id, dto.owner_id)
            .setAccess_key(dto.access_key)
            .setIs_favorite(dto.is_favorite)
            .setAvailability(dto.availability)
            .setDate(dto.date)
            .setDescription(dto.description)
            .setDimensions(dto.dimensions)
            .setPrice(dto.price)
            .setSku(dto.sku)
            .setTitle(dto.title)
            .setWeight(dto.weight)
            .setPhotos(mapAll(dto.photos) { mapPhoto(it) })
            .setThumb_photo(dto.thumb_photo)
    }

    private fun mapMarketAlbum(dto: VKApiMarketAlbum): MarketAlbumDboEntity {
        return MarketAlbumDboEntity().set(dto.id, dto.owner_id)
            .setAccess_key(dto.access_key)
            .setCount(dto.count)
            .setTitle(dto.title)
            .setUpdated_time(dto.updated_time)
            .setPhoto(dto.photo?.let { mapPhoto(it) })
    }

    private fun mapAudioArtist(dto: VKApiAudioArtist): AudioArtistDboEntity {
        return AudioArtistDboEntity()
            .setId(dto.id)
            .setName(dto.name)
            .setPhoto(
                mapAll(
                    dto.photo
                ) {
                    mapArtistImage(it)
                }
            )
    }

    private fun mapAudioMessage(dto: VKApiAudioMessage): AudioMessageDboEntity {
        return AudioMessageDboEntity().set(dto.id, dto.owner_id)
            .setAccessKey(dto.access_key)
            .setDuration(dto.duration)
            .setLinkMp3(dto.linkMp3)
            .setLinkOgg(dto.linkOgg)
            .setWaveform(dto.waveform)
            .setTranscript(dto.transcript)
            .setWasListened(dto.was_listened)
    }


    fun mapDoc(dto: VKApiDoc): DocumentDboEntity {
        val dbo = DocumentDboEntity().set(dto.id, dto.ownerId)
            .setTitle(dto.title)
            .setSize(dto.size)
            .setExt(dto.ext)
            .setUrl(dto.url)
            .setDate(dto.date)
            .setType(dto.type)
            .setAccessKey(dto.accessKey)
        dto.preview.requireNonNull {
            it.photo?.sizes?.requireNonNull { pit ->
                dbo.setPhoto(mapPhotoSizes(pit))
            }
            dto.preview?.video.requireNonNull { pit ->
                dbo.setVideo(VideoPreviewDbo().set(pit.src, pit.width, pit.height, pit.fileSize))
            }
            dto.preview?.graffiti.requireNonNull { pit ->
                dbo.setGraffiti(GraffitiDbo().set(pit.src, pit.width, pit.height))
            }
        }
        return dbo
    }


    fun mapMessage(dto: VKApiMessage): MessageDboEntity {
        val encrypted = analizeMessageBody(dto.body) == MessageType.CRYPTED
        var randomId: Long = 0
        dto.random_id.nonNullNoEmpty {
            try {
                randomId = it.toLong()
            } catch (ignored: NumberFormatException) {
            }
        }
        val entity = MessageDboEntity().set(dto.id, dto.peer_id, dto.from_id)
            .setDate(dto.date)
            .setOut(dto.out)
            .setBody(dto.body)
            .setEncrypted(encrypted)
            .setImportant(dto.important)
            .setKeyboard(mapKeyboard(dto.keyboard))
            .setDeleted(dto.deleted)
            .setDeletedForAll(false) // cant be deleted for all?
            .setForwardCount(safeCountOf(dto.fwd_messages))
            .setHasAttachments(dto.attachments != null && dto.attachments?.nonEmpty() == true)
            .setStatus(MessageStatus.SENT) // only sent can be
            .setOriginalId(dto.id)
            .setAction(Message.fromApiChatAction(dto.action))
            .setActionMemberId(dto.action_mid)
            .setActionEmail(dto.action_email)
            .setActionText(dto.action_text)
            .setPhoto50(dto.action_photo_50)
            .setPhoto100(dto.action_photo_100)
            .setPhoto200(dto.action_photo_200)
            .setRandomId(randomId)
            .setUpdateTime(dto.update_time)
            .setPayload(dto.payload)
        if (entity.isHasAttachments) {
            entity.setAttachments(dto.attachments?.let { mapAttachmentsList(it) })
        } else {
            entity.setAttachments(null)
        }
        entity.setForwardMessages(dto.fwd_messages.transformNonNullNullable({
            if (it.size == 1) {
                listOf(mapMessage(it[0]))
            } else {
                val fwds: MutableList<MessageDboEntity> = ArrayList(it.size)
                for (f in it) {
                    fwds.add(mapMessage(f))
                }
                fwds
            }
        }, {
            null
        }))
        return entity
    }


    fun mapVideo(dto: VKApiVideo): VideoDboEntity {
        return VideoDboEntity().set(dto.id, dto.owner_id)
            .setAlbumId(dto.album_id)
            .setTitle(dto.title)
            .setDescription(dto.description)
            .setDuration(dto.duration)
            .setLink(dto.link)
            .setDate(dto.date)
            .setAddingDate(dto.adding_date)
            .setViews(dto.views)
            .setPlayer(dto.player)
            .setImage(dto.image)
            .setAccessKey(dto.access_key)
            .setCommentsCount(dto.comments?.count.orZero())
            .setCanComment(dto.can_comment)
            .setCanRepost(dto.can_repost)
            .setUserLikes(dto.user_likes)
            .setRepeat(dto.repeat)
            .setLikesCount(dto.likes)
            .setPrivacyView(dto.privacy_view?.let { mapPrivacy(it) })
            .setPrivacyComment(dto.privacy_comment?.let { mapPrivacy(it) })
            .setMp4link240(dto.mp4_240)
            .setMp4link360(dto.mp4_360)
            .setMp4link480(dto.mp4_480)
            .setMp4link720(dto.mp4_720)
            .setMp4link1080(dto.mp4_1080)
            .setMp4link1440(dto.mp4_1440)
            .setMp4link2160(dto.mp4_2160)
            .setExternalLink(dto.external)
            .setHls(dto.hls)
            .setLive(dto.live)
            .setPlatform(dto.platform)
            .setCanEdit(dto.can_edit)
            .setCanAdd(dto.can_add)
            .setPrivate(dto.is_private)
            .setFavorite(dto.is_favorite)
    }

    private fun mapPrivacy(dto: VKApiPrivacy): PrivacyEntity {
        return PrivacyEntity().set(
            dto.category,
            mapAll(
                dto.entries
            ) {
                PrivacyEntity.Entry().set(it.type, it.id, it.allowed)
            }
        )
    }

    private fun mapAudioPlaylist(dto: VKApiAudioPlaylist): AudioPlaylistDboEntity {
        return AudioPlaylistDboEntity()
            .setId(dto.id)
            .setOwnerId(dto.owner_id)
            .setAccess_key(dto.access_key)
            .setArtist_name(dto.artist_name)
            .setCount(dto.count)
            .setDescription(dto.description)
            .setGenre(dto.genre)
            .setYear(dto.Year)
            .setTitle(dto.title)
            .setThumb_image(dto.thumb_image)
            .setUpdate_time(dto.update_time)
            .setOriginal_access_key(dto.original_access_key)
            .setOriginal_id(dto.original_id)
            .setOriginal_owner_id(dto.original_owner_id)
    }

    private fun mapGiftItem(dto: VKApiGiftItem): GiftItemDboEntity {
        return GiftItemDboEntity().setId(dto.id)
            .setThumb48(dto.thumb_48)
            .setThumb96(dto.thumb_96)
            .setThumb256(dto.thumb_256)
    }


    fun mapPhoto(dto: VKApiPhoto): PhotoDboEntity {
        return PhotoDboEntity().set(dto.id, dto.owner_id)
            .setAlbumId(dto.album_id)
            .setWidth(dto.width)
            .setHeight(dto.height)
            .setText(dto.text)
            .setDate(dto.date)
            .setUserLikes(dto.user_likes)
            .setCanComment(dto.can_comment)
            .setLikesCount(dto.likes)
            .setRepostsCount(dto.reposts)
            .setCommentsCount(dto.comments?.count.orZero())
            .setTagsCount(dto.tags)
            .setAccessKey(dto.access_key)
            .setPostId(dto.post_id)
            .setDeleted(false) //cant bee deleted
            .setSizes(mapPhotoSizes(dto.sizes))
    }

    private fun mapPhotoSize(dto: PhotoSizeDto): PhotoSizeEntity.Size {
        return PhotoSizeEntity.Size()
            .setH(dto.height)
            .setW(dto.width)
            .setUrl(firstNonEmptyString(dto.url, dto.src))
    }

    private fun mapPhotoSizes(dtos: List<PhotoSizeDto>?): PhotoSizeEntity {
        val sizes = PhotoSizeEntity()
        if (dtos != null) {
            for (dto in dtos) {
                when (dto.type) {
                    PhotoSizeDto.Type.S -> sizes.setS(mapPhotoSize(dto))
                    PhotoSizeDto.Type.M -> sizes.setM(mapPhotoSize(dto))
                    PhotoSizeDto.Type.X -> sizes.setX(mapPhotoSize(dto))
                    PhotoSizeDto.Type.Y -> sizes.setY(mapPhotoSize(dto))
                    PhotoSizeDto.Type.Z -> sizes.setZ(mapPhotoSize(dto))
                    PhotoSizeDto.Type.W -> sizes.setW(mapPhotoSize(dto))
                    PhotoSizeDto.Type.O -> sizes.setO(mapPhotoSize(dto))
                    PhotoSizeDto.Type.P -> sizes.setP(mapPhotoSize(dto))
                    PhotoSizeDto.Type.Q -> sizes.setQ(mapPhotoSize(dto))
                    PhotoSizeDto.Type.R -> sizes.setR(mapPhotoSize(dto))
                }
            }
        }
        return sizes
    }

    private class CEntity(
        val id: Int,
        val ownerId: Long,
        val type: Int,
        val accessKey: String?,
        val dboEntity: DboEntity
    )
}
