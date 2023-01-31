package dev.ragnarok.fenrir.fragment.accounts

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.*
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Constants.DEFAULT_ACCOUNT_TYPE
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.EnterPinActivity
import dev.ragnarok.fenrir.activity.FileManagerSelectActivity
import dev.ragnarok.fenrir.activity.LoginActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.ProxyManagerActivity
import dev.ragnarok.fenrir.api.Auth.scope
import dev.ragnarok.fenrir.dialog.directauth.DirectAuthDialog
import dev.ragnarok.fenrir.dialog.directauth.DirectAuthDialog.Companion.newInstance
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.place.PlaceFactory.getPreferencesPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.MessagesReplyItemCallback
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.serializeble.json.*
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import okhttp3.*
import java.util.*

class AccountsFragment : BaseMvpFragment<AccountsPresenter, IAccountsView>(), IAccountsView,
    View.OnClickListener, AccountAdapter.Callback,
    MenuProvider {
    private val requestPinForExportAccount = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            startExportAccounts()
        }
    }
    private val requestWritePermissionExportAccount = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        if (Settings.get().security().isUsePinForSecurity) {
            requestPinForExportAccount.launch(
                Intent(
                    requireActivity(),
                    EnterPinActivity::class.java
                )
            )
        } else {
            createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
        }
    }
    private val requestWritePermissionExchangeToken = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        if (Settings.get().security().isUsePinForSecurity) {
            requestEnterPinForExchangeToken.launch(
                Intent(
                    requireActivity(),
                    EnterPinActivity::class.java
                )
            )
        } else {
            presenter?.fireResetTempAccount()
            createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
        }
    }
    private val requestReadPermissionImportAccount = requestPermissionsAbs(
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ) {
        startImportAccounts()
    }

    private val requestReadPermissionImportExchangeToken = requestPermissionsAbs(
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ) {
        startImportByExchangeToken()
    }

    private var empty: TextView? = null
    private var mRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: AccountAdapter? = null
    private val requestEnterPinForShowPassword = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val accountFromTmp = presenter?.fireResetAndGetTempAccount() ?: 0L
            if (accountFromTmp == 0L) {
                return@registerForActivityResult
            }
            val restore: SaveAccount =
                kJson.decodeFromString(
                    SaveAccount.serializer(),
                    Settings.get().accounts().getLogin(accountFromTmp)
                        ?: return@registerForActivityResult
                )
            val messageData = requireActivity().getString(
                R.string.restore_login_info,
                restore.login,
                restore.password,
                restore.two_factor_auth
            )
            MaterialAlertDialogBuilder(requireActivity())
                .setMessage(messageData)
                .setTitle(R.string.login_password_hint)
                .setNeutralButton(R.string.login_password_hint) { _: DialogInterface?, _: Int ->
                    val clipboard = requireActivity().getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager?
                    val clip = ClipData.newPlainText("response", restore.password)
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
                }
                .setNegativeButton(R.string.login_hint) { _: DialogInterface?, _: Int ->
                    val clipboard = requireActivity().getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager?
                    val clip =
                        ClipData.newPlainText("response", restore.login)
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
                }
                .setCancelable(true)
                .show()
        } else {
            presenter?.fireResetTempAccount()
        }
    }

    private val requestEnterPinForExchangeToken = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            presenter?.createExchangeToken(requireActivity())
        }
    }

    private val requestLoginWeb = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val uid = result.data?.extras?.getLong(Extra.USER_ID)
            val token = result.data?.getStringExtra(Extra.TOKEN)
            val Login = result.data?.getStringExtra(Extra.LOGIN)
            val Password = result.data?.getStringExtra(Extra.PASSWORD)
            val TwoFA = result.data?.getStringExtra(Extra.TWO_FA)
            val isSave = result.data?.getBooleanExtra(Extra.SAVE, false)
            if (uid != null) {
                if (isSave != null) {
                    presenter?.processNewAccount(
                        uid,
                        token,
                        DEFAULT_ACCOUNT_TYPE,
                        Login,
                        Password,
                        TwoFA,
                        true,
                        isSave
                    )
                }
            }
        }
    }

    private val importExchangeToken = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data?.getStringExtra(Extra.PATH)
                ?.let { presenter?.importExchangeToken(requireActivity(), it) }
        }
    }

    private val importAccounts = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data?.getStringExtra(Extra.PATH)?.let { presenter?.importAccounts(it) }
        }
    }

    private val exportAccounts = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data?.getStringExtra(Extra.PATH)
                ?.let { presenter?.exportAccounts(requireActivity(), it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_accounts, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        empty = root.findViewById(R.id.empty)
        mRecyclerView = root.findViewById(R.id.list)
        mRecyclerView?.layoutManager = LinearLayoutManager(
            requireActivity(),
            RecyclerView.VERTICAL,
            false
        )
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener { presenter?.fireLoad(true) }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        ItemTouchHelper(MessagesReplyItemCallback { o: Int ->
            if (mAdapter?.checkPosition(o) == true) {
                val account = mAdapter?.getByPosition(o) ?: return@MessagesReplyItemCallback
                val idCurrent = account.getOwnerObjectId() == Settings.get()
                    .accounts()
                    .current
                if (!idCurrent) {
                    presenter?.fireSetAsActive(account)
                }
            }
        }).attachToRecyclerView(mRecyclerView)
        root.findViewById<View>(R.id.fab).setOnClickListener(this)
        mAdapter = AccountAdapter(requireActivity(), Collections.emptyList(), this)
        mRecyclerView?.adapter = mAdapter
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        parentFragmentManager.setFragmentResultListener(
            DirectAuthDialog.ACTION_LOGIN_VIA_WEB,
            this
        ) { _: String?, _: Bundle? -> startLoginViaWeb() }
        parentFragmentManager.setFragmentResultListener(
            DirectAuthDialog.ACTION_VALIDATE_VIA_WEB,
            this
        ) { _: String?, result: Bundle ->
            val url = result.getString(Extra.URL)
            val Login = result.getString(Extra.LOGIN)
            val Password = result.getString(Extra.PASSWORD)
            val TwoFA = result.getString(Extra.TWO_FA)
            val isSave = result.getBoolean(Extra.SAVE)
            startValidateViaWeb(url, Login, Password, TwoFA, isSave)
        }
        parentFragmentManager.setFragmentResultListener(
            DirectAuthDialog.ACTION_LOGIN_COMPLETE,
            this
        ) { _: String?, result: Bundle ->
            val uid = result.getLong(Extra.USER_ID)
            val token = result.getString(Extra.TOKEN)
            val Login = result.getString(Extra.LOGIN)
            val Password = result.getString(Extra.PASSWORD)
            val TwoFA = result.getString(Extra.TWO_FA)
            val isSave = result.getBoolean(Extra.SAVE)
            presenter?.processNewAccount(
                uid,
                token,
                DEFAULT_ACCOUNT_TYPE,
                Login,
                Password,
                TwoFA,
                true,
                isSave
            )
        }
    }

    override fun resolveEmptyText(isEmpty: Boolean) {
        if (!isAdded || empty == null) return
        empty?.visibility =
            if (isEmpty) View.VISIBLE else View.INVISIBLE
    }

    override fun invalidateMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.title = null
            actionBar.subtitle = null
        }
    }

    @Suppress("DEPRECATION")
    private fun startExportAccounts() {
        exportAccounts.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                "dirs"
            )
        )
    }

    private fun startLoginViaWeb() {
        val intent = createIntent(requireActivity(), Constants.API_ID.toString(), scope)
        requestLoginWeb.launch(intent)
    }

    private fun startValidateViaWeb(
        url: String?,
        Login: String?,
        Password: String?,
        TwoFa: String?,
        needSave: Boolean
    ) {
        val intent = createIntent(requireActivity(), url, Login, Password, TwoFa, needSave)
        requestLoginWeb.launch(intent)
    }

    override fun startDirectLogin() {
        val auth = newInstance()
        auth.show(parentFragmentManager, "direct-login")
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyItemRangeRemoved(positionStart: Int, count: Int) {
        mAdapter?.notifyItemRangeRemoved(positionStart, count)
    }

    override fun notifyItemRangeInserted(positionStart: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(positionStart, count)
    }

    override fun showColoredSnack(text: String?, @ColorInt color: Int) {
        CustomSnackbars.createCustomSnackbars(
            view,
            mRecyclerView
        )
            ?.setDurationSnack(Snackbar.LENGTH_LONG)
            ?.coloredSnack(
                text,
                color
            )?.show()
    }

    override fun showColoredSnack(
        @StringRes resId: Int,
        @ColorInt color: Int,
        vararg params: Any?
    ) {
        CustomSnackbars.createCustomSnackbars(
            view,
            mRecyclerView
        )
            ?.setDurationSnack(Snackbar.LENGTH_LONG)
            ?.coloredSnack(
                resId,
                color,
                params
            )?.show()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            startDirectLogin()
        }
    }

    override fun onClick(account: Account) {
        val idCurrent = account.getOwnerObjectId() == Settings.get()
            .accounts()
            .current
        val menus = ModalBottomSheetDialogFragment.Builder()
        if (account.getOwnerObjectId() > 0) {
            menus.add(
                OptionRequest(
                    0,
                    getString(R.string.delete),
                    R.drawable.ic_outline_delete,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    1,
                    getString(R.string.add_to_home_screen),
                    R.drawable.plus,
                    false
                )
            )
            if (!Settings.get().accounts().getLogin(account.getOwnerObjectId()).isNullOrEmpty()) {
                menus.add(
                    OptionRequest(
                        3,
                        getString(R.string.login_password_hint),
                        R.drawable.view,
                        true
                    )
                )
            }
            if (Utils.isOfficialVKAccount(account.getOwnerObjectId())) {
                menus.add(
                    OptionRequest(
                        5,
                        getString(R.string.exchange_token),
                        R.drawable.save,
                        true
                    )
                )
            }

            if (!idCurrent) {
                menus.add(
                    OptionRequest(
                        2,
                        getString(R.string.set_as_active),
                        R.drawable.account_circle,
                        false
                    )
                )
            }
        } else {
            menus.add(
                OptionRequest(
                    0,
                    getString(R.string.delete),
                    R.drawable.ic_outline_delete,
                    true
                )
            )
        }
        if (isHiddenAccount(account.getOwnerObjectId())) {
            menus.add(
                OptionRequest(
                    4,
                    getString(R.string.set_device),
                    R.drawable.ic_smartphone,
                    false
                )
            )
        }
        menus.header(
            account.displayName,
            R.drawable.account_circle,
            account.owner?.maxSquareAvatar
        )
        menus.show(
            childFragmentManager,
            "account_options",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    when (option.id) {
                        0 -> presenter?.fireDelete(requireActivity(), account)
                        1 -> presenter?.createShortcut(requireActivity(), account)
                        2 -> presenter?.fireSetAsActive(account)
                        3 -> if (!Settings.get().security().isUsePinForSecurity) {
                            createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
                        } else {
                            presenter?.fireSetTempAccount(account.getOwnerObjectId())
                            requestEnterPinForShowPassword.launch(
                                Intent(
                                    requireActivity(),
                                    EnterPinActivity::class.java
                                )
                            )
                        }

                        4 -> {
                            val root =
                                View.inflate(requireActivity(), R.layout.dialog_enter_text, null)
                            (root.findViewById<View>(R.id.editText) as TextInputEditText).setText(
                                Settings.get().accounts().getDevice(account.getOwnerObjectId())
                            )
                            MaterialAlertDialogBuilder(requireActivity())
                                .setTitle(R.string.set_device)
                                .setCancelable(true)
                                .setView(root)
                                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                                    Settings.get().accounts().storeDevice(
                                        account.getOwnerObjectId(),
                                        (root.findViewById<View>(R.id.editText) as TextInputEditText).editableText.toString()
                                    )
                                    Includes.proxySettings.broadcastUpdate(null)
                                }
                                .setNegativeButton(R.string.button_cancel, null)
                                .show()
                        }

                        5 -> {
                            presenter?.fireSetTempAccount(account.getOwnerObjectId())
                            if (!hasReadWriteStoragePermission(requireActivity())) {
                                requestWritePermissionExchangeToken.launch()
                            } else {
                                if (!Settings.get().security().isUsePinForSecurity) {
                                    createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
                                } else {
                                    requestEnterPinForExchangeToken.launch(
                                        Intent(
                                            requireActivity(),
                                            EnterPinActivity::class.java
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            })
    }

    @Suppress("DEPRECATION")
    private fun startImportAccounts() {
        importAccounts.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                "json"
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun startImportByExchangeToken() {
        importExchangeToken.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                "json"
            )
        )
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_proxy -> {
                startProxySettings()
                return true
            }

            R.id.action_preferences -> {
                getPreferencesPlace(
                    Settings.get().accounts().current
                ).tryOpenWith(requireActivity())
                return true
            }

            R.id.entry_account -> {
                val root = View.inflate(requireActivity(), R.layout.entry_account, null)
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.entry_account)
                    .setCancelable(true)
                    .setView(root)
                    .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                        try {
                            val access_token =
                                (root.findViewById<View>(R.id.edit_access_token) as TextInputEditText).text.toString()
                                    .trim { it <= ' ' }
                            val selected =
                                (root.findViewById<View>(R.id.access_token_type) as Spinner).selectedItemPosition
                            val types = intArrayOf(
                                AccountType.VK_ANDROID,
                                AccountType.KATE,
                                AccountType.VK_ANDROID_HIDDEN,
                                AccountType.KATE_HIDDEN
                            )
                            if (access_token.isNotEmpty() && selected >= 0 && selected <= 3) {
                                presenter?.processAccountByAccessToken(
                                    access_token,
                                    types[selected]
                                )
                            }
                        } catch (ignored: NumberFormatException) {
                        }
                    }
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()
                return true
            }

            R.id.export_accounts -> {
                if (Settings.get()
                        .accounts().registered.isEmpty()
                ) return true
                if (!hasReadWriteStoragePermission(requireActivity())) {
                    requestWritePermissionExportAccount.launch()
                    return true
                }
                if (Settings.get().security().isUsePinForSecurity) {
                    requestPinForExportAccount.launch(
                        Intent(
                            requireActivity(),
                            EnterPinActivity::class.java
                        )
                    )
                } else {
                    createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
                }
                return true
            }

            R.id.import_accounts -> {
                if (!hasReadStoragePermission(requireActivity())) {
                    requestReadPermissionImportAccount.launch()
                    return true
                }
                startImportAccounts()
                return true
            }

            R.id.import_by_exchange_token -> {
                if (!hasReadStoragePermission(requireActivity())) {
                    requestReadPermissionImportExchangeToken.launch()
                    return true
                }
                startImportByExchangeToken()
                return true
            }

            else -> return false
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AccountsPresenter> {
        return object : IPresenterFactory<AccountsPresenter> {
            override fun create(): AccountsPresenter {
                return AccountsPresenter(saveInstanceState)
            }
        }
    }

    private fun startProxySettings() {
        startActivity(Intent(requireActivity(), ProxyManagerActivity::class.java))
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_accounts, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.export_accounts).isVisible = presenter?.isNotEmptyAccounts() ?: false
        menu.findItem(R.id.import_by_exchange_token).isVisible = Utils.isOfficialDefault
    }

    override fun displayData(accounts: List<Account>) {
        mAdapter?.setData(accounts)
    }

    override fun isLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }
}