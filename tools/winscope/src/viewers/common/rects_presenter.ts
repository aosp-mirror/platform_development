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
import {UiRect} from 'viewers/components/rects/types2d';
import {DisplayIdentifier} from './display_identifier';
import {RectFilter} from './rect_filter';
import {RectShowState} from './rect_show_state';
import {UserOptions} from './user_options';

export class RectsPresenter {
  private allCurrentRects: UiRect[] = [];
  private rectFilter = new RectFilter();
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
  ) {}

  getUserOptions() {
    return this.userOptions;
  }

  getRectsToDraw() {
    return this.rectsToDraw;
  }

  getRectIdToShowState() {
    return this.rectIdToShowState;
  }

  getDisplays() {
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
