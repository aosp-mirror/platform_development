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

import Chip from "./chip";

export type FilterType = (item: HierarchyTreeNode | PropertiesTreeNode | null) => boolean;

export type UiTreeNode = HierarchyTreeNode | PropertiesTreeNode;

export interface TreeNodeTrace {
  parent: TreeNodeTrace|undefined;
  children: TreeNodeTrace[];
  name: string;
  kind: string;
  stableId: string;
  displays?: TreeNodeTrace[];
  windowStates?: TreeNodeTrace[];
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
  isRootLayer?: boolean;
  chips?: Chip[];
  diffType?: string;
  skip?: any;
  equals?: any;
  obj?: any;
  get?: any;
  proto?: any;
}

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
  chips?: Chip[] = [];
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
  NONE: "none",
  ADDED: "added",
  DELETED: "deleted",
  ADDED_MOVE: "addedMove",
  DELETED_MOVE: "deletedMove",
  MODIFIED: "modified",
};

export class Terminal {}

export class TreeUtils
{
  public static findDescendantNode(node: TreeNodeTrace, isTargetNode: FilterType): TreeNodeTrace|undefined {
    if (isTargetNode(node)) {
      return node;
    }

    for (const child of node.children) {
      const target = this.findDescendantNode(child, isTargetNode);
      if (target) {
        return target;
      }
    }

    return undefined;
  }

  public static findAncestorNode(node: TreeNodeTrace, isTargetNode: FilterType): TreeNodeTrace|undefined {
    let ancestor = node.parent;

    while (ancestor && !isTargetNode(ancestor)) {
      ancestor = ancestor.parent;
    }

    return ancestor;
  }

  public static makeNodeFilter(filterString: string): FilterType {
    const filterStrings = filterString.split(",");
    const positive: any[] = [];
    const negative: any[] = [];
    filterStrings.forEach((f) => {
      f = f.trim();
      if (f.startsWith("!")) {
        const regex = new RegExp(f.substring(1), "i");
        negative.push((s: any) => !regex.test(s));
      } else {
        const regex = new RegExp(f, "i");
        positive.push((s: any) => regex.test(s));
      }
    });
    const filter = (item: any) => {
      if (item) {
        const apply = (f: any) => f(`${item.name}`);
        return (positive.length === 0 || positive.some(apply)) &&
          (negative.length === 0 || negative.every(apply));
      }
      return false;
    };
    return filter;
  }

  public static diffClass(item: UiTreeNode): string {
    const diffType = item.diffType;
    return diffType ?? "";
  }

  public static isHighlighted(item: UiTreeNode, highlightedItems: Array<string>) {
    return item instanceof HierarchyTreeNode && highlightedItems.includes(`${item.id}`);
  }

  public static isVisibleNode(kind: string, type?: string) {
    return kind === "WindowState" || kind === "Activity" || type?.includes("Layer");
  }

  public static isParentNode(kind: string) {
    return this.PARENT_NODE_KINDS.includes(kind);
  }

  private static readonly PARENT_NODE_KINDS = ["entry", "WindowManagerState"];
}
