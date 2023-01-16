package dev.ragnarok.fenrir.fragment.communitycontrol.communitymanageredit

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.settings.AvatarStyle
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView

class CommunityManagerEditFragment :
    BaseMvpFragment<CommunityManagerEditPresenter, ICommunityManagerEditView>(),
    ICommunityManagerEditView, MenuProvider {
    private var mAvatar: ImageView? = null
    private var mOnlineView: OnlineView? = null
    private var mName: TextView? = null
    private var mDomain: TextView? = null
    private var mButtonModerator: RadioButton? = null
    private var mButtonEditor: RadioButton? = null
    private var mButtonAdmin: RadioButton? = null
    private var mShowAsContact: MaterialCheckBox? = null
    private var mContactInfoRoot: ViewGroup? = null
    private var mPosition: TextInputEditText? = null
    private var mEmail: TextInputEditText? = null
    private var mPhone: TextInputEditText? = null
    private var mRadioGroupRoles: RadioGroup? = null
    private var mRadioGroupCreator: RadioGroup? = null
    private var mOptionDeleteVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_manager_edit, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mAvatar = root.findViewById(R.id.avatar)
        mAvatar?.setBackgroundResource(
            if (Settings.get()
                    .ui().avatarStyle == AvatarStyle.OVAL
            ) R.drawable.sel_button_square_5_white else R.drawable.sel_button_round_5_white
        )
        mAvatar?.setOnClickListener {
            presenter?.fireAvatarClick()
        }
        mOnlineView = root.findViewById(R.id.online)
        mName = root.findViewById(R.id.name)
        mDomain = root.findViewById(R.id.domain)
        mButtonModerator = root.findViewById(R.id.button_moderator)
        mButtonEditor = root.findViewById(R.id.button_editor)
        mButtonAdmin = root.findViewById(R.id.button_admin)
        mRadioGroupRoles = root.findViewById(R.id.radio_group_roles)
        mRadioGroupRoles?.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.button_moderator -> {
                    presenter?.fireModeratorChecked()
                }
                R.id.button_editor -> {
                    presenter?.fireEditorChecked()
                }
                R.id.button_admin -> {
                    presenter?.fireAdminChecked()
                }
            }
        }
        mRadioGroupCreator = root.findViewById(R.id.radio_group_creator)
        mShowAsContact = root.findViewById(R.id.community_manager_show_in_contacts)
        mShowAsContact?.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            presenter?.fireShowAsContactChecked(
                checked
            )
        }
        mContactInfoRoot = root.findViewById(R.id.contact_info_root)
        mPosition = root.findViewById(R.id.community_manager_positon)
        mPosition?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.firePositionEdit(
                    s
                )
            }
        })
        mEmail = root.findViewById(R.id.community_manager_email)
        mEmail?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireEmailEdit(
                    s
                )
            }
        })
        mPhone = root.findViewById(R.id.community_manager_phone)
        mPhone?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.firePhoneEdit(
                    s
                )
            }
        })
        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.community_manager_edit, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                presenter?.fireButtonSaveClick()
                true
            }
            R.id.action_delete -> {
                presenter?.fireDeleteClick()
                true
            }
            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.action_delete).isVisible = mOptionDeleteVisible
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityManagerEditPresenter> {
        return object : IPresenterFactory<CommunityManagerEditPresenter> {
            override fun create(): CommunityManagerEditPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val groupId = requireArguments().getLong(Extra.GROUP_ID)
                val users: List<User>? =
                    requireArguments().getParcelableArrayListCompat(Extra.USERS)
                val manager: Manager? = requireArguments().getParcelableCompat(Extra.MANAGER)
                return manager?.let {
                    CommunityManagerEditPresenter(
                        accountId,
                        groupId,
                        it,
                        saveInstanceState
                    )
                }
                    ?: CommunityManagerEditPresenter(
                        accountId,
                        groupId,
                        users.orEmpty(),
                        saveInstanceState
                    )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.edit_manager_title)
        setToolbarSubtitle(this, R.string.editing)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayUserInfo(user: User) {
        if (mAvatar != null) {
            displayAvatar(mAvatar, RoundTransformation(), user.maxSquareAvatar, null)
        }
        safelySetText(mName, user.fullName)
        val iconRes =
            getOnlineIcon(user.isOnline, user.isOnlineMobile, user.platform, user.onlineApp)
        if (mOnlineView != null) {
            mOnlineView?.visibility = if (iconRes != null) View.VISIBLE else View.INVISIBLE
            if (iconRes != null) {
                mOnlineView?.setIcon(iconRes)
            }
        }
        if (user.domain.nonNullNoEmpty()) {
            safelySetText(mDomain, "@" + user.domain)
        } else {
            safelySetText(mDomain, "@id" + user.getOwnerObjectId())
        }
    }

    override fun showUserProfile(accountId: Long, user: User) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun checkModerator() {
        safelySetChecked(mButtonModerator, true)
    }

    override fun checkEditor() {
        safelySetChecked(mButtonEditor, true)
    }

    override fun checkAdmin() {
        safelySetChecked(mButtonAdmin, true)
    }

    override fun setShowAsContactCheched(cheched: Boolean) {
        safelySetChecked(mShowAsContact, cheched)
    }

    override fun setContactInfoVisible(visible: Boolean) {
        safelySetVisibleOrGone(mContactInfoRoot, visible)
    }

    override fun displayPosition(position: String?) {
        safelySetText(mPosition, position)
    }

    override fun displayEmail(email: String?) {
        safelySetText(mEmail, email)
    }

    override fun displayPhone(phone: String?) {
        safelySetText(mPhone, phone)
    }

    override fun configRadioButtons(isCreator: Boolean) {
        safelySetVisibleOrGone(mRadioGroupRoles, !isCreator)
        safelySetVisibleOrGone(mRadioGroupCreator, isCreator)
    }

    override fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun setDeleteOptionVisible(visible: Boolean) {
        mOptionDeleteVisible = visible
        requireActivity().invalidateOptionsMenu()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    companion object {
        fun newInstance(
            accountId: Long,
            groupId: Long,
            users: ArrayList<User>?
        ): CommunityManagerEditFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.GROUP_ID, groupId)
            args.putParcelableArrayList(Extra.USERS, users)
            val fragment = CommunityManagerEditFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            accountId: Long,
            groupId: Long,
            manager: Manager?
        ): CommunityManagerEditFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.GROUP_ID, groupId)
            args.putParcelable(Extra.MANAGER, manager)
            val fragment = CommunityManagerEditFragment()
            fragment.arguments = args
            return fragment
        }
    }
}