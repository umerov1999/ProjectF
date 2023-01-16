package dev.ragnarok.fenrir.fragment.communitycontrol.communityban

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IGroupSettingsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.BlockReason
import dev.ragnarok.fenrir.model.IdOption
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Logger.wtf
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import java.text.DateFormat
import java.util.*

class CommunityBanEditPresenter : AccountDependencyPresenter<ICommunityBanEditView> {
    private val groupId: Long
    private val banned: Banned?
    private val users: ArrayList<Owner>
    private val interactor: IGroupSettingsInteractor
    private var index: Int
    private var blockFor: BlockFor
    private var reason: Int
    private var comment: String? = null
    private var showCommentToUser = false
    private var requestNow = false

    constructor(
        accountId: Long,
        groupId: Long,
        banned: Banned,
        savedInstanceState: Bundle?
    ) : super(
        accountId,
        savedInstanceState
    ) {
        this.groupId = groupId
        this.banned = banned
        users = singletonArrayList(banned.banned)
        val info = banned.info
        blockFor = BlockFor(info.endDate)
        reason = info.reason
        comment = info.comment
        showCommentToUser = info.isCommentVisible
        index = 0
        interactor = InteractorFactory.createGroupSettingsInteractor()
    }

    constructor(
        accountId: Long,
        groupId: Long,
        users: ArrayList<Owner>,
        savedInstanceState: Bundle?
    ) : super(accountId, savedInstanceState) {
        this.groupId = groupId
        banned = null
        this.users = users
        index = 0
        blockFor = BlockFor(BlockFor.FOREVER) // by default
        reason = BlockReason.OTHER
        interactor = InteractorFactory.createGroupSettingsInteractor()
    }

    private fun currentBanned(): Owner {
        return users[index]
    }

    private fun resolveCommentViews() {
        view?.diplayComment(
            comment
        )
        view?.setShowCommentChecked(
            showCommentToUser
        )
    }

    private fun resolveBanStatusView() {
        if (banned != null) {
            view?.displayBanStatus(
                banned.admin.getOwnerObjectId(),
                banned.admin.fullName,
                banned.info.endDate
            )
        }
    }

    private fun resolveUserInfoViews() {
        view?.displayUserInfo(
            currentBanned()
        )
    }

    private fun resolveBlockForView() {
        val blockForText: String = when (blockFor.type) {
            BlockFor.FOREVER -> getString(R.string.block_for_forever)
            BlockFor.YEAR -> getString(R.string.block_for_year)
            BlockFor.MONTH -> getString(R.string.block_for_month)
            BlockFor.WEEK -> getString(R.string.block_for_week)
            BlockFor.DAY -> getString(R.string.block_for_day)
            BlockFor.HOUR -> getString(R.string.block_for_hour)
            BlockFor.CUSTOM -> formatBlockFor()
            else -> throw IllegalStateException()
        }
        view?.displayBlockFor(
            blockForText
        )
    }

    private fun resolveReasonView() {
        when (reason) {
            BlockReason.SPAM -> view?.displayReason(
                getString(R.string.reason_spam)
            )
            BlockReason.IRRELEVANT_MESSAGES -> view?.displayReason(
                getString(R.string.reason_irrelevant_messages)
            )
            BlockReason.STRONG_LANGUAGE -> view?.displayReason(
                getString(R.string.reason_strong_language)
            )
            BlockReason.VERBAL_ABUSE -> view?.displayReason(
                getString(R.string.reason_verbal_abuse)
            )
            else -> view?.displayReason(
                getString(R.string.reason_other)
            )
        }
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveProgressView()
    }

