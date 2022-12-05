package dev.ragnarok.fenrir.place

import android.net.Uri
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.activity.SinglePhotoActivity
import dev.ragnarok.fenrir.activity.VideoPlayerActivity
import dev.ragnarok.fenrir.activity.gifpager.GifPagerActivity
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.buildArgsForAlbum
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.buildArgsForFave
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.buildArgsForSimpleGallery
import dev.ragnarok.fenrir.activity.storypager.StoryPagerActivity
import dev.ragnarok.fenrir.dialog.ResolveDomainDialog
import dev.ragnarok.fenrir.fragment.BrowserFragment
import dev.ragnarok.fenrir.fragment.DocPreviewFragment
import dev.ragnarok.fenrir.fragment.PreferencesFragment
import dev.ragnarok.fenrir.fragment.abswall.AbsWallFragment
import dev.ragnarok.fenrir.fragment.attachments.postcreate.PostCreateFragment
import dev.ragnarok.fenrir.fragment.attachments.repost.RepostFragment
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.audio.audios.AudiosFragment
import dev.ragnarok.fenrir.fragment.audio.audiosbyartist.AudiosByArtistFragment
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.lists.CatalogV2ListFragment
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.CatalogV2SectionFragment
import dev.ragnarok.fenrir.fragment.comments.CommentsFragment
import dev.ragnarok.fenrir.fragment.communitycontrol.communitymembers.CommunityMembersFragment
import dev.ragnarok.fenrir.fragment.conversation.ConversationFragmentFactory
import dev.ragnarok.fenrir.fragment.createphotoalbum.CreatePhotoAlbumFragment
import dev.ragnarok.fenrir.fragment.createpoll.CreatePollFragment
import dev.ragnarok.fenrir.fragment.docs.DocsFragment
import dev.ragnarok.fenrir.fragment.fave.FaveTabsFragment
import dev.ragnarok.fenrir.fragment.feed.FeedFragment
import dev.ragnarok.fenrir.fragment.feedback.FeedbackFragment
import dev.ragnarok.fenrir.fragment.feedbanned.FeedBannedFragment
import dev.ragnarok.fenrir.fragment.friends.birthday.BirthDayFragment
import dev.ragnarok.fenrir.fragment.friends.friendsbyphones.FriendsByPhonesFragment
import dev.ragnarok.fenrir.fragment.friends.friendstabs.FriendsTabsFragment
import dev.ragnarok.fenrir.fragment.groupchats.GroupChatsFragment
import dev.ragnarok.fenrir.fragment.likes.LikesFragment
import dev.ragnarok.fenrir.fragment.marketview.MarketViewFragment
import dev.ragnarok.fenrir.fragment.messages.chatmembers.ChatMembersFragment
import dev.ragnarok.fenrir.fragment.messages.fwds.FwdsFragment
import dev.ragnarok.fenrir.fragment.messages.messageslook.MessagesLookFragment
import dev.ragnarok.fenrir.fragment.messages.notreadmessages.NotReadMessagesFragment
import dev.ragnarok.fenrir.fragment.poll.PollFragment
import dev.ragnarok.fenrir.fragment.search.AudioSearchTabsFragment
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.SearchTabsFragment
import dev.ragnarok.fenrir.fragment.search.SingleTabSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.fragment.topics.TopicsFragment
import dev.ragnarok.fenrir.fragment.videoalbumsbyvideo.VideoAlbumsByVideoFragment
import dev.ragnarok.fenrir.fragment.videopreview.VideoPreviewFragment
import dev.ragnarok.fenrir.fragment.videos.VideosFragment
import dev.ragnarok.fenrir.fragment.videos.VideosTabsFragment
import dev.ragnarok.fenrir.fragment.vkphotos.VKPhotosFragment
import dev.ragnarok.fenrir.fragment.voters.VotersFragment
import dev.ragnarok.fenrir.fragment.wallattachments.wallsearchcommentsattachments.WallSearchCommentsAttachmentsFragment
import dev.ragnarok.fenrir.fragment.wallpost.WallPostFragment
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils

object PlaceFactory {

    fun getUserDetailsPlace(accountId: Int, user: User, details: UserDetails): Place {
        return Place(Place.USER_DETAILS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.USER, user)
            .withParcelableExtra("details", details)
    }

