package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import dev.ragnarok.fenrir.util.Utils
import java.util.Collections

open class RowLayout : ViewGroup {
    private val horizontalSpacing: Int = DEFAULT_HORIZONTAL_SPACING
    private val verticalSpacing: Int = DEFAULT_VERTICAL_SPACING
    private var currentRows = emptyList<RowMeasurement>()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val maxInternalWidth = MeasureSpec.getSize(widthMeasureSpec) - horizontalPadding
        val maxInternalHeight = MeasureSpec.getSize(heightMeasureSpec) - verticalPadding
        val rows: MutableList<RowMeasurement> = ArrayList()
        var currentRow = RowMeasurement(maxInternalWidth, widthMode)
        rows.add(currentRow)
        for (child in layoutChildren) {
            val childLayoutParams = child.layoutParams
            val childWidthSpec =
                createChildMeasureSpec(childLayoutParams.width, maxInternalWidth, widthMode)
            val childHeightSpec =
                createChildMeasureSpec(childLayoutParams.height, maxInternalHeight, heightMode)
            child.measure(childWidthSpec, childHeightSpec)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            if (currentRow.wouldExceedMax(childWidth)) {
                currentRow = RowMeasurement(maxInternalWidth, widthMode)
                rows.add(currentRow)
            }
            currentRow.addChildDimensions(childWidth, childHeight)
        }
        var longestRowWidth = 0
        var totalRowHeight = 0
        for (index in rows.indices) {
            val row = rows[index]
            totalRowHeight += row.height
            if (index < rows.size - 1) {
                totalRowHeight += verticalSpacing
            }
            longestRowWidth = longestRowWidth.coerceAtLeast(row.width)
        }
        setMeasuredDimension(
            if (widthMode == MeasureSpec.EXACTLY) MeasureSpec.getSize(widthMeasureSpec) else (longestRowWidth
                    + horizontalPadding),
            if (heightMode == MeasureSpec.EXACTLY) MeasureSpec.getSize(heightMeasureSpec) else totalRowHeight + verticalPadding
        )
        currentRows = Collections.unmodifiableList(rows)
    }

    private fun createChildMeasureSpec(childLayoutParam: Int, max: Int, parentMode: Int): Int {
        val spec: Int = when (childLayoutParam) {
            LayoutParams.MATCH_PARENT -> {
                MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY)
            }

            LayoutParams.WRAP_CONTENT -> {
                MeasureSpec.makeMeasureSpec(
                    max,
                    if (parentMode == MeasureSpec.UNSPECIFIED) MeasureSpec.UNSPECIFIED else MeasureSpec.AT_MOST
                )
            }

            else -> {
                MeasureSpec.makeMeasureSpec(childLayoutParam, MeasureSpec.EXACTLY)
            }
        }
        return spec
    }

    override fun onLayout(
        changed: Boolean,
        leftPosition: Int,
        topPosition: Int,
        rightPosition: Int,
        bottomPosition: Int
    ) {
        val widthOffset = measuredWidth - paddingRight
        var x = paddingLeft
        var y = paddingTop
        val rowIterator = currentRows.iterator()
        var currentRow = rowIterator.next()
        for (child in layoutChildren) {
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            if (x + childWidth > widthOffset) {
                x = paddingLeft
                y += currentRow.height + verticalSpacing
                if (rowIterator.hasNext()) {
                    currentRow = rowIterator.next()
                }
            }
            child.layout(x, y, x + childWidth, y + childHeight)
            x += childWidth + horizontalSpacing
        }
    }

    private val layoutChildren: List<View>
        get() {
            val children: MutableList<View> = ArrayList()
            for (index in 0..<childCount) {
                val child = getChildAt(index)
                if (child.visibility != GONE) {
                    children.add(child)
                }
            }
            return children
        }
    private val verticalPadding: Int
        get() = paddingTop + paddingBottom
    private val horizontalPadding: Int
        get() = paddingLeft + paddingRight

    private inner class RowMeasurement(private val maxWidth: Int, private val widthMode: Int) {
        var width = 0
            private set
        var height = 0
            private set

        fun wouldExceedMax(childWidth: Int): Boolean {
            return widthMode != MeasureSpec.UNSPECIFIED && getNewWidth(childWidth) > maxWidth
        }

        fun addChildDimensions(childWidth: Int, childHeight: Int) {
            width = getNewWidth(childWidth)
            height = height.coerceAtLeast(childHeight)
        }

        private fun getNewWidth(childWidth: Int): Int {
            return if (width == 0) childWidth else width + horizontalSpacing + childWidth
        }
    }

    companion object {
        val DEFAULT_HORIZONTAL_SPACING = Utils.dp(4f)
        val DEFAULT_VERTICAL_SPACING = Utils.dp(4f)
    }
}
