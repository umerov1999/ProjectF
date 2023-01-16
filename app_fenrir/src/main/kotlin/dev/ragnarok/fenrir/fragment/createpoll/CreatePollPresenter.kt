package dev.ragnarok.fenrir.fragment.createpoll

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IPollInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime

class CreatePollPresenter(
    accountId: Long,
    private val mOwnerId: Long,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<ICreatePollView>(accountId, savedInstanceState) {
    private val pollInteractor: IPollInteractor = InteractorFactory.createPollInteractor()
    private var mQuestion: String? = null
    private var mOptions: Array<String?>? = null
    private var mAnonymous = false
    private var mMultiply = false
    private var creationNow = false
    override fun onGuiCreated(viewHost: ICreatePollView) {
        super.onGuiCreated(viewHost)
        viewHost.displayQuestion(mQuestion)
        viewHost.setAnonymous(mAnonymous)
        viewHost.setMultiply(mMultiply)
        viewHost.displayOptions(mOptions)
        resolveProgressDialog()
    }

    private fun setCreationNow(creationNow: Boolean) {
        this.creationNow = creationNow
        resolveProgressDialog()
    }

    private fun create() {
        if (mQuestion.isNullOrEmpty()) {
            view?.showQuestionError(R.string.field_is_required)
            return
        }
        val nonEmptyOptions: MutableList<String> = ArrayList()
        mOptions?.let {
            for (o in it) {
                if (!o.isNullOrEmpty()) {
                    nonEmptyOptions.add("\"" + o + "\"")
                }
            }
        }
        if (nonEmptyOptions.isEmpty()) {
            view?.showOptionError(
                0,
                R.string.field_is_required
            )
            return
        }
        setCreationNow(true)
        appendDisposable(pollInteractor.createPoll(
            accountId,
            mQuestion,
            mAnonymous,
            mMultiply,
            mOwnerId,
            nonEmptyOptions
        )
            .fromIOToMain()
            .subscribe({ onPollCreated(it) }) { t ->
                onPollCreateError(
                    t
                )
            })
    }

    private fun onPollCreateError(t: Throwable) {
        setCreationNow(false)
        showError(getCauseIfRuntime(t))
    }

    private fun onPollCreated(poll: Poll) {
        setCreationNow(false)
        view?.sendResultAndGoBack(
            poll
        )
    }

    private fun resolveProgressDialog() {
        if (creationNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.publication,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    fun fireQuestionEdited(text: CharSequence?) {
        mQuestion = text?.toString()
    }

    fun fireOptionEdited(index: Int, s: CharSequence?) {
        mOptions?.set(index, s?.toString())
    }

    fun fireAnonyamousChecked(b: Boolean) {
        mAnonymous = b
    }

    fun fireDoneClick() {
        create()
    }

    fun fireMultiplyChecked(isChecked: Boolean) {
        mMultiply = isChecked
    }

    init {
        if (savedInstanceState == null) {
            mOptions = arrayOfNulls(10)
        }
    }
}