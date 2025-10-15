package dev.ragnarok.fenrir.view

import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView

class LoadMoreFooterHelper {
    private var callback: Callback? = null
    private var holder: Holder? = null
    private var state = LoadMoreState.INVISIBLE
    private var animation_id = 0
    fun switchToState(@LoadMoreState state: Int) {
        if (this.state == state) {
            return
        }
        this.state = state
        holder?.container?.visibility =
            if (state == LoadMoreState.INVISIBLE) View.GONE else View.VISIBLE
        when (state) {
            LoadMoreState.LOADING -> {
                holder?.tvEndOfList?.setImageDrawable(null)
                holder?.tvEndOfList?.visibility = View.INVISIBLE
                holder?.currentLoadMore?.visibility = View.INVISIBLE
                holder?.progress?.visibility = View.VISIBLE
            }

            LoadMoreState.END_OF_LIST -> {
                holder?.tvEndOfList?.visibility = View.VISIBLE
                holder?.tvEndOfList?.setRepeat(false)
                when (animation_id) {
                    0 -> {
                        holder?.tvEndOfList?.setRepeat(false)
                        holder?.tvEndOfList?.fromRes(
                            dev.ragnarok.fenrir_common.R.raw.end_list_succes,
                            intArrayOf(
                                0xffffff, CurrentTheme.getColorControlNormal(
                                    holder?.currentLoadMore?.context
                                )
                            )
                        )
                    }

                    1 -> {
                        holder?.tvEndOfList?.setRepeat(false)
                        holder?.tvEndOfList?.fromRes(
                            dev.ragnarok.fenrir_common.R.raw.end_list_balls,
                            intArrayOf(
                                0xffffff, CurrentTheme.getColorControlNormal(
                                    holder?.currentLoadMore?.context
                                )
                            )
                        )
                    }

                    else -> {
                        holder?.tvEndOfList?.setRepeat(true)
                        holder?.tvEndOfList?.fromRes(
                            dev.ragnarok.fenrir_common.R.raw.end_list_wave,
                            intArrayOf(
                                0x777777, CurrentTheme.getColorPrimary(
                                    holder?.currentLoadMore?.context
                                ), 0x333333, CurrentTheme.getColorSecondary(
                                    holder?.currentLoadMore?.context
                                )
                            )
                        )
                    }
                }
                holder?.tvEndOfList?.startAnimation()
                holder?.currentLoadMore?.visibility = View.INVISIBLE
                holder?.progress?.visibility = View.INVISIBLE
            }

            LoadMoreState.CAN_LOAD_MORE -> {
                holder?.tvEndOfList?.setImageDrawable(null)
                holder?.tvEndOfList?.visibility = View.INVISIBLE
                holder?.currentLoadMore?.visibility = View.VISIBLE
                holder?.progress?.visibility = View.INVISIBLE
            }

            LoadMoreState.INVISIBLE -> {}
        }
    }

    fun updateLoadMoreButton(isFab: Boolean) {
        holder?.updateLoadMoreButton(isFab)
    }

    interface Callback {
        fun onLoadMoreClick()
    }

    class Holder(root: View) {
        val container: View = root.findViewById(R.id.footer_load_more_root)
        val progress: CircularProgressIndicator = root.findViewById(R.id.footer_load_more_progress)
        val bLoadMoreButton: MaterialButton = root.findViewById(R.id.footer_load_more_run)
        val bLoadMoreFab: FloatingActionButton = root.findViewById(R.id.footer_load_more_run_fab)
        val tvEndOfList: ThorVGLottieView = root.findViewById(R.id.footer_load_more_end_of_list)

        var currentLoadMore: View = bLoadMoreButton

        fun updateLoadMoreButton(isFab: Boolean) {
            if (isFab) {
                currentLoadMore = bLoadMoreFab
                bLoadMoreFab.visibility = View.INVISIBLE
                bLoadMoreButton.visibility = View.GONE
            } else {
                currentLoadMore = bLoadMoreButton
                bLoadMoreFab.visibility = View.GONE
                bLoadMoreButton.visibility = View.INVISIBLE
            }
        }
    }

    companion object {
        fun createFrom(view: View?, callback: Callback?): LoadMoreFooterHelper? {
            view ?: return null
            val helper = LoadMoreFooterHelper()
            helper.animation_id = Settings.get().main().endListAnimation
            helper.holder = Holder(view)
            helper.callback = callback
            helper.holder?.bLoadMoreButton?.setOnClickListener { callback?.onLoadMoreClick() }
            helper.holder?.bLoadMoreFab?.setOnClickListener { callback?.onLoadMoreClick() }
            return helper
        }
    }
}