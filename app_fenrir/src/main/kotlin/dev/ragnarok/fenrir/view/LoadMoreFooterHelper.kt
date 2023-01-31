package dev.ragnarok.fenrir.view

import android.view.View
import com.google.android.material.progressindicator.CircularProgressIndicator
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class LoadMoreFooterHelper {
    var callback: Callback? = null
    var holder: Holder? = null
    var state = LoadMoreState.INVISIBLE
    var animation_id = 0
    fun switchToState(@LoadMoreState state: Int) {
        this.state = state
        holder?.container?.visibility =
            if (state == LoadMoreState.INVISIBLE) View.GONE else View.VISIBLE
        when (state) {
            LoadMoreState.LOADING -> {
                holder?.tvEndOfList?.setImageDrawable(null)
                holder?.tvEndOfList?.visibility = View.INVISIBLE
                holder?.bLoadMore?.visibility = View.INVISIBLE
                holder?.progress?.visibility = View.VISIBLE
            }

            LoadMoreState.END_OF_LIST -> {
                holder?.tvEndOfList?.visibility = View.VISIBLE
                holder?.tvEndOfList?.setAutoRepeat(false)
                when (animation_id) {
                    0 -> {
                        holder?.tvEndOfList?.setAutoRepeat(false)
                        holder?.tvEndOfList?.fromRes(
                            dev.ragnarok.fenrir_common.R.raw.end_list_succes,
                            Utils.dp(40f),
                            Utils.dp(40f),
                            intArrayOf(
                                0xffffff, CurrentTheme.getColorControlNormal(
                                    holder?.bLoadMore?.context
                                )
                            )
                        )
                    }

                    1 -> {
                        holder?.tvEndOfList?.setAutoRepeat(false)
                        holder?.tvEndOfList?.fromRes(
                            dev.ragnarok.fenrir_common.R.raw.end_list_balls,
                            Utils.dp(40f),
                            Utils.dp(40f),
                            intArrayOf(
                                0xffffff, CurrentTheme.getColorControlNormal(
                                    holder?.bLoadMore?.context
                                )
                            )
                        )
                    }

                    else -> {
                        holder?.tvEndOfList?.setAutoRepeat(true)
                        holder?.tvEndOfList?.fromRes(
                            dev.ragnarok.fenrir_common.R.raw.end_list_wave,
                            Utils.dp(80f),
                            Utils.dp(40f),
                            intArrayOf(
                                0x777777, CurrentTheme.getColorPrimary(
                                    holder?.bLoadMore?.context
                                ), 0x333333, CurrentTheme.getColorSecondary(
                                    holder?.bLoadMore?.context
                                )
                            )
                        )
                    }
                }
                holder?.tvEndOfList?.playAnimation()
                holder?.bLoadMore?.visibility = View.INVISIBLE
                holder?.progress?.visibility = View.INVISIBLE
            }

            LoadMoreState.CAN_LOAD_MORE -> {
                holder?.tvEndOfList?.setImageDrawable(null)
                holder?.tvEndOfList?.visibility = View.INVISIBLE
                holder?.bLoadMore?.visibility = View.VISIBLE
                holder?.progress?.visibility = View.INVISIBLE
            }

            LoadMoreState.INVISIBLE -> {}
        }
    }

    interface Callback {
        fun onLoadMoreClick()
    }

    class Holder(root: View) {
        val container: View = root.findViewById(R.id.footer_load_more_root)
        val progress: CircularProgressIndicator = root.findViewById(R.id.footer_load_more_progress)
        val bLoadMore: View = root.findViewById(R.id.footer_load_more_run)
        val tvEndOfList: RLottieImageView = root.findViewById(R.id.footer_load_more_end_of_list)

    }

    companion object {

        fun createFrom(view: View?, callback: Callback?): LoadMoreFooterHelper? {
            view ?: return null
            val helper = LoadMoreFooterHelper()
            helper.animation_id = Settings.get().other().endListAnimation
            helper.holder = Holder(view)
            helper.callback = callback
            helper.holder?.bLoadMore?.setOnClickListener { callback?.onLoadMoreClick() }
            return helper
        }
    }
}