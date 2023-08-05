/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.carousel;

import android.view.View;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

/**
 * A class responsible for creating a model used by a carousel to mask and offset views as they move
 * along a scrolling axis.
 */
public abstract class CarouselStrategy {

  /**
   * Calculates a keyline arrangement and returns a constructed {@link KeylineState}.
   *
   * <p>This method is called when {@link Carousel} measures the first item to be added to its
   * scroll container. This method then must create a {@link KeylineState} which tells {@link
   * Carousel} how to fill the scroll container with items - how many are visible at once, what
   * their sizes are, and where they're placed.
   *
   * <p>For example, take a simple arrangement that fills the scroll container with two large items
   * and one small item. As the user scrolls the first large item moves off-screen to the left, the
   * second large item moves to position 1, the small item unmasks into a large item at position 2,
   * and a new small item scrolls into view from the right. To create this arrangement, pick any
   * size for the small item that will be smaller than the large item. Next, take the carousel's
   * total space, subtract the small item size and divide the remainder by two - this is your large
   * item size. After determining the size of our large and small items, we can now construct a
   * {@link KeylineState} and add keylines representing each item:
   *
   * <pre>
   *
   * // Find the centers of the items in our arrangement, aligning the first item's left with the
   * // left of the scroll container (0).
   * float firstLargeItemCenter = largeChildSize / 2F;
   * float smallItemCenter = (largeChildSize * 2F) + (smallChildSize / 2F);
   *
   * // Get our child margins to use when calculating mask percentage
   * LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
   * float childMargins = childLayoutParams.leftMargin + childLayoutParams.rightMargin;
   *
   * return new KeylineState.Builder(largeChildWidth)
   *     .addKeylineRange(
   *         firstLargeItemCenter, // offsetLoc
   *         getChildMaskPercentage(largeChildSize, largeChildSize, childMargins), // mask
   *         largeChildSize, // maskedItemSize
   *         2, // count
   *         true) // isFocal
   *     .addKeyline(
   *         smallItemCenter, // offsetLoc
   *         getChildMaskPercentage(smallChildSize, largeChildSize, childMargins), // mask
   *         smallChildSize); // maskedItemSize
   *
   * </pre>
   *
   * <p>A strategy does not need to take layout direction into account. {@link
   * CarouselLayoutManager} automatically reverses the strategy's {@link KeylineState} when laid out
   * in right-to-left. Additionally, {@link CarouselLayoutManager} shifts the focal keylines to the
   * start or end of the container when at the start or end of a list in order to allow every item
   * in the list to pass through the focal state.
   *
   * <p>For additional guidelines on constructing valid KeylineStates, see {@link
   * KeylineState.Builder}.
   *
   * @param carousel The carousel to create a {@link KeylineState} for
   * @param child The first measured view from the carousel.
   * @return A {@link KeylineState} to be used by the layout manager to offset and mask children
   *     along the scrolling axis.
   */
  abstract KeylineState onFirstChildMeasuredWithMargins(
      @NonNull Carousel carousel, @NonNull View child);

  /**
   * Helper method to calculate a child's mask percentage given its masked size, unmasked size, and
   * margins.
   *
   * @param maskedSize The size this method calculates a mask percentage for
   * @param unmaskedSize The size this child is when fully unmasked (mask == 0F). This should likely
   *     be the {@code itemSize} passed to the {@link KeylineState.Builder} constructor.
   * @param childMargins The total margins at the start+end or top+bottom of this child. By default,
   *     these are removed from the returned mask as margins should not change in size as a child's
   *     mask changes.
   * @return A percentage by which the child should be masked in order to be sized at {@code
   *     maskedSize}. 0F is fully unmasked and 1F is fully masked.
   */
  @FloatRange(from = 0F, to = 1F)
  static float getChildMaskPercentage(float maskedSize, float unmaskedSize, float childMargins) {
    return 1F - ((maskedSize - childMargins) / (unmaskedSize - childMargins));
  }

  /**
   * Gets whether this carousel should mask items against the edges of the carousel container.
   *
   * @return true if items in the carousel should mask/squash against the edges of the carousel
   *     container. false if the carousel should allow items to bleed past the edges of the
   *     container and be clipped.
   */
  boolean isContained() {
    return true;
  }
}
