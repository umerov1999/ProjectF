package dev.ragnarok.fenrir.fragment.friends.followers

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.DeltaOwnerActivity
import dev.ragnarok.fenrir.fragment.absownerslist.AbsOwnersListFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner

class FollowersFragment : AbsOwnersListFragment<FollowersPresenter, IFollowersView>(),
    IFollowersView {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FollowersPresenter> {
        return object : IPresenterFactory<FollowersPresenter> {
            override fun create(): FollowersPresenter {
                return FollowersPresenter(
                    requireArguments().getInt(
                        Extra.ACCOUNT_ID
                    ),
                    requireArguments().getInt(Extra.USER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun notifyRemoved(position: Int) {
        mOwnersAdapter?.notifyItemRemoved(position)
    }

    override fun onLongClick(owner: Owner): Boolean {
        presenter?.removeFollower(
            owner
        )
        return true
    }

    override fun showModFollowers(
        add: List<Owner>,
        remove: List<Owner>,
        accountId: Int,
        ownerId: Int
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

        fun newInstance(accountId: Int, userId: Int): FollowersFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.USER_ID, userId)
            val followersFragment = FollowersFragment()
            followersFragment.arguments = args
            return followersFragment
        }
    }
}