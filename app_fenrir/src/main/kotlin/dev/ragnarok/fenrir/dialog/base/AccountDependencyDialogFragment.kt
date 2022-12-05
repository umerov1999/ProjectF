package dev.ragnarok.fenrir.dialog.base

import android.Manifest
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.place.PlaceFactory.getArtistPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosInAlbumPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getDocPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getExternalLinkPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getForwardMessagesPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getHistoryVideoPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMarketPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMarketViewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMessagesLookupPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVKPhotosAlbumPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class AccountDependencyDialogFragment : BaseDialogFragment(), OnAttachmentsActionCallback {
    private val mCompositeDisposable = CompositeDisposable()
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }
    protected var accountId = 0
        private set
    open var isSupportAccountHotSwap = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        require(requireArguments().containsKey(Extra.ACCOUNT_ID)) { "Fragments args does not contains Extra.ACCOUNT_ID" }
        accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
        mCompositeDisposable.add(
            Settings.get()
                .accounts()
                .observeChanges()
                .observeOn(provideMainThreadScheduler())
                .subscribe { fireAccountChange(it) })
    }

    private fun fireAccountChange(newAid: Int) {
        val oldAid = accountId
        if (!isSupportAccountHotSwap) {
            if (newAid != oldAid) {
                isInvalidAccountContext = true
                onAccountContextInvalidState()
            } else {
                isInvalidAccountContext = false
            }
            return
        }
        if (newAid == oldAid) return
        beforeAccountChange(oldAid, newAid)
        accountId = newAid
        requireArguments().putInt(Extra.ACCOUNT_ID, newAid)
        afterAccountChange(oldAid, newAid)
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    protected fun appendDisposable(disposable: Disposable) {
        mCompositeDisposable.add(disposable)
    }

    protected open fun afterAccountChange(oldAid: Int, newAid: Int) {}
    protected open fun beforeAccountChange(oldAid: Int, newAid: Int) {}
    override fun onPollOpen(poll: Poll) {
        ///PlaceManager.withContext(getContext())
        //        .toPoll()
        //        .withArguments(PollDialog.buildArgs(getAccountId(), poll, true))
        //       .open();
    }

    override fun onVideoPlay(video: Video) {
        getVideoPreviewPlace(accountId, video).tryOpenWith(requireActivity())
    }

    override fun onAudioPlay(position: Int, audios: ArrayList<Audio>) {
        startForPlayList(requireActivity(), audios, position, false)
        if (!Settings.get().other().isShow_mini_player) getPlayerPlace(
            Settings.get().accounts().current
        ).tryOpenWith(requireActivity())
    }

    override fun onForwardMessagesOpen(messages: ArrayList<Message>) {
        getForwardMessagesPlace(accountId, messages).tryOpenWith(requireActivity())
    }

    override fun onOpenOwner(ownerId: Int) {
        getOwnerWallPlace(accountId, ownerId, null).tryOpenWith(requireActivity())
    }

    override fun onGoToMessagesLookup(message: Message) {
        getMessagesLookupPlace(accountId, message.peerId, message.getObjectId(), null).tryOpenWith(
            requireActivity()
        )
    }

    override fun onDocPreviewOpen(document: Document) {
        getDocPreviewPlace(accountId, document).tryOpenWith(requireActivity())
    }

    override fun onPostOpen(post: Post) {
        getPostPreviewPlace(accountId, post.vkid, post.ownerId, post).tryOpenWith(requireActivity())
    }

    override fun onLinkOpen(link: Link) {
        LinkHelper.openLinkInBrowser(requireActivity(), link.url)
    }

    override fun onUrlOpen(url: String) {
        getExternalLinkPlace(accountId, url).tryOpenWith(requireActivity())
    }

    override fun onWikiPageOpen(page: WikiPage) {
        page.viewUrl?.let { getExternalLinkPlace(accountId, it).tryOpenWith(requireActivity()) }
    }

    override fun onPhotosOpen(photos: ArrayList<Photo>, index: Int, refresh: Boolean) {
        getSimpleGalleryPlace(accountId, photos, index, refresh).tryOpenWith(requireActivity())
    }

    override fun onStoryOpen(story: Story) {
        getHistoryVideoPreviewPlace(accountId, ArrayList(setOf(story)), 0).tryOpenWith(
            requireActivity()
        )
    }

    override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
        getSingleURLPhotoPlace(url, prefix, photo_prefix).tryOpenWith(requireActivity())
    }

    override fun onAudioPlaylistOpen(playlist: AudioPlaylist) {
        getAudiosInAlbumPlace(
            accountId,
            playlist.getOwnerId(),
            playlist.getId(),
            playlist.getAccess_key()
        ).tryOpenWith(requireActivity())
    }

    override fun onWallReplyOpen(reply: WallReply) {
        getCommentsPlace(
            accountId,
            Commented(reply.postId, reply.ownerId, CommentedType.POST, null),
            reply.getObjectId()
        )
            .tryOpenWith(requireActivity())
    }

    override fun onPhotoAlbumOpen(album: PhotoAlbum) {
        getVKPhotosAlbumPlace(
            accountId,
            album.ownerId,
            album.getObjectId(),
            null
        ).tryOpenWith(requireActivity())
    }

    override fun onMarketAlbumOpen(market_album: MarketAlbum) {
        getMarketPlace(
            accountId,
            market_album.getOwner_id(),
            market_album.getId(),
            false
        ).tryOpenWith(
            requireActivity()
        )
    }

    override fun onMarketOpen(market: Market) {
        getMarketViewPlace(accountId, market).tryOpenWith(requireActivity())
    }

    override fun onArtistOpen(artist: AudioArtist) {
        getArtistPlace(accountId, artist.getId()).tryOpenWith(requireActivity())
    }

    override fun onFaveArticle(article: Article) {}
    override fun onShareArticle(article: Article) {
        startForSendAttachments(requireActivity(), Settings.get().accounts().current, article)
    }

    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }

    protected open fun onAccountContextInvalidState() {
        if (isAdded && isResumed) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isInvalidAccountContext) {
            parentFragmentManager.popBackStack()
        }
    }

    open var isInvalidAccountContext: Boolean
        get() = requireArguments().getBoolean(ARGUMENT_INVALID_ACCOUNT_CONTEXT)
        protected set(invalidAccountContext) {
            requireArguments().putBoolean(ARGUMENT_INVALID_ACCOUNT_CONTEXT, invalidAccountContext)
        }

    companion object {
        private const val ARGUMENT_INVALID_ACCOUNT_CONTEXT = "invalid_account_context"
    }
}