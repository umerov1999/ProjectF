package dev.ragnarok.fenrir.fragment.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.CallSuper
import dev.ragnarok.fenrir.Includes.voicePlayerFactory
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.media.voice.IVoicePlayer
import dev.ragnarok.fenrir.media.voice.IVoicePlayer.IPlayerStatusListener
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LastReadId
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Lookup
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils

abstract class AbsMessageListPresenter<V : IBasicMessageListView> internal constructor(
    accountId: Long,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<V>(accountId, savedInstanceState), IPlayerStatusListener {
    protected val lastReadId = LastReadId(0, 0)
    val data: ArrayList<Message> = ArrayList()
    private var mVoicePlayer: IVoicePlayer? = null
    private var mVoiceMessageLookup: Lookup?
    private fun syncVoiceLookupState() {
        val needLookup = mVoicePlayer?.isSupposedToPlay == true && guiIsReady
        if (needLookup) {
            mVoiceMessageLookup?.start()
        } else {
            mVoiceMessageLookup?.stop()
        }
    }

    protected open fun resolveListView() {
        view?.displayMessages(accountId, data, lastReadId)
    }

    protected fun indexOf(conversationMessageId: Int, peerId: Long): Int {
        for (i in data.indices) {
            if (data[i].conversation_message_id == conversationMessageId && data[i].peerId == peerId) {
                return i
            }
        }
        return -1
    }

    protected fun findById(conversationMessageId: Int, peerId: Long): Message? {
        for (element in data) {
            if (element.conversation_message_id == conversationMessageId && element.peerId == peerId) {
                return element
            }
        }
        return null
    }

    protected fun indexOf(messageId: Int): Int {
        return Utils.indexOf(data, messageId)
    }

    protected fun findById(messageId: Int): Message? {
        return Utils.findById(data, messageId)
    }

    fun clearSelection(position: Int) {
        if (position >= 0 && data.size > position) {
            data[position].isSelected = false
            safeNotifyItemChanged(position)
        }
    }

    private fun clearSelection(): Boolean {
        var hasChanges = false
        for (message in data) {
            if (message.isSelected) {
                message.isSelected = false
                hasChanges = true
            }
        }
        return hasChanges
    }

    open fun resolveActionMode() {
        val selectionCount = Utils.countOfSelection(data)
        if (selectionCount > 0) {
            view?.showActionMode(
                selectionCount.toString(),
                canEdit = false,
                canPin = false,
                canStar = false,
                doStar = false,
                canSpam = true
            )
        } else {
            view?.finishActionMode()
        }
    }

    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        syncVoiceLookupState()
        resolveListView()
        resolveActionMode()
    }

    protected fun safeNotifyDataChanged() {
        view?.notifyDataChanged()
    }

    fun safeNotifyItemChanged(index: Int) {
        view?.notifyItemChanged(index)
    }

    fun fireMessageLongClick(message: Message, position: Int) {
        message.isSelected = !message.isSelected
        resolveActionMode()
        safeNotifyItemChanged(position)
    }

    fun fireMessageClick(message: Message, position: Int, x: Int?, y: Int?) {
        val actionModeActive = Utils.countOfSelection(data)
        if (actionModeActive > 0) {
            message.isSelected = !message.isSelected
            resolveActionMode()
            safeNotifyItemChanged(position)
        } else {
            onMessageClick(message, position, x, y)
        }
    }

    protected open fun onMessageClick(
        message: Message,
        position: Int,
        x: Int?, y: Int?
    ) {
    }

    fun fireActionModeDestroy() {
        onActionModeDestroy()
    }

    @CallSuper
    protected fun onActionModeDestroy() {
        if (clearSelection()) {
            safeNotifyDataChanged()
        }
    }

    fun fireActionModeDeleteClick() {
        onActionModeDeleteClick()
    }

    protected open fun onActionModeDeleteClick() {}
    fun fireActionModeSpamClick() {
        onActionModeSpamClick()
    }

    protected open fun onActionModeSpamClick() {}
    fun fireActionModeCopyClick() {
        onActionModeCopyClick()
    }

    open fun fireReactionModeCopyClick(position: Int) {
        if (Utils.isHiddenAccount(accountId)) {
            return
        }
        if (position >= 0 && data.size > position) {
            data[position].setReactionEditMode(!data[position].reactionEditMode)
            safeNotifyItemChanged(position)
        }
    }

    private fun onActionModeCopyClick() {
        val selected: List<Message> = Utils.getSelected(
            data, true
        )
        if (selected.isEmpty()) return
        val result = StringBuilder()
        var firstTime = true
        for (message in selected) {
            val body =
                if (message.decryptedBody.isNullOrEmpty()) message.body else message.decryptedBody
            result.append(if (!firstTime) "\n" else "")
            result.append(body)
            if (message.isVoiceMessage) {
                if (!body.isNullOrEmpty()) {
                    result.append("\n\n")
                }
                var firstTimeVoice = true
                for (vs in message.attachments?.voiceMessages.orEmpty()) {
                    if (!vs.getTranscript().isNullOrEmpty()) {
                        result.append(if (!firstTimeVoice) "\n" else "")
                        result.append(vs.getTranscript())
                        firstTimeVoice = false
                    }
                }
            }
            firstTime = false
        }
        val clipboard = applicationContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("messages", result)
        clipboard?.setPrimaryClip(clip)
        view?.customToast?.setDuration(Toast.LENGTH_LONG)?.showToast(R.string.copied_to_clipboard)
    }

    fun fireForwardClick() {
        onActionModeForwardClick()
    }

    protected open fun onActionModeForwardClick() {}

    @Suppress("UNUSED_PARAMETER")
    fun fireVoicePlayButtonClick(
        voiceHolderId: Int,
        voiceMessageId: Int,
        messageId: Int,
        peerId: Long,
        voiceMessage: VoiceMessage
    ) {
        val player = mVoicePlayer ?: return
        try {
            val messageChanged = player.toggle(voiceMessageId, voiceMessage)
            if (messageChanged) {
                if (!voiceMessage.wasListened()) {
                    if (!Utils.isHiddenCurrent && Settings.get().main().isMarkListenedVoice) {
                        appendDisposable(
                            Repository.messages.markAsListened(accountId, messageId)
                                .fromIOToMain()
                                .subscribe({
                                    voiceMessage.setWasListened(true)
                                    resolveVoiceMessagePlayingState()
                                }, RxUtils.ignore())
                        )
                    }
                }
                resolveVoiceMessagePlayingState()
            } else {
                val paused = !player.isSupposedToPlay
                val progress = player.progress
                val isSpeed = player.isPlaybackSpeed
                view?.bindVoiceHolderById(
                    voiceHolderId,
                    true,
                    paused,
                    progress,
                    false,
                    isSpeed
                )
            }
        } catch (ignored: Exception) {
        }
        syncVoiceLookupState()
    }

    fun fireVoicePlaybackSpeed() {
        mVoicePlayer?.togglePlaybackSpeed()
    }

    internal fun resolveVoiceMessagePlayingState(anim: Boolean = false) {
        val player = mVoicePlayer ?: return
        val optionalVoiceMessageId = player.playingVoiceId
        if (optionalVoiceMessageId.isEmpty) {
            view?.disableVoicePlaying()
        } else {
            val progress = player.progress
            val paused = !player.isSupposedToPlay
            val isSpeed = player.isPlaybackSpeed

            view?.configNowVoiceMessagePlaying(
                optionalVoiceMessageId.get() ?: return,
                progress,
                paused,
                anim,
                isSpeed
            )
        }
    }

    public override fun onGuiDestroyed() {
        syncVoiceLookupState()
        super.onGuiDestroyed()
    }

    fun fireVoiceHolderCreated(voiceMessageId: Int, voiceHolderId: Int) {
        val player = mVoicePlayer ?: return
        val currentVoiceId = player.playingVoiceId
        val play = currentVoiceId.nonEmpty() && currentVoiceId.get() == voiceMessageId
        val paused = play && !player.isSupposedToPlay
        val isSpeed = player.isPlaybackSpeed
        view?.bindVoiceHolderById(
            voiceHolderId,
            play,
            paused,
            player.progress,
            false,
            isSpeed
        )
    }

    override fun onPlayerStatusChange(status: Int) {
        //Optional<Integer> voiceMessageId = mVoicePlayer.getPlayingVoiceId();
    }

    override fun onDestroyed() {
        super.onDestroyed()
        mVoicePlayer?.setCallback(null)
        mVoicePlayer?.release()
        mVoicePlayer = null
        mVoiceMessageLookup?.stop()
        mVoiceMessageLookup?.setCallback(null)
        mVoiceMessageLookup = null
    }

    private fun createVoicePlayer() {
        mVoicePlayer = voicePlayerFactory.createPlayer()
        mVoicePlayer?.setCallback(this)
    }

    private fun checkForwardedMessageForAudio(
        toFirst: Boolean,
        position: Int,
        audiosList: ArrayList<Audio>,
        message: Message
    ): Int {
        var tmpPosition = position
        message.attachments?.audios.nonNullNoEmpty {
            if (toFirst) {
                tmpPosition += it.size
                audiosList.addAll(0, it)
            } else {
                audiosList.addAll(it)
            }
        }
        message.fwd?.nonNullNoEmpty {
            for (i in it) {
                tmpPosition = checkForwardedMessageForAudio(toFirst, position, audiosList, i)
            }
        }
        return tmpPosition
    }

    fun fireAudioPlayClick(position: Int, audiosList: ArrayList<Audio>, holderPosition: Int?) {
        if (holderPosition == null) {
            view?.playAudioList(accountId, position, audiosList)
            return
        }
        var tmpPos = position
        val comboAudios = ArrayList<Audio>()
        comboAudios.addAll(audiosList)
        for (i in (holderPosition + 1)..<data.size.coerceAtMost(100)) {
            tmpPos = checkForwardedMessageForAudio(true, tmpPos, comboAudios, data[i])
        }
        if (holderPosition - 1 >= 0) {
            for (i in (holderPosition - 1) downTo 0) {
                tmpPos = checkForwardedMessageForAudio(false, tmpPos, comboAudios, data[i])
            }
        }
        view?.playAudioList(accountId, tmpPos, comboAudios)
    }

    init {
        createVoicePlayer()
        mVoiceMessageLookup = Lookup(500)
        mVoiceMessageLookup?.setCallback(object : Lookup.Callback {
            override fun onIterated() {
                resolveVoiceMessagePlayingState(true)
            }
        })
    }
}