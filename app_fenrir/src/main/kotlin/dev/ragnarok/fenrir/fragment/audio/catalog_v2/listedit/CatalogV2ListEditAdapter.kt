package dev.ragnarok.fenrir.fragment.audio.catalog_v2.listedit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_CATALOG
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_SERVER_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_PLAYLIST
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_RECOMMENDATIONS

class CatalogV2ListEditAdapter(private var data: List<Int>) :
    RecyclerView.Adapter<CatalogV2ListEditAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_simple_category, parent, false)
        )
    }

    @StringRes
    private fun getTitle(n: Int): Int {
        return when (n) {
            TYPE_CATALOG -> R.string.audio_catalog_v2
            TYPE_LOCAL_AUDIO -> R.string.local_audios
            TYPE_LOCAL_SERVER_AUDIO -> R.string.on_server
            TYPE_AUDIO -> R.string.my_saved
            TYPE_PLAYLIST -> R.string.playlists
            TYPE_RECOMMENDATIONS -> R.string.recommendation
            else -> throw UnsupportedOperationException()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = data[position]
        holder.text.setText(getTitle(category))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Int>) {
        this.data = data
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: MaterialTextView = itemView.findViewById(R.id.item_simple_category_text)
    }
}
