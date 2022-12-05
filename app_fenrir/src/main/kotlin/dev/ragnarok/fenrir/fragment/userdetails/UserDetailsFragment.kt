package dev.ragnarok.fenrir.fragment.userdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.RecyclerMenuAdapter
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class UserDetailsFragment : BaseMvpFragment<UserDetailsPresenter, IUserDetailsView>(),
    IUserDetailsView, RecyclerMenuAdapter.ActionListener {
    private var menuAdapter: RecyclerMenuAdapter? = null
    private var ivAvatar: ImageView? = null
    private var ivAvatarHighRes: ImageView? = null
    private var ivMail: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_details, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        ivAvatar = view.findViewById(R.id.ivAvatar)
        ivAvatarHighRes = view.findViewById(R.id.ivAvatarHighRes)
        ivMail = view.findViewById(R.id.fabOpenChat)
        ivMail?.setOnClickListener { presenter?.fireChatClick() }
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        ivAvatarHighRes?.setOnClickListener { presenter?.firePhotoClick() }
        menuAdapter = RecyclerMenuAdapter(emptyList())
        menuAdapter?.setActionListener(this)
        recyclerView.adapter = menuAdapter
        val behavior = BottomSheetBehavior.from(recyclerView)
        behavior.addBottomSheetCallback(ProfileBottomSheetCallback())
        return view
    }

    override fun openChatWith(accountId: Int, messagesOwnerId: Int, peer: Peer) {
        PlaceFactory.getChatPlace(accountId, messagesOwnerId, peer).tryOpenWith(requireActivity())
    }

    override fun displayData(items: List<AdvancedItem>) {
        menuAdapter?.setItems(items)
    }

    override fun displayToolbarTitle(user: User?) {
        if (user == null) {
            return
        }
        val actionBar = ActivityUtils.supportToolbarFor(this)
        actionBar?.title = user.fullName
        val ava: String? = Utils.firstNonEmptyString(user.photoMax, user.photo200, user.photo100)
        if (ivAvatar != null && ava != null) {
            PicassoInstance.with().load(ava)
                .into(ivAvatar ?: return)
        }
        ivMail?.visibility = if (user.canWritePrivateMessage) View.VISIBLE else View.GONE
    }

    override fun openOwnerProfile(accountId: Int, ownerId: Int, owner: Owner?) {
        PlaceFactory.getOwnerWallPlace(accountId, ownerId, owner).tryOpenWith(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<UserDetailsPresenter> =
        object : IPresenterFactory<UserDetailsPresenter> {
            override fun create(): UserDetailsPresenter {
                return UserDetailsPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.USER)!!,
                    requireArguments().getParcelableCompat("details")!!,
                    saveInstanceState
                )
            }
        }

    override fun onLongClick(item: AdvancedItem) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null) {
            val title = item.title?.getText(requireContext())
            val subtitle = item.subtitle?.getText(requireContext())
            val details = Utils.joinNonEmptyStrings("\n", title, subtitle)
            val clip: ClipData =
                if (item.type == AdvancedItem.TYPE_COPY_DETAILS_ONLY || item.type == AdvancedItem.TYPE_OPEN_URL) {
                    ClipData.newPlainText("Details", subtitle)
                } else {
                    ClipData.newPlainText("Details", details)
                }
            clipboard.setPrimaryClip(clip)
            createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
        }
    }

    private fun loadHighResWithAnimation(url: String?) {
        if (url == null) return

        PicassoInstance.with()
            .load(url)
            .into(object : BitmapTarget {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {}

                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                    ivAvatarHighRes?.setImageBitmap(bitmap)
                    ivAvatarHighRes?.fadeIn(700L) {
                        ivAvatar?.hide()
                    }
                }
            })
    }

    override fun onPhotosLoaded(photo: Photo) {
        ivAvatar?.postDelayed({
            loadHighResWithAnimation(photo.getUrlForSize(PhotoSize.W, true))
        }, 1000L)
    }

    override fun openPhotoAlbum(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        photos: ArrayList<Photo>,
        position: Int
    ) {
        PlaceFactory.getPhotoAlbumGalleryPlace(
            accountId,
            albumId,
            ownerId,
            photos,
            position,
            false,
            Settings.get().other().isInvertPhotoRev
        )
            .tryOpenWith(requireActivity())
    }

    override fun openPhotoUser(user: User) {
        PlaceFactory.getSingleURLPhotoPlace(
            user.originalAvatar,
            user.fullName,
            "id" + user.getObjectId()
        ).tryOpenWith(requireActivity())
    }

    override fun onClick(item: AdvancedItem) {
        if (item.type == AdvancedItem.TYPE_OPEN_URL) {
            val subtitle = item.subtitle?.getText(requireContext())
            if (subtitle.nonNullNoEmpty() && item.urlPrefix.nonNullNoEmpty()) {
                LinkHelper.openLinkInBrowser(requireActivity(), item.urlPrefix + "/" + subtitle)
            }
        } else {
            presenter?.fireItemClick(item)
        }
    }

    companion object {
        const val PARALLAX_COEFF = 0.5f


        fun newInstance(accountId: Int, user: User, details: UserDetails): UserDetailsFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.USER, user)
            args.putParcelable("details", details)
            val fragment = UserDetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private inner class ProfileBottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {

        private val imageHeight = resources.getDimensionPixelSize(R.dimen.profile_avatar_height)

        override fun onStateChanged(p0: View, state: Int) {

        }

        override fun onSlide(p0: View, offset: Float) {
            when {
                offset <= 0f -> return
                else -> {
                    val margin = imageHeight * -offset * PARALLAX_COEFF
                    (ivAvatar?.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                        topMargin = margin.toInt()
                        ivAvatar?.layoutParams = this
                    }
                    (ivAvatarHighRes?.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                        topMargin = margin.toInt()
                        ivAvatarHighRes?.layoutParams = this
                    }
                }
            }
        }
    }
}