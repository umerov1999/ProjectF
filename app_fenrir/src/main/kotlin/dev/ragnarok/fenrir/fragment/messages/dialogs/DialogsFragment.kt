package dev.ragnarok.fenrir.fragment.messages.dialogs

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.EnterPinActivity
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity.Companion.startFriendsSelection
import dev.ragnarok.fenrir.dialog.DialogNotifOptionsDialog
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.messages.dialogs.IDialogsView.IContextView
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.DialogsSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.MessageSearchCriteria
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Dialog
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getChatPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getImportantMessages
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.place.PlaceFactory.securitySettingsPlace
import dev.ragnarok.fenrir.settings.ISettings.INotificationSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.*
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.HelperSimple.NOTIFICATION_PERMISSION
import dev.ragnarok.fenrir.util.HelperSimple.needHelp
import dev.ragnarok.fenrir.util.Utils.addFlagIf
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.hasOreo
import dev.ragnarok.fenrir.util.Utils.removeFlag
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.UpEditFab
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class DialogsFragment : BaseMvpFragment<DialogsPresenter, IDialogsView>(), IDialogsView,
    DialogsAdapter.ClickListener, MenuProvider {
    private val requestSelectProfile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val users: ArrayList<Owner> = (result.data
                ?: return@registerForActivityResult).getParcelableArrayListExtraCompat(Extra.OWNERS)
                ?: return@registerForActivityResult
            lazyPresenter {
                fireUsersForChatSelected(users)
            }
        }
    }
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: DialogsAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mFab: UpEditFab? = null
    private val requestEnterPin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            Settings.get().security().showHiddenDialogs = true
            reconfigureOptionsHide(true)
            notifyDataSetChanged()
        }
    }
    private var mFabScrollListener: RecyclerView.OnScrollListener? = null
    private fun onSecurityClick() {
        if (Settings.get().security().isUsePinForSecurity) {
            requestEnterPin.launch(Intent(requireActivity(), EnterPinActivity::class.java))
        } else {
            createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
            securitySettingsPlace.tryOpenWith(requireActivity())
        }
    }

    internal fun reconfigureOptionsHide(isShowHidden: Boolean) {
        mAdapter?.updateShowHidden(isShowHidden)
        if (!Settings.get().security().hasHiddenDialogs) {
            mFab?.setImageResource(R.drawable.pencil)
            Settings.get().security().showHiddenDialogs = false
            return
        }
        mFab?.setImageResource(if (isShowHidden) R.drawable.offline else R.drawable.pencil)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestNPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS
        )
    ) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        if (Utils.hasTiramisu() && needHelp(
                NOTIFICATION_PERMISSION,
                1
            ) && !AppPerms.hasNotificationPermissionSimple(requireActivity())
        ) {
            requestNPermission.launch()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_dialogs, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val optionView = OptionView()
        presenter?.fireOptionViewCreated(
            optionView
        )
        menu.findItem(R.id.action_search).isVisible = optionView.pCanSearch
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_search -> {
                presenter?.fireSearchClick()
                return true
            }

            R.id.action_star -> {
                presenter?.fireImportantClick()
                return true
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dialogs, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mFab = root.findViewById(R.id.fab)
        mFab?.setOnClickListener {
            if (Settings.get().security().showHiddenDialogs) {
                Settings.get().security().showHiddenDialogs = false
                reconfigureOptionsHide(false)
                notifyDataSetChanged()
            } else {
                if (mFab?.isEdit == true) {
                    createGroupChat()
                } else {
                    mRecyclerView?.smoothScrollToPosition(0)
                }
            }
        }
        mFab?.setOnLongClickListener {
            if (!Settings.get().security().showHiddenDialogs && Settings.get().security()
                    .hasHiddenDialogs
            ) {
                onSecurityClick()
            }
            true
        }
        mFabScrollListener = mFab?.getRecyclerObserver(20)
        mRecyclerView = root.findViewById(R.id.recycleView)
        mRecyclerView?.layoutManager = LinearLayoutManager(requireActivity())
        PicassoPauseOnScrollListener.addListener(mRecyclerView, DialogsAdapter.PICASSO_TAG)
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefreshConfirmationHidden()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = DialogsAdapter(requireActivity(), emptyList())
        mAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAdapter
        reconfigureOptionsHide(Settings.get().security().showHiddenDialogs)
        return root
    }

    override fun setCreateGroupChatButtonVisible(visible: Boolean) {
        if (mFab != null && mRecyclerView != null) {
            mFab?.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                mFabScrollListener?.let { mRecyclerView?.addOnScrollListener(it) }
            } else {
                mFabScrollListener?.let { mRecyclerView?.removeOnScrollListener(it) }
            }
        }
    }

    override fun notifyHasAttachments(has: Boolean) {
        if (has) {
            ItemTouchHelper(MessagesReplyItemCallback { o: Int ->
                if (mAdapter?.checkPosition(o) == true) {
                    val dialog = mAdapter?.getByPosition(o) ?: return@MessagesReplyItemCallback
                    presenter?.fireRepost(
                        dialog
                    )
                }
            }).attachToRecyclerView(mRecyclerView)
        }
    }

    override fun updateAccountIdNoRefresh(accountId: Long) {
        mAdapter?.updateAccount(accountId)
    }

    override fun onDialogClick(dialog: Dialog) {
        presenter?.fireDialogClick(
            dialog
        )
    }

    override fun onDialogLongClick(dialog: Dialog): Boolean {
        val contextView = ContextView()
        presenter?.fireContextViewCreated(
            contextView,
            dialog
        )
        val menus = ModalBottomSheetDialogFragment.Builder()
        val delete = getString(R.string.delete)
        val addToHomeScreen = getString(R.string.add_to_home_screen)
        val notificationSettings = getString(R.string.peer_notification_settings)
        val notificationEnable = getString(R.string.enable_notifications)
        val notificationDisable = getString(R.string.disable_notifications)
        val addToShortcuts = getString(R.string.add_to_launcher_shortcuts)
        val setHide = getString(R.string.hide_dialog)
        val setShow = getString(R.string.set_no_hide_dialog)
        val setRead = getString(R.string.read)
        val setPin = getString(R.string.pin)
        val setUnPin = getString(R.string.unpin)
        if (contextView.pCanDelete) {
            menus.add(OptionRequest(1, delete, R.drawable.ic_outline_delete, true))
        }
        menus.add(
            OptionRequest(
                2,
                if (contextView.pIsPinned) setUnPin else setPin,
                if (contextView.pIsPinned) R.drawable.unpin else R.drawable.pin,
                true
            )
        )
        if (contextView.pCanAddToHomeScreen) {
            menus.add(OptionRequest(3, addToHomeScreen, R.drawable.ic_home_outline, false))
        }
        if (contextView.pCanConfigNotifications) {
            if (hasOreo()) {
                val mask = Settings.get()
                    .notifications()
                    .getNotifPref(Settings.get().accounts().current, dialog.peerId)
                if (hasFlag(mask, INotificationSettings.FLAG_SHOW_NOTIF)) {
                    menus.add(
                        OptionRequest(
                            4,
                            notificationDisable,
                            R.drawable.notification_disable,
                            false
                        )
                    )
                } else {
                    menus.add(OptionRequest(4, notificationEnable, R.drawable.feed, false))
                }
            } else {
                menus.add(OptionRequest(4, notificationSettings, R.drawable.feed, false))
            }
        }
        if (contextView.pCanAddToShortcuts && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            menus.add(OptionRequest(5, addToShortcuts, R.drawable.about_writed, false))
        }
        if (!contextView.pIsHidden) {
            menus.add(OptionRequest(6, setHide, R.drawable.offline, true))
        }
        if (contextView.pIsHidden && Settings.get().security().showHiddenDialogs) {
            menus.add(OptionRequest(7, setShow, R.drawable.ic_eye_white_vector, false))
        }
        if (contextView.pCanRead) {
            menus.add(OptionRequest(8, setRead, R.drawable.email, true))
        }
        menus.header(
            if (contextView.pIsHidden && !Settings.get()
                    .security().showHiddenDialogs
            ) getString(R.string.dialogs) else dialog.getDisplayTitle(requireActivity()),
            R.drawable.email,
            dialog.imageUrl
        )
        menus.columns(1)
        menus.show(
            childFragmentManager,
            "dialog_options"
        ) { _, option ->
            when (option.id) {
                1 -> CustomSnackbars.createCustomSnackbars(view)
                    ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                    ?.themedSnack(R.string.delete_chat_do)
                    ?.setAction(
                        R.string.button_yes
                    ) {
                        presenter?.fireRemoveDialogClick(
                            dialog
                        )
                    }
                    ?.show()

                2 -> if (contextView.pIsPinned) {
                    presenter?.fireUnPin(
                        dialog
                    )
                } else {
                    presenter?.firePin(
                        dialog
                    )
                }

                3 -> presenter?.fireCreateShortcutClick(
                    dialog
                )

                4 -> if (hasOreo()) {
                    val accountId = Settings.get().accounts().current
                    var mask = Settings.get()
                        .notifications()
                        .getNotifPref(accountId, dialog.peerId)
                    mask = if (hasFlag(mask, INotificationSettings.FLAG_SHOW_NOTIF)) {
                        removeFlag(mask, INotificationSettings.FLAG_SHOW_NOTIF)
                    } else {
                        addFlagIf(mask, INotificationSettings.FLAG_SHOW_NOTIF, true)
                    }
                    Settings.get()
                        .notifications()
                        .setNotifPref(accountId, dialog.peerId, mask)
                    mAdapter?.notifyDataSetChanged()
                } else {
                    presenter?.fireNotificationsSettingsClick(
                        dialog
                    )
                }

                5 -> presenter?.fireAddToLauncherShortcuts(
                    dialog
                )

                6 -> if (!Settings.get().security().isUsePinForSecurity) {
                    createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
                    securitySettingsPlace.tryOpenWith(requireActivity())
                } else {
                    Settings.get().security().addHiddenDialog(dialog.getOwnerObjectId())
                    reconfigureOptionsHide(Settings.get().security().showHiddenDialogs)
                    notifyDataSetChanged()
                    if (needHelp(HelperSimple.HIDDEN_DIALOGS, 3)) {
                        showSnackbar(R.string.hidden_dialogs_helper, true)
                    }
                }

                7 -> {
                    Settings.get().security().removeHiddenDialog(dialog.getOwnerObjectId())
                    reconfigureOptionsHide(Settings.get().security().showHiddenDialogs)
                    notifyDataSetChanged()
                }

                8 -> presenter?.fireRead(
                    dialog
                )
            }
        }
        return true
    }

    override fun askToReload() {
        CustomSnackbars.createCustomSnackbars(view, null, true)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)?.defaultSnack(R.string.update_dialogs)
            ?.setAction(R.string.button_yes) {
                presenter?.fireRefresh()
            }?.show()
    }

    override fun showDialogSendHelper() {
        showSnackbar(R.string.dialog_send_helper, true)
    }

    override fun onAvatarClick(dialog: Dialog) {
        presenter?.fireDialogAvatarClick(
            dialog
        )
    }

    private fun createGroupChat() {
        requestSelectProfile.launch(startFriendsSelection(requireActivity()))
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.DIALOGS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.dialogs)
            actionBar.subtitle = requireArguments().getString(Extra.SUBTITLE)
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_DIALOGS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    /*
    @Override
    public void onDestroyView() {
        if (mAdapter != null) {
            mAdapter.cleanup();
        }

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.destroyDrawingCache();
            mSwipeRefreshLayout.clearAnimation();
        }

        super.onDestroyView();
    }

     */
    override fun displayData(data: List<Dialog>, accountId: Long) {
        mAdapter?.setData(data, accountId)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun goToChat(
        accountId: Long,
        messagesOwnerId: Long,
        peerId: Long,
        title: String?,
        ava_url: String?
    ) {
        getChatPlace(
            accountId,
            messagesOwnerId,
            Peer(peerId).setTitle(title).setAvaUrl(ava_url)
        ).tryOpenWith(requireActivity())
    }

    override fun goToSearch(accountId: Long) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.info)
            .setCancelable(true)
            .setMessage(R.string.what_search)
            .setNeutralButton(R.string.search_dialogs) { _: DialogInterface?, _: Int ->
                val criteria = DialogsSearchCriteria("")
                getSingleTabSearchPlace(accountId, SearchContentType.DIALOGS, criteria)
                    .tryOpenWith(requireActivity())
            }
            .setPositiveButton(R.string.search_messages) { _: DialogInterface?, _: Int ->
                val criteria = MessageSearchCriteria("")
                getSingleTabSearchPlace(accountId, SearchContentType.MESSAGES, criteria)
                    .tryOpenWith(requireActivity())
            }
            .show()
    }

    override fun goToImportant(accountId: Long) {
        getImportantMessages(accountId).tryOpenWith(requireActivity())
    }

    override fun showSnackbar(@StringRes res: Int, isLong: Boolean) {
        CustomSnackbars.createCustomSnackbars(view)
            ?.setDurationSnack(if (isLong) BaseTransientBottomBar.LENGTH_LONG else BaseTransientBottomBar.LENGTH_SHORT)
            ?.defaultSnack(res)?.show()
    }

    override fun showEnterNewGroupChatTitle(users: List<User>) {
        InputTextDialog.Builder(requireActivity())
            .setTitleRes(R.string.set_groupchat_title)
            .setAllowEmpty(true)
            .setInputType(InputType.TYPE_CLASS_TEXT)
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    presenter?.fireNewGroupChatTitleEntered(
                        users,
                        newValue
                    )
                }

                override fun onCanceled() {

                }
            })
            .show()
    }

    override fun showNotificationSettings(accountId: Long, peerId: Long) {
        if (hasOreo()) {
            return
        }
        DialogNotifOptionsDialog.newInstance(
            accountId,
            peerId,
            object : DialogNotifOptionsDialog.Listener {
                override fun onSelected() {
                    mAdapter?.notifyDataSetChanged()
                }
            }).show(parentFragmentManager, "dialog-notif-options")
    }

    override fun goToOwnerWall(accountId: Long, ownerId: Long, owner: Owner?) {
        getOwnerWallPlace(accountId, ownerId, owner).tryOpenWith(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<DialogsPresenter> {
        return object : IPresenterFactory<DialogsPresenter> {
            override fun create(): DialogsPresenter {
                return DialogsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    requireActivity().intent.getParcelableExtraCompat(MainActivity.EXTRA_INPUT_ATTACHMENTS),
                    saveInstanceState
                )
            }
        }
    }

    private class OptionView : IDialogsView.IOptionView {
        var pCanSearch = false
        override fun setCanSearch(can: Boolean) {
            pCanSearch = can
        }
    }

    private class ContextView : IContextView {
        var pCanDelete = false
        var pCanAddToHomeScreen = false
        var pCanConfigNotifications = false
        var pCanAddToShortcuts = false
        var pCanRead = false
        var pIsHidden = false
        var pIsPinned = false
        override fun setCanDelete(can: Boolean) {
            pCanDelete = can
        }

        override fun setCanAddToHomeScreen(can: Boolean) {
            pCanAddToHomeScreen = can
        }

        override fun setCanConfigNotifications(can: Boolean) {
            pCanConfigNotifications = can
        }

        override fun setCanAddToShortcuts(can: Boolean) {
            pCanAddToShortcuts = can
        }

        override fun setIsHidden(can: Boolean) {
            pIsHidden = can
        }

        override fun setCanRead(can: Boolean) {
            pCanRead = can
        }

        override fun setPinned(pinned: Boolean) {
            pIsPinned = pinned
        }
    }

    companion object {
        fun newInstance(accountId: Long, dialogsOwnerId: Long, subtitle: String?): DialogsFragment {
            val fragment = DialogsFragment()
            val args = Bundle()
            args.putString(Extra.SUBTITLE, subtitle)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, dialogsOwnerId)
            fragment.arguments = args
            return fragment
        }
    }
}
