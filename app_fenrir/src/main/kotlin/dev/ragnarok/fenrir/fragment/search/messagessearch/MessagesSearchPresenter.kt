package dev.ragnarok.fenrir.fragment.search.messagessearch

import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.Mode
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.MessageSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.media.voice.IVoicePlayer
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Lookup
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import io.reactivex.rxjava3.core.Single

class MessagesSearchPresenter(
    accountId: Long,
    criteria: MessageSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IMessagesSearchView, MessageSearchCriteria, Message, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
), IVoicePlayer.IPlayerStatusListener {
    private var mVoicePlayer: IVoicePlayer? = null
    private var mVoiceMessageLookup: Lookup?
    private val messagesInteractor: IMessagesRepository = messages
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): MessageSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    private fun syncVoiceLookupState() {
        val needLookup = mVoicePlayer?.isSupposedToPlay == true && guiIsReady
        if (needLookup) {
            mVoiceMessageLookup?.start()
        } else {
            mVoiceMessageLookup?.stop()
        }
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
        mVoicePlayer = Includes.voicePlayerFactory.createPlayer()
        mVoicePlayer?.setCallback(this)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Long,
        criteria: MessageSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Message>, IntNextFrom>> {
        val offset = startFrom.offset
        return messagesInteractor
            .searchMessages(accountId, criteria.peerId, COUNT, offset, criteria.query)
            .map { messages -> create(messages, IntNextFrom(offset + COUNT)) }
    }

    override fun instantiateEmptyCriteria(): MessageSearchCriteria {
        return MessageSearchCriteria("")
    }

    override fun canSearch(criteria: MessageSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    fun fireMessageLongClick(message: Message) {
        appendDisposable(
            messagesInteractor.getConversationSingle(
                accountId, message.peerId,
                Mode.NET
            ).fromIOToMain().subscribe({
                view?.goToPeerLookup(
                    accountId,
                    Peer(message.peerId).setTitle(it.getTitle()).setAvaUrl(it.imageUrl)
                )
            }, { showError(it) })
        )
    }

    fun fireMessageClick(message: Message) {
        view?.goToMessagesLookup(
            accountId,
            message.peerId,
            message.getObjectId()
        )
    }

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
                            messages.markAsListened(accountId, messageId)
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

    fun fireTranscript(voiceMessageId: String?, messageId: Int) {
        appendDisposable(
            messages.recogniseAudioMessage(accountId, messageId, voiceMessageId)
                .fromIOToMain()
                .subscribe({ }) { })
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

    companion object {
        private const val COUNT = 50
    }

    init {
        createVoicePlayer()
        mVoiceMessageLookup = Lookup(500)
        mVoiceMessageLookup?.setCallback(object : Lookup.Callback {
            override fun onIterated() {
                resolveVoiceMessagePlayingState(true)
            }
        })
        if (canSearch(criteria)) {
            doSearch()
        }
    }
}