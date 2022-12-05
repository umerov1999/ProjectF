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
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(EXTRA_TARGET_ID),
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
        fun newInstance(accountId: Int, targetId: Int): MutualFriendsFragment {
            val bundle = Bundle()
            bundle.putInt(EXTRA_TARGET_ID, targetId)
            bundle.putInt(Extra.ACCOUNT_ID, accountId)
            val friendsFragment = MutualFriendsFragment()
            friendsFragment.arguments = bundle
            return friendsFragment
        }
    }
}