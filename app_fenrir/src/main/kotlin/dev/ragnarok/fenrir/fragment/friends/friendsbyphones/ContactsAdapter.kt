package dev.ragnarok.fenrir.fragment.friends.friendsbyphones

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.model.Sex
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.UserInfoResolveUtil
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class ContactsAdapter(private val mContext: Context, private var mData: List<ContactConversation>) :
    RecyclerView.Adapter<ContactsAdapter.ContactHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var mClickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
        return ContactHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_contact, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        val contact = mData[position]
        holder.tvName.text = contact.title
        holder.tvPhone.text = contact.phone
        holder.tvPhone.visibility = if (contact.phone.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (contact.isContact) {
            holder.tvOnline.visibility = View.VISIBLE
            when (contact.last_seen_status) {
                "today" -> holder.tvOnline.setText(R.string.dialog_day_today)
                "recently" -> holder.tvOnline.setText(R.string.recently)
                null -> holder.tvOnline.visibility = View.GONE
                else -> holder.tvOnline.text = contact.last_seen_status
            }
        } else {
            holder.tvOnline.text = UserInfoResolveUtil.getUserActivityLine(
                holder.tvOnline.context,
                contact.lastSeen,
                false,
                Sex.UNKNOWN,
                true
            )
        }
        val url = contact.photo
        if (url.nonNullNoEmpty()) {
            holder.EmptyAvatar.visibility = View.INVISIBLE
            displayAvatar(
                holder.ivAvatar,
                transformation,
                url,
                Constants.PICASSO_TAG
            )
        } else {
            with().cancelRequest(holder.ivAvatar)
            contact.title.ifNonNullNoEmpty({
                holder.EmptyAvatar.visibility = View.VISIBLE
                var name = it
                if (name.length > 2) name = name.substring(0, 2)
                name = name.trim { it1 -> it1 <= ' ' }
                holder.EmptyAvatar.text = name
            }, {
                holder.EmptyAvatar.visibility = View.INVISIBLE
            })
            holder.ivAvatar.setImageBitmap(
                transformation.localTransform(
                    Utils.createGradientChatImage(
                        200,
                        200,
                        contact.id
                    )
                )
            )
        }
        holder.itemView.setOnClickListener {
            mClickListener?.onContactClick(contact)
        }
        holder.itemView.setOnLongClickListener {
            mClickListener?.onContactLongClick(contact) == true
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setItems(data: List<ContactConversation>) {
        mData = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        mClickListener = clickListener
    }

    interface ClickListener {
        fun onContactClick(contact: ContactConversation)
        fun onContactLongClick(contact: ContactConversation): Boolean
    }

    class ContactHolder(root: View) : RecyclerView.ViewHolder(root) {
        val tvName: TextView = root.findViewById(R.id.item_contact_name)
        val tvPhone: TextView = root.findViewById(R.id.item_contact_phone)
        val tvOnline: TextView = root.findViewById(R.id.item_people_online)
        val ivAvatar: ImageView = root.findViewById(R.id.item_contact_avatar)
        val EmptyAvatar: TextView = itemView.findViewById(R.id.empty_avatar_text)
    }
}