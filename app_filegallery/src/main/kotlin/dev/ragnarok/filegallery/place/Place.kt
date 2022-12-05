package dev.ragnarok.filegallery.place

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentResultListener

open class Place : Parcelable {
    val type: Int
    var isNeedFinishMain = false
        private set
    private var requestListenerKey: String? = null
    private var requestListener: FragmentResultListener? = null
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var args: Bundle? = null

    constructor(type: Int) {
        this.type = type
    }

    protected constructor(p: Parcel) {
        type = p.readInt()
        args = p.readBundle(javaClass.classLoader)
    }

    fun tryOpenWith(context: Context) {
        if (context is PlaceProvider) {
            (context as PlaceProvider).openPlace(this)
        }
    }

    fun setFragmentListener(
        requestListenerKey: String,
        requestListener: FragmentResultListener
    ): Place {
        this.requestListenerKey = requestListenerKey
        this.requestListener = requestListener
        return this
    }

    fun setActivityResultLauncher(activityResultLauncher: ActivityResultLauncher<Intent>): Place {
        this.activityResultLauncher = activityResultLauncher
        return this
    }

    fun setNeedFinishMain(needFinishMain: Boolean): Place {
        isNeedFinishMain = needFinishMain
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeBundle(args)
    }

    fun setArguments(arguments: Bundle?): Place {
        args = arguments
        return this
    }

    fun withStringExtra(name: String, value: String?): Place {
        prepareArguments().putString(name, value)
        return this
    }

    fun withParcelableExtra(name: String, parcelableExtra: Parcelable?): Place {
        prepareArguments().putParcelable(name, parcelableExtra)
        return this
    }

    fun withIntExtra(name: String, value: Int): Place {
        prepareArguments().putInt(name, value)
        return this
    }

    fun withLongExtra(name: String, value: Long): Place {
        prepareArguments().putLong(name, value)
        return this
    }

    fun prepareArguments(): Bundle {
        if (args == null) {
            args = Bundle()
        }
        return args!!
    }

    fun safeArguments(): Bundle {
        return args ?: Bundle()
    }

    fun applyFragmentListener(fragment: Fragment, fragmentManager: FragmentManager) {
        requestListener?.let {
            requestListenerKey?.let { it1 ->
                fragmentManager.setFragmentResultListener(
                    it1,
                    fragment,
                    it
                )
            }
        }
    }

    fun launchActivityForResult(context: Activity, intent: Intent) {
        if (activityResultLauncher != null && !isNeedFinishMain) {
            activityResultLauncher?.launch(intent)
        } else {
            context.startActivity(intent)
            if (isNeedFinishMain) {
                context.finish()
                context.overridePendingTransition(0, 0)
            }
        }
    }

    companion object {
        const val FILE_MANAGER = 1
        const val PREFERENCES = 2
        const val SETTINGS_THEME = 3
        const val AUDIO_PLAYER = 4
        const val PHOTO_LOCAL = 5
        const val PHOTO_LOCAL_SERVER = 6
        const val LOCAL_MEDIA_SERVER = 7
        const val VIDEO_PLAYER = 8
        const val TAGS = 9
        const val TAG_DIRS = 10
        const val SECURITY = 11

        @JvmField
        val CREATOR: Parcelable.Creator<Place> = object : Parcelable.Creator<Place> {
            override fun createFromParcel(p: Parcel): Place {
                return Place(p)
            }

            override fun newArray(size: Int): Array<Place?> {
                return arrayOfNulls(size)
            }
        }
    }
}
