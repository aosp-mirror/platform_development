/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {UiRect} from 'viewers/components/rects/types2d';

export class RectFilter {
  private hide: string[] = [];
  private show: string[] = [];

  filterRects(
    rects: UiRect[],
    isOnlyVisibleMode: boolean,
    isApplyNonHiddenMode: boolean,
  ): UiRect[] {
    if (!(isOnlyVisibleMode || isApplyNonHiddenMode)) {
      return rects;
    }
    return rects.filter((rect) => {
      const satisfiesOnlyVisible = rect.isDisplay || rect.isVisible;
      const isNotHidden = !this.hide.includes(rect.id);
      const forceShow = this.show.includes(rect.id);

      if (isOnlyVisibleMode && isApplyNonHiddenMode) {
        return forceShow || (satisfiesOnlyVisible && isNotHidden);
      }
      if (isOnlyVisibleMode) {
        return satisfiesOnlyVisible;
      }
      return isNotHidden;
    });
  }

  getNonHiddenRectIds(allRectIds: string[], filteredRects: UiRect[]): string[] {
    return allRectIds.filter((rectId) => {
      const forceHide = this.hide.includes(rectId);
      const forceShow = this.show.includes(rectId);
      const isShown = filteredRects.some((r) => r.id === rectId);
      return !forceHide && (forceShow || isShown);
    });
  }

  updateRectShowState(id: string, newShowState: boolean) {
    const newShow = this.show.filter((r) => (newShowState ? true : r !== id));
    const newHide = this.hide.filter((r) => (newShowState ? r !== id : true));

    if (newShowState && !newShow.includes(id)) {
      newShow.push(id);
    } else if (!newShowState && !newHide.includes(id)) {
      newHide.push(id);
    }

    this.hide = newHide;
    this.show = newShow;
  }
}
