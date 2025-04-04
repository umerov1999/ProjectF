package dev.ragnarok.fenrir.fragment.friends.friendsbyphones

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.kJsonPretty
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.decodeFromBufferedSource
import okio.buffer
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class FriendsByPhonesPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFriendsByPhonesView>(accountId, savedInstanceState) {
    private val data: MutableList<ContactConversation> = ArrayList()
    private val dataSearch: MutableList<ContactConversation> = ArrayList()
    private val accountsInteractor: IAccountsInteractor =
        InteractorFactory.createAccountInteractor()
    private var netLoadingNow = false
    private var query: String? = null
    private fun resolveRefreshingView() {
        view?.displayLoading(
            netLoadingNow
        )
    }

    private fun requestActualData() {
        netLoadingNow = true
        resolveRefreshingView()
        appendJob(
            accountsInteractor.getContactList(accountId, 0, 1000)
                .fromIOToMain({ owners -> onActualDataReceived(owners) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
    }

    fun fireImportContacts(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val contacts: List<ContactConversation> = kJson.decodeFromBufferedSource(
                    ListSerializer(ContactConversation.serializer()),
                    file.source().buffer()
                )
                data.clear()
                data.addAll(contacts)
                view?.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            view?.customToast?.showToastError(e.localizedMessage)
        }
    }

    fun fireExport(file: File) {
        var out: FileOutputStream? = null
        try {

            val bytes =
                kJsonPretty.encodeToString(ListSerializer(ContactConversation.serializer()), data)
                    .toByteArray(
                        Charsets.UTF_8
                    )
            out = FileOutputStream(file)
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            out.write(bom)
            out.write(bytes)
            out.flush()
            Includes.provideApplicationContext().sendBroadcast(
                @Suppress("deprecation")
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
            view?.customToast?.showToastError(e.localizedMessage)
        } finally {
            Utils.safelyClose(out)
        }
    }

    private fun updateCriteria() {
        dataSearch.clear()
        if (query.isNullOrEmpty()) {
            view?.displayData(data)
            view?.notifyDataSetChanged()
            return
        }
        for (i in data) {
            if (query?.lowercase(Locale.getDefault())?.let {
                    i.title?.lowercase(Locale.getDefault())?.contains(it)
                } == true
            ) {
                dataSearch.add(i)
            }
        }
        view?.displayData(dataSearch)
        view?.notifyDataSetChanged()
    }

    fun fireQuery(q: String?) {
        query = if (q.isNullOrEmpty()) null else {
            q
        }
        updateCriteria()
    }

    fun fireRefresh(context: Context) {
        if (query.trimmedNonNullNoEmpty()) {
            return
        }
        netLoadingNow = true
        resolveRefreshingView()
        appendJob(
            accountsInteractor.importMessagesContacts(accountId, context, 0, 1000)
                .fromIOToMain({ owners -> onActualDataReceived(owners) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
    }

    fun fireReset() {
        if (query.trimmedNonNullNoEmpty()) {
            return
        }
        netLoadingNow = true
        resolveRefreshingView()
        appendJob(
            accountsInteractor.resetMessagesContacts(accountId, 0, 1000)
                .fromIOToMain({ owners -> onActualDataReceived(owners) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
    }

    private fun onActualDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onActualDataReceived(owners: List<ContactConversation>) {
        netLoadingNow = false
        resolveRefreshingView()
        data.clear()
        data.addAll(owners)
        view?.notifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IFriendsByPhonesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
        resolveRefreshingView()
    }

    fun fireRefresh() {
        if (query.trimmedNonNullNoEmpty()) {
            netLoadingNow = false
            resolveRefreshingView()
            return
        }
        requestActualData()
    }

    fun onUserOwnerClicked(owner: ContactConversation) {
        view?.showChat(
            accountId,
            owner
        )
    }

    init {
        requestActualData()
    }
}