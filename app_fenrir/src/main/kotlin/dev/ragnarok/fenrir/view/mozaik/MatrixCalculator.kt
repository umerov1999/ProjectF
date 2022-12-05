package dev.ragnarok.fenrir.view.mozaik

import kotlin.math.abs

class MatrixCalculator(private val count: Int, private val libra: Libra) {
    private fun analize(matrix: Array<IntArray>, target: Result) {
        val maxDiff = getMaxDiff(libra, matrix)
        if (maxDiff < target.minDiff || target.matrix == null) {
            target.minDiff = maxDiff
            target.matrix = cloneArray(matrix)
        }
    }

    fun calculate(rows: Int): Array<IntArray>? {
        val result = checkAllVariants(rows)
        return result.matrix
    }

    private fun checkAllVariants(rowsCount: Int): Result {
        val result = Result()
        val rows = Array(rowsCount) { IntArray(count) }
        for (i in rowsCount - 1 downTo 0) {
            val array = IntArray(count)
            for (a in 0 until count) {
                array[a] = -1
            }
            rows[i] = array
        }
        val forFirst = count - rowsCount
        for (i in 0 until count) {
            val toFirst = i < forFirst + 1
            rows[if (toFirst) 0 else i - forFirst][if (toFirst) i else 0] = i
        }
        doShuffle(rows, result)
        return result
    }

    private fun doShuffle(data: Array<IntArray>, result: Result) {
        analize(data, result)
        moveAll(data, 0, result)
    }

    private fun moveAll(data: Array<IntArray>, startFromIndex: Int, result: Result) {
        while (canMoveToNext(startFromIndex, data)) {
            move(startFromIndex, data)
            analize(data, result)
            if (startFromIndex + 1 < data.size - 1) {
                moveAll(cloneArray(data), startFromIndex + 1, result)
            }
        }
    }

    interface Libra {
        fun getWeight(index: Int): Float
    }

    private class Result {
        var minDiff = Float.MAX_VALUE
        var matrix: Array<IntArray>? = null
    }

    companion object {
        internal fun getMaxDiff(libra: Libra, variant: Array<IntArray>): Float {
            //float[][] realRows = new float[variant.length][variant[0].length];

            //for (int i = 0; i < variant.length; i++) {
            //    for (int a = 0; a < variant[i].length; a++) {
            //        int v = variant[i][a];
//
            //       if (v == -1) {
            //            realRows[i][a] = 0;
            //        } else {
            //            realRows[i][a] = libra.getWeight(v);
            //        }
            //    }
            //}
            val sums = FloatArray(variant.size)
            for (i in variant.indices) {
                sums[i] = getWeightSumm(libra, *variant[i])
            }

            //for (int i = 0; i < realRows.length; i++) {
            //    float[] rowArray = realRows[i];
            //    float sum = getSum(rowArray);
            //    sums[i] = sum;
            //}
            val average = getAverage(*sums)
            var maxDiff = 0f
            for (sum in sums) {
                val diff = abs(sum - average)
                if (diff > maxDiff) {
                    maxDiff = diff
                }
            }
            return maxDiff
        }

        private fun getWeightSumm(libra: Libra, vararg positions: Int): Float {
            var s = 0f
            for (position in positions) {
                if (position == -1) {
                    continue
                }
                s += libra.getWeight(position)
            }
            return s
        }

        private fun getAverage(vararg values: Float): Float {
            var sum = 0f
            var nonZeroValuesCount = 0
            for (value in values) {
                sum += value
                if (value != 0f) {
                    nonZeroValuesCount++
                }
            }
            return sum / nonZeroValuesCount.toFloat()
        }

        /**
         * Clones the provided array
         *
         * @param src
         * @return a new clone of the provided array
         */
        internal fun cloneArray(src: Array<IntArray>): Array<IntArray> {
            val length = src.size
            val target = Array(length) { IntArray(src[0].size) }
            for (i in 0 until length) {
                System.arraycopy(src[i], 0, target[i], 0, src[i].size)
            }
            return target
        }

        /**
         * Можно ли переместить последний елемент субмассива data по индексу row на следующую строку
         *
         * @param row
         * @param data
         * @return
         */
        internal fun canMoveToNext(row: Int, data: Array<IntArray>): Boolean {
            // можно только в том случае, если в строке есть хотябы 2 валидных значения
            // и с главном массиве есть следующая строка после row
            return data.size > row + 1 && data[row].isNotEmpty() && data[row][1] != -1
        }

        /**
         * Переместить последний елемент из строки с индексом row на следующую
         */
        internal fun move(row: Int, data: Array<IntArray>) {
            //if(data.length < row){
            //    throw new IllegalArgumentException();
            //}
            val rowArray = data[row]
            val nextRowArray = data[row + 1]
            if (nextRowArray[nextRowArray.size - 1] != -1) {
                move(row + 1, data)
            }
            val moveIndex = getLastNoNegativeIndex(rowArray)
            //if(moveIndex == -1){
            //    throw new IllegalStateException();
            //}
            val value = rowArray[moveIndex]
            shiftByOneToRight(nextRowArray)
            nextRowArray[0] = value
            rowArray[moveIndex] = -1
        }
        /*private static float getSum(float... values) {
        float s = 0;
        for (float f : values) {
            s = s + f;
        }

        return s;
    }*/
        /**
         * Сдвинуть все значение на 1 вправо, значение первого елемента будет заменено на -1
         *
         * @param array
         */
        private fun shiftByOneToRight(array: IntArray) {
            for (i in array.indices.reversed()) {
                if (i == 0) {
                    array[i] = -1
                } else {
                    array[i] = array[i - 1]
                }
            }
        }

        /**
         * Получить индекс последнего елемента, чье значение не равно -1
         *
         * @param array
         * @return
         */
        private fun getLastNoNegativeIndex(array: IntArray): Int {
            for (i in array.indices.reversed()) {
                if (array[i] != -1) {
                    return i
                }
            }
            return -1
        }
    }
}