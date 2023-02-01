/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {Chip} from './chip';

export type UiTreeNode = HierarchyTreeNode | PropertiesTreeNode;

export class HierarchyTreeNode {
  constructor(
    public name: string,
    public kind: string,
    public stableId: string,
    children?: HierarchyTreeNode[]
  ) {
    this.children = children ?? [];
  }

  children: HierarchyTreeNode[];
  shortName?: string;
  type?: string;
  id?: string | number;
  layerId?: number;
  displayId?: number;
  stackId?: number;
  isVisible?: boolean;
  isMissing?: boolean;
  hwcCompositionType?: number;
  zOrderRelativeOfId?: number;
  zOrderRelativeOf?: any;
  zOrderRelativeParentOf?: any;
  isRootLayer?: boolean;
  showInFilteredView?: boolean;
  showInOnlyVisibleView?: boolean;
  simplifyNames?: boolean;
  chips: Chip[] = [];
  diffType?: string;
  skip?: any;
}

export interface PropertiesDump {
  [key: string]: any;
}

export interface PropertiesTreeNode {
  properties?: any;
  kind?: string;
  stableId?: string;
  children?: PropertiesTreeNode[];
  propertyKey?: string | Terminal | null;
  propertyValue?: string | Terminal | null;
  name?: string | Terminal;
  diffType?: string;
  combined?: boolean;
} //TODO: make specific

export const DiffType = {
  NONE: 'none',
  ADDED: 'added',
  DELETED: 'deleted',
  ADDED_MOVE: 'addedMove',
  DELETED_MOVE: 'deletedMove',
  MODIFIED: 'modified',
};

export class Terminal {}

export class UiTreeUtils {
  static diffClass(item: UiTreeNode): string {
    const diffType = item.diffType;
    return diffType ?? '';
  }

  static isHighlighted(item: UiTreeNode, highlightedItems: string[]) {
    return item instanceof HierarchyTreeNode && highlightedItems.includes(`${item.stableId}`);
  }

  static isVisibleNode(kind: string, type?: string) {
    return kind === 'WindowState' || kind === 'Activity' || type?.includes('Layer');
  }

  static isParentNode(kind: string) {
    return UiTreeUtils.PARENT_NODE_KINDS.includes(kind);
  }

  private static readonly PARENT_NODE_KINDS = [
    'entry',
    'WindowManagerState',
    'InputMethodClient entry',
    'InputMethodService entry',
    'InputMethodManagerService entry',
  ];
}
