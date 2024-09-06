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

import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {DisplayIdentifier} from './display_identifier';
import {RectShowState} from './rect_show_state';
import {TextFilter} from './text_filter';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export interface UiDataHierarchy {
  highlightedItem: string;
  pinnedItems: UiHierarchyTreeNode[];
  hierarchyUserOptions: UserOptions;
  hierarchyTrees: UiHierarchyTreeNode[] | undefined;
  propertiesUserOptions: UserOptions;
  propertiesTree: UiPropertyTreeNode | undefined;
  highlightedProperty: string;
  hierarchyFilter: TextFilter;
  propertiesFilter: TextFilter;
  rectsToDraw?: UiRect[];
  rectIdToShowState?: Map<string, RectShowState> | undefined;
  displays?: DisplayIdentifier[];
  rectsUserOptions?: UserOptions;
}
