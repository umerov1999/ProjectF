package dev.ragnarok.fenrir.fragment.feed

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity.Companion.startFaveSelection
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalOptionsAdapter
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalOptionsAdapter.CustomListener
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getLikesCopiesPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.is600dp
import dev.ragnarok.fenrir.util.Utils.isLandscape
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper.Companion.createFrom
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class FeedFragment : PlaceSupportMvpFragment<FeedPresenter, IFeedView>(), IFeedView,
    SwipeRefreshLayout.OnRefreshListener, FeedAdapter.ClickListener,
    HorizontalOptionsAdapter.Listener<FeedSource>, CustomListener<FeedSource>, MenuProvider {
    private val requestProfileSelect = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val owners: ArrayList<Owner>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.OWNERS)
            lazyPresenter {
                presenter?.fireAddToFaveList(requireActivity(), owners)
            }
        }
    }
    private var mAdapter: FeedAdapter? = null
    private var mEmptyText: TextView? = null
    private var mRecycleView: RecyclerView? = null
    private var mFeedLayoutManager: RecyclerView.LayoutManager? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mLoadMoreFooterHelper: LoadMoreFooterHelper? = null
    private var mFeedSourceAdapter: HorizontalOptionsAdapter<FeedSource>? = null
    private var mHeaderLayoutManager: LinearLayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_feed, menu)
        val s = presenter?.configureMenu()
        s.nonNullNoEmpty {
            menu.addSubMenu(R.string.sections).let { vv ->
                for ((p, i) in s.orEmpty().withIndex()) {
                    vv.add(
                        if (i.isCustom) i.getTitle(requireActivity()) else "#" + i.getTitle(
                            requireActivity()
                        )
                    )
                        .setOnMenuItemClickListener {
                            mRecycleView?.stopScroll()
                            mRecycleView?.scrollToPosition(0)
                            mHeaderLayoutManager?.scrollToPosition(p)
                            presenter?.fireFeedSourceClick(
                                i
                            )
                            true
                        }
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.refresh -> {
                presenter?.fireRefresh()
                true
            }

            R.id.action_create_list -> {
                requestProfileSelect.launch(startFaveSelection(requireActivity()))
                true
            }

            R.id.action_feed_ban -> {
                PlaceFactory.getFeedBanPlace(Settings.get().accounts().current)
                    .tryOpenWith(requireActivity())
                true
            }

            else -> false
        }
    }

    override fun scrollTo(pos: Int) {
        mFeedLayoutManager?.scrollToPosition(pos)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_new_feed, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        styleSwipeRefreshLayoutWithCurrentTheme(mSwipeRefreshLayout, false)
        mFeedLayoutManager = if (is600dp(requireActivity())) {
            val land = isLandscape(requireActivity())
            StaggeredGridLayoutManager(if (land) 2 else 1, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        }
        mRecycleView = root.findViewById(R.id.fragment_feeds_list)
        mRecycleView?.layoutManager = mFeedLayoutManager
        mRecycleView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        mRecycleView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener(4, 1000) {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToBottom()
            }
        })
        val Goto: FloatingActionButton = root.findViewById(R.id.goto_button)
        Goto.setOnClickListener {
            mRecycleView?.stopScroll()
            mRecycleView?.scrollToPosition(0)
        }
        Goto.setOnLongClickListener {
            mRecycleView?.stopScroll()
            mFeedLayoutManager?.scrollToPosition(0)
            presenter?.fireRefresh()
            true
        }
        mEmptyText = root.findViewById(R.id.fragment_feeds_empty_text)
        val footerView =
            inflater.inflate(R.layout.footer_load_more, mRecycleView, false) as ViewGroup
        mLoadMoreFooterHelper = createFrom(footerView, object : LoadMoreFooterHelper.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireLoadMoreClick()
            }
        })

        //ViewGroup headerView = (ViewGroup) inflater.inflate(R.layout.header_feed, mRecycleView, false);
        val headerRecyclerView: RecyclerView = root.findViewById(R.id.header_list)
        mHeaderLayoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        headerRecyclerView.layoutManager = mHeaderLayoutManager
        mAdapter = FeedAdapter(requireActivity(), mutableListOf(), this)
        mAdapter?.setClickListener(this)
        mAdapter?.addFooter(footerView)
        //mAdapter.addHeader(headerView);
        mRecycleView?.adapter = mAdapter
        mFeedSourceAdapter = HorizontalOptionsAdapter(mutableListOf())
        mFeedSourceAdapter?.setListener(this)
        mFeedSourceAdapter?.setDeleteListener(this)
        headerRecyclerView.adapter = mFeedSourceAdapter
        return root
    }

    /*private void resolveEmptyText() {
        if(!isAdded()) return;
        mEmptyText.setVisibility(!nowRequest() && isEmpty(mData) ? View.VISIBLE : View.INVISIBLE);
        mEmptyText.setText(R.string.feeds_empty_text);
    }*/
    /* private void loadFeedSourcesData() {
        restoreCurrentSourceIds();

        initFeedSources(null);
        mFeedListsLoader.loadAll(getAccountId());
        executeRequest(FeedRequestFactory.getFeedListsRequest());
    }*/
    /*private void loadFeedData() {
        // восстанавливаем из настроек последнее значение next_from, данные до которого
        // хранятся в базе данных
        mNextFrom = Settings.get()
                .other()
                .restoreFeedNextFrom(getAccountId());

        // загружаем все новости, которые сохранены в базу данных текущего аккаунта
        mFeedLoader.load(buildCriteria(), true);

        // если же предыдущего состояния next_from нет, то запрашиваем новости
        // с сервера (в противном случае ничего не загружаем с сервиса, пользователь
        // должен ПРОДОЛЖИТЬ читать ленту с того места, где закончил)
        if (TextUtils.isEmpty(mNextFrom)) {
            requestFeed();
        }
    }*/
    override fun onAvatarClick(ownerId: Long) {
        super.onOwnerClick(ownerId)
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.FEED)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.feed)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_FEED)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onOwnerClick(ownerId: Long) {
        onOpenOwner(ownerId)
    }

    override fun onRepostClick(news: News) {
        presenter?.fireNewsRepostClick(
            news
        )
    }

    override fun onPostClick(news: News) {
        presenter?.fireNewsBodyClick(
            news
        )
    }

    override fun onBanClick(news: News) {
        presenter?.fireBanClick(
            news
        )
    }

    override fun onIgnoreClick(news: News) {
        presenter?.fireIgnoreClick(
            news
        )
    }

    override fun onFaveClick(news: News) {
        presenter?.fireAddBookmark(
            news.sourceId,
            news.postId
        )
    }

    override fun onCommentButtonClick(news: News) {
        if (!news.isCommentCanPost) {
            CustomToast.createCustomToast(requireActivity())
                .showToastError(R.string.comments_disabled_post)
        }
        presenter?.fireNewsCommentClick(
            news
        )
    }

    override fun onLikeClick(news: News, add: Boolean) {
        presenter?.fireLikeClick(
            news
        )
    }

    override fun onLikeLongClick(news: News): Boolean {
        presenter?.fireNewsLikeLongClick(
            news
        )
        return true
    }

    override fun onShareLongClick(news: News): Boolean {
        presenter?.fireNewsShareLongClick(
            news
        )
        return true
    }

    override fun goToLikes(accountId: Long, type: String?, ownerId: Long, id: Int) {
        getLikesCopiesPlace(
            accountId,
            type,
            ownerId,
            id,
            ILikesInteractor.FILTER_LIKES
        ).tryOpenWith(requireActivity())
    }

    override fun goToReposts(accountId: Long, type: String?, ownerId: Long, id: Int) {
        getLikesCopiesPlace(
            accountId,
            type,
            ownerId,
            id,
            ILikesInteractor.FILTER_COPIES
        ).tryOpenWith(requireActivity())
    }

    override fun goToPostComments(accountId: Long, postId: Int, ownerId: Long) {
        val commented = Commented(postId, ownerId, CommentedType.POST, null)
        getCommentsPlace(accountId, commented, null).tryOpenWith(requireActivity())
    }

    override fun askToReload() {
        CustomSnackbars.createCustomSnackbars(view, null, true)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)?.defaultSnack(R.string.update_news)
            ?.setAction(R.string.do_update) {
                mFeedLayoutManager?.scrollToPosition(0)
                presenter?.fireRefresh()
            }?.show()
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    @SuppressLint("RestrictedApi")
    private fun restoreRecycleViewManagerState(state: String?) {
        if (state.nonNullNoEmpty()) {
            /*
            if (mFeedLayoutManager is LinearLayoutManager) {
                val savedState = gson().fromJson(state, LinearLayoutManager.SavedState::class.java)
                mFeedLayoutManager?.onRestoreInstanceState(savedState)
            } else if (mFeedLayoutManager is StaggeredGridLayoutManager) {
                val savedState =
                    gson().fromJson(state, StaggeredGridLayoutManager.SavedState::class.java)
                mFeedLayoutManager?.onRestoreInstanceState(savedState)
            }
             */

            when (mFeedLayoutManager) {
                is LinearLayoutManager -> {
                    val savedState =
                        kJson.decodeFromString(LinearLayoutManager_SavedState.serializer(), state)
                    mFeedLayoutManager?.onRestoreInstanceState(savedState)
                }

                is StaggeredGridLayoutManager -> {
                    val savedState = kJson.decodeFromString(
                        StaggeredGridLayoutManager_SavedState.serializer(),
                        state
                    )
                    mFeedLayoutManager?.onRestoreInstanceState(savedState)
                }
            }
        }
    }

    override fun onPause() {
        val parcelable = mFeedLayoutManager?.onSaveInstanceState()
        val json: String = when (mFeedLayoutManager) {
            is LinearLayoutManager -> {
                kJson.encodeToString(
                    LinearLayoutManager_SavedState.serializer(),
                    parcelable as LinearLayoutManager_SavedState
                )
            }

            is StaggeredGridLayoutManager -> {
                kJson.encodeToString(
                    StaggeredGridLayoutManager_SavedState.serializer(),
                    parcelable as StaggeredGridLayoutManager_SavedState
                )
            }

            else -> {
                super.onPause()
                return
            }
        }
        presenter?.fireScrollStateOnPause(
            json
        )
        super.onPause()
    }

    /*@Override
    public void onFeedListsLoadFinished(List<VKApiFeedList> result) {
        initFeedSources(result);
        int selected = refreshFeedSourcesSelection();

        if (mFeedSourceAdapter != null) {
            mFeedSourceAdapter.notifyDataSetChanged();
        }

        if (selected != -1 && mHeaderLayoutManager != null) {
            mHeaderLayoutManager.scrollToPosition(selected);
        }
    }*/
    /*@Override
    protected void beforeAccountChange(int oldAid, int newAid) {
        super.beforeAccountChange(oldAid, newAid);
        mFeedLoader.stopIfLoading();
        mFeedListsLoader.stopIfLoading();

        storeListPosition();
        ignoreAll();
    }*/
    /*@Override
    protected void afterAccountChange(int oldAid, int newAid) {
        super.afterAccountChange(oldAid, newAid);
        restoreCurrentSourceIds();

        mEndOfContent = false;
        mData.clear();
        mFeedSources.clear();

        mAdapter.notifyDataSetChanged();
        mFeedSourceAdapter.notifyDataSetChanged();

        loadFeedData();
        loadFeedSourcesData();

        resolveEmptyText();
        resolveFooter();
    }*/
    override fun onOptionClick(entry: FeedSource) {
        mRecycleView?.stopScroll()
        mRecycleView?.scrollToPosition(0)
        presenter?.fireFeedSourceClick(
            entry
        )
    }

    override fun onDeleteOptionClick(entry: FeedSource, position: Int) {
        CustomSnackbars.createCustomSnackbars(view)
            ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)?.themedSnack(R.string.do_delete)
            ?.setAction(
                R.string.button_yes
            ) {
                mFeedSourceAdapter?.removeChild(position)
                presenter?.fireFeedSourceDelete(
                    entry.getValue()?.replace("list", "")?.toInt()
                )
            }?.show()
    }

    override fun showSuccessToast() {
        CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
            .showToastSuccessBottom(R.string.success)
    }

    override fun displayFeedSources(sources: MutableList<FeedSource>) {
        mFeedSourceAdapter?.setItems(sources)
    }

    override fun notifyFeedSourcesChanged() {
        mFeedSourceAdapter?.notifyDataSetChanged()
        requireActivity().invalidateOptionsMenu()
    }

    override fun displayFeed(data: MutableList<News>, rawScrollState: String?) {
        mAdapter?.setItems(data)
        if (rawScrollState.nonNullNoEmpty() && mFeedLayoutManager != null) {
            try {
                restoreRecycleViewManagerState(rawScrollState)
            } catch (ignored: Exception) {
            }
        }
        resolveEmptyTextVisibility()
    }

    override fun notifyFeedDataChanged() {
        mAdapter?.notifyDataSetChanged()
        resolveEmptyTextVisibility()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position + (mAdapter?.headersCount ?: 0), count)
        resolveEmptyTextVisibility()
    }

    override fun notifyItemChanged(position: Int) {
        mAdapter?.notifyItemChanged(position + (mAdapter?.headersCount ?: 0))
    }

    override fun setupLoadMoreFooter(@LoadMoreState state: Int) {
        mLoadMoreFooterHelper?.switchToState(state)
    }

    fun resolveEmptyTextVisibility() {
        if (mEmptyText != null && mAdapter != null) {
            mEmptyText?.visibility =
                if (mAdapter?.realItemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.let { it.post { it.isRefreshing = refreshing } }
    }

    override fun scrollFeedSourcesToPosition(position: Int) {
        mHeaderLayoutManager?.scrollToPosition(position)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FeedPresenter> {
        return object : IPresenterFactory<FeedPresenter> {
            override fun create(): FeedPresenter {
                return FeedPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        fun buildArgs(accountId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            return args
        }

        fun newInstance(accountId: Long): FeedFragment {
            return newInstance(buildArgs(accountId))
        }

        fun newInstance(args: Bundle?): FeedFragment {
            val feedFragment = FeedFragment()
            feedFragment.arguments = args
            return feedFragment
        }
    }
}