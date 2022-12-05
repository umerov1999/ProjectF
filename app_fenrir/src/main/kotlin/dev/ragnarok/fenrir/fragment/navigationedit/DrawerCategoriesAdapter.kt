package dev.ragnarok.fenrir.fragment.navigationedit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.DrawerCategory

class DrawerCategoriesAdapter(private var data: List<DrawerCategory>) :
    RecyclerView.Adapter<DrawerCategoriesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_drawer_category, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = data[position]
        holder.checkBox.setText(category.getTitleResCategory())
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = category.isActive()
        holder.checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            category.setActive(isChecked)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<DrawerCategory>) {
        this.data = data
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: MaterialCheckBox = itemView.findViewById(R.id.item_drawer_category_check)
    }
}