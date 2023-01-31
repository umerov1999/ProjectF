package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.db.model.entity.*
import dev.ragnarok.fenrir.db.model.entity.AudioArtistDboEntity.AudioArtistImageEntity
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity.AnimationEntity
import dev.ragnarok.fenrir.db.model.entity.UserDetailsEntity.RelativeEntity
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.fillPostOwners
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAllMutable
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.AudioArtist.AudioArtistImage
import dev.ragnarok.fenrir.model.Document.VideoPreview
import dev.ragnarok.fenrir.model.database.Country
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.VKOwnIds

object Entity2Model {
    fun buildVideoAlbumFromDbo(dbo: VideoAlbumDboEntity): VideoAlbum {
        return VideoAlbum(dbo.id, dbo.ownerId)
            .setTitle(dbo.title)
            .setCount(dbo.count)
            .setPrivacy(dbo.privacy?.let { mapSimplePrivacy(it) })
            .setImage(dbo.image)
            .setUpdatedTime(dbo.updateTime)
    }

    fun buildTopicFromDbo(dbo: TopicDboEntity, owners: IOwnersBundle): Topic {
        val topic = Topic(dbo.id, dbo.ownerId)
            .setTitle(dbo.title)
            .setCreationTime(dbo.createdTime)
            .setCreatedByOwnerId(dbo.creatorId)
            .setLastUpdateTime(dbo.lastUpdateTime)
            .setUpdatedByOwnerId(dbo.updatedBy)
            .setClosed(dbo.isClosed)
            .setFixed(dbo.isFixed)
            .setCommentsCount(dbo.commentsCount)
            .setFirstCommentBody(dbo.firstComment)
            .setLastCommentBody(dbo.lastComment)
        if (dbo.updatedBy != 0L) {
            topic.setUpdater(owners.getById(dbo.updatedBy))
        }
        if (dbo.creatorId != 0L) {
            topic.setCreator(owners.getById(dbo.creatorId))
        }
        return topic
    }


    fun buildCommunitiesFromDbos(dbos: List<CommunityEntity>): List<Community> {
        val communities: MutableList<Community> = ArrayList(dbos.size)
        for (dbo in dbos) {
            communities.add(buildCommunityFromDbo(dbo))
        }
        return communities
    }


    fun buildCommunityFromDbo(dbo: CommunityEntity): Community {
        return Community(dbo.id)
            .setName(dbo.name)
            .setScreenName(dbo.screenName)
            .setBlacklisted(dbo.isBlacklisted)
            .setClosed(dbo.closed)
            .setVerified(dbo.isVerified)
            .setAdmin(dbo.isAdmin)
            .setAdminLevel(dbo.adminLevel)
            .setMember(dbo.isMember)
            .setMemberStatus(dbo.memberStatus)
            .setCommunityType(dbo.type)
            .setPhoto50(dbo.photo50)
            .setPhoto100(dbo.photo100)
            .setPhoto200(dbo.photo200)
            .setMembersCount(dbo.membersCount)
            .setHasUnseenStories(dbo.hasUnseenStories)
    }


    fun buildCommunityDetailsFromDbo(dbo: CommunityDetailsEntity): CommunityDetails {
        val details = CommunityDetails()
            .setCanMessage(dbo.isCanMessage)
            .setFavorite(dbo.isSetFavorite)
            .setSubscribed(dbo.isSetSubscribed)
            .setStatus(dbo.status)
            .setStatusAudio(dbo.statusAudio?.let { buildAudioFromDbo(it) })
            .setAllWallCount(dbo.allWallCount)
            .setOwnerWallCount(dbo.ownerWallCount)
            .setPostponedWallCount(dbo.postponedWallCount)
            .setSuggestedWallCount(dbo.suggestedWallCount)
            .setDonutWallCount(dbo.donutWallCount)
            .setTopicsCount(dbo.topicsCount)
            .setDocsCount(dbo.docsCount)
            .setPhotosCount(dbo.photosCount)
            .setAudiosCount(dbo.audiosCount)
            .setVideosCount(dbo.videosCount)
            .setProductsCount(dbo.productsCount)
            .setProductServicesCount(dbo.productServicesCount)
            .setNarrativesCount(dbo.narrativesCount)
            .setArticlesCount(dbo.articlesCount)
            .setChatsCount(dbo.chatsCount)
        dbo.menu?.nonNullNoEmpty {
            val o = ArrayList<CommunityDetails.Menu>(it.size)
            for (i in it) {
                o.add(CommunityDetails.Menu(i.id, i.url, i.title, i.type, i.cover))
            }
            details.setMenu(o)
        }
        details.setCover(dbo.cover.requireNonNull({
            val cover = CommunityDetails.Cover()
                .setEnabled(it.isEnabled)
                .setImages(ArrayList(safeCountOf(it.images)))
            it.images.nonNullNoEmpty { pit ->
                for (imageDto in pit) {
                    cover.getImages()?.add(
                        CommunityDetails.CoverImage(
                            imageDto.url,
                            imageDto.height,
                            imageDto.width
                        )
                    )
                }
            }
            cover
        }, {
            CommunityDetails.Cover().setEnabled(false)
        }))
        details.setDescription(dbo.description)
        return details
    }

    private fun buildUserArray(users: List<Long>, owners: IOwnersBundle): List<User> {
        val data: MutableList<User> = ArrayList(safeCountOf(users))
        for (pair in users) {
            val dt = owners.getById(pair)
            if (dt.ownerType == OwnerType.USER) data.add(
                owners.getById(
                    pair
                ) as User
            )
        }
        return data
    }


    fun buildUsersFromDbo(dbos: List<UserEntity>): List<User> {
        val users: MutableList<User> = ArrayList(dbos.size)
        for (dbo in dbos) {
            map(dbo)?.let { users.add(it) }
        }
        return users
    }

