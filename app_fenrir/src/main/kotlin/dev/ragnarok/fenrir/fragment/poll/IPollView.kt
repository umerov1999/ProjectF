package dev.ragnarok.fenrir.fragment.poll

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Poll

interface IPollView : IMvpView, IErrorView {
    fun displayQuestion(title: String?)
    fun displayPhoto(photo_url: String?)
    fun displayType(anonymous: Boolean)
    fun displayCreationTime(unixtime: Long)
    fun displayVoteCount(count: Int)
    fun displayVotesList(
        answers: MutableList<Poll.Answer>?,
        canCheck: Boolean,
        multiply: Boolean,
        checked: MutableSet<Long>
    )

    fun displayLoading(loading: Boolean)
    fun setupButton(voted: Boolean)

    fun openVoters(
        accountId: Long,
        ownerId: Long,
        pollId: Int,
        board: Boolean,
        answer: Long
    )
}