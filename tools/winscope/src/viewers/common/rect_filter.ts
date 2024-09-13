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

import {UiRect} from 'viewers/components/rects/ui_rect';
import {RectShowState} from './rect_show_state';

export class RectFilter {
  private forcedStates = new Map<string, RectShowState>();

  constructor(private convertToForcedStateKey: (id: string) => string) {}

  filterRects(
    rects: UiRect[],
    isOnlyVisibleMode: boolean,
    isIgnoreRectShowStateMode: boolean,
    isOnlyWithContentMode: boolean,
  ): UiRect[] {
    if (
      !isOnlyVisibleMode &&
      isIgnoreRectShowStateMode &&
      !isOnlyWithContentMode
    ) {
      return rects;
    }
    return rects.filter((rect) => {
      const satisfiesHasContent = rect.hasContent || rect.isDisplay;
      if (isOnlyWithContentMode && !satisfiesHasContent) {
        return false;
      }

      const satisfiesOnlyVisible = rect.isDisplay || rect.isVisible;
      const key = this.convertToForcedStateKey(rect.id);
      const forceHidden = this.forcedStates.get(key) === RectShowState.HIDE;
      const forceShow = this.forcedStates.get(key) === RectShowState.SHOW;

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
      const key = this.convertToForcedStateKey(rect.id);
      const forcedState = this.forcedStates.get(key);
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
    this.forcedStates.set(this.convertToForcedStateKey(id), newShowState);
  }

  clear() {
    this.forcedStates.clear();
  }
}
