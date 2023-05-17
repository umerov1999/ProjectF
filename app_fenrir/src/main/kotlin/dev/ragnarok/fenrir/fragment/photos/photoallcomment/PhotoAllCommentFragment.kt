package dev.ragnarok.fenrir.fragment.photos.photoallcomment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.comments.CommentsAdapter
import dev.ragnarok.fenrir.fragment.comments.CommentsAdapter.OnCommentActionListener
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.menu.options.CommentsPhotoOption
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.spots.SpotsDialog
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView

class PhotoAllCommentFragment :
    PlaceSupportMvpFragment<PhotoAllCommentPresenter, IPhotoAllCommentView>(),
    IPhotoAllCommentView, SwipeRefreshLayout.OnRefreshListener, OnCommentActionListener,
    EmojiconTextView.OnHashTagClickListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CommentsAdapter? = null
    private var mEmpty: TextView? = null
    private var mDeepLookingProgressDialog: AlertDialog? = null
    private var recyclerView: RecyclerView? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_photo_all_comment, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        recyclerView = root.findViewById(android.R.id.list)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        linearLayoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = CommentsAdapter(requireActivity(), mutableListOf(), this)
        mAdapter?.setListener(this)
        mAdapter?.setOnHashTagClickListener(this)
        recyclerView?.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(comments: MutableList<Comment>) {
        if (mAdapter != null) {
            mAdapter?.setItems(comments)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun dismissDeepLookingCommentProgress() {
        mDeepLookingProgressDialog?.dismiss()
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

    override fun moveFocusTo(index: Int, smooth: Boolean) {
        if (mAdapter == null) {
            return
        }
        val adapterPosition = index + (mAdapter?.headersCount ?: 0)
        if (smooth) {
            recyclerView?.smoothScrollToPosition(adapterPosition)
        } else {
            linearLayoutManager?.scrollToPosition(adapterPosition)
        }
    }

    override fun notifyDataAddedToTop(count: Int) {
        if (mAdapter != null) {
            val startSize = (mAdapter?.realItemCount ?: 0)
            mAdapter?.notifyItemRangeInserted(startSize + (mAdapter?.headersCount ?: 0), count)
        }
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index + (mAdapter?.headersCount ?: 0))
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PhotoAllCommentPresenter> {
        return object : IPresenterFactory<PhotoAllCommentPresenter> {
            override fun create(): PhotoAllCommentPresenter {
                return PhotoAllCommentPresenter(
                    requireArguments().getLong(
                        Extra.ACCOUNT_ID
                    ), requireArguments().getLong(Extra.OWNER_ID), saveInstanceState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.comments)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onReplyToOwnerClick(ownerId: Long, commentId: Int) {
        presenter?.fireReplyToOwnerClick(
            commentId
        )
    }

    override fun onRestoreComment(commentId: Int) {}
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
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.header(comment.fullAuthorName, R.drawable.comment, comment.maxAuthorAvaUrl)
        menus.columns(2)
        menus.add(
            OptionRequest(
                CommentsPhotoOption.go_to_photo_item_comment,
                getString(R.string.photo),
                R.drawable.dir_photo,
                true
            )
        )
        if (!comment.text.isNullOrEmpty()) {
            menus.add(
                OptionRequest(
                    CommentsPhotoOption.copy_item_comment,
                    getString(R.string.copy_value),
                    R.drawable.content_copy,
                    true
                )
            )
        }
        menus.add(
            OptionRequest(
                CommentsPhotoOption.report_item_comment,
                getString(R.string.report),
                R.drawable.report,
                true
            )
        )
        if (!comment.isUserLikes) {
            menus.add(
                OptionRequest(
                    CommentsPhotoOption.like_item_comment,
                    getString(R.string.like),
                    R.drawable.heart,
                    false
                )
            )
        } else {
            menus.add(
                OptionRequest(
                    CommentsPhotoOption.dislike_item_comment,
                    getString(R.string.dislike),
                    R.drawable.ic_no_heart,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                CommentsPhotoOption.who_like_item_comment,
                getString(R.string.who_likes),
                R.drawable.heart_filled,
                false
            )
        )
        menus.add(
            OptionRequest(
                CommentsPhotoOption.send_to_friend_item_comment,
                getString(R.string.send_to_friend),
                R.drawable.friends,
                false
            )
        )
        menus.show(
            requireActivity().supportFragmentManager,
            "comments_photo_options",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    when (option.id) {
                        CommentsPhotoOption.go_to_photo_item_comment ->
                            presenter?.fireGoPhotoClick(
                                comment
                            )

                        CommentsPhotoOption.copy_item_comment -> {
                            val clipboard = requireActivity()
                                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                            val clip = ClipData.newPlainText("comment", comment.text)
                            clipboard?.setPrimaryClip(clip)
                            createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                                .showToast(R.string.copied_to_clipboard)
                        }

                        CommentsPhotoOption.report_item_comment -> presenter?.fireReport(
                            comment,
                            requireActivity()
                        )

                        CommentsPhotoOption.like_item_comment -> presenter?.fireCommentLikeClick(
                            comment,
                            true
                        )

                        CommentsPhotoOption.dislike_item_comment -> presenter?.fireCommentLikeClick(
                            comment,
                            false
                        )

                        CommentsPhotoOption.who_like_item_comment -> presenter?.fireWhoLikesClick(
                            comment
                        )

                        CommentsPhotoOption.send_to_friend_item_comment -> presenter?.fireReplyToChat(
                            comment,
                            requireActivity()
                        )
                    }
                }
            })
    }

    override fun onHashTagClicked(hashTag: String) {
        presenter?.fireHashtagClick(
            hashTag
        )
    }

    companion object {
        fun newInstance(accountId: Long, ownerId: Long): PhotoAllCommentFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            val fragment = PhotoAllCommentFragment()
            fragment.arguments = args
            return fragment
        }
    }
}