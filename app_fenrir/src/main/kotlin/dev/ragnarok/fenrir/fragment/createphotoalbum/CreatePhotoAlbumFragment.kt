package dev.ragnarok.fenrir.fragment.createphotoalbum

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
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor
import dev.ragnarok.fenrir.view.steppers.base.AbsStepHolder
import dev.ragnarok.fenrir.view.steppers.base.AbsSteppersVerticalAdapter
import dev.ragnarok.fenrir.view.steppers.impl.*

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
        return presenter?.fireBackButtonClick() ?: false
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

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<EditPhotoAlbumPresenter> {
        return object : IPresenterFactory<EditPhotoAlbumPresenter> {
            override fun create(): EditPhotoAlbumPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                if (requireArguments().containsKey(Extra.ALBUM)) {
                    val abum: PhotoAlbum = requireArguments().getParcelableCompat(Extra.ALBUM)!!
                    val editor: PhotoAlbumEditor =
                        requireArguments().getParcelableCompat(EXTRA_EDITOR)!!
                    return EditPhotoAlbumPresenter(
                        accountId,
                        abum,
                        editor,
                        requireActivity(),
                        saveInstanceState
                    )
                } else {
                    val ownerId = requireArguments().getInt(Extra.OWNER_ID)
                    return EditPhotoAlbumPresenter(
                        accountId,
                        ownerId,
                        requireActivity(),
                        saveInstanceState
                    )
                }
            }
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
        fun buildArgsForEdit(aid: Int, album: PhotoAlbum, editor: PhotoAlbumEditor): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_EDITOR, editor)
            bundle.putParcelable(Extra.ALBUM, album)
            bundle.putInt(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun buildArgsForCreate(aid: Int, ownerId: Int): Bundle {
            val bundle = Bundle()
            bundle.putInt(Extra.OWNER_ID, ownerId)
            bundle.putInt(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun newInstance(args: Bundle?): CreatePhotoAlbumFragment {
            val fragment = CreatePhotoAlbumFragment()
            fragment.arguments = args
            return fragment
        }
    }
}