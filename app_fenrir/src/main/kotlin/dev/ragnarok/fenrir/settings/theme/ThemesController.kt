package dev.ragnarok.fenrir.settings.theme

import androidx.annotation.StyleRes
import com.google.android.material.color.DynamicColors
import dev.ragnarok.fenrir.Common
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.Settings

object ThemesController {
    val themes = arrayOf(
        ThemeValue(
            "cold",
            "#346096",
            "#545F70",
            "#A4C9FF",
            "#BCC7DB",
            "Cold",
            R.style.App_DayNight_Cold,
            R.style.App_DayNight_Cold_Amoled,
            R.style.App_DayNight_Cold_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "cold_gold",
            "#9BA0FC",
            "#FDDCB8",
            "Cold Gold",
            R.style.App_DayNight_ColdGold,
            R.style.App_DayNight_ColdGold_Amoled,
            R.style.App_DayNight_ColdGold_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "cold_green",
            "#556518",
            "#5C6145",
            "#BDCF76",
            "#C4CAA8",
            "Cold Green",
            R.style.App_DayNight_ColdGreen,
            R.style.App_DayNight_ColdGreen_Amoled,
            R.style.App_DayNight_ColdGreen_MD1
        ).toast("#556518", "#448AFF"),
        ThemeValue(
            "coffee_pistachio",
            "#9B5D56",
            "#8EBD9D",
            "#E0BCA0",
            "#BEDAC3",
            "Coffee Pistachio",
            R.style.App_DayNight_CoffeePistachio,
            R.style.App_DayNight_CoffeePistachio_Amoled,
            R.style.App_DayNight_CoffeePistachio_MD1
        ).toast("#9B5D56", "#448AFF"),
        ThemeValue(
            "ice",
            "#448AFF",
            "#1E88E5",
            "Ice",
            R.style.App_DayNight_Ice,
            R.style.App_DayNight_Ice_Amoled,
            R.style.App_DayNight_Ice_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "old_ice",
            "#448AFF",
            "#82B1FF",
            "Old Ice",
            R.style.App_DayNight_OldIce,
            R.style.App_DayNight_OldIce_Amoled,
            R.style.App_DayNight_OldIce_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "fire",
            "#FF9800",
            "#FFA726",
            "Fire",
            R.style.App_DayNight_Fire,
            R.style.App_DayNight_Fire_Amoled,
            R.style.App_DayNight_Fire_MD1,
        ),
        ThemeValue(
            "red",
            "#FF0000",
            "#F44336",
            "Red",
            R.style.App_DayNight_Red,
            R.style.App_DayNight_Red_Amoled,
            R.style.App_DayNight_Red_MD1
        ),
        ThemeValue(
            "violet",
            "#9800FF",
            "#8500FF",
            "Violet",
            R.style.App_DayNight_Violet,
            R.style.App_DayNight_Violet_Amoled,
            R.style.App_DayNight_Violet_MD1
        ),
        ThemeValue(
            "gray",
            "#444444",
            "#777777",
            "Gray",
            R.style.App_DayNight_Gray,
            R.style.App_DayNight_Gray_Amoled,
            R.style.App_DayNight_Gray_MD1
        ),
        ThemeValue(
            "yellow_violet",
            "#FF9800",
            "#8500FF",
            "Fire Violet",
            R.style.App_DayNight_YellowViolet,
            R.style.App_DayNight_YellowViolet_Amoled,
            R.style.App_DayNight_YellowViolet_MD1
        ),
        ThemeValue(
            "red_violet",
            "#F44336",
            "#9800FF",
            "Red Violet",
            R.style.App_DayNight_RedViolet,
            R.style.App_DayNight_RedViolet_Amoled,
            R.style.App_DayNight_RedViolet_MD1
        ),
        ThemeValue(
            "contrast",
            "#000000",
            "#444444",
            "#ffffff",
            "#777777",
            "Contrast",
            R.style.App_DayNight_Contrast,
            R.style.App_DayNight_Contrast_Amoled,
            R.style.App_DayNight_Contrast_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "orange",
            "#FF5722",
            "#FF6F00",
            "Orange",
            R.style.App_DayNight_Orange,
            R.style.App_DayNight_Orange_Amoled,
            R.style.App_DayNight_Orange_MD1
        ),
        ThemeValue(
            "orange_gray",
            "#FF5722",
            "#777777",
            "Orange Gray",
            R.style.App_DayNight_OrangeGray,
            R.style.App_DayNight_OrangeGray_Amoled,
            R.style.App_DayNight_OrangeGray_MD1
        ),
        ThemeValue(
            "violet_gray",
            "#8500FF",
            "#777777",
            "Violet Gray",
            R.style.App_DayNight_VioletGray,
            R.style.App_DayNight_VioletGray_Amoled,
            R.style.App_DayNight_VioletGray_MD1
        ),
        ThemeValue(
            "pink_gray",
            "#FF4F8B",
            "#777777",
            "Pink Gray",
            R.style.App_DayNight_PinkGray,
            R.style.App_DayNight_PinkGray_Amoled,
            R.style.App_DayNight_PinkGray_MD1
        ),
        ThemeValue(
            "violet_ice",
            "#757AFF",
            "#50F2C4",
            "Violet Ice",
            R.style.App_DayNight_VioletIce,
            R.style.App_DayNight_VioletIce_Amoled,
            R.style.App_DayNight_VioletIce_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "ice_green",
            "#448AFF",
            "#4CAF50",
            "Ice Green",
            R.style.App_DayNight_IceGreen,
            R.style.App_DayNight_IceGreen_Amoled,
            R.style.App_DayNight_IceGreen_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "blue_red",
            "#448AFF",
            "#FF0000",
            "Blue Red",
            R.style.App_DayNight_BlueRed,
            R.style.App_DayNight_BlueRed_Amoled,
            R.style.App_DayNight_BlueRed_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "green",
            "#268000",
            "#4CAF50",
            "Green",
            R.style.App_DayNight_Green,
            R.style.App_DayNight_Green_Amoled,
            R.style.App_DayNight_Green_MD1
        ).toast("#4D7198", "#448AFF"),
        ThemeValue(
            "lineage",
            "#167C80",
            "#63FFDE",
            "Lineage",
            R.style.App_DayNight_Lineage,
            R.style.App_DayNight_Lineage_Amoled,
            R.style.App_DayNight_Lineage_MD1
        ),
        ThemeValue(
            "fuxia_neon_yellow",
            "#FE59C2",
            "#CFFF04",
            "Fuxia Neon Yellow",
            R.style.App_DayNight_FuxiaNeonYellow,
            R.style.App_DayNight_FuxiaNeonYellow_Amoled,
            R.style.App_DayNight_FuxiaNeonYellow_MD1
        ),
        ThemeValue(
            "fuxia_neon_violet",
            "#FE4164",
            "#BC13FE",
            "Fuxia Neon Violet",
            R.style.App_DayNight_FuxiaNeonViolet,
            R.style.App_DayNight_FuxiaNeonViolet_Amoled,
            R.style.App_DayNight_FuxiaNeonViolet_MD1
        ),
        ThemeValue(
            "neon_yellow_ice",
            "#AAD300",
            "#04D9FF",
            "Neon Yellow Ice",
            R.style.App_DayNight_NeonYellowIce,
            R.style.App_DayNight_NeonYellowIce_Amoled,
            R.style.App_DayNight_NeonYellowIce_MD1
        ),
        ThemeValue(
            "random",
            "#ffffff",
            "#ffffff",
            "Random",
            R.style.App_DayNight_Ice,
            R.style.App_DayNight_Ice_Amoled,
            R.style.App_DayNight_Ice_MD1
        ).specialised(true),
        ThemeValue(
            "dynamic",
            "#ffffff",
            "#ffffff",
            "Dynamic",
            R.style.App_DayNight_Dynamic,
            R.style.App_DayNight_Dynamic_Amoled,
            R.style.App_DayNight_Dynamic_MD1
        ).specialised(true).toast("#4D7198", "#448AFF")
            .enable(DynamicColors.isDynamicColorAvailable()),
    )
    private var randomTheme = themes.random()
    private val defaultTheme = ThemeValue(
        "cold",
        "#9BC4FC",
        "#D8C2FF",
        "Cold",
        R.style.App_DayNight_Cold,
        R.style.App_DayNight_Cold_Amoled,
        R.style.App_DayNight_Cold_MD1
    ).toast("#4D7198", "#448AFF")

    private fun getCurrentTheme(): ThemeValue {
        val key: String = Settings.get().ui().mainThemeKey

        if (key == "random") {
            return randomTheme
        }
        for (i in themes) {
            if (i.id == key) {
                return i
            }
        }
        return defaultTheme
    }

    fun nextRandom() {
        randomTheme = themes.random()
        Common.randomizePaganSymbol()
    }

    @StyleRes
    fun currentStyle(): Int {
        val t = getCurrentTheme()
        return when (Settings.get().main().themeOverlay) {
            ThemeOverlay.AMOLED -> {
                t.themeAmoledRes
            }

            ThemeOverlay.MD1 -> {
                t.themeMD1Res
            }

            else -> {
                t.themeRes
            }
        }
    }

    fun toastColor(isReadMessage: Boolean): Int {
        val t = getCurrentTheme()
        return if (isReadMessage) t.colorReadToast else t.colorToast
    }
}
