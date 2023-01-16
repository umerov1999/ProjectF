package dev.ragnarok.fenrir.fragment.friends.recommendationsfriends

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.absownerslist.AbsOwnersListFragment
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory

class RecommendationsFriendsFragment :
    AbsOwnersListFragment<RecommendationsFriendsPresenter, ISimpleOwnersView>() {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<RecommendationsFriendsPresenter> {
        return object : IPresenterFactory<RecommendationsFriendsPresenter> {
            override fun create(): RecommendationsFriendsPresenter {
                return RecommendationsFriendsPresenter(
                    requireArguments().getLong(Extra.USER_ID),
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
        fun newInstance(accountId: Long, userId: Long): RecommendationsFriendsFragment {
            val bundle = Bundle()
            bundle.putLong(Extra.USER_ID, userId)
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            val friendsFragment = RecommendationsFriendsFragment()
            friendsFragment.arguments = bundle
            return friendsFragment
        }
    }
}