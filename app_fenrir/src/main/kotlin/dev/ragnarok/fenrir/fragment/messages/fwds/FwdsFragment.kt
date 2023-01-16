package dev.ragnarok.fenrir.fragment.messages.fwds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.VoiceActionListener
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.messages.chat.MessagesAdapter
import dev.ragnarok.fenrir.fragment.messages.chat.MessagesAdapter.OnMessageActionListener
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.LastReadId
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.VoiceMessage

class FwdsFragment : PlaceSupportMvpFragment<FwdsPresenter, IFwdsView>(), OnMessageActionListener,
    IFwdsView, VoiceActionListener {
    private var mAdapter: MessagesAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_fwds, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mAdapter = MessagesAdapter(requireActivity(), mutableListOf(), this, true)
        mAdapter?.setOnMessageActionListener(this)
        mAdapter?.setVoiceActionListener(this)
        recyclerView.adapter = mAdapter
        return root
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.subtitle = null
            actionBar.setTitle(R.string.title_messages)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onAvatarClick(message: Message, userId: Long, position: Int) {
        onOpenOwner(userId)
    }

    override fun onLongAvatarClick(message: Message, userId: Long, position: Int) {
        onOpenOwner(userId)
    }

    override fun onRestoreClick(message: Message, position: Int) {
        // not supported
    }

    override fun onBotKeyboardClick(button: Keyboard.Button) {
        // not supported
    }

    override fun onMessageLongClick(message: Message, position: Int): Boolean {
        // not supported
        return false
    }

    override fun onMessageClicked(message: Message, position: Int) {
        // not supported
    }

    override fun onMessageDelete(message: Message) {}
    override fun displayMessages(messages: MutableList<Message>, lastReadId: LastReadId) {
        mAdapter?.setItems(messages, lastReadId)
    }

    override fun notifyMessagesUpAdded(position: Int, count: Int) {
        // not supported
    }

    override fun notifyDataChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyMessagesDownAdded(count: Int) {
        // not supported
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

    override fun showActionMode(
        title: String,
        canEdit: Boolean,
        canPin: Boolean,
        canStar: Boolean,
        doStar: Boolean,
        canSpam: Boolean
    ) {
        // not supported
    }

    override fun finishActionMode() {
        // not supported
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemBindableChanged(index)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FwdsPresenter> {
        return object : IPresenterFactory<FwdsPresenter> {
            override fun create(): FwdsPresenter {
                val messages: ArrayList<Message> =
                    requireArguments().getParcelableArrayListCompat(Extra.MESSAGES)!!
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                return FwdsPresenter(accountId, messages, saveInstanceState)
            }
        }
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
        peerId: Long,
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
        fun buildArgs(accountId: Long, messages: ArrayList<Message>): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelableArrayList(Extra.MESSAGES, messages)
            return args
        }

        fun newInstance(args: Bundle?): FwdsFragment {
            val fwdsFragment = FwdsFragment()
            fwdsFragment.arguments = args
            return fwdsFragment
        }
    }
}