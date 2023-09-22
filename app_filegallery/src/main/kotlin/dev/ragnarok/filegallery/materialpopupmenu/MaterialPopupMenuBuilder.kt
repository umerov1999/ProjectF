@file:Suppress("unused")

package dev.ragnarok.filegallery.materialpopupmenu

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.GravityInt
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.materialpopupmenu.internal.MaterialRecyclerViewPopupWindow
import java.util.function.BiConsumer
import java.util.function.Predicate

/**
 * Builder for creating a [MaterialPopupMenu].
 *
 * The [MaterialPopupMenu] must have at least one section.
 * All sections must also have at least one item and each item must have a non-null label set.
 *
 * @author Piotr Zawadzki
 */
@PopupMenuMarker
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MaterialPopupMenuBuilder internal constructor(
    private val context: Context,
    private val view: View
) {

    internal class Data {
        var style = 0
        var dropdownGravity = Gravity.NO_GRAVITY
        var fixedContentWidthInPx = 0
        var dropDownVerticalOffset: Int = MaterialRecyclerViewPopupWindow.DISABLED_INT
        var dropDownHorizontalOffset: Int = MaterialRecyclerViewPopupWindow.DISABLED_INT
        var customAnimation: PopupAnimation? = null

        /**
         * Flag that determines if this menu is a submenu.
         */
        var isSubMenu = false
    }

    private val data = Data()

    /**
     * Style of the popup menu.
     *
     * You should use [R.style.Widget_MPM_Menu_Material3] Material 3, respectively.
     *
     * Setting this to `0` will make the popup use the default style resolved based on context
     * passed to [MaterialPopupMenu.show] function. You can customize that default style by defining
     * [R.attr.materialPopupMenuStyle] in your theme style.
     *
     * are also declared in your style.
     */
    @get:StyleRes
    var style: Int
        set(@StyleRes value) {
            data.style = value
        }
        get() = data.style

    /**
     * Gravity of the dropdown list. This is commonly used to
     * set gravity to START or END for alignment with the anchor.
     * Setting [Gravity.BOTTOM] will anchor the dropdown list below the view.
     */
    @get:GravityInt
    var dropdownGravity: Int
        set(@GravityInt value) {
            data.dropdownGravity = value
        }
        get() = data.dropdownGravity

    /**
     * Setting this to a non-zero value will force the width of the popup menu to be exactly this value.
     * If set to 0, the default mechanism for measuring popup menu width will be applied.
     */
    @get:Px
    var fixedContentWidthInPx: Int
        set(@Px value) {
            data.fixedContentWidthInPx = value
        }
        get() = data.fixedContentWidthInPx

    /**
     * Setting this will override `android:dropDownVerticalOffset` set by the style applied in [style].
     */
    var dropDownVerticalOffset: Int
        set(value) {
            data.dropDownVerticalOffset = value
        }
        get() = data.dropDownVerticalOffset

    /**
     * Setting this will override `android:dropDownHorizontalOffset` set by the style applied in [style].
     */
    var dropDownHorizontalOffset: Int
        set(value) {
            data.dropDownHorizontalOffset = value
        }
        get() = data.dropDownHorizontalOffset

    /**
     * Optional custom animation.
     */
    var customAnimation: PopupAnimation?
        set(value) {
            data.customAnimation = value
        }
        get() = data.customAnimation

    private val sectionList = mutableListOf<Section>()

    private var onDismissListener: Runnable? = null

    private var calculateHeightOfAnchorView: Boolean = false

    /**
     * Adds a new section to the popup menu.
     *
     * Sections are separated with a divider from each other and must contain at least one item.
     * Section titles are optional.
     * @param init block containing section definition
     */
    fun section(init: Section.() -> Unit) = sectionList.add(Section(context).apply(init))

    fun onDismiss(listener: () -> Unit) {
        onDismissListener = Runnable(listener::invoke)
    }

    fun setCalculateHeightOfAnchorView(calculateHeightOfAnchorView: Boolean) {
        this.calculateHeightOfAnchorView = calculateHeightOfAnchorView
    }

    /**
     * Creates a [MaterialPopupMenu] with the already configured params.
     *
     * This might throw [IllegalStateException] if it wasn't configured properly
     * `-` see class description for validation details.
     */
    fun build(): MaterialPopupMenu {
        require(sectionList.isNotEmpty()) { "Popup menu sections cannot be empty!" }
        return MaterialPopupMenu(view = view,
            context = context,
            data = data,
            dismissListener = onDismissListener,
            calculateHeightOfAnchorView = calculateHeightOfAnchorView,
            sections = sectionList.filter { !it.shouldBeHidden }
                .mapTo(mutableListOf(), Section::toPopupMenuSection))
    }

    /**
     * Holds section info for the builder. This gets converted to [MaterialPopupMenu.PopupMenuSection].
     */
    @PopupMenuMarker
    class Section internal constructor(private val context: Context) {

        internal class Data {
            var title: CharSequence? = null
            var titleRes = 0
            var shouldBeHidden = false
        }

        private val data = Data()

        /**
         * Optional section holder. `null` by default.
         * If the title is non-null it will be displayed in the menu.
         */
        var title: CharSequence?
            set(value) {
                data.title = value
            }
            get() = data.title

        @get:StringRes
        var titleRes: Int
            set(@StringRes value) {
                data.titleRes = value
            }
            get() = data.titleRes

        var shouldBeHidden: Boolean
            set(value) {
                data.shouldBeHidden = value
            }
            get() = data.shouldBeHidden

        private val itemsHolderList = mutableListOf<AbstractItem>()

        /**
         * Adds a normal item to the section.
         * @param init block containing item definition
         */
        fun item(label: CharSequence, init: Item.() -> Unit) {
            itemsHolderList.add(Item(label).apply(init))
        }

        fun item(labelRes: Int, init: Item.() -> Unit) {
            itemsHolderList.add(Item(labelRes).apply(init))
        }

        /**
         * Adds a checkbox-based item to the section.
         * @param init block containing item definition
         */
        fun checkboxItem(label: CharSequence, init: CheckboxItem.() -> Unit) {
            itemsHolderList.add(CheckboxItem(label).apply(init))
        }

        fun checkboxItem(labelRes: Int, init: CheckboxItem.() -> Unit) {
            itemsHolderList.add(CheckboxItem(labelRes).apply(init))
        }

        /**
         * Adds a custom item to the section.
         * @param init block containing custom item definition
         */
        fun customItem(view: View, init: CustomItem.() -> Unit) {
            itemsHolderList.add(CustomItem(view).apply(init))
        }

        /**
         * Adds a radio group item to the section.
         */
        fun radioGroupItem(init: RadioGroupItem.() -> Unit) {
            itemsHolderList.add(RadioGroupItem().apply(init))
        }

        /**
         * Adds a switch item to the section.
         */
        fun switchItem(label: CharSequence, init: SwitchItem.() -> Unit) {
            itemsHolderList.add(SwitchItem(label).apply(init))
        }

        fun switchItem(labelRes: Int, init: SwitchItem.() -> Unit) {
            itemsHolderList.add(SwitchItem(labelRes).apply(init))
        }

        internal fun navBackItem(
            labelRes: Int = 0, label: CharSequence? = null, init: NavBackItem.() -> Unit
        ) = itemsHolderList.add(
            when {
                labelRes != 0 -> NavBackItem(labelRes)
                label != null -> NavBackItem(label)
                else -> NavBackItem()
            }.apply(init)
        )

        internal fun toPopupMenuSection(): MaterialPopupMenu.PopupMenuSection {
            require(itemsHolderList.isNotEmpty()) { "Section '$title' has no items!" }
            return MaterialPopupMenu.PopupMenuSection(data,
                itemsHolderList.filter { !it.data.shouldBeHidden }
                    .map(AbstractItem::convertToPopupMenuItem))
        }
    }

    /**
     * Base class for normal items with a label and icon.
     */
    sealed class NormalItem : AbstractItem() {

        internal sealed class Data private constructor(
            val label: CharSequence?,
            val labelRes: Int
        ) : AbstractItem.Data() {
            constructor(label: CharSequence) : this(label, 0)
            constructor(labelRes: Int) : this(null, labelRes)

            var labelColor = 0
            var labelTypeface: Typeface = Typeface.DEFAULT
            var labelAlignment = Gravity.NO_GRAVITY
            open var icon = 0
            var iconDrawable: Drawable? = null
            var iconColor = 0
        }

        abstract override val data: Data

        /**
         * Optional text color of the label. If not set or 0 the default color will be used.
         */
        @get:ColorInt
        var labelColor: Int
            set(@ColorInt value) {
                data.labelColor = value
            }
            get() = data.labelColor

        /**
         * Optional typeface of the label.
         */
        var labelTypeface: Typeface
            set(value) {
                data.labelTypeface = value
            }
            get() = data.labelTypeface

        /**
         * Label alignment in an item. If not set [Gravity.NO_GRAVITY] will be used.
         * @see [TextView.setGravity]
         */
        @get:GravityInt
        var labelAlignment: Int
            set(@GravityInt value) {
                data.labelAlignment = value
            }
            get() = data.labelAlignment

        /**
         * Optional icon to be displayed together with the label.
         *
         * This must be a valid drawable resource ID if set.
         *
         * `0` means that no icon should be displayed.
         *
         * Alternatively, you can set the drawable using [iconDrawable].
         *
         * If both this and [iconDrawable] are set [iconDrawable] will be used.
         */
        @get:DrawableRes
        var icon: Int
            set(@DrawableRes value) {
                data.icon = value
            }
            get() = data.icon

        /**
         * Optional icon to be displayed together with the label.
         *
         * `null` means that no icon should be displayed.
         *
         * Alternatively, you can set the drawable using [icon].
         *
         * If both [icon] and this are set this will be used.
         */
        var iconDrawable: Drawable?
            set(value) {
                data.iconDrawable = value
            }
            get() = data.iconDrawable

        /**
         * Optional icon tint color.
         *
         * This must be a valid color Int if set.
         * If not set, default tinting will be applied.
         */
        @get:ColorInt
        var iconColor: Int
            set(@ColorInt value) {
                data.iconColor = value
            }
            get() = data.iconColor

    }

    /**
     * Special item exclusively for submenus.
     */
    @PopupMenuMarker
    class NavBackItem private constructor(override val data: Data) : NormalItem() {

        internal class Data : NormalItem.Data {
            constructor() : super(R.string.button_back)
            constructor(label: CharSequence) : super(label)
            constructor(labelRes: Int) : super(labelRes)

            override var icon: Int = R.drawable.arrow_left
        }

        internal constructor() : this(Data())
        internal constructor(label: CharSequence) : this(Data(label))
        internal constructor(labelRes: Int) : this(Data(labelRes))

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuNavBackItem =
            MaterialPopupMenu.PopupMenuNavBackItem(data, resolveOnShowCallback())
    }

    @PopupMenuMarker
    class Item private constructor(override val data: Data) : NormalItem() {

        internal class Data : NormalItem.Data {
            constructor(label: CharSequence) : super(label)
            constructor(labelRes: Int) : super(labelRes)

            var subMenu: MaterialPopupMenu? = null
        }

        internal constructor(label: CharSequence) : this(Data(label))
        internal constructor(labelRes: Int) : this(Data(labelRes))

        /**
         * Set another menu that will be opened when this item is clicked. This item will
         * have a "nested" icon shown at the end.
         *
         * Also, the submenu will have a "go back" item automatically "injected" to
         * the top, which brings you back to this menu when clicked.
         *
         * **Note:** [dismissOnSelect] will have no effect.
         */
        fun subMenu(
            menu: MaterialPopupMenu,
            labelRes: Int = 0,
            label: CharSequence? = null,
            navBackItem: NavBackItem.() -> Unit = {}
        ) {
            data.subMenu = menu.setIsSubMenu(labelRes, label, navBackItem)
        }

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuItem =
            MaterialPopupMenu.PopupMenuItem(data, resolveOnShowCallback())
    }

    /**
     * Base class for normal items with a [CompoundButton] that will be toggled when the item is clicked.
     */
    sealed class ToggleItem : NormalItem() {

        internal sealed class Data : NormalItem.Data {
            constructor(label: CharSequence) : super(label)
            constructor(labelRes: Int) : super(labelRes)

            override var dismissOnSelect: Boolean = false
            var config: BiConsumer<OnShowCallback, CompoundButton>? = null
            var toggleCondition: Predicate<CompoundButton>? = null
            var isChecked = false

            fun isItemEligibleForToggling(compoundButton: CompoundButton): Boolean {
                val condition = toggleCondition
                return condition == null || condition.test(compoundButton)
            }
        }

        abstract override val data: Data

        fun setupToggle(config: (OnShowCallback, CompoundButton) -> Unit) {
            data.config = BiConsumer(config::invoke)
        }

        /**
         * Set a condition to toggle the compound button. Return `true` if it should be, `false` if it shouldn't.
         */
        fun toggleCondition(toggleCondition: (CompoundButton) -> Boolean) {
            data.toggleCondition = Predicate(toggleCondition::invoke)
        }

        var isChecked: Boolean
            get() = data.isChecked
            set(value) {
                data.isChecked = value
            }

    }

    @PopupMenuMarker
    class SwitchItem private constructor(override val data: Data) : ToggleItem() {

        internal class Data : ToggleItem.Data {
            constructor(label: CharSequence) : super(label)
            constructor(labelRes: Int) : super(labelRes)
        }

        internal constructor(label: CharSequence) : this(Data(label))
        internal constructor(labelRes: Int) : this(Data(labelRes))

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuSwitchItem =
            MaterialPopupMenu.PopupMenuSwitchItem(data, resolveOnShowCallback())

    }

    @PopupMenuMarker
    class CheckboxItem private constructor(override val data: Data) : ToggleItem() {

        internal class Data : ToggleItem.Data {
            constructor(label: CharSequence) : super(label)
            constructor(labelRes: Int) : super(labelRes)
        }

        internal constructor(label: CharSequence) : this(Data(label))
        internal constructor(labelRes: Int) : this(Data(labelRes))

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuCheckboxItem =
            MaterialPopupMenu.PopupMenuCheckboxItem(data, resolveOnShowCallback())
    }

    /**
     * Holds section custom item info for the builder. This gets converted to [MaterialPopupMenu.PopupMenuCustomItem].
     */
    @PopupMenuMarker
    class CustomItem internal constructor(view: View) : AbstractItem() {

        internal class Data(val view: View) : AbstractItem.Data() {
            var disableDefaultClickHandlers = false
        }

        override val data = Data(view)

        var disableDefaultClickHandlers: Boolean
            set(value) {
                data.disableDefaultClickHandlers = value
            }
            get() = data.disableDefaultClickHandlers

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuCustomItem =
            MaterialPopupMenu.PopupMenuCustomItem(data, resolveOnShowCallback())
    }

    @PopupMenuMarker
    class RadioButtonItem private constructor(override val data: Data) : ToggleItem() {

        internal class Data : ToggleItem.Data {
            constructor(label: CharSequence) : super(label)
            constructor(labelRes: Int) : super(labelRes)
        }

        internal constructor(label: CharSequence) : this(Data(label))
        internal constructor(labelRes: Int) : this(Data(labelRes))

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuRadioButtonItem =
            MaterialPopupMenu.PopupMenuRadioButtonItem(data, resolveOnShowCallback())
    }

    @PopupMenuMarker
    class RadioGroupItem internal constructor() : AbstractItem() {

        internal class Data : AbstractItem.Data() {
            val radioButtonItems = mutableListOf<MaterialPopupMenu.PopupMenuRadioButtonItem>()
        }

        override val data = Data()

        fun radioButtonItem(label: CharSequence, init: RadioButtonItem.() -> Unit) {
            data.radioButtonItems.add(RadioButtonItem(label).apply(init).convertToPopupMenuItem())
        }

        fun radioButtonItem(labelRes: Int, init: RadioButtonItem.() -> Unit) {
            data.radioButtonItems.add(
                RadioButtonItem(labelRes).apply(init).convertToPopupMenuItem()
            )
        }

        override fun convertToPopupMenuItem(): MaterialPopupMenu.PopupMenuRadioGroupItem {
            require(data.radioButtonItems.size >= 2) { "Radio groups must have 2 or more items" }
            return MaterialPopupMenu.PopupMenuRadioGroupItem(data, resolveOnShowCallback())
        }
    }

    @PopupMenuMarker
    sealed class AbstractItem {

        internal sealed class Data {
            var onSelectListener: Runnable? = null
            open var dismissOnSelect = true
            var shouldBeHidden = false
        }

        internal abstract val data: Data

        /**
         * Callback to be invoked once an item gets selected.
         */
        fun onSelect(listener: () -> Unit) {
            data.onSelectListener = Runnable(listener::invoke)
        }

        /**
         * Whether to dismiss the popup once an item gets selected.
         * Defaults to `true` for [NormalItem] and [CustomItem], false for [ToggleItem], has no effect for [NavBackItem].
         */
        var dismissOnSelect: Boolean
            set(value) {
                data.dismissOnSelect = value
            }
            get() = data.dismissOnSelect

        /**
         * Whether this item will be removed from the list.
         * Defaults to `false`.
         */
        var shouldBeHiddenIf: Boolean
            set(value) {
                data.shouldBeHidden = value
            }
            get() = data.shouldBeHidden

        private var onShowCallback: (OnShowCallback.() -> Unit)? = null

        /**
         * Callback to be invoked once the item gets shown.
         */
        fun onShow(callback: OnShowCallback.() -> Unit) {
            onShowCallback = callback
        }

        internal abstract fun convertToPopupMenuItem(): MaterialPopupMenu.AbstractPopupMenuItem

        protected fun resolveOnShowCallback() = OnShowCallback(onShowCallback)

    }
}

/**
 * Creates a [MaterialPopupMenu], applies your desired optional configuration and shows it.
 * @param menuConfig Optional block containing popup menu configuration.
 * Do **NOT** call [MaterialPopupMenu.show] here, this function does it for you.
 * @param menuBuilder block containing popup menu definition
 */
fun View.popupMenu(
    context: Context,
    menuConfig: MaterialPopupMenu.() -> Unit = {},
    menuBuilder: MaterialPopupMenuBuilder.() -> Unit
) = popupMenuBuilder(context, menuBuilder).build().apply(menuConfig).show()

fun View.popupMenu(
    context: Context,
    x: Int,
    y: Int,
    menuConfig: MaterialPopupMenu.() -> Unit = {},
    menuBuilder: MaterialPopupMenuBuilder.() -> Unit
) = popupMenuBuilder(context, menuBuilder).build().apply(menuConfig).showAtLocation(x, y)

fun View.popupMenuBuilder(
    context: Context,
    menuBuilder: MaterialPopupMenuBuilder.() -> Unit
) = MaterialPopupMenuBuilder(context, this).apply(menuBuilder)

@DslMarker
internal annotation class PopupMenuMarker