    fun buildOwnerUsersFromDbo(dbos: List<UserEntity>): List<Owner> {
        val users: MutableList<Owner> = ArrayList(dbos.size)
        for (dbo in dbos) {
            map(dbo)?.let { users.add(it) }
        }
        return users
    }

    fun buildFaveUsersFromDbo(dbos: List<FavePageEntity>): List<FavePage> {
        val users: MutableList<FavePage> = ArrayList(dbos.size)
        for (dbo in dbos) {
            users.add(map(dbo))
        }
        return users
    }


    fun buildUserDetailsFromDbo(dbo: UserDetailsEntity, owners: IOwnersBundle): UserDetails {
        val details = UserDetails()
            .setPhotoId(
                dbo.photoId?.let {
                    IdPair(
                        it.id,
                        it.ownerId
                    )
                }
            )
            .setStatusAudio(dbo.statusAudio?.let { buildAudioFromDbo(it) })
            .setFriendsCount(dbo.friendsCount)
            .setOnlineFriendsCount(dbo.onlineFriendsCount)
            .setMutualFriendsCount(dbo.mutualFriendsCount)
            .setFollowersCount(dbo.followersCount)
            .setGroupsCount(dbo.groupsCount)
            .setPhotosCount(dbo.photosCount)
            .setAudiosCount(dbo.audiosCount)
            .setVideosCount(dbo.videosCount)
            .setArticlesCount(dbo.articlesCount)
            .setProductsCount(dbo.productsCount)
            .setProductServicesCount(dbo.productServicesCount)
            .setNarrativesCount(dbo.narrativesCount)
            .setGiftCount(dbo.giftCount)
            .setAllWallCount(dbo.allWallCount)
            .setOwnWallCount(dbo.ownWallCount)
            .setPostponedWallCount(dbo.postponedWallCount)
            .setClosed(dbo.isClosed)
            .setCity(dbo.city?.let { map(it) })
            .setCountry(dbo.country?.let { map(it) })
            .setHometown(dbo.homeTown)
            .setPhone(dbo.phone)
            .setHomePhone(dbo.homePhone)
            .setSkype(dbo.skype)
            .setInstagram(dbo.instagram)
            .setTwitter(dbo.twitter)
            .setFacebook(dbo.facebook)
        details.setMilitaries(mapAll(dbo.militaries, Entity2Model::map))
        details.setCareers(mapAll(dbo.careers) { orig -> map(orig, owners) })
        details.setUniversities(mapAll(dbo.universities, Entity2Model::map))
        details.setSchools(mapAll(dbo.schools, Entity2Model::map))
        details.setRelatives(mapAll(dbo.relatives) { orig -> map(orig, owners) })
        details.setRelation(dbo.relation)
        details.setRelationPartner(if (dbo.relationPartnerId != 0L) owners.getById(dbo.relationPartnerId) else null)
        details.setLanguages(dbo.languages)
        details.setPolitical(dbo.political)
        details.setPeopleMain(dbo.peopleMain)
        details.setLifeMain(dbo.lifeMain)
        details.setSmoking(dbo.smoking)
        details.setAlcohol(dbo.alcohol)
        details.setInspiredBy(dbo.inspiredBy)
        details.setReligion(dbo.religion)
        details.setSite(dbo.site)
        details.setInterests(dbo.interests)
        details.setMusic(dbo.music)
        details.setActivities(dbo.activities)
        details.setMovies(dbo.movies)
        details.setTv(dbo.tv)
        details.setGames(dbo.games)
        details.setQuotes(dbo.quotes)
        details.setAbout(dbo.about)
        details.setBooks(dbo.books)
        details.setFavorite(dbo.isSetFavorite)
        details.setSubscribed(dbo.isSetSubscribed)
        details.setCover(dbo.cover.requireNonNull({
            val cover = UserDetails.Cover()
                .setEnabled(it.isEnabled)
                .setImages(ArrayList(safeCountOf(it.images)))
            it.images.nonNullNoEmpty { pit ->
                for (imageDto in pit) {
                    cover.getImages()?.add(
                        UserDetails.CoverImage(
                            imageDto.url,
                            imageDto.height,
                            imageDto.width
                        )
                    )
                }
            }
            cover
        }, {
            UserDetails.Cover().setEnabled(false)
        }))
        return details
    }

    fun map(entity: RelativeEntity, owners: IOwnersBundle): UserDetails.Relative {
        return UserDetails.Relative()
            .setUser(if (entity.id > 0) owners.getById(entity.id) as User else null)
            .setName(entity.name)
            .setType(entity.type)
    }

    fun map(entity: SchoolEntity): School {
        return School()
            .setCityId(entity.cityId)
            .setCountryId(entity.countryId)
            .setId(entity.id)
            .setClazz(entity.clazz)
            .setName(entity.name)
            .setTo(entity.to)
            .setFrom(entity.from)
            .setYearGraduated(entity.yearGraduated)
    }

    fun map(entity: UniversityEntity): University {
        return University()
            .setName(entity.name)
            .setCityId(entity.cityId)
            .setCountryId(entity.countryId)
            .setStatus(entity.status)
            .setGraduationYear(entity.graduationYear)
            .setId(entity.id)
            .setFacultyId(entity.facultyId)
            .setFacultyName(entity.facultyName)
            .setChairId(entity.chairId)
            .setChairName(entity.chairName)
            .setForm(entity.form)
    }

    fun map(entity: MilitaryEntity): Military {
        return Military()
            .setCountryId(entity.countryId)
            .setFrom(entity.from)
            .setUnit(entity.unit)
            .setUntil(entity.until)
            .setUnitId(entity.unitId)
    }

    fun map(entity: CareerEntity, bundle: IOwnersBundle): Career {
        return Career()
            .setCityId(entity.cityId)
            .setCompany(entity.company)
            .setCountryId(entity.countryId)
            .setFrom(entity.from)
            .setUntil(entity.until)
            .setPosition(entity.position)
            .setGroup(if (entity.groupId == 0L) null else bundle.getById(-entity.groupId) as Community)
    }

