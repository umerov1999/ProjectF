package dev.ragnarok.fenrir.fragment.comments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.fragment.attachments.commentcreate.CommentCreateFragment
import dev.ragnarok.fenrir.fragment.attachments.commentedit.CommentEditFragment
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.comments.CommentsAdapter.OnCommentActionListener
import dev.ragnarok.fenrir.fragment.comments.ICommentsView.ICommentContextView
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.menu.options.CommentsOption
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentCreatePlace
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunityAddBanPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getEditCommentPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.MessagesReplyItemCallback
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.spots.SpotsDialog
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.CommentsInputViewController
import dev.ragnarok.fenrir.view.LoadMoreFooterHelperComment
import dev.ragnarok.fenrir.view.LoadMoreFooterHelperComment.Companion.createFrom
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.OnStickerClickedListener
import dev.ragnarok.fenrir.view.emoji.StickersKeyWordsAdapter

class CommentsFragment : PlaceSupportMvpFragment<CommentsPresenter, ICommentsView>(),
    ICommentsView, OnStickerClickedListener, CommentsInputViewController.OnInputActionCallback,
    OnCommentActionListener, EmojiconTextView.OnHashTagClickListener, BackPressCallback,
    MenuProvider {
    private var mInputController: CommentsInputViewController? = null
    private var mRecyclerView: RecyclerView? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mReplyView: ViewGroup? = null
    private var mReplyText: TextView? = null
    private var upHelper: LoadMoreFooterHelperComment? = null
    private var downhelper: LoadMoreFooterHelperComment? = null
    private var mAdapter: CommentsAdapter? = null
    private var mCenterProgressBar: CircularProgressIndicator? = null
    private var mEmptyView: View? = null
    private var mAuthorAvatar: ImageView? = null
    private var mDeepLookingProgressDialog: AlertDialog? = null
    private var stickersKeywordsView: RecyclerView? = null
    private var stickersAdapter: StickersKeyWordsAdapter? = null
    private var mCanSendCommentAsAdmin = false
    private var mTopicPollAvailable = false
    private var mGotoSourceAvailable = false

    @StringRes
    private var mGotoSourceText: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        parentFragmentManager.setFragmentResultListener(
            CommentCreateFragment.REQUEST_CREATE_COMMENT,
            this
        ) { _: String?, result: Bundle ->
            val body = result.getString(Extra.BODY)
            lazyPresenter {
                fireEditBodyResult(body)
            }
        }

        parentFragmentManager.setFragmentResultListener(
            CommentEditFragment.REQUEST_COMMENT_EDIT,
            this
        ) { _: String?, result: Bundle ->
            val comment1: Comment? = result.getParcelableCompat(
                Extra.COMMENT
            )
            if (comment1 != null) {
                lazyPresenter {
                    fireCommentEditResult(comment1)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_comments, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        stickersKeywordsView = root.findViewById(R.id.stickers)
        stickersAdapter = StickersKeyWordsAdapter(requireActivity(), emptyList())
        stickersAdapter?.setStickerClickedListener(object : OnStickerClickedListener {
            override fun onStickerClick(sticker: Sticker) {
                presenter?.let {
                    it.fireStickerClick(sticker)
                    it.resetDraftMessage()
                }
            }
        })
        stickersKeywordsView?.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        stickersKeywordsView?.adapter = stickersAdapter
        stickersKeywordsView?.visibility = View.GONE
        mAuthorAvatar = root.findViewById(R.id.author_avatar)
        mInputController = CommentsInputViewController(requireActivity(), root, this)
        mInputController?.setOnSickerClickListener(this)
        mInputController?.setSendOnEnter(Settings.get().main().isSendByEnter)
        mLinearLayoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, true)
        mRecyclerView = root.findViewById(R.id.list)
        mRecyclerView?.layoutManager = mLinearLayoutManager
        mReplyView = root.findViewById(R.id.fragment_comments_reply_container)
        mReplyText = root.findViewById(R.id.fragment_comments_reply_user)
        root.findViewById<View>(R.id.fragment_comments_delete_reply)
            .setOnClickListener {
                presenter?.fireReplyCancelClick()
            }
        val loadUpView = inflater.inflate(R.layout.footer_load_more_comment, mRecyclerView, false)
        upHelper = createFrom(loadUpView, object : LoadMoreFooterHelperComment.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireUpLoadMoreClick()
            }
        })
        upHelper?.setEndOfListText(" ")
        val loadDownView = inflater.inflate(R.layout.footer_load_more_comment, mRecyclerView, false)
        downhelper = createFrom(loadDownView, object : LoadMoreFooterHelperComment.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireDownLoadMoreClick()
            }
        })
        downhelper?.setEndOfListTextRes(R.string.place_for_your_comment)
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToTop()
            }
        })
        mAdapter = CommentsAdapter(requireActivity(), mutableListOf(), this)
        mAdapter?.addHeader(loadDownView)
        mAdapter?.addFooter(loadUpView)
        mAdapter?.setListener(this)
        mAdapter?.setOnHashTagClickListener(this)
        mRecyclerView?.adapter = mAdapter
        mCenterProgressBar = root.findViewById(R.id.progress_bar)
        mEmptyView = root.findViewById(R.id.empty_text)
        ItemTouchHelper(MessagesReplyItemCallback { o: Int ->
            presenter?.fireReplyToCommentClick(mAdapter?.getItemRawPosition(o) ?: 0)
        }).attachToRecyclerView(mRecyclerView)
        return root
    }

    override fun onSendLongClick(): Boolean {
        if (mCanSendCommentAsAdmin) {
            presenter?.fireSendLongClick()
            return true
        }
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommentsPresenter> {
        return object : IPresenterFactory<CommentsPresenter> {
            override fun create(): CommentsPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val commented: Commented = requireArguments().getParcelableCompat(Extra.COMMENTED)!!
                var focusTo: Int? = null
                var ThreadComment: Int? = null
                if (requireArguments().containsKey(EXTRA_AT_COMMENT_OBJECT)) {
                    focusTo = requireArguments().getInt(EXTRA_AT_COMMENT_OBJECT)
                    requireArguments().remove(EXTRA_AT_COMMENT_OBJECT)
                }
                if (requireArguments().containsKey(EXTRA_AT_COMMENT_THREAD)) {
                    ThreadComment = requireArguments().getInt(EXTRA_AT_COMMENT_THREAD)
                    requireArguments().remove(EXTRA_AT_COMMENT_THREAD)
                }
                return CommentsPresenter(
                    accountId,
                    commented,
                    focusTo,
                    ThreadComment,
                    saveInstanceState
                )
            }
        }
    }

    override fun displayData(data: MutableList<Comment>) {
        mAdapter?.setItems(data)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun setupLoadUpHeader(state: Int) {
        upHelper?.switchToState(state)
    }

    override fun setupLoadDownFooter(state: Int) {
        downhelper?.switchToState(state)
    }

    override fun notifyDataAddedToTop(count: Int) {
        val startSize = mAdapter?.realItemCount ?: 0
        mAdapter?.notifyItemRangeInserted(startSize + (mAdapter?.headersCount ?: 0), count)
    }

    override fun notifyDataAddedToBottom(count: Int) {
        mAdapter?.notifyItemRemoved(0)
        mAdapter?.notifyItemRangeInserted(0, count + 1)
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index + (mAdapter?.headersCount ?: 0))
    }

    override fun moveFocusTo(index: Int, smooth: Boolean) {
        if (mAdapter == null) {
            return
        }
        val adapterPosition = index + (mAdapter?.headersCount ?: 0)
        if (smooth) {
            mRecyclerView?.smoothScrollToPosition(adapterPosition)
        } else {
            mLinearLayoutManager?.scrollToPosition(adapterPosition)
        }
    }

    override fun displayBody(body: String?) {
        mInputController?.setTextQuietly(body)
        presenter?.fireTextEdited(
            body
        )
    }

    override fun displayAttachmentsCount(count: Int) {
        mInputController?.setAttachmentsCount(count)
    }

    override fun setButtonSendAvailable(available: Boolean) {
        mInputController?.setCanSendNormalMessage(available)
    }

    override fun openAttachmentsManager(
        accountId: Long,
        draftCommentId: Int,
        sourceOwnerId: Long,
        draftCommentBody: String?
    ) {
        getCommentCreatePlace(accountId, draftCommentId, sourceOwnerId, draftCommentBody)
            .tryOpenWith(requireActivity())
    }

    override fun setupReplyViews(replyTo: String?) {
        mReplyView?.visibility =
            if (replyTo != null) View.VISIBLE else View.GONE
        mReplyText?.text = replyTo
    }

    override fun replaceBodySelectionTextTo(replyText: String?) {
        if (mInputController != null) {
            val edit = mInputController?.inputField
            val selectionStart = edit?.selectionStart
            val selectionEnd = edit?.selectionEnd
            if (selectionStart != null) {
                if (selectionEnd != null) {
                    edit.text?.replace(selectionStart, selectionEnd, replyText)
                }
            }
        }
    }

    override fun goToCommentEdit(accountId: Long, comment: Comment, commemtId: Int?) {
        getEditCommentPlace(accountId, comment, commemtId)
            .tryOpenWith(requireActivity())
    }

    override fun goToWallPost(accountId: Long, postId: Int, postOwnerId: Long) {
        getPostPreviewPlace(accountId, postId, postOwnerId).tryOpenWith(requireActivity())
    }

    override fun goToVideoPreview(accountId: Long, videoId: Int, videoOwnerId: Long) {
        getVideoPreviewPlace(accountId, videoOwnerId, videoId, null, null).tryOpenWith(
            requireActivity()
        )
    }

    override fun banUser(accountId: Long, groupId: Long, user: User) {
        getCommunityAddBanPlace(accountId, groupId, singletonArrayList(user)).tryOpenWith(
            requireActivity()
        )
    }

    override fun displayAuthorAvatar(url: String?) {
        mAuthorAvatar?.let {
            if (url.nonNullNoEmpty()) {
                it.visibility = View.VISIBLE
                with()
                    .load(url)
                    .transform(RoundTransformation())
                    .into(it)
            } else {
                mAuthorAvatar?.visibility = View.GONE
                with()
                    .cancelRequest(it)
            }
        }
    }

    override fun scrollToPosition(position: Int) {
        mLinearLayoutManager?.scrollToPosition(position + (mAdapter?.headersCount ?: 0))
    }

    override fun showCommentSentToast() {
        customToast.showToastSuccessBottom(R.string.toast_comment_sent, true)
    }

    override fun showAuthorSelectDialog(owners: List<Owner>) {
        val data = ArrayList(owners)
        val adapter = OwnersListAdapter(requireActivity(), data)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.select_comment_author)
            .setAdapter(adapter) { _: DialogInterface?, which: Int ->
                if (Settings.get().accounts().registered.contains(data[which].ownerId)) {
                    presenter?.fireAuthorSelected(
                        data[which]
                    )
                } else {
                    showError(R.string.token_community_required)
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun setupOptionMenu(
        topicPollAvailable: Boolean,
        gotoSourceAvailable: Boolean,
        gotoSourceText: Int?
    ) {
        mTopicPollAvailable = topicPollAvailable
        mGotoSourceAvailable = gotoSourceAvailable
        mGotoSourceText = gotoSourceText
        try {
            requireActivity().invalidateOptionsMenu()
        } catch (ignored: Exception) {
        }
    }

    override fun setEpmtyTextVisible(visible: Boolean) {
        mEmptyView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setCenterProgressVisible(visible: Boolean) {
        mCenterProgressBar?.visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    override fun displayDeepLookingCommentProgress() {
        mDeepLookingProgressDialog =
            SpotsDialog.Builder().setContext(requireActivity()).setCancelable(true)
                .setCancelListener {
                    presenter?.fireDeepLookingCancelledByUser()
                }
                .build()
        mDeepLookingProgressDialog?.show()
    }

    override fun dismissDeepLookingCommentProgress() {
        mDeepLookingProgressDialog?.dismiss()
    }

    override fun setCanSendSelectAuthor(can: Boolean) {
        mCanSendCommentAsAdmin = can
    }

    override fun updateStickers(items: List<Sticker>) {
        if (items.isEmpty()) {
            stickersKeywordsView?.visibility = View.GONE
        } else {
            stickersKeywordsView?.visibility = View.VISIBLE
        }
        stickersAdapter?.setData(items)
    }

    override fun onStickerClick(sticker: Sticker) {
        presenter?.fireStickerClick(sticker)
    }

    override fun onInputTextChanged(s: String?) {
        presenter?.let {
            it.fireInputTextChanged(s)
            it.fireTextEdited(s)
        }
    }

    override fun onSendClicked() {
        presenter?.fireSendClick()
    }

    override fun onAttachClick() {
        presenter?.fireAttachClick()
    }

    override fun onReplyToOwnerClick(ownerId: Long, commentId: Int) {
        presenter?.fireReplyToOwnerClick(
            commentId
        )
    }

    override fun onRestoreComment(commentId: Int) {
        presenter?.fireCommentRestoreClick(
            commentId
        )
    }

    override fun onAvatarClick(ownerId: Long) {
        onOpenOwner(ownerId)
    }

    override fun onCommentLikeClick(comment: Comment, add: Boolean) {
        presenter?.fireCommentLikeClick(
            comment,
            add
        )
    }

    override fun populateCommentContextMenu(comment: Comment) {
        if (comment.fromId == 0L) {
            return
        }
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.header(comment.fullAuthorName, R.drawable.comment, comment.maxAuthorAvaUrl)
        menus.columns(2)
        val contextView = ContextView()
        presenter?.fireCommentContextViewCreated(
            contextView,
            comment
        )
        if (!comment.text.isNullOrEmpty()) {
            menus.add(
                OptionRequest(
                    CommentsOption.copy_item_comment,
                    getString(R.string.copy_value),
                    R.drawable.content_copy,
                    true
                )
            )
        }
        menus.add(
            OptionRequest(
                CommentsOption.reply_item_comment,
                getString(R.string.reply),
                R.drawable.reply,
                true
            )
        )
        menus.add(
            OptionRequest(
                CommentsOption.report_item_comment,
                getString(R.string.report),
                R.drawable.report,
                true
            )
        )
        if (contextView.pCanDelete) {
            menus.add(
                OptionRequest(
                    CommentsOption.delete_item_comment,
                    getString(R.string.delete),
                    R.drawable.ic_outline_delete,
                    true
                )
            )
        }
        if (contextView.pCanEdit) {
            menus.add(
                OptionRequest(
                    CommentsOption.edit_item_comment,
                    getString(R.string.edit),
                    R.drawable.pencil,
                    true
                )
            )
        }
        if (contextView.pCanBan) {
            menus.add(
                OptionRequest(
                    CommentsOption.block_author_item_comment,
                    getString(R.string.ban_author),
                    R.drawable.block_outline,
                    false
                )
            )
        }
        if (!comment.isUserLikes) {
            menus.add(
                OptionRequest(
                    CommentsOption.like_item_comment,
                    getString(R.string.like),
                    R.drawable.heart,
                    false
                )
            )
        } else {
            menus.add(
                OptionRequest(
                    CommentsOption.dislike_item_comment,
                    getString(R.string.dislike),
                    R.drawable.ic_no_heart,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                CommentsOption.who_like_item_comment,
                getString(R.string.who_likes),
                R.drawable.heart_filled,
                false
            )
        )
        menus.add(
            OptionRequest(
                CommentsOption.send_to_friend_item_comment,
                getString(R.string.send_to_friend),
                R.drawable.friends,
                false
            )
        )
        menus.show(
            childFragmentManager,
            "comments_options"
        ) { _, option ->
            when (option.id) {
                CommentsOption.copy_item_comment -> {
                    val clipboard = requireActivity()
                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("comment", comment.text)
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                        .showToast(R.string.copied_to_clipboard)
                }

                CommentsOption.reply_item_comment -> presenter?.fireReplyToCommentClick(
                    comment
                )

                CommentsOption.report_item_comment -> presenter?.fireReport(
                    requireActivity(),
                    comment
                )

                CommentsOption.delete_item_comment -> presenter?.fireCommentDeleteClick(
                    comment
                )

                CommentsOption.edit_item_comment -> presenter?.fireCommentEditClick(
                    comment
                )

                CommentsOption.block_author_item_comment -> presenter?.fireBanClick(
                    comment
                )

                CommentsOption.like_item_comment -> presenter?.fireCommentLikeClick(
                    comment,
                    true
                )

                CommentsOption.dislike_item_comment -> presenter?.fireCommentLikeClick(
                    comment,
                    false
                )

                CommentsOption.who_like_item_comment -> presenter?.fireWhoLikesClick(
                    comment
                )

                CommentsOption.send_to_friend_item_comment -> presenter?.fireReplyToChat(
                    requireActivity(),
                    comment
                )
            }
        }
    }

    override fun onHashTagClicked(hashTag: String) {
        presenter?.fireHashtagClick(
            hashTag
        )
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.comments_list_menu, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.open_poll).isVisible = mTopicPollAvailable
        val gotoSource = menu.findItem(R.id.to_commented)
        gotoSource.isVisible = mGotoSourceAvailable
        if (mGotoSourceAvailable) {
            mGotoSourceText?.let { gotoSource.setTitle(it) }
        }
        val desc = Settings.get().main().isCommentsDesc
        menu.findItem(R.id.direction).setIcon(getDirectionIcon(desc))
    }

    @DrawableRes
    private fun getDirectionIcon(desc: Boolean): Int {
        return if (desc) R.drawable.double_up else R.drawable.double_down
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.refresh -> {
                presenter?.fireRefreshClick()
                return true
            }

            R.id.open_poll -> {
                presenter?.fireTopicPollClick()
                return true
            }

            R.id.to_commented -> {
                presenter?.fireGotoSourceClick()
                return true
            }

            R.id.direction -> {
                val decs = Settings.get().main().toggleCommentsDirection
                menuItem.setIcon(getDirectionIcon(decs))
                presenter?.fireDirectionChanged()
                return true
            }

            else -> return false
        }
    }

    override fun onBackPressed(): Boolean {
        return mInputController?.onBackPressed() == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mInputController?.destroyView()
        mInputController = null
    }

    private class ContextView : ICommentContextView {
        var pCanEdit = false
        var pCanDelete = false
        var pCanBan = false
        override fun setCanEdit(can: Boolean) {
            pCanEdit = can
        }

        override fun setCanDelete(can: Boolean) {
            pCanDelete = can
        }

        override fun setCanBan(can: Boolean) {
            pCanBan = can
        }
    }

    companion object {
        private const val EXTRA_AT_COMMENT_OBJECT = "at_comment_object"
        private const val EXTRA_AT_COMMENT_THREAD = "at_comment_thread"
        fun newInstance(place: Place): CommentsFragment {
            val fragment = CommentsFragment()
            fragment.arguments = place.safeArguments()
            return fragment
        }

        fun buildArgs(
            accountId: Long,
            commented: Commented?,
            focusToComment: Int?,
            CommentThread: Int?
        ): Bundle {
            val bundle = Bundle()
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            bundle.putParcelable(Extra.COMMENTED, commented)
            if (focusToComment != null) {
                bundle.putInt(EXTRA_AT_COMMENT_OBJECT, focusToComment)
            }
            if (CommentThread != null) bundle.putInt(EXTRA_AT_COMMENT_THREAD, CommentThread)
            return bundle
        }
    }
}