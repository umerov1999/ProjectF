package dev.ragnarok.fenrir.settings.theme

import android.graphics.Color
import androidx.annotation.StyleRes

class ThemeValue {
    val id: String
    val name: String
    val colorDayPrimary: Int
    val colorDaySecondary: Int
    val colorNightPrimary: Int
    val colorNightSecondary: Int
    var colorToast: Int
    var colorReadToast: Int
    var disabled: Boolean
    var special: Boolean

    @StyleRes
    val themeRes: Int

    @StyleRes
    val themeAmoledRes: Int

    @StyleRes
    val themeMD1Res: Int

    constructor(
        id: String,
        colorPrimary: String,
        colorSecondary: String,
        name: String,
        @StyleRes themeRes: Int,
        @StyleRes themeAmoledRes: Int,
        @StyleRes themeMD1Res: Int
    ) {
        colorDayPrimary = Color.parseColor(colorPrimary)
        colorDaySecondary = Color.parseColor(colorSecondary)
        colorNightPrimary = colorDayPrimary
        colorNightSecondary = colorDaySecondary
        colorToast = Color.parseColor(colorPrimary)
        colorReadToast = Color.parseColor(colorPrimary)
        disabled = false
        special = false
        this.id = id
        this.name = name
        this.themeRes = themeRes
        this.themeAmoledRes = themeAmoledRes
        this.themeMD1Res = themeMD1Res
    }

    constructor(
        id: String,
        colorDayPrimary: String,
        colorDaySecondary: String,
        colorNightPrimary: String,
        colorNightSecondary: String,
        name: String,
        @StyleRes themeRes: Int,
        @StyleRes themeAmoledRes: Int,
        @StyleRes themeMD1Res: Int
    ) {
        this.colorDayPrimary = Color.parseColor(colorDayPrimary)
        this.colorDaySecondary = Color.parseColor(colorDaySecondary)
        this.colorNightPrimary = Color.parseColor(colorNightPrimary)
        this.colorNightSecondary = Color.parseColor(colorNightSecondary)
        colorToast = Color.parseColor(colorDayPrimary)
        colorReadToast = Color.parseColor(colorDayPrimary)
        disabled = false
        special = false
        this.id = id
        this.name = name
        this.themeRes = themeRes
        this.themeAmoledRes = themeAmoledRes
        this.themeMD1Res = themeMD1Res
    }

    fun toast(colorToast: String): ThemeValue {
        this.colorToast = Color.parseColor(colorToast)
        this.colorReadToast = Color.parseColor(colorToast)
        return this
    }

    fun toast(colorToast: Int): ThemeValue {
        this.colorToast = colorToast
        this.colorReadToast = colorToast
        return this
    }

    fun toast(colorToast: String, colorReadToast: String): ThemeValue {
        this.colorToast = Color.parseColor(colorToast)
        this.colorReadToast = Color.parseColor(colorReadToast)
        return this
    }

    fun toast(colorToast: Int, colorReadToast: Int): ThemeValue {
        this.colorToast = colorToast
        this.colorReadToast = colorReadToast
        return this
    }

    fun enable(bEnable: Boolean): ThemeValue {
        disabled = !bEnable
        return this
    }

    fun specialised(bSpecial: Boolean): ThemeValue {
        special = bSpecial
        return this
    }
}