    fun map(entity: CountryDboEntity): Country {
        return Country(entity.id, entity.title)
    }

    fun map(entity: CityEntity): City {
        return City(entity.id, entity.title)
            .setArea(entity.area)
            .setImportant(entity.isImportant)
            .setRegion(entity.region)
    }


    fun map(entity: UserEntity?): User? {
        return if (entity == null) {
            null
        } else User(entity.id)
            .setFirstName(entity.firstName)
            .setLastName(entity.lastName)
            .setOnline(entity.isOnline)
            .setOnlineMobile(entity.isOnlineMobile)
            .setOnlineApp(entity.onlineApp)
            .setPhoto50(entity.photo50)
            .setPhoto100(entity.photo100)
            .setPhoto200(entity.photo200)
            .setPhotoMax(entity.photoMax)
            .setLastSeen(entity.lastSeen)
            .setPlatform(entity.platform)
            .setStatus(entity.status)
            .setSex(entity.sex)
            .setDomain(entity.domain)
            .setFriend(entity.isFriend)
            .setFriendStatus(entity.friendStatus)
            .setCanWritePrivateMessage(entity.canWritePrivateMessage)
            .setBlacklisted(entity.blacklisted)
            .setBlacklisted_by_me(entity.blacklisted_by_me)
            .setVerified(entity.isVerified)
            .setCan_access_closed(entity.isCan_access_closed)
            .setMaiden_name(entity.maiden_name)
            .setBdate(entity.bdate)
            .setHasUnseenStories(entity.hasUnseenStories)
    }

    fun map(entity: FavePageEntity): FavePage {
        return FavePage(entity.id)
            .setDescription(entity.description)
            .setUpdatedDate(entity.updateDate)
            .setFaveType(entity.faveType)
            .setUser(map(entity.user))
            .setGroup(map(entity.group))
    }


    fun map(entity: CommunityEntity?): Community? {
        return if (entity == null) {
            null
        } else Community(entity.id)
            .setName(entity.name)
            .setScreenName(entity.screenName)
            .setBlacklisted(entity.isBlacklisted)
            .setPhoto50(entity.photo50)
            .setPhoto100(entity.photo100)
            .setPhoto200(entity.photo200)
            .setAdmin(entity.isAdmin)
            .setAdminLevel(entity.adminLevel)
            .setClosed(entity.closed)
            .setVerified(entity.isVerified)
            .setMember(entity.isMember)
            .setMemberStatus(entity.memberStatus)
            .setMembersCount(entity.membersCount)
            .setCommunityType(entity.type)
            .setHasUnseenStories(entity.hasUnseenStories)
    }


    fun mapPhotoAlbum(entity: PhotoAlbumDboEntity): PhotoAlbum {
        return PhotoAlbum(entity.id, entity.ownerId)
            .setSize(entity.size)
            .setTitle(entity.title)
            .setDescription(entity.description)
            .setCanUpload(entity.isCanUpload)
            .setUpdatedTime(entity.updatedTime)
            .setCreatedTime(entity.createdTime)
            .setSizes(entity.sizes?.let { buildPhotoSizesFromDbo(it) })
            .setPrivacyView(entity.privacyView?.let { mapSimplePrivacy(it) })
            .setPrivacyComment(entity.privacyComment?.let { mapSimplePrivacy(it) })
            .setUploadByAdminsOnly(entity.isUploadByAdminsOnly)
            .setCommentsDisabled(entity.isCommentsDisabled)
    }


    fun buildCommentFromDbo(dbo: CommentEntity?, owners: IOwnersBundle): Comment? {
        dbo ?: return null
        val attachments =
            if (dbo.getAttachments().isNullOrEmpty()) null else buildAttachmentsFromDbos(
                dbo.getAttachments(), owners
            )
        return Comment(
            Commented(
                dbo.sourceId,
                dbo.sourceOwnerId,
                dbo.sourceType,
                dbo.sourceAccessKey
            )
        )
            .setId(dbo.id)
            .setFromId(dbo.fromId)
            .setDate(dbo.date)
            .setText(dbo.text)
            .setReplyToUser(dbo.replyToUserId)
            .setReplyToComment(dbo.replyToComment)
            .setLikesCount(dbo.likesCount)
            .setUserLikes(dbo.isUserLikes)
            .setCanLike(dbo.isCanLike)
            .setCanEdit(dbo.isCanEdit)
            .setAttachments(attachments)
            .setAuthor(owners.getById(dbo.fromId))
            .setThreadsCount(dbo.threadsCount)
            .setThreads(buildCommentsFromDbo(dbo.threads, owners))
            .setPid(dbo.pid)
            .setDeleted(dbo.isDeleted)
    }

    private fun buildCommentsFromDbo(
        dbos: List<CommentEntity>?,
        owners: IOwnersBundle
    ): List<Comment>? {
        if (dbos.isNullOrEmpty()) {
            return null
        }
        val o: MutableList<Comment> = ArrayList()
        for (i in dbos) {
            val u = buildCommentFromDbo(i, owners)
            if (u != null) {
                o.add(u)
            }
        }
        return o
    }


    fun buildDialogFromDbo(
        accountId: Long,
        entity: DialogDboEntity,
        owners: IOwnersBundle
    ): Dialog {
        val message = entity.message?.let { message(accountId, it, owners) }
        val dialog = Dialog()
            .setLastMessageId(entity.lastMessageId)
            .setPeerId(entity.peerId)
            .setPhoto50(entity.photo50)
            .setPhoto100(entity.photo100)
            .setPhoto200(entity.photo200)
            .setTitle(entity.title)
            .setMessage(message)
            .setUnreadCount(entity.unreadCount)
            .setOutRead(entity.outRead)
            .setInRead(entity.inRead)
            .setGroupChannel(entity.isGroupChannel)
            .setMajor_id(entity.major_id)
            .setMinor_id(entity.minor_id)
        when (Peer.getType(entity.peerId)) {
            Peer.GROUP, Peer.USER -> dialog.setInterlocutor(owners.getById(dialog.peerId))
            Peer.CHAT, Peer.CONTACT -> dialog.setInterlocutor(message?.senderId?.let {
                owners.getById(
                    it
                )
            })

            else -> throw IllegalArgumentException("Invalid peer_id")
        }
        return dialog
    }


