package dev.ragnarok.fenrir.fragment.voters

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.absownerslist.AbsOwnersListFragment
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory

class VotersFragment : AbsOwnersListFragment<VotersPresenter, ISimpleOwnersView>() {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VotersPresenter> {
        return object : IPresenterFactory<VotersPresenter> {
            override fun create(): VotersPresenter {
                return VotersPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    requireArguments().getInt(Extra.POLL),
                    requireArguments().getLong(Extra.ANSWER),
                    requireArguments().getBoolean(Extra.IS_BOARD),
                    saveInstanceState
                )
            }
        }
    }

    override fun hasToolbar(): Boolean {
        return true
    }

    override fun needShowCount(): Boolean {
        return true
    }

    companion object {
        fun newInstance(args: Bundle?): VotersFragment {
            val fragment = VotersFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(
            accountId: Long,
            ownerId: Long,
            pollId: Int,
            board: Boolean,
            answer: Long
        ): Bundle {
            val bundle = Bundle()
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            bundle.putLong(Extra.OWNER_ID, ownerId)
            bundle.putInt(Extra.POLL, pollId)
            bundle.putLong(Extra.ANSWER, answer)
            bundle.putBoolean(Extra.IS_BOARD, board)
            return bundle
        }
    }
}