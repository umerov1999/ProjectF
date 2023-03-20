package dev.ragnarok.fenrir.dialog.privacyview

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.FriendList
import dev.ragnarok.fenrir.model.Privacy
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.Utils

class PrivacyAdapter(private val mContext: Context, private val mPrivacy: Privacy) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mActionListener: ActionListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(mContext)
        when (viewType) {
            TYPE_ENTRY -> return EntryViewHolder(
                inflater.inflate(
                    R.layout.item_privacy_entry,
                    parent,
                    false
                )
            )

            TYPE_TITLE -> return TitleViewHolder(
                inflater.inflate(
                    R.layout.item_privacy_title,
                    parent,
                    false
                )
            )
        }
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TitleViewHolder) {
            bindTitle(holder)
            return
        }
        if (position <= mPrivacy.getAllowedUsers().size) {
            val allowedUserIndex = position - 1
            bindUserEntry(
                holder as EntryViewHolder,
                mPrivacy.getAllowedUsers()[allowedUserIndex],
                true
            )
            return
        }
        if (position <= count(mPrivacy.getAllowedUsers(), mPrivacy.getAllowedLists())) {
            val allowedListIndex = position - count(mPrivacy.getAllowedUsers()) - 1
            bindListEntry(
                holder as EntryViewHolder,
                mPrivacy.getAllowedLists()[allowedListIndex],
                true
            )
            return
        }
        if (position <= count(
                mPrivacy.getAllowedUsers(),
                mPrivacy.getAllowedLists(),
                mPrivacy.getDisallowedUsers()
            ) + 1
        ) {
            val excludedUserIndex =
                position - count(mPrivacy.getAllowedUsers(), mPrivacy.getAllowedLists()) - 2
            bindUserEntry(
                holder as EntryViewHolder,
                mPrivacy.getDisallowedUsers()[excludedUserIndex],
                false
            )
            return
        }
        val excludedListIndex = position - count(
            mPrivacy.getAllowedUsers(),
            mPrivacy.getAllowedLists(),
            mPrivacy.getDisallowedUsers()
        ) - 2
        bindListEntry(
            holder as EntryViewHolder,
            mPrivacy.getDisallowedLists()[excludedListIndex],
            false
        )
    }

    private fun count(vararg collection: Collection<*>?): Int {
        return Utils.safeCountOfMultiple(*collection)
    }

    private fun bindTitle(holder: TitleViewHolder) {
        if (mLayoutManager is StaggeredGridLayoutManager) {
            val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.isFullSpan = true
            holder.itemView.layoutParams = layoutParams
        }
        val position = holder.bindingAdapterPosition
        if (position == 0) {
            val title = mContext.getString(typeTitle)
            val fullText = mContext.getString(R.string.who_can_have_access) + " " + title
            val spannable: Spannable = SpannableStringBuilder.valueOf(fullText)
            val span: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    mActionListener?.onTypeClick()
                }
            }
            spannable.setSpan(
                span,
                fullText.length - title.length,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            holder.title.setText(spannable, TextView.BufferType.SPANNABLE)
        } else {
            holder.title.setText(R.string.who_cannot_have_access)
        }
        holder.buttonAdd.setOnClickListener {
            if (mActionListener != null) {
                if (position == 0) {
                    mActionListener?.onAddToAllowedClick()
                } else {
                    mActionListener?.onAddToDisallowedClick()
                }
            }
        }
    }

    private val typeTitle: Int
        get() = when (mPrivacy.type) {
            Privacy.Type.FRIENDS -> R.string.privacy_to_friends_only
            Privacy.Type.FRIENDS_OF_FRIENDS, Privacy.Type.FRIENDS_OF_FRIENDS_ONLY -> R.string.privacy_to_friends_and_friends_of_friends
            Privacy.Type.ONLY_ME, Privacy.Type.NOBODY -> R.string.privacy_to_only_me
            else -> R.string.privacy_to_all_users
        }

    private fun bindListEntry(holder: EntryViewHolder, friendList: FriendList, allow: Boolean) {
        with().cancelRequest(holder.avatar)
        holder.avatar.setImageResource(R.drawable.ic_privacy_friends_list)
        holder.title.text = friendList.getName()
        holder.buttonRemove.setOnClickListener {
            if (mActionListener != null) {
                if (allow) {
                    mActionListener?.onAllowedFriendsListRemove(friendList)
                } else {
                    mActionListener?.onDisallowedFriendsListRemove(friendList)
                }
            }
        }
    }

    private fun bindUserEntry(holder: EntryViewHolder, user: User, allow: Boolean) {
        with()
            .load(user.maxSquareAvatar)
            .into(holder.avatar)
        holder.title.text = user.fullName
        holder.buttonRemove.setOnClickListener {
            if (mActionListener != null) {
                if (allow) {
                    mActionListener?.onAllowedUserRemove(user)
                } else {
                    mActionListener?.onDisallowedUserRemove(user)
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mLayoutManager = recyclerView.layoutManager
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mLayoutManager = null
    }

    override fun getItemCount(): Int {
        // 2 titles
        return 2 + count(
            mPrivacy.getAllowedUsers(),
            mPrivacy.getAllowedLists(),
            mPrivacy.getDisallowedUsers(),
            mPrivacy.getDisallowedLists()
        )
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TITLE
        }
        return if (position == count(mPrivacy.getAllowedUsers(), mPrivacy.getAllowedLists()) + 1) {
            TYPE_TITLE
        } else TYPE_ENTRY
    }

    fun setActionListener(actionListener: ActionListener?) {
        mActionListener = actionListener
    }

    interface ActionListener {
        fun onTypeClick()
        fun onAllowedUserRemove(user: User)
        fun onAllowedFriendsListRemove(friendList: FriendList)
        fun onDisallowedUserRemove(user: User)
        fun onDisallowedFriendsListRemove(friendList: FriendList)
        fun onAddToAllowedClick()
        fun onAddToDisallowedClick()
    }

    internal class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val buttonAdd: View = itemView.findViewById(R.id.button_add)

        init {
            title.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    internal class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        val buttonRemove: View = itemView.findViewById(R.id.button_remove)
        val title: TextView = itemView.findViewById(R.id.name)
    }

    companion object {
        private const val TYPE_ENTRY = 0
        private const val TYPE_TITLE = 1
    }
}