    fun buildKeyboardFromDbo(keyboard: KeyboardEntity?): Keyboard? {
        if (keyboard == null || keyboard.buttons.isNullOrEmpty()) {
            return null
        }
        val buttons: ArrayList<List<Keyboard.Button>> = ArrayList(keyboard.buttons?.size.orZero())
        for (i in keyboard.buttons.orEmpty()) {
            val vt: ArrayList<Keyboard.Button> = ArrayList(i.size)
            for (s in i) {
                vt.add(
                    Keyboard.Button().setType(s.type).setColor(s.color).setLabel(s.label)
                        .setLink(s.link).setPayload(s.payload)
                )
            }
            buttons.add(vt)
        }
        return Keyboard().setAuthor_id(
            keyboard.author_id
        ).setInline(keyboard.inline)
            .setOne_time(keyboard.one_time).setButtons(buttons)
    }


    fun message(accountId: Long, dbo: MessageDboEntity, owners: IOwnersBundle): Message {
        val message = Message(dbo.id)
            .setAccountId(accountId)
            .setBody(dbo.body)
            .setPeerId(dbo.peerId)
            .setSenderId(dbo.fromId)
            .setOut(dbo.isOut)
            .setStatus(dbo.status)
            .setDate(dbo.date)
            .setHasAttachments(dbo.isHasAttachments)
            .setForwardMessagesCount(dbo.forwardCount)
            .setDeleted(dbo.isDeleted)
            .setDeletedForAll(dbo.isDeletedForAll)
            .setOriginalId(dbo.originalId)
            .setCryptStatus(if (dbo.isEncrypted) CryptStatus.ENCRYPTED else CryptStatus.NO_ENCRYPTION)
            .setImportant(dbo.isImportant)
            .setAction(dbo.action)
            .setActionMid(dbo.actionMemberId)
            .setActionEmail(dbo.actionEmail)
            .setActionText(dbo.actionText)
            .setPhoto50(dbo.photo50)
            .setPhoto100(dbo.photo100)
            .setPhoto200(dbo.photo200)
            .setSender(owners.getById(dbo.fromId))
            .setRandomId(dbo.randomId)
            .setUpdateTime(dbo.updateTime)
            .setPayload(dbo.payload)
            .setKeyboard(buildKeyboardFromDbo(dbo.keyboard))
        if (dbo.actionMemberId != 0L) {
            message.setActionUser(owners.getById(dbo.actionMemberId))
        }
        if (dbo.getAttachments().nonNullNoEmpty()) {
            message.setAttachments(buildAttachmentsFromDbos(dbo.getAttachments(), owners))
        }
        dbo.forwardMessages.nonNullNoEmpty {
            for (fwdDbo in it) {
                message.prepareFwd(it.size)
                    .add(message(accountId, fwdDbo, owners))
            }
        }
        return message
    }

    private fun buildAttachmentsFromDbos(
        entities: List<DboEntity>?,
        owners: IOwnersBundle
    ): Attachments {
        val attachments = Attachments()
        if (entities.nonNullNoEmpty()) {
            for (entity in entities) {
                attachments.add(buildAttachmentFromDbo(entity, owners))
            }
        }
        return attachments
    }


    fun buildAttachmentFromDbo(dboEntity: DboEntity, owners: IOwnersBundle): AbsModel {
        when (dboEntity) {
            is PhotoDboEntity -> {
                return map(dboEntity)
            }

            is VideoDboEntity -> {
                return buildVideoFromDbo(dboEntity)
            }

            is PostDboEntity -> {
                return buildPostFromDbo(dboEntity, owners)
            }

            is LinkDboEntity -> {
                return buildLinkFromDbo(dboEntity)
            }

            is ArticleDboEntity -> {
                return buildArticleFromDbo(dboEntity)
            }

            is StoryDboEntity -> {
                return buildStoryFromDbo(dboEntity, owners)
            }

            is PhotoAlbumDboEntity -> {
                return mapPhotoAlbum(dboEntity)
            }

            is GraffitiDboEntity -> {
                return buildGraffitiFromDbo(dboEntity)
            }

            is AudioPlaylistDboEntity -> {
                return buildAudioPlaylistFromDbo(dboEntity)
            }

            is CallDboEntity -> {
                return buildCallFromDbo(dboEntity)
            }

            is GeoDboEntity -> {
                return buildGeoFromDbo(dboEntity)
            }

            is WallReplyDboEntity -> {
                return buildWallReplyDbo(dboEntity, owners)
            }

            is NotSupportedDboEntity -> {
                return buildNotSupportedFromDbo(dboEntity)
            }

            is EventDboEntity -> {
                return buildEventFromDbo(dboEntity, owners)
            }

            is MarketDboEntity -> {
                return buildMarketFromDbo(dboEntity)
            }

            is MarketAlbumDboEntity -> {
                return buildMarketAlbumFromDbo(dboEntity)
            }

            is AudioArtistDboEntity -> {
                return buildAudioArtistFromDbo(dboEntity)
            }

            is PollDboEntity -> {
                return buildPollFromDbo(dboEntity)
            }

            is DocumentDboEntity -> {
                return buildDocumentFromDbo(dboEntity)
            }

            is PageDboEntity -> {
                return buildWikiPageFromDbo(dboEntity)
            }

            is StickerDboEntity -> {
                return buildStickerFromDbo(dboEntity)
            }

            is AudioDboEntity -> {
                return buildAudioFromDbo(dboEntity)
            }

            is TopicDboEntity -> {
                return buildTopicFromDbo(dboEntity, owners)
            }

            is AudioMessageDboEntity -> {
                return map(dboEntity)
            }

            is GiftItemDboEntity -> {
                return buildGiftItemFromDbo(dboEntity)
            }

            else -> throw UnsupportedOperationException("Unsupported DBO class: " + dboEntity.javaClass)
        }
    }

