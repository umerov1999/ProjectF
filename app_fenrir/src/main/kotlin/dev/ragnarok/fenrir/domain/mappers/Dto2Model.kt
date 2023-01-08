package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.VKApiConversation.CurrentKeyboard
import dev.ragnarok.fenrir.api.model.longpoll.AddMessageUpdate
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.api.util.VKStringUtils.unescape
import dev.ragnarok.fenrir.crypt.CryptHelper.analizeMessageBody
import dev.ragnarok.fenrir.crypt.MessageType
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.domain.mappers.MapUtil.calculateConversationAcl
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.AudioArtist.AudioArtistImage
import dev.ragnarok.fenrir.model.Document.VideoPreview
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.safeCountOf

object Dto2Model {

    fun transform(dto: VKApiFriendList): FriendList {
        return FriendList(dto.id, dto.name)
    }

    fun transform(dto: VKApiPhotoAlbum): PhotoAlbum {
        return PhotoAlbum(dto.id, dto.owner_id)
            .setSize(dto.size)
            .setTitle(dto.title)
            .setDescription(dto.description)
            .setCanUpload(dto.can_upload)
            .setUpdatedTime(dto.updated)
            .setCreatedTime(dto.created)
            .setSizes(if (dto.photo != null) transform(dto.photo) else null)
            .setUploadByAdminsOnly(dto.upload_by_admins_only)
            .setCommentsDisabled(dto.comments_disabled)
            .setPrivacyView(dto.privacy_view?.let { transform(it) })
            .setPrivacyComment(dto.privacy_comment?.let { transform(it) })
    }


    fun transform(chat: VKApiChat): Chat {
        return Chat(chat.id)
            .setPhoto50(chat.photo_50)
            .setPhoto100(chat.photo_100)
            .setPhoto200(chat.photo_200)
            .setTitle(chat.title)
    }

    fun transform(narrative: VKApiNarratives): Narratives {
        return Narratives(narrative.id, narrative.owner_id)
            .setTitle(narrative.title)
            .setCover(narrative.cover)
            .setStory_ids(narrative.story_ids)
    }

    fun transformOwner(owner: VKApiOwner?): Owner? {
        return if (owner is VKApiUser) transformUser(owner) else transformCommunity(
            owner as VKApiCommunity
        )
    }

    fun transformOwners(
        users: Collection<VKApiUser>?,
        communities: Collection<VKApiCommunity>?
    ): List<Owner> {
        val owners: MutableList<Owner> = ArrayList(safeCountOf(users) + safeCountOf(communities))
        if (users != null) {
            for (user in users) {
                owners.add(transformUser(user))
            }
        }
        if (communities != null) {
            for (community in communities) {
                transformCommunity(community)?.let { owners.add(it) }
            }
        }
        return owners
    }

    private fun transformGroupChat(chats: VKApiGroupChats): GroupChats {
        return GroupChats(chats.id)
            .setInvite_link(chats.invite_link)
            .setIs_closed(chats.is_closed)
            .setPhoto(chats.photo)
            .setTitle(chats.title)
            .setMembers_count(chats.members_count)
            .setLastUpdateTime(chats.last_message_date)
    }

    fun transformCommunity(community: VKApiCommunity?): Community? {
        community ?: return null
        return Community(community.id)
            .setName(community.fullName)
            .setScreenName(community.screen_name)
            .setBlacklisted(community.blacklisted)
            .setClosed(community.is_closed)
            .setVerified(community.verified)
            .setAdmin(community.is_admin)
            .setAdminLevel(community.admin_level)
            .setMember(community.is_member)
            .setMemberStatus(community.member_status)
            .setCommunityType(community.type)
            .setPhoto50(community.photo_50)
            .setPhoto100(community.photo_100)
            .setPhoto200(community.photo_200)
            .setMembersCount(community.members_count)
            .setHasUnseenStories(community.has_unseen_stories)
    }

    fun transform(dto: VKApiGiftItem): GiftItem {
        return GiftItem(dto.id)
            .setThumb48(dto.thumb_48)
            .setThumb96(dto.thumb_96)
            .setThumb256(dto.thumb_256)
    }

    fun transform(dto: VKApiGift): Gift {
        return Gift(dto.id)
            .setFromId(dto.from_id)
            .setMessage(dto.message)
            .setDate(dto.date)
            .setGiftItem(dto.gift?.let { transform(it) })
            .setPrivacy(dto.privacy)
    }


    fun transformCommunities(dtos: List<VKApiCommunity>?): List<Community> {
        return mapAll(
            dtos
        ) {
            transformCommunity(it)
        }
    }


    fun transformGroupChats(dtos: List<VKApiGroupChats>?): List<GroupChats> {
        return mapAll(
            dtos
        ) {
            transformGroupChat(it)
        }
    }


    fun transformGifts(dtos: List<VKApiGift>?): List<Gift> {
        return mapAll(
            dtos
        ) { transform(it) }
    }


