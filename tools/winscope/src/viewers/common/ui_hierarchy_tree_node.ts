/*
 * Copyright 2024, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {Chip} from './chip';
import {DiffNode} from './diff_node';
import {DiffType} from './diff_type';

export class UiHierarchyTreeNode extends HierarchyTreeNode implements DiffNode {
  private chips: Chip[] = [];
  private diff: DiffType = DiffType.NONE;
  private displayName: string = this.name;
  private isOldNodeInternal = false;
  private showHeading = true;
  private nextNodeDfs: this | undefined;
  private prevNodeDfs: this | undefined;

  static from(
    node: HierarchyTreeNode,
    parent?: UiHierarchyTreeNode,
  ): UiHierarchyTreeNode {
    const displayNode = new UiHierarchyTreeNode(
      node.id,
      node.name,
      (node as any).propertiesProvider,
    );
    const rects = node.getRects();
    if (rects) displayNode.setRects(rects);

    if (parent) displayNode.setParent(parent);

    const zParent = node.getZParent();
    if (zParent) displayNode.setZParent(zParent);

    node
      .getRelativeChildren()
      .forEach((zChild) => displayNode.addRelativeChild(zChild));

    node.getAllChildren().forEach((child) => {
      displayNode.addOrReplaceChild(
        UiHierarchyTreeNode.from(child, displayNode),
      );
    });
    node.getWarnings().forEach((warning) => displayNode.addWarning(warning));

    return displayNode;
  }

  setDiff(diff: DiffType): void {
    this.diff = diff;
  }

  getDiff(): DiffType {
    return this.diff;
  }

  heading(): string | undefined {
    return this.showHeading ? this.id.split(' ')[0].split('.')[0] : undefined;
  }

  setShowHeading(value: boolean) {
    this.showHeading = value;
  }

  setDisplayName(name: string) {
    this.displayName = name;
  }

  getDisplayName(): string {
    return this.displayName;
  }

  addChip(chip: Chip): void {
    this.chips.push(chip);
  }

  getChips(): Chip[] {
    return this.chips;
  }

  setIsOldNode(value: boolean) {
    this.isOldNodeInternal = value;
  }

  isOldNode() {
    return this.isOldNodeInternal;
  }

  getNextDfs(): this | undefined {
    return this.nextNodeDfs;
  }

  getPrevDfs(): this | undefined {
    return this.prevNodeDfs;
  }

  assignDfsOrder() {
    if (!this.isRoot()) {
      console.warn('Attempted to assign DFS order from non-root node.');
      return;
    }

    let prev: this | undefined;
    this.forEachNodeDfs((node) => {
      if (prev) {
        prev.setNextDfs(node);
        node.setPrevDfs(prev);
      }
      prev = node;
    });
  }

  private setNextDfs(node: this) {
    this.nextNodeDfs = node;
  }

  private setPrevDfs(node: this) {
    this.prevNodeDfs = node;
  }
}
