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
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.FriendsCounters
import dev.ragnarok.fenrir.model.GroupSettings
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserDetails
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.WallEditorAttrs
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils

object PlaceFactory {

    fun getUserDetailsPlace(accountId: Long, user: User, details: UserDetails): Place {
        return Place(Place.USER_DETAILS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
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


    fun getUserBlackListPlace(accountId: Long): Place {
        return Place(Place.USER_BLACKLIST)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getRequestExecutorPlace(accountId: Long): Place {
        return Place(Place.REQUEST_EXECUTOR)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getCommunityManagerEditPlace(accountId: Long, groupId: Long, manager: Manager?): Place {
        return Place(Place.COMMUNITY_MANAGER_EDIT)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.GROUP_ID, groupId)
            .withParcelableExtra(Extra.MANAGER, manager)
    }


    fun getCommunityManagerAddPlace(
        accountId: Long,
        groupId: Long,
        users: ArrayList<User>?
    ): Place {
        val place = Place(Place.COMMUNITY_MANAGER_ADD)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.GROUP_ID, groupId)
        place.prepareArguments().putParcelableArrayList(Extra.USERS, users)
        return place
    }


    fun getTmpSourceGalleryPlace(accountId: Long, source: TmpSource, index: Int): Place {
        return Place(Place.VK_PHOTO_TMP_SOURCE)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.INDEX, index)
            .withParcelableExtra(Extra.SOURCE, source)
    }


    fun getTmpSourceGalleryPlace(accountId: Long, ptr: Long, index: Int): Place {
        return Place(Place.VK_PHOTO_TMP_SOURCE)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.INDEX, index)
            .withLongExtra(Extra.SOURCE, ptr)
    }