    private fun buildAudioFromDbo(dbo: AudioDboEntity): Audio {
        return Audio()
            .setAccessKey(dbo.accessKey)
            .setAlbumId(dbo.albumId)
            .setAlbum_owner_id(dbo.album_owner_id)
            .setAlbum_access_key(dbo.album_access_key)
            .setArtist(dbo.artist)
            .setDuration(dbo.duration)
            .setUrl(dbo.url)
            .setId(dbo.id)
            .setOwnerId(dbo.ownerId)
            .setLyricsId(dbo.lyricsId)
            .setDate(dbo.date)
            .setTitle(dbo.title)
            .setGenre(dbo.genre)
            .setAlbum_title(dbo.album_title)
            .setThumb_image_big(dbo.thumb_image_big)
            .setThumb_image_little(dbo.thumb_image_little)
            .setThumb_image_very_big(dbo.thumb_image_very_big)
            .setIsHq(dbo.isHq)
            .setMain_artists(dbo.main_artists).updateDownloadIndicator()
    }

    private fun buildAudioPlaylistFromDbo(dto: AudioPlaylistDboEntity): AudioPlaylist {
        return AudioPlaylist()
            .setId(dto.id)
            .setOwnerId(dto.ownerId)
            .setAccess_key(dto.access_key)
            .setArtist_name(dto.artist_name)
            .setCount(dto.count)
            .setDescription(dto.description)
            .setGenre(dto.genre)
            .setYear(dto.year)
            .setTitle(dto.title)
            .setThumb_image(dto.thumb_image)
            .setUpdate_time(dto.update_time)
            .setOriginal_access_key(dto.original_access_key)
            .setOriginal_id(dto.original_id)
            .setOriginal_owner_id(dto.original_owner_id)
    }

    private fun buildGiftItemFromDbo(entity: GiftItemDboEntity): GiftItem {
        return GiftItem(entity.id)
            .setThumb48(entity.thumb48)
            .setThumb96(entity.thumb96)
            .setThumb256(entity.thumb256)
    }


    fun buildStickerFromDbo(entity: StickerDboEntity): Sticker {
        return Sticker(entity.id)
            .setImages(mapAll(entity.images, Entity2Model::map))
            .setImagesWithBackground(mapAll(entity.imagesWithBackground, Entity2Model::map))
            .setAnimations(mapAll(entity.animations) {
                mapStickerAnimation(
                    it
                )
            })
            .setAnimationUrl(entity.animationUrl)
    }

    private fun mapStickerAnimation(entity: AnimationEntity): Sticker.Animation {
        return Sticker.Animation(entity.url, entity.type)
    }


    fun map(entity: StickerSetEntity): StickerSet {
        return StickerSet(
            mapAll(entity.icon, Entity2Model::map),
            mapAll(entity.stickers) {
                buildStickerFromDbo(it)
            },
            entity.title
        )
    }

    fun map(entity: StickersKeywordsEntity): StickersKeywords {
        return StickersKeywords(
            entity.keywords,
            mapAll(entity.stickers) {
                buildStickerFromDbo(it)
            })
    }

    fun map(entity: StickerDboEntity.Img): Sticker.Image {
        return Sticker.Image(entity.url, entity.width, entity.height)
    }

    fun map(entity: StickerSetEntity.Img): StickerSet.Image {
        return StickerSet.Image(entity.url, entity.width, entity.height)
    }

    private fun buildWikiPageFromDbo(dbo: PageDboEntity): WikiPage {
        return WikiPage(dbo.id, dbo.ownerId)
            .setCreatorId(dbo.creatorId)
            .setTitle(dbo.title)
            .setSource(dbo.source)
            .setEditionTime(dbo.editionTime)
            .setCreationTime(dbo.creationTime)
            .setParent(dbo.parent)
            .setParent2(dbo.parent2)
            .setViews(dbo.views)
            .setViewUrl(dbo.viewUrl)
    }

    fun map(entity: AudioMessageDboEntity): VoiceMessage {
        return VoiceMessage(entity.id, entity.ownerId)
            .setAccessKey(entity.accessKey)
            .setDuration(entity.duration)
            .setLinkMp3(entity.linkMp3)
            .setLinkOgg(entity.linkOgg)
            .setWaveform(entity.waveform)
            .setTranscript(entity.transcript)
            .setWasListened(entity.was_listened)
    }


    fun buildDocumentFromDbo(dbo: DocumentDboEntity): Document {
        val document = Document(dbo.id, dbo.ownerId)
        document.setTitle(dbo.title)
            .setSize(dbo.size)
            .setExt(dbo.ext)
            .setUrl(dbo.url)
            .setAccessKey(dbo.accessKey)
            .setDate(dbo.date).setType(dbo.type)
        dbo.photo.requireNonNull {
            document.setPhotoPreview(buildPhotoSizesFromDbo(it))
        }
        dbo.video.requireNonNull {
            document.setVideoPreview(
                VideoPreview()
                    .setWidth(it.width)
                    .setHeight(it.height)
                    .setSrc(it.src)
            )
        }
        dbo.graffiti.requireNonNull {
            document.setGraffiti(
                Document.Graffiti()
                    .setHeight(it.height)
                    .setWidth(it.width)
                    .setSrc(it.src)
            )
        }
        return document
    }

    fun map(entity: PollDboEntity.Answer): Poll.Answer {
        return Poll.Answer(entity.id)
            .setRate(entity.rate)
            .setText(entity.text)
            .setVoteCount(entity.voteCount)
    }

