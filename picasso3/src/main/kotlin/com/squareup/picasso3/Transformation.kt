/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso3

import android.graphics.Bitmap
import com.squareup.picasso3.RequestHandler.Result

/** Image transformation.  */
interface Transformation {
    /**
     * Transform the source result into a new result. If you create a new bitmap instance, you must
     * call [android.graphics.Bitmap.recycle] on `source`. You may return the original
     * if no transformation is required.
     */
    fun transform(source: Result.Bitmap): Result.Bitmap
    fun localTransform(source: Bitmap?): Bitmap?

    /**
     * Returns a unique key for the transformation, used for caching purposes. If the transformation
     * has parameters (e.g. size, scale factor, etc) then these should be part of the key.
     */
    fun key(): String
}
