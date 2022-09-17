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

export type FilterType = (item: HierarchyTree | PropertiesTree | null) => boolean;

export type Tree = HierarchyTree | PropertiesTree;

export class HierarchyTree {
  constructor(
    public name: string,
    public kind: string,
    public stableId: string,
    children?: HierarchyTree[]
  ) {
    this.children = children ?? [];
  }

  children: HierarchyTree[];
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

export interface TreeFlickerItem {
  children: TreeFlickerItem[];
  name: string;
  kind: string;
  stableId: string;
  displays?: TreeFlickerItem[];
  windowStates?: TreeFlickerItem[];
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

export interface PropertiesDump {
  [key: string]: any;
}

export interface PropertiesTree {
  properties?: any;
  kind?: string;
  stableId?: string;
  children?: PropertiesTree[];
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

export function diffClass(item: Tree): string {
  const diffType = item.diffType;
  return diffType ?? "";
}

export function isHighlighted(item: Tree, highlightedItems: Array<string>) {
  return item instanceof HierarchyTree && highlightedItems.includes(`${item.id}`);
}

export function getFilter(filterString: string): FilterType {
  const filterStrings = filterString.split(",");
  const positive: any[] = [];
  const negative: any[] = [];
  filterStrings.forEach((f) => {
    f = f.trim();
    if (f.startsWith("!")) {
      const regex = new RegExp(f.substring(1), "i");
      negative.push((s:any) => !regex.test(s));
    } else {
      const regex = new RegExp(f, "i");
      positive.push((s:any) => regex.test(s));
    }
  });
  const filter = (item: any) => {
    if (item) {
      const apply = (f:any) => f(`${item.name}`);
      return (positive.length === 0 || positive.some(apply)) &&
        (negative.length === 0 || negative.every(apply));
    }
    return false;
  };
  return filter;
}

const parentNodeKinds = ["entry", "WindowManagerState"];

export function isParentNode(kind: string) {
  return parentNodeKinds.includes(kind);
}

export function isVisibleNode(kind: string, type?: string) {
  return kind === "WindowState" || kind === "Activity" || type?.includes("Layer");
}
