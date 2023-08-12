package dev.ragnarok.fenrir.fragment.videos.videopreview

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.link.LinkHelper.openLinkInBrowser
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory.withSpans
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Text
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.menu.Item
import dev.ragnarok.fenrir.model.menu.Section
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getLikesCopiesPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVkInternalPlayerPlace
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.settings.AppPrefs
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTime
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadVideo
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.shareLink
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.natives.animation.AnimatedShapeableImageView
import dev.ragnarok.fenrir.view.natives.animation.AspectRatioAnimatedShapeableImageView

class VideoPreviewFragment : BaseMvpFragment<VideoPreviewPresenter, IVideoPreviewView>(),
    View.OnClickListener, View.OnLongClickListener, IVideoPreviewView, MenuProvider {
    private val ownerLinkAdapter: OwnerLinkSpanFactory.ActionListener =
        object : LinkActionAdapter() {
            override fun onOwnerClick(ownerId: Long) {
                presenter?.fireOwnerClick(
                    ownerId
                )
            }
        }
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }
    private var mRootView: View? = null
    private var likeButton: CircleCounterButton? = null
    private var commentsButton: CircleCounterButton? = null
    private var mTitleText: TextView? = null
    private var mSubtitleText: TextView? = null
    private var mPreviewImage: AspectRatioAnimatedShapeableImageView? = null
    private var mOwnerAvatar: ImageView? = null
    private var mOwnerText: TextView? = null
    private var mUploadDate: TextView? = null
    private var mAddedDate: TextView? = null
    private var mTransformation: Transformation? = null
    private var mOwnerGroup: ViewGroup? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: android.view.Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_video_preview, menu)
    }

    override fun onPrepareMenu(menu: android.view.Menu) {
        super.onPrepareMenu(menu)
        val view = OptionView()
        presenter?.fireOptionViewCreated(
            view
        )
        menu.findItem(R.id.action_add_to_my_videos).isVisible = view.pCanAdd
        menu.findItem(R.id.action_delete_from_my_videos).isVisible = view.pIsMy
        menu.findItem(R.id.action_edit).isVisible = view.pIsMy
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_add_to_my_videos -> {
                presenter?.fireAddToMyClick()
                true
            }

            R.id.action_copy_url -> {
                presenter?.fireCopyUrlClick(
                    requireActivity()
                )
                true
            }

            R.id.action_delete_from_my_videos -> {
                presenter?.fireDeleteMyClick()
                true
            }

            R.id.action_edit -> {
                presenter?.fireEditVideo(
                    requireActivity()
                )
                true
            }

            else -> false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mRootView = inflater.inflate(R.layout.fragment_video, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(mRootView?.findViewById(R.id.toolbar))
        mPreviewImage = mRootView?.findViewById(R.id.fragment_video_preview_image)
        likeButton = mRootView?.findViewById(R.id.like_button)
        val shareButton: CircleCounterButton? = mRootView?.findViewById(R.id.share_button)
        commentsButton = mRootView?.findViewById(R.id.comments_button)
        commentsButton?.setOnClickListener(this)
        shareButton?.setOnClickListener(this)
        likeButton?.setOnClickListener(this)
        likeButton?.setOnLongClickListener(this)
        mTitleText = mRootView?.findViewById(R.id.fragment_video_title)
        mSubtitleText = mRootView?.findViewById(R.id.fragment_video_subtitle)
        mOwnerAvatar = mRootView?.findViewById(R.id.item_owner_avatar)
        mOwnerText = mRootView?.findViewById(R.id.item_owner_name)
        mUploadDate = mRootView?.findViewById(R.id.item_upload_time)
        mAddedDate = mRootView?.findViewById(R.id.item_added_time)
        mOwnerGroup = mRootView?.findViewById(R.id.item_owner)
        mOwnerGroup?.setOnClickListener {
            presenter?.fireOpenOwnerClicked()
        }
        mTransformation = CurrentTheme.createTransformationForAvatar()
        if (Settings.get().other().isDo_auto_play_video) {
            mRootView?.findViewById<View>(R.id.cover_cardview)?.setOnClickListener {
                presenter?.fireAutoPlayClick()
            }
            mRootView?.findViewById<View>(R.id.cover_cardview)?.setOnLongClickListener {
                presenter?.firePlayClick()
                true
            }
        } else {
            mRootView?.findViewById<View>(R.id.cover_cardview)?.setOnClickListener {
                presenter?.firePlayClick()
            }
            mRootView?.findViewById<View>(R.id.cover_cardview)?.setOnLongClickListener {
                presenter?.fireAutoPlayClick()
                true
            }
        }
        mRootView?.findViewById<View>(R.id.try_again_button)?.setOnClickListener {
            presenter?.fireTryAgainClick()
        }
        return mRootView
    }

    private fun playWithExternalSoftware(url: String) {
        if (url.isEmpty()) {
            createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                .showToastError(R.string.error_video_playback_is_not_possible)
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideoPreviewPresenter> {
        var documentAccessKey: String? = null
        if (requireArguments().containsKey(Extra.ACCESS_KEY)) {
            documentAccessKey = requireArguments().getString(Extra.ACCESS_KEY)
        }
        val finalDocumentAccessKey = documentAccessKey
        return object : IPresenterFactory<VideoPreviewPresenter> {
            override fun create(): VideoPreviewPresenter {
                return VideoPreviewPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getInt(EXTRA_VIDEO_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    finalDocumentAccessKey,
                    requireArguments().getParcelableCompat(Extra.VIDEO),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayLoading() {
        mRootView?.findViewById<View>(R.id.content)?.visibility = View.GONE
        mRootView?.findViewById<View>(R.id.loading_root)?.visibility =
            View.VISIBLE
        mRootView?.findViewById<View>(R.id.progressBar)?.visibility =
            View.VISIBLE
        mRootView?.findViewById<View>(R.id.post_loading_text)?.visibility =
            View.VISIBLE
        mRootView?.findViewById<View>(R.id.try_again_button)?.visibility = View.GONE
    }

    override fun displayLoadingError() {
        mRootView?.findViewById<View>(R.id.content)?.visibility = View.GONE
        mRootView?.findViewById<View>(R.id.loading_root)?.visibility = View.VISIBLE
        mRootView?.findViewById<View>(R.id.progressBar)?.visibility =
            View.GONE
        mRootView?.findViewById<View>(R.id.post_loading_text)?.visibility =
            View.GONE
        mRootView?.findViewById<View>(R.id.try_again_button)?.visibility =
            View.VISIBLE
    }

    override fun displayVideoInfo(video: Video) {
        if (mRootView != null) {
            mRootView?.findViewById<View>(R.id.content)?.visibility = View.VISIBLE
            mRootView?.findViewById<View>(R.id.loading_root)?.visibility =
                View.GONE
        }
        if (video.date != 0L && mUploadDate != null) {
            mUploadDate?.visibility = View.VISIBLE
            mUploadDate?.text = requireActivity().getString(
                R.string.uploaded_video,
                getDateFromUnixTime(requireActivity(), video.date)
            )
        }
        if (video.addingDate != 0L && mAddedDate != null) {
            mAddedDate?.visibility = View.VISIBLE
            mAddedDate?.text = requireActivity().getString(
                R.string.added_video,
                getDateFromUnixTime(requireActivity(), video.addingDate)
            )
        }
        safelySetText(mTitleText, video.title)
        if (mSubtitleText != null) {
            val subtitle = withSpans(
                video.description,
                owners = true,
                topics = false,
                listener = ownerLinkAdapter
            )
            mSubtitleText?.setText(subtitle, TextView.BufferType.SPANNABLE)
            mSubtitleText?.movementMethod = LinkMovementMethod.getInstance()
        }
        val imageUrl = video.image
        val trailerUrl = video.trailer

        mPreviewImage?.let { pp ->
            if (Settings.get().other().isAutoplay_gif && trailerUrl.nonNullNoEmpty()) {
                with().cancelRequest(pp)
                pp.setDecoderCallback(object :
                    AnimatedShapeableImageView.OnDecoderInit {
                    override fun onLoaded(success: Boolean) {
                        if (!success) {
                            if (imageUrl.nonNullNoEmpty()) {
                                with()
                                    .load(imageUrl)
                                    .placeholder(R.drawable.background_gray)
                                    .tag(Constants.PICASSO_TAG)
                                    .into(pp)
                            } else {
                                with().cancelRequest(pp)
                            }
                        }
                    }
                })
                pp.fromNet(
                    (video.ownerId.toString() + "_" + video.id.toString()),
                    video.trailer,
                    Utils.createOkHttp(Constants.GIF_TIMEOUT, true)
                )
            } else if (imageUrl.nonNullNoEmpty()) {
                with()
                    .load(imageUrl)
                    .placeholder(R.drawable.background_gray)
                    .into(pp)
            }
        }
    }

    override fun displayLikes(count: Int, userLikes: Boolean) {
        likeButton?.setIcon(if (userLikes) R.drawable.heart_filled else R.drawable.heart)
        likeButton?.count = count
        likeButton?.isActive = userLikes
    }

    override fun setCommentButtonVisible(visible: Boolean) {
        commentsButton?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun displayCommentCount(count: Int) {
        commentsButton?.count = count
    }

    override fun showSuccessToast() {
        createCustomToast(requireActivity()).showToastSuccessBottom(
            R.string.success,
            Toast.LENGTH_SHORT
        )
    }

    override fun showOwnerWall(accountId: Long, ownerId: Long) {
        getOwnerWallPlace(accountId, ownerId, null).tryOpenWith(requireActivity())
    }

    override fun showSubtitle(subtitle: String?) {
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.subtitle = subtitle
        }
    }

    override fun showComments(accountId: Long, commented: Commented) {
        getCommentsPlace(accountId, commented, null).tryOpenWith(requireActivity())
    }

    override fun displayShareDialog(accountId: Long, video: Video, canPostToMyWall: Boolean) {
        val items: Array<String> = if (canPostToMyWall) {
            if (!video.private) {
                arrayOf(
                    getString(R.string.share_link),
                    getString(R.string.repost_send_message),
                    getString(R.string.repost_to_wall)
                )
            } else {
                arrayOf(getString(R.string.repost_send_message), getString(R.string.repost_to_wall))
            }
        } else {
            if (!video.private) {
                arrayOf(getString(R.string.share_link), getString(R.string.repost_send_message))
            } else {
                arrayOf(getString(R.string.repost_send_message))
            }
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items) { _: DialogInterface?, i: Int ->
                if (video.private) {
                    when (i) {
                        0 -> startForSendAttachments(requireActivity(), accountId, video)
                        1 -> goToPostCreation(
                            requireActivity(),
                            accountId,
                            accountId,
                            EditingPostType.TEMP,
                            listOf(video)
                        )
                    }
                } else {
                    when (i) {
                        0 -> shareLink(
                            requireActivity(),
                            "https://vk.com/video" + video.ownerId + "_" + video.id,
                            video.title
                        )

                        1 -> startForSendAttachments(requireActivity(), accountId, video)
                        2 -> goToPostCreation(
                            requireActivity(),
                            accountId,
                            accountId,
                            EditingPostType.TEMP,
                            listOf(video)
                        )
                    }
                }
            }
            .setCancelable(true)
            .setTitle(R.string.repost_title)
            .show()
    }

    private fun createDirectVkPlayItems(
        video: Video,
        section: Section,
        isDownload: Boolean
    ): List<Item> {
        val items: MutableList<Item> = ArrayList()
        if (video.hls.nonNullNoEmpty() && !isDownload) {
            items.add(
                Item(Menu.HLS, Text(R.string.play_hls))
                    .setIcon(R.drawable.video)
                    .setColor(Color.parseColor("#ff0000"))
                    .setSection(section)
            )
        }
        if (video.live.nonNullNoEmpty() && !isDownload) {
            items.add(
                Item(Menu.LIVE, Text(R.string.player_live))
                    .setSection(section)
                    .setColor(Color.parseColor("#ff0000"))
                    .setIcon(R.drawable.video)
            )
        }
        if (video.mp4link240.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_240, Text(R.string.play_240))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        if (video.mp4link360.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_360, Text(R.string.play_360))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        if (video.mp4link480.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_480, Text(R.string.play_480))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        if (video.mp4link720.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_720, Text(R.string.play_720))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        if (video.mp4link1080.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_1080, Text(R.string.play_1080))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        if (video.mp4link1440.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_1440, Text(R.string.play_1440))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        if (video.mp4link2160.nonNullNoEmpty()) {
            items.add(
                Item(Menu.P_2160, Text(R.string.play_2160))
                    .setIcon(R.drawable.video)
                    .setSection(section)
            )
        }
        return items
    }

    override fun showVideoPlayMenu(accountId: Long, video: Video) {
        val items: MutableList<Item> =
            ArrayList(createDirectVkPlayItems(video, SECTION_PLAY, false))
        val external = video.externalLink
        if (external.nonNullNoEmpty()) {
            if (external.contains("youtube")) {
                val hasReVanced = AppPrefs.isReVancedYoutubeInstalled(requireActivity())
                if (hasReVanced) {
                    items.add(
                        Item(Menu.YOUTUBE_VANCED, Text(R.string.title_play_in_youtube_vanced))
                            .setIcon(R.drawable.ic_play_youtube)
                            .setSection(SECTION_PLAY)
                    )
                }
                items.add(
                    Item(Menu.NEW_PIPE, Text(R.string.title_play_in_newpipe))
                        .setIcon(R.drawable.ic_new_pipe)
                        .setSection(SECTION_PLAY)
                )
                if (!hasReVanced && AppPrefs.isYoutubeInstalled(requireActivity())) {
                    items.add(
                        Item(Menu.YOUTUBE, Text(R.string.title_play_in_youtube))
                            .setIcon(R.drawable.ic_play_youtube)
                            .setSection(SECTION_PLAY)
                    )
                }
            } else if (external.contains("coub") && AppPrefs.isCoubInstalled(requireActivity())) {
                items.add(
                    Item(Menu.COUB, Text(R.string.title_play_in_coub))
                        .setIcon(R.drawable.ic_play_coub)
                        .setSection(SECTION_PLAY)
                )
            }
            items.add(
                Item(Menu.PLAY_ANOTHER_SOFT, Text(R.string.title_play_in_another_software))
                    .setSection(SECTION_OTHER)
                    .setIcon(R.drawable.ic_external)
            )
        }
        if (firstNonEmptyString(
                video.mp4link240,
                video.mp4link360,
                video.mp4link480,
                video.mp4link720,
                video.mp4link1080,
                video.mp4link1440,
                video.mp4link2160,
                video.live,
                video.hls
            )
                .nonNullNoEmpty()
        ) {

            // потом выбираем качество
            items.add(
                Item(Menu.P_EXTERNAL_PLAYER, Text(R.string.play_in_external_player))
                    .setIcon(R.drawable.ic_external)
                    .setSection(SECTION_OTHER)
            )
        }
        if (video.player.nonNullNoEmpty()) {
            items.add(
                Item(Menu.PLAY_BROWSER, Text(R.string.title_play_in_browser))
                    .setIcon(R.drawable.ic_external)
                    .setSection(SECTION_OTHER)
            )
        }
        if (external.nonNullNoEmpty()) {
            items.add(
                Item(Menu.COPY_LINK, Text(R.string.target_url))
                    .setIcon(R.drawable.content_copy)
                    .setSection(SECTION_OTHER)
            )
        }
        items.add(
            Item(
                Menu.ADD_TO_FAVE,
                if (video.isFavorite) Text(R.string.remove_from_bookmarks) else Text(R.string.add_to_bookmarks)
            )
                .setIcon(R.drawable.star)
                .setSection(SECTION_OTHER)
        )
        if (firstNonEmptyString(
                video.mp4link240,
                video.mp4link360,
                video.mp4link480,
                video.mp4link720,
                video.mp4link1080,
                video.mp4link1440,
                video.mp4link2160
            )
                .nonNullNoEmpty()
        ) {
            items.add(
                Item(Menu.DOWNLOAD, Text(R.string.download))
                    .setIcon(R.drawable.save)
                    .setSection(SECTION_OTHER)
            )
        }
        val adapter = MenuAdapter(requireActivity(), items, false)
        MaterialAlertDialogBuilder(requireActivity())
            .setAdapter(adapter) { _: DialogInterface?, which: Int ->
                onPlayMenuItemClick(
                    video,
                    items[which]
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun doAutoPlayVideo(accountId: Long, video: Video) {
        if (!video.live.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_LIVE)
        } else if (!video.hls.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_HLS)
        } else if (!video.mp4link2160.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_2160)
        } else if (!video.mp4link1440.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_1440)
        } else if (!video.mp4link1080.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_1080)
        } else if (!video.mp4link720.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_720)
        } else if (!video.mp4link480.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_480)
        } else if (!video.mp4link360.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_360)
        } else if (!video.mp4link240.isNullOrEmpty()) {
            openInternal(video, InternalVideoSize.SIZE_240)
        } else if (video.externalLink.nonNullNoEmpty()) {
            if (video.externalLink?.contains("youtube") == true) {
                when {
                    AppPrefs.isReVancedYoutubeInstalled(requireActivity()) -> {
                        playWithYoutubeReVanced(video)
                    }

                    AppPrefs.isNewPipeInstalled(requireActivity()) -> {
                        playWithNewPipe(video)
                    }

                    AppPrefs.isYoutubeInstalled(requireActivity()) -> {
                        playWithYoutube(video)
                    }

                    else -> {
                        playWithExternalSoftware(video.externalLink ?: return)
                    }
                }
            } else if (video.externalLink?.contains("coub") == true && AppPrefs.isCoubInstalled(
                    requireActivity()
                )
            ) {
                playWithCoub(video)
            } else {
                playWithExternalSoftware(video.externalLink ?: return)
            }
        } else if (video.player.nonNullNoEmpty()) {
            playWithExternalSoftware(video.player ?: return)
        } else {
            createCustomToast(requireActivity()).showToastError(R.string.video_not_have_link)
        }
    }

    override fun goToLikes(accountId: Long, type: String, ownerId: Long, id: Int) {
        getLikesCopiesPlace(accountId, type, ownerId, id, ILikesInteractor.FILTER_LIKES)
            .tryOpenWith(requireActivity())
    }

    override fun displayOwner(owner: Owner) {
        mOwnerGroup?.visibility = View.VISIBLE
        if (mOwnerAvatar != null) {
            mOwnerAvatar?.visibility = View.VISIBLE
            displayAvatar(
                mOwnerAvatar,
                mTransformation,
                owner.maxSquareAvatar,
                Constants.PICASSO_TAG
            )
        }
        if (mOwnerText != null) {
            mOwnerText?.visibility = View.VISIBLE
            mOwnerText?.text = owner.fullName
        }
    }

    private fun onPlayMenuItemClick(video: Video, item: Item) {
        when (item.key) {
            Menu.P_240 -> openInternal(video, InternalVideoSize.SIZE_240)
            Menu.P_360 -> openInternal(video, InternalVideoSize.SIZE_360)
            Menu.P_480 -> openInternal(video, InternalVideoSize.SIZE_480)
            Menu.P_720 -> openInternal(video, InternalVideoSize.SIZE_720)
            Menu.P_1080 -> openInternal(video, InternalVideoSize.SIZE_1080)
            Menu.P_1440 -> openInternal(video, InternalVideoSize.SIZE_1440)
            Menu.P_2160 -> openInternal(video, InternalVideoSize.SIZE_2160)
            Menu.LIVE -> openInternal(video, InternalVideoSize.SIZE_LIVE)
            Menu.HLS -> openInternal(video, InternalVideoSize.SIZE_HLS)
            Menu.P_EXTERNAL_PLAYER -> showPlayExternalPlayerMenu(video)
            Menu.NEW_PIPE -> if (AppPrefs.isNewPipeInstalled(requireActivity())) {
                playWithNewPipe(video)
            } else {
                openLinkInBrowser(
                    requireActivity(),
                    "https://github.com/TeamNewPipe/NewPipe/releases"
                )
            }

            Menu.YOUTUBE -> playWithYoutube(video)
            Menu.YOUTUBE_VANCED -> playWithYoutubeReVanced(video)
            Menu.COUB -> playWithCoub(video)
            Menu.PLAY_ANOTHER_SOFT -> video.externalLink?.let { playWithExternalSoftware(it) }
            Menu.PLAY_BROWSER -> video.player?.let { playWithExternalSoftware(it) }
            Menu.DOWNLOAD -> if (!hasReadWriteStoragePermission(requireActivity())) {
                requestWritePermission.launch()
            } else {
                showDownloadPlayerMenu(video)
            }

            Menu.ADD_TO_FAVE -> presenter?.fireFaveVideo()
            Menu.COPY_LINK -> {
                val clipboard =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip = ClipData.newPlainText("response", video.externalLink)
                clipboard?.setPrimaryClip(clip)
                createCustomToast(context).showToast(R.string.copied)
            }
        }
    }

    private fun showPlayExternalPlayerMenu(video: Video) {
        val section = Section(Text(R.string.title_select_resolution))
        val items = createDirectVkPlayItems(video, section, false)
        val adapter = MenuAdapter(requireActivity(), items, false)
        MaterialAlertDialogBuilder(requireActivity())
            .setAdapter(adapter) { _: DialogInterface?, which: Int ->
                val item = items[which]
                when (item.key) {
                    Menu.P_240 -> video.mp4link240?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.P_360 -> video.mp4link360?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.P_480 -> video.mp4link480?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.P_720 -> video.mp4link720?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.P_1080 -> video.mp4link1080?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.P_1440 -> video.mp4link1440?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.P_2160 -> video.mp4link2160?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.LIVE -> video.live?.let { playDirectVkLinkInExternalPlayer(it) }
                    Menu.HLS -> video.hls?.let { playDirectVkLinkInExternalPlayer(it) }
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun showDownloadPlayerMenu(video: Video) {
        val section = Section(Text(R.string.download))
        val items = createDirectVkPlayItems(video, section, true)
        val adapter = MenuAdapter(requireActivity(), items, false)
        MaterialAlertDialogBuilder(requireActivity())
            .setAdapter(adapter) { _: DialogInterface?, which: Int ->
                val item = items[which]
                when (item.key) {
                    Menu.P_240 -> video.mp4link240?.let {
                        doDownloadVideo(
                            requireActivity(), video,
                            it, "240"
                        )
                    }

                    Menu.P_360 -> video.mp4link360?.let {
                        doDownloadVideo(
                            requireActivity(), video,
                            it, "360"
                        )
                    }

                    Menu.P_480 -> video.mp4link480?.let {
                        doDownloadVideo(
                            requireActivity(), video,
                            it, "480"
                        )
                    }

                    Menu.P_720 -> video.mp4link720?.let {
                        doDownloadVideo(
                            requireActivity(), video,
                            it, "720"
                        )
                    }

                    Menu.P_1080 -> video.mp4link1080?.let {
                        doDownloadVideo(
                            requireActivity(),
                            video,
                            it,
                            "1080"
                        )
                    }

                    Menu.P_1440 -> video.mp4link1440?.let {
                        doDownloadVideo(
                            requireActivity(),
                            video,
                            it,
                            "2K"
                        )
                    }

                    Menu.P_2160 -> video.mp4link2160?.let {
                        doDownloadVideo(
                            requireActivity(),
                            video,
                            it,
                            "4K"
                        )
                    }
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun playDirectVkLinkInExternalPlayer(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(url), "video/mp4")
        startActivity(intent)
    }

    private fun openInternal(video: Video, @InternalVideoSize size: Int) {
        getVkInternalPlayerPlace(video, size, false).tryOpenWith(requireActivity())
    }

    private fun playWithCoub(video: Video) {
        val outerLink = video.externalLink
        val intent = Intent()
        intent.data = Uri.parse(outerLink)
        intent.action = Intent.ACTION_VIEW
        intent.component = ComponentName("com.coub.android", "com.coub.android.ui.ViewCoubActivity")
        startActivity(intent)
    }

    private fun playWithNewPipe(video: Video) {
        val outerLink = video.externalLink
        val intent = Intent()
        intent.data = Uri.parse(outerLink)
        intent.action = Intent.ACTION_VIEW
        intent.component = ComponentName("org.schabi.newpipe", "org.schabi.newpipe.RouterActivity")
        startActivity(intent)
    }

    private fun playWithYoutube(video: Video) {
        val outerLink = video.externalLink
        val intent = Intent()
        intent.data = Uri.parse(outerLink)
        intent.action = Intent.ACTION_VIEW
        intent.component = ComponentName(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
        )
        startActivity(intent)
    }

    private fun playWithYoutubeReVanced(video: Video) {
        val outerLink = video.externalLink
        val intent = Intent()
        intent.data = Uri.parse(outerLink)
        intent.action = Intent.ACTION_VIEW
        intent.component = ComponentName(
            AppPrefs.revanced?.first.orEmpty(),
            AppPrefs.revanced?.second.orEmpty()
        )
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.video)
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.like_button -> {
                presenter?.fireLikeClick()
            }

            R.id.comments_button -> {
                presenter?.fireCommentsClick()
            }

            R.id.share_button -> {
                presenter?.fireShareClick()
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (v.id == R.id.like_button) {
            presenter?.fireLikeLongClick()
            return true
        }
        return false
    }

    private class OptionView : IVideoPreviewView.IOptionView {
        var pCanAdd = false
        var pIsMy = false
        override fun setCanAdd(can: Boolean) {
            pCanAdd = can
        }

        override fun setIsMy(my: Boolean) {
            pIsMy = my
        }
    }

    private object Menu {
        const val P_240 = 240
        const val P_360 = 360
        const val P_480 = 480
        const val P_720 = 720
        const val P_1080 = 1080
        const val P_1440 = 1440
        const val P_2160 = 2160
        const val HLS = -1
        const val LIVE = -2
        const val P_EXTERNAL_PLAYER = -3
        const val YOUTUBE = -4
        const val YOUTUBE_VANCED = -5
        const val NEW_PIPE = -6
        const val COUB = -7
        const val PLAY_ANOTHER_SOFT = -8
        const val PLAY_BROWSER = -9
        const val DOWNLOAD = -10
        const val COPY_LINK = -11
        const val ADD_TO_FAVE = -12
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"
        private val SECTION_PLAY = Section(Text(R.string.section_play_title))
        private val SECTION_OTHER = Section(Text(R.string.other))
        fun buildArgs(
            accountId: Long,
            ownerId: Long,
            videoId: Int,
            accessKey: String?,
            video: Video?
        ): Bundle {
            val bundle = Bundle()
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            bundle.putLong(Extra.OWNER_ID, ownerId)
            bundle.putInt(EXTRA_VIDEO_ID, videoId)
            if (!accessKey.isNullOrEmpty()) {
                bundle.putString(Extra.ACCESS_KEY, accessKey)
            }
            if (video != null) {
                bundle.putParcelable(Extra.VIDEO, video)
            }
            return bundle
        }

        fun newInstance(args: Bundle?): VideoPreviewFragment {
            val fragment = VideoPreviewFragment()
            fragment.arguments = args
            return fragment
        }
    }
}