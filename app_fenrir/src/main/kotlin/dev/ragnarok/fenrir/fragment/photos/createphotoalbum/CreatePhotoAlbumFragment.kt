package dev.ragnarok.fenrir.fragment.photos.createphotoalbum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.hideSoftKeyboard
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.photos.vkphotos.IVKPhotosView
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.view.steppers.base.AbsStepHolder
import dev.ragnarok.fenrir.view.steppers.base.AbsSteppersVerticalAdapter
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStep1Holder
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStep2Holder
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStep3Holder
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStep4Holder
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost

class CreatePhotoAlbumFragment : BaseMvpFragment<EditPhotoAlbumPresenter, IEditPhotoAlbumView>(),
    BackPressCallback, IEditPhotoAlbumView, CreatePhotoAlbumStep4Holder.ActionListener,
    CreatePhotoAlbumStep3Holder.ActionListener, CreatePhotoAlbumStep2Holder.ActionListener,
    CreatePhotoAlbumStep1Holder.ActionListener {
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: AbsSteppersVerticalAdapter<CreatePhotoAlbumStepsHost>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_create_photo_album, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mRecyclerView = root.findViewById(R.id.recycleView)
        mRecyclerView?.layoutManager = LinearLayoutManager(requireActivity())
        return root
    }

    override fun goToAlbum(accountId: Long, album: VKApiPhotoAlbum) {
        PlaceFactory.getVKPhotosAlbumPlace(
            accountId, album.owner_id, album.id,
            IVKPhotosView.ACTION_SHOW_PHOTOS
        )
            .withParcelableExtra(Extra.ALBUM, PhotoAlbum(album.id, album.owner_id))
            .tryOpenWith(requireActivity())
    }

    override fun goToEditedAlbum(accountId: Long, album: PhotoAlbum?, ret: Boolean?) {
        if (ret == null || !ret) return
        PlaceFactory.getVKPhotosAlbumPlace(
            accountId, (album ?: return).ownerId, album.getObjectId(),
            IVKPhotosView.ACTION_SHOW_PHOTOS
        )
            .withParcelableExtra(Extra.ALBUM, album)
            .tryOpenWith(requireActivity())
    }

    override fun updateStepView(step: Int) {
        mAdapter?.notifyItemChanged(step)
    }

    override fun moveSteppers(from: Int, to: Int) {
        mRecyclerView?.scrollToPosition(to)
        mAdapter?.notifyItemChanged(from)
        mAdapter?.notifyItemChanged(to)
    }

    override fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun hideKeyboard() {
        hideSoftKeyboard(requireActivity())
    }

    override fun updateStepButtonsAvailability(step: Int) {
        mAdapter?.updateNextButtonAvailability(step)
    }

    override fun attachSteppersHost(mHost: CreatePhotoAlbumStepsHost) {
        mAdapter = object : AbsSteppersVerticalAdapter<CreatePhotoAlbumStepsHost>(mHost, this) {
            override fun createHolderForStep(
                parent: ViewGroup,
                host: CreatePhotoAlbumStepsHost,
                step: Int
            ): AbsStepHolder<CreatePhotoAlbumStepsHost> {
                return createHolder(step, parent)
            }
        }
        mRecyclerView?.adapter = mAdapter
    }

    internal fun createHolder(
        step: Int,
        parent: ViewGroup
    ): AbsStepHolder<CreatePhotoAlbumStepsHost> {
        return when (step) {
            CreatePhotoAlbumStepsHost.STEP_TITLE_AND_DESCRIPTION -> CreatePhotoAlbumStep1Holder(
                parent, this
            )

            CreatePhotoAlbumStepsHost.STEP_UPLOAD_AND_COMMENTS -> CreatePhotoAlbumStep2Holder(
                parent, this
            )

            CreatePhotoAlbumStepsHost.STEP_PRIVACY_VIEW -> CreatePhotoAlbumStep3Holder(
                parent,
                this
            )

            CreatePhotoAlbumStepsHost.STEP_PRIVACY_COMMENT -> CreatePhotoAlbumStep4Holder(
                parent,
                this
            )

            else -> throw IllegalArgumentException("Inavalid step index: $step")
        }
    }

    override fun onBackPressed(): Boolean {
        return presenter?.fireBackButtonClick() == true
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.create_album)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onNextButtonClick(step: Int) {
        presenter?.fireStepPositiveButtonClick(
            step
        )
    }

    override fun onCancelButtonClick(step: Int) {
        presenter?.fireStepNegativeButtonClick(
            step
        )
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): EditPhotoAlbumPresenter {
        val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        if (requireArguments().containsKey(Extra.ALBUM)) {
            val abum: PhotoAlbum = requireArguments().getParcelableCompat(Extra.ALBUM)!!
            val editor: PhotoAlbumEditor =
                requireArguments().getParcelableCompat(EXTRA_EDITOR)!!
            return EditPhotoAlbumPresenter(
                accountId,
                abum,
                editor,
                saveInstanceState
            )
        } else {
            val ownerId = requireArguments().getLong(Extra.OWNER_ID)
            return EditPhotoAlbumPresenter(
                accountId,
                ownerId,
                saveInstanceState
            )
        }
    }

    override fun onPrivacyCommentClick() {
        presenter?.firePrivacyCommentClick()
    }

    override fun onPrivacyViewClick() {
        presenter?.firePrivacyViewClick()
    }

    override fun onUploadByAdminsOnlyChecked(checked: Boolean) {
        presenter?.fireUploadByAdminsOnlyChecked(
            checked
        )
    }

    override fun onCommentsDisableChecked(checked: Boolean) {
        presenter?.fireDisableCommentsClick(
            checked
        )
    }

    override fun onTitleEdited(text: CharSequence?) {
        presenter?.fireTitleEdit(
            text
        )
    }

    override fun onDescriptionEdited(text: CharSequence?) {}

    companion object {
        private const val EXTRA_EDITOR = "editor"
        fun buildArgsForEdit(aid: Long, album: PhotoAlbum, editor: PhotoAlbumEditor): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_EDITOR, editor)
            bundle.putParcelable(Extra.ALBUM, album)
            bundle.putLong(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun buildArgsForCreate(aid: Long, ownerId: Long): Bundle {
            val bundle = Bundle()
            bundle.putLong(Extra.OWNER_ID, ownerId)
            bundle.putLong(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun newInstance(args: Bundle?): CreatePhotoAlbumFragment {
            val fragment = CreatePhotoAlbumFragment()
            fragment.arguments = args
            return fragment
        }
    }
}