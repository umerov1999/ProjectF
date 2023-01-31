package dev.ragnarok.fenrir.fragment.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.model.AttachmentsTypes
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forCopyPost
import dev.ragnarok.fenrir.fragment.base.holder.IdentificableHolder
import dev.ragnarok.fenrir.fragment.base.holder.SharedHolders
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadVoice
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.PhotosViewHelper
import dev.ragnarok.fenrir.view.WaveFormView
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs

class AttachmentsViewBinder(
    private val mContext: Context,
    attachmentsActionCallback: OnAttachmentsActionCallback
) {
    private val photosViewHelper: PhotosViewHelper =
        PhotosViewHelper(mContext, attachmentsActionCallback)
    private val mAvatarTransformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val mActiveWaveFormColor: Int
    private val mNoactiveWaveFormColor: Int
    private val mVoiceSharedHolders: SharedHolders<VoiceHolder> = SharedHolders(true)
    private val mAttachmentsActionCallback: OnAttachmentsActionCallback?
    private val isNightSticker: Boolean
    private val expandVoiceTranscript: Boolean
    private var mVoiceActionListener: VoiceActionListener? = null
    private var mOnHashTagClickListener: EmojiconTextView.OnHashTagClickListener? = null
    fun setOnHashTagClickListener(onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?) {
        mOnHashTagClickListener = onHashTagClickListener
    }

    fun displayAttachments(
        attachments: Attachments?,
        containers: AttachmentsHolder,
        postsAsLinks: Boolean,
        messageId: Int?,
        peerId: Long?
    ) {
        if (attachments == null) {
            safeSetVisibitity(containers.vgAudios, View.GONE)
            safeSetVisibitity(containers.vgVideos, View.GONE)
            safeSetVisibitity(containers.vgArticles, View.GONE)
            safeSetVisibitity(containers.vgDocs, View.GONE)
            safeSetVisibitity(containers.vgPhotos, View.GONE)
            safeSetVisibitity(containers.vgPosts, View.GONE)
            safeSetVisibitity(containers.vgStickers, View.GONE)
            safeSetVisibitity(containers.voiceMessageRoot, View.GONE)
            safeSetVisibitity(containers.vgFriends, View.GONE)
            photosViewHelper.removeZoomable(containers.vgPhotos)
            containers.vgAudios?.dispose()
        } else {
            displayArticles(attachments.articles, containers.vgArticles)
            containers.vgAudios?.displayAudios(attachments.audios, mAttachmentsActionCallback)
            displayVoiceMessages(
                attachments.voiceMessages,
                containers.voiceMessageRoot,
                messageId,
                peerId
            )
            displayDocs(attachments.getDocLinks(postsAsLinks, true), containers.vgDocs)
            if (containers.vgStickers != null) {
                displayStickers(attachments.stickers, containers.vgStickers)
            }
            containers.vgPhotos?.let { photosViewHelper.displayPhotos(attachments.postImages, it) }
            containers.vgVideos?.let {
                photosViewHelper.displayVideos(
                    attachments.postImagesVideos,
                    it
                )
            }
        }
    }

    private fun displayVoiceMessages(
        voices: ArrayList<VoiceMessage>?,
        container: ViewGroup?,
        messageId: Int?,
        peerId: Long?
    ) {
        if (voices.isNullOrEmpty() || container == null) {
            container?.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val i = voices.size - container.childCount
        for (j in 0 until i) {
            val itemView =
                LayoutInflater.from(mContext).inflate(R.layout.item_voice_message, container, false)
            val holder = VoiceHolder(itemView)
            itemView.tag = holder
            container.addView(itemView)
        }
        for (g in 0 until container.childCount) {
            val root = container.getChildAt(g) as ViewGroup? ?: continue
            if (g < voices.size) {
                val holder = root.tag as VoiceHolder? ?: continue
                val voice = voices[g]
                bindVoiceHolder(holder, voice, messageId, peerId)
                root.visibility = View.VISIBLE
            } else {
                root.visibility = View.GONE
            }
        }
    }

    fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        amin: Boolean,
        speed: Boolean
    ) {
        val holder = mVoiceSharedHolders.findHolderByHolderId(holderId)
        if (holder != null) {
            bindVoiceHolderPlayState(holder, play, paused, progress, amin, speed)
        }
    }

    private fun bindVoiceHolderPlayState(
        holder: VoiceHolder,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        anim: Boolean,
        speed: Boolean
    ) {
        @DrawableRes val icon = if (play && !paused) R.drawable.pause else R.drawable.play
        holder.mButtonPlay.setImageResource(icon)
        holder.mWaveFormView.setCurrentActiveProgress(if (play) progress else 1.0f, anim)
        Utils.setTint(
            holder.mSpeed,
            if (speed) CurrentTheme.getColorPrimary(mContext) else CurrentTheme.getColorOnSurface(
                mContext
            )
        )
        holder.mSpeed.visibility = if (play) View.VISIBLE else View.GONE
    }

    fun configNowVoiceMessagePlaying(
        voiceMessageId: Int,
        progress: Float,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    ) {
        val holders: SparseArray<MutableSet<WeakReference<VoiceHolder>>> = mVoiceSharedHolders.cache
        for (i in 0 until holders.size()) {
            val key = holders.keyAt(i)
            val play = key == voiceMessageId
            val set = holders[key]
            for (reference in set) {
                val holder = reference.get()
                if (holder != null) {
                    bindVoiceHolderPlayState(holder, play, paused, progress, amin, speed)
                }
            }
        }
    }

    fun setVoiceActionListener(voiceActionListener: VoiceActionListener?) {
        mVoiceActionListener = voiceActionListener
    }

    fun disableVoiceMessagePlaying() {
        val holders: SparseArray<MutableSet<WeakReference<VoiceHolder>>> = mVoiceSharedHolders.cache
        for (i in 0 until holders.size()) {
            val key = holders.keyAt(i)
            val set = holders[key]
            for (reference in set) {
                val holder = reference.get()
                if (holder != null) {
                    bindVoiceHolderPlayState(
                        holder,
                        play = false,
                        paused = false,
                        progress = 0f,
                        anim = false,
                        speed = false
                    )
                }
            }
        }
    }

    private fun bindVoiceHolder(
        holder: VoiceHolder,
        voice: VoiceMessage,
        messageId: Int?,
        peerId: Long?
    ) {
        val voiceMessageId = voice.getId()
        mVoiceSharedHolders.put(voiceMessageId, holder)
        holder.mDurationText.text = AppTextUtils.getDurationString(voice.getDuration())

        // can bee NULL/empty
        voice.getWaveform().ifNonNullNoEmpty({
            holder.mWaveFormView.setWaveForm(it)
        }, {
            holder.mWaveFormView.setWaveForm(DEFAUL_WAVEFORM)
        })
        if (voice.wasListened()) {
            holder.mButtonPlay.background = null
        } else {
            holder.mButtonPlay.setBackgroundResource(R.drawable.spinner)
        }
        if (voice.getTranscript().isNullOrEmpty()) {
            holder.TranscriptText.visibility = View.GONE
            if (messageId == null) {
                holder.mDoTranscript.visibility = View.GONE
            } else {
                holder.mDoTranscript.visibility = View.VISIBLE
                holder.mDoTranscript.setOnClickListener {
                    if (mVoiceActionListener != null) {
                        mVoiceActionListener?.onTranscript(
                            voice.getOwnerId().toString() + "_" + voice.getId(),
                            messageId
                        )
                        holder.mDoTranscript.visibility = View.GONE
                    }
                }
            }
        } else {
            if (voice.isShowTranscript() || expandVoiceTranscript) {
                holder.TranscriptText.visibility = View.VISIBLE
                holder.TranscriptText.text = voice.getTranscript()
                holder.mDoTranscript.visibility = View.GONE
            } else {
                holder.TranscriptText.visibility = View.GONE
                holder.mDoTranscript.visibility = View.VISIBLE
                holder.mDoTranscript.setOnClickListener {
                    voice.setShowTranscript(true)
                    holder.TranscriptText.visibility = View.VISIBLE
                    holder.TranscriptText.text = voice.getTranscript()
                    holder.mDoTranscript.visibility = View.GONE
                }
            }
        }
        holder.mWaveFormView.setOnLongClickListener {
            if (!hasReadWriteStoragePermission(mContext)) {
                mAttachmentsActionCallback?.onRequestWritePermissions()
                return@setOnLongClickListener true
            }
            doDownloadVoice(mContext, voice)
            true
        }
        holder.mButtonPlay.setOnClickListener {
            mVoiceActionListener?.onVoicePlayButtonClick(
                holder.holderId,
                voiceMessageId,
                messageId.orZero(),
                peerId.orZero(),
                voice
            )
        }
        holder.mSpeed.setOnClickListener {
            mVoiceActionListener?.onVoiceTogglePlaybackSpeed()
        }
        mVoiceActionListener?.onVoiceHolderBinded(voiceMessageId, holder.holderId)
    }

    private fun displayStickers(stickers: List<Sticker>?, stickersContainer: ViewGroup?) {
        if (stickers.isNullOrEmpty() || stickersContainer == null) {
            stickersContainer?.visibility = View.GONE
            return
        }
        stickersContainer.visibility = View.VISIBLE
        if (stickersContainer.childCount == 0) {
            val localView = RLottieImageView(mContext)
            stickersContainer.addView(localView)
        }
        val imageView = stickersContainer.getChildAt(0) as RLottieImageView? ?: return
        val sticker = stickers[0]
        val prefferedStickerSize = Utils.dpToPx(PREFFERED_STICKER_SIZE.toFloat(), mContext)
            .toInt()
        val image = sticker.getImage(256, isNightSticker)
        val horisontal = image.height.orZero() < image.width.orZero()
        val proporsion = image.width.orZero().toDouble() / image.height.toDouble()
        val finalWidth: Float
        val finalHeihgt: Float
        if (horisontal) {
            finalWidth = prefferedStickerSize.toFloat()
            finalHeihgt = (finalWidth / proporsion).toFloat()
        } else {
            finalHeihgt = prefferedStickerSize.toFloat()
            finalWidth = (finalHeihgt * proporsion).toFloat()
        }
        imageView.layoutParams.height = finalHeihgt.toInt()
        imageView.layoutParams.width = finalWidth.toInt()
        if (sticker.isAnimated) {
            imageView.fromNet(
                sticker.getAnimationByType(if (isNightSticker) "dark" else "light"),
                Utils.createOkHttp(5, true),
                finalWidth.toInt(),
                finalHeihgt.toInt()
            )
            stickersContainer.setOnLongClickListener {
                imageView.replayAnimation()
                true
            }
        } else {
            with()
                .load(image.url)
                .tag(Constants.PICASSO_TAG)
                .into(imageView)
        }
    }

    fun displayCopyHistory(
        posts: List<Post>?,
        container: ViewGroup?,
        reduce: Boolean,
        layout: Int
    ) {
        if (posts.isNullOrEmpty() || container == null) {
            container?.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val i = posts.size - container.childCount
        for (j in 0 until i) {
            val itemView = LayoutInflater.from(container.context).inflate(layout, container, false)
            val holder = CopyHolder(itemView as ViewGroup, mAttachmentsActionCallback)
            itemView.setTag(holder)
            if (!reduce) {
                holder.bodyView.autoLinkMask = Linkify.WEB_URLS
                holder.bodyView.movementMethod = LinkMovementMethod.getInstance()
            }
            container.addView(itemView)
        }
        for (g in 0 until container.childCount) {
            val postViewGroup = container.getChildAt(g) as ViewGroup? ?: continue
            if (g < posts.size) {
                val check = postViewGroup.tag as CopyHolder? ?: continue
                val copy = posts[g]
                postViewGroup.visibility = View.VISIBLE
                val text =
                    if (reduce) AppTextUtils.reduceStringForPost(copy.text) else copy.text
                check.bodyView.visibility =
                    if (copy.text.isNullOrEmpty()) View.GONE else View.VISIBLE
                check.bodyView.setOnHashTagClickListener(mOnHashTagClickListener)
                check.bodyView.text =
                    OwnerLinkSpanFactory.withSpans(
                        text,
                        owners = true,
                        topics = false,
                        listener = object : LinkActionAdapter() {
                            override fun onOwnerClick(ownerId: Long) {
                                mAttachmentsActionCallback?.onOpenOwner(ownerId)
                            }
                        })
                check.ivAvatar.setOnClickListener {
                    mAttachmentsActionCallback?.onOpenOwner(
                        copy.authorId
                    )
                }
                displayAvatar(
                    check.ivAvatar,
                    mAvatarTransformation,
                    copy.authorPhoto,
                    Constants.PICASSO_TAG
                )
                check.tvShowMore.visibility = if (reduce && Utils.safeLenghtOf(
                        copy.text
                    ) > 400
                ) View.VISIBLE else View.GONE
                check.ownerName.text = copy.authorName
                check.buttonDots.tag = copy
                displayAttachments(copy.attachments, check.attachmentsHolder, false, null, null)
            } else {
                postViewGroup.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun displayFriendsPost(users: List<User>?, container: ViewGroup?, layout: Int) {
        if (users.isNullOrEmpty() || container == null) {
            container?.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val i = users.size - container.childCount
        for (j in 0 until i) {
            val itemView = LayoutInflater.from(container.context).inflate(layout, container, false)
            val holder = FriendsPostViewHolder(itemView, mAttachmentsActionCallback)
            itemView.tag = holder
            container.addView(itemView)
        }
        for (g in 0 until container.childCount) {
            val postViewGroup = container.getChildAt(g) as ViewGroup? ?: continue
            if (g < users.size) {
                val holder = postViewGroup.tag as FriendsPostViewHolder? ?: continue
                val user = users[g]
                postViewGroup.visibility = View.VISIBLE
                if (user.fullName.isEmpty()) holder.tvTitle.visibility =
                    View.INVISIBLE else {
                    holder.tvTitle.visibility = View.VISIBLE
                    holder.tvTitle.text = user.fullName
                }
                if (user.domain.isNullOrEmpty()) holder.tvDescription.visibility =
                    View.INVISIBLE else {
                    holder.tvDescription.visibility = View.VISIBLE
                    holder.tvDescription.text = "@" + user.domain
                }
                val imageUrl = user.maxSquareAvatar
                if (imageUrl != null) {
                    displayAvatar(
                        holder.ivImage,
                        mAvatarTransformation,
                        imageUrl,
                        Constants.PICASSO_TAG
                    )
                } else {
                    with().cancelRequest(holder.ivImage)
                    holder.ivImage.setImageResource(R.drawable.ic_avatar_unknown)
                }
                holder.ivImage.setOnClickListener {
                    mAttachmentsActionCallback?.onOpenOwner(
                        user.ownerId
                    )
                }
            } else {
                postViewGroup.visibility = View.GONE
            }
        }
    }

    fun displayForwards(
        fwds: List<Message>?,
        fwdContainer: ViewGroup?,
        postsAsLinks: Boolean
    ) {
        if (fwds.isNullOrEmpty() || fwdContainer == null) {
            fwdContainer?.visibility = View.GONE
            return
        }
        fwdContainer.visibility = View.VISIBLE
        val i = fwds.size - fwdContainer.childCount
        for (j in 0 until i) {
            val localView = LayoutInflater.from(fwdContainer.context)
                .inflate(R.layout.item_forward_message, fwdContainer, false)
            fwdContainer.addView(localView)
        }
        for (g in 0 until fwdContainer.childCount) {
            val itemView = fwdContainer.getChildAt(g) as ViewGroup? ?: continue
            if (g < fwds.size) {
                val message = fwds[g]
                itemView.visibility = View.VISIBLE
                itemView.tag = null
                if (Settings.get().other().isDeveloper_mode) {
                    itemView.findViewById<View>(R.id.item_message_bubble)
                        .setOnLongClickListener {
                            mAttachmentsActionCallback?.onGoToMessagesLookup(message)
                            true
                        }
                }
                val tvBody = itemView.findViewById<TextView>(R.id.item_fwd_message_text)
                tvBody.text = OwnerLinkSpanFactory.withSpans(
                    if (message.cryptStatus == CryptStatus.DECRYPTED) message.decryptedBody else message.body,
                    owners = true,
                    topics = false,
                    listener = object : LinkActionAdapter() {
                        override fun onOwnerClick(ownerId: Long) {
                            mAttachmentsActionCallback?.onOpenOwner(ownerId)
                        }
                    })
                tvBody.visibility =
                    if (message.body.isNullOrEmpty()) View.GONE else View.VISIBLE
                (itemView.findViewById<View>(R.id.item_fwd_message_username) as TextView).text =
                    message.sender?.fullName
                (itemView.findViewById<View>(R.id.item_fwd_message_time) as TextView).text =
                    AppTextUtils.getDateFromUnixTime(message.date)
                val tvFwds: MaterialButton = itemView.findViewById(R.id.item_forward_message_fwds)
                tvFwds.visibility =
                    if (message.forwardMessagesCount > 0) View.VISIBLE else View.GONE
                tvFwds.setOnClickListener {
                    message.fwd?.let { it1 -> mAttachmentsActionCallback?.onForwardMessagesOpen(it1) }
                }
                val ivAvatar = itemView.findViewById<ImageView>(R.id.item_fwd_message_avatar)
                displayAvatar(
                    ivAvatar,
                    mAvatarTransformation,
                    message.sender?.maxSquareAvatar,
                    Constants.PICASSO_TAG
                )
                ivAvatar.setOnClickListener {
                    mAttachmentsActionCallback?.onOpenOwner(
                        message.senderId
                    )
                }
                val attachmentContainers = AttachmentsHolder()
                attachmentContainers.setVgAudios(itemView.findViewById(R.id.audio_attachments))
                    .setVgVideos(itemView.findViewById(R.id.video_attachments))
                    .setVgDocs(itemView.findViewById(R.id.docs_attachments))
                    .setVgArticles(itemView.findViewById(R.id.articles_attachments))
                    .setVgPhotos(itemView.findViewById(R.id.photo_attachments))
                    .setVgPosts(itemView.findViewById(R.id.posts_attachments))
                    .setVgStickers(itemView.findViewById(R.id.stickers_attachments))
                    .setVoiceMessageRoot(itemView.findViewById(R.id.voice_message_attachments))
                displayAttachments(
                    message.attachments,
                    attachmentContainers,
                    postsAsLinks,
                    message.getObjectId(),
                    message.peerId
                )
            } else {
                itemView.visibility = View.GONE
                itemView.tag = null
            }
        }
    }

    private fun displayDocs(docs: List<DocLink>?, root: ViewGroup?) {
        if (docs.isNullOrEmpty() || root == null) {
            root?.visibility = View.GONE
            return
        }
        root.visibility = View.VISIBLE
        val i = docs.size - root.childCount
        for (j in 0 until i) {
            root.addView(LayoutInflater.from(mContext).inflate(R.layout.item_document, root, false))
        }
        for (g in 0 until root.childCount) {
            val itemView = root.getChildAt(g) as ViewGroup? ?: continue
            if (g < docs.size) {
                val doc = docs[g]
                itemView.visibility = View.VISIBLE
                itemView.tag = null
                val tvTitle = itemView.findViewById<TextView>(R.id.item_document_title)
                val tvDetails = itemView.findViewById<TextView>(R.id.item_document_ext_size)
                val tvPostText: EmojiconTextView = itemView.findViewById(R.id.item_message_text)
                val ivPhotoT: ShapeableImageView = itemView.findViewById(R.id.item_document_image)
                val ivGraffiti = itemView.findViewById<ImageView>(R.id.item_document_graffiti)
                val ivPhoto_Post = itemView.findViewById<ImageView>(R.id.item_post_avatar_image)
                val ivType = itemView.findViewById<ImageView>(R.id.item_document_type)
                val tvShowMore = itemView.findViewById<TextView>(R.id.item_post_show_more)
                val title = doc.getTitle(mContext)
                val details = doc.getSecondaryText(mContext)
                val imageUrl = doc.imageUrl
                val ext = if (doc.getExt(mContext) == null) "" else doc.getExt(mContext) + ", "
                val subtitle =
                    Utils.firstNonEmptyString(ext, " ") + Utils.firstNonEmptyString(details, " ")
                if (title.isNullOrEmpty()) {
                    tvTitle.visibility = View.GONE
                } else {
                    tvTitle.text = title
                    tvTitle.visibility = View.VISIBLE
                }
                if (doc.type == AttachmentsTypes.POST) {
                    tvShowMore.visibility = if (subtitle.length > 400) View.VISIBLE else View.GONE
                    tvDetails.visibility = View.GONE
                    tvPostText.visibility = View.VISIBLE
                    tvPostText.text = OwnerLinkSpanFactory.withSpans(
                        AppTextUtils.reduceStringForPost(subtitle),
                        owners = true,
                        topics = false,
                        listener = object : LinkActionAdapter() {
                            override fun onOwnerClick(ownerId: Long) {
                                mAttachmentsActionCallback?.onOpenOwner(ownerId)
                            }
                        })
                } else if (doc.type == AttachmentsTypes.WALL_REPLY) {
                    tvShowMore.visibility = if (subtitle.length > 400) View.VISIBLE else View.GONE
                    tvDetails.visibility = View.GONE
                    tvPostText.visibility = View.VISIBLE
                    tvPostText.text = OwnerLinkSpanFactory.withSpans(
                        AppTextUtils.reduceStringForPost(subtitle),
                        owners = true,
                        topics = false,
                        listener = object : LinkActionAdapter() {
                            override fun onOwnerClick(ownerId: Long) {
                                mAttachmentsActionCallback?.onOpenOwner(ownerId)
                            }
                        })
                } else if (doc.type == AttachmentsTypes.EVENT) {
                    tvShowMore.visibility = View.GONE
                    tvDetails.visibility = View.GONE
                    tvPostText.visibility = View.VISIBLE
                    tvPostText.text = OwnerLinkSpanFactory.withSpans(
                        AppTextUtils.reduceStringForPost(subtitle),
                        owners = true,
                        topics = false,
                        listener = object : LinkActionAdapter() {
                            override fun onOwnerClick(ownerId: Long) {
                                mAttachmentsActionCallback?.onOpenOwner(ownerId)
                            }
                        })
                } else if (doc.type == AttachmentsTypes.NOT_SUPPORTED) {
                    tvShowMore.visibility = View.GONE
                    tvDetails.visibility = View.GONE
                    tvPostText.visibility = View.VISIBLE
                    tvPostText.text = AppTextUtils.reduceStringForPost(subtitle)
                } else {
                    tvDetails.visibility = View.VISIBLE
                    tvPostText.visibility = View.GONE
                    tvShowMore.visibility = View.GONE
                    if (subtitle.isEmpty()) {
                        tvDetails.visibility = View.GONE
                    } else {
                        tvDetails.text = subtitle
                        tvDetails.visibility = View.VISIBLE
                    }
                }
                val attachmentsRoot =
                    itemView.findViewById<View>(R.id.item_message_attachment_container)
                val attachmentsHolder = AttachmentsHolder()
                attachmentsHolder.setVgAudios(attachmentsRoot.findViewById(R.id.audio_attachments))
                    .setVgVideos(attachmentsRoot.findViewById(R.id.video_attachments))
                    .setVgDocs(attachmentsRoot.findViewById(R.id.docs_attachments))
                    .setVgArticles(attachmentsRoot.findViewById(R.id.articles_attachments))
                    .setVgPhotos(attachmentsRoot.findViewById(R.id.photo_attachments))
                    .setVgPosts(attachmentsRoot.findViewById(R.id.posts_attachments))
                    .setVoiceMessageRoot(attachmentsRoot.findViewById(R.id.voice_message_attachments))
                attachmentsRoot.visibility = View.GONE
                itemView.setOnClickListener { openDocLink(doc) }
                ivPhoto_Post.visibility = View.GONE
                ivGraffiti.visibility = View.GONE
                when (doc.type) {
                    AttachmentsTypes.DOC -> if (imageUrl != null) {
                        ivType.visibility = View.GONE
                        ivPhotoT.visibility = View.VISIBLE
                        displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                    } else {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.GONE
                        ivType.setImageResource(R.drawable.file)
                    }

                    AttachmentsTypes.GRAFFITI -> {
                        ivPhotoT.visibility = View.GONE
                        if (imageUrl != null) {
                            ivType.visibility = View.GONE
                            ivGraffiti.visibility = View.VISIBLE
                            displayAvatar(ivGraffiti, null, imageUrl, Constants.PICASSO_TAG)
                        } else {
                            ivType.visibility = View.VISIBLE
                            ivType.setImageResource(R.drawable.counter)
                        }
                    }

                    AttachmentsTypes.AUDIO_PLAYLIST -> if (imageUrl != null) {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.VISIBLE
                        displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                        ivType.setImageResource(R.drawable.audio_player)
                    } else {
                        ivPhotoT.visibility = View.GONE
                    }

                    AttachmentsTypes.ALBUM -> if (imageUrl != null) {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.VISIBLE
                        displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                        ivType.setImageResource(R.drawable.album_photo)
                    } else {
                        ivPhotoT.visibility = View.GONE
                    }

                    AttachmentsTypes.MARKET_ALBUM -> if (imageUrl != null) {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.VISIBLE
                        displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                        ivType.setImageResource(R.drawable.ic_market_stack)
                    } else {
                        ivPhotoT.visibility = View.GONE
                    }

                    AttachmentsTypes.ARTIST -> if (imageUrl != null) {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.VISIBLE
                        displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                        ivType.setImageResource(R.drawable.artist_icon)
                    } else {
                        ivPhotoT.visibility = View.GONE
                    }

                    AttachmentsTypes.MARKET -> if (imageUrl != null) {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.VISIBLE
                        displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                        ivType.setImageResource(R.drawable.ic_market_outline)
                    } else {
                        ivPhotoT.visibility = View.GONE
                    }

                    AttachmentsTypes.STORY -> {
                        ivPhotoT.visibility = View.GONE
                        ivType.visibility = View.GONE
                        if (imageUrl != null) {
                            ivPhoto_Post.visibility = View.VISIBLE
                            displayAvatar(
                                ivPhoto_Post,
                                mAvatarTransformation,
                                imageUrl,
                                Constants.PICASSO_TAG
                            )
                        } else {
                            ivPhoto_Post.visibility = View.GONE
                        }
                        val st = doc.attachment as Story
                        val prw = when {
                            st.photo != null -> st.photo?.getUrlForSize(
                                PhotoSize.X,
                                true
                            )

                            st.video != null -> st.video?.image
                            else -> null
                        }
                        if (prw != null) {
                            ivPhotoT.visibility = View.VISIBLE
                            displayAvatar(ivPhotoT, null, prw, Constants.PICASSO_TAG)
                        } else {
                            ivPhotoT.visibility = View.GONE
                        }
                    }

                    AttachmentsTypes.POST -> {
                        ivPhotoT.visibility = View.GONE
                        ivType.visibility = View.GONE
                        if (imageUrl != null) {
                            ivPhoto_Post.visibility = View.VISIBLE
                            displayAvatar(
                                ivPhoto_Post,
                                mAvatarTransformation,
                                imageUrl,
                                Constants.PICASSO_TAG
                            )
                        } else {
                            ivPhoto_Post.visibility = View.GONE
                        }
                        val post = doc.attachment as Post
                        val hasAttachments = post.attachments?.hasAttachments == true
                        attachmentsRoot.visibility = if (hasAttachments) View.VISIBLE else View.GONE
                        if (hasAttachments) displayAttachments(
                            post.attachments,
                            attachmentsHolder,
                            false,
                            null, null
                        )
                    }

                    AttachmentsTypes.WALL_REPLY -> {
                        ivPhotoT.visibility = View.GONE
                        ivType.visibility = View.VISIBLE
                        ivType.setImageResource(R.drawable.comment)
                        if (imageUrl != null) {
                            ivPhoto_Post.visibility = View.VISIBLE
                            displayAvatar(
                                ivPhoto_Post,
                                mAvatarTransformation,
                                imageUrl,
                                Constants.PICASSO_TAG
                            )
                        } else {
                            ivPhoto_Post.visibility = View.GONE
                        }
                        val comment = doc.attachment as WallReply
                        val hasCommentAttachments = comment.attachments?.hasAttachments == true
                        attachmentsRoot.visibility =
                            if (hasCommentAttachments) View.VISIBLE else View.GONE
                        if (hasCommentAttachments) displayAttachments(
                            comment.attachments,
                            attachmentsHolder,
                            false,
                            null, null
                        )
                    }

                    AttachmentsTypes.EVENT -> {
                        ivPhotoT.visibility = View.GONE
                        ivType.visibility = View.VISIBLE
                        ivType.setImageResource(R.drawable.feed)
                        if (imageUrl != null) {
                            ivPhoto_Post.visibility = View.VISIBLE
                            displayAvatar(
                                ivPhoto_Post,
                                mAvatarTransformation,
                                imageUrl,
                                Constants.PICASSO_TAG
                            )
                        } else {
                            ivPhoto_Post.visibility = View.GONE
                        }
                    }

                    AttachmentsTypes.LINK, AttachmentsTypes.WIKI_PAGE -> {
                        ivType.visibility = View.VISIBLE
                        if (imageUrl != null) {
                            ivPhotoT.visibility = View.VISIBLE
                            displayAvatar(ivPhotoT, null, imageUrl, Constants.PICASSO_TAG)
                        } else {
                            ivPhotoT.visibility = View.GONE
                        }
                        ivType.setImageResource(R.drawable.attachment)
                    }

                    AttachmentsTypes.NOT_SUPPORTED -> {
                        ivType.visibility = View.GONE
                        ivPhotoT.visibility = View.VISIBLE
                        ivPhotoT.setImageResource(R.drawable.not_supported)
                    }

                    AttachmentsTypes.POLL -> {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.GONE
                        ivType.setImageResource(R.drawable.chart_bar)
                    }

                    AttachmentsTypes.CALL -> {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.GONE
                        ivType.setImageResource(R.drawable.phone_call)
                    }

                    AttachmentsTypes.GEO -> {
                        ivType.visibility = View.VISIBLE
                        ivPhotoT.visibility = View.GONE
                        ivType.setImageResource(R.drawable.geo)
                    }

                    else -> {
                        ivType.visibility = View.GONE
                        ivPhotoT.visibility = View.GONE
                    }
                }
            } else {
                itemView.visibility = View.GONE
                itemView.tag = null
            }
        }
    }

    private fun displayArticles(articles: List<Article>?, root: ViewGroup?) {
        if (articles.isNullOrEmpty() || root == null) {
            root?.visibility = View.GONE
            return
        }
        root.visibility = View.VISIBLE
        val i = articles.size - root.childCount
        for (j in 0 until i) {
            root.addView(LayoutInflater.from(mContext).inflate(R.layout.item_article, root, false))
        }
        for (g in 0 until root.childCount) {
            val itemView = root.getChildAt(g) as ViewGroup? ?: continue
            if (g < articles.size) {
                val article = articles[g]
                itemView.visibility = View.VISIBLE
                itemView.tag = null
                val ivPhoto = itemView.findViewById<ImageView>(R.id.item_article_image)
                val ivSubTitle = itemView.findViewById<TextView>(R.id.item_article_subtitle)
                val ivTitle = itemView.findViewById<TextView>(R.id.item_article_title)
                val ivName = itemView.findViewById<TextView>(R.id.item_article_name)
                val btFave = itemView.findViewById<ImageView>(R.id.item_article_to_fave)
                val btShare = itemView.findViewById<ImageView>(R.id.item_article_share)
                val ivButton = itemView.findViewById<Button>(R.id.item_article_read)
                if (article.uRL != null) {
                    btFave.visibility = View.VISIBLE
                    btFave.setImageResource(if (article.isFavorite) R.drawable.favorite else R.drawable.star)
                    btFave.setOnClickListener {
                        mAttachmentsActionCallback?.onFaveArticle(article)
                        article.setIsFavorite(!article.isFavorite)
                        btFave.setImageResource(if (article.isFavorite) R.drawable.favorite else R.drawable.star)
                    }
                    ivButton.visibility = View.VISIBLE
                    ivButton.setOnClickListener {
                        article.uRL.nonNullNoEmpty {
                            mAttachmentsActionCallback?.onUrlOpen(it)
                        }
                    }
                } else {
                    ivButton.visibility = View.GONE
                    btFave.visibility = View.GONE
                }
                btShare.setOnClickListener {
                    mAttachmentsActionCallback?.onShareArticle(
                        article
                    )
                }
                var photo_url: String? = null
                if (article.photo != null) {
                    photo_url = article.photo?.getUrlForSize(
                        Settings.get().main().prefPreviewImageSize,
                        false
                    )
                }
                if (photo_url != null) {
                    ivPhoto.visibility = View.VISIBLE
                    displayAvatar(ivPhoto, null, photo_url, Constants.PICASSO_TAG)
                    ivPhoto.setOnLongClickListener {
                        article.photo?.let {
                            val temp = ArrayList(listOf(it))
                            mAttachmentsActionCallback?.onPhotosOpen(temp, 0, false)
                        }
                        true
                    }
                } else ivPhoto.visibility = View.GONE
                if (article.subTitle != null) {
                    ivSubTitle.visibility = View.VISIBLE
                    ivSubTitle.text = article.subTitle
                } else ivSubTitle.visibility = View.GONE
                if (article.title != null) {
                    ivTitle.visibility = View.VISIBLE
                    ivTitle.text = article.title
                } else ivTitle.visibility = View.GONE
                if (article.ownerName != null) {
                    ivName.visibility = View.VISIBLE
                    ivName.text = article.ownerName
                } else ivName.visibility = View.GONE
            } else {
                itemView.visibility = View.GONE
                itemView.tag = null
            }
        }
    }

    private fun openDocLink(link: DocLink) {
        when (link.type) {
            AttachmentsTypes.DOC -> mAttachmentsActionCallback?.onDocPreviewOpen(
                link.attachment as Document
            )

            AttachmentsTypes.POST -> mAttachmentsActionCallback?.onPostOpen(link.attachment as Post)
            AttachmentsTypes.LINK -> mAttachmentsActionCallback?.onLinkOpen(link.attachment as Link)
            AttachmentsTypes.POLL -> mAttachmentsActionCallback?.onPollOpen(link.attachment as Poll)
            AttachmentsTypes.WIKI_PAGE -> mAttachmentsActionCallback?.onWikiPageOpen(
                link.attachment as WikiPage
            )

            AttachmentsTypes.STORY -> mAttachmentsActionCallback?.onStoryOpen(link.attachment as Story)
            AttachmentsTypes.AUDIO_PLAYLIST -> mAttachmentsActionCallback?.onAudioPlaylistOpen(
                link.attachment as AudioPlaylist
            )

            AttachmentsTypes.WALL_REPLY -> mAttachmentsActionCallback?.onWallReplyOpen(
                link.attachment as WallReply
            )

            AttachmentsTypes.EVENT -> mAttachmentsActionCallback?.onOpenOwner(-1 * abs((link.attachment as Event).id))
            AttachmentsTypes.ALBUM -> mAttachmentsActionCallback?.onPhotoAlbumOpen(
                link.attachment as PhotoAlbum
            )

            AttachmentsTypes.MARKET_ALBUM -> mAttachmentsActionCallback?.onMarketAlbumOpen(
                link.attachment as MarketAlbum
            )

            AttachmentsTypes.MARKET -> mAttachmentsActionCallback?.onMarketOpen(link.attachment as Market)
            AttachmentsTypes.ARTIST -> mAttachmentsActionCallback?.onArtistOpen(link.attachment as AudioArtist)
            AttachmentsTypes.NOT_SUPPORTED -> {
                val clipboard =
                    mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip =
                    ClipData.newPlainText("response", (link.attachment as NotSupported).getBody())
                clipboard?.setPrimaryClip(clip)
                CustomToast.createCustomToast(mContext).showToast(R.string.copied)
            }

            AttachmentsTypes.GEO -> {
                val geo = link.attachment as Geo
                if (geo.latitude.nonNullNoEmpty() && geo.longitude.nonNullNoEmpty() && mContext is Activity) {
                    LinkHelper.openUrl(
                        mContext,
                        Settings.get().accounts().current,
                        "https://www.google.com/maps?q=${geo.latitude},${geo.longitude}",
                        false
                    )
                }
            }
        }
    }

    interface VoiceActionListener : EventListener {
        fun onVoiceHolderBinded(voiceMessageId: Int, voiceHolderId: Int)
        fun onVoicePlayButtonClick(
            voiceHolderId: Int,
            voiceMessageId: Int,
            messageId: Int,
            peerId: Long,
            voiceMessage: VoiceMessage
        )

        fun onVoiceTogglePlaybackSpeed()
        fun onTranscript(voiceMessageId: String, messageId: Int)
    }

    interface OnAttachmentsActionCallback {
        fun onPollOpen(poll: Poll)
        fun onVideoPlay(video: Video)
        fun onAudioPlay(position: Int, audios: ArrayList<Audio>)
        fun onForwardMessagesOpen(messages: ArrayList<Message>)
        fun onOpenOwner(ownerId: Long)
        fun onGoToMessagesLookup(message: Message)
        fun onDocPreviewOpen(document: Document)
        fun onPostOpen(post: Post)
        fun onLinkOpen(link: Link)
        fun onUrlOpen(url: String)
        fun onFaveArticle(article: Article)
        fun onShareArticle(article: Article)
        fun onWikiPageOpen(page: WikiPage)
        fun onPhotosOpen(photos: ArrayList<Photo>, index: Int, refresh: Boolean)
        fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String)
        fun onStoryOpen(story: Story)
        fun onWallReplyOpen(reply: WallReply)
        fun onAudioPlaylistOpen(playlist: AudioPlaylist)
        fun onPhotoAlbumOpen(album: PhotoAlbum)
        fun onMarketAlbumOpen(market_album: MarketAlbum)
        fun onMarketOpen(market: Market)
        fun onArtistOpen(artist: AudioArtist)
        fun onRequestWritePermissions()
    }

    private class CopyHolder(
        val itemView: ViewGroup,
        val callback: OnAttachmentsActionCallback?
    ) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.item_copy_history_post_avatar)
        val ownerName: TextView = itemView.findViewById(R.id.item_post_copy_owner_name)
        val bodyView: EmojiconTextView = itemView.findViewById(R.id.item_post_copy_text)
        val tvShowMore: View = itemView.findViewById(R.id.item_post_copy_show_more)
        val buttonDots: View = itemView.findViewById(R.id.item_copy_history_post_dots)
        val attachmentsHolder: AttachmentsHolder = forCopyPost(itemView)
        fun showDotsMenu() {
            val menu = PopupMenu(itemView.context, buttonDots)
            menu.menu.add(R.string.open_post).setOnMenuItemClickListener {
                val copy = buttonDots.tag as Post
                callback?.onPostOpen(copy)
                true
            }
            menu.show()
        }

        init {
            buttonDots.setOnClickListener { showDotsMenu() }
        }
    }

    private class FriendsPostViewHolder(root: View, callback: OnAttachmentsActionCallback?) :
        IdentificableHolder {
        val callback: OnAttachmentsActionCallback?
        val ivImage: ImageView = root.findViewById(R.id.item_link_pic)
        val tvTitle: TextView = root.findViewById(R.id.item_link_name)
        val tvDescription: TextView = root.findViewById(R.id.item_link_description)
        override val holderId: Int
            get() = ivImage.tag as Int

        init {
            ivImage.tag = generateHolderId()
            this.callback = callback
        }
    }

    private inner class VoiceHolder(itemView: View) : IdentificableHolder {
        val mWaveFormView: WaveFormView = itemView.findViewById(R.id.item_voice_wave_form_view)
        val mButtonPlay: ImageView
        val mDurationText: TextView
        val TranscriptText: TextView
        val mDoTranscript: TextView
        val mSpeed: ImageView
        override val holderId: Int
            get() = mWaveFormView.tag as Int

        init {
            mWaveFormView.setActiveColor(mActiveWaveFormColor)
            mWaveFormView.setNoactiveColor(mNoactiveWaveFormColor)
            mWaveFormView.setSectionCount(if (Utils.isLandscape(itemView.context)) 128 else 64)
            mWaveFormView.tag = generateHolderId()
            mButtonPlay = itemView.findViewById(R.id.item_voice_button_play)
            mDurationText = itemView.findViewById(R.id.item_voice_duration)
            TranscriptText = itemView.findViewById(R.id.transcription_text)
            mDoTranscript = itemView.findViewById(R.id.item_voice_translate)
            mSpeed = itemView.findViewById(R.id.item_voice_speed)
        }
    }

    companion object {
        private const val PREFFERED_STICKER_SIZE = 120
        private val DEFAUL_WAVEFORM = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        private var sHolderIdCounter = 0
        internal fun safeSetVisibitity(view: View?, visibility: Int) {
            if (view != null) view.visibility = visibility
        }

        internal fun generateHolderId(): Int {
            sHolderIdCounter++
            return sHolderIdCounter
        }
    }

    init {
        mAttachmentsActionCallback = attachmentsActionCallback
        mActiveWaveFormColor = CurrentTheme.getColorPrimary(mContext)
        mNoactiveWaveFormColor = Utils.adjustAlpha(mActiveWaveFormColor, 0.5f)
        isNightSticker =
            Settings.get().ui().isStickers_by_theme && Settings.get().ui().isDarkModeEnabled(
                mContext
            )
        expandVoiceTranscript = Settings.get().main().isExpand_voice_transcript
    }
}