package dev.ragnarok.fenrir.fragment.vkphotoalbums

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.vkphotoalbums.PhotoAlbumsPresenter.AdditionalParams
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getCreatePhotoAlbumPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getEditPhotoAlbumPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getLocalServerPhotosPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPhotoAllCommentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVKPhotosAlbumPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class VKPhotoAlbumsFragment : BaseMvpFragment<PhotoAlbumsPresenter, IPhotoAlbumsView>(),
    IPhotoAlbumsView, VkPhotoAlbumsAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener,
    MenuProvider {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mFab: FloatingActionButton? = null
    private var mAdapter: VkPhotoAlbumsAdapter? = null
    private var mEmptyText: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_albums_gallery, container, false)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        if (!hasHideToolbarExtra()) {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        mSwipeRefreshLayout = view.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = view.findViewById(R.id.list)
        mEmptyText = view.findViewById(R.id.empty)
        val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
        recyclerView.layoutManager = GridLayoutManager(requireActivity(), columnCount)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = VkPhotoAlbumsAdapter(requireActivity(), emptyList())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        mFab = view.findViewById(R.id.fab)
        mFab?.setOnClickListener {
            presenter?.fireCreateAlbumClick()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.VK_PHOTO_ALBUMS)
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.photos)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun showDeleteConfirmDialog(album: PhotoAlbum) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.remove_confirm)
            .setMessage(R.string.album_remove_confirm_message)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireAlbumDeletingConfirmed(
                    album
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(data: List<PhotoAlbum>) {
        if (mAdapter != null) {
            mAdapter?.setData(data)
            resolveEmptyTextVisibility()
        }
    }

    override fun displayLoading(loading: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmptyText != null && mAdapter != null) {
            mEmptyText?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.photos)
    }

    override fun openAlbum(accountId: Int, album: PhotoAlbum, owner: Owner?, action: String?) {
        if (album.getObjectId() == -311) {
            getLocalServerPhotosPlace(accountId).tryOpenWith(requireActivity())
        } else {
            getVKPhotosAlbumPlace(accountId, album.ownerId, album.getObjectId(), action)
                .withParcelableExtra(Extra.ALBUM, album)
                .withParcelableExtra(Extra.OWNER, ParcelableOwnerWrapper(owner))
                .tryOpenWith(requireActivity())
        }
    }

    override fun showAlbumContextMenu(album: PhotoAlbum) {
        val items = arrayOf(getString(R.string.delete), getString(R.string.edit))
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(album.getDisplayTitle(requireActivity()))
            .setItems(items) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> presenter?.fireAlbumDeleteClick(
                        album
                    )
                    1 -> presenter?.fireAlbumEditClick(
                        album
                    )
                }
            }
            .show()
    }

    override fun doSelection(album: PhotoAlbum) {
        val result = Intent()
        result.putExtra(Extra.OWNER_ID, album.ownerId)
        result.putExtra(Extra.ALBUM_ID, album.getObjectId())
        requireActivity().setResult(Activity.RESULT_OK, result)
        requireActivity().finish()
    }

    override fun setCreateAlbumFabVisible(visible: Boolean) {
        if (mFab == null) return
        if (mFab?.isShown == true && !visible) {
            mFab?.hide()
        }
        if (mFab?.isShown == false && visible) {
            mFab?.show()
        }
    }

    override fun goToAlbumCreation(accountId: Int, ownerId: Int) {
        getCreatePhotoAlbumPlace(accountId, ownerId)
            .tryOpenWith(requireActivity())
    }

    override fun goToAlbumEditing(accountId: Int, album: PhotoAlbum, editor: PhotoAlbumEditor) {
        getEditPhotoAlbumPlace(accountId, album, editor) //.withParcelableExtra(Extra.OWNER, owner)
            .tryOpenWith(requireActivity())
    }

    override fun setDrawerPhotoSectionActive(active: Boolean) {
        if (requireActivity() is OnSectionResumeCallback) {
            if (active) {
                (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_PHOTOS)
            } else {
                (requireActivity() as OnSectionResumeCallback).onClearSelection()
            }
        }
    }

    override fun notifyItemRemoved(index: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRemoved(index)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyItemChanged(index: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemChanged(index)
        }
    }

    override fun notifyDataAdded(position: Int, size: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, size)
            resolveEmptyTextVisibility()
        }
    }

    override fun goToPhotoComments(accountId: Int, ownerId: Int) {
        getPhotoAllCommentsPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_photo_albums, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.action_photo_toggle_like).setIcon(
            if (Settings.get().other().isDisable_likes) R.drawable.ic_no_heart else R.drawable.heart
        )
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_photo_comments -> {
                presenter?.fireAllComments()
                true
            }
            R.id.action_photo_toggle_like -> {
                Settings.get().other().isDisable_likes = !Settings.get().other().isDisable_likes
                requireActivity().invalidateOptionsMenu()
                true
            }
            else -> false
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PhotoAlbumsPresenter> {
        return object : IPresenterFactory<PhotoAlbumsPresenter> {
            override fun create(): PhotoAlbumsPresenter {
                val ownerId = requireArguments().getInt(Extra.OWNER_ID)
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val wrapper: ParcelableOwnerWrapper? =
                    requireArguments().getParcelableCompat(Extra.OWNER)
                val owner = wrapper?.get()
                val action = requireArguments().getString(Extra.ACTION)
                return PhotoAlbumsPresenter(
                    accountId, ownerId, AdditionalParams()
                        .setAction(action)
                        .setOwner(owner), saveInstanceState
                )
            }
        }
    }

    override fun onVkPhotoAlbumClick(album: PhotoAlbum) {
        presenter?.fireAlbumClick(
            album
        )
    }

    override fun onVkPhotoAlbumLongClick(album: PhotoAlbum): Boolean {
        return presenter?.fireAlbumLongClick(album) ?: false
    }

    companion object {
        const val ACTION_SELECT_ALBUM = "dev.ragnarok.fenrir.ACTION_SELECT_ALBUM"
        fun newInstance(
            accountId: Int,
            ownerId: Int,
            action: String?,
            ownerWrapper: ParcelableOwnerWrapper?,
            hide_toolbar: Boolean
        ): VKPhotoAlbumsFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putParcelable(Extra.OWNER, ownerWrapper)
            args.putString(Extra.ACTION, action)
            if (hide_toolbar) args.putBoolean(EXTRA_HIDE_TOOLBAR, true)
            val fragment = VKPhotoAlbumsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}