package dev.ragnarok.fenrir.fragment.messages.chat

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.VoiceActionListener
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.CryptStatus
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.LastReadId
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.MessageStatus
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadSticker
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.MessageView
import dev.ragnarok.fenrir.view.OnlineView
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView
import dev.ragnarok.fenrir.view.emoji.BotKeyboardView.BotKeyboardViewDelegate
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import java.text.SimpleDateFormat
import java.util.Date

class MessagesAdapter(
    private val context: Context,
    items: MutableList<Message>,
    private var lastReadId: LastReadId,
    private val attachmentsActionCallback: OnAttachmentsActionCallback,
    disable_read: Boolean
) : RecyclerBindableAdapter<Message, RecyclerView.ViewHolder>(items) {
    private val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Utils.appLocale)
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(context, attachmentsActionCallback)
    private val avatarTransformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val selectedDrawable: ShapeDrawable = ShapeDrawable(OvalShape())
    private val unreadColor: Int
    private val disable_read: Boolean
    private val isNightSticker: Boolean
    private val ownerLinkAdapter: OwnerLinkSpanFactory.ActionListener =
        object : LinkActionAdapter() {
            override fun onOwnerClick(ownerId: Long) {
                attachmentsActionCallback.onOpenOwner(ownerId)
            }
        }
    private var onHashTagClickListener: EmojiconTextView.OnHashTagClickListener? = null
    private var onMessageActionListener: OnMessageActionListener? = null

    constructor(
        context: Context,
        items: MutableList<Message>,
        callback: OnAttachmentsActionCallback,
        disable_read: Boolean
    ) : this(context, items, LastReadId(0, 0), callback, disable_read)

    override fun onBindItemViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int,
        type: Int
    ) {
        val message = getItem(position)
        when (type) {
            TYPE_SERVICE -> bindServiceHolder(viewHolder as ServiceMessageHolder, message)
            TYPE_GRAFFITI_FRIEND, TYPE_GRAFFITI_MY, TYPE_MY_MESSAGE, TYPE_FRIEND_MESSAGE -> bindNormalMessage(
                viewHolder as MessageHolder,
                message
            )

            TYPE_STICKER_FRIEND, TYPE_STICKER_MY -> bindStickerHolder(
                viewHolder as StickerMessageHolder,
                message
            )

            TYPE_GIFT_FRIEND, TYPE_GIFT_MY -> bindGiftHolder(
                viewHolder as GiftMessageHolder,
                message
            )
        }
    }

    private fun bindStickerHolder(holder: StickerMessageHolder, message: Message) {
        bindBaseMessageHolder(holder, message)
        if (message.isDeleted) {
            holder.root.alpha = 0.6f
            holder.Restore.visibility = View.VISIBLE
            holder.Restore.setOnClickListener {
                onMessageActionListener?.onRestoreClick(
                    message,
                    getItemRawPosition(holder.bindingAdapterPosition)
                )
            }
        } else {
            holder.root.alpha = 1f
            holder.Restore.visibility = View.GONE
        }
        val sticker = message.attachments?.stickers?.get(0)
        if (sticker?.isAnimated == true) {
            holder.sticker.fromNet(
                sticker.getAnimationByType(if (isNightSticker) "dark" else "light"),
                Utils.createOkHttp(Constants.GIF_TIMEOUT, true),
                Utils.dp(128f),
                Utils.dp(128f)
            )
        } else {
            val image = sticker?.getImage(256, isNightSticker)
            image?.url.ifNonNullNoEmpty({
                with()
                    .load(it)
                    .into(holder.sticker)
            }, {
                with().cancelRequest(holder.sticker)
            })
        }
        holder.sticker.setOnLongClickListener {
            if (!hasReadWriteStoragePermission(context)) {
                attachmentsActionCallback.onRequestWritePermissions()
            } else {
                if (sticker != null) {
                    doDownloadSticker(context, sticker)
                }
            }
            true
        }
        val hasAttachments =
            message.fwd.nonNullNoEmpty() || message.attachments?.size_no_stickers().orZero() > 0
        holder.attachmentsRoot.visibility = if (hasAttachments) View.VISIBLE else View.GONE
        if (hasAttachments) {
            attachmentsViewBinder.displayAttachments(
                message.attachments,
                holder.attachmentsHolder,
                true,
                message.getObjectId(),
                message.peerId
            )
            attachmentsViewBinder.displayForwards(message.fwd, holder.forwardMessagesRoot, true)
        }
    }

    fun setItems(messages: MutableList<Message>, lastReadId: LastReadId) {
        this.lastReadId = lastReadId
        setItems(messages)
    }

    private fun bindGiftHolder(holder: GiftMessageHolder, message: Message) {
        bindBaseMessageHolder(holder, message)
        if (message.isDeleted) {
            holder.root.alpha = 0.6f
            holder.Restore.visibility = View.VISIBLE
            holder.Restore.setOnClickListener {
                onMessageActionListener?.onRestoreClick(
                    message,
                    getItemRawPosition(holder.bindingAdapterPosition)
                )
            }
        } else {
            holder.root.alpha = 1f
            holder.Restore.visibility = View.GONE
        }
        holder.message.visibility = if (message.body.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.message.text =
            OwnerLinkSpanFactory.withSpans(
                message.body,
                owners = true,
                topics = false,
                listener = ownerLinkAdapter
            )
        val giftItem = message.attachments?.gifts?.get(0)
        giftItem?.thumb256.ifNonNullNoEmpty({
            with()
                .load(it)
                .into(holder.gift)
        }, {
            with().cancelRequest(holder.gift)
        })
    }

    private fun bindStatusText(textView: TextView, status: Int, time: Long, updateTime: Long) {
        when (status) {
            MessageStatus.SENDING -> {
                textView.text = context.getString(R.string.sending)
                textView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.default_message_status
                    )
                )
            }

            MessageStatus.QUEUE -> {
                textView.text = context.getString(R.string.in_order)
                textView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.default_message_status
                    )
                )
            }

            MessageStatus.ERROR -> {
                textView.text = context.getString(R.string.error)
                textView.setTextColor(Color.RED)
            }

            MessageStatus.WAITING_FOR_UPLOAD -> {
                textView.setText(R.string.waiting_for_upload)
                textView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.default_message_status
                    )
                )
            }

            else -> {
                var text = AppTextUtils.getDateFromUnixTime(time)
                if (updateTime != 0L) {
                    DATE.time = updateTime * 1000
                    text =
                        text + " " + context.getString(R.string.message_edited_at, df.format(DATE))
                }
                textView.text = text
                textView.setTextColor(CurrentTheme.getSecondaryTextColorCode(context))
            }
        }
    }

    private fun bindReadState(root: View, read: Boolean) {
        root.setBackgroundColor(if (read) Color.TRANSPARENT else unreadColor)
        root.background.alpha = 60
    }

    private fun bindBaseMessageHolder(holder: BaseMessageHolder, message: Message) {
        holder.important.visibility = if (message.isImportant) View.VISIBLE else View.GONE
        bindStatusText(holder.status, message.status, message.date, message.updateTime)
        var read =
            if (message.isOut) lastReadId.getOutgoing() >= message.getObjectId() else lastReadId.getIncoming() >= message.getObjectId()
        if (disable_read) read = true
        bindReadState(holder.itemView, message.status == MessageStatus.SENT && read)
        if (message.isSelected) {
            holder.itemView.setBackgroundColor(CurrentTheme.getColorSecondary(context))
            holder.itemView.background.alpha = 80
            holder.avatar.background = selectedDrawable
            holder.avatar.setImageResource(R.drawable.ic_message_check_vector)
        } else {
            val avaurl = message.sender?.maxSquareAvatar
            displayAvatar(holder.avatar, avatarTransformation, avaurl, Constants.PICASSO_TAG)
            holder.avatar.setBackgroundColor(Color.TRANSPARENT)
        }
        if (holder.user != null) holder.user.text = message.sender?.fullName
        holder.avatar.setOnClickListener {
            onMessageActionListener?.onAvatarClick(
                message,
                message.senderId,
                getItemRawPosition(holder.bindingAdapterPosition)
            )
        }
        holder.avatar.setOnLongClickListener {
            onMessageActionListener?.onLongAvatarClick(
                message,
                message.senderId,
                getItemRawPosition(holder.bindingAdapterPosition)
            )
            true
        }
        holder.itemView.setOnClickListener {
            onMessageActionListener?.onMessageClicked(
                message,
                getItemRawPosition(holder.bindingAdapterPosition)
            )
        }
        holder.itemView.setOnLongClickListener {
            onMessageActionListener?.onMessageLongClick(
                message,
                getItemRawPosition(holder.bindingAdapterPosition)
            ) == true
        }
    }

    private fun bindNormalMessage(holder: MessageHolder, message: Message) {
        bindBaseMessageHolder(holder, message)
        if (holder.botKeyboardView != null && message.keyboard?.inline == true && message.keyboard?.buttons?.size.orZero() > 0) {
            holder.botKeyboardView.visibility = View.VISIBLE
            holder.botKeyboardView.setButtons(message.keyboard?.buttons, false)
        } else {
            holder.botKeyboardView?.visibility = View.GONE
        }
        holder.botKeyboardView?.setDelegate(object : BotKeyboardViewDelegate {
            override fun didPressedButton(button: Keyboard.Button, needClose: Boolean) {
                onMessageActionListener?.onBotKeyboardClick(button)
            }
        })
        if (message.isDeleted) {
            holder.root.alpha = 0.6f
            holder.Restore.visibility = View.VISIBLE
            holder.Restore.setOnClickListener {
                onMessageActionListener?.onRestoreClick(
                    message,
                    getItemRawPosition(holder.bindingAdapterPosition)
                )
            }
        } else {
            holder.root.alpha = 1f
            holder.Restore.visibility = View.GONE
        }
        holder.body.visibility = if (message.body.isNullOrEmpty()) View.GONE else View.VISIBLE
        var displayedBody: String? = null
        when (message.cryptStatus) {
            CryptStatus.NO_ENCRYPTION, CryptStatus.ENCRYPTED, CryptStatus.DECRYPT_FAILED -> displayedBody =
                message.body

            CryptStatus.DECRYPTED -> displayedBody = message.decryptedBody
        }
        if (!message.isGraffiti) {
            when (message.cryptStatus) {
                CryptStatus.ENCRYPTED, CryptStatus.DECRYPT_FAILED -> holder.bubble.setNonGradientColor(
                    Color.parseColor("#D4ff0000")
                )

                CryptStatus.NO_ENCRYPTION, CryptStatus.DECRYPTED -> if (message.isOut) {
                    if (Settings.get().other().isCustom_MyMessage) holder.bubble.setGradientColor(
                        Settings.get().other().colorMyMessage,
                        Settings.get().other().secondColorMyMessage
                    ) else {
                        if (Settings.get()
                                .main().isMy_message_no_color
                        ) holder.bubble.setNonGradientColor(
                            CurrentTheme.getColorFromAttrs(
                                R.attr.message_bubble_color,
                                context,
                                "#D4ff0000"
                            )
                        ) else {
                            holder.bubble.setGradientColor(
                                CurrentTheme.getColorFromAttrs(
                                    R.attr.my_messages_bubble_color,
                                    context,
                                    "#D4ff0000"
                                ),
                                CurrentTheme.getColorFromAttrs(
                                    R.attr.my_messages_secondary_bubble_color,
                                    context,
                                    "#D4ff0000"
                                )
                            )
                        }
                    }
                } else holder.bubble.setNonGradientColor(
                    CurrentTheme.getColorFromAttrs(
                        R.attr.message_bubble_color,
                        context,
                        "#D4ff0000"
                    )
                )
            }
        }
        holder.body.text =
            OwnerLinkSpanFactory.withSpans(
                displayedBody,
                owners = true,
                topics = false,
                listener = ownerLinkAdapter
            )
        holder.encryptedView.visibility =
            if (message.cryptStatus == CryptStatus.NO_ENCRYPTION) View.GONE else View.VISIBLE
        val hasAttachments =
            message.fwd.nonNullNoEmpty() || message.attachments?.hasAttachments == true
        holder.attachmentsRoot.visibility = if (hasAttachments) View.VISIBLE else View.GONE
        if (hasAttachments) {
            attachmentsViewBinder.displayAttachments(
                message.attachments,
                holder.attachmentsHolder,
                true,
                message.getObjectId(),
                message.peerId
            )
            attachmentsViewBinder.displayForwards(message.fwd, holder.forwardMessagesRoot, true)
        }
    }

    private fun bindServiceHolder(holder: ServiceMessageHolder, message: Message) {
        if (message.isDeleted) {
            holder.root.alpha = 0.6f
            holder.bRestore.visibility = View.VISIBLE
            holder.bRestore.setOnClickListener {
                onMessageActionListener?.onRestoreClick(
                    message,
                    getItemRawPosition(holder.bindingAdapterPosition)
                )
            }
        } else {
            holder.root.alpha = 1f
            holder.bRestore.visibility = View.GONE
        }
        if (message.keyboard?.inline == true && message.keyboard?.buttons?.size.orZero() > 0) {
            holder.botKeyboardView.visibility = View.VISIBLE
            holder.botKeyboardView.setButtons(message.keyboard?.buttons, false)
        } else {
            holder.botKeyboardView.visibility = View.GONE
        }
        holder.botKeyboardView.setDelegate(object : BotKeyboardViewDelegate {
            override fun didPressedButton(button: Keyboard.Button, needClose: Boolean) {
                onMessageActionListener?.onBotKeyboardClick(button)
            }
        })
        var read =
            if (message.isOut) lastReadId.getOutgoing() >= message.getObjectId() else lastReadId.getIncoming() >= message.getObjectId()
        if (disable_read) read = true
        bindReadState(holder.itemView, message.status == MessageStatus.SENT && read)
        holder.tvAction.text = message.getServiceText(context)
        holder.itemView.setOnClickListener {
            onMessageActionListener?.onMessageClicked(
                message,
                getItemRawPosition(holder.bindingAdapterPosition)
            )
        }
        holder.itemView.setOnLongClickListener {
            onMessageActionListener?.onMessageDelete(message)
            true
        }
        val hasAttachments =
            message.fwd.nonNullNoEmpty() || message.attachments?.size().orZero() > 0
        holder.attachmentsRoot.visibility = if (hasAttachments) View.VISIBLE else View.GONE
        attachmentsViewBinder.displayAttachments(
            message.attachments,
            holder.mAttachmentsHolder,
            true,
            message.getObjectId(),
            message.peerId
        )
    }

    override fun viewHolder(view: View, type: Int): RecyclerView.ViewHolder {
        when (type) {
            TYPE_GRAFFITI_FRIEND, TYPE_GRAFFITI_MY, TYPE_MY_MESSAGE, TYPE_FRIEND_MESSAGE -> return MessageHolder(
                view
            )

            TYPE_SERVICE -> return ServiceMessageHolder(view)
            TYPE_STICKER_FRIEND, TYPE_STICKER_MY -> return StickerMessageHolder(view)
            TYPE_GIFT_FRIEND, TYPE_GIFT_MY -> return GiftMessageHolder(view)
        }
        throw UnsupportedOperationException()
    }

    override fun layoutId(type: Int): Int {
        when (type) {
            TYPE_MY_MESSAGE -> return R.layout.item_message_my
            TYPE_FRIEND_MESSAGE -> return R.layout.item_message_friend
            TYPE_GRAFFITI_MY -> return R.layout.item_message_graffiti_my
            TYPE_GRAFFITI_FRIEND -> return R.layout.item_message_graffiti_friend
            TYPE_SERVICE -> return R.layout.item_service_message
            TYPE_STICKER_FRIEND -> return R.layout.item_message_friend_sticker
            TYPE_STICKER_MY -> return R.layout.item_message_my_sticker
            TYPE_GIFT_FRIEND -> return R.layout.item_message_friend_gift
            TYPE_GIFT_MY -> return R.layout.item_message_my_gift
        }
        throw IllegalArgumentException()
    }

    override fun getItemType(position: Int): Int {
        val m = getItem(position - headersCount)
        if (m.isServiseMessage) {
            return TYPE_SERVICE
        }
        if (m.isSticker) {
            return if (m.isOut) TYPE_STICKER_MY else TYPE_STICKER_FRIEND
        }
        if (m.isGraffiti) {
            return if (m.isOut) TYPE_GRAFFITI_MY else TYPE_GRAFFITI_FRIEND
        }
        if (m.isGift) {
            return if (m.isOut) TYPE_GIFT_MY else TYPE_GIFT_FRIEND
        }
        return if (m.isOut) TYPE_MY_MESSAGE else TYPE_FRIEND_MESSAGE
    }

    fun setVoiceActionListener(voiceActionListener: VoiceActionListener?) {
        attachmentsViewBinder.setVoiceActionListener(voiceActionListener)
    }

    fun configNowVoiceMessagePlaying(
        voiceId: Int,
        progress: Float,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    ) {
        attachmentsViewBinder.configNowVoiceMessagePlaying(voiceId, progress, paused, amin, speed)
    }

    fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        amin: Boolean,
        speed: Boolean
    ) {
        attachmentsViewBinder.bindVoiceHolderById(holderId, play, paused, progress, amin, speed)
    }

    fun disableVoiceMessagePlaying() {
        attachmentsViewBinder.disableVoiceMessagePlaying()
    }

    fun setOnHashTagClickListener(onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?) {
        this.onHashTagClickListener = onHashTagClickListener
    }

    fun setOnMessageActionListener(onMessageActionListener: OnMessageActionListener?) {
        this.onMessageActionListener = onMessageActionListener
    }

    interface OnMessageActionListener {
        fun onAvatarClick(message: Message, userId: Long, position: Int)
        fun onLongAvatarClick(message: Message, userId: Long, position: Int)
        fun onRestoreClick(message: Message, position: Int)
        fun onBotKeyboardClick(button: Keyboard.Button)
        fun onMessageLongClick(message: Message, position: Int): Boolean
        fun onMessageClicked(message: Message, position: Int)
        fun onMessageDelete(message: Message)
    }

    private class ServiceMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.item_message_bubble)
        val tvAction: TextView = itemView.findViewById(R.id.item_service_message_text)
        val attachmentsRoot: View = itemView.findViewById(R.id.item_message_attachment_container)
        val mAttachmentsHolder: AttachmentsHolder = AttachmentsHolder()
        val botKeyboardView: BotKeyboardView = itemView.findViewById(R.id.input_keyboard_container)
        val bRestore: Button = itemView.findViewById(R.id.item_message_restore)

        init {
            mAttachmentsHolder.setVgAudios(itemView.findViewById(R.id.audio_attachments))
                .setVgVideos(itemView.findViewById(R.id.video_attachments))
                .setVgDocs(itemView.findViewById(R.id.docs_attachments))
                .setVgArticles(itemView.findViewById(R.id.articles_attachments))
                .setVgPhotos(itemView.findViewById(R.id.photo_attachments))
                .setVgPosts(itemView.findViewById(R.id.posts_attachments))
                .setVgStickers(itemView.findViewById(R.id.stickers_attachments))
        }
    }

    private class StickerMessageHolder(itemView: View) :
        BaseMessageHolder(itemView) {
        val sticker: RLottieImageView = itemView.findViewById(R.id.sticker)
        val attachmentsRoot: View = itemView.findViewById(R.id.item_message_attachment_container)
        val attachmentsHolder: AttachmentsHolder = AttachmentsHolder()
        val forwardMessagesRoot: ViewGroup = itemView.findViewById(R.id.forward_messages)

        init {
            attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
                .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
                .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
                .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
                .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
                .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
                .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments))
        }
    }

    private abstract class BaseMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.message_container)
        val user: EmojiconTextView? = itemView.findViewById(R.id.item_message_user)
        val status: TextView = itemView.findViewById(R.id.item_message_status_text)
        val avatar: ImageView = itemView.findViewById(R.id.item_message_avatar)
        val important: OnlineView = itemView.findViewById(R.id.item_message_important)
        val Restore: Button = itemView.findViewById(R.id.item_message_restore)
    }

    private inner class GiftMessageHolder(itemView: View) :
        BaseMessageHolder(itemView) {
        val gift: ImageView = itemView.findViewById(R.id.gift)
        val message: EmojiconTextView = itemView.findViewById(R.id.item_message_text)

        init {
            message.movementMethod = LinkMovementMethod.getInstance()
            message.setOnHashTagClickListener(onHashTagClickListener)
            message.setOnLongClickListener { this.itemView.performLongClick() }
            message.setOnClickListener { this.itemView.performClick() }
        }
    }

    private inner class MessageHolder(itemView: View) :
        BaseMessageHolder(itemView) {
        val body: EmojiconTextView = itemView.findViewById(R.id.item_message_text)
        val forwardMessagesRoot: ViewGroup
        val bubble: MessageView
        val attachmentsRoot: View
        val attachmentsHolder: AttachmentsHolder
        val encryptedView: View = itemView.findViewById(R.id.item_message_encrypted)
        val botKeyboardView: BotKeyboardView? = itemView.findViewById(R.id.input_keyboard_container)

        init {
            body.movementMethod = LinkMovementMethod.getInstance()
            body.setOnHashTagClickListener(onHashTagClickListener)
            body.setOnLongClickListener { this.itemView.performLongClick() }
            body.setOnClickListener { this.itemView.performClick() }
            forwardMessagesRoot = itemView.findViewById(R.id.forward_messages)
            bubble = itemView.findViewById(R.id.item_message_bubble)
            attachmentsRoot = itemView.findViewById(R.id.item_message_attachment_container)
            attachmentsHolder = AttachmentsHolder()
            attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
                .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
                .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
                .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
                .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
                .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
                .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments))
        }
    }

    companion object {
        private const val TYPE_MY_MESSAGE = 1
        private const val TYPE_FRIEND_MESSAGE = 2
        private const val TYPE_SERVICE = 3
        private const val TYPE_STICKER_MY = 4
        private const val TYPE_STICKER_FRIEND = 5
        private const val TYPE_GIFT_MY = 6
        private const val TYPE_GIFT_FRIEND = 7
        private const val TYPE_GRAFFITI_MY = 8
        private const val TYPE_GRAFFITI_FRIEND = 9
        private val DATE = Date()
    }

    init {
        selectedDrawable.paint.color = CurrentTheme.getColorPrimary(context)
        unreadColor = CurrentTheme.getMessageUnreadColor(context)
        this.disable_read = disable_read
        isNightSticker =
            Settings.get().ui().isStickers_by_theme && Settings.get().ui().isDarkModeEnabled(
                context
            )
    }
}