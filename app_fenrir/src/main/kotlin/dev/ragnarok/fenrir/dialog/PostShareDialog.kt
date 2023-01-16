package dev.ragnarok.fenrir.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.videopreview.MenuAdapter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Text
import dev.ragnarok.fenrir.model.menu.Item
import dev.ragnarok.fenrir.util.AssertUtils.assertTrue
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PostShareDialog : DialogFragment() {
    private val compositeDisposable = CompositeDisposable()
    private var mAccountId = 0L
    private var mPost: Post? = null
    private var mAdapter: MenuAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        mPost = requireArguments().getParcelableCompat(Extra.POST)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun onItemClick(item: Item) {
        val data = Bundle()
        val method = item.key
        data.putLong(Extra.ACCOUNT_ID, mAccountId)
        data.putInt(EXTRA_METHOD, method)
        data.putParcelable(Extra.POST, mPost)
        if (method == Methods.REPOST_GROUP) {
            data.putLong(EXTRA_OWNER_ID, item.extra)
        }
        parentFragmentManager.setFragmentResult(REQUEST_POST_SHARE, data)
        dismissAllowingStateLoss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val interactor = owners
        val items: MutableList<Item> = ArrayList()
        items.add(Item(Methods.SHARE_LINK, Text(R.string.share_link)).setIcon(R.drawable.web))
        items.add(
            Item(
                Methods.SEND_MESSAGE,
                Text(R.string.repost_send_message)
            ).setIcon(R.drawable.share)
        )
        val canRepostYourself =
            mPost?.ownerId != mAccountId && mPost?.isFriendsOnly == false && mPost?.authorId != mAccountId
        if (canRepostYourself) {
            items.add(
                Item(
                    Methods.REPOST_YOURSELF,
                    Text(R.string.repost_to_wall)
                ).setIcon(R.drawable.ic_outline_share)
            )
        }
        mAdapter = MenuAdapter(requireActivity(), items, true)
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.repost_title)
            .setAdapter(mAdapter) { _: DialogInterface?, which: Int -> onItemClick(items[which]) }
            .setNegativeButton(R.string.button_cancel, null)
        val iAmOwnerAndAuthor = mPost?.ownerId == mAccountId && mPost?.authorId == mAccountId

        // Аккуратно, сложная логика!!!
        val canShareToGroups =
            mPost?.isCanRepost == true || iAmOwnerAndAuthor && mPost?.isFriendsOnly == false
        if (canShareToGroups) {
            compositeDisposable.add(
                interactor
                    .getCommunitiesWhereAdmin(
                        mAccountId,
                        admin = true,
                        editor = true,
                        moderator = false
                    )
                    .fromIOToMain()
                    .subscribe({ owners ->
                        for (owner in owners) {
                            if (owner.ownerId == mPost?.ownerId) {
                                continue
                            }
                            items.add(
                                Item(Methods.REPOST_GROUP, Text(owner.fullName))
                                    .setIcon(owner.get100photoOrSmaller())
                                    .setExtra(owner.ownerId)
                            )
                        }
                        mAdapter?.notifyDataSetChanged()
                    }, ignore())
            )
        }
        return builder.create()
    }

    object Methods {
        const val SHARE_LINK = 1
        const val SEND_MESSAGE = 2
        const val REPOST_YOURSELF = 3
        const val REPOST_GROUP = 4
    }

    companion object {
        const val REQUEST_POST_SHARE = "request_post_share"
        private const val EXTRA_METHOD = "share-method"
        private const val EXTRA_OWNER_ID = "share-owner-id"
        fun newInstance(accountId: Long, post: Post): PostShareDialog {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.POST, post)
            val fragment = PostShareDialog()
            fragment.arguments = args
            return fragment
        }

        fun extractMethod(data: Bundle): Int {
            assertTrue(data.containsKey(EXTRA_METHOD))
            return data.getInt(EXTRA_METHOD)
        }

        fun extractPost(data: Bundle): Post? {
            return data.getParcelableCompat(Extra.POST)
        }

        fun extractAccountId(data: Bundle): Long {
            assertTrue(data.containsKey(Extra.ACCOUNT_ID))
            return data.getLong(Extra.ACCOUNT_ID)
        }

        fun extractOwnerId(data: Bundle): Long {
            assertTrue(data.containsKey(EXTRA_OWNER_ID))
            return data.getLong(EXTRA_OWNER_ID)
        }
    }
}