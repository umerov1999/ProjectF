package dev.ragnarok.fenrir.fragment.messages.chat

import android.net.Uri
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.fragment.messages.IBasicMessageListView
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.WriteText
import dev.ragnarok.fenrir.upload.UploadDestination

interface IChatView : IBasicMessageListView, IErrorView, IToastView {

    fun setupLoadUpHeaderState(@LoadMoreState state: Int)
    fun displayDraftMessageAttachmentsCount(count: Int)
    fun displayDraftMessageText(text: String?)
    fun appendMessageText(text: String?)
    fun displayToolbarTitle(text: String?)
    fun displayToolbarAvatar(peer: Peer?)
    fun displayToolbarSubtitle(text: String?)
    fun displayWriting(writeText: WriteText)
    fun hideWriting()
    fun displayWriting(owner: Owner, count: Int, is_text: Boolean)
    fun requestRecordPermissions()
    fun displayRecordingDuration(time: Long)
    fun doCloseAfterSend()
    fun scrollToUnread(position: Int, loading: Boolean)

    fun setupPrimaryButtonAsEditing(canSave: Boolean)
    fun setupPrimaryButtonAsRecording()
    fun setupPrimaryButtonAsRegular(canSend: Boolean, canStartRecording: Boolean)

    fun displayPinnedMessage(pinned: Message?, canChange: Boolean)
    fun hideInputView()

    fun goToMessageAttachmentsEditor(
        accountId: Long, messageOwnerId: Long, destination: UploadDestination,
        body: String?, attachments: ModelsBundle?, isGroupChat: Boolean
    )

    fun showErrorSendDialog(message: Message)
    fun notifyItemRemoved(position: Int)

    fun configOptionMenu(
        canLeaveChat: Boolean,
        canChangeTitle: Boolean,
        canShowMembers: Boolean,
        encryptionStatusVisible: Boolean,
        encryptionEnabled: Boolean,
        encryptionPlusEnabled: Boolean,
        keyExchangeVisible: Boolean,
        chronoVisible: Boolean,
        ProfileVisible: Boolean,
        InviteLink: Boolean
    )

    fun goToSearchMessage(accountId: Long, peer: Peer)
    fun showImageSizeSelectDialog(streams: List<Uri>)

    fun resetUploadImages()
    fun resetInputAttachments()
    fun notifyChatResume(accountId: Long, peerId: Long, title: String?, image: String?)
    fun goToConversationAttachments(accountId: Long, peerId: Long)
    fun goToChatMembers(accountId: Long, chatId: Long)
    fun showChatMembers(accountId: Long, chatId: Long)
    fun showChatTitleChangeDialog(initialValue: String?)
    fun showUserWall(accountId: Long, peerId: Long)
    fun forwardMessagesToAnotherConversation(messages: ArrayList<Message>, accountId: Long)
    fun displayForwardTypeSelectDialog(messages: ArrayList<Message>)
    fun setEmptyTextVisible(visible: Boolean)
    fun setupRecordPauseButton(available: Boolean, isPlaying: Boolean)
    fun displayInitiateKeyExchangeQuestion(@KeyLocationPolicy keyStoragePolicy: Int)
    fun showEncryptionKeysPolicyChooseDialog(requestCode: Int)
    fun showEncryptionDisclaimerDialog(requestCode: Int)
    fun showEditAttachmentsDialog(attachments: MutableList<AttachmentEntry>, isGroupChat: Boolean)

    fun displayEditingMessage(message: Message?)

    fun notifyEditAttachmentChanged(index: Int)
    fun notifyEditAttachmentRemoved(index: Int)
    fun startImagesSelection(accountId: Long, ownerId: Long)
    fun notifyEditAttachmentsAdded(position: Int, size: Int)
    fun notifyEditUploadProgressUpdate(id: Int, progress: Int)
    fun startVideoSelection(accountId: Long, ownerId: Long)
    fun startAudioSelection(accountId: Long)
    fun startDocSelection(accountId: Long)
    fun startCamera(fileUri: Uri)
    fun showDeleteForAllDialog(
        removeAllIds: ArrayList<Message>,
        editRemoveAllIds: ArrayList<Message>
    )

    fun scrollTo(position: Int)
    fun showSnackbar(@StringRes res: Int, isLong: Boolean)
    fun goToMessagesLookup(accountId: Long, peerId: Long, messageId: Int, message: Message)
    fun goToUnreadMessages(
        accountId: Long,
        messageId: Int,
        incoming: Int,
        outgoing: Int,
        unreadCount: Int,
        peer: Peer
    )

    fun convertToKeyboard(keyboard: Keyboard?)

    fun updateStickers(items: List<Sticker>)
    fun copyToClipBoard(link: String)

    fun openPollCreationWindow(accountId: Long, ownerId: Long)
}