    private fun buildPollFromDbo(entity: PollDboEntity): Poll {
        return Poll(entity.id, entity.ownerId)
            .setAnonymous(entity.isAnonymous)
            .setAnswers(mapAllMutable(entity.answers, Entity2Model::map))
            .setBoard(entity.isBoard)
            .setCreationTime(entity.creationTime)
            .setMyAnswerIds(entity.myAnswerIds)
            .setQuestion(entity.question)
            .setVoteCount(entity.voteCount)
            .setClosed(entity.isClosed)
            .setAuthorId(entity.authorId)
            .setCanVote(entity.isCanVote)
            .setCanEdit(entity.isCanEdit)
            .setCanReport(entity.isCanReport)
            .setCanShare(entity.isCanShare)
            .setEndDate(entity.endDate)
            .setMultiple(entity.isMultiple)
            .setPhoto(entity.photo)
    }

    private fun buildLinkFromDbo(dbo: LinkDboEntity): Link {
        return Link()
            .setUrl(dbo.url)
            .setTitle(dbo.title)
            .setCaption(dbo.caption)
            .setDescription(dbo.description)
            .setPreviewPhoto(dbo.previewPhoto)
            .setPhoto(dbo.photo?.let { map(it) })
    }


    fun buildArticleFromDbo(dbo: ArticleDboEntity): Article {
        return Article(dbo.id, dbo.ownerId)
            .setAccessKey(dbo.accessKey)
            .setOwnerName(dbo.ownerName)
            .setPhoto(dbo.photo?.let { map(it) })
            .setTitle(dbo.title)
            .setSubTitle(dbo.subTitle)
            .setURL(dbo.uRL)
            .setIsFavorite(dbo.isFavorite)
    }

    private fun buildCallFromDbo(dbo: CallDboEntity): Call {
        return Call().setInitiator_id(dbo.initiator_id)
            .setReceiver_id(dbo.receiver_id)
            .setState(dbo.state)
            .setTime(dbo.time)
    }

    private fun buildGeoFromDbo(dbo: GeoDboEntity): Geo {
        return Geo().setLatitude(dbo.latitude)
            .setLongitude(dbo.longitude)
            .setTitle(dbo.title)
            .setAddress(dbo.address)
            .setCountry(dbo.country)
            .setId(dbo.id)
    }

    private fun buildWallReplyDbo(dbo: WallReplyDboEntity, owners: IOwnersBundle): WallReply {
        val comment = WallReply().setId(dbo.id)
            .setOwnerId(dbo.ownerId)
            .setFromId(dbo.fromId)
            .setPostId(dbo.postId)
            .setText(dbo.text)
            .setAuthor(owners.getById(dbo.fromId))
        val attachments = if (dbo.getAttachments().nonNullNoEmpty()) buildAttachmentsFromDbos(
            dbo.getAttachments(),
            owners
        ) else null
        comment.setAttachments(attachments)
        return comment
    }

    private fun buildNotSupportedFromDbo(dbo: NotSupportedDboEntity): NotSupported {
        return NotSupported().setType(dbo.type).setBody(dbo.body)
    }

    private fun buildEventFromDbo(dbo: EventDboEntity, owners: IOwnersBundle): Event {
        return Event(dbo.id).setButton_text(dbo.button_text).setText(dbo.text)
            .setSubject(owners.getById(if (dbo.id >= 0) -dbo.id else dbo.id))
    }


    fun buildMarketFromDbo(dbo: MarketDboEntity): Market {
        return Market(dbo.id, dbo.owner_id)
            .setAccess_key(dbo.access_key)
            .setIs_favorite(dbo.isIs_favorite)
            .setAvailability(dbo.availability)
            .setDate(dbo.date)
            .setDescription(dbo.description)
            .setDimensions(dbo.dimensions)
            .setPrice(dbo.price)
            .setSku(dbo.sku)
            .setTitle(dbo.title)
            .setWeight(dbo.weight)
            .setPhotos(mapAll(dbo.photos) { map(it) })
            .setThumb_photo(dbo.thumb_photo)
    }

    private fun buildMarketAlbumFromDbo(dbo: MarketAlbumDboEntity): MarketAlbum {
        return MarketAlbum(dbo.id, dbo.owner_id)
            .setAccess_key(dbo.access_key)
            .setCount(dbo.count)
            .setTitle(dbo.title)
            .setUpdated_time(dbo.updated_time)
            .setPhoto(dbo.photo?.let { map(it) })
    }

    private fun mapArtistImage(dbo: AudioArtistImageEntity): AudioArtistImage {
        return AudioArtistImage(dbo.url, dbo.width, dbo.height)
    }

    private fun buildAudioArtistFromDbo(dbo: AudioArtistDboEntity): AudioArtist {
        return AudioArtist(dbo.id)
            .setName(dbo.name)
            .setPhoto(mapAll(dbo.photo) {
                mapArtistImage(
                    it
                )
            })
    }

    private fun buildStoryFromDbo(dbo: StoryDboEntity, owners: IOwnersBundle): Story {
        return Story().setId(dbo.id)
            .setOwnerId(dbo.ownerId)
            .setDate(dbo.date)
            .setExpires(dbo.expires)
            .setIs_expired(dbo.isIs_expired)
            .setAccessKey(dbo.accessKey)
            .setTarget_url(dbo.target_url)
            .setOwner(owners.getById(dbo.ownerId))
            .setPhoto(dbo.photo?.let { map(it) })
            .setVideo(dbo.video?.let { buildVideoFromDbo(it) })
    }

    private fun buildGraffitiFromDbo(dto: GraffitiDboEntity): Graffiti {
        return Graffiti().setId(dto.id)
            .setOwner_id(dto.owner_id)
            .setAccess_key(dto.access_key)
            .setHeight(dto.height)
            .setWidth(dto.width)
            .setUrl(dto.url)
    }


