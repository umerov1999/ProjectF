package dev.ragnarok.fenrir.fragment.accounts

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.Auth
import dev.ragnarok.fenrir.api.adapters.AbsDtoAdapter.Companion.asJsonObjectSafe
import dev.ragnarok.fenrir.api.adapters.AbsDtoAdapter.Companion.asPrimitiveSafe
import dev.ragnarok.fenrir.api.adapters.AbsDtoAdapter.Companion.hasArray
import dev.ragnarok.fenrir.api.adapters.AbsDtoAdapter.Companion.hasObject
import dev.ragnarok.fenrir.api.adapters.AbsDtoAdapter.Companion.hasPrimitive
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.db.DBHelper
import dev.ragnarok.fenrir.db.model.entity.DialogDboEntity
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.exception.UnauthorizedException
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.isMsgPack
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.longpoll.LongpollInstance
import dev.ragnarok.fenrir.model.Account
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.SaveAccount
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.criteria.DialogsCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.nonNullNoEmptyOr
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.backup.SettingsBackup
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.ShortcutUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.serializeble.json.Json
import dev.ragnarok.fenrir.util.serializeble.json.JsonArrayBuilder
import dev.ragnarok.fenrir.util.serializeble.json.JsonObjectBuilder
import dev.ragnarok.fenrir.util.serializeble.json.contentOrNull
import dev.ragnarok.fenrir.util.serializeble.json.decodeFromStream
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive
import dev.ragnarok.fenrir.util.serializeble.json.long
import dev.ragnarok.fenrir.util.serializeble.json.longOrNull
import dev.ragnarok.fenrir.util.serializeble.json.put
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import dev.ragnarok.fenrir.util.toast.CustomToast
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.exceptions.Exceptions
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AccountsPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<IAccountsView>(savedInstanceState) {

    private var mData: ArrayList<Account> = ArrayList()
    private val mOwnersInteractor: IOwnersRepository = Repository.owners
    private val networker: INetworker = Includes.networkInterfaces
    private val accountsInteractor: IAccountsInteractor =
        InteractorFactory.createAccountInteractor()
    private var tempAccountId = 0L

    override fun onGuiCreated(viewHost: IAccountsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mData)
        view?.resolveEmptyText(mData.isEmpty())
    }

    fun isNotEmptyAccounts(): Boolean {
        return mData.isNotEmpty()
    }

    fun fireResetTempAccount() {
        tempAccountId = 0
    }

    fun fireResetAndGetTempAccount(): Long {
        val tmp = tempAccountId
        tempAccountId = 0
        return tmp
    }

    fun fireSetTempAccount(accountId: Long) {
        tempAccountId = accountId
    }

    fun fireLoad(refresh: Boolean) {
        if (!refresh) {
            view?.isLoading(true)
        }
        appendDisposable(accountsInteractor
            .getAll(refresh)
            .fromIOToMain()
            .subscribe({
                view?.isLoading(false)
                val sz = mData.size
                mData.clear()
                view?.notifyItemRangeRemoved(0, sz)
                mData.addAll(it)
                view?.notifyItemRangeInserted(0, it.size)
                view?.resolveEmptyText(mData.isEmpty())
                if (mData.isEmpty()) {
                    view?.invalidateMenu()
                    view?.startDirectLogin()
                }
            }) { view?.isLoading(false) })
    }

    private fun indexOf(uid: Long): Int {
        mData.nonNullNoEmpty {
            for (i in it.indices) {
                if (it[i].getOwnerObjectId() == uid) {
                    return i
                }
            }
        }
        return -1
    }

    private fun merge(account: Account) {
        val index = indexOf(account.getOwnerObjectId())
        mData.let {
            if (index != -1) {
                it[index] = account
            } else {
                it.add(account)
            }
            view?.notifyDataSetChanged()
        }
        view?.resolveEmptyText(mData.isEmpty())
    }

    fun fireDelete(context: Context, account: Account) {
        Settings.get()
            .accounts()
            .removeAccessToken(account.getOwnerObjectId())
        Settings.get()
            .accounts()
            .removeType(account.getOwnerObjectId())
        Settings.get()
            .accounts()
            .removeLogin(account.getOwnerObjectId())
        Settings.get()
            .accounts()
            .removeDevice(account.getOwnerObjectId())
        val s = Settings.get()
            .accounts()
            .remove(account.getOwnerObjectId())
        DBHelper.removeDatabaseFor(context, account.getOwnerObjectId())
        LongpollInstance.longpollManager.forceDestroy(account.getOwnerObjectId())
        val indx = indexOf(account.getOwnerObjectId())
        mData.removeAt(indx)
        view?.notifyItemRemoved(indx)
        s?.let {
            for (o in mData.indices) {
                if (mData[o].getOwnerObjectId() == s) {
                    view?.notifyItemChanged(o)
                    break
                }
            }
        }
        view?.resolveEmptyText(mData.isEmpty())
        appendDisposable(
            Includes.stores.stickers().clearAccount(account.getOwnerObjectId()).fromIOToMain()
                .subscribe(RxUtils.dummy(), RxUtils.ignore())
        )
    }

    fun processNewAccount(
        uid: Long,
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
        appendDisposable(
            mOwnersInteractor.getBaseOwnerInfo(uid, uid, IOwnersRepository.MODE_ANY)
                .fromIOToMain()
                .subscribe({ merge(Account(uid, it)) }) { })
    }

    fun fireSetAsActive(account: Account) {
        Settings.get()
            .accounts().current = account.getOwnerObjectId()
        view?.notifyDataSetChanged()
    }

    fun processAccountByAccessToken(token: String, @AccountType type: Int) {
        appendDisposable(
            getUserIdByAccessToken(
                type,
                token
            )
                .fromIOToMain()
                .subscribe({
                    processNewAccount(
                        it,
                        token,
                        type,
                        null,
                        null,
                        "fenrir_app",
                        isCurrent = false,
                        needSave = false
                    )
                }, { it2 ->
                    it2.localizedMessage?.let {
                        view?.showColoredSnack(it, Color.parseColor("#eeff0000"))
                    }
                })
        )
    }

    fun createShortcut(context: Context, account: Account) {
        if (account.getOwnerObjectId() < 0) {
            return  // this is community
        }
        val user = account.owner as User
        appendDisposable(
            ShortcutUtils.createAccountShortcutRx(
                context,
                account.getOwnerObjectId(),
                account.displayName,
                user.maxSquareAvatar ?: VKApiUser.CAMERA_50
            ).fromIOToMain().subscribe(
                {
                    view?.showColoredSnack(R.string.success, Color.parseColor("#AA48BE2D"))
                }
            ) { t ->
                t.localizedMessage?.let {
                    view?.showColoredSnack(it, Color.parseColor("#eeff0000"))
                }
            })
    }

    fun createExchangeToken(context: Context) {
        if (tempAccountId == 0L) {
            return
        }
        val accountFromTmp = tempAccountId
        tempAccountId = 0L
        appendDisposable(
            accountsInteractor.getExchangeToken(accountFromTmp).fromIOToMain().subscribe({
                if (it.token.nonNullNoEmpty()) {
                    DownloadWorkUtils.CheckDirectory(Settings.get().other().docDir)
                    val file = File(
                        Settings.get().other().docDir, "${accountFromTmp}_exchange_token.json"
                    )
                    appendDisposable(
                        mOwnersInteractor.findBaseOwnersDataAsBundle(
                            Settings.get().accounts().current,
                            Settings.get().accounts().registered,
                            IOwnersRepository.MODE_ANY
                        )
                            .fromIOToMain()
                            .subscribe({ own ->
                                saveExchangeToken(
                                    context,
                                    accountFromTmp,
                                    file,
                                    it.token.orEmpty(),
                                    own
                                )
                            }) { _ ->
                                saveExchangeToken(
                                    context,
                                    accountFromTmp,
                                    file,
                                    it.token.orEmpty(),
                                    null
                                )
                            })
                }
            }, {
                view?.customToast?.showToastError(
                    ErrorLocalizer.localizeThrowable(context, it)
                )
            }
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun saveExchangeToken(
        context: Context,
        user_id: Long,
        file: File,
        token: String,
        Users: IOwnersBundle?
    ) {
        var out: FileOutputStream? = null
        try {
            val root = JsonObjectBuilder()
            val owner = Users?.getById(user_id)
            root.put("api_ver", Constants.API_VERSION)
            root.put("user_name", owner?.fullName)
            root.put("user_id", user_id)
            root.put("type", Settings.get().accounts().getType(user_id))
            root.put("domain", owner?.domain)
            root.put("exchange_token", token)
            root.put("avatar", owner?.maxSquareAvatar)
            root.put("device_id", Utils.getDeviceId(context))
            root.put("sak_version", "1.102")
            root.put("last_access_token", Settings.get().accounts().getAccessToken(user_id))
            val login = Settings.get().accounts().getLogin(user_id)
            val device = Settings.get().accounts().getDevice(user_id)
            if (!login.isNullOrEmpty()) {
                root.put("login", login)
            }
            if (!device.isNullOrEmpty()) {
                root.put("device", device)
            }
            val bytes = Json { prettyPrint = true }.printJsonElement(root.build()).toByteArray(
                Charsets.UTF_8
            )
            out = FileOutputStream(file)
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            out.write(bom)
            out.write(bytes)
            out.flush()
            Includes.provideApplicationContext().sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)
                )
            )
            view?.customToast?.showToast(
                R.string.saved_to_param_file_name,
                file.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            view?.customToast?.showToastError(e.localizedMessage)
        } finally {
            Utils.safelyClose(out)
        }
    }

    fun importExchangeToken(context: Context, path: String) {
        try {
            val file = File(
                path
            )
            if (file.exists()) {
                val elem = kJson.parseToJsonElement(FileInputStream(file)).jsonObject

                val exchangeToken = elem["exchange_token"]?.asPrimitiveSafe?.contentOrNull
                    ?: return
                val type =
                    elem["type"]?.asPrimitiveSafe?.intOrNull ?: return
                val device = elem["device"]?.asPrimitiveSafe?.contentOrNull
                val device_id = elem["device_id"]?.asPrimitiveSafe?.contentOrNull
                val api_ver = elem["api_ver"]?.asPrimitiveSafe?.contentOrNull
                val sak_version = elem["sak_version"]?.asPrimitiveSafe?.contentOrNull

                appendDisposable(
                    networker.vkDirectAuth(type, device).authByExchangeToken(
                        Constants.API_ID,
                        Constants.API_ID,
                        exchangeToken,
                        Auth.scope,
                        "expired_token",
                        device_id,
                        sak_version,
                        null,
                        api_ver
                    ).fromIOToMain().subscribe({
                        val aToken = it.resultUrl?.let { it1 -> tryExtractAccessToken(it1) }
                            ?: return@subscribe
                        val user_id: Long =
                            it.resultUrl?.let { it1 -> tryExtractUserId(it1)?.toLong() }
                                ?: return@subscribe

                        processNewAccount(
                            user_id, aToken, type, null, null, "fenrir_app",
                            isCurrent = false,
                            needSave = false
                        )
                        view?.showColoredSnack(R.string.success, Color.parseColor("#AA48BE2D"))

                        if (hasPrimitive(elem, "login")) {
                            Settings.get().accounts().storeLogin(
                                user_id,
                                elem["login"]?.jsonPrimitive?.contentOrNull ?: return@subscribe
                            )
                        }
                        if (device_id.nonNullNoEmpty()) {
                            Settings.get().accounts().storeDevice(
                                user_id,
                                device
                            )
                        }
                    }, {
                        view?.customToast?.showToastError(
                            ErrorLocalizer.localizeThrowable(context, it)
                        )
                    })
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            view?.customToast?.showToastError(e.localizedMessage)
        }
    }

    fun importAccounts(path: String) {
        try {
            val file = File(
                path
            )
            if (file.exists()) {
                val obj = kJson.parseToJsonElement(FileInputStream(file)).jsonObject
                if (obj["app"]?.asJsonObjectSafe?.get("settings_format")?.asPrimitiveSafe?.intOrNull != Constants.EXPORT_SETTINGS_FORMAT) {
                    view?.customToast?.setDuration(Toast.LENGTH_LONG)
                        ?.showToastError(R.string.wrong_settings_format)
                    return
                } else if (obj["app"]?.asJsonObjectSafe?.get("api_type")?.asPrimitiveSafe?.intOrNull != Constants.DEFAULT_ACCOUNT_TYPE) {
                    view?.customToast?.setDuration(Toast.LENGTH_LONG)
                        ?.showToastWarningBottom(R.string.settings_for_another_client)
                }
                val reader = obj["fenrir_accounts"]
                for (i in reader?.jsonArray.orEmpty()) {
                    val elem = i.jsonObject
                    val id = elem["user_id"]?.asPrimitiveSafe?.longOrNull ?: continue
                    if (Settings.get().accounts().registered.contains(id)) continue
                    val token = elem["access_token"]?.asPrimitiveSafe?.contentOrNull ?: continue
                    val Type = elem["type"]?.asPrimitiveSafe?.intOrNull ?: continue
                    processNewAccount(
                        id, token, Type, null, null, "fenrir_app",
                        isCurrent = false,
                        needSave = false
                    )
                    if (hasPrimitive(elem, "login")) {
                        Settings.get().accounts().storeLogin(
                            id,
                            elem["login"]?.jsonPrimitive?.contentOrNull ?: continue
                        )
                    }
                    if (hasPrimitive(elem, "device")) {
                        Settings.get().accounts().storeDevice(
                            id,
                            elem["device"]?.jsonPrimitive?.contentOrNull ?: continue
                        )
                    }
                }
                if (hasObject(obj, "settings")) {
                    SettingsBackup().doRestore(obj["settings"]?.jsonObject)
                    view?.customToast?.setDuration(Toast.LENGTH_LONG)
                    view?.customToast?.showToastSuccessBottom(
                        R.string.need_restart
                    )
                }
                try {
                    if (hasArray(obj, "conversations_saved")) {
                        for (i in obj["conversations_saved"]?.jsonArray.orEmpty()) {
                            val aid = i.jsonObject["account_id"]?.asPrimitiveSafe?.long ?: continue
                            val dialogsJsonElem =
                                i.jsonObject["conversation"]?.jsonArray ?: continue
                            if (!dialogsJsonElem.isEmpty()) {
                                Includes.stores.dialogs().insertDialogs(
                                    aid, kJson.decodeFromJsonElement(
                                        ListSerializer(
                                            DialogDboEntity.serializer()
                                        ), dialogsJsonElem
                                    ), true
                                ).blockingAwait()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            view?.customToast?.showToast(
                R.string.accounts_restored,
                file.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            view?.customToast?.showToastError(e.localizedMessage)
        }
    }

    fun exportAccounts(context: Context, path: String) {
        val file = File(
            path,
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
                        context,
                        file,
                        it
                    )
                }) { saveAccounts(context, file, null) })
    }

    @Suppress("DEPRECATION")
    private fun saveAccounts(context: Context, file: File, Users: IOwnersBundle?) {
        var out: FileOutputStream? = null
        try {
            val root = JsonObjectBuilder()
            val arr = JsonArrayBuilder()
            val registered = Settings.get().accounts().registered
            for (i in registered) {
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
            app.put("version", Utils.getAppVersionName(context))
            app.put("api_type", Constants.DEFAULT_ACCOUNT_TYPE)
            app.put("settings_format", Constants.EXPORT_SETTINGS_FORMAT)
            root.put("app", app.build())
            root.put("fenrir_accounts", arr.build())
            val settings = SettingsBackup().doBackup()
            root.put("settings", settings)

            val arrDialogs = JsonArrayBuilder()
            for (i in registered) {
                if (Utils.isHiddenAccount(i)) {
                    try {
                        val dialogs =
                            Includes.stores.dialogs().getDialogs(DialogsCriteria(i)).blockingGet()
                        val tmp = JsonObjectBuilder()
                        tmp.put(
                            "account_id", i
                        )
                        tmp.put(
                            "conversation", kJson.encodeToJsonElement(
                                ListSerializer(
                                    DialogDboEntity.serializer()
                                ), dialogs
                            )
                        )
                        arrDialogs.add(
                            tmp.build()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            root.put("conversations_saved", arrDialogs.build())
            val bytes = Json { prettyPrint = true }.printJsonElement(root.build()).toByteArray(
                Charsets.UTF_8
            )
            out = FileOutputStream(file)
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            out.write(bom)
            out.write(bytes)
            out.flush()
            Includes.provideApplicationContext().sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)
                )
            )
            CustomToast.createCustomToast(context).showToast(
                R.string.saved_to_param_file_name,
                file.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            view?.customToast?.showToastError(e.localizedMessage)
        } finally {
            Utils.safelyClose(out)
        }
    }

    companion object {
        private fun tryExtractAccessToken(url: String): String? {
            return VKStringUtils.extractPattern(url, "access_token=(.*?)&")
        }

        private fun tryExtractUserId(url: String): String? {
            return VKStringUtils.extractPattern(url, "user_id=(\\d*)")
        }

        private fun getUserIdByAccessToken(
            @AccountType type: Int,
            accessToken: String
        ): Single<Long> {
            val bodyBuilder = FormBody.Builder()
            bodyBuilder.add("access_token", accessToken)
                .add("v", Constants.API_VERSION)
                .add("lang", Constants.DEVICE_COUNTRY_CODE)
                .add("https", "1")
                .add(
                    "device_id", Utils.getDeviceId(Includes.provideApplicationContext())
                )
            return Includes.networkInterfaces.getVkRestProvider().provideRawHttpClient(type, null)
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
    }

    init {
        fireLoad(false)
    }
}
