package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.AppPerms.DoRequestPermissions
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class CatalogV2SectionFragment :
    BaseMvpFragment<CatalogV2SectionPresenter, ICatalogV2SectionView>(),
    ICatalogV2SectionView, CatalogV2SectionAdapter.ClickListener {
    private val requestWritePermission: DoRequestPermissions = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) { createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text) }
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CatalogV2SectionAdapter? = null
    private var mLoadMoreFooterHelper: LoadMoreFooterHelper? = null
    private var recyclerView: RecyclerView? = null
    private var mySearchView: MySearchView? = null
    private var inTabsContainer = false
    private var supportSearch = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inTabsContainer = requireArguments().getBoolean(EXTRA_IN_TABS_CONTAINER)
        supportSearch = requireArguments().getBoolean(EXTRA_SEARCH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(
            if (supportSearch) R.layout.fragment_catalog_v2_section_with_search else R.layout.fragment_catalog_v2_section,
            container,
            false
        )
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        if (!inTabsContainer) {
            toolbar.visibility = View.VISIBLE
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        mEmpty = root.findViewById(R.id.fragment_audio_catalog_empty_text)
        recyclerView = root.findViewById(R.id.recycleView)
        recyclerView?.layoutManager = LinearLayoutManager(requireActivity())
        //recyclerView.setRecycledViewPool(CatalogV2SectionAdapter.poolCatalogV2Section)
        PicassoPauseOnScrollListener.addListener(recyclerView)
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener(4, 1000) {
            override fun onScrollToLastElement() {
                presenter?.onNext()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = CatalogV2SectionAdapter(
            mutableListOf(),
            presenter?.accountId ?: Settings.get().accounts().current,
            requireActivity()
        )
        mAdapter?.setClickListener(this)
        val footerView =
            inflater.inflate(R.layout.footer_load_more, recyclerView, false) as ViewGroup
        mLoadMoreFooterHelper =
            LoadMoreFooterHelper.createFrom(footerView, object : LoadMoreFooterHelper.Callback {
                override fun onLoadMoreClick() {
                    presenter?.onNext()
                }
            })
        mAdapter?.addFooter(footerView)
        recyclerView?.adapter = mAdapter
        resolveEmptyText()
        val gotoButton: FloatingActionButton = root.findViewById(R.id.goto_button)

        gotoButton.setOnLongClickListener {
            val curr = MusicPlaybackController.currentAudio
            if (curr != null) {
                PlaceFactory.getPlayerPlace(Settings.get().accounts().current)
                    .tryOpenWith(requireActivity())
            } else {
                createCustomToast(requireActivity()).showToastError(R.string.null_audio)
            }
            true
        }

        gotoButton.setOnClickListener {
            val curr = MusicPlaybackController.currentAudio
            if (curr != null) {
                val index =
                    presenter?.getAudioPos(null, curr) ?: -1
                if (index >= 0) {
                    recyclerView?.scrollToPosition(
                        index + (mAdapter?.headersCount ?: 0)
                    )
                } else createCustomToast(requireActivity()).showToast(R.string.audio_not_found)
            } else createCustomToast(requireActivity()).showToastError(R.string.null_audio)
        }
        if (supportSearch) {
            mySearchView = root.findViewById(R.id.searchview)
            mySearchView?.setRightButtonVisibility(false)
            mySearchView?.setLeftIcon(R.drawable.magnify)
            mySearchView?.setOnBackButtonClickListener(object :
                MySearchView.OnBackButtonClickListener {
                override fun onBackButtonClick() {
                    if (mySearchView?.text.nonNullNoEmpty() && mySearchView?.text.trimmedNonNullNoEmpty()) {
                        presenter?.fireSearchRequestSubmitted(
                            mySearchView?.text.toString()
                        )
                    }
                }
            })
            mySearchView?.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    presenter?.fireSearchRequestSubmitted(
                        query
                    )
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })
        }
        return root
    }

    override fun search(accountId: Long, q: String) {
        PlaceFactory.getCatalogV2AudioCatalogPlace(accountId, accountId, null, q, null)
            .tryOpenWith(requireActivity())
    }

    override fun updateLayoutManager(type: String) {
        recyclerView?.let {
            it.layoutManager = when (type) {
                "links" -> {
                    val columnCount = resources.getInteger(R.integer.photos_column_count)
                    GridLayoutManager(requireActivity(), columnCount)
                }

                "music_recommended_playlists" -> {
                    val columnCount = resources.getInteger(R.integer.articles_column_count)
                    GridLayoutManager(requireActivity(), columnCount)
                }

                "music_playlists", "videos", "artist_videos" -> {
                    val columnCount = resources.getInteger(R.integer.videos_column_count)
                    GridLayoutManager(requireActivity(), columnCount)
                }

                else -> {
                    LinearLayoutManager(requireActivity())
                }
            }
        }
    }

    override fun setupLoadMoreFooter(@LoadMoreState state: Int) {
        mLoadMoreFooterHelper?.switchToState(state)
    }

    private fun resolveEmptyText() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (!inTabsContainer) {
            Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
            val actionBar = supportToolbarFor(this)
            if (actionBar != null) {
                actionBar.setTitle(R.string.audio_catalog)
                actionBar.subtitle = null
            }
            if (requireActivity() is OnSectionResumeCallback) {
                (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_AUDIOS)
            }
            ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity())
        }
    }

    override fun displayData(pages: MutableList<AbsModel>) {
        if (mAdapter != null) {
            mAdapter?.setItems(pages)
            resolveEmptyText()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyText()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemBindableRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun notifyDataChanged(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemBindableRangeChanged(position, count)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CatalogV2SectionPresenter> {
        return object : IPresenterFactory<CatalogV2SectionPresenter> {
            override fun create(): CatalogV2SectionPresenter {
                return CatalogV2SectionPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getString(Extra.SECTION_ID)!!,
                    saveInstanceState
                )
            }
        }
    }

    override fun onAddPlayList(index: Int, album: AudioPlaylist) {
        presenter?.onAdd(
            album
        )
    }

    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }

    override fun onNext(loading: Boolean) {
        presenter?.changeActualBlockLoading(loading)
    }

    override fun onError(throwable: Throwable) {
        showThrowable(throwable)
        presenter?.changeActualBlockLoading(false)
    }

    companion object {
        const val EXTRA_IN_TABS_CONTAINER = "in_tabs_container"
        const val EXTRA_SEARCH = "search_mode"
        fun newInstance(
            accountId: Long,
            sectionId: String,
            isHideToolbar: Boolean,
            supportSearch: Boolean
        ): CatalogV2SectionFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.SECTION_ID, sectionId)
            args.putBoolean(EXTRA_IN_TABS_CONTAINER, isHideToolbar)
            args.putBoolean(EXTRA_SEARCH, supportSearch)
            val fragment = CatalogV2SectionFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(accountId: Long, sectionId: String): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.SECTION_ID, sectionId)
            args.putBoolean(EXTRA_IN_TABS_CONTAINER, false)
            args.putBoolean(EXTRA_SEARCH, false)
            return args
        }

        fun newInstance(args: Bundle?): CatalogV2SectionFragment {
            val fragment = CatalogV2SectionFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
