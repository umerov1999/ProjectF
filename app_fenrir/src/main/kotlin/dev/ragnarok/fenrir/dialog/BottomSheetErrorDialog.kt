package dev.ragnarok.fenrir.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.media.exo.ExoUtil
import dev.ragnarok.fenrir.util.Utils.makeMediaItem
import dev.ragnarok.fenrir.util.toast.CustomToast

@OptIn(UnstableApi::class)
class BottomSheetErrorDialog : BottomSheetDialogFragment() {
    var mCurrentMediaPlayer: ExoPlayer? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            View.inflate(requireActivity(), R.layout.dialog_bottomsheet_error, null)
        val title = requireArguments().getString(Extra.TITLE)
        val description = requireArguments().getString(Extra.DATA)

        val mTitle = view.findViewById<MaterialTextView>(R.id.item_error_title)
        val mStacktrace = view.findViewById<MaterialTextView>(R.id.item_error_stacktrace)
        val mButtonCopy = view.findViewById<ExtendedFloatingActionButton>(R.id.item_button_copy)

        mTitle.text = getString(R.string.error_title, title)
        mStacktrace.text = getString(R.string.error_stacktrace, description)

        mButtonCopy.setOnClickListener {
            val clipboard =
                requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            if (clipboard != null) {
                val clip = ClipData.newPlainText(
                    "errors",
                    "$title: {$description}"
                )
                clipboard.setPrimaryClip(clip)
                CustomToast.createCustomToast(requireActivity())
                    .showToastInfo(R.string.crash_error_activity_error_details_copied)
            }
            dismiss()
        }
        if (savedInstanceState == null) {
            mCurrentMediaPlayer = ExoPlayer.Builder(
                requireActivity(), DefaultRenderersFactory(requireActivity())
            ).build()

            val source =
                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(requireActivity()))
                    .createMediaSource(makeMediaItem("file:///android_asset/error.ogg"))
            mCurrentMediaPlayer?.setMediaSource(source)
            mCurrentMediaPlayer?.prepare()
            mCurrentMediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(), true
            )
            ExoUtil.startPlayer(mCurrentMediaPlayer)
        }
        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        mCurrentMediaPlayer?.release()
    }
}
