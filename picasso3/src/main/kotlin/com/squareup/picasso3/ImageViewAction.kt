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

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.squareup.picasso3.RequestHandler.Result
import java.lang.ref.WeakReference

internal class ImageViewAction(
    picasso: Picasso,
    target: ImageView,
    data: Request,
    private val errorDrawable: Drawable?,
    @DrawableRes val errorResId: Int,
    private val noFade: Boolean,
    var callback: Callback?
) : Action(picasso, data) {
    private val targetReference = WeakReference(target)

    override fun complete(result: Result) {
        val target = targetReference.get() ?: return

        PicassoDrawable.setResult(
            target,
            picasso.context,
            result,
            noFade,
            picasso.indicatorsEnabled
        )
        callback?.onSuccess()
    }

    override fun error(e: Exception) {
        val target = targetReference.get() ?: return

        val placeholder = target.drawable
        if (placeholder is Animatable) {
            (placeholder as Animatable).stop()
        }
        if (errorResId != 0) {
            target.setImageResource(errorResId)
        } else if (errorDrawable != null) {
            target.setImageDrawable(errorDrawable)
        }
        callback?.onError(e)
    }

    override fun getTarget(): ImageView? {
        return targetReference.get()
    }

    override fun cancel() {
        super.cancel()
        callback = null
    }
}