    fun transformMarketAlbums(dtos: List<VKApiMarketAlbum>?): List<MarketAlbum> {
        return mapAll(
            dtos
        ) {
            transform(it)
        }
    }

    fun transformMarket(dtos: List<VKApiMarket>?): List<Market> {
        return mapAll(
            dtos
        ) { transform(it) }
    }


    fun transformUsers(dtos: List<VKApiUser>?): List<User> {
        return mapAll(
            dtos
        ) { transformUser(it) }
    }

    fun transformFaveUser(favePage: FavePageResponse): FavePage {
        var id = 0
        when (favePage.type) {
            FavePageType.USER -> id = favePage.user?.id ?: 0
            FavePageType.COMMUNITY -> id = favePage.group?.id ?: 0
        }
        val page = FavePage(id)
            .setDescription(favePage.description)
            .setFaveType(favePage.type)
            .setUpdatedDate(favePage.updated_date)
        favePage.user.requireNonNull {
            page.setUser(transformUser(it))
        }
        if (favePage.group != null) {
            page.setGroup(transformCommunity(favePage.group))
        }
        return page
    }


    fun transformUser(user: VKApiUser): User {
        return User(user.id)
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
            .setBlacklisted(user.blacklisted)
            .setBlacklisted_by_me(user.blacklisted_by_me)
            .setCan_access_closed(user.can_access_closed)
            .setBdate(user.bdate)
            .setVerified(user.verified)
            .setMaiden_name(user.maiden_name)
            .setHasUnseenStories(user.has_unseen_stories)
    }

    fun transform(accountUid: Int, update: AddMessageUpdate): VKApiMessage {
        val message = VKApiMessage()
        message.id = update.messageId
        message.out = update.isOut
        message.important = update.important
        message.deleted = update.deleted
        //message.read_state = !update.unread;
        message.peer_id = update.peerId
        message.from_id =
            if (message.out) accountUid else if (Peer.isGroupChat(update.peerId) || Peer.isContactChat(
                    update.peerId
                )
            ) update.from else update.peerId
        message.body = unescape(update.text)
        //message.title = update.subject;
        message.date = update.timestamp
        message.action_mid = update.sourceMid
        message.action_text = update.sourceText
        message.action = update.sourceAct
        message.random_id = update.random_id
        message.keyboard = update.keyboard
        message.payload = update.payload
        message.update_time = update.edit_time
        return message
    }

    fun transform(
        accountId: Int,
        dtos: List<VKApiDialog>,
        owners: IOwnersBundle,
        contacts: List<VKApiConversation.ContactElement>?
    ): List<Dialog> {
        val data: MutableList<Dialog> = ArrayList(dtos.size)
        for (dto in dtos) {
            transform(accountId, dto, owners, contacts)?.let { data.add(it) }
        }
        return data
    }

    private fun mapKeyboard(keyboard: CurrentKeyboard?): Keyboard? {
        if (keyboard == null || keyboard.buttons.isNullOrEmpty()) {
            return null
        }
        val buttons: ArrayList<List<Keyboard.Button>> = ArrayList()
        for (i in keyboard.buttons.orEmpty()) {
            val v: MutableList<Keyboard.Button> = ArrayList()
            for (s in i) {
                if (s.action == null || "text" != s.action?.type && "open_link" != s.action?.type) {
                    continue
                }
                s.action.requireNonNull {
                    v.add(
                        Keyboard.Button().setType(it.type).setColor(s.color)
                            .setLabel(it.label).setLink(it.link)
                            .setPayload(it.payload)
                    )
                }
            }
            if (v.isNotEmpty()) {
                buttons.add(v)
            }
        }
        return if (buttons.isNotEmpty()) {
            Keyboard().setAuthor_id(keyboard.author_id)
                .setInline(keyboard.inline)
                .setOne_time(keyboard.one_time)
                .setButtons(buttons)
        } else null
    }


