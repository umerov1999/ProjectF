package dev.ragnarok.fenrir.fragment.messages.chat

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.activity.AttachmentsActivity
import dev.ragnarok.fenrir.activity.AudioSelectActivity
import dev.ragnarok.fenrir.activity.DualTabPhotoActivity
import dev.ragnarok.fenrir.activity.FileManagerSelectActivity
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.activity.NotReadMessagesActivity
import dev.ragnarok.fenrir.activity.PhotosActivity
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.activity.SwipebleActivity
import dev.ragnarok.fenrir.activity.VideoSelectActivity
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.db.model.AttachmentsTypes
import dev.ragnarok.fenrir.dialog.ImageSizeAlertDialog
import dev.ragnarok.fenrir.dialog.LandscapeExpandBottomSheetDialog
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.messages.chat.sheet.AttachmentsBottomSheetAdapter
import dev.ragnarok.fenrir.fragment.messages.chat.sheet.MessageAttachmentsFragment
import dev.ragnarok.fenrir.fragment.messages.chatusersdomain.ChatUsersDomainFragment
import dev.ragnarok.fenrir.fragment.poll.createpoll.CreatePollDialogFragment
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.MessageSearchCriteria
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.link.internal.TopicLink
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.ChatOnScrollListener
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.materialpopupmenu.popupMenu
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.ChatConfig
import dev.ragnarok.fenrir.model.FwdMessages
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.LastReadId
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.model.WriteText
import dev.ragnarok.fenrir.model.selection.FileManagerSelectableSource
import dev.ragnarok.fenrir.model.selection.LocalGallerySelectableSource
import dev.ragnarok.fenrir.model.selection.LocalPhotosSelectableSource
import dev.ragnarok.fenrir.model.selection.LocalVideosSelectableSource
import dev.ragnarok.fenrir.model.selection.Sources
import dev.ragnarok.fenrir.model.selection.VKPhotosSelectableSource
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.FindAttachmentType
import dev.ragnarok.fenrir.util.InputTextDialog
import dev.ragnarok.fenrir.util.MessagesReplyItemCallback
import dev.ragnarok.fenrir.util.ScreenshotHelper
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.InputViewController
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.ReactionContainer
import dev.ragnarok.fenrir.view.WeakViewAnimatorAdapter
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup
import dev.ragnarok.fenrir.view.emoji.StickersKeyWordsAdapter
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import me.minetsh.imaging.IMGEditActivity
import java.io.File
import java.lang.ref.WeakReference

