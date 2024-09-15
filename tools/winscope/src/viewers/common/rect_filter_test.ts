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

import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';
import {RectFilter} from './rect_filter';
import {RectShowState} from './rect_show_state';

describe('RectFilter', () => {
  let rectFilter: RectFilter;
  const nonVisibleRect = makeRect('nonVisibleRect', false);
  const visibleRect = makeRect('visibleRect', true);
  const displayRect = makeRect('displayRect', false, true);
  const allRects = [visibleRect, nonVisibleRect, displayRect];
  const preFilteredRects = [visibleRect, nonVisibleRect];
  let expectedRectIdToShowState: Map<string, RectShowState>;

  beforeEach(() => {
    rectFilter = new RectFilter();
    expectedRectIdToShowState = new Map([
      [visibleRect.id, RectShowState.SHOW],
      [nonVisibleRect.id, RectShowState.SHOW],
      [displayRect.id, RectShowState.SHOW],
    ]);
  });

  it('creates rect id to show state map', () => {
    expect(rectFilter.getRectIdToShowState(allRects, allRects)).toEqual(
      expectedRectIdToShowState,
    );

    // sets hide state for rect that is not present in filtered rects
    expectedRectIdToShowState.set(displayRect.id, RectShowState.HIDE);
    expect(rectFilter.getRectIdToShowState(allRects, preFilteredRects)).toEqual(
      expectedRectIdToShowState,
    );

    // keeps rect that is not present in drawn rects but has force-show state
    rectFilter.updateRectShowState(displayRect.id, RectShowState.SHOW);
    expectedRectIdToShowState.set(displayRect.id, RectShowState.SHOW);
    expect(rectFilter.getRectIdToShowState(allRects, preFilteredRects)).toEqual(
      expectedRectIdToShowState,
    );
  });

  it('updates rect forced show state', () => {
    expect(isShown(visibleRect.id)).toBeTrue();

    // robust to same state (RectShowState.SHOW)
    rectFilter.updateRectShowState(visibleRect.id, RectShowState.SHOW);
    expect(isShown(visibleRect.id)).toBeTrue();

    rectFilter.updateRectShowState(visibleRect.id, RectShowState.HIDE);
    expect(isShown(visibleRect.id)).toBeFalse();

    // robust to same state (RectShowState.HIDE)
    rectFilter.updateRectShowState(visibleRect.id, RectShowState.HIDE);
    expect(isShown(visibleRect.id)).toBeFalse();

    rectFilter.updateRectShowState(visibleRect.id, RectShowState.SHOW);
    expect(isShown(visibleRect.id)).toBeTrue();

    expect(isShown(displayRect.id)).toBeFalse();
    rectFilter.updateRectShowState(displayRect.id, RectShowState.SHOW);
    expect(isShown(displayRect.id)).toBeTrue();
  });

  it('does not filter rects if applicable', () => {
    expect(rectFilter.filterRects(allRects, false, true)).toEqual(allRects);
  });

  it('filters non visible rects', () => {
    const filteredRects = rectFilter.filterRects(allRects, true, true);
    expect(filteredRects).toEqual([visibleRect, displayRect]);

    // robust to all visible rects
    expect(rectFilter.filterRects(filteredRects, true, true)).toEqual(
      filteredRects,
    );

    // does not apply force-hide state
    rectFilter.updateRectShowState(visibleRect.id, RectShowState.HIDE);
    expect(rectFilter.filterRects(allRects, true, true)).toEqual(filteredRects);

    // does not apply force-show state
    rectFilter.updateRectShowState(nonVisibleRect.id, RectShowState.SHOW);
    expect(rectFilter.filterRects(allRects, true, true)).toEqual(filteredRects);
  });

  it('applies show/hide states', () => {
    rectFilter.updateRectShowState(visibleRect.id, RectShowState.HIDE);
    const filteredRects = rectFilter.filterRects(allRects, false, false);
    expect(filteredRects).toEqual([nonVisibleRect, displayRect]);

    // robust to no change in show/hide states
    expect(rectFilter.filterRects(filteredRects, false, false)).toEqual(
      filteredRects,
    );

    rectFilter.updateRectShowState(visibleRect.id, RectShowState.SHOW);
    expect(rectFilter.filterRects(allRects, false, false)).toEqual(allRects);
  });

  it('filters non visible rects and applies show/hide states', () => {
    // does not remove force-shown non-visible rect
    rectFilter.updateRectShowState(nonVisibleRect.id, RectShowState.SHOW);
    expect(rectFilter.filterRects(allRects, true, false)).toEqual(allRects);

    // removes force-hidden visible rect
    rectFilter.updateRectShowState(visibleRect.id, RectShowState.HIDE);
    const filteredRects = rectFilter.filterRects(allRects, true, false);
    expect(filteredRects).toEqual([nonVisibleRect, displayRect]);

    // removes non-visible rect but keeps visible rect that has not had hide/show state applied
    rectFilter.updateRectShowState(nonVisibleRect.id, RectShowState.HIDE);
    expect(rectFilter.filterRects(allRects, true, false)).toEqual([
      displayRect,
    ]);
  });

  function isShown(rectId: string) {
    const nonHiddenRectIds = rectFilter.getRectIdToShowState(
      allRects,
      preFilteredRects,
    );
    return nonHiddenRectIds.get(rectId) === RectShowState.SHOW;
  }

  function makeRect(id: string, isVisible: boolean, isDisplay = false) {
    return new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setLabel(id)
      .setTransform({
        dsdx: 1,
        dsdy: 0,
        dtdx: 0,
        dtdy: 1,
        tx: 0,
        ty: 0,
      })
      .setIsVisible(isVisible)
      .setIsDisplay(isDisplay)
      .setId(id)
      .setGroupId(0)
      .setIsVirtual(false)
      .setIsClickable(false)
      .setCornerRadius(0)
      .setDepth(0)
      .setOpacity(0.5)
      .build();
  }
});
