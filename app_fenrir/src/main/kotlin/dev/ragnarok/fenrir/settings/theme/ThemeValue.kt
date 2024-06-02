package dev.ragnarok.fenrir.settings.theme

import androidx.annotation.StyleRes
import dev.ragnarok.fenrir.toColor

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
        colorDayPrimary = colorPrimary.toColor()
        colorDaySecondary = colorSecondary.toColor()
        colorNightPrimary = colorDayPrimary
        colorNightSecondary = colorDaySecondary
        colorToast = colorPrimary.toColor()
        colorReadToast = colorPrimary.toColor()
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
        this.colorDayPrimary = colorDayPrimary.toColor()
        this.colorDaySecondary = colorDaySecondary.toColor()
        this.colorNightPrimary = colorNightPrimary.toColor()
        this.colorNightSecondary = colorNightSecondary.toColor()
        colorToast = colorDayPrimary.toColor()
        colorReadToast = colorDayPrimary.toColor()
        disabled = false
        special = false
        this.id = id
        this.name = name
        this.themeRes = themeRes
        this.themeAmoledRes = themeAmoledRes
        this.themeMD1Res = themeMD1Res
    }

    fun toast(colorToast: String): ThemeValue {
        this.colorToast = colorToast.toColor()
        this.colorReadToast = colorToast.toColor()
        return this
    }

    fun toast(colorToast: Int): ThemeValue {
        this.colorToast = colorToast
        this.colorReadToast = colorToast
        return this
    }

    fun toast(colorToast: String, colorReadToast: String): ThemeValue {
        this.colorToast = colorToast.toColor()
        this.colorReadToast = colorReadToast.toColor()
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
