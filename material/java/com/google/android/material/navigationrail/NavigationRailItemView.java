/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.google.android.material.navigationrail;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static java.lang.Math.max;

import android.content.Context;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.google.android.material.navigation.NavigationBarItemView;

/** @hide For internal use only. */
@RestrictTo(LIBRARY_GROUP)
final class NavigationRailItemView extends NavigationBarItemView {
  public NavigationRailItemView(@NonNull Context context) {
    super(context);
    // TODO(b/338647654): Measure Navigation Rail from the bottom of the textview, not baseline
    setMeasureBottomPaddingFromLabelBaseline(true);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
      int preferredHeight = MeasureSpec.getSize(heightMeasureSpec);
      int measuredHeight = getMeasuredHeight();
      int bestHeight = max(measuredHeight, preferredHeight);

      // Set view to use measured width, but use the best height possible
      setMeasuredDimension(getMeasuredWidthAndState(), bestHeight);
    }
  }

  @Override
  @LayoutRes
  protected int getItemLayoutResId() {
    return R.layout.mtrl_navigation_rail_item;
  }

  @Override
  @DimenRes
  protected int getItemDefaultMarginResId() {
    return R.dimen.mtrl_navigation_rail_icon_margin;
  }
}