    fun transform(
        accountId: Int,
        dto: VKApiConversation,
        bundle: IOwnersBundle,
        contacts: List<VKApiConversation.ContactElement>?
    ): Conversation? {
        val entity = Conversation(dto.peer?.id ?: return null)
            .setInRead(dto.inRead)
            .setOutRead(dto.outRead)
            .setUnreadCount(dto.unreadCount)
            .setAcl(calculateConversationAcl(dto))
        if (!Peer.isGroupChat(dto.peer?.id.orZero())) {
            val own = bundle.getById(dto.peer?.id ?: return null)
            entity.setTitle(own.fullName)
            entity.setPhoto50(own.get100photoOrSmaller())
            entity.setPhoto100(own.get100photoOrSmaller())
            entity.setPhoto200(own.maxSquareAvatar)
        }
        dto.settings.requireNonNull {
            entity.setTitle(it.title)
            it.pinnedMesage.requireNonNull { pit ->
                entity.setPinned(transform(accountId, pit, bundle))
            }
            dto.settings?.photo.requireNonNull { pit ->
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


    fun transform(
        accountId: Int,
        dto: VKApiDialog,
        bundle: IOwnersBundle,
        contacts: List<VKApiConversation.ContactElement>?
    ): Dialog? {
        val message = dto.lastMessage
        val interlocutor = message?.let {
            if (Peer.isGroup(it.peer_id) || Peer.isUser(it.peer_id)) {
                bundle.getById(it.peer_id)
            } else {
                bundle.getById(it.from_id)
            }
        }
        val dialog = Dialog()
            .setPeerId(message?.peer_id ?: return null)
            .setUnreadCount(dto.conversation?.unreadCount.orZero())
            .setInRead(dto.conversation?.inRead.orZero())
            .setOutRead(dto.conversation?.outRead.orZero())
            .setMessage(transform(accountId, message, bundle))
            .setLastMessageId(message.id)
            .setInterlocutor(interlocutor)
        dto.conversation?.settings.requireNonNull {
            dialog.setTitle(it.title)
            dialog.setGroupChannel(it.is_group_channel)
            it.photo.requireNonNull { pit ->
                dialog.setPhoto50(pit.photo50)
                    .setPhoto100(pit.photo100).setPhoto200(pit.photo200)
            }
        }
        if ("contact" == dto.conversation?.peer?.type && contacts.nonNullNoEmpty()) {
            for (i in contacts) {
                if (i.id == dto.conversation?.peer?.local_id) {
                    dialog.setTitle(i.name)
                    dialog.setPhoto50(i.photo_50)
                        .setPhoto100(i.photo_100).setPhoto200(i.photo_200)
                    break
                }
            }
        }
        dto.conversation?.sort_id.requireNonNull {
            dialog.setMajor_id(it.major_id)
            dialog.setMinor_id(it.minor_id)
        }
        return dialog
    }


    fun transformMessages(
        aid: Int,
        dtos: List<VKApiMessage>,
        owners: IOwnersBundle
    ): List<Message> {
        val data: MutableList<Message> = ArrayList(dtos.size)
        for (dto in dtos) {
            data.add(transform(aid, dto, owners))
        }
        return data
    }

    private fun transformKeyboard(keyboard: CurrentKeyboard?): Keyboard? {
        if (keyboard == null || keyboard.buttons.isNullOrEmpty()) {
            return null
        }
        val buttons: ArrayList<List<Keyboard.Button>> = ArrayList()
        for (i in keyboard.buttons.orEmpty()) {
            val v: ArrayList<Keyboard.Button> = ArrayList()
            for (s in i) {
                if (s.action == null || "text" != s.action?.type && "open_link" != s.action?.type) {
                    continue
                }
                s.action?.let {
                    v.add(
                        Keyboard.Button().setType(it.type).setColor(s.color)
                            .setLabel(it.label).setLink(it.link)
                            .setPayload(it.payload)
                    )
                }
            }
            if (v.isNotEmpty()) {
                buttons.add(v)
            }
        }
        return if (buttons.isNotEmpty()) {
            Keyboard().setAuthor_id(keyboard.author_id)
                .setInline(keyboard.inline)
                .setOne_time(keyboard.one_time)
                .setButtons(buttons)
        } else null
    }


    fun transform(aid: Int, message: VKApiMessage, owners: IOwnersBundle): Message {
        val encrypted = analizeMessageBody(message.body) == MessageType.CRYPTED
        val appMessage = Message(message.id)
            .setAccountId(aid)
            .setBody(message.body) //.setTitle(message.title)
            .setPeerId(message.peer_id)
            .setSenderId(message.from_id) //.setRead(message.read_state)
            .setOut(message.out)
            .setStatus(MessageStatus.SENT)
            .setDate(message.date)
            .setHasAttachments(message.attachments != null && message.attachments?.nonEmpty() == true)
            .setForwardMessagesCount(safeCountOf(message.fwd_messages))
            .setDeleted(message.deleted)
            .setDeletedForAll(false) // cant be deleted from api?
            .setOriginalId(message.id)
            .setCryptStatus(if (encrypted) CryptStatus.ENCRYPTED else CryptStatus.NO_ENCRYPTION)
            .setImportant(message.important)
            .setAction(Message.fromApiChatAction(message.action))
            .setActionMid(message.action_mid)
            .setActionEmail(message.action_email)
            .setActionText(message.action_text)
            .setPhoto50(message.action_photo_50)
            .setPhoto100(message.action_photo_100)
            .setPhoto200(message.action_photo_200)
            .setSender(owners.getById(message.from_id))
            .setPayload(message.payload)
            .setKeyboard(transformKeyboard(message.keyboard))
        if (message.action_mid != 0) {
            appMessage.setActionUser(owners.getById(message.action_mid))
        }
        if (message.attachments != null && message.attachments?.nonEmpty() == true) {
            message.attachments?.let {
                appMessage.setAttachments(buildAttachments(it, owners))
            }
        }
        message.fwd_messages.nonNullNoEmpty {
            for (fwd in it) {
                appMessage.prepareFwd(it.size).add(transform(aid, fwd, owners))
            }
        }
        message.random_id.nonNullNoEmpty {
            try {
                appMessage.setRandomId(it.toLong())
            } catch (ignored: NumberFormatException) {
            }
        }
        return appMessage
    }

    fun transform(dto: VKApiDoc.Graffiti): Document.Graffiti {
        return Document.Graffiti()
            .setWidth(dto.width)
            .setHeight(dto.height)
            .setSrc(dto.src)
    }

    fun transform(dto: VKApiDoc.Video): VideoPreview {
        return VideoPreview()
            .setHeight(dto.height)
            .setSrc(dto.src)
            .setWidth(dto.width)
    }

    private fun dto2model(dto: PhotoSizeDto): PhotoSizes.Size {
        return PhotoSizes.Size(dto.width, dto.height, Utils.firstNonEmptyString(dto.url, dto.src))
    }


    fun transform(dtos: List<PhotoSizeDto>?): PhotoSizes {
        val sizes = PhotoSizes()
        if (dtos != null) {
            for (dto in dtos) {
                when (dto.type) {
                    PhotoSizeDto.Type.S -> sizes.setS(dto2model(dto))
                    PhotoSizeDto.Type.M -> sizes.setM(dto2model(dto))
                    PhotoSizeDto.Type.X -> sizes.setX(dto2model(dto))
                    PhotoSizeDto.Type.Y -> sizes.setY(dto2model(dto))
                    PhotoSizeDto.Type.Z -> sizes.setZ(dto2model(dto))
                    PhotoSizeDto.Type.W -> sizes.setW(dto2model(dto))
                    PhotoSizeDto.Type.O -> sizes.setO(dto2model(dto))
                    PhotoSizeDto.Type.P -> sizes.setP(dto2model(dto))
                    PhotoSizeDto.Type.Q -> sizes.setQ(dto2model(dto))
                    PhotoSizeDto.Type.R -> sizes.setR(dto2model(dto))
                }
            }
        }
        return sizes
    }

    fun transform(orig: VKApiPrivacy): SimplePrivacy {
        val entries = ArrayList<SimplePrivacy.Entry>(safeCountOf(orig.entries))
        for (entry in orig.entries) {
            entries.add(SimplePrivacy.Entry(entry.type, entry.id, entry.allowed))
        }
        return SimplePrivacy(orig.category, entries)
    }


    fun transform(
        simplePrivacy: SimplePrivacy,
        owners: IOwnersBundle,
        friendListMap: Map<Int, FriendList>
    ): Privacy {
        val privacy = Privacy()
        privacy.setType(simplePrivacy.getType() ?: "null")
        for (entry in simplePrivacy.getEntries().orEmpty()) {
            when (entry.getType()) {
                VKApiPrivacy.Entry.TYPE_FRIENDS_LIST -> if (entry.isAllowed()) {
                    friendListMap[entry.getId()]?.let { privacy.allowFor(it) }
                } else {
                    friendListMap[entry.getId()]?.let { privacy.disallowFor(it) }
                }
                VKApiPrivacy.Entry.TYPE_OWNER -> if (entry.isAllowed()) {
                    privacy.allowFor(owners.getById(entry.getId()) as User)
                } else {
                    privacy.disallowFor(owners.getById(entry.getId()) as User)
                }
            }
        }
        return privacy
    }

    private fun buildUserArray(users: List<Int>, owners: IOwnersBundle): List<User> {
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

    fun buildComment(commented: Commented, dto: VKApiComment, owners: IOwnersBundle): Comment {
        val comment = Comment(commented)
            .setId(dto.id)
            .setFromId(dto.from_id)
            .setDate(dto.date)
            .setText(dto.text)
            .setReplyToComment(dto.reply_to_comment)
            .setReplyToUser(dto.reply_to_user)
            .setLikesCount(dto.likes)
            .setUserLikes(dto.user_likes)
            .setCanLike(dto.can_like)
            .setCanEdit(dto.can_edit)
            .setThreadsCount(dto.threads_count)
            .setThreads(buildComments(commented, dto.threads, owners))
            .setPid(dto.pid)
        if (dto.from_id != 0) {
            comment.setAuthor(owners.getById(dto.from_id))
        }
        dto.attachments.requireNonNull {
            comment.setAttachments(buildAttachments(it, owners))
            //comment.setHasAttachmens(comment.getAttachments().count());
        }
        return comment
    }

    private fun buildComments(
        commented: Commented,
        dtos: List<VKApiComment>?,
        owners: IOwnersBundle
    ): List<Comment>? {
        if (dtos.isNullOrEmpty()) {
            return null
        }
        val o: MutableList<Comment> = ArrayList()
        for (i in dtos) {
            val u = buildComment(commented, i, owners)
            o.add(u)
        }
        return o
    }


    fun transform(dto: VKApiTopic, owners: IOwnersBundle): Topic {
        val topic = Topic(dto.id, dto.owner_id)
            .setTitle(dto.title)
            .setCreationTime(dto.created)
            .setCreatedByOwnerId(dto.created_by)
            .setLastUpdateTime(dto.updated)
            .setUpdatedByOwnerId(dto.updated_by)
            .setClosed(dto.is_closed)
            .setFixed(dto.is_fixed)
            .setCommentsCount(dto.comments?.count.orZero())
            .setFirstCommentBody(dto.first_comment)
            .setLastCommentBody(dto.last_comment)
        if (dto.updated_by != 0) {
            topic.setUpdater(owners.getById(dto.updated_by))
        }
        if (dto.created_by != 0) {
            topic.setCreator(owners.getById(dto.created_by))
        }
        return topic
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


    fun transform(dto: VKApiPoll): Poll {
        val answers: MutableList<Poll.Answer> = ArrayList(safeCountOf(dto.answers))
        dto.answers.nonNullNoEmpty {
            for (answer in it) {
                answers.add(
                    Poll.Answer(answer.id)
                        .setRate(answer.rate)
                        .setText(answer.text)
                        .setVoteCount(answer.votes)
                )
            }
        }
        return Poll(dto.id, dto.owner_id)
            .setAnonymous(dto.anonymous)
            .setAnswers(answers)
            .setBoard(dto.is_board)
            .setCreationTime(dto.created)
            .setMyAnswerIds(dto.answer_ids)
            .setQuestion(dto.question)
            .setVoteCount(dto.votes)
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


    fun transformStory(dto: VKApiStory, owners: IOwnersBundle): Story {
        return Story().setId(dto.id)
            .setOwnerId(dto.owner_id)
            .setDate(dto.date)
            .setExpires(dto.expires_at)
            .setIs_expired(dto.is_expired)
            .setAccessKey(dto.access_key)
            .setTarget_url(dto.target_url)
            .setPhoto(dto.photo?.let { transform(it) })
            .setVideo(dto.video?.let { transform(it) })
            .setOwner(owners.getById(dto.owner_id))
    }

    fun transform(dto: VKApiCall): Call {
        return Call().setInitiator_id(dto.initiator_id)
            .setReceiver_id(dto.receiver_id)
            .setState(dto.state)
            .setTime(dto.time)
    }

    fun transform(dto: VKApiGeo): Geo {
        return Geo().setLatitude(dto.latitude)
            .setLongitude(dto.longitude)
            .setTitle(dto.title)
            .setAddress(dto.address)
            .setCountry(dto.country)
            .setId(dto.id)
    }

    fun transform(dto: VKApiWallReply, owners: IOwnersBundle): WallReply {
        val comment = WallReply().setId(dto.id)
            .setOwnerId(dto.owner_id)
            .setFromId(dto.from_id)
            .setPostId(dto.post_id)
            .setText(dto.text)
            .setAuthor(owners.getById(dto.from_id))
        dto.attachments.requireNonNull {
            comment.setAttachments(buildAttachments(it, owners))
            //comment.setHasAttachmens(comment.getAttachments().count());
        }
        return comment
    }

    fun transform(dto: VKApiNotSupported): NotSupported {
        return NotSupported().setType(dto.attachmentType).setBody(dto.bodyJson)
    }

    private fun transformEvent(dto: VKApiEvent, owners: IOwnersBundle): Event {
        return Event(dto.id).setButton_text(dto.button_text).setText(dto.text)
            .setSubject(owners.getById(if (dto.id >= 0) -dto.id else dto.id))
    }


    fun transform(dto: VKApiMarket): Market {
        return Market(dto.id, dto.owner_id)
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
            .setThumb_photo(dto.thumb_photo)
            .setPhotos(mapAll(dto.photos) { transform(it) })
    }

    fun transform(dto: VKApiMarketAlbum): MarketAlbum {
        return MarketAlbum(dto.id, dto.owner_id)
            .setAccess_key(dto.access_key)
            .setCount(dto.count)
            .setTitle(dto.title)
            .setUpdated_time(dto.updated_time)
            .setPhoto(dto.photo?.let { transform(it) })
    }

    fun transform(dto: VKApiGraffiti): Graffiti {
        return Graffiti().setId(dto.id)
            .setOwner_id(dto.owner_id)
            .setAccess_key(dto.access_key)
            .setHeight(dto.height)
            .setWidth(dto.width)
            .setUrl(dto.url)
    }


    fun transform(dto: VKApiShortLink): ShortLink {
        return ShortLink().setKey(dto.key)
            .setShort_url(dto.short_url)
            .setUrl(dto.url)
            .setAccess_key(dto.access_key)
            .setViews(dto.views)
            .setTimestamp(dto.timestamp)
    }


    fun transform(dto: VKApiPhoto): Photo {
        return Photo()
            .setId(dto.id)
            .setAlbumId(dto.album_id)
            .setOwnerId(dto.owner_id)
            .setWidth(dto.width)
            .setHeight(dto.height)
            .setText(dto.text)
            .setDate(dto.date)
            .setUserLikes(dto.user_likes)
            .setCanComment(dto.can_comment)
            .setLikesCount(dto.likes)
            .setCommentsCount(dto.comments?.count.orZero())
            .setTagsCount(dto.tags)
            .setAccessKey(dto.access_key)
            .setDeleted(false)
            .setPostId(dto.post_id)
            .setSizes(if (dto.sizes == null) null else transform(dto.sizes))
    }


    fun transform(dto: VKApiAudio): Audio {
        return Audio()
            .setId(dto.id)
            .setOwnerId(dto.owner_id)
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
            .setDeleted(false)
            .setAlbum_title(dto.album_title)
            .setThumb_image_big(dto.thumb_image_big)
            .setThumb_image_little(dto.thumb_image_little)
            .setThumb_image_very_big(dto.thumb_image_very_big)
            .setIsHq(dto.isHq)
            .setMain_artists(dto.main_artists).updateDownloadIndicator()
    }


    fun transform(dto: VKApiAudioPlaylist): AudioPlaylist {
        return AudioPlaylist()
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

    fun transform(link: VKApiLink): Link {
        return Link()
            .setUrl(link.url)
            .setTitle(link.title)
            .setCaption(link.caption)
            .setDescription(link.description)
            .setPreviewPhoto(link.preview_photo)
            .setPhoto(link.photo?.let { transform(it) })
    }


    fun transform(article: VKApiArticle): Article {
        return Article(article.id, article.owner_id)
            .setAccessKey(article.access_key)
            .setOwnerName(article.owner_name)
            .setPhoto(article.photo?.let { transform(it) })
            .setTitle(article.title)
            .setSubTitle(article.subtitle)
            .setURL(article.url)
            .setIsFavorite(article.is_favorite)
    }


    fun map(dto: VKApiAudioArtist.Image): AudioArtistImage {
        return AudioArtistImage(dto.url, dto.width, dto.height)
    }


    fun transform(dto: VKApiAudioArtist): AudioArtist {
        return AudioArtist(dto.id)
            .setName(dto.name)
            .setPhoto(
                mapAll(
                    dto.photo
                ) {
                    map(it)
                }
            )
    }


    fun map(dto: VKApiSticker.Image): Sticker.Image {
        return Sticker.Image(dto.url, dto.width, dto.height)
    }

    fun transform(dto: VKApiSticker): Sticker {
        return Sticker(dto.sticker_id)
            .setImages(
                mapAll(
                    dto.images
                ) {
                    map(it)
                }
            )
            .setImagesWithBackground(
                mapAll(
                    dto.images_with_background
                ) {
                    map(it)
                }
            )
            .setAnimationUrl(dto.animation_url)
    }

    fun transform(dto: FaveLinkDto): FaveLink {
        return FaveLink(dto.id)
            .setUrl(dto.url)
            .setTitle(dto.title)
            .setDescription(dto.description)
            .setPhoto(dto.photo?.let { transform(it) })
    }

    fun transform(dto: VKApiAudioMessage): VoiceMessage {
        return VoiceMessage(dto.id, dto.owner_id)
            .setDuration(dto.duration)
            .setWaveform(dto.waveform)
            .setLinkOgg(dto.linkOgg)
            .setLinkMp3(dto.linkMp3)
            .setAccessKey(dto.access_key)
            .setTranscript(dto.transcript)
            .setWasListened(dto.was_listened)
    }


    fun transform(dto: VKApiDoc): Document {
        val document = Document(dto.id, dto.ownerId)
        document.setTitle(dto.title)
            .setSize(dto.size)
            .setExt(dto.ext)
            .setUrl(dto.url)
            .setAccessKey(dto.accessKey)
            .setDate(dto.date).setType(dto.type)
        dto.preview.requireNonNull {
            it.photo?.sizes.requireNonNull { pit ->
                document.setPhotoPreview(transform(pit))
            }
            dto.preview?.video.requireNonNull { pit ->
                document.setVideoPreview(
                    VideoPreview()
                        .setWidth(pit.width)
                        .setHeight(pit.height)
                        .setSrc(pit.src)
                )
            }
            dto.preview?.graffiti.requireNonNull { pit ->
                document.setGraffiti(
                    Document.Graffiti()
                        .setHeight(pit.height)
                        .setWidth(pit.width)
                        .setSrc(pit.src)
                )
            }
        }
        return document
    }

    /*public static Document transform(@NonNull VKApiDocument dto) {
        Document document = dto.isVoiceMessage() ? new VoiceMessage(dto.id, dto.owner_id)
                : new Document(dto.id, dto.owner_id);

        document.setTitle(dto.title)
                .setSize(dto.size)
                .setExt(dto.ext)
                .setUrl(dto.url)
                .setAccessKey(dto.access_key)
                .setDate(dto.date)
                .setType(dto.type);

        if (document instanceof VoiceMessage) {
            ((VoiceMessage) document)
                    .setDuration(dto.preview.audio_msg.duration)
                    .setWaveform(dto.preview.audio_msg.waveform)
                    .setLinkOgg(dto.preview.audio_msg.link_ogg)
                    .setLinkMp3(dto.preview.audio_msg.link_mp3);
        }

        if (dto.preview != null) {
            if (dto.preview.photo_sizes != null) {
                document.setPhotoPreview(transform(dto.preview.photo_sizes));
            }

            if (dto.preview.video_preview != null) {
                document.setVideoPreview(new Document.VideoPreview()
                        .setWidth(dto.preview.video_preview.width)
                        .setHeight(dto.preview.video_preview.height)
                        .setSrc(dto.preview.video_preview.src));
            }

            if (dto.preview.graffiti != null) {
                document.setGraffiti(new Document.Graffiti()
                        .setHeight(dto.preview.graffiti.height)
                        .setWidth(dto.preview.graffiti.width)
                        .setSrc(dto.preview.graffiti.src));
            }
        }

        return document;
    }*/

    fun transform(dto: VKApiVideo): Video {
        return Video()
            .setId(dto.id)
            .setOwnerId(dto.owner_id)
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
            .setPrivacyView(dto.privacy_view?.let { transform(it) })
            .setPrivacyComment(dto.privacy_comment?.let { transform(it) })
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

    fun transform(dto: VKApiWikiPage): WikiPage {
        return WikiPage(dto.id, dto.owner_id)
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

    private fun buildAttachments(
        apiAttachments: VKApiAttachments,
        owners: IOwnersBundle
    ): Attachments {
        val attachments = Attachments()
        val entries = apiAttachments.entryList()
        for (entry in entries) {
            val attachment = entry.attachment
            when (attachment.getType()) {
                VKApiAttachment.TYPE_AUDIO -> attachments.prepareAudios().add(
                    transform(attachment as VKApiAudio)
                )
                VKApiAttachment.TYPE_STICKER -> attachments.prepareStickers()
                    .add(transform(attachment as VKApiSticker))
                VKApiAttachment.TYPE_PHOTO -> attachments.preparePhotos()
                    .add(transform(attachment as VKApiPhoto))
                VKApiAttachment.TYPE_DOC -> attachments.prepareDocs()
                    .add(transform(attachment as VKApiDoc))
                VKApiAttachment.TYPE_AUDIO_MESSAGE -> attachments.prepareVoiceMessages().add(
                    transform(attachment as VKApiAudioMessage)
                )
                VKApiAttachment.TYPE_VIDEO -> attachments.prepareVideos()
                    .add(transform(attachment as VKApiVideo))
                VKApiAttachment.TYPE_LINK -> attachments.prepareLinks()
                    .add(transform(attachment as VKApiLink))
                VKApiAttachment.TYPE_ARTICLE -> attachments.prepareArticles()
                    .add(transform(attachment as VKApiArticle))
                VKApiAttachment.TYPE_STORY -> attachments.prepareStories()
                    .add(transformStory(attachment as VKApiStory, owners))
                VKApiAttachment.TYPE_ALBUM -> attachments.preparePhotoAlbums().add(
                    transform(attachment as VKApiPhotoAlbum)
                )
                VKApiAttachment.TYPE_CALL -> attachments.prepareCalls()
                    .add(transform(attachment as VKApiCall))
                VKApiAttachment.TYPE_GEO -> attachments.prepareGeos()
                    .add(transform(attachment as VKApiGeo))
                VKApiAttachment.TYPE_WALL_REPLY -> attachments.prepareWallReply()
                    .add(transform(attachment as VKApiWallReply, owners))
                VKApiAttachment.TYPE_NOT_SUPPORT -> attachments.prepareNotSupporteds()
                    .add(transform(attachment as VKApiNotSupported))
                VKApiAttachment.TYPE_EVENT -> attachments.prepareEvents()
                    .add(transformEvent(attachment as VKApiEvent, owners))
                VKApiAttachment.TYPE_MARKET -> attachments.prepareMarkets()
                    .add(transform(attachment as VKApiMarket))
                VKApiAttachment.TYPE_MARKET_ALBUM -> attachments.prepareMarketAlbums()
                    .add(transform(attachment as VKApiMarketAlbum))
                VKApiAttachment.TYPE_ARTIST -> attachments.prepareAudioArtist()
                    .add(transform(attachment as VKApiAudioArtist))
                VKApiAttachment.TYPE_AUDIO_PLAYLIST -> attachments.prepareAudioPlaylists().add(
                    transform(attachment as VKApiAudioPlaylist)
                )
                VKApiAttachment.TYPE_GRAFFITI -> attachments.prepareGraffity()
                    .add(transform(attachment as VKApiGraffiti))
                VKApiAttachment.TYPE_POLL -> attachments.preparePolls()
                    .add(transform(attachment as VKApiPoll))
                VKApiAttachment.TYPE_WIKI_PAGE -> attachments.prepareWikiPages()
                    .add(transform(attachment as VKApiWikiPage))
                VKApiAttachment.TYPE_POST -> attachments.preparePosts()
                    .add(transform(attachment as VKApiPost, owners))
            }
        }
        return attachments
    }


    fun transformPosts(dtos: Collection<VKApiPost>, bundle: IOwnersBundle): List<Post> {
        val posts: MutableList<Post> = ArrayList(safeCountOf(dtos))
        for (dto in dtos) {
            posts.add(transform(dto, bundle))
        }
        return posts
    }


    fun transformAttachmentsPosts(
        dtos: Collection<VKApiAttachments.Entry>,
        bundle: IOwnersBundle
    ): List<Post> {
        val posts: MutableList<Post> = ArrayList(safeCountOf(dtos))
        for (dto in dtos) {
            if (dto.attachment is VKApiPost) posts.add(
                transform(
                    dto.attachment,
                    bundle
                )
            )
        }
        return posts
    }


    fun transform(dto: VKApiPost, owners: IOwnersBundle): Post {
        val post = Post()
            .setDbid(PostDboEntity.NO_STORED)
            .setVkid(dto.id)
            .setOwnerId(dto.owner_id)
            .setAuthorId(dto.from_id)
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
            .setCanRepost(dto.can_publish)
            .setRepostCount(dto.reposts_count)
            .setUserReposted(dto.user_reposted)
            .setPostType(dto.post_type)
            .setSignerId(dto.signer_id)
            .setCreatorId(dto.created_by)
            .setCanEdit(dto.can_edit)
            .setFavorite(dto.is_favorite)
            .setIsDonut(dto.is_donut)
            .setCanPin(dto.can_pin)
            .setPinned(dto.is_pinned)
            .setViewCount(dto.views)
            .setCopyright(dto.copyright?.let { Post.Copyright(it.name, it.link) })
        dto.post_source.requireNonNull {
            post.setSource(
                PostSource(
                    it.type,
                    it.platform,
                    it.data,
                    it.url
                )
            )
        }
        if (dto.hasAttachments()) {
            post.setAttachments(dto.attachments?.let { buildAttachments(it, owners) })
        }
        if (dto.hasCopyHistory()) {
            val copyCount = safeCountOf(dto.copy_history)
            for (copy in dto.copy_history.orEmpty()) {
                post.prepareCopyHierarchy(copyCount).add(transform(copy, owners))
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


    fun buildNews(original: VKApiNews, owners: IOwnersBundle): News {
        val news = News()
            .setType(original.type)
            .setSourceId(original.source_id)
            .setPostType(original.post_type)
            .setFinalPost(original.final_post)
            .setCopyOwnerId(original.copy_owner_id)
            .setCopyPostId(original.copy_post_id)
            .setCopyPostDate(original.copy_post_date)
            .setDate(original.date)
            .setPostId(original.post_id)
            .setText(original.text)
            .setCanEdit(original.can_edit)
            .setCanDelete(original.can_delete)
            .setCommentCount(original.comment_count)
            .setCommentCanPost(original.comment_can_post)
            .setLikeCount(original.like_count)
            .setUserLike(original.user_like)
            .setCanLike(original.can_like)
            .setCanPublish(original.can_publish)
            .setRepostsCount(original.reposts_count)
            .setUserReposted(original.user_reposted)
            .setFriends(original.friends?.let { buildUserArray(it, owners) })
            .setSource(owners.getById(original.source_id))
            .setViewCount(original.views)
            .setCopyright(original.copyright?.let { News.Copyright(it.name, it.link) })
        if (original.hasCopyHistory()) {
            val copies = ArrayList<Post>(original.copy_history?.size.orZero())
            for (copy in original.copy_history.orEmpty()) {
                copies.add(transform(copy, owners))
            }
            news.setCopyHistory(copies)
        }
        if (original.hasAttachments()) {
            news.setAttachments(original.attachments?.let { buildAttachments(it, owners) })
        }
        return news
    }


    fun fillPostOwners(post: Post, owners: IOwnersBundle) {
        if (post.authorId != 0) {
            post.setAuthor(owners.getById(post.authorId))
        }
        if (post.signerId != 0) {
            post.setCreator(owners.getById(post.signerId) as User)
        } else if (post.creatorId != 0) {
            post.setCreator(owners.getById(post.creatorId) as User)
        }
    }
}
