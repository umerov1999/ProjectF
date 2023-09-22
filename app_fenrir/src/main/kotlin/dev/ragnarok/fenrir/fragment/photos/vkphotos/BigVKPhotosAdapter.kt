package dev.ragnarok.fenrir.fragment.photos.vkphotos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.holder.IdentificableHolder
import dev.ragnarok.fenrir.fragment.base.holder.SharedHolders
import dev.ragnarok.fenrir.fragment.base.multidata.DifferentDataAdapter
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.CircleRoadProgress
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class BigVKPhotosAdapter(
    private val mContext: Context,
    uploads: List<Upload>,
    photoWrappers: List<SelectablePhotoWrapper>,
    picassoTag: String
) : DifferentDataAdapter() {
    private val mUploadViewHolders: SharedHolders<UploadViewHolder>
    private val mPhotoHolders: MutableSet<PhotoViewHolder>
    private val mColorPrimaryWithAlpha: Int
    private val mColorSecondaryWithAlpha: Int
    private val mPicassoTag: String
    private var isShowBDate = false
    private var mPhotosActionListener: PhotosActionListener? = null
    private var mUploadActionListener: UploadActionListener? = null
    fun setIsShowDate(show: Boolean) {
        isShowBDate = show
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_PHOTO -> return PhotoViewHolder(
                LayoutInflater.from(mContext).inflate(R.layout.vk_photo_item_big, parent, false)
            )

            VIEW_TYPE_UPLOAD -> return UploadViewHolder(
                LayoutInflater.from(mContext).inflate(R.layout.vk_upload_photo_item, parent, false)
            )
        }
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, adapterPosition: Int) {
        when (getItemViewType(adapterPosition)) {
            VIEW_TYPE_PHOTO -> bindPhotoViewHolder(
                holder as PhotoViewHolder,
                getItem(adapterPosition)
            )

            VIEW_TYPE_UPLOAD -> bindUploadViewHolder(
                holder as UploadViewHolder,
                getItem(adapterPosition)
            )
        }
    }

    private fun removePhotoViewHolderByTag(tag: SelectablePhotoWrapper) {
        val iterator = mPhotoHolders.iterator()
        while (iterator.hasNext()) {
            if (tag == iterator.next().itemView.tag) {
                iterator.remove()
            }
        }
    }

    private fun bindUploadViewHolder(holder: UploadViewHolder, upload: Upload) {
        mUploadViewHolders.put(upload.getObjectId(), holder)
        holder.setupProgress(upload.status, upload.progress, false)
        holder.setupTitle(upload.status, upload.progress)
        with()
            .load(upload.fileId?.let { buildUriForPicasso(Content_Local.PHOTO, it) })
            .tag(mPicassoTag)
            .placeholder(R.drawable.background_gray)
            .into(holder.image)
        holder.progressRoot.setOnClickListener {
            mUploadActionListener?.onUploadRemoveClicked(upload)
        }
    }

    private fun bindPhotoViewHolder(holder: PhotoViewHolder, photoWrapper: SelectablePhotoWrapper) {
        removePhotoViewHolderByTag(photoWrapper)
        holder.itemView.tag = photoWrapper
        mPhotoHolders.add(holder)
        Logger.d(TAG, "Added photo view holder, total size: " + mPhotoHolders.size)
        val photo = photoWrapper.photo
        holder.tvLike.text = AppTextUtils.getCounterWithK(photo.likesCount)
        holder.tvLike.visibility = if (photo.likesCount > 0) View.VISIBLE else View.GONE
        holder.ivLike.visibility = if (photo.likesCount > 0) View.VISIBLE else View.GONE
        holder.ivDownload.visibility = if (photoWrapper.isDownloaded) View.VISIBLE else View.GONE
        holder.tvComment.text = AppTextUtils.getCounterWithK(photo.commentsCount)
        holder.tvComment.visibility = if (photo.commentsCount > 0) View.VISIBLE else View.GONE
        holder.ivComment.visibility = if (photo.commentsCount > 0) View.VISIBLE else View.GONE
        holder.bottomRoot.setBackgroundColor(mColorPrimaryWithAlpha)
        holder.bottomRoot.visibility =
            if (photo.likesCount + photo.commentsCount > 0) View.VISIBLE else View.GONE
        if (isShowBDate) {
            holder.bottomTop.visibility = View.VISIBLE
            holder.bottomTop.setBackgroundColor(mColorSecondaryWithAlpha)
            holder.tvDate.text = AppTextUtils.getDateFromUnixTimeShorted(mContext, photo.date)
        } else {
            holder.bottomTop.visibility = View.GONE
        }
        if (Settings.get().main().isNative_parcel_photo && FenrirNative.isNativeLoaded) {
            if (photoWrapper.current) {
                holder.current.visibility = View.VISIBLE
                holder.current.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.donater_fire,
                    Utils.dp(100f),
                    Utils.dp(100f),
                    intArrayOf(0xFF812E, CurrentTheme.getColorPrimary(mContext)),
                    true
                )
                holder.current.playAnimation()
            } else {
                holder.current.visibility = View.GONE
                holder.current.clearAnimationDrawable()
            }
        }
        holder.setSelected(photoWrapper.isSelected)
        holder.resolveIndexText(photoWrapper)
        val targetUrl = photo.getUrlForSize(PhotoSize.Q, false)
        holder.photoImageView.visibility =
            if (targetUrl.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        if (targetUrl.nonNullNoEmpty()) {
            with()
                .load(targetUrl)
                .tag(mPicassoTag)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
        }
        val clickListener = View.OnClickListener {
            mPhotosActionListener?.onPhotoClick(holder, photoWrapper)
        }
        holder.photoImageView.setOnClickListener(clickListener)
        holder.index.setOnClickListener(clickListener)
        holder.darkView.setOnClickListener(clickListener)
    }

    override fun getItemViewType(adapterPosition: Int): Int {
        val dataType = getDataTypeByAdapterPosition(adapterPosition)
        when (dataType) {
            DATA_TYPE_PHOTO -> return VIEW_TYPE_PHOTO
            DATA_TYPE_UPLOAD -> return VIEW_TYPE_UPLOAD
        }
        throw IllegalStateException("Unknown data type, dataType: $dataType")
    }

    fun setPhotosActionListener(photosActionListener: PhotosActionListener?) {
        mPhotosActionListener = photosActionListener
    }

    fun setUploadActionListener(uploadActionListener: UploadActionListener?) {
        mUploadActionListener = uploadActionListener
    }

    fun updatePhotoHoldersSelectionAndIndexes() {
        for (holder in mPhotoHolders) {
            val photo = holder.itemView.tag as SelectablePhotoWrapper
            holder.setSelected(photo.isSelected)
            holder.resolveIndexText(photo)
        }
    }

    fun updateUploadHoldersProgress(uploadId: Int, smoothly: Boolean, progress: Int) {
        val holder = mUploadViewHolders.findOneByEntityId(uploadId)
        if (holder != null) {
            if (smoothly) {
                holder.progress.changePercentageSmoothly(progress)
            } else {
                holder.progress.changePercentage(progress)
            }
            val progressText = "$progress%"
            holder.title.text = progressText
        }
    }

    fun cleanup() {
        mPhotoHolders.clear()
        mUploadViewHolders.release()
    }

    interface PhotosActionListener {
        fun onPhotoClick(holder: PhotoViewHolder, photoWrapper: SelectablePhotoWrapper)
    }

    interface UploadActionListener {
        fun onUploadRemoveClicked(upload: Upload)
    }

    private class UploadViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), IdentificableHolder {
        val image: ImageView
        val progressRoot: View
        val progress: CircleRoadProgress
        val title: TextView
        fun setupProgress(status: Int, progressValue: Int, smoothly: Boolean) {
            if (smoothly && status == Upload.STATUS_UPLOADING) {
                progress.changePercentageSmoothly(progressValue)
            } else {
                progress.visibility =
                    if (status == Upload.STATUS_UPLOADING) View.VISIBLE else View.GONE
                progress.changePercentage(if (status == Upload.STATUS_UPLOADING) progressValue else 0)
            }
        }

        fun setupTitle(status: Int, progress: Int) {
            when (status) {
                Upload.STATUS_QUEUE -> title.setText(R.string.in_order)
                Upload.STATUS_UPLOADING -> {
                    val progressText = "$progress%"
                    title.text = progressText
                }

                Upload.STATUS_ERROR -> title.setText(R.string.error)
                Upload.STATUS_CANCELLING -> title.setText(R.string.cancelling)
            }
        }

        override val holderId: Int
            get() = itemView.tag as Int

        init {
            super.itemView.tag = generateNextHolderId()
            image = itemView.findViewById(R.id.image)
            progressRoot = itemView.findViewById(R.id.progress_root)
            progress = itemView.findViewById(R.id.progress)
            title = itemView.findViewById(R.id.title)
        }
    }

    class PhotoViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.imageView)
        val index: TextView = itemView.findViewById(R.id.item_photo_index)
        val darkView: View = itemView.findViewById(R.id.selected)
        val bottomRoot: ViewGroup = itemView.findViewById(R.id.vk_photo_item_bottom)
        val bottomTop: ViewGroup = itemView.findViewById(R.id.vk_photo_item_top)
        val tvLike: TextView = itemView.findViewById(R.id.vk_photo_item_like_counter)
        val tvDate: TextView = itemView.findViewById(R.id.vk_photo_item_date)
        val tvComment: TextView = itemView.findViewById(R.id.vk_photo_item_comment_counter)
        val ivLike: ImageView = itemView.findViewById(R.id.vk_photo_item_like)
        val ivComment: ImageView = itemView.findViewById(R.id.vk_photo_item_comment)
        val ivDownload: ImageView = itemView.findViewById(R.id.is_downloaded)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
        fun setSelected(selected: Boolean) {
            index.visibility = if (selected) View.VISIBLE else View.GONE
            darkView.visibility = if (selected) View.VISIBLE else View.GONE
        }

        fun resolveIndexText(photo: SelectablePhotoWrapper) {
            index.text = if (photo.index == 0) "" else photo.index.toString()
        }

    }

    companion object {
        const val DATA_TYPE_PHOTO = 1
        const val DATA_TYPE_UPLOAD = 0
        private val TAG = BigVKPhotosAdapter::class.java.simpleName
        private const val VIEW_TYPE_PHOTO = 0
        private const val VIEW_TYPE_UPLOAD = 1
        private var holderIdGenerator = 0
        internal fun generateNextHolderId(): Int {
            holderIdGenerator++
            return holderIdGenerator
        }
    }

    init {
        mPhotoHolders = HashSet()
        mUploadViewHolders = SharedHolders(false)
        mPicassoTag = picassoTag
        mColorPrimaryWithAlpha = Utils.adjustAlpha(CurrentTheme.getColorPrimary(mContext), 0.75f)
        mColorSecondaryWithAlpha =
            Utils.adjustAlpha(CurrentTheme.getColorSecondary(mContext), 0.60f)
        setData(DATA_TYPE_UPLOAD, uploads)
        setData(DATA_TYPE_PHOTO, photoWrappers)
    }
}