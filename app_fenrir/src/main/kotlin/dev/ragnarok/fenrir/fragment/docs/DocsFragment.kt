package dev.ragnarok.fenrir.fragment.docs

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.DualTabPhotoActivity.Companion.createIntent
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalOptionsAdapter
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.selection.FileManagerSelectableSource
import dev.ragnarok.fenrir.model.selection.LocalPhotosSelectableSource
import dev.ragnarok.fenrir.model.selection.Sources
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getDocPreviewPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getGifPagerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class DocsFragment : BaseMvpFragment<DocsListPresenter, IDocListView>(), IDocListView,
    DocsAdapter.ActionListener, DocsUploadAdapter.ActionListener,
    DocsAsImagesAdapter.ActionListener {
    private val requestFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = result.data?.getStringExtra(Extra.PATH)
            val photos: ArrayList<LocalPhoto>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
            if (file.nonNullNoEmpty()) {
                lazyPresenter {
                    fireFileForUploadSelected(file)
                }
            } else if (photos.nonNullNoEmpty()) {
                lazyPresenter {
                    fireLocalPhotosForUploadSelected(photos)
                }
            }
        }
    }
    private val requestReadPermission =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            lazyPresenter { fireReadPermissionResolved() }
        }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mDocsAdapter: RecyclerBindableAdapter<Document, *>? = null
    private var mUploadAdapter: DocsUploadAdapter? = null
    private var mFiltersAdapter: HorizontalOptionsAdapter<DocFilter>? = null
    private var mHeaderView: View? = null
    private var mRecyclerView: RecyclerView? = null
    private var mUploadRoot: View? = null
    private var mImagesOnly = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_docs, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mRecyclerView = root.findViewById(R.id.recycler_view)

        // тут, значит, некая многоходовочка
        // Так как мы не знаем, какой тип данных мы показываем (фото или просто документы),
        // то при создании view мы просим presenter уведомить об этом типе.
        // Предполагается, что presenter НЕЗАМЕДЛИТЕЛЬНО вызовет у view метод setAdapterType(boolean imagesOnly)
        presenter?.pleaseNotifyViewAboutAdapterType()
        // и мы дальше по коду можем использовать переменную mImagesOnly
        mRecyclerView?.layoutManager = createLayoutManager(mImagesOnly)
        mDocsAdapter = createAdapter(mImagesOnly, mutableListOf())
        val buttonAdd: FloatingActionButton = root.findViewById(R.id.add_button)
        buttonAdd.setOnClickListener {
            presenter?.fireButtonAddClick()
        }
        val uploadRecyclerView: RecyclerView = root.findViewById(R.id.uploads_recycler_view)
        uploadRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mUploadAdapter = DocsUploadAdapter(emptyList(), this)
        uploadRecyclerView.adapter = mUploadAdapter
        mHeaderView = View.inflate(requireActivity(), R.layout.header_feed, null)
        val headerRecyclerView: RecyclerView? = mHeaderView?.findViewById(R.id.header_list)
        headerRecyclerView?.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mFiltersAdapter = HorizontalOptionsAdapter(mutableListOf())
        mFiltersAdapter?.setListener(object : HorizontalOptionsAdapter.Listener<DocFilter> {
            override fun onOptionClick(entry: DocFilter) {
                presenter?.fireFilterClick(
                    entry
                )
            }
        })
        headerRecyclerView?.adapter = mFiltersAdapter
        mHeaderView?.let {
            mDocsAdapter?.addHeader(it)
        }
        mRecyclerView?.adapter = mDocsAdapter
        mUploadRoot = root.findViewById(R.id.uploads_root)
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        return root
    }

    private fun createLayoutManager(asImages: Boolean): RecyclerView.LayoutManager {
        return if (asImages) {
            val columnCount = resources.getInteger(R.integer.local_gallery_column_count)
            GridLayoutManager(requireActivity(), columnCount)
        } else {
            LinearLayoutManager(requireActivity())
        }
    }

    override fun displayData(documents: MutableList<Document>, asImages: Boolean) {
        mImagesOnly = asImages
        if (mRecyclerView == null) {
            return
        }
        if (asImages && mDocsAdapter is DocsAsImagesAdapter) {
            (mDocsAdapter as DocsAsImagesAdapter).setItems(documents)
            return
        }
        if (!asImages && mDocsAdapter is DocsAdapter) {
            (mDocsAdapter as DocsAdapter).setItems(documents)
            return
        }
        mDocsAdapter = if (asImages) {
            val docsAsImagesAdapter = DocsAsImagesAdapter(documents)
            docsAsImagesAdapter.setActionListener(this)
            docsAsImagesAdapter
        } else {
            val docsAdapter = DocsAdapter(documents)
            docsAdapter.setActionListener(this)
            docsAdapter
        }
        mRecyclerView?.layoutManager = createLayoutManager(asImages)
        mDocsAdapter = createAdapter(asImages, documents)
        mHeaderView?.let { mDocsAdapter?.addHeader(it) }
        mRecyclerView?.adapter = mDocsAdapter
    }

    private fun createAdapter(
        asImages: Boolean,
        documents: MutableList<Document>
    ): RecyclerBindableAdapter<Document, *> {
        return if (asImages) {
            val docsAsImagesAdapter = DocsAsImagesAdapter(documents)
            docsAsImagesAdapter.setActionListener(this)
            docsAsImagesAdapter
        } else {
            val docsAdapter = DocsAdapter(documents)
            docsAdapter.setActionListener(this)
            docsAdapter
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun notifyDataSetChanged() {
        mDocsAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdd(position: Int, count: Int) {
        mDocsAdapter?.notifyItemBindableRangeInserted(position, count)
    }

    override fun notifyDataRemoved(position: Int) {
        mDocsAdapter?.notifyItemBindableRemoved(position)
    }

    override fun openDocument(accountId: Int, document: Document) {
        getDocPreviewPlace(accountId, document).tryOpenWith(requireActivity())
    }

    override fun returnSelection(docs: ArrayList<Document>) {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, docs)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun goToGifPlayer(accountId: Int, gifs: ArrayList<Document>, selected: Int) {
        getGifPagerPlace(accountId, gifs, selected).tryOpenWith(requireActivity())
    }

    override fun requestReadExternalStoragePermission() {
        requestReadPermission.launch()
    }

    override fun startSelectUploadFileActivity(accountId: Int) {
        val sources = Sources()
            .with(FileManagerSelectableSource())
            .with(LocalPhotosSelectableSource())
        val intent = createIntent(requireActivity(), 10, sources)
        requestFile.launch(intent)
    }

    override fun setUploadDataVisible(visible: Boolean) {
        mUploadRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayUploads(data: List<Upload>) {
        mUploadAdapter?.setData(data)
    }

    override fun notifyUploadItemsAdded(position: Int, count: Int) {
        mUploadAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun notifyUploadItemChanged(position: Int) {
        mUploadAdapter?.notifyItemChanged(position)
    }

    override fun notifyUploadItemRemoved(position: Int) {
        mUploadAdapter?.notifyItemRemoved(position)
    }

    override fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean) {
        mUploadAdapter?.changeUploadProgress(position, progress, smoothly)
    }

    override fun displayFilterData(filters: MutableList<DocFilter>) {
        mFiltersAdapter?.setItems(filters)
    }

    override fun notifyFiltersChanged() {
        mFiltersAdapter?.notifyDataSetChanged()
    }

    override fun setAdapterType(imagesOnly: Boolean) {
        mImagesOnly = imagesOnly
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<DocsListPresenter> {
        return object : IPresenterFactory<DocsListPresenter> {
            override fun create(): DocsListPresenter {
                return DocsListPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(Extra.OWNER_ID),
                    requireArguments().getString(Extra.ACTION),
                    saveInstanceState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.DOCS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.documents)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_DOCS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onDocClick(index: Int, doc: Document) {
        presenter?.fireDocClick(
            doc
        )
    }

    override fun onDocLongClick(index: Int, doc: Document): Boolean {
        presenter?.fireMenuClick(
            requireActivity(),
            index,
            doc
        )
        return true
    }

    override fun onRemoveClick(upload: Upload) {
        presenter?.fireRemoveClick(
            upload
        )
    }

    companion object {
        fun buildArgs(accountId: Int, ownerId: Int, action: String?): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putString(Extra.ACTION, action)
            return args
        }

        fun newInstance(args: Bundle?): DocsFragment {
            val fragment = DocsFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(accountId: Int, ownerId: Int, action: String?): DocsFragment {
            return newInstance(buildArgs(accountId, ownerId, action))
        }
    }
}