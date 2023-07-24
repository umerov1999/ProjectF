package dev.ragnarok.fenrir.fragment.shortcutsview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.ShortcutStored

class ShortcutsViewFragment : BaseMvpFragment<ShortcutsViewPresenter, IShortcutsView>(),
    IShortcutsView, ShortcutsListAdapter.ActionListener {
    private var mAdapter: ShortcutsListAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shortcuts, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val columns = resources.getInteger(R.integer.photos_column_count)
        val gridLayoutManager = GridLayoutManager(requireActivity(), columns)
        recyclerView.layoutManager = gridLayoutManager
        PicassoPauseOnScrollListener.addListener(recyclerView)
        mAdapter = ShortcutsListAdapter(emptyList())
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.add_to_launcher_shortcuts)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayData(shortcuts: List<ShortcutStored>) {
        mAdapter?.setData(shortcuts)
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ShortcutsViewPresenter> {
        return object : IPresenterFactory<ShortcutsViewPresenter> {
            override fun create(): ShortcutsViewPresenter {
                return ShortcutsViewPresenter(
                    saveInstanceState
                )
            }
        }
    }

    override fun onShortcutClick(shortcutStored: ShortcutStored) {
        presenter?.fireShortcutClick(requireActivity(), shortcutStored)
    }

    override fun onShortcutRemoved(pos: Int, shortcutStored: ShortcutStored) {
        presenter?.fireShortcutDeleted(pos, shortcutStored)
    }
}
