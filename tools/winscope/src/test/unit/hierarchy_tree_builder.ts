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
import {Chip} from 'viewers/common/chip';
import {HierarchyTreeNode} from 'viewers/common/ui_tree_utils';

class HierarchyTreeBuilder {
  stableId = '';
  name = '';
  kind = '';
  children: HierarchyTreeNode[] = [];
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
  showInFilteredView = true;
  showInOnlyVisibleView?: boolean;
  simplifyNames = false;
  chips: Chip[] = [];
  diffType?: string;
  skip?: any;

  setId(id: string | number) {
    this.id = id;
    return this;
  }

  setKind(kind: string) {
    this.kind = kind;
    return this;
  }

  setStableId(stableId: string) {
    this.stableId = stableId;
    return this;
  }

  setName(name: string) {
    this.name = name;
    return this;
  }

  setShortName(shortName: string) {
    this.shortName = shortName;
    return this;
  }

  setChips(chips: Chip[]) {
    this.chips = chips;
    return this;
  }

  setDiffType(diffType: string) {
    this.diffType = diffType;
    return this;
  }

  setChildren(children: HierarchyTreeNode[]) {
    this.children = children;
    return this;
  }

  setDisplayId(displayId: number) {
    this.displayId = displayId;
    return this;
  }

  setLayerId(layerId: number) {
    this.layerId = layerId;
    return this;
  }

  setStackId(stackId: number) {
    this.stackId = stackId;
    return this;
  }

  setIsVisible(isVisible: boolean) {
    this.isVisible = isVisible;
    return this;
  }

  setVisibleView(showInOnlyVisibleView: boolean) {
    this.showInOnlyVisibleView = showInOnlyVisibleView;
    return this;
  }

  setFilteredView(showInFilteredView: boolean) {
    this.showInFilteredView = showInFilteredView;
    return this;
  }

  setSimplifyNames(simplifyNames: boolean) {
    this.simplifyNames = simplifyNames;
    return this;
  }

  build(): HierarchyTreeNode {
    const node = new HierarchyTreeNode(this.name, this.kind, this.stableId, this.children);

    node.chips = this.chips;
    node.showInFilteredView = this.showInFilteredView;
    node.simplifyNames = this.simplifyNames;

    if (this.id) {
      node.id = this.id;
    }

    if (this.diffType) {
      node.diffType = this.diffType;
    }

    if (this.displayId) {
      node.displayId = this.displayId;
    }

    if (this.layerId) {
      node.layerId = this.layerId;
    }

    if (this.stackId) {
      node.stackId = this.stackId;
    }

    if (this.isVisible) {
      node.isVisible = this.isVisible;
    }

    if (this.showInOnlyVisibleView) {
      node.showInOnlyVisibleView = this.showInOnlyVisibleView;
    }

    if (this.shortName) {
      node.shortName = this.shortName;
    }

    return node;
  }
}

export {HierarchyTreeBuilder};