    private fun resolveProgressView() {
        if (requestNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.saving,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    override fun onGuiCreated(viewHost: ICommunityBanEditView) {
        super.onGuiCreated(viewHost)
        resolveUserInfoViews()
        resolveProgressView()
        resolveReasonView()
        resolveBlockForView()
        resolveBanStatusView()
        resolveCommentViews()
    }

    fun fireButtonSaveClick() {
        setRequestNow(true)
        val ownerId = currentBanned().ownerId
        val endDate = blockFor.unblockingDate
        val endDateUnixtime = if (endDate != null) endDate.time / 1000 else null
        appendDisposable(interactor.ban(
            accountId,
            groupId,
            ownerId,
            endDateUnixtime,
            reason,
            comment,
            showCommentToUser
        )
            .fromIOToMain()
            .subscribe({ onAddBanComplete() }) { throwable ->
                onAddBanError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    private fun onAddBanComplete() {
        setRequestNow(false)
        view?.customToast?.showToastSuccessBottom(
            R.string.success
        )
        if (index == users.size - 1) {
            view?.goBack()
        } else {
            // switch to next user
            index++
            resolveUserInfoViews()
        }
    }

    private fun onAddBanError(throwable: Throwable) {
        setRequestNow(false)
        throwable.printStackTrace()
        showError(throwable)
    }

    fun fireShowCommentCheck(isChecked: Boolean) {
        showCommentToUser = isChecked
    }

    fun fireCommentEdit(s: CharSequence?) {
        comment = s.toString()
    }

    fun fireBlockForClick() {
        val options: MutableList<IdOption> = ArrayList()
        if (blockFor.type == BlockFor.CUSTOM) {
            options.add(IdOption(BLOCK_FOR_UNCHANGED, formatBlockFor()))
        }
        options.add(IdOption(BlockFor.FOREVER, getString(R.string.block_for_forever)))
        options.add(IdOption(BlockFor.YEAR, getString(R.string.block_for_year)))
        options.add(IdOption(BlockFor.MONTH, getString(R.string.block_for_month)))
        options.add(IdOption(BlockFor.WEEK, getString(R.string.block_for_week)))
        options.add(IdOption(BlockFor.DAY, getString(R.string.block_for_day)))
        options.add(IdOption(BlockFor.HOUR, getString(R.string.block_for_hour)))
        view?.displaySelectOptionDialog(
            REQUEST_CODE_BLOCK_FOR, options
        )
    }

    fun fireOptionSelected(requestCode: Int, idOption: IdOption) {
        when (requestCode) {
            REQUEST_CODE_BLOCK_FOR -> if (idOption.getObjectId() != BLOCK_FOR_UNCHANGED) {
                blockFor = BlockFor(idOption.getObjectId())
                resolveBlockForView()
            } //else not changed
            REQUEST_CODE_REASON -> {
                reason = idOption.getObjectId()
                resolveReasonView()
            }
        }
    }

    private fun formatBlockFor(): String {
        val date = blockFor.unblockingDate
        if (date == null) {
            wtf(TAG, "formatBlockFor, date-is-null???")
            return "NULL"
        }
        val formattedDate = DateFormat.getDateInstance().format(date)
        val formattedTime = DateFormat.getTimeInstance().format(date)
        return getString(R.string.until_date_time, formattedDate, formattedTime)
    }

    fun fireResonClick() {
        val options: MutableList<IdOption> = ArrayList()
        options.add(IdOption(BlockReason.SPAM, getString(R.string.reason_spam)))
        options.add(
            IdOption(
                BlockReason.IRRELEVANT_MESSAGES,
                getString(R.string.reason_irrelevant_messages)
            )
        )
        options.add(
            IdOption(
                BlockReason.STRONG_LANGUAGE,
                getString(R.string.reason_strong_language)
            )
        )
        options.add(IdOption(BlockReason.VERBAL_ABUSE, getString(R.string.reason_verbal_abuse)))
        options.add(IdOption(BlockReason.OTHER, getString(R.string.reason_other)))
        view?.displaySelectOptionDialog(
            REQUEST_CODE_REASON, options
        )
    }

    fun fireAvatarClick() {
        view?.openProfile(
            accountId,
            currentBanned()
        )
    }

    private class BlockFor {
        val type: Int
        val customDate: Long

        constructor(type: Int) {
            this.type = type
            customDate = 0
        }

        constructor(customDate: Long) {
            this.customDate = customDate
            type = if (customDate > 0) CUSTOM else FOREVER
        }

        val unblockingDate: Date?
            get() {
                if (type == CUSTOM) {
                    return Date(customDate * 1000)
                }
                val calendar = Calendar.getInstance()
                when (type) {
                    YEAR -> calendar.add(Calendar.YEAR, 1)
                    MONTH -> calendar.add(Calendar.MONTH, 1)
                    WEEK -> calendar.add(Calendar.DAY_OF_MONTH, 7)
                    DAY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                    HOUR -> calendar.add(Calendar.HOUR, 1)
                    FOREVER -> return null
                }
                return calendar.time
            }

        companion object {
            const val FOREVER = 0
            const val YEAR = 1
            const val MONTH = 2
            const val WEEK = 3
            const val DAY = 4
            const val HOUR = 5
            const val CUSTOM = 6
        }
    }

    companion object {
        private val TAG = CommunityBanEditPresenter::class.java.simpleName
        private const val BLOCK_FOR_UNCHANGED = -1
        private const val REQUEST_CODE_BLOCK_FOR = 1
        private const val REQUEST_CODE_REASON = 2
    }
}