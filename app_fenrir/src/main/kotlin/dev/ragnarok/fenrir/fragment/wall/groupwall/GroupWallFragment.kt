package dev.ragnarok.fenrir.fragment.wall.groupwall

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.google.android.material.textview.MaterialTextView
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.activity.LoginActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.LoginActivity.Companion.extractGroupTokens
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalMenuAdapter
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalOptionsAdapter
import dev.ragnarok.fenrir.fragment.docs.DocsListPresenter
import dev.ragnarok.fenrir.fragment.wall.AbsWallFragment
import dev.ragnarok.fenrir.fragment.wall.groupwall.IGroupWallView.IOptionMenuView
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.CommunityDetails
import dev.ragnarok.fenrir.model.GroupSettings
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper
import dev.ragnarok.fenrir.model.PostFilter
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.BlurTransformation
import dev.ragnarok.fenrir.picasso.transforms.MonochromeTransformation
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunityControlPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getDialogsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getDocumentsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getGroupChatsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMarketAlbumPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMarketPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getShowCommunityInfoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getShowCommunityLinksInfoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleURLPhotoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getTopicsPlace
import dev.ragnarok.fenrir.settings.AvatarStyle
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.dp
import dev.ragnarok.fenrir.util.Utils.getVerifiedColor
import dev.ragnarok.fenrir.util.Utils.setBackgroundTint
import dev.ragnarok.fenrir.view.ProfileCoverDrawable
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import kotlin.math.abs