    fun buildNewsFromDbo(dbo: NewsDboEntity, owners: IOwnersBundle): News {
        val news = News()
            .setType(dbo.type)
            .setSourceId(dbo.sourceId)
            .setSource(owners.getById(dbo.sourceId))
            .setPostType(dbo.postType)
            .setFinalPost(dbo.isFinalPost)
            .setCopyOwnerId(dbo.copyOwnerId)
            .setCopyPostId(dbo.copyPostId)
            .setCopyPostDate(dbo.copyPostDate)
            .setDate(dbo.date)
            .setPostId(dbo.postId)
            .setText(dbo.text)
            .setCanEdit(dbo.isCanEdit)
            .setCanDelete(dbo.isCanDelete)
            .setCommentCount(dbo.commentCount)
            .setCommentCanPost(dbo.isCanPostComment)
            .setLikeCount(dbo.likesCount)
            .setUserLike(dbo.isUserLikes)
            .setCanLike(dbo.isCanLike)
            .setCanPublish(dbo.isCanPublish)
            .setRepostsCount(dbo.repostCount)
            .setUserReposted(dbo.isUserReposted)
            .setCopyright(dbo.copyright?.let { News.Copyright(it.name, it.link) })
            .setFriends(
                dbo.friendsTags?.let {
                    buildUserArray(
                        it,
                        owners
                    )
                }
            )
            .setViewCount(dbo.views)
        if (dbo.attachments.nonNullNoEmpty()) {
            news.setAttachments(buildAttachmentsFromDbos(dbo.attachments, owners))
        } else {
            news.setAttachments(null)
        }
        if (dbo.copyHistory.nonNullNoEmpty()) {
            dbo.copyHistory.nonNullNoEmpty {
                val copies: ArrayList<Post> = ArrayList(it.size)
                for (copyDbo in it) {
                    copies.add(buildPostFromDbo(copyDbo, owners))
                }
                news.setCopyHistory(copies)
            }
        } else {
            news.setCopyHistory(null)
        }
        return news
    }


    fun buildPostFromDbo(dbo: PostDboEntity, owners: IOwnersBundle): Post {
        val post = Post()
            .setDbid(dbo.dbid)
            .setVkid(dbo.id)
            .setOwnerId(dbo.ownerId)
            .setAuthorId(dbo.fromId)
            .setDate(dbo.date)
            .setText(dbo.text)
            .setReplyOwnerId(dbo.replyOwnerId)
            .setReplyPostId(dbo.replyPostId)
            .setFriendsOnly(dbo.isFriendsOnly)
            .setCommentsCount(dbo.commentsCount)
            .setCanPostComment(dbo.isCanPostComment)
            .setLikesCount(dbo.likesCount)
            .setUserLikes(dbo.isUserLikes)
            .setCanLike(dbo.isCanLike)
            .setCanRepost(dbo.isCanPublish)
            .setRepostCount(dbo.repostCount)
            .setUserReposted(dbo.isUserReposted)
            .setPostType(dbo.postType)
            .setSignerId(dbo.signedId)
            .setCreatorId(dbo.createdBy)
            .setCanEdit(dbo.isCanEdit)
            .setFavorite(dbo.isFavorite)
            .setCanPin(dbo.isCanPin)
            .setIsDonut(dbo.isDonut)
            .setPinned(dbo.isPinned)
            .setViewCount(dbo.views)
            .setCopyright(dbo.copyright?.let { Post.Copyright(it.name, it.link) })
        val sourceDbo = dbo.source
        if (sourceDbo != null) {
            post.setSource(
                PostSource(
                    sourceDbo.type,
                    sourceDbo.platform,
                    sourceDbo.data,
                    sourceDbo.url
                )
            )
        }
        if (dbo.getAttachments().nonNullNoEmpty()) {
            post.setAttachments(buildAttachmentsFromDbos(dbo.getAttachments(), owners))
        }
        dbo.copyHierarchy.nonNullNoEmpty {
            val copyCount = safeCountOf(it)
            for (copyDbo in it) {
                post.prepareCopyHierarchy(copyCount).add(buildPostFromDbo(copyDbo, owners))
            }
        }
        fillPostOwners(post, owners)
        if (post.hasCopyHierarchy()) {
            for (copy in post.getCopyHierarchy().orEmpty()) {
                fillPostOwners(copy, owners)
            }
        }
        return post
    }

    private fun mapSimplePrivacy(dbo: PrivacyEntity): SimplePrivacy {
        return SimplePrivacy(
            dbo.type,
            mapAll(dbo.entries) { orig ->
                SimplePrivacy.Entry(
                    orig.type,
                    orig.id,
                    orig.isAllowed
                )
            })
    }


    fun buildVideoFromDbo(entity: VideoDboEntity): Video {
        return Video()
            .setId(entity.id)
            .setOwnerId(entity.ownerId)
            .setAlbumId(entity.albumId)
            .setTitle(entity.title)
            .setDescription(entity.description)
            .setDuration(entity.duration)
            .setLink(entity.link)
            .setDate(entity.date)
            .setAddingDate(entity.addingDate)
            .setViews(entity.views)
            .setPlayer(entity.player)
            .setImage(entity.image)
            .setAccessKey(entity.accessKey)
            .setCommentsCount(entity.commentsCount)
            .setCanComment(entity.isCanComment)
            .setCanRepost(entity.isCanRepost)
            .setUserLikes(entity.isUserLikes)
            .setRepeat(entity.isRepeat)
            .setLikesCount(entity.likesCount)
            .setPrivacyView(entity.privacyView?.let { mapSimplePrivacy(it) })
            .setPrivacyComment(entity.privacyComment?.let { mapSimplePrivacy(it) })
            .setMp4link240(entity.mp4link240)
            .setMp4link360(entity.mp4link360)
            .setMp4link480(entity.mp4link480)
            .setMp4link720(entity.mp4link720)
            .setMp4link1080(entity.mp4link1080)
            .setMp4link1440(entity.mp4link1440)
            .setMp4link2160(entity.mp4link2160)
            .setExternalLink(entity.externalLink)
            .setHls(entity.hls)
            .setLive(entity.live)
            .setPlatform(entity.platform)
            .setCanEdit(entity.isCanEdit)
            .setCanAdd(entity.isCanAdd)
            .setPrivate(entity.private)
            .setFavorite(entity.isFavorite)
    }


