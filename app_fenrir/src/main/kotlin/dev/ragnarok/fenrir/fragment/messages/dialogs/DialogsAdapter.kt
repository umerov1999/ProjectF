package dev.ragnarok.fenrir.fragment.messages.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView
import java.text.SimpleDateFormat
import java.util.*

class DialogsAdapter(private val mContext: Context, private var mDialogs: List<Dialog>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val DF_TODAY = SimpleDateFormat("HH:mm", Utils.appLocale)
    private val DF_OLD = SimpleDateFormat("dd/MM", Utils.appLocale)
    private val mTransformation: Transformation = CurrentTheme.createTransformationForAvatar()

    @DrawableRes
    private val mFavoritesAvatar = CurrentTheme.createFavoritesAvatar()
    private val mForegroundColorSpan: ForegroundColorSpan =
        ForegroundColorSpan(CurrentTheme.getPrimaryTextColorCode(mContext))
    private val mDataObserver: AdapterDataObserver
    private val headerInDialog: Boolean = Settings.get().other().isHeaders_in_dialog
    private var showHidden = false
    private var mStartOfToday: Long = 0
    private var mClickListener: ClickListener? = null
    private var accountId = 0L
    fun updateShowHidden(showHidden: Boolean) {
        this.showHidden = showHidden
    }

    internal fun initStartOfTodayDate() {
        // А - Аптемезация
        mStartOfToday = Utils.startOfTodayMillis()
    }

    fun getByPosition(position: Int): Dialog {
        return mDialogs[position]
    }

    fun checkPosition(position: Int): Boolean {
        return position >= 0 && mDialogs.size > position
    }

    fun cleanup() {
        unregisterAdapterDataObserver(mDataObserver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            DATA_TYPE_HIDDEN -> return HiddenViewHolder(
                LayoutInflater.from(mContext).inflate(R.layout.line_hidden, parent, false)
            )
            DATA_TYPE_NORMAL -> return DialogViewHolder(
                LayoutInflater.from(mContext).inflate(R.layout.item_dialog, parent, false)
            )
        }
        throw UnsupportedOperationException()
    }

    private fun getDataTypeByAdapterPosition(adapterPosition: Int): Int {
        return if (Settings.get().security()
                .isHiddenDialog(getByPosition(adapterPosition).getOwnerObjectId()) && !showHidden
        ) {
            DATA_TYPE_HIDDEN
        } else DATA_TYPE_NORMAL
    }

    override fun getItemViewType(adapterPosition: Int): Int {
        return getDataTypeByAdapterPosition(adapterPosition)
    }

    private fun findPreviousUnhidden(pos: Int): Dialog? {
        for (i in pos downTo 0) {
            if (getDataTypeByAdapterPosition(i) == DATA_TYPE_NORMAL) {
                return getByPosition(i)
            }
        }
        return null
    }

    @SuppressLint("SwitchIntDef")
    override fun onBindViewHolder(dualHolder: RecyclerView.ViewHolder, position: Int) {
        if (getDataTypeByAdapterPosition(position) == DATA_TYPE_HIDDEN) {
            return
        }
        val holder = dualHolder as DialogViewHolder
        val dialog = getByPosition(position)
        val previous = if (position == 0) null else findPreviousUnhidden(position - 1)
        var lastMessage: SpannableStringBuilder
        val query = OwnerLinkSpanFactory.withSpans(
            if (dialog.lastMessageBody != null) dialog.lastMessageBody else "",
            owners = true,
            topics = false,
            listener = null
        )
        if (query == null) {
            lastMessage =
                if (dialog.lastMessageBody != null) SpannableStringBuilder.valueOf(dialog.lastMessageBody) else SpannableStringBuilder()
        } else {
            lastMessage = SpannableStringBuilder()
            lastMessage.append(query)
        }
        @MessageType var attachment_message = MessageType.NO
        dialog.message.requireNonNull {
            attachment_message = it.messageTypeByAttachments
        }
        if (attachment_message != MessageType.NO) {
            val type: String = when (attachment_message) {
                MessageType.AUDIO -> mContext.getString(R.string.audio_message)
                MessageType.CALL -> mContext.getString(R.string.call_message)
                MessageType.DOC -> mContext.getString(R.string.doc_message)
                MessageType.GIFT -> mContext.getString(R.string.gift_message)
                MessageType.GRAFFITI -> mContext.getString(R.string.graffiti_message)
                MessageType.PHOTO -> mContext.getString(R.string.photo_message)
                MessageType.STICKER -> mContext.getString(R.string.sticker_message)
                MessageType.VIDEO -> mContext.getString(R.string.video_message)
                MessageType.VOICE -> mContext.getString(R.string.voice_message)
                MessageType.WALL -> mContext.getString(R.string.wall_message)
                else -> mContext.getString(R.string.attachments)
            }
            val spannable = SpannableStringBuilder.valueOf(type)
            spannable.setSpan(
                ForegroundColorSpan(CurrentTheme.getColorPrimary(mContext)),
                0,
                spannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            lastMessage = if (lastMessage.isEmpty()) spannable else lastMessage.append(" ")
                .append(spannable)
        }
        if (dialog.hasForwardMessages()) {
            val spannable =
                SpannableStringBuilder.valueOf(mContext.getString(R.string.forward_messages))
            spannable.setSpan(
                ForegroundColorSpan(CurrentTheme.getColorPrimary(mContext)),
                0,
                spannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            lastMessage = if (lastMessage.isEmpty()) spannable else lastMessage.append(" ")
                .append(spannable)
        }
        val lastMessageAction = dialog.lastMessageAction
        if (lastMessageAction != ChatAction.NO_ACTION) {
            val spannable =
                SpannableStringBuilder.valueOf(mContext.getString(R.string.service_message))
            spannable.setSpan(
                ForegroundColorSpan(CurrentTheme.getColorPrimary(mContext)),
                0,
                spannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            lastMessage = spannable
        }
        if (dialog.isChat) {
            val spannable = SpannableStringBuilder.valueOf(
                if (dialog.isLastMessageOut) mContext.getString(R.string.dialog_me) else dialog.getSenderShortName(
                    mContext
                )
            )
            spannable.setSpan(
                mForegroundColorSpan,
                0,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            lastMessage = spannable.append(": ").append(lastMessage)
        }
        holder.mDialogMessage.text = lastMessage
        val lastMessageRead = dialog.isLastMessageRead
        val titleTextStyle = getTextStyle(dialog.isLastMessageOut, lastMessageRead)
        holder.mDialogTitle.setTypeface(null, titleTextStyle)
        var online = false
        var onlineMobile = false
        @UserPlatform var platform = UserPlatform.UNKNOWN
        var app = 0
        var blacklisted = false
        if (dialog.interlocutor is User) {
            val interlocutor = dialog.interlocutor as User
            holder.mDialogTitle.setTextColor(
                Utils.getVerifiedColor(
                    mContext,
                    interlocutor.isVerified
                )
            )
            online = interlocutor.isOnline
            onlineMobile = interlocutor.isOnlineMobile
            platform = interlocutor.platform
            app = interlocutor.onlineApp
            if (!dialog.isChat) {
                holder.ivVerified.visibility =
                    if (interlocutor.isVerified) View.VISIBLE else View.GONE
                holder.blacklisted.visibility =
                    if (interlocutor.blacklisted) View.VISIBLE else View.GONE
                blacklisted = interlocutor.blacklisted
            } else {
                holder.blacklisted.visibility = View.GONE
                holder.ivVerified.visibility = View.GONE
            }
        } else {
            if (dialog.interlocutor is Community) {
                holder.mDialogTitle.setTextColor(
                    Utils.getVerifiedColor(
                        mContext,
                        dialog.interlocutor?.isVerified == true
                    )
                )
                holder.ivVerified.visibility =
                    if (dialog.interlocutor?.isVerified == true) View.VISIBLE else View.GONE
            } else {
                holder.ivVerified.visibility = View.GONE
                holder.mDialogTitle.setTextColor(Utils.getVerifiedColor(mContext, false))
            }
            holder.blacklisted.visibility = View.GONE
        }
        val iconRes = getOnlineIcon(online, onlineMobile, platform, app)
        holder.ivOnline.setIcon(iconRes ?: 0)
        holder.ivDialogType.setImageResource(if (dialog.isGroupChannel) R.drawable.channel else R.drawable.person_multiple)
        holder.ivDialogType.visibility = if (dialog.isChat) View.VISIBLE else View.GONE
        holder.ivUnreadTicks.visibility = if (dialog.isLastMessageOut) View.VISIBLE else View.GONE
        holder.ivUnreadTicks.setImageResource(if (lastMessageRead) R.drawable.check_all else R.drawable.check)
        holder.silent.visibility = if (Settings.get().notifications()
                .isSilentChat(accountId, dialog.getOwnerObjectId())
        ) View.VISIBLE else View.GONE
        holder.ivOnline.visibility =
            if (online && !dialog.isChat && !dialog.isContact) View.VISIBLE else View.GONE
        val counterVisible = dialog.unreadCount > 0
        holder.tvUnreadCount.text = AppTextUtils.getCounterWithK(dialog.unreadCount)
        holder.tvUnreadCount.visibility = if (counterVisible) View.VISIBLE else View.INVISIBLE
        val lastMessageJavaTime = dialog.lastMessageDate * 1000
        val headerStatus = getDivided(dialog, previous)
        if (headerInDialog) {
            when (headerStatus) {
                DIV_DISABLE, DIV_TODAY -> holder.mHeaderTitle.visibility = View.GONE
                DIV_TODAY_OTHER_PINNED -> {
                    holder.mHeaderTitle.visibility = View.VISIBLE
                    holder.mHeaderTitle.setText(R.string.dialog_day_today)
                }
                DIV_OLD -> {
                    holder.mHeaderTitle.visibility = View.VISIBLE
                    holder.mHeaderTitle.setText(R.string.dialog_day_older)
                }
                DIV_YESTERDAY -> {
                    holder.mHeaderTitle.visibility = View.VISIBLE
                    holder.mHeaderTitle.setText(R.string.dialog_day_yesterday)
                }
                DIV_THIS_WEEK -> {
                    holder.mHeaderTitle.visibility = View.VISIBLE
                    holder.mHeaderTitle.setText(R.string.dialog_day_ten_days)
                }
                DIV_PINNED -> {
                    holder.mHeaderTitle.visibility = View.VISIBLE
                    holder.mHeaderTitle.setText(R.string.dialog_pinned)
                }
            }
        } else {
            holder.mHeaderTitle.visibility = View.GONE
        }
        DATE.time = lastMessageJavaTime
        if (lastMessageJavaTime < mStartOfToday) {
            holder.tvDate.setTextColor(CurrentTheme.getSecondaryTextColorCode(mContext))
            if (getStatus(dialog) == DIV_YESTERDAY || getStatus(dialog) == DIV_PINNED) holder.tvDate.text =
                DF_TODAY.format(
                    DATE
                ) else holder.tvDate.text = DF_OLD.format(DATE)
        } else {
            holder.tvDate.text = DF_TODAY.format(DATE)
            holder.tvDate.setTextColor(CurrentTheme.getColorPrimary(mContext))
        }

        if (accountId != dialog.peerId) {
            holder.mDialogTitle.text = dialog.getDisplayTitle(mContext)
            if (dialog.imageUrl != null) {
                holder.EmptyAvatar.visibility = View.INVISIBLE
                displayAvatar(
                    holder.ivAvatar,
                    mTransformation,
                    dialog.imageUrl,
                    PICASSO_TAG,
                    monochrome = blacklisted
                )
            } else {
                with().cancelRequest(holder.ivAvatar)
                dialog.getDisplayTitle(mContext).ifNonNullNoEmpty({
                    holder.EmptyAvatar.visibility = View.VISIBLE
                    var name = it
                    if (name.length > 2) name = name.substring(0, 2)
                    name = name.trim { it1 -> it1 <= ' ' }
                    holder.EmptyAvatar.text = name
                }, {
                    holder.EmptyAvatar.visibility = View.INVISIBLE
                })
                holder.ivAvatar.setImageBitmap(
                    mTransformation.localTransform(
                        Utils.createGradientChatImage(
                            200,
                            200,
                            dialog.getOwnerObjectId()
                        )
                    )
                )
            }
        } else {
            holder.EmptyAvatar.visibility = View.INVISIBLE
            holder.mDialogTitle.setText(R.string.favorites)
            with().cancelRequest(holder.ivAvatar)
            holder.ivAvatar.setImageResource(mFavoritesAvatar)
        }
        holder.mContentRoot.setOnClickListener {
            mClickListener?.onDialogClick(dialog)
        }
        holder.ivAvatar.setOnClickListener {
            mClickListener?.onAvatarClick(dialog)
        }
        holder.mContentRoot.setOnLongClickListener {
            mClickListener?.onDialogLongClick(
                dialog
            ) == true
        }
    }

    private fun getTextStyle(out: Boolean, read: Boolean): Int {
        return if (read || out) Typeface.NORMAL else Typeface.BOLD
    }

    private fun getDivided(dialog: Dialog, previous: Dialog?): Int {
        val stCurrent = getStatus(dialog)
        return if (previous == null) {
            stCurrent
        } else {
            val stPrevious = getStatus(previous)
            if (stPrevious == DIV_PINNED && stCurrent == DIV_TODAY) {
                DIV_TODAY_OTHER_PINNED
            } else if (stCurrent == stPrevious) {
                DIV_DISABLE
            } else {
                stCurrent
            }
        }
    }

    private fun getStatus(dialog: Dialog): Int {
        val time = dialog.lastMessageDate * 1000
        if (dialog.major_id > 0) {
            return DIV_PINNED
        }
        if (time >= mStartOfToday) {
            return DIV_TODAY
        }
        if (time >= mStartOfToday - 86400000) {
            return DIV_YESTERDAY
        }
        return if (time >= mStartOfToday - 864000000) {
            DIV_THIS_WEEK
        } else DIV_OLD
    }

    fun setClickListener(clickListener: ClickListener?): DialogsAdapter {
        mClickListener = clickListener
        return this
    }

    fun setData(data: List<Dialog>, accountId: Long) {
        mDialogs = data
        this.accountId = accountId
        notifyDataSetChanged()
    }

    fun updateAccount(accountId: Long) {
        this.accountId = accountId
    }

    override fun getItemCount(): Int {
        return mDialogs.size
    }

    interface ClickListener : EventListener {
        fun onDialogClick(dialog: Dialog)
        fun onDialogLongClick(dialog: Dialog): Boolean
        fun onAvatarClick(dialog: Dialog)
    }

    private class HiddenViewHolder(view: View) : RecyclerView.ViewHolder(
        view
    )

    private class DialogViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val mContentRoot: View = view.findViewById(R.id.content_root)
        val mDialogTitle: TextView = view.findViewById(R.id.dialog_title)
        val mDialogMessage: TextView = view.findViewById(R.id.dialog_message)
        val ivDialogType: ImageView = view.findViewById(R.id.dialog_type)
        val ivAvatar: ImageView = view.findViewById(R.id.item_chat_avatar)
        val ivVerified: ImageView = itemView.findViewById(R.id.item_verified)
        val blacklisted: ImageView = itemView.findViewById(R.id.item_blacklisted)
        val silent: ImageView = itemView.findViewById(R.id.dialog_silent)
        val tvUnreadCount: TextView = view.findViewById(R.id.item_chat_unread_count)
        val ivUnreadTicks: ImageView = view.findViewById(R.id.unread_ticks)
        val ivOnline: OnlineView = view.findViewById(R.id.item_chat_online)
        val tvDate: TextView = view.findViewById(R.id.item_chat_date)
        val mHeaderTitle: TextView = view.findViewById(R.id.header_title)
        val EmptyAvatar: TextView = view.findViewById(R.id.empty_avatar_text)

    }

    companion object {
        const val PICASSO_TAG = "dialogs.adapter.tag"
        private val DATE = Date()
        private const val DIV_TODAY_OTHER_PINNED = -2
        private const val DIV_PINNED = -1
        private const val DIV_DISABLE = 0
        private const val DIV_TODAY = 1
        private const val DIV_YESTERDAY = 2
        private const val DIV_THIS_WEEK = 3
        private const val DIV_OLD = 4
        private const val DATA_TYPE_NORMAL = 0
        private const val DATA_TYPE_HIDDEN = 1
    }

    init {
        mDataObserver = object : AdapterDataObserver() {
            override fun onChanged() {
                initStartOfTodayDate()
            }
        }
        registerAdapterDataObserver(mDataObserver)
        initStartOfTodayDate()
    }
}