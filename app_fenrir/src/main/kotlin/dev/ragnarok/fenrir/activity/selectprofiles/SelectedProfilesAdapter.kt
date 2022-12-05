package dev.ragnarok.fenrir.activity.selectprofiles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import java.util.*

class SelectedProfilesAdapter(private val mContext: Context, private val mData: List<Owner>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mTransformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var mActionListener: ActionListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_CHECK -> return CheckViewHolder(
                LayoutInflater.from(mContext)
                    .inflate(R.layout.item_selection_check, parent, false)
            )
            VIEW_TYPE_USER -> return ProfileViewHolder(
                LayoutInflater.from(mContext)
                    .inflate(R.layout.item_selected_user, parent, false)
            )
        }
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            bindCheckViewHolder(holder as CheckViewHolder)
        } else {
            bindProfileViewHolder(holder as ProfileViewHolder, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_CHECK else VIEW_TYPE_USER
    }

    private fun bindCheckViewHolder(holder: CheckViewHolder) {
        if (mData.isEmpty()) {
            holder.counter.setText(R.string.press_plus_for_add)
        } else {
            holder.counter.text = mData.size.toString()
        }
        holder.root.setOnClickListener {
            mActionListener?.onCheckClick()
        }
    }

    private fun bindProfileViewHolder(holder: ProfileViewHolder, adapterPosition: Int) {
        val owner = mData[toDataPosition(adapterPosition)]
        var title: String? = null
        var ava: String? = null
        if (owner is User) {
            title = owner.firstName
            ava = owner.photo50
        } else if (owner is Community) {
            title = owner.fullName
            ava = owner.photo50
        }
        holder.name.text = title
        with()
            .load(ava)
            .transform(mTransformation)
            .into(holder.avatar)
        holder.buttonRemove.setOnClickListener {
            mActionListener?.onClick(holder.bindingAdapterPosition, owner)
        }
    }

    override fun getItemCount(): Int {
        return mData.size + 1
    }

    fun setActionListener(actionListener: ActionListener?) {
        mActionListener = actionListener
    }

    fun toAdapterPosition(dataPosition: Int): Int {
        return dataPosition + 1
    }

    fun toDataPosition(adapterPosition: Int): Int {
        return adapterPosition - 1
    }

    fun notifyHeaderChange() {
        notifyItemChanged(0)
    }

    interface ActionListener : EventListener {
        fun onClick(adapterPosition: Int, owner: Owner)
        fun onCheckClick()
    }

    private class CheckViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val counter: TextView = itemView.findViewById(R.id.counter)
        val root: View = itemView.findViewById(R.id.root)
    }

    private inner class ProfileViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        val name: TextView = itemView.findViewById(R.id.name)
        val buttonRemove: ImageView = itemView.findViewById(R.id.button_remove)

        init {
            buttonRemove.drawable.setTint(CurrentTheme.getColorOnSurface(mContext))
            val root = itemView.findViewById<View>(R.id.root)
            root.background.setTint(CurrentTheme.getMessageBackgroundSquare(mContext))
            //root.getBackground().setAlpha(180);
        }
    }

    companion object {
        private const val VIEW_TYPE_CHECK = 0
        private const val VIEW_TYPE_USER = 1
    }

}