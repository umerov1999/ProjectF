package dev.ragnarok.fenrir.fragment.poll.createpoll

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IPollInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.rxutils.RxUtils

class CreatePollPresenter(
    accountId: Long,
    private val mOwnerId: Long,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<ICreatePollView>(accountId, savedInstanceState) {
    private val pollInteractor: IPollInteractor = InteractorFactory.createPollInteractor()
    private var mQuestion: String? = null
    private var mOptions: Array<String?> = arrayOfNulls(10)
    private var mAnonymous = false
    private var mMultiply = false
    private var mDisableUnvote = false
    private var creationNow = false
    private val backgroundsPollList = ArrayList<Poll.PollBackground>(1)
    private var selectedBackgroundPoll: Int = 0
    override fun onGuiCreated(viewHost: ICreatePollView) {
        super.onGuiCreated(viewHost)
        viewHost.displayQuestion(mQuestion)
        viewHost.setAnonymous(mAnonymous)
        viewHost.setMultiply(mMultiply)
        viewHost.setDisableUnvote(mDisableUnvote)
        viewHost.displayOptions(mOptions)
        resolveProgressDialog()
        viewHost.setBackgroundsPoll(backgroundsPollList, selectedBackgroundPoll)
    }

    fun fireSelectedBackgroundPoll(selectedBackgroundPoll: Int) {
        this.selectedBackgroundPoll = selectedBackgroundPoll
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
        mOptions.let {
            for (o in it) {
                if (o.nonNullNoEmpty()) {
                    nonEmptyOptions.add(o)
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
        val backgroundId =
            if (backgroundsPollList[selectedBackgroundPoll].id == -1 && backgroundsPollList[selectedBackgroundPoll].name == "default") null else backgroundsPollList[selectedBackgroundPoll].id
        setCreationNow(true)
        appendDisposable(pollInteractor.createPoll(
            accountId,
            mQuestion,
            mAnonymous,
            mMultiply,
            mDisableUnvote,
            backgroundId,
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
        mOptions[index] = s?.toString()
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

    fun fireDisableUnvoteChecked(disableUnvote: Boolean) {
        mDisableUnvote = disableUnvote
    }

    init {
        backgroundsPollList.add(Poll.PollBackground(-1).setName("default"))
        appendDisposable(
            pollInteractor.getBackgrounds(accountId).fromIOToMain().subscribe(
                {
                    backgroundsPollList.clear()
                    backgroundsPollList.addAll(it)
                    view?.setBackgroundsPoll(backgroundsPollList, 0)
                }, RxUtils.ignore()
            )
        )
    }
}