    val drawerEditPlace: Place
        get() = Place(Place.DRAWER_EDIT)
    val sideDrawerEditPlace: Place
        get() = Place(Place.SIDE_DRAWER_EDIT)

    val catalogV2ListEditPlace: Place
        get() = Place(Place.CATALOG_V2_LIST_EDIT)

    val proxyAddPlace: Place
        get() = Place(Place.PROXY_ADD)


    fun getUserBlackListPlace(accountId: Int): Place {
        return Place(Place.USER_BLACKLIST)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getRequestExecutorPlace(accountId: Int): Place {
        return Place(Place.REQUEST_EXECUTOR)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getCommunityManagerEditPlace(accountId: Int, groupId: Int, manager: Manager?): Place {
        return Place(Place.COMMUNITY_MANAGER_EDIT)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.GROUP_ID, groupId)
            .withParcelableExtra(Extra.MANAGER, manager)
    }


    fun getCommunityManagerAddPlace(accountId: Int, groupId: Int, users: ArrayList<User>?): Place {
        val place = Place(Place.COMMUNITY_MANAGER_ADD)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.GROUP_ID, groupId)
        place.prepareArguments().putParcelableArrayList(Extra.USERS, users)
        return place
    }


    fun getTmpSourceGalleryPlace(accountId: Int, source: TmpSource, index: Int): Place {
        return Place(Place.VK_PHOTO_TMP_SOURCE)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.INDEX, index)
            .withParcelableExtra(Extra.SOURCE, source)
    }


    fun getTmpSourceGalleryPlace(accountId: Int, ptr: Long, index: Int): Place {
        return Place(Place.VK_PHOTO_TMP_SOURCE)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.INDEX, index)
            .withLongExtra(Extra.SOURCE, ptr)
    }


    fun getCommunityAddBanPlace(accountId: Int, groupId: Int, users: ArrayList<User>?): Place {
        val place = Place(Place.COMMUNITY_ADD_BAN)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.GROUP_ID, groupId)
        place.prepareArguments().putParcelableArrayList(Extra.USERS, users)
        return place
    }


    fun getCommunityBanEditPlace(accountId: Int, groupId: Int, banned: Banned?): Place {
        return Place(Place.COMMUNITY_BAN_EDIT)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.GROUP_ID, groupId)
            .withParcelableExtra(Extra.BANNED, banned)
    }


    fun getCommunityControlPlace(
        accountId: Int,
        community: Community?,
        settings: GroupSettings?
    ): Place {
        return Place(Place.COMMUNITY_CONTROL)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.SETTINGS, settings)
            .withParcelableExtra(Extra.OWNER, community)
    }


    fun getShowComunityInfoPlace(accountId: Int, community: Community?): Place {
        return Place(Place.COMMUNITY_INFO)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.OWNER, community)
    }


    fun getShowComunityLinksInfoPlace(accountId: Int, community: Community?): Place {
        return Place(Place.COMMUNITY_INFO_LINKS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.OWNER, community)
    }

    fun getNewsfeedCommentsPlace(accountId: Int): Place {
        return Place(Place.NEWSFEED_COMMENTS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getSingleTabSearchPlace(
        accountId: Int,
        @SearchContentType type: Int,
        criteria: BaseSearchCriteria?
    ): Place {
        return Place(Place.SINGLE_SEARCH)
            .setArguments(SingleTabSearchFragment.buildArgs(accountId, type, criteria))
    }

    val logsPlace: Place
        get() = Place(Place.LOGS)


    fun getLocalImageAlbumPlace(album: LocalImageAlbum?): Place {
        return Place(Place.LOCAL_IMAGE_ALBUM)
            .withParcelableExtra(Extra.ALBUM, album)
    }


    fun getCommentCreatePlace(
        accountId: Int,
        commentId: Int,
        sourceOwnerId: Int,
        body: String?
    ): Place {
        return Place(Place.COMMENT_CREATE)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.COMMENT_ID, commentId)
            .withIntExtra(Extra.OWNER_ID, sourceOwnerId)
            .withStringExtra(Extra.BODY, body)
    }


    fun getPhotoAlbumGalleryPlace(
        accountId: Int,
        albumId: Int,
        ownerId: Int,
        parcelNativePointer: Long,
        position: Int,
        readOnly: Boolean,
        invert: Boolean
    ): Place {
        return Place(Place.VK_PHOTO_ALBUM_GALLERY_NATIVE)
            .setArguments(
                buildArgsForAlbum(
                    accountId,
                    albumId,
                    ownerId,
                    parcelNativePointer,
                    position,
                    readOnly,
                    invert
                )
            )
    }


    fun getPhotoAlbumGalleryPlace(
        accountId: Int,
        albumId: Int,
        ownerId: Int,
        photos: ArrayList<Photo>,
        position: Int,
        readOnly: Boolean,
        invert: Boolean
    ): Place {
        return Place(Place.VK_PHOTO_ALBUM_GALLERY)
            .setArguments(
                buildArgsForAlbum(
                    accountId,
                    albumId,
                    ownerId,
                    photos,
                    position,
                    readOnly,
                    invert
                )
            )
    }


    fun getPhotoAlbumGalleryPlace(
        accountId: Int,
        albumId: Int,
        ownerId: Int,
        source: TmpSource,
        position: Int,
        readOnly: Boolean,
        invert: Boolean
    ): Place {
        return Place(Place.VK_PHOTO_ALBUM_GALLERY_SAVED)
            .setArguments(
                buildArgsForAlbum(
                    accountId,
                    albumId,
                    ownerId,
                    source,
                    position,
                    readOnly,
                    invert
                )
            )
    }

    fun getSimpleGalleryPlace(
        accountId: Int,
        photos: ArrayList<Photo>,
        position: Int,
        needRefresh: Boolean
    ): Place {
        return Place(Place.SIMPLE_PHOTO_GALLERY)
            .setArguments(buildArgsForSimpleGallery(accountId, position, photos, needRefresh))
    }


    fun getFavePhotosGallery(accountId: Int, photos: ArrayList<Photo>, position: Int): Place {
        return Place(Place.FAVE_PHOTOS_GALLERY)
            .setArguments(buildArgsForFave(accountId, photos, position))
    }


    fun getCreatePollPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.CREATE_POLL).setArguments(
            CreatePollFragment.buildArgs(
                accountId,
                ownerId
            )
        )
    }


    val settingsThemePlace: Place
        get() = Place(Place.SETTINGS_THEME)


    fun getPollPlace(accountId: Int, poll: Poll): Place {
        return Place(Place.POLL).setArguments(PollFragment.buildArgs(accountId, poll))
    }


    fun getGifPagerPlace(accountId: Int, documents: ArrayList<Document>, index: Int): Place {
        val place = Place(Place.GIF_PAGER)
        place.setArguments(GifPagerActivity.buildArgs(accountId, documents, index))
        return place
    }


    fun getWallAttachmentsPlace(accountId: Int, ownerId: Int, type: String?): Place {
        return Place(Place.WALL_ATTACHMENTS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
            .withStringExtra(Extra.TYPE, type)
    }

    fun getMessagesLookupPlace(
        aid: Int,
        peerId: Int,
        focusMessageId: Int,
        message: Message?
    ): Place {
        return Place(Place.MESSAGE_LOOKUP)
            .setArguments(MessagesLookFragment.buildArgs(aid, peerId, focusMessageId, message))
    }


    fun getWallSearchCommentsAttachmentsPlace(
        accountId: Int,
        ownerId: Int,
        posts: ArrayList<Int>
    ): Place {
        return Place(Place.SEARCH_COMMENTS)
            .setArguments(
                WallSearchCommentsAttachmentsFragment.buildArgs(
                    accountId,
                    ownerId,
                    posts
                )
            )
    }


    fun getUnreadMessagesPlace(
        aid: Int,
        focusMessageId: Int,
        incoming: Int,
        outgoing: Int,
        unreadCount: Int,
        peer: Peer
    ): Place {
        return Place(Place.UNREAD_MESSAGES)
            .setArguments(
                NotReadMessagesFragment.buildArgs(
                    aid,
                    focusMessageId,
                    incoming,
                    outgoing,
                    unreadCount,
                    peer
                )
            )
    }


    fun getEditPhotoAlbumPlace(aid: Int, album: PhotoAlbum, editor: PhotoAlbumEditor): Place {
        return Place(Place.EDIT_PHOTO_ALBUM)
            .setArguments(CreatePhotoAlbumFragment.buildArgsForEdit(aid, album, editor))
    }


    fun getCreatePhotoAlbumPlace(aid: Int, ownerId: Int): Place {
        return Place(Place.CREATE_PHOTO_ALBUM)
            .setArguments(CreatePhotoAlbumFragment.buildArgsForCreate(aid, ownerId))
    }

    fun getMarketAlbumPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.MARKET_ALBUMS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
    }

    fun getNarrativesPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.NARRATIVES)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
    }

    fun getMarketPlace(accountId: Int, ownerId: Int, albumId: Int, isService: Boolean): Place {
        return Place(Place.MARKETS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
            .withIntExtra(Extra.ALBUM_ID, albumId)
            .withBoolExtra(Extra.SERVICE, isService)
    }


    fun getPhotoAllCommentsPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.PHOTO_ALL_COMMENT)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
    }


    fun getMarketViewPlace(accountId: Int, market: Market): Place {
        return Place(Place.MARKET_VIEW).setArguments(
            MarketViewFragment.buildArgs(
                accountId,
                market
            )
        )
    }

    val notificationSettingsPlace: Place
        get() = Place(Place.NOTIFICATION_SETTINGS)


    val securitySettingsPlace: Place
        get() = Place(Place.SECURITY)


    fun getVkInternalPlayerPlace(
        video: Video?,
        @InternalVideoSize size: Int,
        isLocal: Boolean
    ): Place {
        val place = Place(Place.VK_INTERNAL_PLAYER)
        place.prepareArguments().putParcelable(VideoPlayerActivity.EXTRA_VIDEO, video)
        place.prepareArguments().putInt(VideoPlayerActivity.EXTRA_SIZE, size)
        place.prepareArguments().putBoolean(VideoPlayerActivity.EXTRA_LOCAL, isLocal)
        return place
    }


    fun getResolveDomainPlace(aid: Int, url: String?, domain: String?): Place {
        return Place(Place.RESOLVE_DOMAIN).setArguments(
            ResolveDomainDialog.buildArgs(
                aid,
                url,
                domain
            )
        )
    }

    fun getBookmarksPlace(aid: Int, tab: Int): Place {
        return Place(Place.BOOKMARKS).setArguments(FaveTabsFragment.buildArgs(aid, tab))
    }

    fun getNotificationsPlace(aid: Int): Place {
        return Place(Place.NOTIFICATIONS).setArguments(FeedbackFragment.buildArgs(aid))
    }

    fun getFeedPlace(aid: Int): Place {
        return Place(Place.FEED).setArguments(FeedFragment.buildArgs(aid))
    }

    fun getDocumentsPlace(aid: Int, ownerId: Int, action: String?): Place {
        return Place(Place.DOCS).setArguments(DocsFragment.buildArgs(aid, ownerId, action))
    }

    fun getPreferencesPlace(aid: Int): Place {
        return Place(Place.PREFERENCES).setArguments(PreferencesFragment.buildArgs(aid))
    }

    fun getDialogsPlace(accountId: Int, dialogsOwnerId: Int, subtitle: String?): Place {
        return Place(Place.DIALOGS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, dialogsOwnerId)
            .withStringExtra(Extra.SUBTITLE, subtitle)
    }


    fun getChatPlace(accountId: Int, messagesOwnerId: Int, peer: Peer): Place {
        return Place(Place.CHAT)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, messagesOwnerId)
            .withParcelableExtra(Extra.PEER, peer)
    }


    fun getLocalServerPhotosPlace(accountId: Int): Place {
        return Place(Place.LOCAL_SERVER_PHOTO)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getVKPhotosAlbumPlace(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        action: String?,
        selected: Int = -1
    ): Place {
        return Place(Place.VK_PHOTO_ALBUM).setArguments(
            VKPhotosFragment.buildArgs(
                accountId,
                ownerId,
                albumId,
                action,
                selected
            )
        )
    }

    fun getVKPhotoAlbumsPlace(
        accountId: Int,
        ownerId: Int,
        action: String?,
        ownerWrapper: ParcelableOwnerWrapper?
    ): Place {
        return Place(Place.VK_PHOTO_ALBUMS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
            .withStringExtra(Extra.ACTION, action)
            .withParcelableExtra(Extra.OWNER, ownerWrapper)
    }

    fun getExternalLinkPlace(
        accountId: Int,
        url: String,
        owner: String? = null,
        type: String? = null
    ): Place {
        return Place(Place.EXTERNAL_LINK).setArguments(
            BrowserFragment.buildArgs(
                accountId,
                url,
                owner,
                type
            )
        )
    }

    fun getRepostPlace(accountId: Int, gid: Int?, post: Post?): Place {
        return Place(Place.REPOST).setArguments(RepostFragment.buildArgs(accountId, gid, post))
    }

    fun getEditCommentPlace(accountId: Int, comment: Comment?, commemtId: Int?): Place {
        val ret = Place(Place.EDIT_COMMENT)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.COMMENT, comment)
        if (commemtId != null) ret.withIntExtra(Extra.COMMENT_ID, commemtId)
        return ret
    }

    fun getAudiosPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.AUDIOS).withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
    }

    fun getShortLinks(accountId: Int): Place {
        return Place(Place.SHORT_LINKS).withIntExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getImportantMessages(accountId: Int): Place {
        return Place(Place.IMPORTANT_MESSAGES).withIntExtra(Extra.ACCOUNT_ID, accountId)
    }

    fun getRemoteFileManager(): Place {
        return Place(Place.REMOTE_FILE_MANAGER)
    }

    fun getOwnerArticles(accountId: Int, ownerId: Int): Place {
        return Place(Place.OWNER_ARTICLES).withIntExtra(Extra.ACCOUNT_ID, accountId).withIntExtra(
            Extra.OWNER_ID, ownerId
        )
    }


    fun getMentionsPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.MENTIONS).withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
    }


    fun getAudiosInAlbumPlace(
        accountId: Int,
        ownerId: Int,
        albumId: Int?,
        access_key: String?
    ): Place {
        return Place(Place.AUDIOS_IN_ALBUM).setArguments(
            AudiosFragment.buildArgs(
                accountId,
                ownerId,
                albumId,
                access_key
            )
        )
    }


    fun SearchByAudioPlace(accountId: Int, audio_ownerId: Int, audio_id: Int): Place {
        return Place(Place.SEARCH_BY_AUDIO).withIntExtra(Extra.ACCOUNT_ID, accountId).withIntExtra(
            Extra.OWNER_ID, audio_ownerId
        ).withIntExtra(Extra.ID, audio_id)
    }


    fun getPlayerPlace(accountId: Int): Place {
        return Place(Place.PLAYER).setArguments(AudioPlayerFragment.buildArgs(accountId))
    }

    fun getVideosPlace(accountId: Int, ownerId: Int, action: String?): Place {
        return Place(Place.VIDEOS).setArguments(
            VideosTabsFragment.buildArgs(
                accountId,
                ownerId,
                action
            )
        )
    }


    fun getVideoAlbumPlace(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        action: String?,
        albumTitle: String?
    ): Place {
        return Place(Place.VIDEO_ALBUM)
            .setArguments(VideosFragment.buildArgs(accountId, ownerId, albumId, action, albumTitle))
    }


    fun getVideoPreviewPlace(accountId: Int, video: Video): Place {
        return Place(Place.VIDEO_PREVIEW)
            .setArguments(
                VideoPreviewFragment.buildArgs(
                    accountId,
                    video.ownerId,
                    video.id,
                    video.accessKey,
                    video
                )
            )
    }


    fun getHistoryVideoPreviewPlace(accountId: Int, stories: ArrayList<Story>, index: Int): Place {
        return Place(Place.STORY_PLAYER)
            .setArguments(StoryPagerActivity.buildArgs(accountId, stories, index))
    }

    fun getVideoPreviewPlace(
        accountId: Int,
        ownerId: Int,
        videoId: Int,
        accessKey: String?,
        video: Video?
    ): Place {
        return Place(Place.VIDEO_PREVIEW)
            .setArguments(
                VideoPreviewFragment.buildArgs(
                    accountId,
                    ownerId,
                    videoId,
                    accessKey,
                    video
                )
            )
    }

    fun getSingleURLPhotoPlace(url: String?, prefix: String?, photo_prefix: String?): Place {
        return Place(Place.SINGLE_PHOTO)
            .setArguments(SinglePhotoActivity.buildArgs(url, prefix, photo_prefix))
    }

    fun getLikesCopiesPlace(
        accountId: Int,
        type: String?,
        ownerId: Int,
        itemId: Int,
        filter: String?
    ): Place {
        return Place(Place.LIKES_AND_COPIES)
            .setArguments(LikesFragment.buildArgs(accountId, type, ownerId, itemId, filter))
    }

    fun getCommunitiesPlace(accountId: Int, userId: Int): Place {
        return Place(Place.COMMUNITIES)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.USER_ID, userId)
    }

    fun getFriendsFollowersPlace(
        accountId: Int,
        userId: Int,
        tab: Int,
        counters: FriendsCounters?
    ): Place {
        return Place(Place.FRIENDS_AND_FOLLOWERS)
            .setArguments(FriendsTabsFragment.buildArgs(accountId, userId, tab, counters))
    }


    fun getChatMembersPlace(accountId: Int, chatId: Int): Place {
        return Place(Place.CHAT_MEMBERS).setArguments(
            ChatMembersFragment.buildArgs(
                accountId,
                chatId
            )
        )
    }

    fun getOwnerWallPlace(accountId: Int, owner: Owner): Place {
        val ownerId = owner.ownerId
        return getOwnerWallPlace(accountId, ownerId, owner)
    }

    fun getOwnerWallPlace(accountId: Int, ownerId: Int, owner: Owner?): Place {
        return Place(Place.WALL).setArguments(AbsWallFragment.buildArgs(accountId, ownerId, owner))
    }


    fun getTopicsPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.TOPICS).setArguments(TopicsFragment.buildArgs(accountId, ownerId))
    }

    fun getSearchPlace(accountId: Int, tab: Int): Place {
        return Place(Place.SEARCH).setArguments(SearchTabsFragment.buildArgs(accountId, tab))
    }

    fun getAudiosTabsSearchPlace(accountId: Int): Place {
        return Place(Place.AUDIOS_SEARCH_TABS).setArguments(
            AudioSearchTabsFragment.buildArgs(
                accountId
            )
        )
    }


    fun getGroupChatsPlace(accountId: Int, groupId: Int): Place {
        return Place(Place.GROUP_CHATS).setArguments(
            GroupChatsFragment.buildArgs(
                accountId,
                groupId
            )
        )
    }


    fun getCreatePostPlace(
        accountId: Int, ownerId: Int, @EditingPostType editingType: Int,
        input: List<AbsModel>?, attrs: WallEditorAttrs,
        streams: ArrayList<Uri>?, body: String?, mime: String?
    ): Place {
        val bundle = ModelsBundle(Utils.safeCountOf(input))
        if (input != null) {
            bundle.append(input)
        }
        return Place(Place.BUILD_NEW_POST)
            .setArguments(
                PostCreateFragment.buildArgs(
                    accountId,
                    ownerId,
                    editingType,
                    bundle,
                    attrs,
                    streams,
                    body,
                    mime
                )
            )
    }


    fun getForwardMessagesPlace(accountId: Int, messages: ArrayList<Message>): Place {
        return Place(Place.FORWARD_MESSAGES).setArguments(
            FwdsFragment.buildArgs(
                accountId,
                messages
            )
        )
    }


    fun getEditPostPlace(accountId: Int, post: Post, attrs: WallEditorAttrs): Place {
        return Place(Place.EDIT_POST)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.POST, post)
            .withParcelableExtra(Extra.ATTRS, attrs)
    }

    fun getPostPreviewPlace(accountId: Int, postId: Int, ownerId: Int): Place {
        return getPostPreviewPlace(accountId, postId, ownerId, null)
    }

    fun getPostPreviewPlace(accountId: Int, postId: Int, ownerId: Int, post: Post?): Place {
        return Place(Place.WALL_POST)
            .setArguments(WallPostFragment.buildArgs(accountId, postId, ownerId, post))
    }

    fun getCommunityMembersPlace(accountId: Int, groupId: Int): Place {
        return Place(Place.COMMUNITY_MEMBERS)
            .setArguments(CommunityMembersFragment.buildArgs(accountId, groupId))
    }

    fun getFriendsBirthdaysPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.FRIENDS_BIRTHDAYS)
            .setArguments(BirthDayFragment.buildArgs(accountId, ownerId))
    }

    fun getCatalogV2AudioCatalogPlace(
        accountId: Int,
        ownerId: Int,
        artistId: String?,
        query: String?,
        url: String?
    ): Place {
        return Place(Place.CATALOG_V2_AUDIO_CATALOG)
            .setArguments(CatalogV2ListFragment.buildArgs(accountId, ownerId, artistId, query, url))
    }

    fun getCatalogV2AudioSectionPlace(accountId: Int, sectionId: String): Place {
        return Place(Place.CATALOG_V2_AUDIO_SECTION)
            .setArguments(CatalogV2SectionFragment.buildArgs(accountId, sectionId))
    }

    fun getAlbumsByVideoPlace(
        accountId: Int,
        ownerId: Int,
        video_ownerId: Int,
        video_Id: Int
    ): Place {
        return Place(Place.ALBUMS_BY_VIDEO)
            .setArguments(
                VideoAlbumsByVideoFragment.buildArgs(
                    accountId,
                    ownerId,
                    video_ownerId,
                    video_Id
                )
            )
    }


    fun getDocPreviewPlace(
        accountId: Int,
        docId: Int,
        ownerId: Int,
        accessKey: String?,
        document: Document?
    ): Place {
        val place = Place(Place.DOC_PREVIEW)
        place.setArguments(
            DocPreviewFragment.buildArgs(
                accountId,
                docId,
                ownerId,
                accessKey,
                document
            )
        )
        return place
    }


    fun getDocPreviewPlace(accountId: Int, document: Document): Place {
        return getDocPreviewPlace(
            accountId,
            document.id,
            document.ownerId,
            document.accessKey,
            document
        )
    }


    fun getConversationAttachmentsPlace(accountId: Int, peerId: Int, type: String?): Place {
        return Place(Place.CONVERSATION_ATTACHMENTS)
            .setArguments(ConversationFragmentFactory.buildArgs(accountId, peerId, type))
    }

    fun getCommentsPlace(accountId: Int, commented: Commented?, focusToCommentId: Int?): Place {
        return Place(Place.COMMENTS)
            .setArguments(CommentsFragment.buildArgs(accountId, commented, focusToCommentId, null))
    }

    fun getFeedBanPlace(accountId: Int): Place {
        return Place(Place.FEED_BAN)
            .setArguments(FeedBannedFragment.buildArgs(accountId))
    }

    fun getCommentsThreadPlace(
        accountId: Int,
        commented: Commented?,
        focusToCommentId: Int?,
        commemtId: Int?
    ): Place {
        return Place(Place.COMMENTS)
            .setArguments(
                CommentsFragment.buildArgs(
                    accountId,
                    commented,
                    focusToCommentId,
                    commemtId
                )
            )
    }


    fun getArtistPlace(accountId: Int, id: String?): Place {
        return if (Settings.get().other().isAudio_catalog_v2) getCatalogV2AudioCatalogPlace(
            accountId,
            accountId,
            id, null, null
        ) else Place(Place.ARTIST).setArguments(
            AudiosByArtistFragment.buildArgs(
                accountId,
                id
            )
        )
    }


    fun getFriendsByPhonesPlace(accountId: Int): Place {
        return Place(Place.FRIENDS_BY_PHONES)
            .setArguments(FriendsByPhonesFragment.buildArgs(accountId))
    }


    fun getGiftsPlace(accountId: Int, ownerId: Int): Place {
        return Place(Place.GIFTS)
            .withIntExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.OWNER_ID, ownerId)
    }

    fun getShortcutsPlace(): Place {
        return Place(Place.SHORTCUTS)
    }

    fun getVotersPlace(
        accountId: Int,
        ownerId: Int,
        pollId: Int,
        board: Boolean,
        answer: Long
    ): Place {
        return Place(Place.VOTERS)
            .setArguments(VotersFragment.buildArgs(accountId, ownerId, pollId, board, answer))
    }
}