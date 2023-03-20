/*
 * Copyright (C) 2016 JetRadar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.ragnarok.fenrir.view.snowfall

import kotlin.math.abs
import kotlin.random.Random

internal class Randomizer {
    private val random by lazy { Random(System.nanoTime()) }

    private var nextNextGaussian = 0.0
    private var haveNextNextGaussian = false
    private fun nextGaussian(): Double {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        return if (haveNextNextGaussian) {
            haveNextNextGaussian = false
            nextNextGaussian
        } else {
            var v1: Double
            var v2: Double
            var s: Double
            do {
                v1 = 2 * random.nextDouble() - 1 // between -1 and 1
                v2 = 2 * random.nextDouble() - 1 // between -1 and 1
                s = v1 * v1 + v2 * v2
            } while (s >= 1 || s == 0.0)
            val multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s)
            nextNextGaussian = v2 * multiplier
            haveNextNextGaussian = true
            v1 * multiplier
        }
    }

    fun randomDouble(max: Int): Double {
        return random.nextDouble() * (max + 1)
    }

    fun randomInt(min: Int, max: Int, gaussian: Boolean = false): Int {
        return randomInt(max - min, gaussian) + min
    }

    fun randomInt(max: Int, gaussian: Boolean = false): Int {
        return if (gaussian) {
            (abs(randomGaussian()) * (max + 1)).toInt()
        } else {
            random.nextInt(max + 1)
        }
    }

    private fun randomGaussian(): Double {
        val gaussian = nextGaussian() / 3 // more 99% of instances in range (-1, 1)
        return if (gaussian > -1 && gaussian < 1) gaussian else randomGaussian()
    }

    fun randomSignum(): Int {
        return if (random.nextBoolean()) 1 else -1
    }
}