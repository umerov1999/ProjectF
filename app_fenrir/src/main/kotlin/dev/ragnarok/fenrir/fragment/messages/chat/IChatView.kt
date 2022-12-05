package dev.ragnarok.fenrir.fragment.messages.chat

import android.net.Uri
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.fragment.messages.IBasicMessageListView
import dev.ragnarok.fenrir.model.*
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
    fun setupPrimaryButtonAsRegular(canSend: Boolean, canStartRecoring: Boolean)

    fun displayPinnedMessage(pinned: Message?, canChange: Boolean)
    fun hideInputView()

    fun goToMessageAttachmentsEditor(
        accountId: Int, messageOwnerId: Int, destination: UploadDestination,
        body: String?, attachments: ModelsBundle?
    )

    fun showErrorSendDialog(message: Message)
    fun notifyItemRemoved(position: Int)

    fun configOptionMenu(
        canLeaveChat: Boolean,
        canChangeTitle: Boolean,
        canShowMembers: Boolean,
        encryptionStatusVisible: Boolean,
        encryprionEnabled: Boolean,
        encryptionPlusEnabled: Boolean,
        keyExchangeVisible: Boolean,
        HronoVisible: Boolean,
        ProfileVisible: Boolean,
        InviteLink: Boolean
    )

    fun goToSearchMessage(accountId: Int, peer: Peer)
    fun showImageSizeSelectDialog(streams: List<Uri>)

    fun resetUploadImages()
    fun resetInputAttachments()
    fun notifyChatResume(accountId: Int, peerId: Int, title: String?, image: String?)
    fun goToConversationAttachments(accountId: Int, peerId: Int)
    fun goToChatMembers(accountId: Int, chatId: Int)
    fun showChatMembers(accountId: Int, chatId: Int)
    fun showChatTitleChangeDialog(initialValue: String?)
    fun showUserWall(accountId: Int, peerId: Int)
    fun forwardMessagesToAnotherConversation(messages: ArrayList<Message>, accountId: Int)
    fun diplayForwardTypeSelectDialog(messages: ArrayList<Message>)
    fun setEmptyTextVisible(visible: Boolean)
    fun setupRecordPauseButton(available: Boolean, isPlaying: Boolean)
    fun displayIniciateKeyExchangeQuestion(@KeyLocationPolicy keyStoragePolicy: Int)
    fun showEncryptionKeysPolicyChooseDialog(requestCode: Int)
    fun showEncryptionDisclaimerDialog(requestCode: Int)
    fun showEditAttachmentsDialog(attachments: MutableList<AttachmentEntry>)

    fun displayEditingMessage(message: Message?)

    fun notifyEditAttachmentChanged(index: Int)
    fun notifyEditAttachmentRemoved(index: Int)
    fun startImagesSelection(accountId: Int, ownerId: Int)
    fun notifyEditAttachmentsAdded(position: Int, size: Int)
    fun notifyEditUploadProgressUpdate(id: Int, progress: Int)
    fun startVideoSelection(accountId: Int, ownerId: Int)
    fun startAudioSelection(accountId: Int)
    fun startDocSelection(accountId: Int)
    fun startCamera(fileUri: Uri)
    fun showDeleteForAllDialog(
        removeAllIds: ArrayList<Message>,
        editRemoveAllIds: ArrayList<Message>
    )

    fun scrollTo(position: Int)
    fun showSnackbar(@StringRes res: Int, isLong: Boolean)
    fun goToMessagesLookup(accountId: Int, peerId: Int, messageId: Int, message: Message)
    fun goToUnreadMessages(
        accountId: Int,
        messageId: Int,
        incoming: Int,
        outgoing: Int,
        unreadCount: Int,
        peer: Peer
    )

    fun convert_to_keyboard(keyboard: Keyboard?)

    fun updateStickers(items: List<Sticker>)
    fun copyToClipBoard(link: String)
}