    fun map(dbo: PhotoDboEntity): Photo {
        return Photo()
            .setId(dbo.id)
            .setAlbumId(dbo.albumId)
            .setOwnerId(dbo.ownerId)
            .setWidth(dbo.width)
            .setHeight(dbo.height)
            .setText(dbo.text)
            .setDate(dbo.date)
            .setUserLikes(dbo.isUserLikes)
            .setCanComment(dbo.isCanComment)
            .setLikesCount(dbo.likesCount)
            .setRepostsCount(dbo.repostsCount)
            .setCommentsCount(dbo.commentsCount)
            .setTagsCount(dbo.tagsCount)
            .setAccessKey(dbo.accessKey)
            .setDeleted(dbo.isDeleted)
            .setPostId(dbo.postId)
            .setSizes(dbo.sizes?.let { buildPhotoSizesFromDbo(it) })
    }

    private fun entity2modelNullable(size: PhotoSizeEntity.Size?): PhotoSizes.Size? {
        return if (size != null) {
            PhotoSizes.Size(size.getW(), size.h, size.url)
        } else null
    }

    private fun buildPhotoSizesFromDbo(dbo: PhotoSizeEntity): PhotoSizes {
        return PhotoSizes()
            .setS(entity2modelNullable(dbo.s))
            .setM(entity2modelNullable(dbo.m))
            .setX(entity2modelNullable(dbo.x))
            .setO(entity2modelNullable(dbo.o))
            .setP(entity2modelNullable(dbo.p))
            .setQ(entity2modelNullable(dbo.q))
            .setR(entity2modelNullable(dbo.r))
            .setY(entity2modelNullable(dbo.y))
            .setZ(entity2modelNullable(dbo.z))
            .setW(entity2modelNullable(dbo.w))
    }


    fun fillOwnerIds(ids: VKOwnIds, dbos: List<DboEntity?>?) {
        if (dbos != null) {
            for (entity in dbos) {
                fillOwnerIds(ids, entity)
            }
        }
    }


    fun fillPostOwnerIds(ids: VKOwnIds, dbo: PostDboEntity?) {
        if (dbo != null) {
            ids.append(dbo.fromId)
            ids.append(dbo.signedId)
            ids.append(dbo.createdBy)
            fillOwnerIds(ids, dbo.getAttachments())
            fillOwnerIds(ids, dbo.copyHierarchy)
        }
    }

    private fun fillStoryOwnerIds(ids: VKOwnIds, dbo: StoryDboEntity?) {
        if (dbo != null) {
            ids.append(dbo.ownerId)
        }
    }


    fun fillOwnerIds(ids: VKOwnIds, entity: CommentEntity?) {
        fillCommentOwnerIds(ids, entity)
    }


    fun fillOwnerIds(ids: VKOwnIds, dboEntity: DboEntity?) {
        when (dboEntity) {
            is MessageDboEntity -> {
                fillMessageOwnerIds(ids, dboEntity)
            }

            is PostDboEntity -> {
                fillPostOwnerIds(ids, dboEntity)
            }

            is StoryDboEntity -> {
                fillStoryOwnerIds(ids, dboEntity)
            }

            is WallReplyDboEntity -> {
                fillWallReplyOwnerIds(ids, dboEntity)
            }

            is EventDboEntity -> {
                fillEventIds(ids, dboEntity)
            }

            else -> {}
        }
    }

    private fun fillWallReplyOwnerIds(ids: VKOwnIds, dbo: WallReplyDboEntity?) {
        if (dbo != null) {
            ids.append(dbo.fromId)
            if (dbo.getAttachments() != null) {
                fillOwnerIds(ids, dbo.getAttachments())
            }
        }
    }

    private fun fillEventIds(ids: VKOwnIds, dbo: EventDboEntity?) {
        if (dbo != null) {
            ids.append(if (dbo.id >= 0) -dbo.id else dbo.id)
        }
    }


    fun fillCommentOwnerIds(ids: VKOwnIds, dbo: CommentEntity?) {
        if (dbo != null) {
            if (dbo.fromId != 0L) {
                ids.append(dbo.fromId)
            }
            if (dbo.replyToUserId != 0L) {
                ids.append(dbo.replyToUserId)
            }
            if (dbo.getAttachments() != null) {
                fillOwnerIds(ids, dbo.getAttachments())
            }
            dbo.threads.nonNullNoEmpty {
                for (i in it) {
                    fillCommentOwnerIds(ids, i)
                }
            }
        }
    }


    fun fillOwnerIds(ids: VKOwnIds, dbo: NewsDboEntity?) {
        if (dbo != null) {
            ids.append(dbo.sourceId)
            fillOwnerIds(ids, dbo.attachments)
            fillOwnerIds(ids, dbo.copyHistory)
            dbo.friendsTags.nonNullNoEmpty {
                ids.appendAll(it)
            }
        }
    }

    private fun fillMessageOwnerIds(ids: VKOwnIds, dbo: MessageDboEntity?) {
        if (dbo == null) {
            return
        }
        ids.append(dbo.fromId)
        ids.append(dbo.actionMemberId) // тут 100% пользователь, нюанс в том, что он может быть < 0, если email
        if (!Peer.isGroupChat(dbo.peerId) && !Peer.isContactChat(dbo.peerId)) {
            ids.append(dbo.peerId)
        }
        dbo.forwardMessages.nonNullNoEmpty {
            for (fwd in it) {
                fillMessageOwnerIds(ids, fwd)
            }
        }
        dbo.getAttachments().nonNullNoEmpty {
            for (attachmentEntity in it) {
                fillOwnerIds(ids, attachmentEntity)
            }
        }
    }
}