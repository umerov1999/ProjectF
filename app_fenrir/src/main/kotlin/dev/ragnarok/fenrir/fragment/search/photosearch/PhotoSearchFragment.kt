package dev.ragnarok.fenrir.fragment.search.photosearch

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils

class PhotoSearchFragment :
    AbsSearchFragment<PhotoSearchPresenter, IPhotoSearchView, Photo, SearchPhotosAdapter>(),
    SearchPhotosAdapter.PhotosActionListener, IPhotoSearchView {
    override fun setAdapterData(adapter: SearchPhotosAdapter, data: MutableList<Photo>) {
        adapter.setData(data)
    }

    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            val ps =
                ((result.data ?: return@registerForActivityResult).extras
                    ?: return@registerForActivityResult).getInt(Extra.POSITION)
            mAdapter?.updateCurrentPosition(ps)
            recyclerView?.scrollToPosition(ps)
        }
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Photo>): SearchPhotosAdapter {
        val adapter = SearchPhotosAdapter(requireActivity(), data, TAG)
        adapter.setPhotosActionListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        if (Settings.get().main().single_line_photos) {
            return Utils.getSingleElementsLayoutManager(
                requireActivity()
            )
        }
        val columnCount = resources.getInteger(R.integer.local_gallery_column_count)
        return GridLayoutManager(requireActivity(), columnCount)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?) = PhotoSearchPresenter(
        requireArguments().getLong(Extra.ACCOUNT_ID),
        requireArguments().getParcelableCompat(Extra.CRITERIA),
        saveInstanceState
    )

    override fun displayGallery(accountId: Long, photos: ArrayList<Photo>, position: Int) {
        getSimpleGalleryPlace(accountId, photos, position, true).setActivityResultLauncher(
            requestPhotoUpdate
        ).tryOpenWith(requireActivity())
    }

    override fun displayGalleryNative(accountId: Long, photos: Long, position: Int) {
        getSimpleGalleryPlace(accountId, photos, position, true).setActivityResultLauncher(
            requestPhotoUpdate
        ).tryOpenWith(requireActivity())
    }

    override fun onPhotoClick(holder: SearchPhotosAdapter.PhotoViewHolder, photo: Photo) {
        presenter?.firePhotoClick(
            photo
        )
    }

    companion object {
        private val TAG = PhotoSearchFragment::class.simpleName.orEmpty()


        fun newInstance(
            accountId: Long,
            initialCriteria: PhotoSearchCriteria?
        ): PhotoSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = PhotoSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}