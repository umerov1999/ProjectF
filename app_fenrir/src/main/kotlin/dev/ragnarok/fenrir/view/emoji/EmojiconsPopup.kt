package dev.ragnarok.fenrir.view.emoji

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.GridView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.textfield.TextInputEditText
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.Sticker.LocalSticker
import dev.ragnarok.fenrir.model.StickerSet
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.view.emoji.section.*

class EmojiconsPopup(private var rootView: View?, private val mContext: Activity) {
    private var keyBoardHeight = 0
    var isKeyBoardOpen = false
        private set
    var onEmojiconClickedListener: OnEmojiconClickedListener? = null
    var onStickerClickedListener: OnStickerClickedListener? = null
    var onMyStickerClickedListener: OnMyStickerClickedListener? = null
        private set
    var onEmojiconBackspaceClickedListener: OnEmojiconBackspaceClickedListener? = null
    var onSoftKeyboardOpenCloseListener: OnSoftKeyboardOpenCloseListener? = null
    private var emojiContainer: View? = null
    private var keyboardTmpHeight: Int = 0
    private val onGlobalLayoutListener: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        override fun onGlobalLayout() {
            val r = Rect()
            if (rootView == null) {
                return
            }
            rootView?.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView?.rootView?.height ?: 0
            var heightDifference = screenHeight - (r.bottom - r.top)
            val navBarHeight =
                mContext.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (navBarHeight > 0) {
                heightDifference -= mContext.resources.getDimensionPixelSize(navBarHeight)
            }
            val statusbarHeight =
                mContext.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (statusbarHeight > 0) {
                heightDifference -= mContext.resources.getDimensionPixelSize(statusbarHeight)
            }
            if (heightDifference > 200) {
                keyBoardHeight = heightDifference
                if (emojiContainer != null && !Settings.get().ui().isEmojis_full_screen) {
                    val layoutParams = emojiContainer?.layoutParams
                    if (keyboardTmpHeight != layoutParams?.height) {
                        keyboardTmpHeight = keyBoardHeight
                        layoutParams?.height = keyBoardHeight
                        emojiContainer?.layoutParams = layoutParams
                    }
                }
                if (!isKeyBoardOpen) {
                    onSoftKeyboardOpenCloseListener?.onKeyboardOpen()
                }
                isKeyBoardOpen = true
            } else {
                isKeyBoardOpen = false
                onSoftKeyboardOpenCloseListener?.onKeyboardClose()
            }
        }
    }
    private var emojisPager: ViewPager2? = null
    fun storeState() {
        if (emojisPager != null) {
            getPreferences(mContext)
                .edit()
                .putInt(KEY_PAGE, emojisPager?.currentItem ?: 0)
                .apply()
        }
    }

    fun setMyOnStickerClickedListener(onMyStickerClickedListener: OnMyStickerClickedListener?) {
        this.onMyStickerClickedListener = onMyStickerClickedListener
    }

    private fun listenKeyboardSize() {
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    fun getEmojiView(emojiParentView: ViewGroup): View? {
        if (emojiContainer == null) {
            emojiContainer = createCustomView(emojiParentView)
            val finalKeyboardHeight = when {
                Settings.get()
                    .ui().isEmojis_full_screen -> mContext.resources.getDimension(
                    R.dimen.keyboard_max_height
                ).toInt()

                keyBoardHeight > 0 -> keyBoardHeight
                else -> mContext.resources.getDimension(
                    R.dimen.keyboard_height
                ).toInt()
            }
            val layoutParams = emojiContainer?.layoutParams
            if (keyboardTmpHeight != layoutParams?.height) {
                keyboardTmpHeight = finalKeyboardHeight
                layoutParams?.height = finalKeyboardHeight
            }
            emojiContainer?.layoutParams = layoutParams
        }
        return emojiContainer
    }

    private fun createCustomView(parent: ViewGroup): View {
        val accountId = Settings.get()
            .accounts()
            .current
        val stickerSets = InteractorFactory.createStickersInteractor()
            .getStickerSets(accountId)
            .blockingGet()
        val view = LayoutInflater.from(mContext).inflate(R.layout.emojicons, parent, false)
        emojisPager = view.findViewById(R.id.emojis_pager)
        val views = listOf(
            People.DATA,
            Nature.DATA,
            Food.DATA,
            Sport.DATA,
            Cars.DATA,
            Electronics.DATA,
            Symbols.DATA
        )
        val sections: MutableList<AbsSection> = ArrayList()
        sections.add(
            EmojiSection(
                EmojiSection.TYPE_PEOPLE,
                getTintedDrawable(R.drawable.ic_emoji_people_vector)
            )
        )
        sections.add(
            EmojiSection(
                EmojiSection.TYPE_NATURE,
                getTintedDrawable(R.drawable.pine_tree)
            )
        )
        sections.add(EmojiSection(EmojiSection.TYPE_FOOD, getTintedDrawable(R.drawable.pizza)))
        sections.add(EmojiSection(EmojiSection.TYPE_SPORT, getTintedDrawable(R.drawable.bike)))
        sections.add(EmojiSection(EmojiSection.TYPE_CARS, getTintedDrawable(R.drawable.car)))
        sections.add(
            EmojiSection(
                EmojiSection.TYPE_ELECTRONICS,
                getTintedDrawable(R.drawable.laptop_chromebook)
            )
        )
        sections.add(
            EmojiSection(
                EmojiSection.TYPE_SYMBOLS,
                getTintedDrawable(R.drawable.pound_box)
            )
        )
        sections.add(
            EmojiSection(
                EmojiSection.TYPE_MY_STICKERS,
                getTintedDrawable(R.drawable.dir_sticker)
            )
        )
        val stickersGridViews: MutableList<StickerSet> = ArrayList()
        for (stickerSet in stickerSets) {
            stickersGridViews.add(stickerSet)
            sections.add(StickerSection(stickerSet))
        }
        val mEmojisAdapter = EmojisPagerAdapter(views, stickersGridViews, this)
        emojisPager?.adapter = mEmojisAdapter
        val storedPage = getPreferences(mContext).getInt(KEY_PAGE, 0)
        if (mEmojisAdapter.itemCount > storedPage) {
            emojisPager?.currentItem = storedPage
        }
        val recyclerView: RecyclerView = view.findViewById(R.id.recycleView)
        val manager = LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = manager
        sections[emojisPager?.currentItem ?: 0].active = true
        val topSectionAdapter = SectionsAdapter(sections, mContext)
        recyclerView.adapter = topSectionAdapter
        view.findViewById<View>(R.id.backspace)
            .setOnTouchListener(RepeatListener(700, 50) { v: View ->
                onEmojiconBackspaceClickedListener?.onEmojiconBackspaceClicked(v)
            })
        topSectionAdapter.setListener(object : SectionsAdapter.Listener {
            override fun onClick(position: Int) {
                emojisPager?.currentItem = position
            }
        })
        emojisPager?.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                var oldSelectionIndex = -1
                for (i in sections.indices) {
                    val section = sections[i]
                    if (section.active) {
                        oldSelectionIndex = i
                    }
                    section.active = position == i
                }
                topSectionAdapter.notifyItemChanged(position)
                if (oldSelectionIndex != -1) {
                    topSectionAdapter.notifyItemChanged(oldSelectionIndex)
                }
                manager.scrollToPosition(position)
            }
        })
        return view
    }

    private fun getTintedDrawable(resourceID: Int): Drawable {
        val drawable = ContextCompat.getDrawable(mContext, resourceID)
        drawable?.setTint(CurrentTheme.getColorOnSurface(mContext))
        return drawable!!
    }

    fun destroy() {
        storeState()
        rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        rootView = null
    }

    interface OnStickerClickedListener {
        fun onStickerClick(sticker: Sticker)
    }

    interface OnMyStickerClickedListener {
        fun onMyStickerClick(file: LocalSticker)
    }

    interface OnEmojiconClickedListener {
        fun onEmojiconClicked(emojicon: Emojicon)
    }

    interface OnEmojiconBackspaceClickedListener {
        fun onEmojiconBackspaceClicked(v: View)
    }

    interface OnSoftKeyboardOpenCloseListener {
        fun onKeyboardOpen()
        fun onKeyboardClose()
    }

    private class Holder(rootView: View) : RecyclerView.ViewHolder(
        rootView
    )

    private class EmojisPagerAdapter constructor(
        private val views: List<Array<Emojicon>>,
        private val stickersGridViews: List<StickerSet>,
        private val mEmojiconPopup: EmojiconsPopup
    ) : RecyclerView.Adapter<Holder>() {
        override fun getItemViewType(position: Int): Int {
            return when (position) {
                0, 1, 2, 3, 4, 5, 6 -> 0
                7 -> 2
                else -> 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val res: Int = when (viewType) {
                0 -> R.layout.emojicon_grid
                2 -> R.layout.my_stickers_grid
                else -> R.layout.stickers_grid
            }
            return Holder(LayoutInflater.from(parent.context).inflate(res, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            when (position) {
                0, 1, 2, 3, 4, 5, 6 -> {
                    val gridView = holder.itemView.findViewById<GridView>(R.id.Emoji_GridView)
                    val mData: Array<Emojicon> = views[position].clone()
                    val mAdapter = EmojiAdapter(holder.itemView.context, mData)
                    mAdapter.setEmojiClickListener(object : OnEmojiconClickedListener {
                        override fun onEmojiconClicked(emojicon: Emojicon) {
                            mEmojiconPopup.onEmojiconClickedListener?.onEmojiconClicked(emojicon)
                        }

                    })
                    gridView.adapter = mAdapter
                }

                7 -> {
                    val recyclerView: RecyclerView =
                        holder.itemView.findViewById(R.id.grid_stickers)
                    PicassoPauseOnScrollListener.addListener(recyclerView)
                    val myStickersAdapter = MyStickersAdapter(holder.itemView.context)
                    myStickersAdapter.setMyStickerClickedListener(object :
                        OnMyStickerClickedListener {
                        override fun onMyStickerClick(file: LocalSticker) {
                            mEmojiconPopup.onMyStickerClickedListener?.onMyStickerClick(file)
                        }
                    })
                    val gridMyLayoutManager = GridLayoutManager(holder.itemView.context, 4)
                    recyclerView.layoutManager = gridMyLayoutManager
                    (holder.itemView.findViewById<View>(R.id.header_sticker) as TextView).text =
                        recyclerView.context.getString(R.string.my)
                    recyclerView.adapter = myStickersAdapter
                }

                else -> {
                    val recyclerView: RecyclerView =
                        holder.itemView.findViewById(R.id.grid_stickers)
                    PicassoPauseOnScrollListener.addListener(recyclerView)
                    val mAdaptert =
                        StickersAdapter(holder.itemView.context, stickersGridViews[position - 8])
                    mAdaptert.setStickerClickedListener(object : OnStickerClickedListener {
                        override fun onStickerClick(sticker: Sticker) {
                            mEmojiconPopup.onStickerClickedListener?.onStickerClick(sticker)
                        }
                    })
                    val gridLayoutManager = GridLayoutManager(holder.itemView.context, 4)
                    recyclerView.layoutManager = gridLayoutManager
                    var title = stickersGridViews[position - 8].getTitle()
                    if (title.nonNullNoEmpty() && title == "recent") title =
                        recyclerView.context.getString(R.string.usages)
                    (holder.itemView.findViewById<View>(R.id.header_sticker) as TextView).text =
                        title
                    recyclerView.adapter = mAdaptert
                }
            }
        }

        override fun getItemCount(): Int {
            return views.size + stickersGridViews.size + 1
        }
    }

    /**
     * A class, that can be used as a TouchListener on any view (e.g. a Button).
     * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
     * click is fired immediately, next before initialInterval, and subsequent before
     * normalInterval.
     *
     *
     *
     * Interval is scheduled before the onClick completes, so it has to run fast.
     * If it runs slow, it does not generate skipped onClicks.
     */
    class RepeatListener(
        initialInterval: Int,
        normalInterval: Int,
        clickListener: View.OnClickListener?
    ) : OnTouchListener {
        private val normalInterval: Int
        private val clickListener: View.OnClickListener
        private val handler = Handler(Looper.getMainLooper())
        private val initialInterval: Int
        private var downView: View? = null
        private val handlerRunnable: Runnable = object : Runnable {
            override fun run() {
                if (downView == null) {
                    return
                }
                handler.removeCallbacksAndMessages(downView)
                handler.postAtTime(this, downView, SystemClock.uptimeMillis() + normalInterval)
                clickListener?.onClick(downView)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    downView = view
                    handler.removeCallbacks(handlerRunnable)
                    handler.postAtTime(
                        handlerRunnable,
                        downView,
                        SystemClock.uptimeMillis() + initialInterval
                    )
                    clickListener.onClick(view)
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    handler.removeCallbacksAndMessages(downView)
                    downView = null
                    return true
                }
            }
            return false
        }

        init {
            requireNotNull(clickListener) { "null runnable" }
            require(!(initialInterval < 0 || normalInterval < 0)) { "negative interval" }
            this.initialInterval = initialInterval
            this.normalInterval = normalInterval
            this.clickListener = clickListener
        }
    }

    companion object {
        private const val KEY_PAGE = "emoji_page"
        fun input(editText: TextInputEditText?, emojicon: Emojicon?) {
            if (editText == null || emojicon == null) {
                return
            }
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if (start < 0) {
                editText.append(emojicon.emoji)
            } else {
                editText.text?.replace(
                    start.coerceAtMost(end),
                    start.coerceAtLeast(end),
                    emojicon.emoji,
                    0,
                    emojicon.emoji?.length ?: 0
                )
            }
        }

        fun backspace(editText: TextInputEditText) {
            val event =
                KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL)
            editText.dispatchKeyEvent(event)
        }
    }

    init {
        listenKeyboardSize()
    }
}