class GroupWallFragment : AbsWallFragment<IGroupWallView, GroupWallPresenter>(), IGroupWallView {
    private val requestCommunity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val tokens = extractGroupTokens(
                result.data ?: return@registerForActivityResult
            )
            lazyPresenter {
                fireGroupTokensReceived(tokens ?: return@lazyPresenter)
            }
        }
    }
    private val ownerLinkAdapter: OwnerLinkSpanFactory.ActionListener =
        object : LinkActionAdapter() {
            override fun onOwnerClick(ownerId: Long) {
                presenter?.fireOwnerClick(
                    ownerId
                )
            }
        }
    private var mHeaderHolder: GroupHeaderHolder? = null
    override fun displayBaseCommunityData(community: Community, details: CommunityDetails) {
        if (mHeaderHolder == null) return
        mHeaderHolder?.tvName?.text = community.fullName
        details.getCover()?.getImages().ifNonNullNoEmpty({
            var def = 0
            var url: String? = null
            for (i in it) {
                if (i.getWidth() * i.getHeight() > def) {
                    def = i.getWidth() * i.getHeight()
                    url = i.getUrl()
                }
            }
            displayCommunityCover(community.isBlacklisted, url, true)
        }, {
            displayCommunityCover(community.isBlacklisted, community.maxSquareAvatar, false)
        })
        val statusText: String? = if (details.getStatusAudio() != null) {
            details.getStatusAudio()?.artistAndTitle
        } else {
            details.getStatus()
        }
        mHeaderHolder?.tvStatus?.text = statusText
        mHeaderHolder?.tvAudioStatus?.visibility =
            if (details.getStatusAudio() != null) View.VISIBLE else View.GONE
        val domain =
            if (community.domain.nonNullNoEmpty()) "@" + community.domain else ("club" + community.id)
        mHeaderHolder?.tvDomain?.text = domain
        mHeaderHolder?.tvName?.setTextColor(
            getVerifiedColor(
                requireActivity(),
                community.isVerified
            )
        )
        mHeaderHolder?.tvDomain?.setTextColor(
            getVerifiedColor(
                requireActivity(),
                community.isVerified
            )
        )
        val donate_anim = Settings.get().other().donate_anim_set
        if (donate_anim > 0 && community.isDonated) {
            mHeaderHolder?.bDonate?.visibility = View.VISIBLE
            mHeaderHolder?.bDonate?.setAutoRepeat(true)
            if (donate_anim == 2) {
                val cur = Settings.get().ui().mainThemeKey
                if ("fire" == cur || "yellow_violet" == cur) {
                    mHeaderHolder?.tvName?.setTextColor(Color.parseColor("#df9d00"))
                    mHeaderHolder?.tvDomain?.setTextColor(Color.parseColor("#df9d00"))
                    setBackgroundTint(mHeaderHolder?.ivVerified, Color.parseColor("#df9d00"))
                    mHeaderHolder?.bDonate?.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.donater_fire,
                        dp(100f),
                        dp(100f)
                    )
                } else {
                    mHeaderHolder?.tvName?.setTextColor(CurrentTheme.getColorPrimary(requireActivity()))
                    mHeaderHolder?.tvDomain?.setTextColor(
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
        mHeaderHolder?.ivVerified?.visibility =
            if (community.isVerified) View.VISIBLE else View.GONE
        if (!details.isCanMessage()) mHeaderHolder?.fabMessage?.setImageResource(R.drawable.close) else mHeaderHolder?.fabMessage?.setImageResource(
            R.drawable.email
        )
        val photoUrl = community.maxSquareAvatar
        if (photoUrl.nonNullNoEmpty()) {
            mHeaderHolder?.ivAvatar?.let {
                val sks = with()
                    .load(photoUrl)
                    .transform(if (community.hasUnseenStories) CurrentTheme.createTransformationStrokeForAvatar() else CurrentTheme.createTransformationForAvatar())
                if (community.isBlacklisted) {
                    sks.transform(MonochromeTransformation())
                }
                sks.into(it)
            }
        }
        mHeaderHolder?.ivAvatar?.setOnClickListener {
            presenter?.fireAvatarPhotoClick(null, null)
        }
        mHeaderHolder?.ivAvatar?.setOnLongClickListener {
            presenter?.fireMentions()
            true
        }
        if (community.isBlacklisted) {
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
        mHeaderHolder?.blacklisted?.visibility =
            if (community.isBlacklisted) View.VISIBLE else View.GONE
    }

    override fun onSinglePhoto(ava: String, prefix: String?, community: Community) {
        getSingleURLPhotoPlace(
            ava,
            community.fullName,
            prefix.orEmpty() + "club" + abs(community.id)
        ).tryOpenWith(requireActivity())
    }

    override fun openVKURL(accountId: Long, link: String) {
        LinkHelper.openUrl(requireActivity(), accountId, link, false)
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

    private fun displayCommunityCover(
        blacklisted: Boolean,
        resource: String?,
        supportOpen: Boolean
    ) {
        if (!Settings.get().other().isShow_wall_cover) return
        if (supportOpen) {
            mHeaderHolder?.vgCover?.setOnLongClickListener {
                presenter?.fireAvatarPhotoClick(resource, null)
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

    override fun invalidateOptionsMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<GroupWallPresenter> {
        return object : IPresenterFactory<GroupWallPresenter> {
            override fun create(): GroupWallPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                val wrapper: ParcelableOwnerWrapper? =
                    requireArguments().getParcelableCompat(Extra.OWNER)
                return GroupWallPresenter(
                    accountId,
                    ownerId,
                    wrapper?.get() as Community?,
                    saveInstanceState
                )
            }
        }
    }

    override fun headerLayout(): Int {
        return R.layout.header_group
    }

    override fun onHeaderInflated(headerRootView: View) {
        mHeaderHolder = GroupHeaderHolder(headerRootView)
        setupPaganContent(mHeaderHolder?.Runes, mHeaderHolder?.paganSymbol)
    }

    override fun setupPrimaryButton(@StringRes title: Int?) {
        if (mHeaderHolder != null) {
            if (title != null) {
                mHeaderHolder?.primaryActionButton?.setText(title)
                mHeaderHolder?.primaryActionButton?.visibility = View.VISIBLE
            } else {
                mHeaderHolder?.primaryActionButton?.visibility = View.GONE
            }
        }
    }

    override fun setupSecondaryButton(@StringRes title: Int?) {
        if (mHeaderHolder != null) {
            if (title != null) {
                mHeaderHolder?.secondaryActionButton?.setText(title)
                mHeaderHolder?.secondaryActionButton?.visibility = View.VISIBLE
            } else {
                mHeaderHolder?.secondaryActionButton?.visibility = View.GONE
            }
        }
    }

    override fun openTopics(accountId: Long, ownerId: Long, owner: Owner?) {
        getTopicsPlace(accountId, ownerId)
            .withParcelableExtra(Extra.OWNER, owner)
            .tryOpenWith(requireActivity())
    }

    override fun openCommunityMembers(accountId: Long, groupId: Long) {
        PlaceFactory.getCommunityMembersPlace(accountId, groupId).tryOpenWith(requireActivity())
    }

    override fun openDocuments(accountId: Long, ownerId: Long, owner: Owner?) {
        getDocumentsPlace(accountId, ownerId, DocsListPresenter.ACTION_SHOW)
            .withParcelableExtra(Extra.OWNER, owner)
            .tryOpenWith(requireActivity())
    }

    override fun displayWallFilters(filters: MutableList<PostFilter>) {
        mHeaderHolder?.mFiltersAdapter?.setItems(filters)
    }

    override fun notifyWallFiltersChanged() {
        mHeaderHolder?.mFiltersAdapter?.notifyDataSetChanged()
    }

    override fun displayWallMenus(menus: MutableList<CommunityDetails.Menu>) {
        mHeaderHolder?.mMenuAdapter?.setItems(menus)
        mHeaderHolder?.menuList?.visibility = if (menus.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun notifyWallMenusChanged(hidden: Boolean) {
        mHeaderHolder?.mMenuAdapter?.notifyDataSetChanged()
        mHeaderHolder?.menuList?.visibility = if (hidden) View.GONE else View.VISIBLE
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.menu_community_wall, menu)
        val optionMenuView = OptionMenuView()
        presenter?.fireOptionMenuViewCreated(
            optionMenuView
        )
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
        if (!optionMenuView.pIsSubscribed) {
            menu.add(R.string.notify_wall_added).setOnMenuItemClickListener {
                presenter?.fireSubscribe()
                true
            }
        } else {
            menu.add(R.string.unnotify_wall_added).setOnMenuItemClickListener {
                presenter?.fireUnSubscribe()
                true
            }
        }
        if (!optionMenuView.pIsFavorite) {
            menu.add(R.string.add_to_bookmarks).setOnMenuItemClickListener {
                presenter?.fireAddToBookmarksClick()
                true
            }
        } else {
            menu.add(R.string.remove_from_bookmarks).setOnMenuItemClickListener {
                presenter?.fireRemoveFromBookmarks()
                true
            }
        }
        menu.add(R.string.mentions).setOnMenuItemClickListener {
            presenter?.fireMentions()
            true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_community_control) {
            presenter?.fireCommunityControlClick()
            return true
        }
        if (menuItem.itemId == R.id.action_community_messages) {
            presenter?.fireCommunityMessagesClick()
            return true
        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.community)
        setToolbarSubtitle(this, null)
    }

    override fun goToCommunityControl(
        accountId: Long,
        community: Community,
        settings: GroupSettings?
    ) {
        getCommunityControlPlace(accountId, community, settings).tryOpenWith(requireActivity())
    }

    override fun goToShowCommunityInfo(accountId: Long, community: Community) {
        getShowCommunityInfoPlace(accountId, community).tryOpenWith(requireActivity())
    }

    override fun goToShowCommunityLinksInfo(accountId: Long, community: Community) {
        getShowCommunityLinksInfoPlace(accountId, community).tryOpenWith(requireActivity())
    }

    override fun goToShowCommunityAboutInfo(accountId: Long, details: CommunityDetails) {
        if (details.getDescription().isNullOrEmpty()) {
            return
        }
        val root = View.inflate(requireActivity(), R.layout.dialog_selectable_text, null)
        val tvText: MaterialTextView = root.findViewById(R.id.selectable_text)
        val subtitle =
            OwnerLinkSpanFactory.withSpans(
                details.getDescription(),
                owners = true,
                topics = false,
                listener = ownerLinkAdapter
            )
        tvText.setText(subtitle, TextView.BufferType.SPANNABLE)
        tvText.movementMethod = LinkMovementMethod.getInstance()
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.description_hint)
            .setView(root)
            .setPositiveButton(R.string.button_ok, null)
            .show()
    }

    override fun goToGroupChats(accountId: Long, community: Community) {
        getGroupChatsPlace(accountId, abs(community.id)).tryOpenWith(requireActivity())
    }

    override fun startLoginCommunityActivity(groupId: Long) {
        val intent = createIntent(
            requireActivity(),
            "2685278",
            "messages,photos,docs,manage",
            listOf(groupId)
        )
        requestCommunity.launch(intent)
    }

    override fun openCommunityDialogs(accountId: Long, groupId: Long, subtitle: String?) {
        getDialogsPlace(accountId, -groupId, subtitle).tryOpenWith(requireActivity())
    }

    override fun displayCounters(
        members: Int,
        topics: Int,
        docs: Int,
        photos: Int,
        audio: Int,
        video: Int,
        articles: Int,
        products: Int,
        chats: Int,
        products_services: Int,
        narratives: Int,
        clips: Int
    ) {
        if (mHeaderHolder == null) return
        setupCounter(mHeaderHolder?.bTopics, topics)
        setupCounter(mHeaderHolder?.bMembers, members)
        setupCounter(mHeaderHolder?.bDocuments, docs)
        setupCounter(mHeaderHolder?.bPhotos, photos)
        setupCounter(mHeaderHolder?.bAudios, audio)
        setupCounter(mHeaderHolder?.bVideos, video)
        setupCounter(mHeaderHolder?.bArticles, articles)
        setupCounter(mHeaderHolder?.bProducts, products)
        setupCounter(mHeaderHolder?.bProductServices, products_services)
        setupCounter(mHeaderHolder?.bNarratives, narratives)
        setupCounter(mHeaderHolder?.bClips, clips)
        setupCounterFlow(mHeaderHolder?.bChats, mHeaderHolder?.bChatsContainer, chats)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val optionMenuView = OptionMenuView()
        presenter?.fireOptionMenuViewCreated(
            optionMenuView
        )
        menu.findItem(R.id.action_community_control).isVisible = optionMenuView.pControlVisible
    }

    override fun openProducts(accountId: Long, ownerId: Long, owner: Owner?) {
        getMarketAlbumPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    override fun openProductServices(accountId: Long, ownerId: Long) {
        getMarketPlace(accountId, ownerId, 0, true).tryOpenWith(requireActivity())
    }

    private class OptionMenuView : IOptionMenuView {
        var pControlVisible = false
        var pIsFavorite = false
        var pIsSubscribed = false
        override fun setControlVisible(visible: Boolean) {
            pControlVisible = visible
        }

        override fun setIsFavorite(favorite: Boolean) {
            pIsFavorite = favorite
        }

        override fun setIsSubscribed(subscribed: Boolean) {
            pIsSubscribed = subscribed
        }
    }

    private inner class GroupHeaderHolder(root: View) {
        val blacklisted: RLottieImageView = root.findViewById(R.id.item_blacklisted)
        val vgCover: ViewGroup = root.findViewById(R.id.cover)
        val ivAvatar: ImageView = root.findViewById(R.id.header_group_avatar)
        val ivVerified: ImageView = root.findViewById(R.id.item_verified)
        val bDonate: RLottieImageView = root.findViewById(R.id.donated_anim)
        val tvName: TextView = root.findViewById(R.id.header_group_name)
        val tvStatus: TextView = root.findViewById(R.id.header_group_status)
        val tvAudioStatus: ImageView = root.findViewById(R.id.fragment_group_audio)
        val tvDomain: TextView = root.findViewById(R.id.header_group_id)
        val bTopics: TextView = root.findViewById(R.id.header_group_btopics)
        val bArticles: TextView = root.findViewById(R.id.header_group_barticles)
        val bChats: TextView = root.findViewById(R.id.header_group_bchats)
        val bChatsContainer: ViewGroup = root.findViewById(R.id.header_group_chats_container)
        val bProducts: TextView = root.findViewById(R.id.header_group_bproducts)
        val bProductServices: TextView = root.findViewById(R.id.header_group_bservices_products)
        val bNarratives: TextView = root.findViewById(R.id.header_group_bnarratives)
        val bClips: TextView = root.findViewById(R.id.header_group_bclips)
        val bMembers: TextView = root.findViewById(R.id.header_group_bmembers)
        val bDocuments: TextView = root.findViewById(R.id.header_group_bdocuments)
        val bPhotos: TextView = root.findViewById(R.id.header_group_bphotos)
        val bAudios: TextView = root.findViewById(R.id.header_group_baudios)
        val bVideos: TextView = root.findViewById(R.id.header_group_bvideos)
        val primaryActionButton: MaterialButton =
            root.findViewById(R.id.header_group_primary_button)
        val secondaryActionButton: MaterialButton =
            root.findViewById(R.id.header_group_secondary_button)
        val fabMessage: FloatingActionButton = root.findViewById(R.id.header_group_fab_message)
        val mFiltersAdapter: HorizontalOptionsAdapter<PostFilter>
        val mMenuAdapter: HorizontalMenuAdapter
        val menuList: RecyclerView = root.findViewById(R.id.menu_recyclerview)
        val paganSymbol: RLottieImageView = root.findViewById(R.id.pagan_symbol)
        val Runes: View = root.findViewById(R.id.runes_container)

        init {
            ivAvatar.setBackgroundResource(
                if (Settings.get()
                        .ui().avatarStyle == AvatarStyle.OVAL
                ) R.drawable.sel_button_square_5_white else R.drawable.sel_button_round_5_white
            )
            val filterList: RecyclerView = root.findViewById(R.id.post_filter_recyclerview)
            filterList.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
            mFiltersAdapter = HorizontalOptionsAdapter(mutableListOf())
            mFiltersAdapter.setListener(object : HorizontalOptionsAdapter.Listener<PostFilter> {
                override fun onOptionClick(entry: PostFilter) {
                    presenter?.fireFilterEntryClick(entry)
                }
            })
            filterList.adapter = mFiltersAdapter

            menuList.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
            mMenuAdapter = HorizontalMenuAdapter(mutableListOf())
            mMenuAdapter.setListener(object : HorizontalMenuAdapter.Listener {
                override fun onWallMenuClick(item: CommunityDetails.Menu, pos: Int) {
                    presenter?.fireMenuClick(item)
                }

                override fun onWallMenuLongClick(item: CommunityDetails.Menu, pos: Int) {
                    presenter?.fireAvatarPhotoClick(item.cover, "menu_")
                }
            })
            menuList.adapter = mMenuAdapter
            tvStatus.setOnClickListener {
                presenter?.fireHeaderStatusClick()
            }
            fabMessage.setOnClickListener {
                presenter?.fireChatClick()
            }
            secondaryActionButton.setOnClickListener {
                presenter?.fireSecondaryButtonClick()
            }
            primaryActionButton.setOnClickListener {
                presenter?.firePrimaryButtonClick()
            }
            root.findViewById<View>(R.id.header_group_photos_container)
                .setOnClickListener {
                    presenter?.fireHeaderPhotosClick()
                }
            root.findViewById<View>(R.id.header_group_videos_container)
                .setOnClickListener {
                    presenter?.fireHeaderVideosClick()
                }
            root.findViewById<View>(R.id.header_group_members_container)
                .setOnClickListener {
                    presenter?.fireHeaderMembersClick()
                }
            root.findViewById<View>(R.id.horiz_scroll).clipToOutline = true
            root.findViewById<View>(R.id.header_group_topics_container)
                .setOnClickListener {
                    presenter?.fireHeaderTopicsClick()
                }
            root.findViewById<View>(R.id.header_group_documents_container)
                .setOnClickListener {
                    presenter?.fireHeaderDocsClick()
                }
            root.findViewById<View>(R.id.header_group_audios_container)
                .setOnClickListener {
                    presenter?.fireHeaderAudiosClick()
                }
            root.findViewById<View>(R.id.header_group_articles_container)
                .setOnClickListener {
                    presenter?.fireHeaderArticlesClick()
                }
            root.findViewById<View>(R.id.header_group_products_container)
                .setOnClickListener {
                    presenter?.fireHeaderProductsClick()
                }
            root.findViewById<View>(R.id.header_group_products_services_container)
                .setOnClickListener {
                    presenter?.fireHeaderProductServicesClick()
                }
            root.findViewById<View>(R.id.header_group_contacts_container)
                .setOnClickListener {
                    presenter?.fireShowCommunityInfoClick()
                }
            root.findViewById<View>(R.id.header_group_links_container)
                .setOnClickListener {
                    presenter?.fireShowCommunityLinksInfoClick()
                }
            root.findViewById<View>(R.id.header_group_about_container)
                .setOnClickListener {
                    presenter?.fireShowCommunityAboutInfoClick()
                }
            root.findViewById<View>(R.id.header_group_narratives_container)
                .setOnClickListener {
                    presenter?.fireNarrativesClick()
                }
            root.findViewById<View>(R.id.header_group_clips_container).let {
                it.setOnClickListener {
                    presenter?.fireClipsClick()
                }
                it.visibility = if (Utils.isOfficialVKCurrent) View.VISIBLE else View.GONE
            }
            bChatsContainer.setOnClickListener {
                presenter?.fireGroupChatsClick()
            }
        }
    }
}