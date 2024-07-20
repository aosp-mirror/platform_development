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
import {RectShowState} from './rect_show_state';

export class RectFilter {
  private forcedStates = new Map<string, RectShowState>();

  filterRects(
    rects: UiRect[],
    isOnlyVisibleMode: boolean,
    isIgnoreRectShowStateMode: boolean,
  ): UiRect[] {
    if (!isOnlyVisibleMode && isIgnoreRectShowStateMode) {
      return rects;
    }
    return rects.filter((rect) => {
      const satisfiesOnlyVisible = rect.isDisplay || rect.isVisible;
      const forceHidden = this.forcedStates.get(rect.id) === RectShowState.HIDE;
      const forceShow = this.forcedStates.get(rect.id) === RectShowState.SHOW;

      if (isOnlyVisibleMode && !isIgnoreRectShowStateMode) {
        return forceShow || (satisfiesOnlyVisible && !forceHidden);
      }
      if (isOnlyVisibleMode) {
        return satisfiesOnlyVisible;
      }
      return !forceHidden;
    });
  }

  getRectIdToShowState(
    allRects: UiRect[],
    shownRects: UiRect[],
  ): Map<string, RectShowState> {
    const rectIdToShowState = new Map<string, RectShowState>();
    allRects.forEach((rect) => {
      const forcedState = this.forcedStates.get(rect.id);
      if (forcedState !== undefined) {
        rectIdToShowState.set(rect.id, forcedState);
        return;
      }
      const isShown = shownRects.some((other) => other.id === rect.id);
      const showState = isShown ? RectShowState.SHOW : RectShowState.HIDE;
      rectIdToShowState.set(rect.id, showState);
    });
    return rectIdToShowState;
  }

  updateRectShowState(id: string, newShowState: RectShowState) {
    this.forcedStates.set(id, newShowState);
  }
}
