package dev.ragnarok.fenrir.fragment.wall.userwall

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso
import com.yalantis.ucrop.UCrop
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.activity.PhotosActivity
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalOptionsAdapter
import dev.ragnarok.fenrir.fragment.wall.AbsWallFragment
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.selection.*
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.BlurTransformation
import dev.ragnarok.fenrir.picasso.transforms.MonochromeTransformation
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunitiesPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getFriendsFollowersPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getGiftsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMarketPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMentionsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getUserDetailsPlace
import dev.ragnarok.fenrir.settings.AvatarStyle
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.InputTextDialog
import dev.ragnarok.fenrir.util.UserInfoResolveUtil
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.dp
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.getVerifiedColor
import dev.ragnarok.fenrir.util.Utils.setBackgroundTint
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView
import dev.ragnarok.fenrir.view.ProfileCoverDrawable
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import java.io.File

class UserWallFragment : AbsWallFragment<IUserWallView, UserWallPresenter>(), IUserWallView {
    private val openRequestResizeAvatar =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                lazyPresenter {
                    fireNewAvatarPhotoSelected(
                        (UCrop.getOutput(result.data ?: return@lazyPresenter)
                            ?: return@lazyPresenter).path
                    )
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                showThrowable(UCrop.getError(result.data ?: return@registerForActivityResult))
            }
        }
    private val openRequestSelectAvatar =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photos: ArrayList<LocalPhoto>? =
                    result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
                if (photos != null && photos.nonNullNoEmpty()) {
                    var to_up = photos[0].getFullImageUri() ?: return@registerForActivityResult
                    if (to_up.path?.let { File(it).isFile } == true) {
                        to_up = Uri.fromFile(to_up.path?.let { File(it) })
                    }
                    openRequestResizeAvatar.launch(
                        UCrop.of(
                            to_up,
                            Uri.fromFile(File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg"))
                        )
                            .withOptions(
                                UCrop.Options().withAspectRatio(1f, 1f).setCompressionQuality(100)
                                    .setCompressionFormat(Bitmap.CompressFormat.JPEG)
                                    .setHideBottomControls(false)
                            )
                            .getIntent(requireActivity())
                    )
                }
            }
        }
    private var mHeaderHolder: UserHeaderHolder? = null

    @SuppressLint("SetTextI18n")
    override fun displayBaseUserInfo(user: User) {
        if (mHeaderHolder == null) return
        if (user.maiden_name.isNullOrEmpty()) {
            mHeaderHolder?.tvName?.text = user.fullName
        } else {
            mHeaderHolder?.tvName?.text =
                firstNonEmptyString(user.fullName, " ") + " (" + user.maiden_name + ")"
        }
        mHeaderHolder?.tvLastSeen?.text =
            UserInfoResolveUtil.getUserActivityLine(requireActivity(), user, true)
        if (!user.canWritePrivateMessage) mHeaderHolder?.fabMessage?.setImageResource(R.drawable.close) else mHeaderHolder?.fabMessage?.setImageResource(
            R.drawable.email
        )
        val screenName = if (user.domain.nonNullNoEmpty()) "@" + user.domain else null
        mHeaderHolder?.tvScreenName?.text = screenName
        mHeaderHolder?.tvName?.setTextColor(getVerifiedColor(requireActivity(), user.isVerified))
        mHeaderHolder?.tvScreenName?.setTextColor(
            getVerifiedColor(
                requireActivity(),
                user.isVerified
            )
        )
        val photoUrl = user.maxSquareAvatar
        if (photoUrl.nonNullNoEmpty()) {
            mHeaderHolder?.ivAvatar?.let {
                val sks = with()
                    .load(photoUrl)
                    .transform(if (user.hasUnseenStories) CurrentTheme.createTransformationStrokeForAvatar() else CurrentTheme.createTransformationForAvatar())
                if (user.blacklisted) {
                    sks.transform(MonochromeTransformation())
                }
                sks.into(it)
            }
        }
        val donate_anim = Settings.get().other().donate_anim_set
        if (donate_anim > 0 && user.isDonated) {
            mHeaderHolder?.bDonate?.visibility = View.VISIBLE
            mHeaderHolder?.bDonate?.setAutoRepeat(true)
            if (donate_anim == 2) {
                val cur = Settings.get().ui().mainThemeKey
                if ("fire" == cur || "yellow_violet" == cur) {
                    mHeaderHolder?.tvName?.setTextColor(Color.parseColor("#df9d00"))
                    mHeaderHolder?.tvScreenName?.setTextColor(Color.parseColor("#df9d00"))
                    setBackgroundTint(mHeaderHolder?.ivVerified, Color.parseColor("#df9d00"))
                    mHeaderHolder?.bDonate?.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.donater_fire,
                        dp(100f),
                        dp(100f)
                    )
                } else {
                    mHeaderHolder?.tvName?.setTextColor(CurrentTheme.getColorPrimary(requireActivity()))
                    mHeaderHolder?.tvScreenName?.setTextColor(
                        CurrentTheme.getColorPrimary(
                            requireActivity()
                        )
                    )
                    setBackgroundTint(
                        mHeaderHolder?.ivVerified,
                        CurrentTheme.getColorPrimary(requireActivity())
                    )
                    mHeaderHolder?.bDonate?.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.donater_fire,
                        dp(100f),
                        dp(100f),
                        intArrayOf(0xFF812E, CurrentTheme.getColorPrimary(requireActivity())),
                        true
                    )
                }
            } else {
                mHeaderHolder?.bDonate?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.donater,
                    dp(100f),
                    dp(100f),
                    intArrayOf(
                        0xffffff,
                        CurrentTheme.getColorPrimary(requireActivity()),
                        0x777777,
                        CurrentTheme.getColorSecondary(requireActivity())
                    )
                )
            }
            mHeaderHolder?.bDonate?.playAnimation()
        } else {
            mHeaderHolder?.bDonate?.setImageDrawable(null)
            mHeaderHolder?.bDonate?.visibility = View.GONE
        }
        mHeaderHolder?.ivVerified?.visibility = if (user.isVerified) View.VISIBLE else View.GONE
        val onlineIcon = getOnlineIcon(true, user.isOnlineMobile, user.platform, user.onlineApp)
        if (!user.isOnline) mHeaderHolder?.ivOnline?.setCircleColor(
            CurrentTheme.getColorFromAttrs(
                R.attr.icon_color_inactive,
                requireContext(),
                "#000000"
            )
        ) else mHeaderHolder?.ivOnline?.setCircleColor(
            CurrentTheme.getColorFromAttrs(
                R.attr.icon_color_active,
                requireContext(),
                "#000000"
            )
        )
        if (onlineIcon != null) {
            mHeaderHolder?.ivOnline?.setIcon(onlineIcon)
        }
        if (user.blacklisted) {
            mHeaderHolder?.blacklisted?.visibility = View.VISIBLE
            if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
                mHeaderHolder?.blacklisted?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.skull,
                    dp(48f),
                    dp(48f),
                    null
                )
                mHeaderHolder?.blacklisted?.playAnimation()
            } else {
                mHeaderHolder?.blacklisted?.setImageResource(R.drawable.audio_died)
                mHeaderHolder?.blacklisted?.setColorFilter(Color.parseColor("#AAFF0000"))
            }
        } else {
            mHeaderHolder?.blacklisted?.visibility = View.GONE
            mHeaderHolder?.blacklisted?.clearAnimationDrawable()
        }
        mHeaderHolder?.blacklisted?.visibility = if (user.blacklisted) View.VISIBLE else View.GONE
    }

    private val coverTarget = object : BitmapTarget {
        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            if (isAdded) {
                mHeaderHolder?.vgCover?.let {
                    ProfileCoverDrawable.setBitmap(
                        it,
                        bitmap,
                        0.6f
                    )
                }
            }
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }

        override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            if (isAdded) {
                mHeaderHolder?.vgCover?.background = null
            }
        }
    }

    override fun displayUserCover(blacklisted: Boolean, resource: String?, supportOpen: Boolean) {
        if (!Settings.get().other().isShow_wall_cover) return
        if (supportOpen) {
            mHeaderHolder?.vgCover?.setOnLongClickListener {
                val usr = presenter?.user
                    ?: return@setOnLongClickListener false
                getSingleURLPhotoPlace(
                    resource,
                    usr.fullName,
                    "id" + usr.getOwnerObjectId()
                ).tryOpenWith(requireActivity())
                true
            }
        } else {
            mHeaderHolder?.vgCover?.setOnLongClickListener {
                false
            }
        }
        with().cancelRequest(coverTarget)
        if (resource.nonNullNoEmpty()) {
            val sks = with()
                .load(resource)
                .transform(BlurTransformation(6f, requireActivity()))
            if (blacklisted) {
                sks.transform(MonochromeTransformation())
            }
            sks.into(coverTarget)
        }
    }

    override fun openUserDetails(accountId: Long, user: User, details: UserDetails) {
        getUserDetailsPlace(accountId, user, details).tryOpenWith(requireActivity())
    }

    /*@Override
    public void displayOwnerData(User user) {
        if (mHeaderHolder == null) return;

        mHeaderHolder.tvName.setText(user.getFullName());
        mHeaderHolder.tvLastSeen.setText(UserInfoResolveUtil.getUserActivityLine(requireActivity(), user));
        mHeaderHolder.tvLastSeen.setAllCaps(false);

        String screenName = "@" + user.screen_name;
        mHeaderHolder.tvScreenName.setText(screenName);

        if (user.status_audio == null) {
            String status = "\"" + user.status + "\"";
            mHeaderHolder.tvStatus.setText(status);
        } else {
            String status = user.status_audio.artist + '-' + user.status_audio.title;
            mHeaderHolder.tvStatus.setText(status);
        }

        mHeaderHolder.tvStatus.setVisibility(isEmpty(user.status) && user.status_audio == null ? View.GONE : View.VISIBLE);

        String photoUrl = user.getMaxSquareAvatar();

        if (nonEmpty(photoUrl)) {
            PicassoInstance.with()
                    .load(photoUrl)
                    .transform(new RoundTransformation())
                    .into(mHeaderHolder.ivAvatar);
        }

        Integer onlineIcon = ViewUtils.getOnlineIcon(user.online, user.online_mobile, user.platform, user.online_app);
        mHeaderHolder.ivOnline.setVisibility(user.online ? View.VISIBLE : View.GONE);

        if (onlineIcon != null) {
            mHeaderHolder.ivOnline.setIcon(onlineIcon);
        }

        */
    /*View mainUserInfoView = mHeaderHolder.infoSections.findViewById(R.id.section_contact_info);
        UserInfoResolveUtil.fillMainUserInfo(requireActivity(), mainUserInfoView, user, new LinkActionAdapter() {
            @Override
            public void onOwnerClick(int ownerId) {
                onOpenOwner(ownerId);
            }
        });

        UserInfoResolveUtil.fill(requireActivity(), mHeaderHolder.infoSections.findViewById(R.id.section_beliefs), user);
        UserInfoResolveUtil.fillPersonalInfo(requireActivity(), mHeaderHolder.infoSections.findViewById(R.id.section_personal), user);*/
    /*

        SelectionUtils.addSelectionProfileSupport(getContext(), mHeaderHolder.avatarRoot, user);
    }*/

    override fun showRegistrationDate(
        @StringRes info: Int,
        registered: String?,
        auth: String?,
        changes: String?
    ) {
        MaterialAlertDialogBuilder(requireActivity())
            .setIcon(R.drawable.dir_person)
            .setMessage(
                getString(
                    info,
                    registered,
                    auth,
                    changes
                )
            )
            .setTitle(getString(R.string.registration_date))
            .setCancelable(true)
            .show()
    }

    override fun displayCounters(
        friends: Int,
        mutual: Int,
        followers: Int,
        groups: Int,
        photos: Int,
        audios: Int,
        videos: Int,
        articles: Int,
        products: Int,
        gifts: Int,
        products_services: Int,
        narratives: Int,
        clips: Int
    ) {
        if (mHeaderHolder != null) {
            if (Settings.get().other().isShow_mutual_count) {
                setupCounterWith(mHeaderHolder?.bFriends, friends, mutual)
            } else {
                setupCounter(mHeaderHolder?.bFriends, friends)
            }
            setupCounter(mHeaderHolder?.bGroups, groups)
            setupCounter(mHeaderHolder?.bPhotos, photos)
            setupCounter(mHeaderHolder?.bAudios, audios)
            setupCounter(mHeaderHolder?.bVideos, videos)
            setupCounter(mHeaderHolder?.bArticles, articles)
            setupCounter(mHeaderHolder?.bGifts, gifts)
            setupCounter(mHeaderHolder?.bNarratives, narratives)
            setupCounter(mHeaderHolder?.bClips, clips)
        }
    }

    override fun displayUserStatus(statusText: String?, swAudioIcon: Boolean) {
        mHeaderHolder?.tvStatus?.text = statusText
        mHeaderHolder?.tvAudioStatus?.visibility =
            if (swAudioIcon) View.VISIBLE else View.GONE
    }

    override fun headerLayout(): Int {
        return R.layout.header_user_profile
    }

    override fun onHeaderInflated(headerRootView: View) {
        mHeaderHolder = UserHeaderHolder(headerRootView)
        mHeaderHolder?.ivAvatar?.setOnClickListener {
            presenter?.fireAvatarClick()
        }
        mHeaderHolder?.ivAvatar?.setOnLongClickListener {
            presenter?.fireAvatarLongClick()
            true
        }
        setupPaganContent(mHeaderHolder?.Runes, mHeaderHolder?.paganSymbol)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<UserWallPresenter> {
        return object : IPresenterFactory<UserWallPresenter> {
            override fun create(): UserWallPresenter {
                requireArguments()
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                val wrapper: ParcelableOwnerWrapper? =
                    requireArguments().getParcelableCompat(Extra.OWNER)
                return UserWallPresenter(
                    accountId,
                    ownerId,
                    wrapper?.get() as User?,
                    saveInstanceState
                )
            }
        }
    }

    override fun displayWallFilters(filters: MutableList<PostFilter>) {
        mHeaderHolder?.mPostFilterAdapter?.setItems(filters)
    }

    override fun notifyWallFiltersChanged() {
        mHeaderHolder?.mPostFilterAdapter?.notifyDataSetChanged()
    }

    override fun setupPrimaryActionButton(@StringRes resourceId: Int?) {
        if (mHeaderHolder != null && resourceId != null) {
            mHeaderHolder?.bPrimaryAction?.setText(resourceId)
        }
    }

    override fun openFriends(accountId: Long, userId: Long, tab: Int, counters: FriendsCounters?) {
        getFriendsFollowersPlace(accountId, userId, tab, counters).tryOpenWith(requireActivity())
    }

    override fun openGroups(accountId: Long, userId: Long, user: User?) {
        getCommunitiesPlace(accountId, userId)
            .withParcelableExtra(Extra.USER, user)
            .tryOpenWith(requireActivity())
    }

    override fun openProducts(accountId: Long, ownerId: Long, owner: Owner?) {
        getMarketPlace(accountId, ownerId, 0, false).tryOpenWith(requireActivity())
    }

    override fun openProductServices(accountId: Long, ownerId: Long) {
        getMarketPlace(accountId, ownerId, 0, true).tryOpenWith(requireActivity())
    }

    override fun openGifts(accountId: Long, ownerId: Long, owner: Owner?) {
        getGiftsPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    override fun showEditStatusDialog(initialValue: String?) {
        InputTextDialog.Builder(requireActivity())
            .setInputType(InputType.TYPE_CLASS_TEXT)
            .setTitleRes(R.string.edit_status)
            .setHint(R.string.enter_your_status)
            .setValue(initialValue)
            .setAllowEmpty(true)
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    presenter?.fireNewStatusEntered(
                        newValue
                    )
                }

                override fun onCanceled() {
                }
            })
            .show()
    }

    override fun showAddToFriendsMessageDialog() {
        InputTextDialog.Builder(requireActivity())
            .setInputType(InputType.TYPE_CLASS_TEXT)
            .setTitleRes(R.string.add_to_friends)
            .setHint(R.string.attach_message)
            .setAllowEmpty(true)
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    presenter?.fireAddToFrindsClick(
                        newValue
                    )
                }

                override fun onCanceled() {
                }
            })
            .show()
    }

    override fun showDeleteFromFriendsMessageDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.delete_from_friends)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireDeleteFromFriends()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .show()
    }

    override fun showUnbanMessageDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.is_to_blacklist)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireRemoveBlacklistClick()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .show()
    }

    override fun showAvatarContextMenu(canUploadAvatar: Boolean) {
        val items: Array<String> = if (canUploadAvatar) {
            arrayOf(
                getString(R.string.open_photo),
                getString(R.string.open_avatar),
                getString(R.string.upload_new_photo),
                getString(R.string.upload_new_story)
            )
        } else {
            arrayOf(getString(R.string.open_photo), getString(R.string.open_avatar))
        }
        MaterialAlertDialogBuilder(requireActivity()).setItems(items) { _: DialogInterface?, i: Int ->
            when (i) {
                0 -> presenter?.fireOpenAvatarsPhotoAlbum()
                1 -> {
                    val usr = presenter?.user
                        ?: return@setItems
                    getSingleURLPhotoPlace(
                        usr.originalAvatar,
                        usr.fullName,
                        "id" + usr.getOwnerObjectId()
                    ).tryOpenWith(requireActivity())
                }

                2 -> {
                    val attachPhotoIntent = Intent(requireActivity(), PhotosActivity::class.java)
                    attachPhotoIntent.putExtra(PhotosActivity.EXTRA_MAX_SELECTION_COUNT, 1)
                    openRequestSelectAvatar.launch(attachPhotoIntent)
                }

                3 -> {
                    presenter?.updateToStory(null)
                    requestUploadStory()
                }
            }
        }.setCancelable(true).show()
    }

    override fun showMention(accountId: Long, ownerId: Long) {
        getMentionsPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    override fun invalidateOptionsMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.profile)
        setToolbarSubtitle(this, null)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        val view = OptionView()
        presenter?.fireOptionViewCreated(
            view
        )
        menu.add(R.string.registration_date).setOnMenuItemClickListener {
            presenter?.fireGetRegistrationDate()
            true
        }
        menu.add(R.string.rename).setOnMenuItemClickListener {
            InputTextDialog.Builder(requireActivity())
                .setTitleRes(R.string.rename_local)
                .setAllowEmpty(true)
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .setCallback(object : InputTextDialog.Callback {
                    override fun onChanged(newValue: String?) {
                        presenter?.renameLocal(
                            newValue
                        )
                    }

                    override fun onCanceled() {
                    }
                })
                .show()
            true
        }
        if (!view.isMy) {
            menu.add(R.string.report).setOnMenuItemClickListener {
                presenter?.fireReport(requireActivity())
                true
            }
            if (!view.isBlacklistedByMe) {
                menu.add(R.string.add_to_blacklist).setOnMenuItemClickListener {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.add_to_blacklist)
                        .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                            presenter?.fireAddToBlacklistClick()
                        }
                        .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                        .show()
                    true
                }
            }
            if (!view.isSubscribed) {
                menu.add(R.string.notify_wall_added).setOnMenuItemClickListener {
                    presenter?.fireSubscribe()
                    true
                }
            } else {
                menu.add(R.string.unnotify_wall_added)
                    .setOnMenuItemClickListener {
                        presenter?.fireUnSubscribe()
                        true
                    }
            }
            if (!view.isFavorite) {
                menu.add(R.string.add_to_bookmarks).setOnMenuItemClickListener {
                    presenter?.fireAddToBookmarks()
                    true
                }
            } else {
                menu.add(R.string.remove_from_bookmarks)
                    .setOnMenuItemClickListener {
                        presenter?.fireRemoveFromBookmarks()
                        true
                    }
            }
        }
        menu.add(R.string.mentions).setOnMenuItemClickListener {
            presenter?.fireMentions()
            true
        }
    }

    private inner class UserHeaderHolder(root: View) {
        val vgCover: ViewGroup = root.findViewById(R.id.cover)
        val ivAvatar: ImageView = root.findViewById(R.id.avatar)
        val ivVerified: ImageView = root.findViewById(R.id.item_verified)
        val tvName: TextView = root.findViewById(R.id.fragment_user_profile_name)
        val tvScreenName: TextView = root.findViewById(R.id.fragment_user_profile_id)
        val tvStatus: TextView = root.findViewById(R.id.fragment_user_profile_status)
        val tvAudioStatus: ImageView = root.findViewById(R.id.fragment_user_profile_audio)
        val tvLastSeen: TextView = root.findViewById(R.id.fragment_user_profile_activity)
        val ivOnline: OnlineView = root.findViewById(R.id.header_navi_menu_online)
        val blacklisted: RLottieImageView = root.findViewById(R.id.item_blacklisted)
        val bFriends: TextView = root.findViewById(R.id.fragment_user_profile_bfriends)
        val bGroups: TextView = root.findViewById(R.id.fragment_user_profile_bgroups)
        val bPhotos: TextView = root.findViewById(R.id.fragment_user_profile_bphotos)
        val bVideos: TextView = root.findViewById(R.id.fragment_user_profile_bvideos)
        val bAudios: TextView = root.findViewById(R.id.fragment_user_profile_baudios)
        val bArticles: TextView = root.findViewById(R.id.fragment_user_profile_barticles)
        val bNarratives: TextView = root.findViewById(R.id.fragment_user_profile_bnarratives)
        val bClips: TextView = root.findViewById(R.id.fragment_user_profile_bclips)
        val bGifts: TextView = root.findViewById(R.id.fragment_user_profile_bgifts)
        val fabMessage: FloatingActionButton =
            root.findViewById(R.id.header_user_profile_fab_message)
        val fabMoreInfo: FloatingActionButton = root.findViewById(R.id.info_btn)
        val bPrimaryAction: MaterialButton = root.findViewById(R.id.subscribe_btn)
        val bDonate: RLottieImageView = root.findViewById(R.id.donated_anim)
        val paganSymbol: RLottieImageView = root.findViewById(R.id.pagan_symbol)
        val Runes: View = root.findViewById(R.id.runes_container)
        val mPostFilterAdapter: HorizontalOptionsAdapter<PostFilter>

        init {
            ivAvatar.setBackgroundResource(
                if (Settings.get()
                        .ui().avatarStyle == AvatarStyle.OVAL
                ) R.drawable.sel_button_square_5_white else R.drawable.sel_button_round_5_white
            )
            val filtersList: RecyclerView = root.findViewById(R.id.post_filter_recyclerview)
            filtersList.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
            mPostFilterAdapter = HorizontalOptionsAdapter(mutableListOf())
            mPostFilterAdapter.setListener(object : HorizontalOptionsAdapter.Listener<PostFilter> {
                override fun onOptionClick(entry: PostFilter) {
                    presenter?.fireFilterClick(
                        entry
                    )
                }
            })
            filtersList.adapter = mPostFilterAdapter
            tvStatus.setOnClickListener {
                presenter?.fireStatusClick()
            }
            fabMoreInfo.setOnClickListener {
                presenter?.fireMoreInfoClick()
            }
            bPrimaryAction.setOnClickListener {
                presenter?.firePrimaryActionsClick()
            }
            fabMessage.setOnClickListener {
                presenter?.fireChatClick()
            }
            root.findViewById<View>(R.id.horiz_scroll).clipToOutline = true
            root.findViewById<View>(R.id.header_user_profile_photos_container)
                .setOnClickListener {
                    presenter?.fireHeaderPhotosClick()
                }
            root.findViewById<View>(R.id.header_user_profile_friends_container)
                .setOnClickListener {
                    presenter?.fireHeaderFriendsClick()
                }
            root.findViewById<View>(R.id.header_user_profile_audios_container)
                .setOnClickListener {
                    presenter?.fireHeaderAudiosClick()
                }
            root.findViewById<View>(R.id.header_user_profile_articles_container)
                .setOnClickListener {
                    presenter?.fireHeaderArticlesClick()
                }
            root.findViewById<View>(R.id.header_user_profile_groups_container)
                .setOnClickListener {
                    presenter?.fireHeaderGroupsClick()
                }
            root.findViewById<View>(R.id.header_user_profile_videos_container)
                .setOnClickListener {
                    presenter?.fireHeaderVideosClick()
                }
            root.findViewById<View>(R.id.header_user_profile_gifts_container)
                .setOnClickListener {
                    presenter?.fireHeaderGiftsClick()
                }
            root.findViewById<View>(R.id.header_user_profile_narratives_container)
                .setOnClickListener {
                    presenter?.fireNarrativesClick()
                }
            root.findViewById<View>(R.id.header_user_profile_clips_container).let {
                it.setOnClickListener {
                    presenter?.fireClipsClick()
                }
                it.visibility = if (Utils.isOfficialVKCurrent) View.VISIBLE else View.GONE
            }
        }
    }
}