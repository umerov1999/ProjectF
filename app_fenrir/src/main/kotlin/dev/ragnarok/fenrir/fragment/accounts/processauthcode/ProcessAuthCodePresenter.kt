package dev.ragnarok.fenrir.fragment.accounts.processauthcode

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.Text
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.model.menu.Section
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class ProcessAuthCodePresenter(
    accountId: Long,
    private val authCode: String,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<IProcessAuthCodeView>(accountId, savedInstanceState) {
    private val pInteractor: IAccountsInteractor = InteractorFactory.createAccountInteractor()
    private val items: MutableList<AdvancedItem> = ArrayList()
    private var actionState = 0

    override fun onGuiCreated(viewHost: IProcessAuthCodeView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(items)
    }

    private fun requestData(action: Int) {
        appendJob(pInteractor.processAuthCode(accountId, authCode, action)
            .fromIOToMain({ onDataReceived(it) }) { t ->
                onDataGetError(
                    t
                )
            })
    }

    fun permit() {
        requestData(actionState)
    }

    private fun onDataGetError(t: Throwable) {
        showError(t)
    }

    private fun onDataReceived(res: VKApiProcessAuthCode) {
        if (res.status == 0) {
            actionState = 1
            items.clear()
            val mainSection = Section(Text(R.string.mail_information))

            items.add(
                AdvancedItem(
                    1, Text(R.string.id),
                    AdvancedItem.TYPE_COPY_DETAILS_ONLY,
                    autolink = false
                )
                    .setSubtitle(Text(res.auth_id))
                    .setIcon(Icon.fromResources(R.drawable.person))
                    .setSection(mainSection)
            )
            items.add(
                AdvancedItem(
                    2, Text(R.string.ip),
                    AdvancedItem.TYPE_COPY_DETAILS_ONLY,
                    autolink = false
                )
                    .setSubtitle(Text(res.ip))
                    .setIcon(Icon.fromResources(R.drawable.star))
                    .setSection(mainSection)
            )
            items.add(
                AdvancedItem(
                    3, Text(R.string.browser),
                    AdvancedItem.TYPE_COPY_DETAILS_ONLY,
                    autolink = false
                )
                    .setSubtitle(Text(res.browser_name))
                    .setIcon(Icon.fromResources(R.drawable.web))
                    .setSection(mainSection)
            )
            items.add(
                AdvancedItem(
                    4, Text(R.string.city),
                    AdvancedItem.TYPE_COPY_DETAILS_ONLY,
                    autolink = false
                )
                    .setSubtitle(Text(res.location))
                    .setIcon(Icon.fromResources(R.drawable.ic_city))
                    .setSection(mainSection)
            )
            view?.notifyDataSetChanged()
        } else {
            view?.success()
        }
    }

    init {
        requestData(actionState)
    }
}
