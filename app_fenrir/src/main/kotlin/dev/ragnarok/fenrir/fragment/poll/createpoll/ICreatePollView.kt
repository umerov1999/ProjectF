package dev.ragnarok.fenrir.fragment.poll.createpoll

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.model.Poll

interface ICreatePollView : IMvpView, IProgressView, IErrorView {
    fun displayQuestion(question: String?)
    fun setAnonymous(anonymous: Boolean)
    fun displayOptions(options: Array<String?>?)
    fun showQuestionError(@StringRes message: Int)
    fun showOptionError(index: Int, @StringRes message: Int)
    fun sendResultAndGoBack(poll: Poll)
    fun setMultiply(multiply: Boolean)
    fun setDisableUnvote(disableUnvote: Boolean)
    fun setBackgroundsPoll(backgroundsPollList: List<Poll.PollBackground>, selected: Int)
}