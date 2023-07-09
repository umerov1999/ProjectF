package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.ContextMenu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders.IViewHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.media.music.MusicPlaybackService
import dev.ragnarok.fenrir.media.music.PlayerStatus
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioArtist
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.WallReply
import dev.ragnarok.fenrir.model.WikiPage
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Layout
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Layout.CATALOG_V2_HOLDER
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2RecommendationPlaylist
import dev.ragnarok.fenrir.model.menu.options.AudioOption
import dev.ragnarok.fenrir.module.StringHash
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.picasso.transforms.ImageHelper
import dev.ragnarok.fenrir.picasso.transforms.PolyTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toMainThread
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Mp3InfoHelper
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.hls.M3U8
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.AudioContainer
import dev.ragnarok.fenrir.view.VP2NestedRecyclerView
import dev.ragnarok.fenrir.view.WeakViewAnimatorAdapter
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.ref.WeakReference

class CatalogV2SectionAdapter(
    data: MutableList<AbsModel>,
    private val account_id: Long,
    private val mContext: Context
) : RecyclerBindableAdapter<AbsModel, IViewHolder>(data) {
    private var clickListener: ClickListener? = null
    private var disposable: Disposable = Disposable.disposed()
    private val mAudioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()

    private val isLongPressDownload: Boolean = Settings.get().main().isUse_long_click_download
    private val isAudio_round_icon: Boolean = Settings.get().main().isAudio_round_icon
    private var audioListDisposable = Disposable.disposed()
    private var mPlayerDisposable = Disposable.disposed()
    private var currAudio: Audio? = currentAudio
    private val isDark: Boolean = Settings.get().ui().isDarkModeEnabled(mContext)
    private var recyclerView: RecyclerView? = null

    companion object {
        val poolCatalogV2Section: RecyclerView.RecycledViewPool by lazy {
            RecyclerView.RecycledViewPool()
        }
    }

    override fun viewHolder(view: View, type: Int): IViewHolder {
        return when (type) {
            CATALOG_V2_HOLDER.TYPE_CATALOG_SLIDER, CATALOG_V2_HOLDER.TYPE_CATALOG_LIST, CATALOG_V2_HOLDER.TYPE_CATALOG_TRIPLE_STACKED_SLIDER -> {
                ItemViewHolder(
                    view,
                    type
                )
            }

            AbsModelType.MODEL_AUDIO -> {
                AudioHolder(view)
            }

            AbsModelType.MODEL_AUDIO_PLAYLIST -> {
                PlaylistHolder(view)
            }

            AbsModelType.MODEL_CATALOG_V2_RECOMMENDATION_PLAYLIST -> {
                RecommendationPlaylistHolder(view)
            }

            else -> CatalogV2Layout.createHolder(type).create(view)
        }
    }

    override fun layoutId(type: Int): Int {
        return when (type) {
            CATALOG_V2_HOLDER.TYPE_CATALOG_SLIDER, CATALOG_V2_HOLDER.TYPE_CATALOG_LIST, CATALOG_V2_HOLDER.TYPE_CATALOG_TRIPLE_STACKED_SLIDER -> {
                R.layout.item_catalog_v2_content_horizontal
            }

            AbsModelType.MODEL_AUDIO -> {
                R.layout.item_audio
            }

            AbsModelType.MODEL_AUDIO_PLAYLIST -> {
                R.layout.item_catalog_v2_audio_playlist
            }

            AbsModelType.MODEL_CATALOG_V2_RECOMMENDATION_PLAYLIST -> {
                R.layout.item_catalog_v2_playlist_recommended_horizontal
            }

            else -> CatalogV2Layout.createHolder(type).getLayout()
        }
    }

    override fun getItemType(position: Int): Int {
        if (getItem(position).getModelType() == AbsModelType.MODEL_CATALOG_V2_BLOCK) {
            return (getItem(position) as CatalogV2Block).layout.getViewHolderType()
        }
        return getItem(position).getModelType()
    }

    override fun onBindItemViewHolder(
        viewHolder: IViewHolder,
        position: Int,
        type: Int
    ) {
        viewHolder.bind(position, getItem(position))
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onAddPlayList(index: Int, album: AudioPlaylist)
        fun onRequestWritePermissions()
        fun onNext(loading: Boolean)
        fun onError(throwable: Throwable)
    }

    inner class ItemViewHolder(
        itemView: View,
        @CATALOG_V2_HOLDER private val type: Int
    ) : IViewHolder(itemView) {
        val list: VP2NestedRecyclerView = itemView.findViewById(R.id.list)

        private inline fun <reified T, reified VH : RecyclerView.ViewHolder> configureAdapterBindable(
            itemDataHolder: CatalogV2Block,
            list: RecyclerView,
            adapter: WeakReference<RecyclerBindableAdapter<T, VH>>
        ) {
            list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(4, 1000) {
                override fun onScrollToLastElement() {
                    if (itemDataHolder.next_from.nonNullNoEmpty()) {
                        clickListener?.onNext(true)
                        disposable = mAudioInteractor.getCatalogV2BlockItems(
                            account_id,
                            itemDataHolder.id.orEmpty(),
                            itemDataHolder.next_from
                        )
                            .fromIOToMain()
                            .subscribe({ data ->
                                val s = itemDataHolder.items?.size.orZero()
                                itemDataHolder.update(data)
                                adapter.get()?.notifyItemBindableRangeInserted(
                                    s,
                                    itemDataHolder.items?.size.orZero() - s
                                )
                                clickListener?.onNext(false)
                            }) {
                                if (it.cause !is ApiException || "Not found" != (it.cause as ApiException).error.errorMsg || it !is ApiException || "Not found" != it.error.errorMsg) {
                                    clickListener?.onError(it)
                                } else {
                                    clickListener?.onNext(false)
                                }
                            }
                    }
                }
            })
        }

        private fun createLayoutManager(itemDataHolder: CatalogV2Block) {
            list.visibility = View.VISIBLE
            list.layoutManager = when (type) {
                CATALOG_V2_HOLDER.TYPE_CATALOG_TRIPLE_STACKED_SLIDER -> StaggeredGridLayoutManager(
                    3,
                    StaggeredGridLayoutManager.HORIZONTAL
                )

                CATALOG_V2_HOLDER.TYPE_CATALOG_LIST, CATALOG_V2_HOLDER.TYPE_CATALOG_SLIDER -> LinearLayoutManager(
                    itemView.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                else -> {
                    throw UnsupportedOperationException()
                }
            }
            list.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
            list.updateUid(itemDataHolder.id?.let { it1 -> StringHash.calculateCRC32(it1) })
        }

        private fun findScrollable(items: ArrayList<AbsModel>): Int {
            for (i in 0 until items.size) {
                if (items[i].getModelType() == AbsModelType.MODEL_AUDIO) {
                    val audio = items[i] as Audio
                    if (audio.isAnimationNow) {
                        return i
                    }
                } else if (items[i].getModelType() == AbsModelType.MODEL_CATALOG_V2_BLOCK) {
                    val block = items[i] as CatalogV2Block
                    if (block.isScroll()) {
                        return i
                    }
                } else if (items[i].getModelType() == AbsModelType.MODEL_CATALOG_V2_RECOMMENDATION_PLAYLIST) {
                    val block = items[i] as CatalogV2RecommendationPlaylist
                    if (block.isScroll()) {
                        return i
                    }
                }
            }
            return -1
        }

        override fun bind(position: Int, itemDataHolder: AbsModel) {
            if (itemDataHolder !is CatalogV2Block) {
                list.visibility = View.GONE
                list.clearOnScrollListeners()
                list.adapter = null
                list.updateUid(-1)
                return
            }
            list.visibility = View.GONE
            list.clearOnScrollListeners()
            list.adapter = null
            list.updateUid(-1)
            itemDataHolder.items?.let {
                val op = findScrollable(it)
                createLayoutManager(itemDataHolder)
                //list.setRecycledViewPool(poolCatalogV2Section)
                val adapter = CatalogV2SectionAdapter(it, account_id, mContext)
                adapter.clickListener = clickListener
                list.adapter = adapter
                configureAdapterBindable(itemDataHolder, list, WeakReference(adapter))
                if (op >= 0) {
                    list.scrollToPosition(
                        op + adapter.headersCount
                    )
                }
            }
        }
    }

    ///////////////////////////////////////SUB_HOLDERS////////////////////////////////////////////////

    ///////////////////////////////////////PLAYLIST_HOLDER////////////////////////////////////////////
    private fun onDelete(index: Int, album: AudioPlaylist) {
        audioListDisposable =
            mAudioInteractor.deletePlaylist(account_id, album.getId(), album.getOwnerId())
                .fromIOToMain()
                .subscribe({
                    getItems().removeAt(index)
                    notifyItemBindableRemoved(index)
                    CustomToast.createCustomToast(mContext).showToast(R.string.deleted)
                }) { throwable ->
                    clickListener?.onError(
                        throwable
                    )
                }
    }

    private fun onAddPlaylist(album: AudioPlaylist) {
        audioListDisposable = mAudioInteractor.followPlaylist(
            account_id,
            album.getId(),
            album.getOwnerId(),
            album.getAccess_key()
        )
            .fromIOToMain()
            .subscribe({
                CustomToast.createCustomToast(mContext).showToast(R.string.success)
            }) {
                clickListener?.onError(
                    it
                )
            }
    }

    inner class PlaylistHolder(itemView: View) : IViewHolder(itemView),
        View.OnCreateContextMenuListener {
        val thumb: ImageView
        val name: TextView
        val year: TextView
        val artist: TextView
        val playlist_container: View
        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val playlist = getItem(position) as AudioPlaylist
            if (Settings.get().accounts().current == playlist.getOwnerId()) {
                menu.add(0, v.id, 0, R.string.delete)
                    .setOnMenuItemClickListener {
                        onDelete(position, playlist)
                        true
                    }
            } else {
                menu.add(0, v.id, 0, R.string.save).setOnMenuItemClickListener {
                    onAddPlaylist(playlist)
                    true
                }
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            thumb = itemView.findViewById(R.id.item_thumb)
            name = itemView.findViewById(R.id.item_name)
            playlist_container = itemView.findViewById(R.id.playlist_container)
            year = itemView.findViewById(R.id.item_year)
            artist = itemView.findViewById(R.id.item_artist)
        }

        override fun bind(position: Int, itemDataHolder: AbsModel) {
            val playlist = itemDataHolder as AudioPlaylist
            if (playlist.getThumb_image().nonNullNoEmpty()) ViewUtils.displayAvatar(
                thumb,
                PolyTransformation(),
                playlist.getThumb_image(),
                Constants.PICASSO_TAG
            ) else thumb.setImageBitmap(
                ImageHelper.getEllipseBitmap(
                    BitmapFactory.decodeResource(
                        mContext.resources,
                        if (isDark) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light
                    ), 0.1f
                )
            )
            name.text = playlist.getTitle()
            if (playlist.getArtist_name().isNullOrEmpty()) artist.visibility = View.GONE else {
                artist.visibility = View.VISIBLE
                artist.text = playlist.getArtist_name()
            }
            if (playlist.getYear() == 0) year.visibility = View.GONE else {
                year.visibility = View.VISIBLE
                year.text = playlist.getYear().toString()
            }
            playlist_container.setOnClickListener {
                if (playlist.getOriginal_access_key()
                        .isNullOrEmpty() || playlist.getOriginal_id() == 0 || playlist.getOriginal_owner_id() == 0L
                ) PlaceFactory.getAudiosInAlbumPlace(
                    account_id, playlist.getOwnerId(), playlist.getId(), playlist.getAccess_key()
                ).tryOpenWith(mContext) else PlaceFactory.getAudiosInAlbumPlace(
                    account_id,
                    playlist.getOriginal_owner_id(),
                    playlist.getOriginal_id(),
                    playlist.getOriginal_access_key()
                ).tryOpenWith(mContext)
            }
        }
    }

    inner class RecommendationPlaylistHolder(itemView: View) : IViewHolder(itemView),
        View.OnCreateContextMenuListener {
        val percentage: TextView
        val percentageTitle: TextView
        val title: TextView
        val subtitle: TextView
        val audios: AudioContainer
        val playlist_container: View
        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val position = recyclerView?.getChildAdapterPosition(itemView) ?: 0
            val recPlaylist = getItem(position) as CatalogV2RecommendationPlaylist
            if (Settings.get().accounts().current == recPlaylist.getOwnerId()) {
                menu.add(0, itemView.id, 0, R.string.delete)
                    .setOnMenuItemClickListener {
                        onDelete(position, recPlaylist.getPlaylist())
                        true
                    }
            } else {
                menu.add(0, itemView.id, 0, R.string.save).setOnMenuItemClickListener {
                    onAddPlaylist(recPlaylist.getPlaylist())
                    true
                }

                menu.add(0, itemView.id, 0, R.string.goto_user).setOnMenuItemClickListener {
                    recPlaylist.getOwner()
                        ?.let { it1 ->
                            PlaceFactory.getOwnerWallPlace(account_id, it1).tryOpenWith(mContext)
                        }
                    true
                }
            }
        }

        init {
            percentage = itemView.findViewById(R.id.percentage)
            percentageTitle = itemView.findViewById(R.id.percentageTitle)
            playlist_container = itemView.findViewById(R.id.playlist_container)
            playlist_container.setOnCreateContextMenuListener(this)
            title = itemView.findViewById(R.id.title)
            subtitle = itemView.findViewById(R.id.subtitle)
            audios = itemView.findViewById(R.id.audio_container)
        }

        @SuppressLint("SetTextI18n")
        override fun bind(position: Int, itemDataHolder: AbsModel) {
            val recPlaylist = itemDataHolder as CatalogV2RecommendationPlaylist
            val playlist = recPlaylist.getPlaylist()
            playlist_container.setOnClickListener {
                if (playlist.getOriginal_access_key()
                        .isNullOrEmpty() || playlist.getOriginal_id() == 0 || playlist.getOriginal_owner_id() == 0L
                ) PlaceFactory.getAudiosInAlbumPlace(
                    account_id, playlist.getOwnerId(), playlist.getId(), playlist.getAccess_key()
                ).tryOpenWith(mContext) else PlaceFactory.getAudiosInAlbumPlace(
                    account_id,
                    playlist.getOriginal_owner_id(),
                    playlist.getOriginal_id(),
                    playlist.getOriginal_access_key()
                ).tryOpenWith(mContext)
            }
            percentage.text = ((recPlaylist.getPercentage() * 100).toInt()).toString() + "%"
            percentageTitle.text = recPlaylist.getPercentageTitle()
            title.text = playlist.getTitle()
            title.setTextColor(recPlaylist.getColor())
            subtitle.text = recPlaylist.getOwner()?.fullName
            audios.displayAudios(recPlaylist.getAudios(), object :
                AttachmentsViewBinder.OnAttachmentsActionCallback {
                override fun onPollOpen(poll: Poll) {
                    TODO("Not yet implemented")
                }

                override fun onVideoPlay(video: Video) {
                    TODO("Not yet implemented")
                }

                override fun onAudioPlay(
                    position: Int,
                    audios: ArrayList<Audio>,
                    holderPosition: Int?
                ) {
                    MusicPlaybackService.startForPlayList(
                        mContext,
                        audios,
                        position,
                        false
                    )
                    if (!Settings.get().other().isShow_mini_player) PlaceFactory.getPlayerPlace(
                        Settings.get().accounts().current
                    ).tryOpenWith(mContext)
                }

                override fun onForwardMessagesOpen(messages: ArrayList<Message>) {
                }

                override fun onOpenOwner(ownerId: Long) {
                }

                override fun onGoToMessagesLookup(message: Message) {
                }

                override fun onDocPreviewOpen(document: Document) {
                }

                override fun onPostOpen(post: Post) {
                }

                override fun onLinkOpen(link: Link) {
                }

                override fun onUrlOpen(url: String) {
                }

                override fun onFaveArticle(article: Article) {
                }

                override fun onShareArticle(article: Article) {
                }

                override fun onWikiPageOpen(page: WikiPage) {
                }

                override fun onPhotosOpen(photos: ArrayList<Photo>, index: Int, refresh: Boolean) {
                }

                override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
                    PlaceFactory.getSingleURLPhotoPlace(url, prefix, photo_prefix)
                        .tryOpenWith(mContext)
                }

                override fun onStoryOpen(story: Story) {
                }

                override fun onWallReplyOpen(reply: WallReply) {
                }

                override fun onAudioPlaylistOpen(playlist: AudioPlaylist) {
                }

                override fun onPhotoAlbumOpen(album: PhotoAlbum) {
                }

                override fun onMarketAlbumOpen(market_album: MarketAlbum) {
                }

                override fun onMarketOpen(market: Market) {
                }

                override fun onArtistOpen(artist: AudioArtist) {
                }

                override fun onRequestWritePermissions() {
                    clickListener?.onRequestWritePermissions()
                }

            }, getItemRawPosition(bindingAdapterPosition))
        }
    }

    ///////////////////////////////////////AUDIO_HOLDER////////////////////////////////////////////////
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        mPlayerDisposable = MusicPlaybackController.observeServiceBinding()
            .toMainThread()
            .subscribe { onServiceBindEvent(it) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
        disposable.dispose()
        clickListener?.onNext(false)
        mPlayerDisposable.dispose()
        audioListDisposable.dispose()
    }


    internal fun deleteTrack(accountId: Long, audio: Audio, position: Int) {
        audioListDisposable =
            mAudioInteractor.delete(accountId, audio.id, audio.ownerId).fromIOToMain().subscribe(
                {
                    getItems().removeAt(position)
                    notifyItemBindableRemoved(position)
                    CustomToast.createCustomToast(mContext).showToast(R.string.deleted)
                }) { t -> CustomToast.createCustomToast(mContext).showToastThrowable(t) }
    }

    internal fun addTrack(accountId: Long, audio: Audio) {
        audioListDisposable = mAudioInteractor.add(accountId, audio, null).fromIOToMain().subscribe(
            { CustomToast.createCustomToast(mContext).showToast(R.string.added) }) { t ->
            CustomToast.createCustomToast(mContext).showToastThrowable(t)
        }
    }

    internal fun getMp3AndBitrate(accountId: Long, audio: Audio) {
        val mode = audio.needRefresh()
        if (mode.first) {
            audioListDisposable =
                mAudioInteractor.getByIdOld(accountId, listOf(audio), mode.second).fromIOToMain()
                    .subscribe({ t -> getBitrate(t[0]) }) {
                        getBitrate(
                            audio
                        )
                    }
        } else {
            getBitrate(audio)
        }
    }

    private fun getBitrate(audio: Audio) {
        val pUrl = audio.url
        if (pUrl.isNullOrEmpty()) {
            return
        }
        audioListDisposable = if (audio.isHLS) {
            M3U8(pUrl).length.fromIOToMain()
                .subscribe(
                    { r: Long ->
                        CustomToast.createCustomToast(mContext).showToast(
                            Mp3InfoHelper.getBitrate(
                                mContext,
                                audio.duration,
                                r
                            )
                        )
                    }
                ) { e -> CustomToast.createCustomToast(mContext).showToastThrowable(e) }
        } else {
            Mp3InfoHelper.getLength(pUrl).fromIOToMain()
                .subscribe(
                    { r: Long ->
                        CustomToast.createCustomToast(mContext).showToast(
                            Mp3InfoHelper.getBitrate(
                                mContext,
                                audio.duration,
                                r
                            )
                        )
                    }
                ) { e -> CustomToast.createCustomToast(mContext).showToastThrowable(e) }
        }
    }

    internal fun get_lyrics(audio: Audio) {
        audioListDisposable =
            mAudioInteractor.getLyrics(Settings.get().accounts().current, audio)
                .fromIOToMain()
                .subscribe({ t ->
                    onAudioLyricsReceived(
                        t,
                        audio
                    )
                }) { t -> CustomToast.createCustomToast(mContext).showToastThrowable(t) }
    }

    private fun onAudioLyricsReceived(Text: String, audio: Audio) {
        val title = audio.artistAndTitle
        MaterialAlertDialogBuilder(mContext)
            .setIcon(R.drawable.dir_song)
            .setMessage(Text)
            .setTitle(title)
            .setPositiveButton(R.string.button_ok, null)
            .setNeutralButton(R.string.copy_text) { _: DialogInterface?, _: Int ->
                val clipboard = mContext.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager?
                val clip = ClipData.newPlainText("response", Text)
                clipboard?.setPrimaryClip(clip)
                CustomToast.createCustomToast(mContext).showToast(R.string.copied_to_clipboard)
            }
            .setCancelable(true)
            .show()
    }

    private fun onServiceBindEvent(@PlayerStatus status: Int) {
        when (status) {
            PlayerStatus.UPDATE_TRACK_INFO, PlayerStatus.SERVICE_KILLED, PlayerStatus.UPDATE_PLAY_PAUSE -> {
                updateAudio(currAudio)
                currAudio = currentAudio
                updateAudio(currAudio)
            }

            PlayerStatus.REPEATMODE_CHANGED, PlayerStatus.SHUFFLEMODE_CHANGED, PlayerStatus.UPDATE_PLAY_LIST -> {}
        }
    }

    private fun updateAudio(audio: Audio?) {
        audio ?: return
        val pos = indexOfAdapter(audio)
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (isAudio_round_icon) R.drawable.audio_button else R.drawable.audio_button_material

    private val transformCover: Transformation by lazy {
        if (isAudio_round_icon) RoundTransformation() else PolyTransformation()
    }

    private fun updateAudioStatus(holder: AudioHolder, audio: Audio) {
        if (audio != currAudio) {
            holder.visual.setImageResource(audio.songIcon)
            holder.play_cover.clearColorFilter()
            return
        }
        when (MusicPlaybackController.playerStatus()) {
            1 -> {
                Utils.doWavesLottie(holder.visual, true)
                holder.play_cover.setColorFilter(Color.parseColor("#44000000"))
            }

            2 -> {
                Utils.doWavesLottie(holder.visual, false)
                holder.play_cover.setColorFilter(Color.parseColor("#44000000"))
            }
        }
    }

    internal fun updateDownloadState(holder: AudioHolder, audio: Audio) {
        if (audio.downloadIndicator == 2) {
            holder.saved.setImageResource(R.drawable.remote_cloud)
            Utils.setColorFilter(holder.saved, CurrentTheme.getColorSecondary(mContext))
        } else {
            holder.saved.setImageResource(R.drawable.save)
            Utils.setColorFilter(holder.saved, CurrentTheme.getColorPrimary(mContext))
        }
        holder.saved.visibility =
            if (audio.downloadIndicator != 0) View.VISIBLE else View.GONE
    }

    private fun fireEditTrack(position: Int, audio: Audio) {
        val root = View.inflate(mContext, R.layout.entry_audio_info, null)
        (root.findViewById<View>(R.id.edit_artist) as TextInputEditText).setText(audio.artist)
        (root.findViewById<View>(R.id.edit_title) as TextInputEditText).setText(audio.title)
        MaterialAlertDialogBuilder(mContext)
            .setTitle(R.string.enter_audio_info)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                val artist =
                    (root.findViewById<View>(R.id.edit_artist) as TextInputEditText).text.toString()
                val title =
                    (root.findViewById<View>(R.id.edit_title) as TextInputEditText).text.toString()
                audioListDisposable = mAudioInteractor.edit(
                    account_id,
                    audio.ownerId,
                    audio.id,
                    artist,
                    title
                ).fromIOToMain()
                    .subscribe({
                        audio.setTitle(title).setArtist(artist)
                        notifyItemBindableChanged(position)
                    }) { t ->
                        clickListener?.onError(Utils.getCauseIfRuntime(t))
                    }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun doMenu(holder: AudioHolder, position: Int, view: View, audio: Audio) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioOption.play_item_audio,
                mContext.getString(R.string.play),
                R.drawable.play,
                true
            )
        )
        if (MusicPlaybackController.canPlayAfterCurrent(audio)) {
            menus.add(
                OptionRequest(
                    AudioOption.play_item_after_current_audio,
                    mContext.getString(R.string.play_audio_after_current),
                    R.drawable.play_next,
                    false
                )
            )
        }
        if (audio.ownerId != Settings.get().accounts().current) {
            menus.add(
                OptionRequest(
                    AudioOption.add_item_audio,
                    mContext.getString(R.string.action_add),
                    R.drawable.list_add,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    AudioOption.add_and_download_button,
                    mContext.getString(R.string.add_and_download_button),
                    R.drawable.add_download,
                    false
                )
            )
        } else {
            menus.add(
                OptionRequest(
                    AudioOption.add_item_audio,
                    mContext.getString(R.string.delete),
                    R.drawable.ic_outline_delete,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    AudioOption.edit_track,
                    mContext.getString(R.string.edit),
                    R.drawable.about_writed,
                    true
                )
            )
        }
        menus.add(
            OptionRequest(
                AudioOption.share_button,
                mContext.getString(R.string.share),
                R.drawable.ic_outline_share,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.save_item_audio,
                mContext.getString(R.string.save),
                R.drawable.save,
                true
            )
        )
        if (audio.albumId != 0) menus.add(
            OptionRequest(
                AudioOption.open_album,
                mContext.getString(R.string.open_album),
                R.drawable.audio_album,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.get_recommendation_by_audio,
                mContext.getString(R.string.get_recommendation_by_audio),
                R.drawable.music_mic,
                false
            )
        )
        if (audio.main_artists.nonNullNoEmpty()) menus.add(
            OptionRequest(
                AudioOption.goto_artist,
                mContext.getString(R.string.audio_goto_artist),
                R.drawable.artist_icon,
                false
            )
        )
        if (audio.lyricsId != 0) menus.add(
            OptionRequest(
                AudioOption.get_lyrics_menu,
                mContext.getString(R.string.get_lyrics_menu),
                R.drawable.lyric,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.bitrate_item_audio,
                mContext.getString(R.string.get_bitrate),
                R.drawable.high_quality,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.search_by_artist,
                mContext.getString(R.string.search_by_artist),
                R.drawable.magnify,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.copy_url,
                mContext.getString(R.string.copy_url),
                R.drawable.content_copy,
                false
            )
        )
        menus.header(
            Utils.firstNonEmptyString(audio.artist, " ") + " - " + audio.title,
            R.drawable.song,
            audio.thumb_image_little
        )
        menus.columns(2)
        menus.show(
            (mContext as FragmentActivity).supportFragmentManager,
            "audio_options"
        ) { _, option ->
            when (option.id) {
                AudioOption.play_item_audio -> {
                    doPlay(position, audio)
                }

                AudioOption.play_item_after_current_audio -> MusicPlaybackController.playAfterCurrent(
                    audio
                )

                AudioOption.edit_track -> {
                    fireEditTrack(position, audio)
                }

                AudioOption.share_button -> SendAttachmentsActivity.startForSendAttachments(
                    mContext,
                    Settings.get().accounts().current,
                    audio
                )

                AudioOption.search_by_artist -> PlaceFactory.getSingleTabSearchPlace(
                    Settings.get().accounts().current,
                    SearchContentType.AUDIOS,
                    AudioSearchCriteria(
                        audio.artist, by_artist = true, in_main_page = false
                    )
                ).tryOpenWith(mContext)

                AudioOption.get_lyrics_menu -> get_lyrics(audio)
                AudioOption.get_recommendation_by_audio -> PlaceFactory.SearchByAudioPlace(
                    Settings.get().accounts().current, audio.ownerId, audio.id
                ).tryOpenWith(mContext)

                AudioOption.open_album -> PlaceFactory.getAudiosInAlbumPlace(
                    Settings.get().accounts().current,
                    audio.album_owner_id,
                    audio.albumId,
                    audio.album_access_key
                ).tryOpenWith(mContext)

                AudioOption.copy_url -> {
                    val clipboard =
                        mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("response", audio.url)
                    clipboard?.setPrimaryClip(clip)
                    CustomToast.createCustomToast(mContext).showToast(R.string.copied)
                }

                AudioOption.add_item_audio -> {
                    val myAudio = audio.ownerId == Settings.get().accounts().current
                    if (myAudio) {
                        deleteTrack(Settings.get().accounts().current, audio, position)
                    } else {
                        addTrack(Settings.get().accounts().current, audio)
                    }
                }

                AudioOption.add_and_download_button -> {
                    if (audio.ownerId != Settings.get().accounts().current) {
                        addTrack(Settings.get().accounts().current, audio)
                    }
                    if (!AppPerms.hasReadWriteStoragePermission(mContext)) {
                        clickListener?.onRequestWritePermissions()
                        return@show
                    }
                    audio.downloadIndicator = 1
                    updateDownloadState(holder, audio)
                    val ret = DownloadWorkUtils.doDownloadAudio(
                        mContext,
                        audio,
                        Settings.get().accounts().current,
                        Force = false,
                        isLocal = false
                    )
                    when (ret) {
                        0 -> CustomToast.createCustomToast(mContext)
                            .showToastBottom(R.string.saved_audio)

                        1, 2 -> {
                            CustomSnackbars.createCustomSnackbars(view)
                                ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                ?.themedSnack(if (ret == 1) R.string.audio_force_download else R.string.audio_force_download_pc)
                                ?.setAction(
                                    R.string.button_yes
                                ) {
                                    DownloadWorkUtils.doDownloadAudio(
                                        mContext,
                                        audio,
                                        Settings.get().accounts().current,
                                        Force = true,
                                        isLocal = false
                                    )
                                }
                                ?.show()
                        }

                        else -> {
                            audio.downloadIndicator = 0
                            updateDownloadState(holder, audio)
                            CustomToast.createCustomToast(mContext)
                                .showToastBottom(R.string.error_audio)
                        }
                    }
                }

                AudioOption.save_item_audio -> {
                    if (!AppPerms.hasReadWriteStoragePermission(mContext)) {
                        clickListener?.onRequestWritePermissions()
                        return@show
                    }
                    audio.downloadIndicator = 1
                    updateDownloadState(holder, audio)
                    val ret = DownloadWorkUtils.doDownloadAudio(
                        mContext,
                        audio,
                        Settings.get().accounts().current,
                        Force = false,
                        isLocal = false
                    )
                    when (ret) {
                        0 -> CustomToast.createCustomToast(mContext)
                            .showToastBottom(R.string.saved_audio)

                        1, 2 -> {
                            CustomSnackbars.createCustomSnackbars(view)
                                ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                ?.themedSnack(if (ret == 1) R.string.audio_force_download else R.string.audio_force_download_pc)
                                ?.setAction(
                                    R.string.button_yes
                                ) {
                                    DownloadWorkUtils.doDownloadAudio(
                                        mContext,
                                        audio,
                                        Settings.get().accounts().current,
                                        Force = true,
                                        isLocal = false
                                    )
                                }
                                ?.show()
                        }

                        else -> {
                            audio.downloadIndicator = 0
                            updateDownloadState(holder, audio)
                            CustomToast.createCustomToast(mContext)
                                .showToastBottom(R.string.error_audio)
                        }
                    }
                }

                AudioOption.bitrate_item_audio -> getMp3AndBitrate(
                    Settings.get().accounts().current, audio
                )

                AudioOption.goto_artist -> {
                    val artists = Utils.getArrayFromHash(
                        audio.main_artists
                    )
                    if (audio.main_artists?.keys?.size.orZero() > 1) {
                        MaterialAlertDialogBuilder(mContext)
                            .setItems(artists[1]) { _: DialogInterface?, which: Int ->
                                PlaceFactory.getArtistPlace(
                                    Settings.get().accounts().current,
                                    artists[0][which]
                                ).tryOpenWith(mContext)
                            }
                            .show()
                    } else {
                        PlaceFactory.getArtistPlace(
                            Settings.get().accounts().current,
                            artists[0][0]
                        ).tryOpenWith(mContext)
                    }
                }
            }
        }
    }

    private fun doPlay(position: Int, audio: Audio) {
        if (MusicPlaybackController.isNowPlayingOrPreparingOrPaused(audio)) {
            if (!Settings.get().other().isUse_stop_audio) {
                MusicPlaybackController.playOrPause()
            } else {
                MusicPlaybackController.stop()
            }
        } else {
            val op = ArrayList<Audio>(MusicPlaybackService.MAX_QUEUE_SIZE)
            val half = MusicPlaybackService.MAX_QUEUE_SIZE / 2
            val st = (position - half).coerceAtLeast(0)
            val en = (position + half).coerceAtMost(realItemCount)
            for (i in st until en) {
                if (getItem(i).getModelType() == AbsModelType.MODEL_AUDIO) {
                    op.add(getItem(i) as Audio)
                }
            }
            MusicPlaybackService.startForPlayList(
                mContext, op, op.indexOf(audio), false
            )
            if (!Settings.get().other().isShow_mini_player) PlaceFactory.getPlayerPlace(
                account_id
            ).tryOpenWith(mContext)
        }
    }

    inner class AudioHolder(itemView: View) : IViewHolder(itemView) {
        val artist: TextView = itemView.findViewById(R.id.dialog_title)
        val title: TextView = itemView.findViewById(R.id.dialog_message)
        val play: View = itemView.findViewById(R.id.item_audio_play)
        val play_cover: ImageView = itemView.findViewById(R.id.item_audio_play_cover)
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)
        val time: TextView = itemView.findViewById(R.id.item_audio_time)
        val saved: ImageView = itemView.findViewById(R.id.saved)
        val lyric: ImageView = itemView.findViewById(R.id.lyric)
        val my: ImageView = itemView.findViewById(R.id.my)
        val quality: ImageView = itemView.findViewById(R.id.quality)
        val Track: View = itemView.findViewById(R.id.track_option)
        val selectionView: MaterialCardView = itemView.findViewById(R.id.item_audio_selection)
        val isSelectedView: MaterialCardView = itemView.findViewById(R.id.item_audio_select_add)
        val animationAdapter: Animator.AnimatorListener
        var animator: ObjectAnimator? = null
        fun startSelectionAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorPrimary(mContext))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 1500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun startSomeAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorSecondary(mContext))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun cancelSelectionAnimation() {
            animator?.cancel()
            animator = null
            selectionView.visibility = View.INVISIBLE
        }

        override fun bind(position: Int, itemDataHolder: AbsModel) {
            val audio = itemDataHolder as Audio
            cancelSelectionAnimation()
            if (audio.isAnimationNow) {
                startSelectionAnimation()
                audio.isAnimationNow = false
            }
            artist.text = audio.artist
            if (!audio.isLocal && !audio.isLocalServer && Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID && !audio.isHLS) {
                quality.visibility = View.VISIBLE
                if (audio.isHq) {
                    quality.setImageResource(R.drawable.high_quality)
                } else {
                    quality.setImageResource(R.drawable.low_quality)
                }
            } else {
                quality.visibility = View.GONE
            }
            title.text = audio.title
            if (audio.duration <= 0) time.visibility = View.INVISIBLE else {
                time.visibility = View.VISIBLE
                time.text = AppTextUtils.getDurationString(audio.duration)
            }
            lyric.visibility = if (audio.lyricsId != 0) View.VISIBLE else View.GONE
            isSelectedView.visibility = if (audio.isSelected) View.VISIBLE else View.GONE
            if (audio.isSelected) {
                when {
                    audio.url.isNullOrEmpty() -> {
                        isSelectedView.setCardBackgroundColor(Color.parseColor("#ff0000"))
                    }

                    DownloadWorkUtils.TrackIsDownloaded(audio) != 0 -> {
                        isSelectedView.setCardBackgroundColor(Color.parseColor("#00aa00"))
                    }

                    else -> {
                        isSelectedView.setCardBackgroundColor(
                            CurrentTheme.getColorPrimary(
                                mContext
                            )
                        )
                    }
                }
            }
            my.visibility =
                if (audio.ownerId == Settings.get().accounts().current) View.VISIBLE else View.GONE
            saved.visibility = View.GONE
            updateDownloadState(this, audio)
            updateAudioStatus(this, audio)
            if (audio.thumb_image_little.nonNullNoEmpty()) {
                PicassoInstance.with()
                    .load(audio.thumb_image_little)
                    .placeholder(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            audioCoverSimple,
                            mContext.theme
                        ) ?: return
                    )
                    .transform(transformCover)
                    .tag(Constants.PICASSO_TAG)
                    .into(play_cover)
            } else {
                PicassoInstance.with().cancelRequest(play_cover)
                play_cover.setImageResource(audioCoverSimple)
            }
            play.setOnLongClickListener {
                if ((audio.thumb_image_very_big.nonNullNoEmpty()
                            || audio.thumb_image_big.nonNullNoEmpty() || audio.thumb_image_little.nonNullNoEmpty()) && audio.artist.nonNullNoEmpty() && audio.title.nonNullNoEmpty()
                ) {
                    Utils.firstNonEmptyString(
                        audio.thumb_image_very_big,
                        audio.thumb_image_big, audio.thumb_image_little
                    )?.let {
                        audio.artist?.let { it1 ->
                            audio.title?.let { it2 ->
                                PlaceFactory.getSingleURLPhotoPlace(it, it1, it2)
                                    .tryOpenWith(mContext)
                            }
                        }
                    }
                }
                true
            }
            play.setOnClickListener { v: View ->
                if (Settings.get().main().isRevert_play_audio) {
                    doMenu(this, getItemRawPosition(bindingAdapterPosition), v, audio)
                } else {
                    doPlay(getItemRawPosition(bindingAdapterPosition), audio)
                }
            }
            if (isLongPressDownload) {
                Track.setOnLongClickListener { v: View? ->
                    if (!AppPerms.hasReadWriteStoragePermission(mContext)) {
                        clickListener?.onRequestWritePermissions()
                        return@setOnLongClickListener false
                    }
                    audio.downloadIndicator = 1
                    updateDownloadState(this, audio)
                    val ret = DownloadWorkUtils.doDownloadAudio(
                        mContext,
                        audio,
                        Settings.get().accounts().current,
                        Force = false,
                        isLocal = false
                    )
                    when (ret) {
                        0 -> CustomToast.createCustomToast(mContext)
                            .showToastBottom(R.string.saved_audio)

                        1, 2 -> {
                            v?.let {
                                CustomSnackbars.createCustomSnackbars(it)
                                    ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                    ?.themedSnack(if (ret == 1) R.string.audio_force_download else R.string.audio_force_download_pc)
                                    ?.setAction(
                                        R.string.button_yes
                                    ) {
                                        DownloadWorkUtils.doDownloadAudio(
                                            mContext,
                                            audio,
                                            Settings.get().accounts().current,
                                            Force = true,
                                            isLocal = false
                                        )
                                    }
                                    ?.show()
                            }
                        }

                        else -> {
                            audio.downloadIndicator = 0
                            updateDownloadState(this, audio)
                            CustomToast.createCustomToast(mContext)
                                .showToastBottom(R.string.error_audio)
                        }
                    }
                    true
                }
            }
            Track.setOnClickListener { view: View ->
                cancelSelectionAnimation()
                startSomeAnimation()
                if (Settings.get().main().isRevert_play_audio) {
                    doPlay(getItemRawPosition(bindingAdapterPosition), audio)
                } else {
                    doMenu(this, getItemRawPosition(bindingAdapterPosition), view, audio)
                }
            }
        }

        init {
            animationAdapter = object : WeakViewAnimatorAdapter<View>(selectionView) {
                override fun onAnimationEnd(view: View) {
                    view.visibility = View.GONE
                }

                override fun onAnimationStart(view: View) {
                    view.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(view: View) {
                    view.visibility = View.GONE
                }
            }
        }
    }
}