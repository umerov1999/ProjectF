package dev.ragnarok.fenrir.fragment.docs.absdocumentpreview

import android.content.Context
import android.os.Bundle
import android.view.View
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IDocsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

open class BaseDocumentPresenter<V : IBasicDocumentView>(
    accountId: Long,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<V>(accountId, savedInstanceState) {
    private val docsInteractor: IDocsInteractor = InteractorFactory.createDocsInteractor()
    fun fireWritePermissionResolved(context: Context, view: View?) {
        onWritePermissionResolved(context, view)
    }

    protected open fun onWritePermissionResolved(context: Context, view: View?) {
        // hook for child classes
    }

    protected fun addYourself(document: Document) {
        val docId = document.id
        val ownerId = document.ownerId
        appendJob(
            docsInteractor.add(accountId, docId, ownerId, document.accessKey)
                .fromIOToMain({
                    onDocAddedSuccessfully()
                }) {
                    showError(getCauseIfRuntime(it))
                })
    }

    protected fun delete(id: Int, ownerId: Long) {
        appendJob(
            docsInteractor.delete(accountId, id, ownerId)
                .fromIOToMain({
                    onDocDeleteSuccessfully()
                }) { t -> onDocDeleteError(t) })
    }

    private fun onDocDeleteError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun onDocDeleteSuccessfully() {
        showToast(R.string.deleted, true)
    }

    private fun onDocAddedSuccessfully() {
        showToast(R.string.added, true)
    }

}