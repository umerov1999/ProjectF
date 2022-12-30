package dev.ragnarok.fenrir.fragment.accounts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.EnterPinActivity
import dev.ragnarok.fenrir.activity.FileManagerSelectActivity
import dev.ragnarok.fenrir.activity.LoginActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.ProxyManagerActivity
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.Auth.scope
import dev.ragnarok.fenrir.api.adapters.AbsAdapter.Companion.asJsonArray
import dev.ragnarok.fenrir.api.adapters.AbsAdapter.Companion.asJsonObject
import dev.ragnarok.fenrir.api.adapters.AbsAdapter.Companion.asJsonObjectSafe
import dev.ragnarok.fenrir.api.adapters.AbsAdapter.Companion.asPrimitiveSafe
import dev.ragnarok.fenrir.api.adapters.AbsAdapter.Companion.has
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.db.DBHelper
import dev.ragnarok.fenrir.dialog.directauth.DirectAuthDialog
import dev.ragnarok.fenrir.dialog.directauth.DirectAuthDialog.Companion.newInstance
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.exception.UnauthorizedException
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.longpoll.LongpollInstance.longpollManager
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.place.PlaceFactory.getPreferencesPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.backup.SettingsBackup
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.MessagesReplyItemCallback
import dev.ragnarok.fenrir.util.ShortcutUtils.createAccountShortcutRx
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getAppVersionName
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.serializeble.json.*
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.exceptions.Exceptions
import okhttp3.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class AccountsFragment : BaseFragment(), View.OnClickListener, AccountAdapter.Callback,
    MenuProvider {
    private val mCompositeDisposable = CompositeDisposable()
    private val requestPin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            startExportAccounts()
        }
    }
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        if (Settings.get().security().isUsePinForSecurity) {
            requestPin.launch(Intent(requireActivity(), EnterPinActivity::class.java))
        } else {
            startExportAccounts()
        }
    }
    private val requestReadPermission = requestPermissionsAbs(
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ) {
        startImportAccounts()
    }
    private var empty: TextView? = null
    private var mRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: AccountAdapter? = null
    private var temp_to_show = 0
    private val requestEnterPin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val restore: SaveAccount =
                kJson.decodeFromString(
                    SaveAccount.serializer(),
                    Settings.get().accounts().getLogin(temp_to_show)
                        ?: return@registerForActivityResult
                )
            val password = requireActivity().getString(
                R.string.restore_login_info,
                restore.login,
                restore.password,
                Settings.get().accounts().getAccessToken(temp_to_show),
                restore.two_factor_auth
            )
            MaterialAlertDialogBuilder(requireActivity())
                .setMessage(password)
                .setTitle(R.string.login_password_hint)
                .setPositiveButton(R.string.button_ok, null)
                .setNeutralButton(R.string.full_data) { _: DialogInterface?, _: Int ->
                    val clipboard = requireActivity().getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager?
                    val clip = ClipData.newPlainText("response", password)
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
                }
                .setNegativeButton(R.string.copy_data) { _: DialogInterface?, _: Int ->
                    val clipboard = requireActivity().getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager?
                    val clip =
                        ClipData.newPlainText("response", restore.login + " " + restore.password)
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
                }
                .setCancelable(true)
                .show()
        }
    }
    private var mData: ArrayList<Account>? = null
    private var mOwnersInteractor: IOwnersRepository = owners
    private val requestLoginWeb = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val uid = result.data?.extras?.getInt(Extra.USER_ID)
            val token = result.data?.getStringExtra(Extra.TOKEN)
            val Login = result.data?.getStringExtra(Extra.LOGIN)
            val Password = result.data?.getStringExtra(Extra.PASSWORD)
            val TwoFA = result.data?.getStringExtra(Extra.TWO_FA)
            val isSave = result.data?.getBooleanExtra(Extra.SAVE, false)
            if (uid != null) {
                if (isSave != null) {
                    processNewAccount(
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

    private val importAccounts = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                val file = File(
                    result.data?.getStringExtra(Extra.PATH) ?: return@registerForActivityResult
                )
                if (file.exists()) {
                    val obj = kJson.parseToJsonElement(FileInputStream(file)).jsonObject
                    if (obj["app"]?.asJsonObjectSafe?.get("settings_format")?.asPrimitiveSafe?.intOrNull != Constants.EXPORT_SETTINGS_FORMAT) {
                        createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                            .showToastError(R.string.wrong_settings_format)
                        return@registerForActivityResult
                    } else if (obj["app"]?.asJsonObjectSafe?.get("api_type")?.asPrimitiveSafe?.intOrNull != DEFAULT_ACCOUNT_TYPE) {
                        createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                            .showToastWarningBottom(R.string.settings_for_another_client)
                    }
                    val reader = obj["fenrir_accounts"]
                    for (i in reader?.asJsonArray.orEmpty()) {
                        val elem = i.asJsonObject
                        val id = elem["user_id"]?.jsonPrimitive?.intOrNull ?: continue
                        if (Settings.get().accounts().registered.contains(id)) continue
                        val token = elem["access_token"]?.jsonPrimitive?.contentOrNull ?: continue
                        val Type = elem["type"]?.jsonPrimitive?.intOrNull ?: continue
                        processNewAccount(
                            id, token, Type, null, null, "fenrir_app",
                            isCurrent = false,
                            needSave = false
                        )
                        if (elem.has("login")) {
                            Settings.get().accounts().storeLogin(
                                id,
                                elem["login"]?.jsonPrimitive?.contentOrNull ?: continue
                            )
                        }
                        if (elem.has("device")) {
                            Settings.get().accounts().storeDevice(
                                id,
                                elem["device"]?.jsonPrimitive?.contentOrNull ?: continue
                            )
                        }
                    }
                    if (obj.has("settings")) {
                        SettingsBackup().doRestore(obj["settings"]?.asJsonObject)
                        createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
                            .showToastSuccessBottom(
                                R.string.need_restart
                            )
                    }
                }
                createCustomToast(requireActivity()).showToast(
                    R.string.accounts_restored,
                    file.absolutePath
                )
            } catch (e: Exception) {
                e.printStackTrace()
                createCustomToast(requireActivity()).showToastError(e.localizedMessage)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun saveAccounts(file: File, Users: IOwnersBundle?) {
        var out: FileOutputStream? = null
        try {
            val root = JsonObjectBuilder()
            val arr = JsonArrayBuilder()
            for (i in Settings.get().accounts().registered) {
                val temp = JsonObjectBuilder()
                val owner = Users?.getById(i)
                temp.put("user_name", owner?.fullName)
                temp.put("user_id", i)
                temp.put("type", Settings.get().accounts().getType(i))
                temp.put("domain", owner?.domain)
                temp.put("access_token", Settings.get().accounts().getAccessToken(i))
                temp.put("avatar", owner?.maxSquareAvatar)
                val login = Settings.get().accounts().getLogin(i)
                val device = Settings.get().accounts().getDevice(i)
                if (!login.isNullOrEmpty()) {
                    temp.put("login", login)
                }
                if (!device.isNullOrEmpty()) {
                    temp.put("device", device)
                }
                arr.add(temp.build())
            }
            val app = JsonObjectBuilder()
            app.put("version", getAppVersionName(requireActivity()))
            app.put("api_type", DEFAULT_ACCOUNT_TYPE)
            app.put("settings_format", Constants.EXPORT_SETTINGS_FORMAT)
            root.put("app", app.build())
            root.put("fenrir_accounts", arr.build())
            val settings = SettingsBackup().doBackup()
            root.put("settings", settings)
            val bytes = Json { prettyPrint = true }.printJsonElement(root.build()).toByteArray(
                Charsets.UTF_8
            )
            out = FileOutputStream(file)
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            out.write(bom)
            out.write(bytes)
            out.flush()
            provideApplicationContext().sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)
                )
            )
            createCustomToast(requireActivity()).showToast(
                R.string.saved_to_param_file_name,
                file.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            createCustomToast(requireActivity()).showToastError(e.localizedMessage)
        } finally {
            safelyClose(out)
        }
    }

    private val exportAccounts = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val file = File(
                result.data?.getStringExtra(Extra.PATH),
                "fenrir_accounts_backup.json"
            )
            appendDisposable(
                mOwnersInteractor.findBaseOwnersDataAsBundle(
                    Settings.get().accounts().current,
                    Settings.get().accounts().registered,
                    IOwnersRepository.MODE_ANY
                )
                    .fromIOToMain()
                    .subscribe({
                        saveAccounts(
                            file,
                            it
                        )
                    }) { saveAccounts(file, null) })
        }
    }

    private val accountsInteractor: IAccountsInteractor =
        InteractorFactory.createAccountInteractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mData = savedInstanceState.getParcelableArrayListCompat(SAVE_DATA)
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
        mSwipeRefreshLayout?.setOnRefreshListener { load(true) }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        ItemTouchHelper(MessagesReplyItemCallback { o: Int ->
            if (mAdapter?.checkPosition(o) == true) {
                val account = mAdapter?.getByPosition(o) ?: return@MessagesReplyItemCallback
                val idCurrent = account.getObjectId() == Settings.get()
                    .accounts()
                    .current
                if (!idCurrent) {
                    setAsActive(account)
                }
            }
        }).attachToRecyclerView(mRecyclerView)
        root.findViewById<View>(R.id.fab).setOnClickListener(this)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        var firstRun = false
        if (mData == null) {
            mData = ArrayList()
            firstRun = true
        }
        mAdapter = AccountAdapter(requireActivity(), mData ?: Collections.emptyList(), this)
        mRecyclerView?.adapter = mAdapter
        if (firstRun) {
            load(false)
        }
        resolveEmptyText()

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
            val uid = result.getInt(Extra.USER_ID)
            val token = result.getString(Extra.TOKEN)
            val Login = result.getString(Extra.LOGIN)
            val Password = result.getString(Extra.PASSWORD)
            val TwoFA = result.getString(Extra.TWO_FA)
            val isSave = result.getBoolean(Extra.SAVE)
            processNewAccount(
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

    private fun resolveEmptyText() {
        if (!isAdded || empty == null) return
        empty?.visibility =
            if (mData.isNullOrEmpty()) View.VISIBLE else View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.title = null
            actionBar.subtitle = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SAVE_DATA, mData)
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    private fun load(refresh: Boolean) {
        if (!refresh) {
            mSwipeRefreshLayout?.isRefreshing = true
        }
        mCompositeDisposable.add(accountsInteractor
            .getAll(refresh)
            .fromIOToMain()
            .subscribe({
                mSwipeRefreshLayout?.isRefreshing = false
                val sz = mData?.size ?: 0
                mData?.clear()
                mAdapter?.notifyItemRangeRemoved(0, sz)
                mData?.addAll(it)
                mAdapter?.notifyItemRangeInserted(0, it.size)
                resolveEmptyText()
                if (isAdded && mData.isNullOrEmpty()) {
                    requireActivity().invalidateOptionsMenu()
                    startDirectLogin()
                }
            }) { mSwipeRefreshLayout?.isRefreshing = false })
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

    private fun indexOf(uid: Int): Int {
        mData.nonNullNoEmpty {
            for (i in it.indices) {
                if (it[i].getObjectId() == uid) {
                    return i
                }
            }
        }
        return -1
    }

    private fun merge(account: Account) {
        val index = indexOf(account.getObjectId())
        mData?.let {
            if (index != -1) {
                it[index] = account
            } else {
                it.add(account)
            }
            mAdapter?.notifyDataSetChanged()
        }
        resolveEmptyText()
    }

    private fun processNewAccount(
        uid: Int,
        token: String?,
        @AccountType type: Int,
        Login: String?,
        Password: String?,
        TwoFA: String?,
        isCurrent: Boolean,
        needSave: Boolean
    ) {
        //Accounts account = new Accounts(token, uid);

        // важно!! Если мы получили новый токен, то необходимо удалить запись
        // о регистрации push-уведомлений
        //PushSettings.unregisterFor(getContext(), account);
        Settings.get()
            .accounts()
            .storeAccessToken(uid, token)
        Settings.get()
            .accounts().storeTokenType(uid, type)
        Settings.get()
            .accounts()
            .registerAccountId(uid, isCurrent)
        if (needSave) {
            val json = kJson.encodeToString(
                SaveAccount.serializer(),
                SaveAccount().set(Login, Password, TwoFA)
            )
            Settings.get()
                .accounts()
                .storeLogin(uid, json)
        }
        merge(Account(uid, null))
        mCompositeDisposable.add(
            mOwnersInteractor.getBaseOwnerInfo(uid, uid, IOwnersRepository.MODE_ANY)
                .fromIOToMain()
                .subscribe({ merge(Account(uid, it)) }) { })
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

    private fun startDirectLogin() {
        val auth = newInstance()
        auth.show(parentFragmentManager, "direct-login")
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            startDirectLogin()
        }
    }

    @SuppressLint("CheckResult")
    internal fun delete(account: Account) {
        Settings.get()
            .accounts()
            .removeAccessToken(account.getObjectId())
        Settings.get()
            .accounts()
            .removeType(account.getObjectId())
        Settings.get()
            .accounts()
            .removeLogin(account.getObjectId())
        Settings.get()
            .accounts()
            .removeDevice(account.getObjectId())
        Settings.get()
            .accounts()
            .remove(account.getObjectId())
        DBHelper.removeDatabaseFor(requireActivity(), account.getObjectId())
        longpollManager.forceDestroy(account.getObjectId())
        mData?.remove(account)
        mAdapter?.notifyDataSetChanged()
        resolveEmptyText()
        Includes.stores.stickers().clearAccount(account.getObjectId()).fromIOToMain()
            .subscribe(RxUtils.dummy(), RxUtils.ignore())
    }

    internal fun setAsActive(account: Account) {
        Settings.get()
            .accounts().current = account.getObjectId()
        mAdapter?.notifyDataSetChanged()
    }

    override fun onClick(account: Account) {
        val idCurrent = account.getObjectId() == Settings.get()
            .accounts()
            .current
        val menus = ModalBottomSheetDialogFragment.Builder()
        if (account.getObjectId() > 0) {
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
            if (!Settings.get().accounts().getLogin(account.getObjectId()).isNullOrEmpty()) {
                menus.add(
                    OptionRequest(
                        3,
                        getString(R.string.login_password_hint),
                        R.drawable.view,
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
        if (isHiddenAccount(account.getObjectId())) {
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
                        0 -> delete(account)
                        1 -> createShortcut(account)
                        2 -> setAsActive(account)
                        3 -> if (!Settings.get().security().isUsePinForSecurity) {
                            createCustomToast(requireActivity()).showToastError(R.string.not_supported_hide)
                        } else {
                            temp_to_show = account.getObjectId()
                            requestEnterPin.launch(
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
                                Settings.get().accounts().getDevice(account.getObjectId())
                            )
                            MaterialAlertDialogBuilder(requireActivity())
                                .setTitle(R.string.set_device)
                                .setCancelable(true)
                                .setView(root)
                                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                                    Settings.get().accounts().storeDevice(
                                        account.getObjectId(),
                                        (root.findViewById<View>(R.id.editText) as TextInputEditText).editableText.toString()
                                    )
                                }
                                .setNegativeButton(R.string.button_cancel, null)
                                .show()
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

    private fun getUserIdByAccessToken(@AccountType type: Int, accessToken: String): Single<Int> {
        val bodyBuilder = FormBody.Builder()
        bodyBuilder.add("access_token", accessToken)
            .add("v", Constants.API_VERSION)
            .add("lang", Constants.DEVICE_COUNTRY_CODE)
            .add("https", "1")
            .add(
                "device_id", Utils.getDeviceId(provideApplicationContext())
            )
        return Includes.networkInterfaces.getVkRestProvider().provideRawHttpClient(type)
            .flatMap { client ->
                Single.create { emitter: SingleEmitter<Response> ->
                    val request: Request = Request.Builder()
                        .url(
                            "https://" + Settings.get().other()
                                .get_Api_Domain() + "/method/users.get"
                        )
                        .post(bodyBuilder.build())
                        .build()
                    val call = client.build().newCall(request)
                    emitter.setCancellable { call.cancel() }
                    try {
                        val response = call.execute()
                        if (!response.isSuccessful) {
                            emitter.tryOnError(HttpException(response.code))
                        } else {
                            emitter.onSuccess(response)
                        }
                        response.close()
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    }
                }
            }
            .map<BaseResponse<List<VKApiUser>>> {
                if (it.body.isMsgPack()
                ) MsgPack.decodeFromOkioStream(it.body.source()) else kJson.decodeFromStream(it.body.byteStream())
            }.map { it1 ->
                it1.error.requireNonNull {
                    throw Exceptions.propagate(ApiException(it))
                }
                val o = it1.response.nonNullNoEmptyOr({
                    if (it.isEmpty()) -1 else it[0].id
                }, { -1 })
                if (o < 0) {
                    throw UnauthorizedException("Token error")
                }
                o
            }
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
                                appendDisposable(
                                    getUserIdByAccessToken(
                                        types[selected],
                                        access_token
                                    )
                                        .fromIOToMain()
                                        .subscribe({
                                            processNewAccount(
                                                it,
                                                access_token,
                                                types[selected],
                                                null,
                                                null,
                                                "fenrir_app",
                                                isCurrent = false,
                                                needSave = false
                                            )
                                        }, { it2 ->
                                            it2.localizedMessage?.let {
                                                CustomSnackbars.createCustomSnackbars(
                                                    view,
                                                    mRecyclerView
                                                )
                                                    ?.setDurationSnack(Snackbar.LENGTH_LONG)
                                                    ?.coloredSnack(
                                                        it,
                                                        Color.parseColor("#eeff0000")
                                                    )?.show()
                                            }
                                        })
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
                    requestWritePermission.launch()
                    return true
                }
                if (Settings.get().security().isUsePinForSecurity) {
                    requestPin.launch(Intent(requireActivity(), EnterPinActivity::class.java))
                } else startExportAccounts()
                return true
            }
            R.id.import_accounts -> {
                if (!hasReadStoragePermission(requireActivity())) {
                    requestReadPermission.launch()
                    return true
                }
                startImportAccounts()
                return true
            }
            else -> return false
        }
    }

    private fun startProxySettings() {
        startActivity(Intent(requireActivity(), ProxyManagerActivity::class.java))
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_accounts, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.export_accounts).isVisible = ((mData?.size ?: 0) > 0)
    }

    internal fun createShortcut(account: Account) {
        if (account.getObjectId() < 0) {
            return  // this is community
        }
        val user = account.owner as User
        appendDisposable(
            createAccountShortcutRx(
                requireActivity(),
                account.getObjectId(),
                account.displayName,
                user.maxSquareAvatar ?: VKApiUser.CAMERA_50
            ).fromIOToMain().subscribe(
                {
                    CustomSnackbars.createCustomSnackbars(view, mRecyclerView)
                        ?.setDurationSnack(Snackbar.LENGTH_LONG)
                        ?.coloredSnack(R.string.success, Color.parseColor("#AA48BE2D"))?.show()
                }
            ) { t ->
                t.localizedMessage?.let {
                    CustomSnackbars.createCustomSnackbars(
                        view,
                        mRecyclerView
                    )
                        ?.setDurationSnack(Snackbar.LENGTH_LONG)
                        ?.coloredSnack(
                            it,
                            Color.parseColor("#eeff0000")
                        )?.show()
                }
            })
    }

    companion object {
        private const val SAVE_DATA = "save_data"
    }
}