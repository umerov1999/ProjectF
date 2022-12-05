package dev.ragnarok.fenrir.fragment.search.messagessearch

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.messages.chat.MessagesAdapter
import dev.ragnarok.fenrir.fragment.messages.chat.MessagesAdapter.OnMessageActionListener
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.MessageSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.place.PlaceFactory.getChatPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getMessagesLookupPlace

class MessagesSearchFragment :
    AbsSearchFragment<MessagesSearchPresenter, IMessagesSearchView, Message, MessagesAdapter>(),
    OnMessageActionListener, IMessagesSearchView, AttachmentsViewBinder.VoiceActionListener {
    override fun setAdapterData(adapter: MessagesAdapter, data: MutableList<Message>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Message>): MessagesAdapter {
        val adapter = MessagesAdapter(requireActivity(), data, this, true)
        //adapter.setOnHashTagClickListener(this);
        adapter.setOnMessageActionListener(this)
        adapter.setVoiceActionListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity())
    }

    override fun onAvatarClick(message: Message, userId: Int, position: Int) {
        presenter?.fireOwnerClick(
            userId
        )
    }

    override fun onLongAvatarClick(message: Message, userId: Int, position: Int) {
        presenter?.fireOwnerClick(
            userId
        )
    }

    override fun onRestoreClick(message: Message, position: Int) {
        // delete is not supported
    }

    override fun onBotKeyboardClick(button: Keyboard.Button) {
        // is not supported
    }

    override fun onMessageLongClick(message: Message, position: Int): Boolean {
        presenter?.fireMessageLongClick(
            message
        )
        return true
    }

    override fun onMessageClicked(message: Message, position: Int) {
        presenter?.fireMessageClick(
            message
        )
    }

    override fun onMessageDelete(message: Message) {}
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<MessagesSearchPresenter> {
        return object : IPresenterFactory<MessagesSearchPresenter> {
            override fun create(): MessagesSearchPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val c: MessageSearchCriteria? =
                    requireArguments().getParcelableCompat(Extra.CRITERIA)
                return MessagesSearchPresenter(accountId, c, saveInstanceState)
            }
        }
    }

    override fun goToMessagesLookup(accountId: Int, peerId: Int, messageId: Int) {
        getMessagesLookupPlace(accountId, peerId, messageId, null).tryOpenWith(requireActivity())
    }

    override fun goToPeerLookup(accountId: Int, peer: Peer) {
        getChatPlace(accountId, accountId, peer).tryOpenWith(requireActivity())
    }

    override fun configNowVoiceMessagePlaying(
        id: Int,
        progress: Float,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    ) {
        mAdapter?.configNowVoiceMessagePlaying(id, progress, paused, amin, speed)
    }

    override fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        amin: Boolean,
        speed: Boolean
    ) {
        mAdapter?.bindVoiceHolderById(holderId, play, paused, progress, amin, speed)
    }

    override fun disableVoicePlaying() {
        mAdapter?.disableVoiceMessagePlaying()
    }


    override fun onVoiceHolderBinded(voiceMessageId: Int, voiceHolderId: Int) {
        presenter?.fireVoiceHolderCreated(
            voiceMessageId,
            voiceHolderId
        )
    }

    override fun onVoicePlayButtonClick(
        voiceHolderId: Int,
        voiceMessageId: Int,
        messageId: Int,
        peerId: Int,
        voiceMessage: VoiceMessage
    ) {
        presenter?.fireVoicePlayButtonClick(
            voiceHolderId,
            voiceMessageId,
            messageId,
            peerId,
            voiceMessage
        )
    }

    override fun onVoiceTogglePlaybackSpeed() {
        presenter?.fireVoicePlaybackSpeed()
    }

    override fun onTranscript(voiceMessageId: String, messageId: Int) {
        presenter?.fireTranscript(
            voiceMessageId,
            messageId
        )
    }

    companion object {
        fun newInstance(
            accountId: Int,
            initialCriteria: MessageSearchCriteria?
        ): MessagesSearchFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            val fragment = MessagesSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}