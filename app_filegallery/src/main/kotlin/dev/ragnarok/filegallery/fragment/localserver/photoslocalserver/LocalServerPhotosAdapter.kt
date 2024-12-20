package dev.ragnarok.filegallery.fragment.localserver.photoslocalserver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance.Companion.with
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.filegallery.util.AppTextUtils
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.view.AspectRatioImageView
import dev.ragnarok.filegallery.view.natives.animation.ThorVGLottieView

class LocalServerPhotosAdapter(private val mContext: Context, private var data: List<Photo>) :
    RecyclerView.Adapter<LocalServerPhotosAdapter.ViewHolder>() {
    private val colorPrimary: Int = getColorPrimary(mContext)
    private val mColorSecondaryWithAlpha: Int = Utils.adjustAlpha(
        getColorSecondary(
            mContext
        ), 0.60f
    )
    private var photoSelectionListener: PhotoSelectionListener? = null
    private var currentPosition = -1
    fun updateCurrentPosition(currentPosition: Int) {
        this.currentPosition = currentPosition
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_local_server_photo, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val photo = data[position]
        if (FenrirNative.isNativeLoaded) {
            if (currentPosition == position) {
                viewHolder.current.visibility = View.VISIBLE
                viewHolder.current.fromRes(
                    R.raw.select_fire,
                    intArrayOf(0xFF812E, colorPrimary),
                    true
                )
                viewHolder.current.startAnimation()
            } else {
                viewHolder.current.visibility = View.GONE
                viewHolder.current.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
            }
        }
        val targetUrl = photo.preview_url
        viewHolder.photoImageView.visibility =
            if (targetUrl.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        if (targetUrl.nonNullNoEmpty()) {
            with()
                .load(targetUrl)
                .tag(Constants.PICASSO_TAG)
                .placeholder(R.drawable.background_gray)
                .into(viewHolder.photoImageView)
        } else {
            with().cancelRequest(viewHolder.photoImageView)
        }
        viewHolder.photoImageView.setOnClickListener {
            photoSelectionListener?.onPhotoClicked(viewHolder.bindingAdapterPosition, photo)
        }
        viewHolder.bottomTop.setBackgroundColor(mColorSecondaryWithAlpha)
        viewHolder.tvDate.text = AppTextUtils.getDateFromUnixTimeShorted(mContext, photo.date)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Photo>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setPhotoSelectionListener(photoSelectionListener: PhotoSelectionListener?) {
        this.photoSelectionListener = photoSelectionListener
    }

    interface PhotoSelectionListener {
        fun onPhotoClicked(position: Int, photo: Photo)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: AspectRatioImageView = itemView.findViewById(R.id.imageView)
        val tvDate: TextView = itemView.findViewById(R.id.vk_photo_item_date)
        val bottomTop: ViewGroup = itemView.findViewById(R.id.vk_photo_item_top)
        val current: ThorVGLottieView = itemView.findViewById(R.id.current)
    }

}