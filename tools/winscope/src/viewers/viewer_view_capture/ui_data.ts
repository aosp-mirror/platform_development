/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {TraceType} from 'trace/trace_type';
import {VcCuratedProperties} from 'viewers/common/curated_properties';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {RectShowState} from 'viewers/common/rect_show_state';
import {UiDataHierarchy} from 'viewers/common/ui_data_hierarchy';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/ui_rect';

export class UiData implements UiDataHierarchy {
  readonly dependencies: TraceType[] = [TraceType.VIEW_CAPTURE];
  rectsToDraw: UiRect[] = [];
  rectIdToShowState: Map<string, RectShowState> | undefined;
  displays: DisplayIdentifier[] = [];
  highlightedItem = '';
  highlightedProperty = '';
  pinnedItems: UiHierarchyTreeNode[] = [];
  rectsUserOptions: UserOptions = {};
  hierarchyUserOptions: UserOptions = {};
  propertiesUserOptions: UserOptions = {};
  hierarchyTrees: UiHierarchyTreeNode[] | undefined;
  propertiesTree: UiPropertyTreeNode | undefined;

  constructor(
    public sfRects: UiRect[] | undefined = undefined,
    public curatedProperties: VcCuratedProperties | undefined = undefined,
  ) {}
}
