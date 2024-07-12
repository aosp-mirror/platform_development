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
  const nonVisibleNoContentRect = makeRect(
    'nonVisibleNoContentRect',
    false,
    false,
  );
  const nonVisibleContentRect = makeRect('nonVisibleContentRect', false, true);
  const visibleContentRect = makeRect('visibleContentRect', true, true);
  const visibleNoContentRect = makeRect('visibleNoContentRect', true, false);
  const displayRect = makeRect('displayRect', false, false, true);
  const allRects = [
    visibleContentRect,
    visibleNoContentRect,
    nonVisibleContentRect,
    nonVisibleNoContentRect,
    displayRect,
  ];
  const preFilteredRects = [
    visibleContentRect,
    visibleNoContentRect,
    nonVisibleContentRect,
    nonVisibleNoContentRect,
  ];
  let expectedRectIdToShowState: Map<string, RectShowState>;

  beforeEach(() => {
    rectFilter = new RectFilter();
    expectedRectIdToShowState = new Map([
      [visibleContentRect.id, RectShowState.SHOW],
      [visibleNoContentRect.id, RectShowState.SHOW],
      [nonVisibleContentRect.id, RectShowState.SHOW],
      [nonVisibleNoContentRect.id, RectShowState.SHOW],
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
    expect(isShown(visibleContentRect.id)).toBeTrue();

    // robust to same state (RectShowState.SHOW)
    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.SHOW);
    expect(isShown(visibleContentRect.id)).toBeTrue();

    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.HIDE);
    expect(isShown(visibleContentRect.id)).toBeFalse();

    // robust to same state (RectShowState.HIDE)
    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.HIDE);
    expect(isShown(visibleContentRect.id)).toBeFalse();

    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.SHOW);
    expect(isShown(visibleContentRect.id)).toBeTrue();

    expect(isShown(displayRect.id)).toBeFalse();
    rectFilter.updateRectShowState(displayRect.id, RectShowState.SHOW);
    expect(isShown(displayRect.id)).toBeTrue();
  });

  it('does not filter rects if applicable', () => {
    expect(rectFilter.filterRects(allRects, false, true, false)).toEqual(
      allRects,
    );
  });

  it('filters non visible rects', () => {
    const filteredRects = rectFilter.filterRects(allRects, true, true, false);
    expect(filteredRects).toEqual([
      visibleContentRect,
      visibleNoContentRect,
      displayRect,
    ]);

    // robust to all visible rects
    expect(rectFilter.filterRects(filteredRects, true, true, false)).toEqual(
      filteredRects,
    );

    // does not apply force-hide state
    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.HIDE);
    expect(rectFilter.filterRects(allRects, true, true, false)).toEqual(
      filteredRects,
    );

    // does not apply force-show state
    rectFilter.updateRectShowState(
      nonVisibleContentRect.id,
      RectShowState.SHOW,
    );
    expect(rectFilter.filterRects(allRects, true, true, false)).toEqual(
      filteredRects,
    );
  });

  it('applies show/hide states', () => {
    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.HIDE);
    const filteredRects = rectFilter.filterRects(allRects, false, false, false);
    expect(filteredRects).toEqual([
      visibleNoContentRect,
      nonVisibleContentRect,
      nonVisibleNoContentRect,
      displayRect,
    ]);

    // robust to no change in show/hide states
    expect(rectFilter.filterRects(filteredRects, false, false, false)).toEqual(
      filteredRects,
    );

    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.SHOW);
    expect(rectFilter.filterRects(allRects, false, false, false)).toEqual(
      allRects,
    );
  });

  it('filters non visible rects and applies show/hide states', () => {
    // does not remove force-shown non-visible rect
    rectFilter.updateRectShowState(
      nonVisibleContentRect.id,
      RectShowState.SHOW,
    );
    rectFilter.updateRectShowState(
      nonVisibleNoContentRect.id,
      RectShowState.SHOW,
    );
    expect(rectFilter.filterRects(allRects, true, false, false)).toEqual(
      allRects,
    );

    // removes force-hidden visible rect
    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.HIDE);
    const filteredRects = rectFilter.filterRects(allRects, true, false, false);
    expect(filteredRects).toEqual([
      visibleNoContentRect,
      nonVisibleContentRect,
      nonVisibleNoContentRect,
      displayRect,
    ]);

    // removes non-visible rect but keeps visible rect that has not had hide/show state applied
    rectFilter.updateRectShowState(
      nonVisibleNoContentRect.id,
      RectShowState.HIDE,
    );
    expect(rectFilter.filterRects(allRects, true, false, false)).toEqual([
      visibleNoContentRect,
      nonVisibleContentRect,
      displayRect,
    ]);
  });

  it('filters rects without content', () => {
    const filteredRects = rectFilter.filterRects(allRects, false, true, true);
    expect(filteredRects).toEqual([
      visibleContentRect,
      nonVisibleContentRect,
      displayRect,
    ]);

    // robust to all rects with content
    expect(rectFilter.filterRects(filteredRects, false, true, true)).toEqual(
      filteredRects,
    );
  });

  it('filters rects without content by show state and visibility', () => {
    const filteredRects = rectFilter.filterRects(allRects, true, false, true);
    expect(filteredRects).toEqual([visibleContentRect, displayRect]);

    // does not apply force-hide state
    rectFilter.updateRectShowState(visibleContentRect.id, RectShowState.HIDE);
    expect(rectFilter.filterRects(allRects, true, true, true)).toEqual(
      filteredRects,
    );

    // does not apply force-show state
    rectFilter.updateRectShowState(
      nonVisibleNoContentRect.id,
      RectShowState.SHOW,
    );
    expect(rectFilter.filterRects(allRects, true, true, true)).toEqual(
      filteredRects,
    );
  });

  function isShown(rectId: string) {
    const nonHiddenRectIds = rectFilter.getRectIdToShowState(
      allRects,
      preFilteredRects,
    );
    return nonHiddenRectIds.get(rectId) === RectShowState.SHOW;
  }

  function makeRect(
    id: string,
    isVisible: boolean,
    hasContent: boolean,
    isDisplay = false,
  ) {
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
      .setIsClickable(false)
      .setCornerRadius(0)
      .setDepth(0)
      .setOpacity(0.5)
      .setHasContent(hasContent)
      .build();
  }
});
