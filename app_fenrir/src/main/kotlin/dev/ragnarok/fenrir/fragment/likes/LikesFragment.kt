package dev.ragnarok.fenrir.fragment.likes

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.absownerslist.AbsOwnersListFragment
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory

class LikesFragment : AbsOwnersListFragment<LikesListPresenter, ISimpleOwnersView>() {
    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(if ("likes" == requireArguments().getString(Extra.FILTER)) R.string.like else R.string.shared)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LikesListPresenter> {
        return object : IPresenterFactory<LikesListPresenter> {
            override fun create(): LikesListPresenter {
                return LikesListPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getString(Extra.TYPE)!!,
                    requireArguments().getInt(Extra.OWNER_ID),
                    requireArguments().getInt(Extra.ITEM_ID),
                    requireArguments().getString(Extra.FILTER)!!,
                    saveInstanceState
                )
            }
        }
    }

    override fun hasToolbar(): Boolean {
        return true
    }

    override fun needShowCount(): Boolean {
        return false
    }

    companion object {
        fun buildArgs(
            accountId: Int,
            type: String?,
            ownerId: Int,
            itemId: Int,
            filter: String?
        ): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.TYPE, type)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.ITEM_ID, itemId)
            args.putString(Extra.FILTER, filter)
            return args
        }

        fun newInstance(args: Bundle): LikesFragment {
            val fragment = LikesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}