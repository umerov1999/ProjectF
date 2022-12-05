package dev.ragnarok.fenrir.view.mozaik

import android.util.SparseIntArray
import dev.ragnarok.fenrir.fragment.base.PostImage

class MozaikLayoutParamsCalculator(
    private val matrix: Array<IntArray>,
    private val images: List<PostImage>,
    private val maxWidth: Int,
    private val spacing: Int
) {
    private val rowHeight: SparseIntArray = SparseIntArray(1)
    private val photoWidth: SparseIntArray = SparseIntArray(1)
    private fun getAspectRatioSumForRow(row: Int): Float {
        var sum = 0f
        val rowArray = matrix[row]
        for (index in rowArray) {
            if (index == -1) {
                break
            }
            sum += images[index].aspectRatio
        }
        return sum
    }

    fun getPostImagePosition(index: Int): PostImagePosition {
        val photo = images[index]
        val rowNumber = getRowNumberForIndex(matrix, index)
        val numberInrow = getColumnNumberForIndex(
            matrix, index
        )
        val propotrionRowSum = getAspectRatioSumForRow(rowNumber).toDouble()
        val currentPhotoProportion = photo.aspectRatio.toDouble()
        val coeficien = currentPhotoProportion / propotrionRowSum
        val width = (maxWidth.toDouble() * coeficien).toInt()
        val height = (photo.height.toDouble() * (width.toDouble() / photo.width
            .toDouble())).toInt()
        var marginLeft = 0
        val firstIndexInRow = index - numberInrow
        for (i in firstIndexInRow until index) {
            marginLeft += photoWidth[i] + spacing
        }
        var marginTop = 0
        for (i in 0 until rowNumber) {
            marginTop += rowHeight[i] + spacing
        }
        val position = PostImagePosition()
        position.sizeY = height
        position.sizeX = width
        position.marginX = marginLeft
        position.marginY = marginTop
        photoWidth.put(index, width)
        rowHeight.put(rowNumber, height)
        return position
    }

    companion object {
        internal fun getRowNumberForIndex(array: Array<IntArray>, index: Int): Int {
            for (i in array.indices) {
                val inner = array[i]
                for (a in inner) {
                    if (a == index) {
                        return i
                    }
                }
            }
            throw IllegalStateException("Value does not exist")
        }

        internal fun getColumnNumberForIndex(array: Array<IntArray>, index: Int): Int {
            for (inner in array) {
                for (i in inner.indices) {
                    if (inner[i] == index) {
                        return i
                    }
                }
            }
            throw IllegalStateException("Value does not exist")
        }
    }

}