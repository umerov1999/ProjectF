package dev.ragnarok.fenrir.fragment.base

import android.Manifest
import android.os.Bundle
import android.view.View
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.dialog.PostShareDialog
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.listener.OwnerClickListener
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import kotlin.math.abs

abstract class PlaceSupportMvpFragment<P : PlaceSupportPresenter<V>, V> : BaseMvpFragment<P, V>(),
    OnAttachmentsActionCallback, IAttachmentsPlacesView,
    OwnerClickListener where V : IMvpView, V : IAttachmentsPlacesView {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            PostShareDialog.REQUEST_POST_SHARE,
            this
        ) { _: String?, result: Bundle? ->
            val method = PostShareDialog.extractMethod(
                result ?: return@setFragmentResultListener
            )
            val accountId1 = PostShareDialog.extractAccountId(result)
            val post1 = PostShareDialog.extractPost(result)
            when (method) {
                PostShareDialog.Methods.SHARE_LINK -> Utils.shareLink(
                    requireActivity(),
                    post1?.generateVkPostLink(),
                    post1?.text
                )
                PostShareDialog.Methods.REPOST_YOURSELF -> PlaceFactory.getRepostPlace(
                    accountId1,
                    null,
                    post1
                ).tryOpenWith(requireActivity())
                PostShareDialog.Methods.SEND_MESSAGE -> post1?.let {
                    SendAttachmentsActivity.startForSendAttachments(
                        requireActivity(),
                        accountId1,
                        it
                    )
                }
                PostShareDialog.Methods.REPOST_GROUP -> {
                    val ownerId = PostShareDialog.extractOwnerId(result)
                    PlaceFactory.getRepostPlace(accountId1, abs(ownerId), post1)
                        .tryOpenWith(requireActivity())
                }
            }
        }
    }

    override fun onOwnerClick(ownerId: Long) {
        presenter?.fireOwnerClick(ownerId)
    }

    override fun openChatWith(accountId: Long, messagesOwnerId: Long, peer: Peer) {
        PlaceFactory.getChatPlace(accountId, messagesOwnerId, peer).tryOpenWith(requireActivity())
    }

    override fun onPollOpen(poll: Poll) {
        presenter?.firePollClick(poll)
    }

    override fun onVideoPlay(video: Video) {
        presenter?.fireVideoClick(video)
    }

    override fun onAudioPlay(position: Int, audios: ArrayList<Audio>) {
        presenter?.fireAudioPlayClick(position, audios)
    }

    override fun onForwardMessagesOpen(messages: ArrayList<Message>) {
        presenter?.fireForwardMessagesClick(messages)
    }

    override fun onOpenOwner(ownerId: Long) {
        presenter?.fireOwnerClick(ownerId)
    }

    override fun onGoToMessagesLookup(message: Message) {
        presenter?.fireGoToMessagesLookup(message)
    }

    override fun goToMessagesLookupFWD(accountId: Long, peerId: Long, messageId: Int) {
        PlaceFactory.getMessagesLookupPlace(accountId, peerId, messageId, null)
            .tryOpenWith(requireActivity())
    }

    override fun goWallReplyOpen(accountId: Long, reply: WallReply) {
        PlaceFactory.getCommentsPlace(
            accountId,
            Commented(reply.postId, reply.ownerId, CommentedType.POST, null),
            reply.getObjectId()
        )
            .tryOpenWith(requireActivity())
    }

    override fun onDocPreviewOpen(document: Document) {
        presenter?.fireDocClick(document)
    }

    override fun onPostOpen(post: Post) {
        presenter?.firePostClick(post)
    }

    override fun onLinkOpen(link: Link) {
        presenter?.fireLinkClick(link)
    }

    override fun onUrlOpen(url: String) {
        presenter?.fireUrlClick(url)
    }

    override fun onFaveArticle(article: Article) {
        presenter?.fireFaveArticleClick(article)
    }

    override fun onShareArticle(article: Article) {
        SendAttachmentsActivity.startForSendAttachments(
            requireActivity(),
            Settings.get().accounts().current,
            article
        )
    }

    override fun onWikiPageOpen(page: WikiPage) {
        presenter?.fireWikiPageClick(page)
    }

    override fun onStoryOpen(story: Story) {
        presenter?.fireStoryClick(story)
    }

    override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
        PlaceFactory.getSingleURLPhotoPlace(url, prefix, photo_prefix)
            .tryOpenWith(requireActivity())
    }

    override fun openStory(accountId: Long, story: Story) {
        PlaceFactory.getHistoryVideoPreviewPlace(accountId, ArrayList(setOf(story)), 0)
            .tryOpenWith(requireActivity())
    }

    override fun onAudioPlaylistOpen(playlist: AudioPlaylist) {
        presenter?.fireAudioPlaylistClick(playlist)
    }

    override fun onWallReplyOpen(reply: WallReply) {
        presenter?.fireWallReplyOpen(reply)
    }

    override fun openAudioPlaylist(accountId: Long, playlist: AudioPlaylist) {
        PlaceFactory.getAudiosInAlbumPlace(
            accountId,
            playlist.getOwnerId(),
            playlist.getId(),
            playlist.getAccess_key()
        ).tryOpenWith(requireActivity())
    }

    override fun onPhotosOpen(photos: ArrayList<Photo>, index: Int, refresh: Boolean) {
        presenter?.firePhotoClick(photos, index, refresh)
    }

    override fun openPhotoAlbum(accountId: Long, album: PhotoAlbum) {
        PlaceFactory.getVKPhotosAlbumPlace(accountId, album.ownerId, album.getObjectId(), null)
            .tryOpenWith(requireActivity())
    }

    override fun onPhotoAlbumOpen(album: PhotoAlbum) {
        presenter?.firePhotoAlbumClick(album)
    }

    override fun onMarketAlbumOpen(market_album: MarketAlbum) {
        presenter?.fireMarketAlbumClick(market_album)
    }

    override fun onMarketOpen(market: Market) {
        presenter?.fireMarketClick(market)
    }

    override fun onArtistOpen(artist: AudioArtist) {
        presenter?.fireArtistClick(artist)
    }

    override fun openLink(accountId: Long, link: Link) {
        LinkHelper.openLinkInBrowser(requireActivity(), link.url)
    }

    override fun openUrl(accountId: Long, url: String) {
        PlaceFactory.getExternalLinkPlace(accountId, url)
            .tryOpenWith(requireActivity())
    }

    override fun openWikiPage(accountId: Long, page: WikiPage) {
        page.viewUrl?.let {
            PlaceFactory.getExternalLinkPlace(accountId, it)
                .tryOpenWith(requireActivity())
        }
    }

    override fun toMarketAlbumOpen(accountId: Long, market_album: MarketAlbum) {
        PlaceFactory.getMarketPlace(
            accountId,
            market_album.getOwner_id(),
            market_album.getId(),
            false
        )
            .tryOpenWith(requireActivity())
    }

    override fun toArtistOpen(accountId: Long, artist: AudioArtist) {
        PlaceFactory.getArtistPlace(accountId, artist.getId()).tryOpenWith(requireActivity())
    }

    override fun toMarketOpen(accountId: Long, market: Market) {
        PlaceFactory.getMarketViewPlace(accountId, market).tryOpenWith(requireActivity())
    }

    override fun openSimplePhotoGallery(
        accountId: Long,
        photos: ArrayList<Photo>,
        index: Int,
        needUpdate: Boolean
    ) {
        PlaceFactory.getSimpleGalleryPlace(accountId, photos, index, needUpdate)
            .tryOpenWith(requireActivity())
    }

    override fun openPost(accountId: Long, post: Post) {
        PlaceFactory.getPostPreviewPlace(accountId, post.vkid, post.ownerId, post)
            .tryOpenWith(requireActivity())
    }

    override fun openDocPreview(accountId: Long, document: Document) {
        PlaceFactory.getDocPreviewPlace(accountId, document).tryOpenWith(requireActivity())
    }

    override fun openOwnerWall(accountId: Long, ownerId: Long) {
        PlaceFactory.getOwnerWallPlace(accountId, ownerId, null).tryOpenWith(requireActivity())
    }

    override fun openForwardMessages(accountId: Long, messages: ArrayList<Message>) {
        PlaceFactory.getForwardMessagesPlace(accountId, messages).tryOpenWith(requireActivity())
    }

    override fun playAudioList(accountId: Long, position: Int, apiAudio: ArrayList<Audio>) {
        startForPlayList(requireActivity(), apiAudio, position, false)
        if (!Settings.get().other().isShow_mini_player) PlaceFactory.getPlayerPlace(
            Settings.get().accounts().current
        ).tryOpenWith(requireActivity())
    }

    override fun openVideo(accountId: Long, apiVideo: Video) {
        PlaceFactory.getVideoPreviewPlace(accountId, apiVideo).tryOpenWith(requireActivity())
    }

    override fun openHistoryVideo(accountId: Long, stories: ArrayList<Story>, index: Int) {
        PlaceFactory.getHistoryVideoPreviewPlace(accountId, stories, index)
            .tryOpenWith(requireActivity())
    }

    override fun openPoll(accountId: Long, apiPoll: Poll) {
        PlaceFactory.getPollPlace(accountId, apiPoll)
            .tryOpenWith(requireActivity())
    }

    override fun openComments(accountId: Long, commented: Commented, focusToCommentId: Int?) {
        PlaceFactory.getCommentsPlace(accountId, commented, focusToCommentId)
            .tryOpenWith(requireActivity())
    }

    override fun openSearch(
        accountId: Long,
        @SearchContentType type: Int,
        criteria: BaseSearchCriteria?
    ) {
        PlaceFactory.getSingleTabSearchPlace(accountId, type, criteria)
            .tryOpenWith(requireActivity())
    }

    override fun goToLikes(accountId: Long, type: String?, ownerId: Long, id: Int) {
        PlaceFactory.getLikesCopiesPlace(
            accountId,
            type,
            ownerId,
            id,
            ILikesInteractor.FILTER_LIKES
        )
            .tryOpenWith(requireActivity())
    }

    override fun goToReposts(accountId: Long, type: String?, ownerId: Long, id: Int) {
        PlaceFactory.getLikesCopiesPlace(
            accountId,
            type,
            ownerId,
            id,
            ILikesInteractor.FILTER_COPIES
        )
            .tryOpenWith(requireActivity())
    }

    override fun repostPost(accountId: Long, post: Post) {
        val dialog = PostShareDialog.newInstance(accountId, post)
        dialog.show(parentFragmentManager, "post-sharing")
    }

    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }
}