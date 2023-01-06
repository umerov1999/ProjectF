package dev.ragnarok.fenrir.fragment.comments

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class OwnersListAdapter(context: Activity, private val data: ArrayList<Owner>) :
    ArrayAdapter<Owner>(
        context, R.layout.item_simple_owner, data
    ) {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val transformationWithStory: Transformation =
        CurrentTheme.createTransformationStrokeForAvatar()

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Owner {
        return data[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_simple_owner, parent, false)
            view.tag = ViewHolder(view)
        } else {
            view = convertView
        }
        val holder = view.tag as ViewHolder
        val item = data[position]
        holder.tvName.text = item.fullName
        displayAvatar(
            holder.ivAvatar,
            if (item.isHasUnseenStories) transformationWithStory else transformation,
            item.maxSquareAvatar,
            Constants.PICASSO_TAG
        )
        holder.subtitle.setText(if (item is User) R.string.profile else R.string.community)
        return view
    }

    private class ViewHolder(root: View) {
        val tvName: TextView = root.findViewById(R.id.name)
        val ivAvatar: ImageView = root.findViewById(R.id.avatar)
        val subtitle: TextView = root.findViewById(R.id.subtitle)
    }
}