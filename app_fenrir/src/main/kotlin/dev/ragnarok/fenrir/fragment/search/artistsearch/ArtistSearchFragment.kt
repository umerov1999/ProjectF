package dev.ragnarok.fenrir.fragment.search.artistsearch

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.api.model.VKApiArtist
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.ArtistSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AudioArtist

class ArtistSearchFragment :
    AbsSearchFragment<ArtistSearchPresenter, IArtistSearchView, VKApiArtist, ArtistSearchAdapter>(),
    ArtistSearchAdapter.ClickListener, IArtistSearchView {
    override fun setAdapterData(adapter: ArtistSearchAdapter, data: MutableList<VKApiArtist>) {
        adapter.setData(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<VKApiArtist>): ArtistSearchAdapter {
        val ret = ArtistSearchAdapter(data, requireActivity())
        ret.setClickListener(this)
        return ret
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ArtistSearchPresenter> {
        return object : IPresenterFactory<ArtistSearchPresenter> {
            override fun create(): ArtistSearchPresenter {
                return ArtistSearchPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    override fun onArtistClick(id: String) {
        presenter?.fireArtistClick(
            AudioArtist(id)
        )
    }

    companion object {

        fun newInstance(
            accountId: Int,
            initialCriteria: ArtistSearchCriteria?
        ): ArtistSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = ArtistSearchFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstanceSelect(
            accountId: Int,
            initialCriteria: ArtistSearchCriteria?
        ): ArtistSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = ArtistSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}