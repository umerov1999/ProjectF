package dev.ragnarok.fenrir.fragment.friends.mutualfriends

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.absownerslist.AbsOwnersListFragment
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory

class MutualFriendsFragment : AbsOwnersListFragment<MutualFriendsPresenter, ISimpleOwnersView>() {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<MutualFriendsPresenter> {
        return object : IPresenterFactory<MutualFriendsPresenter> {
            override fun create(): MutualFriendsPresenter {
                return MutualFriendsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(EXTRA_TARGET_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun hasToolbar(): Boolean {
        return false
    }

    override fun needShowCount(): Boolean {
        return false
    }

    companion object {
        private const val EXTRA_TARGET_ID = "targetId"
        fun newInstance(accountId: Long, targetId: Long): MutualFriendsFragment {
            val bundle = Bundle()
            bundle.putLong(EXTRA_TARGET_ID, targetId)
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            val friendsFragment = MutualFriendsFragment()
            friendsFragment.arguments = bundle
            return friendsFragment
        }
    }
}