class ChatFragment : PlaceSupportMvpFragment<ChatPresenter, IChatView>(), IChatView,
    InputViewController.OnInputActionCallback,
    BackPressCallback, MessagesAdapter.OnMessageActionListener,
    InputViewController.RecordActionsCallback,
    AttachmentsViewBinder.VoiceActionListener, EmojiconsPopup.OnStickerClickedListener,
    EmojiconsPopup.OnMyStickerClickedListener,
    EmojiconTextView.OnHashTagClickListener, BotKeyboardView.BotKeyboardViewDelegate,
    ReactionContainer.ReactionClicked {

    private var headerView: View? = null
    private var loadMoreFooterHelper: LoadMoreFooterHelper? = null

    private var recyclerView: RecyclerView? = null
    private var stickersKeywordsView: RecyclerView? = null
    private var stickersAdapter: StickersKeyWordsAdapter? = null
    private var adapter: MessagesAdapter? = null

    private var downMenuGroup: FrameLayout? = null

    private var inputViewController: InputViewController? = null
    private var emptyText: TextView? = null
    private var emptyAnimation: ThorVGLottieView? = null

    private var pinnedView: View? = null
    private var pinnedAvatar: ImageView? = null
    private var pinnedTitle: TextView? = null
    private var pinnedSubtitle: TextView? = null
    private var buttonUnpin: View? = null

    private val optionMenuSettings = SparseBooleanArray()

    private var toolbarRootView: FrameLayout? = null
    private var actionModeHolder: ActionModeHolder? = null

    private var editMessageGroup: ViewGroup? = null
    private var editMessageText: TextView? = null

    private var gotoButton: FloatingActionButton? = null

    private var writingMsgGroup: View? = null
    private var writingMsg: TextView? = null
    private var writingMsgAva: ImageView? = null
    private var writingMsgType: ImageView? = null

    private var toolbarTitle: TextView? = null
    private var toolbarSubTitle: TextView? = null

    private var toolbarAvatar: ImageView? = null

    private var toolbar: Toolbar? = null
    private var emptyAvatar: TextView? = null

    private var inputView: View? = null

    private var connectivityManager: ConnectivityManager? = null

    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            presenter?.fireNetworkChanged()
        }

        override fun onLost(network: Network) {

        }
    }

    private val requestRecordPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    ) {
        lazyPresenter { fireRecordPermissionsResolved() }
    }
    private val requestCameraEditPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter { fireEditCameraClick() }
    }

    private val requestCameraEditPermissionScoped = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.CAMERA
        )
    ) {
        lazyPresenter { fireEditCameraClick() }
    }

    private val openCameraRequest = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { result ->
        if (result) {
            when (val defaultSize = Settings.get().main().uploadImageSize) {
                null -> {
                    ImageSizeAlertDialog.Builder(requireActivity())
                        .setOnSelectedCallback(object : ImageSizeAlertDialog.OnSelectedCallback {
                            override fun onSizeSelected(size: Int) {
                                presenter?.fireEditPhotoMade(size)
                            }
                        })
                        .show()
                }

                else -> presenter?.fireEditPhotoMade(defaultSize)
            }
        }
    }

    private val requestFile = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(Extra.PATH)?.let {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.info)
                    .setMessage(R.string.do_convert_request)
                    .setPositiveButton(R.string.button_yes) { _, _ ->
                        presenter?.sendRecordingCustomMessageImpl(
                            requireActivity(),
                            it
                        )
                    }
                    .setNegativeButton(R.string.button_cancel) { _, _ ->
                        presenter?.sendRecordingMessageImpl(File(it))
                    }
                    .show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            CreatePollDialogFragment.REQUEST_CREATE_POLL_EDIT,
            this
        ) { _, result ->
            val poll: Poll = result.getParcelableCompat("poll") ?: return@setFragmentResultListener
            presenter?.fireEditAttachmentsSelected(arrayListOf(poll))
        }
        parentFragmentManager.setFragmentResultListener(
            MessageAttachmentsFragment.MESSAGE_CLOSE_ONLY,
            this
        ) { _, _ -> presenter?.fireSendClickFromAttachments() }
        parentFragmentManager.setFragmentResultListener(
            MessageAttachmentsFragment.MESSAGE_SYNC_ATTACHMENTS,
            this
        ) { _, bundle ->
            run {
                if (bundle.containsKey(Extra.BUNDLE)) {
                    val modelBundle = bundle.getParcelableCompat<ModelsBundle>(Extra.BUNDLE)
                    if (modelBundle != null)
                        presenter?.fireEditMessageResult(modelBundle)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_chat, container, false) as ViewGroup
        root.background = CurrentTheme.getChatBackground(requireActivity())

        toolbar = root.findViewById(R.id.toolbar)
        toolbar?.inflateMenu(R.menu.menu_chat)
        toolbar?.menu?.let { prepareOptionsMenu(it) }
        toolbar?.setOnMenuItemClickListener {
            optionsMenuItemSelected(it)
        }

        stickersKeywordsView = root.findViewById(R.id.stickers)
        stickersAdapter = StickersKeyWordsAdapter(requireActivity(), emptyList())
        stickersAdapter?.setStickerClickedListener(object :
            EmojiconsPopup.OnStickerClickedListener {
            override fun onStickerClick(sticker: Sticker) {
                presenter?.fireStickerSendClick(sticker)
                presenter?.resetDraftMessage()
            }
        })
        stickersKeywordsView?.let {
            it.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
            it.adapter = stickersAdapter
            it.visibility = View.GONE
        }

        downMenuGroup = root.findViewById(R.id.down_menu)

        toolbarTitle = root.findViewById(R.id.dialog_title)
        toolbarSubTitle = root.findViewById(R.id.dialog_subtitle)
        toolbarAvatar = root.findViewById(R.id.toolbar_avatar)
        emptyAvatar = root.findViewById(R.id.empty_avatar_text)

        emptyText = root.findViewById(R.id.fragment_chat_empty_text)
        emptyAnimation = root.findViewById(R.id.fragment_chat_empty_animation)
        toolbarRootView = root.findViewById(R.id.toolbar_root)

        writingMsgGroup = root.findViewById(R.id.writingGroup)
        writingMsg = root.findViewById(R.id.writing)
        writingMsgAva = root.findViewById(R.id.writingava)
        writingMsgType = root.findViewById(R.id.writing_type)
        inputView = root.findViewById(R.id.input_view)

        writingMsgGroup?.visibility = View.GONE

        gotoButton = root.findViewById(R.id.goto_button)
        gotoButton?.let {
            if (Utils.isHiddenCurrent) {
                it.setImageResource(R.drawable.attachment)
                it.setOnClickListener { presenter?.fireDialogAttachmentsClick() }
            } else {
                it.setImageResource(R.drawable.ic_outline_keyboard_arrow_up)
                it.setOnClickListener { presenter?.fireScrollToUnread() }
                it.setOnLongClickListener { presenter?.fireDialogAttachmentsClick(); true; }
            }

            if (!Settings.get().main().isEnable_last_read)
                it.visibility = View.GONE
            else
                it.visibility = View.VISIBLE
        }

        recyclerView = root.findViewById(R.id.fragment_friend_dialog_list)
        recyclerView?.apply {
            layoutManager = createLayoutManager()
            itemAnimator?.changeDuration = 0
            itemAnimator?.addDuration = 0
            itemAnimator?.moveDuration = 0
            itemAnimator?.removeDuration = 0
            PicassoPauseOnScrollListener.addListener(this)
            addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
                override fun onScrollToLastElement() {
                    presenter?.fireScrollToEnd()
                }
            })
            if (Settings.get().main().isEnable_last_read) {
                gotoButton?.let { addOnScrollListener(ChatOnScrollListener(it)) }
            }
        }

        ItemTouchHelper(MessagesReplyItemCallback {
            presenter?.fireResendSwipe(
                (adapter ?: return@MessagesReplyItemCallback).getItemRawPosition(
                    it
                )
            )
        }).attachToRecyclerView(recyclerView)

        headerView = inflater.inflate(R.layout.footer_load_more, recyclerView, false)

        headerView?.let {
            loadMoreFooterHelper =
                LoadMoreFooterHelper.createFrom(it, object : LoadMoreFooterHelper.Callback {
                    override fun onLoadMoreClick() {
                        presenter?.fireLoadUpButtonClick()
                    }
                })
        }

        inputViewController = InputViewController(requireActivity(), root, this)
            .also {
                it.setSendOnEnter(Settings.get().main().isSendByEnter)
                it.setRecordActionsCallback(this)
                it.setOnSickerClickListener(this)
                it.setOnMySickerClickListener(this)
                it.setKeyboardBotClickListener(this)
                it.setKeyboardBotLongClickListener {
                    run {
                        recyclerView?.scrollToPosition(0)
                        presenter?.resetChronology()
                        presenter?.fireRefreshClick()
                    }; true
                }
            }

        pinnedView = root.findViewById(R.id.pinned_root_view)
        pinnedView?.let {
            pinnedAvatar = it.findViewById(R.id.pinned_avatar)
            pinnedTitle = it.findViewById(R.id.pinned_title)
            pinnedSubtitle = it.findViewById(R.id.pinned_subtitle)
            buttonUnpin = it.findViewById(R.id.buttonUnpin)
        }
        buttonUnpin?.setOnClickListener { presenter?.fireUnpinClick() }

        pinnedView?.setOnLongClickListener {
            it.isVisible = false
            true
        }

        editMessageGroup = root.findViewById(R.id.editMessageGroup)
        editMessageText = editMessageGroup?.findViewById(R.id.editMessageText)
        editMessageGroup?.findViewById<View>(R.id.buttonCancelEditing)
            ?.setOnClickListener { presenter?.fireCancelEditingClick() }
        return root
    }

    override fun convertToKeyboard(keyboard: Keyboard?) {
        keyboard ?: return
        inputViewController?.updateBotKeyboard(keyboard, !Utils.isHiddenCurrent)
    }

    override fun displayEditingMessage(message: Message?) {
        editMessageGroup?.visibility = if (message == null) View.GONE else View.VISIBLE
        message?.run {
            editMessageText?.text =
                if (text.isNullOrEmpty()) getString(R.string.attachments) else text
        }
    }

    override fun displayWriting(writeText: WriteText) {
        if (writeText.getFrom_ids().isEmpty()) {
            return
        }
        writingMsgGroup?.visibility = View.VISIBLE
        writingMsgGroup?.alpha = 0.0f
        ObjectAnimator.ofFloat(writingMsgGroup, View.ALPHA, 1f).setDuration(200).start()
        writingMsg?.setText(if (writeText.isText) R.string.user_type_message else R.string.user_type_voice)
        writingMsgType?.setImageResource(if (writeText.isText) R.drawable.pencil else R.drawable.voice)
        writingMsgAva?.setImageResource(R.drawable.background_gray_round)
        presenter?.resolveWritingInfo(requireActivity(), writeText)
    }

    @SuppressLint("SetTextI18n")
    override fun displayWriting(owner: Owner, count: Int, is_text: Boolean) {
        if (count > 1) {
            writingMsg?.text = getString(R.string.many_users_typed, owner.fullName, count - 1)
        } else {
            writingMsg?.text = owner.fullName
        }
        writingMsgType?.setImageResource(if (is_text) R.drawable.pencil else R.drawable.voice)
        ViewUtils.displayAvatar(
            writingMsgAva ?: return, CurrentTheme.createTransformationForAvatar(),
            owner.get100photoOrSmaller(), null
        )
    }

    override fun updateStickers(items: List<Sticker>) {
        if (items.isEmpty()) {
            stickersKeywordsView?.visibility = View.GONE
        } else {
            stickersKeywordsView?.visibility = View.VISIBLE
        }
        stickersAdapter?.setData(items)
    }

    override fun hideWriting() {
        val animator: ObjectAnimator? =
            ObjectAnimator.ofFloat(writingMsgGroup, View.ALPHA, 0.0f).apply {
                addListener(object : WeakViewAnimatorAdapter<View?>(writingMsgGroup) {
                    override fun onAnimationEnd(view: View?) {
                        writingMsgGroup?.visibility = View.GONE
                    }
                })
                duration = 200
            }
        animator?.start()
    }

    override fun showPopupOptions(
        position: Int,
        x: Int,
        y: Int,
        canEdit: Boolean,
        canPin: Boolean,
        canStar: Boolean,
        doStar: Boolean,
        canSpam: Boolean
    ) {
        var hasOpt = false
        view?.let { anchorView ->
            if (anchorView.isAttachedToWindow) {
                anchorView.popupMenu(requireActivity(), x, y) {
                    section {
                        if (canEdit) {
                            item(R.string.edit) {
                                icon = R.drawable.pencil
                                iconColor = CurrentTheme.getColorSecondary(requireActivity())
                                onSelect {
                                    presenter?.fireActionModeEditClick()
                                }
                            }
                        }
                        item(R.string.forward) {
                            icon = R.drawable.ic_outline_forward
                            iconColor = CurrentTheme.getColorSecondary(requireActivity())
                            onSelect {
                                presenter?.fireForwardClick()
                            }
                        }
                        item(R.string.copy_data) {
                            icon = R.drawable.content_copy
                            iconColor = CurrentTheme.getColorSecondary(requireActivity())
                            onSelect {
                                presenter?.fireActionModeCopyClick()
                            }
                        }
                        item(R.string.reactions) {
                            icon = R.drawable.emoticon
                            iconColor = CurrentTheme.getColorSecondary(requireActivity())
                            onSelect {
                                presenter?.fireReactionModeClick(position)
                            }
                        }
                        item(R.string.select_more) {
                            icon = R.drawable.ic_arrow_down
                            iconColor = CurrentTheme.getColorSecondary(requireActivity())
                            onSelect {
                                hasOpt = true
                                presenter?.resolveActionMode()
                            }
                        }
                        if (canPin) {
                            item(R.string.pin) {
                                icon = R.drawable.pin
                                iconColor = CurrentTheme.getColorSecondary(requireActivity())
                                onSelect {
                                    presenter?.fireActionModePinClick()
                                }
                            }
                        }
                        if (canStar) {
                            item(R.string.important) {
                                icon = if (doStar) R.drawable.star_add else R.drawable.star_none
                                iconColor = CurrentTheme.getColorSecondary(requireActivity())
                                onSelect {
                                    presenter?.fireActionModeStarClick()
                                }
                            }
                        }
                        item(R.string.delete) {
                            icon = R.drawable.ic_outline_delete
                            iconColor = CurrentTheme.getColorSecondary(requireActivity())
                            onSelect {
                                presenter?.fireActionModeDeleteClick()
                            }
                        }
                        if (canSpam) {
                            item(R.string.reason_spam) {
                                icon = R.drawable.report
                                iconColor = CurrentTheme.getColorSecondary(requireActivity())
                                onSelect {
                                    val dlgAlert = MaterialAlertDialogBuilder(requireActivity())
                                    dlgAlert.setIcon(R.drawable.report_red)
                                    dlgAlert.setMessage(R.string.do_report)
                                    dlgAlert.setTitle(R.string.select)
                                    dlgAlert.setPositiveButton(
                                        R.string.button_yes
                                    ) { _, _ -> presenter?.fireActionModeSpamClick() }
                                    dlgAlert.setNeutralButton(
                                        R.string.delete
                                    ) { _, _ -> presenter?.fireActionModeDeleteClick() }
                                    dlgAlert.setCancelable(true)
                                    dlgAlert.create().show()
                                }
                            }
                        }
                    }
                    onDismiss {
                        if (hasOpt) {
                            return@onDismiss
                        }
                        presenter?.clearSelection(position)
                    }
                    setCalculateHeightOfAnchorView(true)
                }
            }
        }
    }

    private class ActionModeHolder(val rootView: View, fragment: ChatFragment) :
        View.OnClickListener {

        override fun onClick(v: View) {
            when (v.id) {
                R.id.buttonClose -> hide()
                R.id.buttonEdit -> {
                    reference.get()?.presenter?.fireActionModeEditClick()
                    hide()
                }

                R.id.buttonForward -> {
                    reference.get()?.presenter?.fireForwardClick()
                    hide()
                }

                R.id.buttonCopy -> {
                    reference.get()?.presenter?.fireActionModeCopyClick()
                    hide()
                }

                R.id.buttonDelete -> {
                    reference.get()?.presenter?.fireActionModeDeleteClick()
                    hide()
                }

                R.id.buttonSpam -> {
                    val dlgAlert =
                        reference.get()?.let { MaterialAlertDialogBuilder(it.requireActivity()) }
                    dlgAlert?.setIcon(R.drawable.report_red)
                    dlgAlert?.setMessage(R.string.do_report)
                    dlgAlert?.setTitle(R.string.select)
                    dlgAlert?.setPositiveButton(
                        R.string.button_yes
                    ) { _, _ -> reference.get()?.presenter?.fireActionModeSpamClick(); hide(); }
                    dlgAlert?.setNeutralButton(
                        R.string.delete
                    ) { _, _ -> reference.get()?.presenter?.fireActionModeDeleteClick(); hide(); }
                    dlgAlert?.setCancelable(true)
                    dlgAlert?.create()?.show()
                }

                R.id.buttonPin -> {
                    reference.get()?.presenter?.fireActionModePinClick()
                    hide()
                }

                R.id.buttonStar -> {
                    reference.get()?.presenter?.fireActionModeStarClick()
                    hide()
                }
            }
        }

        val reference = WeakReference(fragment)
        val buttonClose: View = rootView.findViewById(R.id.buttonClose)
        val buttonEdit: View = rootView.findViewById(R.id.buttonEdit)
        val buttonForward: View = rootView.findViewById(R.id.buttonForward)
        val buttonCopy: View = rootView.findViewById(R.id.buttonCopy)
        val buttonDelete: View = rootView.findViewById(R.id.buttonDelete)
        val buttonSpam: View = rootView.findViewById(R.id.buttonSpam)
        val buttonPin: View = rootView.findViewById(R.id.buttonPin)
        val buttonStar: ImageView = rootView.findViewById(R.id.buttonStar)
        val titleView: TextView = rootView.findViewById(R.id.actionModeTitle)

        init {
            buttonClose.setOnClickListener(this)
            buttonEdit.setOnClickListener(this)
            buttonForward.setOnClickListener(this)
            buttonCopy.setOnClickListener(this)
            buttonDelete.setOnClickListener(this)
            buttonSpam.setOnClickListener(this)
            buttonPin.setOnClickListener(this)
            buttonStar.setOnClickListener(this)
        }

        fun show() {
            rootView.visibility = View.VISIBLE
        }

        fun isVisible(): Boolean = rootView.isVisible

        fun hide() {
            if (Settings.get().main().isMessages_menu_down) {
                reference.get()?.inputView?.visibility = View.VISIBLE
                reference.get()?.downMenuGroup?.visibility = View.GONE
            }
            rootView.visibility = View.GONE
            reference.get()?.presenter?.fireActionModeDestroy()
        }
    }

    override fun onRecordCancel() {
        presenter?.fireRecordCancelClick()
    }

    override fun onSwithToRecordMode() {
        presenter?.fireRecordingButtonClick()
    }

    override fun onRecordSendClick() {
        presenter?.fireRecordSendClick()
    }

    override fun onRecordCustomClick() {
        if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
            requestSendCustomVoicePermission.launch()
            return
        }
        requestFile.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                null, getString(R.string.send_file_as_voice)
            )
        )
    }

    override fun onResumePauseClick() {
        presenter?.fireRecordResumePauseClick()
    }

    private fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, true)
    }

    override fun displayMessages(
        accountId: Long,
        messages: MutableList<Message>,
        lastReadId: LastReadId
    ) {
        adapter = MessagesAdapter(accountId, requireActivity(), messages, lastReadId, this, false)
            .also {
                it.setOnMessageActionListener(this)
                it.setVoiceActionListener(this)
                it.setonReactionListener(this)
                headerView?.let { it1 -> it.addFooter(it1) }
                it.setOnHashTagClickListener(this)
            }

        recyclerView?.adapter = adapter
    }

    override fun notifyMessagesUpAdded(position: Int, count: Int) {
        adapter?.run {
            notifyItemRangeChanged(position + headersCount, count) //+header if exist
        }
    }

    override fun notifyDataChanged() {
        adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(index: Int) {
        adapter?.notifyItemBindableChanged(index)
    }

    override fun notifyMessagesDownAdded(count: Int) {

    }

    override fun configNowVoiceMessagePlaying(
        id: Int,
        progress: Float,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    ) {
        adapter?.configNowVoiceMessagePlaying(id, progress, paused, amin, speed)
    }

    override fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        amin: Boolean,
        speed: Boolean
    ) {
        adapter?.bindVoiceHolderById(holderId, play, paused, progress, amin, speed)
    }

    override fun disableVoicePlaying() {
        adapter?.disableVoiceMessagePlaying()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): ChatPresenter {
        val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
        val messagesOwnerId = requireArguments().getLong(Extra.OWNER_ID)
        val peer = requireArguments().getParcelableCompat<Peer>(Extra.PEER)!!
        return ChatPresenter(
            aid,
            messagesOwnerId,
            peer,
            createStartConfig(),
            saveInstanceState
        )
    }

    internal fun createStartConfig(): ChatConfig {
        val config = ChatConfig()

        config.setCloseOnSend(requireActivity() is SendAttachmentsActivity)

        val inputStreams = ActivityUtils.checkLocalStreams(requireActivity())
        config.setUploadFiles(
            if (inputStreams == null) null else {
                if (inputStreams.uris.isNullOrEmpty()) null else inputStreams.uris
            }
        )
        config.setUploadFilesMimeType(inputStreams?.mime)

        val models =
            requireActivity().intent.getParcelableExtraCompat<ModelsBundle>(MainActivity.EXTRA_INPUT_ATTACHMENTS)

        models?.run {
            config.appendAll(this)
        }

        val initialText = ActivityUtils.checkLinks(requireActivity())
        config.setInitialText(if (initialText.isNullOrEmpty()) null else initialText)
        return config
    }

    override fun setupLoadUpHeaderState(@LoadMoreState state: Int) {
        loadMoreFooterHelper?.switchToState(state)
    }

    override fun displayDraftMessageAttachmentsCount(count: Int) {
        inputViewController?.setAttachmentsCount(count)
    }

    override fun displayDraftMessageText(text: String?) {
        inputViewController?.setTextQuietly(text)
        presenter?.fireTextEdited(text)
    }

    override fun appendMessageText(text: String?) {
        inputViewController?.appendTextQuietly(text)
    }

    override fun displayToolbarTitle(text: String?) {
        toolbarTitle?.text = text
    }

    override fun displayToolbarSubtitle(text: String?) {
        toolbarSubTitle?.text = text
    }

    override fun displayToolbarAvatar(peer: Peer?) {
        if (peer?.avaUrl.nonNullNoEmpty()) {
            emptyAvatar?.visibility = View.GONE
            toolbarAvatar?.let {
                PicassoInstance.with()
                    .load(peer?.avaUrl)
                    .transform(RoundTransformation())
                    .into(it)
            }
        } else {
            toolbarAvatar?.let { PicassoInstance.with().cancelRequest(it) }
            peer?.let { itv ->
                if (itv.getTitle().nonNullNoEmpty()) {
                    emptyAvatar?.visibility = View.VISIBLE
                    var name: String = itv.getTitle().orEmpty()
                    if (name.length > 2) name = name.substring(0, 2)
                    name = name.trim()
                    emptyAvatar?.text = name
                } else {
                    emptyAvatar?.visibility = View.GONE
                }
                toolbarAvatar?.setImageBitmap(
                    RoundTransformation().localTransform(
                        Utils.createGradientChatImage(
                            200,
                            200,
                            itv.id
                        )
                    )
                )
            }
        }
    }

    override fun setupPrimaryButtonAsEditing(canSave: Boolean) {
        inputViewController?.switchModeToEditing(canSave)
    }

    override fun setupPrimaryButtonAsRecording() {
        inputViewController?.switchModeToRecording()
    }

    override fun setupPrimaryButtonAsRegular(canSend: Boolean, canStartRecording: Boolean) {
        inputViewController?.switchModeToNormal(canSend, canStartRecording)
    }

    override fun requestRecordPermissions() {
        requestRecordPermission.launch()
    }

    override fun displayRecordingDuration(time: Long) {
        inputViewController?.setRecordingDuration(time)
    }

    override fun doCloseAfterSend() {
        try {
            ViewUtils.keyboardHide(requireActivity())
            requireActivity().finish()
        } catch (_: Exception) {

        }
    }

    override fun scrollToUnread(position: Int, loading: Boolean) {
        if (!loading || position > 2) recyclerView?.smoothScrollToPosition(position)
    }

    override fun goToMessagesLookup(
        accountId: Long,
        peerId: Long,
        messageId: Int,
        message: Message
    ) {
        PlaceFactory.getMessagesLookupPlace(accountId, peerId, messageId, message)
            .tryOpenWith(requireActivity())
    }

    private val requestMessagesUnread = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val incoming = result.data?.extras?.getInt(Extra.INCOMING) ?: -1
            val outgoing = result.data?.extras?.getInt(Extra.OUTGOING) ?: -1
            presenter?.fireCheckMessages(incoming, outgoing)
        }
    }

    override fun onAudioPlay(position: Int, audios: ArrayList<Audio>, holderPosition: Int?) {
        presenter?.fireAudioPlayClick(position, audios, holderPosition)
    }

    override fun goToUnreadMessages(
        accountId: Long,
        messageId: Int,
        incoming: Int,
        outgoing: Int,
        unreadCount: Int,
        peer: Peer
    ) {
        if (!Settings.get()
                .main().isNot_read_show || requireActivity() is SwipebleActivity || requireActivity() is SendAttachmentsActivity
            || requireActivity() is SelectProfilesActivity || requireActivity() is AttachmentsActivity
        ) {
            return
        }
        val intent = Intent(requireActivity(), NotReadMessagesActivity::class.java)
        intent.action = NotReadMessagesActivity.ACTION_OPEN_PLACE
        intent.putExtra(
            Extra.PLACE,
            PlaceFactory.getUnreadMessagesPlace(
                accountId,
                messageId,
                incoming,
                outgoing,
                unreadCount,
                peer
            )
        )
        requestMessagesUnread.launch(intent)
        Utils.activityTransactionImmediate(requireActivity())
    }

    override fun displayPinnedMessage(pinned: Message?, canChange: Boolean) {
        pinnedView?.run {
            isVisible = pinned != null
            pinned?.run {
                ViewUtils.displayAvatar(
                    pinnedAvatar, CurrentTheme.createTransformationForAvatar(),
                    sender?.get100photoOrSmaller(), null
                )

                pinnedTitle?.text = sender?.fullName
                if (text.isNullOrEmpty() && pinned.isHasAttachments) {
                    setText(getString(R.string.attachments))
                }
                pinnedSubtitle?.text = OwnerLinkSpanFactory.withSpans(
                    text,
                    owners = true,
                    topics = false,
                    listener = object : OwnerLinkSpanFactory.ActionListener {
                        override fun onTopicLinkClicked(link: TopicLink) {

                        }

                        override fun onOwnerClick(ownerId: Long) {
                            presenter?.fireOwnerClick(ownerId)
                        }

                        override fun onOtherClick(URL: String) {

                        }
                    })
                buttonUnpin?.visibility = if (canChange) View.VISIBLE else View.GONE
                pinnedView?.setOnClickListener { presenter?.fireMessagesLookup(pinned) }
            }
        }
    }

    override fun hideInputView() {
        inputViewController?.run {
            switchModeToDisabled()
        }
    }

    override fun onVoiceHolderBinded(voiceMessageId: Int, voiceHolderId: Int) {
        presenter?.fireVoiceHolderCreated(voiceMessageId, voiceHolderId)
    }

    override fun onVoicePlayButtonClick(
        voiceHolderId: Int,
        voiceMessageId: Int,
        messageId: Int,
        peerId: Long,
        voiceMessage: VoiceMessage
    ) {
        presenter?.fireVoicePlayButtonClick(
            voiceHolderId,
            voiceMessageId,
            messageId,
            peerId,
            voiceMessage
        )
    }

    override fun onVoiceTogglePlaybackSpeed() {
        presenter?.fireVoicePlaybackSpeed()
    }

    override fun onTranscript(voiceMessageId: String, messageId: Int) {
        presenter?.fireTranscript(voiceMessageId, messageId)
    }

    override fun onStickerClick(sticker: Sticker) {
        presenter?.fireStickerSendClick(sticker)
    }

    override fun onMyStickerClick(file: Sticker.LocalSticker) {
        presenter?.fireSendMyStickerClick(file)
    }

    override fun onHashTagClicked(hashTag: String) {
        presenter?.fireHashtagClick(hashTag)
    }

    override fun showActionMode(
        title: String,
        canEdit: Boolean,
        canPin: Boolean,
        canStar: Boolean,
        doStar: Boolean,
        canSpam: Boolean
    ) {
        if (!Settings.get().main().isMessages_menu_down) {
            toolbarRootView?.run {
                if (childCount == Constants.FRAGMENT_CHAT_APP_BAR_VIEW_COUNT) {
                    val v =
                        LayoutInflater.from(context).inflate(R.layout.view_action_mode, this, false)
                    actionModeHolder = ActionModeHolder(v, this@ChatFragment)
                    addView(v)
//              bringChildToFront(v)
                }
            }
        } else {
            inputViewController?.closeBotKeyboard()
            inputViewController?.showEmoji(false)
            inputView?.visibility = View.INVISIBLE
            downMenuGroup?.run {
                visibility = View.VISIBLE
                if (childCount == Constants.FRAGMENT_CHAT_DOWN_MENU_VIEW_COUNT) {
                    val v =
                        LayoutInflater.from(context).inflate(R.layout.view_action_mode, this, false)
                    actionModeHolder = ActionModeHolder(v, this@ChatFragment)
                    addView(v)
//              bringChildToFront(v)
                }
            }
        }

        actionModeHolder?.run {
            show()
            titleView.text = title
            buttonSpam.visibility = if (canSpam) View.VISIBLE else View.GONE
            buttonEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
            buttonPin.visibility = if (canPin) View.VISIBLE else View.GONE
            buttonStar.visibility = if (canStar) View.VISIBLE else View.GONE
            buttonStar.setImageResource(if (doStar) R.drawable.star_add else R.drawable.star_none)
        }
    }

    override fun finishActionMode() {
        actionModeHolder?.rootView?.visibility = View.GONE
        if (Settings.get().main().isMessages_menu_down) {
            inputView?.visibility = View.VISIBLE
            downMenuGroup?.visibility = View.GONE
        }
    }

    private val openRequestPhotoResize =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let {
                    presenter?.fireFilePhotoForUploadSelected(
                        it.getStringExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH),
                        Upload.IMAGE_SIZE_FULL
                    )
                }
            }
        }

    private fun onEditLocalPhotosSelected(photos: List<LocalPhoto>) {
        when (val defaultSize = Settings.get().main().uploadImageSize) {
            null -> {
                ImageSizeAlertDialog.Builder(requireActivity())
                    .setOnSelectedCallback(object : ImageSizeAlertDialog.OnSelectedCallback {
                        override fun onSizeSelected(size: Int) {
                            presenter?.fireEditLocalPhotosSelected(
                                photos,
                                size
                            )
                        }
                    })
                    .show()
            }

            Upload.IMAGE_SIZE_CROPPING -> {
                if (photos.size == 1) {
                    var to_up = photos[0].fullImageUri ?: return
                    if (to_up.path?.let { File(it).isFile } == true) {
                        to_up = Uri.fromFile(to_up.path?.let { File(it) })
                    }

                    openRequestPhotoResize.launch(
                        Intent(requireContext(), IMGEditActivity::class.java)
                            .putExtra(IMGEditActivity.EXTRA_IMAGE_URI, to_up)
                            .putExtra(
                                IMGEditActivity.EXTRA_IMAGE_SAVE_PATH,
                                File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg").absolutePath
                            )
                    )
                } else {
                    presenter?.fireEditLocalPhotosSelected(photos, Upload.IMAGE_SIZE_FULL)
                }
            }

            else -> presenter?.fireEditLocalPhotosSelected(photos, defaultSize)
        }
    }

    private fun onEditLocalVideoSelected(video: LocalVideo) {
        presenter?.fireEditLocalVideoSelected(video)
    }

    private fun onEditLocalFileSelected(file: String) {
        for (i in Settings.get().main().photoExt) {
            if (file.endsWith(i, true)) {
                when (val defaultSize = Settings.get().main().uploadImageSize) {
                    null -> {
                        ImageSizeAlertDialog.Builder(requireActivity())
                            .setOnSelectedCallback(object :
                                ImageSizeAlertDialog.OnSelectedCallback {
                                override fun onSizeSelected(size: Int) {
                                    presenter?.fireFilePhotoForUploadSelected(
                                        file,
                                        size
                                    )
                                }
                            })
                            .show()
                    }

                    Upload.IMAGE_SIZE_CROPPING -> {
                        openRequestPhotoResize.launch(
                            Intent(requireContext(), IMGEditActivity::class.java)
                                .putExtra(
                                    IMGEditActivity.EXTRA_IMAGE_URI,
                                    Uri.fromFile(File(file))
                                )
                                .putExtra(
                                    IMGEditActivity.EXTRA_IMAGE_SAVE_PATH,
                                    File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg").absolutePath
                                )
                        )
                    }

                    else -> presenter?.fireFilePhotoForUploadSelected(file, defaultSize)
                }
                return
            }
        }
        for (i in Settings.get().main().videoExt) {
            if (file.endsWith(i, true)) {
                presenter?.fireFileVideoForUploadSelected(file)
                return
            }
        }
        for (i in Settings.get().main().audioExt) {
            if (file.endsWith(i, true)) {
                presenter?.fireFileAudioForUploadSelected(file)
                return
            }
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.select)
            .setNegativeButton(R.string.select_audio) { _, _ ->
                run {
                    presenter?.fireFileAudioForUploadSelected(file)
                }
            }
            .setNeutralButton(R.string.video) { _, _ ->
                run {
                    presenter?.fireFileVideoForUploadSelected(file)
                }
            }
            .setPositiveButton(R.string.photo) { _, _ ->
                run {
                    when (val defaultSize = Settings.get().main().uploadImageSize) {
                        null -> {
                            ImageSizeAlertDialog.Builder(requireActivity())
                                .setOnSelectedCallback(object :
                                    ImageSizeAlertDialog.OnSelectedCallback {
                                    override fun onSizeSelected(size: Int) {
                                        presenter?.fireFilePhotoForUploadSelected(
                                            file,
                                            size
                                        )
                                    }
                                })
                                .show()
                        }

                        Upload.IMAGE_SIZE_CROPPING -> {
                            openRequestPhotoResize.launch(
                                Intent(requireContext(), IMGEditActivity::class.java)
                                    .putExtra(
                                        IMGEditActivity.EXTRA_IMAGE_URI,
                                        Uri.fromFile(File(file))
                                    )
                                    .putExtra(
                                        IMGEditActivity.EXTRA_IMAGE_SAVE_PATH,
                                        File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg").absolutePath
                                    )
                            )
                        }

                        else -> presenter?.fireFilePhotoForUploadSelected(file, defaultSize)
                    }
                }
            }
            .create().show()
    }

    override fun goToMessageAttachmentsEditor(
        accountId: Long, messageOwnerId: Long, destination: UploadDestination,
        text: String?, attachments: ModelsBundle?, isGroupChat: Boolean
    ) {
        val fragment = MessageAttachmentsFragment.newInstance(
            accountId,
            messageOwnerId,
            destination.id,
            attachments,
            isGroupChat
        )
        fragment.show(parentFragmentManager, "message-attachments")
    }

    private val openRequestPhoto =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == RESULT_OK) {
                val vkphotos: List<Photo> =
                    result.data?.getParcelableArrayListExtraCompat(Extra.ATTACHMENTS)
                        ?: emptyList()
                val localPhotos: List<LocalPhoto> =
                    result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
                        ?: emptyList()

                val vid: LocalVideo? = result.data?.getParcelableExtraCompat(Extra.VIDEO)

                val file = result.data?.getStringExtra(Extra.PATH)

                if (file != null && file.nonNullNoEmpty()) {
                    onEditLocalFileSelected(file)
                } else if (vkphotos.isNotEmpty()) {
                    presenter?.fireEditAttachmentsSelected(vkphotos)
                } else if (localPhotos.isNotEmpty()) {
                    onEditLocalPhotosSelected(localPhotos)
                } else if (vid != null) {
                    onEditLocalVideoSelected(vid)
                }
            }
        }

    override fun startImagesSelection(accountId: Long, ownerId: Long) {
        val sources = Sources()
            .with(LocalPhotosSelectableSource())
            .with(LocalGallerySelectableSource())
            .with(LocalVideosSelectableSource())
            .with(VKPhotosSelectableSource(accountId, ownerId))
            .with(FileManagerSelectableSource())

        val intent = DualTabPhotoActivity.createIntent(requireActivity(), 10, sources)
        openRequestPhoto.launch(intent)
    }

    private val openRequestAudioVideoDoc =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == RESULT_OK) {
                val attachments: List<AbsModel> =
                    result.data?.getParcelableArrayListExtraCompat(Extra.ATTACHMENTS)
                        ?: emptyList()
                presenter?.fireEditAttachmentsSelected(attachments)
            }
        }

    override fun startVideoSelection(accountId: Long, ownerId: Long) {
        val intent = VideoSelectActivity.createIntent(requireActivity(), accountId, ownerId)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun startAudioSelection(accountId: Long) {
        val intent = AudioSelectActivity.createIntent(requireActivity(), accountId)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun startDocSelection(accountId: Long) {
        val intent =
            AttachmentsActivity.createIntent(requireActivity(), accountId, AttachmentsTypes.DOC)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun openPollCreationWindow(accountId: Long, ownerId: Long) {
        CreatePollDialogFragment.newInstance(accountId, ownerId, true)
            .show(parentFragmentManager, "poll_edit")
    }

    internal fun onEditCameraClick() {
        if (AppPerms.hasCameraPermission(requireContext())) {
            presenter?.fireEditCameraClick()
        } else {
            if (Utils.hasScopedStorage())
                requestCameraEditPermissionScoped.launch()
            else
                requestCameraEditPermission.launch()
        }
    }

    internal fun onCompressSettingsClick() {
        presenter?.fireCompressSettings(requireActivity())
    }

    override fun startCamera(fileUri: Uri) {
        openCameraRequest.launch(fileUri)
    }

    override fun showDeleteForAllDialog(
        removeAllIds: ArrayList<Message>,
        editRemoveAllIds: ArrayList<Message>
    ) {
        val msg = MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(R.string.confirmation)
            setMessage(R.string.messages_delete_for_all_question_message)
            setCancelable(true)
            setPositiveButton(R.string.button_for_all) { _, _ ->
                presenter?.fireDeleteForAllClick(
                    removeAllIds
                )
            }
            setNegativeButton(R.string.button_for_me) { _, _ ->
                presenter?.fireDeleteForMeClick(
                    removeAllIds
                )
            }
        }
        if (editRemoveAllIds.nonNullNoEmpty()) {
            msg.setNeutralButton(R.string.button_super_delete) { _, _ ->
                presenter?.fireDeleteSuper(
                    editRemoveAllIds
                )
            }
        }
        msg.show()
    }

    @SuppressLint("ShowToast")
    override fun showSnackbar(@StringRes res: Int, isLong: Boolean) {
        CustomSnackbars.createCustomSnackbars(view, inputView)
            ?.setDurationSnack(if (isLong) BaseTransientBottomBar.LENGTH_LONG else BaseTransientBottomBar.LENGTH_SHORT)
            ?.defaultSnack(res)?.show()
    }

    private class EditAttachmentsHolder(
        rootView: View,
        fragment: ChatFragment,
        attachments: MutableList<AttachmentEntry>,
        isGroupChat: Boolean
    ) : AttachmentsBottomSheetAdapter.ActionListener, View.OnClickListener {
        override fun onClick(v: View) {
            when (v.id) {
                R.id.buttonHide -> reference.get()?.hideEditAttachmentsDialog()
                R.id.buttonSave -> reference.get()?.onEditAttachmentSaveClick()
                R.id.buttonVideo -> reference.get()?.presenter?.onEditAddVideoClick()
                R.id.buttonAudio -> reference.get()?.presenter?.onEditAddAudioClick()
                R.id.buttonDoc -> reference.get()?.presenter?.onEditAddDocClick()
                R.id.buttonCamera -> reference.get()?.onEditCameraClick()
                R.id.button_photo_settings -> reference.get()?.onCompressSettingsClick()
            }
        }

        override fun onAddPhotoButtonClick() {
            reference.get()?.presenter?.fireEditAddImageClick()
        }

        override fun onButtonRemoveClick(entry: AttachmentEntry) {
            reference.get()?.presenter?.fireEditAttachmentRemoved(entry)
        }

        override fun onButtonRetryClick(entry: AttachmentEntry) {
            reference.get()?.presenter?.fireEditAttachmentRetry(entry)
        }

        val reference = WeakReference(fragment)
        val recyclerView: RecyclerView = rootView.findViewById(R.id.recyclerView)
        val emptyView: View = rootView.findViewById(R.id.no_attachments_text)
        val adapter = AttachmentsBottomSheetAdapter(attachments, this)

        init {
            recyclerView.layoutManager =
                LinearLayoutManager(rootView.context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.adapter = adapter

            rootView.findViewById<View>(R.id.buttonHide).setOnClickListener(this)
            rootView.findViewById<View>(R.id.buttonVideo).setOnClickListener(this)
            rootView.findViewById<View>(R.id.buttonAudio).setOnClickListener(this)
            rootView.findViewById<View>(R.id.buttonDoc).setOnClickListener(this)
            rootView.findViewById<View>(R.id.buttonCamera).setOnClickListener(this)
            rootView.findViewById<View>(R.id.buttonSave).setOnClickListener(this)
            rootView.findViewById<View>(R.id.button_photo_settings).setOnClickListener(this)

            rootView.findViewById<View>(R.id.button_photo_settings).visibility =
                if (Settings.get()
                        .main().isChange_upload_size || isGroupChat
                ) View.VISIBLE else View.GONE

            rootView.findViewById<ImageView>(R.id.button_photo_settings)
                .setImageResource(if (isGroupChat) R.drawable.chart_bar else R.drawable.photo_sizes)

            checkEmptyViewVisibility()
        }

        fun checkEmptyViewVisibility() {
            emptyView.visibility = if (adapter.itemCount < 2) View.VISIBLE else View.INVISIBLE
        }

        fun notifyAttachmentRemoved(index: Int) {
            adapter.notifyItemRemoved(index + 1)
            checkEmptyViewVisibility()
        }

        fun notifyAttachmentChanged(index: Int) {
            adapter.notifyItemChanged(index + 1)
        }

        fun notifyAttachmentsAdded(position: Int, count: Int) {
            adapter.notifyItemRangeInserted(position + 1, count)
            checkEmptyViewVisibility()
        }

        fun notifyAttachmentProgressUpdate(id: Int, progress: Int) {
            adapter.changeUploadProgress(id, progress, true)
        }
    }

    override fun notifyEditUploadProgressUpdate(id: Int, progress: Int) {
        editAttachmentsHolder?.notifyAttachmentProgressUpdate(id, progress)
    }

    override fun notifyEditAttachmentsAdded(position: Int, size: Int) {
        editAttachmentsHolder?.notifyAttachmentsAdded(position, size)
    }

    override fun notifyEditAttachmentChanged(index: Int) {
        editAttachmentsHolder?.notifyAttachmentChanged(index)
    }

    override fun notifyEditAttachmentRemoved(index: Int) {
        editAttachmentsHolder?.notifyAttachmentRemoved(index)
    }

    internal fun onEditAttachmentSaveClick() {
        hideEditAttachmentsDialog()
        presenter?.fireEditMessageSaveClick()
    }

    internal fun hideEditAttachmentsDialog() {
        editAttachmentsDialog?.dismiss()
    }

    override fun onSaveClick() {
        presenter?.fireEditMessageSaveClick()
    }

    private var editAttachmentsHolder: EditAttachmentsHolder? = null
    private var editAttachmentsDialog: Dialog? = null

    override fun showEditAttachmentsDialog(
        attachments: MutableList<AttachmentEntry>,
        isGroupChat: Boolean
    ) {
        val view = View.inflate(requireActivity(), R.layout.bottom_sheet_attachments_edit, null)

        val reference = WeakReference(this)
        editAttachmentsHolder = EditAttachmentsHolder(view, this, attachments, isGroupChat)
        editAttachmentsDialog = LandscapeExpandBottomSheetDialog(requireActivity())
            .apply {
                setContentView(view)
                setOnDismissListener { reference.get()?.editAttachmentsHolder = null }
                show()
            }
    }

    override fun showErrorSendDialog(message: Message) {
        val items = arrayOf(getString(R.string.try_again), getString(R.string.delete))

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.sending_message_failed)
            .setItems(items) { _, i ->
                when (i) {
                    0 -> presenter?.fireSendAgainClick(message)
                    1 -> presenter?.fireErrorMessageDeleteClick(message)
                }
            }
            .setCancelable(true)
            .show()
    }

    override fun notifyItemRemoved(position: Int) {
        adapter?.run {
            notifyItemRemoved(position + headersCount) // +headers count
        }
    }

    override fun configOptionMenu(
        canLeaveChat: Boolean,
        canChangeTitle: Boolean,
        canShowMembers: Boolean,
        encryptionStatusVisible: Boolean,
        encryptionEnabled: Boolean,
        encryptionPlusEnabled: Boolean,
        keyExchangeVisible: Boolean,
        chronoVisible: Boolean,
        ProfileVisible: Boolean,
        InviteLink: Boolean
    ) {
        optionMenuSettings.apply {
            put(LEAVE_CHAT_VISIBLE, canLeaveChat)
            put(CHANGE_CHAT_TITLE_VISIBLE, canChangeTitle)
            put(CHAT_MEMBERS_VISIBLE, canShowMembers)
            put(ENCRYPTION_STATUS_VISIBLE, encryptionStatusVisible)
            put(ENCRYPTION_ENABLED, encryptionEnabled)
            put(ENCRYPTION_PLUS_ENABLED, encryptionPlusEnabled)
            put(KEY_EXCHANGE_VISIBLE, keyExchangeVisible)
            put(CHRONO_VISIBLE, chronoVisible)
            put(PROFILE_VISIBLE, ProfileVisible)
            put(CAN_GENERATE_INVITE_LINK, InviteLink)
        }

        try {
            prepareOptionsMenu(toolbar?.menu ?: return)
        } catch (_: Exception) {

        }

    }

    override fun goToSearchMessage(accountId: Long, peer: Peer) {
        val criteria = MessageSearchCriteria("").setPeerId(peer.id)
        PlaceFactory.getSingleTabSearchPlace(accountId, SearchContentType.MESSAGES, criteria)
            .tryOpenWith(requireActivity())
    }

    private val openRequestUploadChatAvatar =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { presenter?.fireNewChatPhotoSelected(UCrop.getOutput(it)!!.path!!) }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                result.data?.let { showThrowable(UCrop.getError(it)) }
            }
        }

    override fun showImageSizeSelectDialog(streams: List<Uri>) {
        ImageSizeAlertDialog.Builder(requireActivity())
            .setOnSelectedCallback(object : ImageSizeAlertDialog.OnSelectedCallback {
                override fun onSizeSelected(size: Int) {
                    if (size == Upload.IMAGE_SIZE_CROPPING && streams.size == 1) {
                        var to_up = streams[0]
                        if (File(to_up.path ?: return).isFile) {
                            to_up = Uri.fromFile(File(to_up.path ?: return))
                        }

                        openRequestPhotoResize.launch(
                            Intent(requireContext(), IMGEditActivity::class.java)
                                .putExtra(IMGEditActivity.EXTRA_IMAGE_URI, to_up)
                                .putExtra(
                                    IMGEditActivity.EXTRA_IMAGE_SAVE_PATH,
                                    File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg").absolutePath
                                )
                        )
                    } else {
                        presenter?.fireImageUploadSizeSelected(streams, size)
                    }
                }
            })
            .setOnCancelCallback(object : ImageSizeAlertDialog.OnCancelCallback {
                override fun onCancel() {
                    presenter?.fireUploadCancelClick()
                }
            })
            .show()
    }

    override fun resetUploadImages() {
        ActivityUtils.resetInputPhotos(requireActivity())
    }

    override fun resetInputAttachments() {
        requireActivity().intent.removeExtra(MainActivity.EXTRA_INPUT_ATTACHMENTS)
        ActivityUtils.resetInputText(requireActivity())
    }

    private fun resolveToolbarNavigationIcon() {
        toolbar?.setNavigationIcon(R.drawable.arrow_left)
        toolbar?.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    private fun resolveLeftButton(peerId: Long) {
        try {
            if (toolbar != null) {
                resolveToolbarNavigationIcon()
                if (peerId < VKApiMessage.CHAT_PEER && peerId < VKApiMessage.CONTACT_PEER) {
                    toolbarAvatar?.setOnClickListener {
                        showUserWall(Settings.get().accounts().current, peerId)
                    }
                    toolbarAvatar?.setOnLongClickListener {
                        presenter?.fireLongAvatarClick(peerId)
                        true
                    }
                } else if (peerId < VKApiMessage.CONTACT_PEER) {
                    toolbarAvatar?.setOnClickListener {
                        presenter?.fireShowChatMembers()
                    }
                    toolbarAvatar?.setOnLongClickListener {
                        appendMessageText("@all,")
                        true
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    internal fun insertDomain(owner: Owner) {
        if (owner.domain.nonNullNoEmpty()) {
            appendMessageText("@" + owner.domain + ",")
        } else {
            appendMessageText("@id" + owner.ownerId + ",")
        }
    }

    override fun notifyChatResume(accountId: Long, peerId: Long, title: String?, image: String?) {
        if (activity is OnSectionResumeCallback) {
            (activity as OnSectionResumeCallback).onChatResume(accountId, peerId, title, image)
        }
        toolbarTitle?.text = title
        resolveLeftButton(peerId)
    }

    override fun goToConversationAttachments(accountId: Long, peerId: Long) {
        val types = arrayOf(
            FindAttachmentType.TYPE_PHOTO,
            FindAttachmentType.TYPE_VIDEO,
            FindAttachmentType.TYPE_DOC,
            FindAttachmentType.TYPE_AUDIO,
            FindAttachmentType.TYPE_LINK,
            FindAttachmentType.TYPE_POST,
            FindAttachmentType.TYPE_MULTI
        )

        val menus = ModalBottomSheetDialogFragment.Builder().apply {
            add(OptionRequest(0, getString(R.string.photos), R.drawable.photo_album, true))
            add(OptionRequest(1, getString(R.string.videos), R.drawable.video, true))
            add(OptionRequest(2, getString(R.string.documents), R.drawable.book, true))
            add(OptionRequest(3, getString(R.string.music), R.drawable.song, true))
            add(OptionRequest(4, getString(R.string.links), R.drawable.web, true))
            add(OptionRequest(5, getString(R.string.posts), R.drawable.pencil, true))
            add(OptionRequest(6, getString(R.string.search), R.drawable.magnify, true))
        }

        menus.show(childFragmentManager, "attachments_select") { _, option ->
            showConversationAttachments(accountId, peerId, types[option.id])
        }
    }

    private fun showConversationAttachments(accountId: Long, peerId: Long, type: String) {
        PlaceFactory.getConversationAttachmentsPlace(accountId, peerId, type)
            .tryOpenWith(requireActivity())
    }

    override fun goToChatMembers(accountId: Long, chatId: Long) {
        PlaceFactory.getChatMembersPlace(accountId, chatId).tryOpenWith(requireActivity())
    }

    override fun showChatMembers(accountId: Long, chatId: Long) {
        ChatUsersDomainFragment.newInstance(
            Settings.get().accounts().current,
            chatId, object : ChatUsersDomainFragment.Listener {
                override fun onSelected(user: Owner) {
                    insertDomain(user)
                }
            }
        ).show(childFragmentManager, "chat_users_domain")
    }

    private val openRequestSelectPhotoToChatAvatar =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == RESULT_OK) {
                val photos: ArrayList<LocalPhoto>? =
                    result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
                if (photos.nonNullNoEmpty()) {
                    var to_up = photos[0].fullImageUri
                    if (File(to_up?.path ?: return@registerForActivityResult).isFile) {
                        to_up = Uri.fromFile(File(to_up.path ?: return@registerForActivityResult))
                    }
                    openRequestUploadChatAvatar.launch(
                        to_up?.let {
                            UCrop.of(
                                it,
                                Uri.fromFile(File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg"))
                            )
                                .withOptions(
                                    UCrop.Options().withAspectRatio(1f, 1f)
                                        .setCompressionQuality(100)
                                        .setCompressionFormat(Bitmap.CompressFormat.JPEG)
                                        .setHideBottomControls(false)
                                )
                                .getIntent(requireActivity())
                        }
                    )
                }
            }
        }

    override fun showChatTitleChangeDialog(initialValue: String?) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.edit_chat)
            .setCancelable(true)
            .setPositiveButton(R.string.change_name) { _, _ ->
                run {
                    InputTextDialog.Builder(requireActivity())
                        .setAllowEmpty(false)
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .setValue(initialValue)
                        .setTitleRes(R.string.change_chat_title)
                        .setCallback(object : InputTextDialog.Callback {
                            override fun onChanged(newValue: String?) {
                                if (newValue != null) {
                                    presenter?.fireChatTitleTyped(newValue)
                                }
                            }

                            override fun onCanceled() {
                            }
                        })
                        .show()
                }
            }
            .setNeutralButton(R.string.change_photo) { _, _ ->
                val attachPhotoIntent = Intent(requireActivity(), PhotosActivity::class.java)
                attachPhotoIntent.putExtra(PhotosActivity.EXTRA_MAX_SELECTION_COUNT, 1)
                openRequestSelectPhotoToChatAvatar.launch(attachPhotoIntent)
            }
            .setNegativeButton(R.string.delete_photo) { _, _ -> presenter?.fireDeleteChatPhoto() }
            .show()
    }

    override fun showUserWall(accountId: Long, peerId: Long) {
        PlaceFactory.getOwnerWallPlace(accountId, peerId, null).tryOpenWith(requireActivity())
    }

    override fun forwardMessagesToAnotherConversation(
        messages: ArrayList<Message>,
        accountId: Long
    ) {
        SendAttachmentsActivity.startForSendAttachments(
            requireActivity(),
            accountId,
            FwdMessages(messages)
        )
    }

    override fun displayForwardTypeSelectDialog(messages: ArrayList<Message>) {
        val items = arrayOf(getString(R.string.here), getString(R.string.to_another_dialogue))

        val listener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                0 -> presenter?.fireForwardToHereClick(messages)
                1 -> presenter?.fireForwardToAnotherClick(messages)
            }
        }

        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items, listener)
            .setCancelable(true)
            .show()
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        emptyText?.isVisible = visible
        emptyAnimation?.isVisible = visible
        if (visible) {
            emptyAnimation?.fromRes(
                dev.ragnarok.fenrir_common.R.raw.valknut,
                intArrayOf(
                    0x333333,
                    CurrentTheme.getColorPrimary(requireActivity()),
                    0x777777,
                    CurrentTheme.getColorSecondary(requireActivity())
                )
            )
            emptyAnimation?.startAnimation()
        } else {
            emptyAnimation?.clearAnimationDrawable(
                callSuper = true, clearState = true,
                cancelTask = true
            )
        }
    }

    override fun setupRecordPauseButton(isRecording: Boolean) {
        inputViewController?.setupRecordPauseButton(isRecording)
    }

    override fun displayInitiateKeyExchangeQuestion(@KeyLocationPolicy keyStoragePolicy: Int) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.key_exchange)
            .setMessage(R.string.you_dont_have_encryption_keys_stored_initiate_key_exchange)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                presenter?.fireInitiateKeyExchangeClick(
                    keyStoragePolicy
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun showEncryptionKeysPolicyChooseDialog(requestCode: Int) {
        val view = View.inflate(activity, R.layout.dialog_select_encryption_key_policy, null)
        val buttonOnDisk = view.findViewById<MaterialRadioButton>(R.id.button_on_disk)
        val buttonInRam = view.findViewById<MaterialRadioButton>(R.id.button_in_ram)

        buttonOnDisk.isChecked = true

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.choose_location_key_store)
            .setView(view)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                if (buttonOnDisk.isChecked) {
                    presenter?.fireDiskKeyStoreSelected(requestCode)
                } else if (buttonInRam.isChecked) {
                    presenter?.fireRamKeyStoreSelected(requestCode)
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun showEncryptionDisclaimerDialog(requestCode: Int) {
        val view = View.inflate(activity, R.layout.content_encryption_terms_of_use, null)
        MaterialAlertDialogBuilder(requireActivity())
            .setView(view)
            .setTitle(R.string.fenrir_encryption)
            .setPositiveButton(R.string.button_accept) { _, _ ->
                presenter?.fireTermsOfUseAcceptClick(
                    requestCode
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    @SuppressLint("ResourceType")
    fun prepareOptionsMenu(menu: Menu) {
        menu.run {
            findItem(R.id.action_leave_chat).isVisible =
                optionMenuSettings.get(LEAVE_CHAT_VISIBLE, false)
            findItem(R.id.action_edit_chat).isVisible =
                optionMenuSettings.get(CHANGE_CHAT_TITLE_VISIBLE, false)
            findItem(R.id.action_chat_members).isVisible =
                optionMenuSettings.get(CHAT_MEMBERS_VISIBLE, false)
            findItem(R.id.action_key_exchange).isVisible =
                optionMenuSettings.get(KEY_EXCHANGE_VISIBLE, false)
            findItem(R.id.change_chrono_history).isVisible =
                optionMenuSettings.get(CHRONO_VISIBLE, false)
            findItem(R.id.show_profile).isVisible = optionMenuSettings.get(PROFILE_VISIBLE, false)
            findItem(R.id.action_invite_link).isVisible =
                optionMenuSettings.get(CAN_GENERATE_INVITE_LINK, false)
        }
        val encryptionStatusItem = menu.findItem(R.id.crypt_state)
        val encryptionStatusVisible = optionMenuSettings.get(ENCRYPTION_STATUS_VISIBLE, false)

        encryptionStatusItem.isVisible = encryptionStatusVisible

        if (encryptionStatusVisible) {
            @DrawableRes
            var drawableRes = R.drawable.ic_outline_lock_open

            if (optionMenuSettings.get(ENCRYPTION_ENABLED, false)) {
                drawableRes = if (optionMenuSettings.get(ENCRYPTION_PLUS_ENABLED, false)) {
                    R.drawable.lock_plus
                } else {
                    R.drawable.ic_outline_lock
                }
            }

            try {
                encryptionStatusItem.setIcon(drawableRes)
            } catch (_: Exception) {
                //java.lang.NullPointerException: Attempt to invoke virtual method
                // 'android.content.res.Resources$Theme android.app.Activity.getTheme()' on a null object reference
            }

        }
    }

    override fun scrollTo(position: Int) {
        recyclerView?.smoothScrollToPosition(position)
    }

    private val requestWriteScreenshotPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        ScreenshotHelper.makeScreenshot(requireActivity())
    }

    private val requestSendCustomVoicePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        requestFile.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                null, getString(R.string.send_file_as_voice)
            )
        )
    }

    private fun downloadChat() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmation)
            .setMessage(R.string.select_type)
            .setPositiveButton(R.string.to_json) { _, _ -> doDownloadChat("json") }
            .setNegativeButton(R.string.to_html) { _, _ -> doDownloadChat("html") }
            .setNeutralButton(R.string.button_cancel, null)
            .show()
    }

    private val requestWriteChatPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        downloadChat()
    }

    private fun optionsMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.last_read -> {
                presenter?.getConversation()?.unreadCount?.let {
                    recyclerView?.smoothScrollToPosition(
                        it
                    )
                }
                return true
            }

            R.id.action_refresh -> {
                recyclerView?.scrollToPosition(0)
                presenter?.resetChronology()
                presenter?.fireRefreshClick()
                return true
            }

            R.id.show_profile -> {
                presenter?.fireShowProfile()
                return true
            }

            R.id.action_short_link -> {
                presenter?.fireShortLinkClick(requireActivity())
                return true
            }

            R.id.change_chrono_history -> {
                recyclerView?.scrollToPosition(0)
                presenter?.invertChronology()
                presenter?.fireRefreshClick()
                return true
            }

            R.id.action_leave_chat -> {
                presenter?.fireLeaveChatClick()
                return true
            }

            R.id.delete_chat -> {
                CustomSnackbars.createCustomSnackbars(view, inputView)
                    ?.setDurationSnack(Snackbar.LENGTH_LONG)?.themedSnack(R.string.delete_chat_do)
                    ?.setAction(
                        R.string.button_yes
                    ) { presenter?.removeDialog() }?.show()
                return true
            }

            R.id.action_edit_chat -> {
                presenter?.fireChatTitleClick()
                return true
            }

            R.id.action_chat_members -> {
                presenter?.fireChatMembersClick()
                return true
            }

            R.id.make_screenshot -> {
                if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
                    requestWriteScreenshotPermission.launch()
                    return true
                }
                ScreenshotHelper.makeScreenshot(requireActivity())
            }

            R.id.download_peer_to -> {
                if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
                    requestWriteChatPermission.launch()
                    return true
                }
                downloadChat()
                return true
            }

            R.id.action_attachments_in_conversation -> presenter?.fireDialogAttachmentsClick()
            R.id.messages_search -> presenter?.fireSearchClick()
            R.id.crypt_state -> presenter?.fireEncryptionStatusClick()
            R.id.action_key_exchange -> presenter?.fireKeyExchangeClick()
            R.id.action_invite_link -> presenter?.fireGenerateInviteLink()
        }

        return false
    }

    private fun doDownloadChat(action: String) {
        presenter?.fireChatDownloadClick(requireActivity(), action)
    }

    override fun onInputTextChanged(s: String?) {
        presenter?.fireDraftMessageTextEdited(s)
        presenter?.fireTextEdited(s)
    }

    override fun onSendClicked(text: String?) {
        presenter?.fireSendClick()
    }

    override fun onAttachClick() {
        presenter?.fireAttachButtonClick()
    }

    override fun onBackPressed(): Boolean {
        if (actionModeHolder?.isVisible() == true) {
            actionModeHolder?.hide()
            return false
        }

        if (inputViewController?.onBackPressed() == false) {
            return false
        }

        if (presenter?.isChronologyInverted == true) {
            presenter?.resetChronology()
            presenter?.fireRefreshClick()
            return false
        }

        return presenter?.onBackPressed() == true
    }

    private fun isActionModeVisible(): Boolean {
        return actionModeHolder?.rootView?.visibility == View.VISIBLE
    }

    override fun onMessageDelete(message: Message) {
        val ids: ArrayList<Message> = ArrayList()
        ids.add(message)
        presenter?.fireDeleteForMeClick(ids)
    }

    override fun onAvatarClick(message: Message, userId: Long, position: Int) {
        if (isActionModeVisible()) {
            presenter?.fireMessageClick(message, position, null, null)
        } else {
            presenter?.fireOwnerClick(userId)
        }
    }

    override fun onRestoreClick(message: Message, position: Int) {
        presenter?.fireMessageRestoreClick(message)
    }

    override fun onBotKeyboardClick(button: Keyboard.Button) {
        presenter?.fireBotSendClick(button, requireActivity())
    }

    override fun onMessageLongClick(message: Message, position: Int): Boolean {
        presenter?.fireMessageLongClick(message, position)
        return true
    }

    override fun onMessageClicked(message: Message, position: Int, x: Int, y: Int) {
        presenter?.fireMessageClick(message, position, x, y)
    }

    override fun onLongAvatarClick(message: Message, userId: Long, position: Int) {
        presenter?.fireLongAvatarClick(userId)
    }

    override fun didPressedButton(button: Keyboard.Button, needClose: Boolean) {
        presenter?.fireBotSendClick(button, requireActivity())
        if (needClose) {
            inputViewController?.closeBotKeyboard()
        }
    }

    override fun onReactionClicked(reaction_id: Int?, conversation_message_id: Int, peerId: Long) {
        presenter?.fireReactionClicked(reaction_id, conversation_message_id, peerId)
    }

    override fun copyToClipBoard(link: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("response", link)
        clipboard?.setPrimaryClip(clip)
        CustomToast.createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
    }

    fun saveState() {
        inputViewController?.storeEmoji()
        presenter?.saveDraftMessageBody()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityManager = if (Settings.get().main().isAuto_read && !Settings.get()
                .accounts().currentHidden && AppPerms.hasAccessNetworkStatePermission(
                requireActivity()
            )
        ) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? else null

        connectivityManager?.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build(),
            networkCallback
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        inputViewController?.destroyView()
        inputViewController = null
    }

    companion object {
        fun newInstance(accountId: Long, messagesOwnerId: Long, peer: Peer): ChatFragment {
            val args = Bundle().apply {
                putLong(Extra.ACCOUNT_ID, accountId)
                putLong(Extra.OWNER_ID, messagesOwnerId)
                putParcelable(Extra.PEER, peer)
            }

            val fragment = ChatFragment()
            fragment.arguments = args
            return fragment
        }

        private const val LEAVE_CHAT_VISIBLE = 1
        private const val CHANGE_CHAT_TITLE_VISIBLE = 2
        private const val CHAT_MEMBERS_VISIBLE = 3
        private const val ENCRYPTION_STATUS_VISIBLE = 4
        private const val ENCRYPTION_ENABLED = 5
        private const val ENCRYPTION_PLUS_ENABLED = 6
        private const val KEY_EXCHANGE_VISIBLE = 7
        private const val CHRONO_VISIBLE = 8
        private const val PROFILE_VISIBLE = 9
        private const val CAN_GENERATE_INVITE_LINK = 10
    }
}
