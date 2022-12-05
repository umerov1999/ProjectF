package dev.ragnarok.fenrir.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class FunctionSource {
    private val title: Text
    private val call: () -> Unit

    @DrawableRes
    private val icon: Int

    constructor(title: String?, @DrawableRes icon: Int, call: () -> Unit) {
        this.title = Text(title)
        this.call = call
        this.icon = icon

    }

    constructor(@StringRes title: Int, @DrawableRes icon: Int, call: () -> Unit) {
        this.title = Text(title)
        this.call = call
        this.icon = icon
    }

    fun getTitle(context: Context?): String? {
        return if (context == null) {
            null
        } else title.getText(context)
    }

    fun Do() {
        call.invoke()
    }

    @DrawableRes
    fun getIcon(): Int {
        return icon
    }
}