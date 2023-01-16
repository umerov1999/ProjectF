package dev.ragnarok.fenrir.fragment.friends.followers

import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.DeltaOwnerActivity
import dev.ragnarok.fenrir.fragment.absownerslist.AbsOwnersListFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Utils

class FollowersFragment : AbsOwnersListFragment<FollowersPresenter, IFollowersView>(),
    IFollowersView {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FollowersPresenter> {
        return object : IPresenterFactory<FollowersPresenter> {
            override fun create(): FollowersPresenter {
                return FollowersPresenter(
                    requireArguments().getLong(
                        Extra.ACCOUNT_ID
                    ),
                    requireArguments().getLong(Extra.USER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun notifyRemoved(position: Int) {
        mOwnersAdapter?.notifyItemRemoved(position)
    }

    override fun onLongClick(owner: Owner): Boolean {
        if (!Utils.follower_kick_mode) {
            MaterialAlertDialogBuilder(requireActivity()).setIcon(R.drawable.report_red)
                .setTitle(R.string.select)
                .setMessage(R.string.block_or_delete)
                .setPositiveButton(R.string.block_user) { _: DialogInterface?, _: Int ->
                    Utils.follower_kick_mode = true
                    presenter?.removeFollower(owner)
                }
                .setCancelable(true).show()
            return true
        }

        presenter?.removeFollower(
            owner
        )
        return true
    }

    override fun showModFollowers(
        add: List<Owner>,
        remove: List<Owner>,
        accountId: Long,
        ownerId: Long
    ) {
        if (add.isEmpty() && remove.isEmpty()) {
            return
        }
        DeltaOwnerActivity.showDeltaActivity(
            requireActivity(),
            accountId,
            DeltaOwner().setOwner(ownerId).appendToList(
                requireActivity(),
                R.string.new_follower,
                add
            ).appendToList(
                requireActivity(),
                R.string.not_follower,
                remove
            )
        )
    }

    override fun hasToolbar(): Boolean {
        return false
    }

    override fun needShowCount(): Boolean {
        return true
    }

    companion object {

        fun newInstance(accountId: Long, userId: Long): FollowersFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.USER_ID, userId)
            val followersFragment = FollowersFragment()
            followersFragment.arguments = args
            return followersFragment
        }
    }
}