package dev.ragnarok.fenrir.dialog

import android.app.Dialog
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.dialog.base.AccountDependencyDialogFragment
import dev.ragnarok.fenrir.domain.IUtilsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getExternalLinkPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.spots.SpotsDialog

class ResolveDomainDialog : AccountDependencyDialogFragment() {
    private var mAccountId = 0L
    private var url: String? = null
    private var domain: String? = null
    private var mUtilsInteractor: IUtilsInteractor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        mUtilsInteractor = InteractorFactory.createUtilsInteractor()
        url = requireArguments().getString(Extra.URL)
        domain = requireArguments().getString(Extra.DOMAIN)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progressDialog = SpotsDialog.Builder().setContext(requireActivity())
            .setMessage(getString(R.string.please_wait)).setCancelable(true).setCancelListener(this)
            .build()
        request()
        return progressDialog
    }

    private fun request() {
        appendDisposable(
            (mUtilsInteractor ?: return).resolveDomain(mAccountId, domain)
                .fromIOToMain()
                .subscribe({ optionalOwner -> onResolveResult(optionalOwner) }) {
                    onResolveError()
                })
    }

    private fun onResolveError() {
        url?.let {
            getExternalLinkPlace(mAccountId, it).tryOpenWith(requireActivity())
        }
        dismissAllowingStateLoss()
    }

    private fun onResolveResult(optionalOwner: Optional<Owner>) {
        if (optionalOwner.isEmpty) {
            getExternalLinkPlace(accountId, url ?: return).tryOpenWith(requireActivity())
        } else {
            getOwnerWallPlace(
                mAccountId,
                optionalOwner.get() ?: return
            ).tryOpenWith(requireActivity())
        }
        dismissAllowingStateLoss()
    }

    companion object {
        fun buildArgs(aid: Long, url: String?, domain: String?): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putString(Extra.URL, url)
            args.putString(Extra.DOMAIN, domain)
            return args
        }

        fun newInstance(aid: Long, url: String?, domain: String?): ResolveDomainDialog {
            return newInstance(buildArgs(aid, url, domain))
        }

        fun newInstance(args: Bundle?): ResolveDomainDialog {
            val domainDialog = ResolveDomainDialog()
            domainDialog.arguments = args
            return domainDialog
        }
    }
}