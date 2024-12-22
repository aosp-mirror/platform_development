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

import {Trace} from 'trace/trace';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {DisplayIdentifier} from './display_identifier';
import {RectFilter} from './rect_filter';
import {RectShowState} from './rect_show_state';
import {UserOptions} from './user_options';

export class RectsPresenter {
  private readonly rectFilter = new RectFilter(this.convertToKey);
  private allCurrentRects: UiRect[] = [];
  private rectsToDraw: UiRect[] = [];
  private displays: DisplayIdentifier[] = [];
  private rectIdToShowState: Map<string, RectShowState> | undefined;

  constructor(
    private userOptions: UserOptions,
    private makeUiRectsStrategy: (
      tree: HierarchyTreeNode,
      trace: Trace<HierarchyTreeNode>,
    ) => UiRect[],
    private makeDisplaysStrategy?: (rects: UiRect[]) => DisplayIdentifier[],
    private convertToKey: (rectId: string) => string = (id: string) => id,
  ) {}

  getUserOptions(): UserOptions {
    return this.userOptions;
  }

  getRectsToDraw(): UiRect[] {
    return this.rectsToDraw;
  }

  getRectIdToShowState(): Map<string, RectShowState> | undefined {
    return this.rectIdToShowState;
  }

  getDisplays(): DisplayIdentifier[] {
    return this.displays;
  }

  setDisplays(displays: DisplayIdentifier[]) {
    this.displays = displays;
  }

  applyHierarchyTreesChange(
    hierarchyTrees: Array<[Trace<HierarchyTreeNode>, HierarchyTreeNode[]]>,
  ) {
    this.allCurrentRects = [];
    for (const [trace, trees] of hierarchyTrees) {
      trees.forEach((tree) => {
        this.allCurrentRects.push(...this.makeUiRectsStrategy(tree, trace));
      });
    }
    this.updateRectsToDrawAndRectIdToShowState();
    if (this.makeDisplaysStrategy) {
      this.displays = this.makeDisplaysStrategy(this.rectsToDraw);
    }
  }

  applyRectsUserOptionsChange(userOptions: UserOptions) {
    this.userOptions = userOptions;
    this.updateRectsToDrawAndRectIdToShowState();
  }

  applyRectShowStateChange(id: string, newShowState: RectShowState) {
    this.rectFilter.updateRectShowState(id, newShowState);
    this.updateRectsToDrawAndRectIdToShowState();
  }

  updateRectShowStates(
    rectIdToShowState: Map<string, RectShowState> | undefined,
  ) {
    this.rectFilter.clear();
    if (rectIdToShowState) {
      for (const [id, state] of rectIdToShowState.entries()) {
        this.rectFilter.updateRectShowState(id, state);
      }
    }
    this.updateRectsToDrawAndRectIdToShowState();
  }

  clear() {
    this.allCurrentRects = [];
    this.rectsToDraw = [];
    this.displays = [];
    this.rectIdToShowState = undefined;
    this.rectFilter.clear();
  }

  private updateRectsToDrawAndRectIdToShowState() {
    this.rectsToDraw = this.filterRects(this.allCurrentRects);
    this.rectIdToShowState = this.rectFilter.getRectIdToShowState(
      this.allCurrentRects,
      this.rectsToDraw,
    );
  }

  private filterRects(rects: UiRect[]): UiRect[] {
    const isOnlyVisibleMode =
      this.userOptions['showOnlyVisible']?.enabled ?? false;
    const isIgnoreRectShowStateMode =
      this.userOptions['ignoreRectShowState']?.enabled ?? false;
    const isOnlyWithContentMode =
      this.userOptions['showOnlyWithContent']?.enabled ?? false;
    return this.rectFilter.filterRects(
      rects,
      isOnlyVisibleMode,
      isIgnoreRectShowStateMode,
      isOnlyWithContentMode,
    );
  }
}
