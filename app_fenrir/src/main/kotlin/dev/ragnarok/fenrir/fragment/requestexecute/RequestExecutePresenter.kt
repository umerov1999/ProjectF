package dev.ragnarok.fenrir.fragment.requestexecute

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.isMsgPack
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeLegalFilename
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.serializeble.json.Json
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.encodeToString
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class RequestExecutePresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IRequestExecuteView>(accountId, savedInstanceState) {
    private val networker: INetworker = get()
    private var body: String? = null
    private var method: String? = null
    private var fullResponseBody: String? = null
    private var trimmedResponseBody: String? = null
    private var loadingNow = false
    private fun executeRequest() {
        val trimmedMethod = if (method.nonNullNoEmpty()) method?.trim { it <= ' ' } else null
        val trimmedBody = if (body.nonNullNoEmpty()) body?.trim { it <= ' ' } else null
        if (trimmedMethod.isNullOrEmpty()) {
            showError(Exception("Method can't be empty"))
            return
        }
        val params: MutableMap<String, String> = HashMap()
        if (trimmedBody.nonNullNoEmpty()) {
            try {
                val lines = trimmedBody.split(Regex("\\r?\\n")).toTypedArray()
                for (line in lines) {
                    val parts = line.split(Regex("=>")).toTypedArray()
                    val name = parts[0].lowercase(Locale.getDefault()).trim { it <= ' ' }
                    var value = parts[1].trim { it <= ' ' }
                    value = value.replace("\"".toRegex(), "")
                    if ((name == "user_id" || name == "peer_id" || name == "owner_id") && (value.equals(
                            "my",
                            ignoreCase = true
                        ) || value.equals("я", ignoreCase = true))
                    ) value = accountId.toString()
                    params[name] = value
                }
            } catch (e: Exception) {
                showError(e)
                return
            }
        }
        setLoadingNow(true)
        appendDisposable(executeSingle(accountId, trimmedMethod, params)
            .fromIOToMain()
            .subscribe({ onRequestResponse(it) }) { throwable ->
                onRequestError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    private fun hasWritePermission(): Boolean {
        return hasReadWriteStoragePermission(applicationContext)
    }

    @Suppress("DEPRECATION")
    private fun saveToFile() {
        if (!hasWritePermission()) {
            view?.requestWriteExternalStoragePermission()
            return
        }
        val rMethod = method ?: return
        var out: FileOutputStream? = null
        try {
            val filename = makeLegalFilename(rMethod, "json")
            val file = File(Environment.getExternalStorageDirectory(), filename)
            file.delete()
            val bytes = fullResponseBody?.toByteArray(Charsets.UTF_8) ?: return
            out = FileOutputStream(file)
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            out.write(bom)
            out.write(bytes)
            out.flush()
            applicationContext.sendBroadcast(
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
            showError(e)
        } finally {
            safelyClose(out)
        }
    }

    override fun onGuiCreated(viewHost: IRequestExecuteView) {
        super.onGuiCreated(viewHost)
        viewHost.displayBody(trimmedResponseBody)
        resolveProgressDialog()
    }

    private fun onRequestResponse(body: Pair<String?, String?>) {
        setLoadingNow(false)
        fullResponseBody = body.first
        trimmedResponseBody = body.second
        view?.displayBody(
            trimmedResponseBody
        )
    }

    private fun onRequestError(throwable: Throwable) {
        setLoadingNow(false)
        showError(throwable)
    }

    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveProgressDialog()
    }

    private fun resolveProgressDialog() {
        if (loadingNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.waiting_for_response_message,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    private fun executeSingle(
        accountId: Long,
        method: String,
        params: Map<String, String>
    ): Single<Pair<String?, String?>> {
        return networker.vkDefault(accountId)
            .other()
            .rawRequest(method, params)
            .map { optional ->
                val responseString = optional.get()
                val fullJson = if (responseString == null) null else toPrettyFormat(responseString)
                var trimmedJson: String? = null
                if (fullJson.nonNullNoEmpty()) {
                    val lines = fullJson.split(Regex("\\r?\\n")).toTypedArray()
                    val trimmed: MutableList<String> = ArrayList()
                    for (line in lines) {
                        if (trimmed.size > 1500) {
                            trimmed.add("\n")
                            trimmed.add("... and more " + (lines.size - 1500) + " lines")
                            break
                        }
                        trimmed.add(line)
                    }
                    trimmedJson = join("\n", trimmed)
                }
                create(fullJson, trimmedJson)
            }
    }

    fun fireSaveClick() {
        saveToFile()
    }

    fun fireWritePermissionResolved() {
        if (hasWritePermission()) {
            saveToFile()
        }
    }

    fun fireExecuteClick() {
        view?.hideKeyboard()
        executeRequest()
    }

    fun fireMethodEdit(s: CharSequence?) {
        method = s.toString()
    }

    fun fireBodyEdit(s: CharSequence?) {
        body = s.toString()
    }

    fun fireCopyClick() {
        val clipboard =
            applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("response", fullResponseBody)
        clipboard?.setPrimaryClip(clip)
        view?.customToast?.showToast(
            R.string.copied_to_clipboard
        )
    }

    companion object {
        /**
         * Convert a JSON string to pretty print version
         */
        internal fun toPrettyFormat(responseBody: ResponseBody): String {
            val json = Json {
                ignoreUnknownKeys = true; isLenient = true; prettyPrint = true
            }
            return if (responseBody.isMsgPack()) {
                json.encodeToString(MsgPack.parseToJsonElement(responseBody.source()))
            } else {
                json.encodeToString(json.parseToJsonElement(responseBody.source()))
            }
        }
    }
}