    fun getCommunityAddBanPlace(accountId: Long, groupId: Long, users: ArrayList<User>?): Place {
        val place = Place(Place.COMMUNITY_ADD_BAN)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.GROUP_ID, groupId)
        place.prepareArguments().putParcelableArrayList(Extra.USERS, users)
        return place
    }


    fun getCommunityBanEditPlace(accountId: Long, groupId: Long, banned: Banned?): Place {
        return Place(Place.COMMUNITY_BAN_EDIT)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.GROUP_ID, groupId)
            .withParcelableExtra(Extra.BANNED, banned)
    }


    fun getCommunityControlPlace(
        accountId: Long,
        community: Community?,
        settings: GroupSettings?
    ): Place {
        return Place(Place.COMMUNITY_CONTROL)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.SETTINGS, settings)
            .withParcelableExtra(Extra.OWNER, community)
    }


    fun getShowCommunityInfoPlace(accountId: Long, community: Community?): Place {
        return Place(Place.COMMUNITY_INFO)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.OWNER, community)
    }


    fun getShowCommunityLinksInfoPlace(accountId: Long, community: Community?): Place {
        return Place(Place.COMMUNITY_INFO_LINKS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.OWNER, community)
    }

    fun getNewsfeedCommentsPlace(accountId: Long): Place {
        return Place(Place.NEWSFEED_COMMENTS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getSingleTabSearchPlace(
        accountId: Long,
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
        accountId: Long,
        commentId: Int,
        sourceOwnerId: Long,
        body: String?
    ): Place {
        return Place(Place.COMMENT_CREATE)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withIntExtra(Extra.COMMENT_ID, commentId)
            .withLongExtra(Extra.OWNER_ID, sourceOwnerId)
            .withStringExtra(Extra.BODY, body)
    }


    fun getPhotoAlbumGalleryPlace(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
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
        accountId: Long,
        albumId: Int,
        ownerId: Long,
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
        accountId: Long,
        albumId: Int,
        ownerId: Long,
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
        accountId: Long,
        photos: ArrayList<Photo>,
        position: Int,
        needRefresh: Boolean
    ): Place {
        return Place(Place.SIMPLE_PHOTO_GALLERY)
            .setArguments(buildArgsForSimpleGallery(accountId, position, photos, needRefresh))
    }


    fun getFavePhotosGallery(accountId: Long, photos: ArrayList<Photo>, position: Int): Place {
        return Place(Place.FAVE_PHOTOS_GALLERY)
            .setArguments(buildArgsForFave(accountId, photos, position))
    }


    fun getCreatePollPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.CREATE_POLL).setArguments(
            CreatePollFragment.buildArgs(
                accountId,
                ownerId
            )
        )
    }


    val settingsThemePlace: Place
        get() = Place(Place.SETTINGS_THEME)


    fun getPollPlace(accountId: Long, poll: Poll): Place {
        return Place(Place.POLL).setArguments(PollFragment.buildArgs(accountId, poll))
    }


    fun getGifPagerPlace(accountId: Long, documents: ArrayList<Document>, index: Int): Place {
        val place = Place(Place.GIF_PAGER)
        place.setArguments(GifPagerActivity.buildArgs(accountId, documents, index))
        return place
    }


    fun getWallAttachmentsPlace(accountId: Long, ownerId: Long, type: String?): Place {
        return Place(Place.WALL_ATTACHMENTS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
            .withStringExtra(Extra.TYPE, type)
    }

    fun getMessagesLookupPlace(
        aid: Long,
        peerId: Long,
        focusMessageId: Int,
        message: Message?
    ): Place {
        return Place(Place.MESSAGE_LOOKUP)
            .setArguments(MessagesLookFragment.buildArgs(aid, peerId, focusMessageId, message))
    }


    fun getWallSearchCommentsAttachmentsPlace(
        accountId: Long,
        ownerId: Long,
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
        aid: Long,
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


    fun getEditPhotoAlbumPlace(aid: Long, album: PhotoAlbum, editor: PhotoAlbumEditor): Place {
        return Place(Place.EDIT_PHOTO_ALBUM)
            .setArguments(CreatePhotoAlbumFragment.buildArgsForEdit(aid, album, editor))
    }


    fun getCreatePhotoAlbumPlace(aid: Long, ownerId: Long): Place {
        return Place(Place.CREATE_PHOTO_ALBUM)
            .setArguments(CreatePhotoAlbumFragment.buildArgsForCreate(aid, ownerId))
    }

    fun getMarketAlbumPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.MARKET_ALBUMS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
    }

    fun getNarrativesPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.NARRATIVES)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
    }

    fun getMarketPlace(accountId: Long, ownerId: Long, albumId: Int, isService: Boolean): Place {
        return Place(Place.MARKETS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
            .withIntExtra(Extra.ALBUM_ID, albumId)
            .withBoolExtra(Extra.SERVICE, isService)
    }


    fun getPhotoAllCommentsPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.PHOTO_ALL_COMMENT)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
    }


    fun getMarketViewPlace(accountId: Long, market: Market): Place {
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


    fun getResolveDomainPlace(aid: Long, url: String?, domain: String?): Place {
        return Place(Place.RESOLVE_DOMAIN).setArguments(
            ResolveDomainDialog.buildArgs(
                aid,
                url,
                domain
            )
        )
    }

    fun getBookmarksPlace(aid: Long, tab: Int): Place {
        return Place(Place.BOOKMARKS).setArguments(FaveTabsFragment.buildArgs(aid, tab))
    }

    fun getNotificationsPlace(aid: Long): Place {
        return Place(Place.NOTIFICATIONS).setArguments(FeedbackFragment.buildArgs(aid))
    }

    fun getFeedPlace(aid: Long): Place {
        return Place(Place.FEED).setArguments(FeedFragment.buildArgs(aid))
    }

    fun getDocumentsPlace(aid: Long, ownerId: Long, action: String?): Place {
        return Place(Place.DOCS).setArguments(DocsFragment.buildArgs(aid, ownerId, action))
    }

    fun getPreferencesPlace(aid: Long): Place {
        return Place(Place.PREFERENCES).setArguments(PreferencesFragment.buildArgs(aid))
    }

    fun getDialogsPlace(accountId: Long, dialogsOwnerId: Long, subtitle: String?): Place {
        return Place(Place.DIALOGS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, dialogsOwnerId)
            .withStringExtra(Extra.SUBTITLE, subtitle)
    }


    fun getChatPlace(accountId: Long, messagesOwnerId: Long, peer: Peer): Place {
        return Place(Place.CHAT)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, messagesOwnerId)
            .withParcelableExtra(Extra.PEER, peer)
    }


    fun getLocalServerPhotosPlace(accountId: Long): Place {
        return Place(Place.LOCAL_SERVER_PHOTO)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getVKPhotosAlbumPlace(
        accountId: Long,
        ownerId: Long,
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
        accountId: Long,
        ownerId: Long,
        action: String?,
        ownerWrapper: ParcelableOwnerWrapper?
    ): Place {
        return Place(Place.VK_PHOTO_ALBUMS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
            .withStringExtra(Extra.ACTION, action)
            .withParcelableExtra(Extra.OWNER, ownerWrapper)
    }

    fun getExternalLinkPlace(
        accountId: Long,
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

    fun getRepostPlace(accountId: Long, gid: Long?, post: Post?): Place {
        return Place(Place.REPOST).setArguments(RepostFragment.buildArgs(accountId, gid, post))
    }

    fun getEditCommentPlace(accountId: Long, comment: Comment?, commentId: Int?): Place {
        val ret = Place(Place.EDIT_COMMENT)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.COMMENT, comment)
        if (commentId != null) ret.withIntExtra(Extra.COMMENT_ID, commentId)
        return ret
    }

    fun getAudiosPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.AUDIOS).withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
    }

    fun getShortLinks(accountId: Long): Place {
        return Place(Place.SHORT_LINKS).withLongExtra(Extra.ACCOUNT_ID, accountId)
    }


    fun getImportantMessages(accountId: Long): Place {
        return Place(Place.IMPORTANT_MESSAGES).withLongExtra(Extra.ACCOUNT_ID, accountId)
    }

    fun getRemoteFileManager(): Place {
        return Place(Place.REMOTE_FILE_MANAGER)
    }

    fun getOwnerArticles(accountId: Long, ownerId: Long): Place {
        return Place(Place.OWNER_ARTICLES).withLongExtra(Extra.ACCOUNT_ID, accountId).withLongExtra(
            Extra.OWNER_ID, ownerId
        )
    }


    fun getMentionsPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.MENTIONS).withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
    }


    fun getAudiosInAlbumPlace(
        accountId: Long,
        ownerId: Long,
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


    fun SearchByAudioPlace(accountId: Long, audio_ownerId: Long, audio_id: Int): Place {
        return Place(Place.SEARCH_BY_AUDIO).withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(
                Extra.OWNER_ID, audio_ownerId
            ).withIntExtra(Extra.ID, audio_id)
    }


    fun getPlayerPlace(accountId: Long): Place {
        return Place(Place.PLAYER).setArguments(AudioPlayerFragment.buildArgs(accountId))
    }

    fun getVideosPlace(accountId: Long, ownerId: Long, action: String?): Place {
        return Place(Place.VIDEOS).setArguments(
            VideosTabsFragment.buildArgs(
                accountId,
                ownerId,
                action
            )
        )
    }


    fun getVideoAlbumPlace(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        action: String?,
        albumTitle: String?
    ): Place {
        return Place(Place.VIDEO_ALBUM)
            .setArguments(VideosFragment.buildArgs(accountId, ownerId, albumId, action, albumTitle))
    }


    fun getVideoPreviewPlace(accountId: Long, video: Video): Place {
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


    fun getHistoryVideoPreviewPlace(accountId: Long, stories: ArrayList<Story>, index: Int): Place {
        return Place(Place.STORY_PLAYER)
            .setArguments(StoryPagerActivity.buildArgs(accountId, stories, index))
    }

    fun getVideoPreviewPlace(
        accountId: Long,
        ownerId: Long,
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
        accountId: Long,
        type: String?,
        ownerId: Long,
        itemId: Int,
        filter: String?
    ): Place {
        return Place(Place.LIKES_AND_COPIES)
            .setArguments(LikesFragment.buildArgs(accountId, type, ownerId, itemId, filter))
    }

    fun getCommunitiesPlace(accountId: Long, userId: Long): Place {
        return Place(Place.COMMUNITIES)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.USER_ID, userId)
    }

    fun getFriendsFollowersPlace(
        accountId: Long,
        userId: Long,
        tab: Int,
        counters: FriendsCounters?
    ): Place {
        return Place(Place.FRIENDS_AND_FOLLOWERS)
            .setArguments(FriendsTabsFragment.buildArgs(accountId, userId, tab, counters))
    }


    fun getChatMembersPlace(accountId: Long, chatId: Long): Place {
        return Place(Place.CHAT_MEMBERS).setArguments(
            ChatMembersFragment.buildArgs(
                accountId,
                chatId
            )
        )
    }

    fun getOwnerWallPlace(accountId: Long, owner: Owner): Place {
        val ownerId = owner.ownerId
        return getOwnerWallPlace(accountId, ownerId, owner)
    }

    fun getOwnerWallPlace(accountId: Long, ownerId: Long, owner: Owner?): Place {
        return Place(Place.WALL).setArguments(AbsWallFragment.buildArgs(accountId, ownerId, owner))
    }


    fun getTopicsPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.TOPICS).setArguments(TopicsFragment.buildArgs(accountId, ownerId))
    }

    fun getSearchPlace(accountId: Long, tab: Int): Place {
        return Place(Place.SEARCH).setArguments(SearchTabsFragment.buildArgs(accountId, tab))
    }

    fun getAudiosTabsSearchPlace(accountId: Long): Place {
        return Place(Place.AUDIOS_SEARCH_TABS).setArguments(
            AudioSearchTabsFragment.buildArgs(
                accountId
            )
        )
    }


    fun getGroupChatsPlace(accountId: Long, groupId: Long): Place {
        return Place(Place.GROUP_CHATS).setArguments(
            GroupChatsFragment.buildArgs(
                accountId,
                groupId
            )
        )
    }


    fun getCreatePostPlace(
        accountId: Long, ownerId: Long, @EditingPostType editingType: Int,
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


    fun getForwardMessagesPlace(accountId: Long, messages: ArrayList<Message>): Place {
        return Place(Place.FORWARD_MESSAGES).setArguments(
            FwdsFragment.buildArgs(
                accountId,
                messages
            )
        )
    }


    fun getEditPostPlace(accountId: Long, post: Post, attrs: WallEditorAttrs): Place {
        return Place(Place.EDIT_POST)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withParcelableExtra(Extra.POST, post)
            .withParcelableExtra(Extra.ATTRS, attrs)
    }

    fun getPostPreviewPlace(accountId: Long, postId: Int, ownerId: Long): Place {
        return getPostPreviewPlace(accountId, postId, ownerId, null)
    }

    fun getPostPreviewPlace(accountId: Long, postId: Int, ownerId: Long, post: Post?): Place {
        return Place(Place.WALL_POST)
            .setArguments(WallPostFragment.buildArgs(accountId, postId, ownerId, post))
    }

    fun getCommunityMembersPlace(accountId: Long, groupId: Long): Place {
        return Place(Place.COMMUNITY_MEMBERS)
            .setArguments(CommunityMembersFragment.buildArgs(accountId, groupId))
    }

    fun getFriendsBirthdaysPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.FRIENDS_BIRTHDAYS)
            .setArguments(BirthDayFragment.buildArgs(accountId, ownerId))
    }

    fun getCatalogV2AudioCatalogPlace(
        accountId: Long,
        ownerId: Long,
        artistId: String?,
        query: String?,
        url: String?
    ): Place {
        return Place(Place.CATALOG_V2_AUDIO_CATALOG)
            .setArguments(CatalogV2ListFragment.buildArgs(accountId, ownerId, artistId, query, url))
    }

    fun getCatalogV2AudioSectionPlace(accountId: Long, sectionId: String): Place {
        return Place(Place.CATALOG_V2_AUDIO_SECTION)
            .setArguments(CatalogV2SectionFragment.buildArgs(accountId, sectionId))
    }

    fun getAlbumsByVideoPlace(
        accountId: Long,
        ownerId: Long,
        video_ownerId: Long,
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
        accountId: Long,
        docId: Int,
        ownerId: Long,
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


    fun getDocPreviewPlace(accountId: Long, document: Document): Place {
        return getDocPreviewPlace(
            accountId,
            document.id,
            document.ownerId,
            document.accessKey,
            document
        )
    }


    fun getConversationAttachmentsPlace(accountId: Long, peerId: Long, type: String?): Place {
        return Place(Place.CONVERSATION_ATTACHMENTS)
            .setArguments(ConversationFragmentFactory.buildArgs(accountId, peerId, type))
    }

    fun getCommentsPlace(accountId: Long, commented: Commented?, focusToCommentId: Int?): Place {
        return Place(Place.COMMENTS)
            .setArguments(CommentsFragment.buildArgs(accountId, commented, focusToCommentId, null))
    }

    fun getFeedBanPlace(accountId: Long): Place {
        return Place(Place.FEED_BAN)
            .setArguments(FeedBannedFragment.buildArgs(accountId))
    }

    fun getCommentsThreadPlace(
        accountId: Long,
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


    fun getArtistPlace(accountId: Long, id: String?): Place {
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


    fun getFriendsByPhonesPlace(accountId: Long): Place {
        return Place(Place.FRIENDS_BY_PHONES)
            .setArguments(FriendsByPhonesFragment.buildArgs(accountId))
    }


    fun getGiftsPlace(accountId: Long, ownerId: Long): Place {
        return Place(Place.GIFTS)
            .withLongExtra(Extra.ACCOUNT_ID, accountId)
            .withLongExtra(Extra.OWNER_ID, ownerId)
    }

    fun getShortcutsPlace(): Place {
        return Place(Place.SHORTCUTS)
    }

    fun getVotersPlace(
        accountId: Long,
        ownerId: Long,
        pollId: Int,
        board: Boolean,
        answer: Long
    ): Place {
        return Place(Place.VOTERS)
            .setArguments(VotersFragment.buildArgs(accountId, ownerId, pollId, board, answer))
    }
}