package dev.ragnarok.fenrir.fragment.theme

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
import dev.ragnarok.fenrir.fragment.base.compat.AbsMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.settings.theme.ThemeValue

class ThemeFragment : AbsMvpFragment<ThemePresenter, IThemeView>(), IThemeView,
    ThemeAdapter.ClickListener {
    private var mAdapter: ThemeAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_theme, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val columns = resources.getInteger(R.integer.photos_column_count)
        val gridLayoutManager = GridLayoutManager(requireActivity(), columns)
        recyclerView.layoutManager = gridLayoutManager
        mAdapter = ThemeAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.theme_edit_title)
        actionBar?.subtitle = null
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ThemePresenter> =
        object : IPresenterFactory<ThemePresenter> {
            override fun create(): ThemePresenter {
                return ThemePresenter(saveInstanceState)
            }
        }

    override fun displayData(data: Array<ThemeValue>) {
        mAdapter?.setData(data)
    }

    override fun onClick(index: Int, value: ThemeValue?) {
        if ((value ?: return).disabled) {
            return
        }
        get().ui().setMainTheme(value.id)
        mAdapter?.updateCurrentId(value.id)
        requireActivity().recreate()
        mAdapter?.notifyDataSetChanged()
    }
}