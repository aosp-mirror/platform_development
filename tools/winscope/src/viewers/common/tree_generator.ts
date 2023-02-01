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
import {FilterType, TreeNode} from 'common/tree_utils';
import {ObjectFormatter} from 'trace/flickerlib/ObjectFormatter';
import {TraceTreeNode} from 'trace/trace_tree_node';
import {
  GPU_CHIP,
  HWC_CHIP,
  MISSING_LAYER,
  RELATIVE_Z_CHIP,
  RELATIVE_Z_PARENT_CHIP,
  VISIBLE_CHIP,
} from 'viewers/common/chip';
import {DiffType, HierarchyTreeNode, UiTreeUtils} from './ui_tree_utils';

type GetNodeIdCallbackType = (node: TraceTreeNode | null) => string | null;
type IsModifiedCallbackType = (
  newTree: TraceTreeNode | null,
  oldTree: TraceTreeNode | null
) => boolean;

const HwcCompositionType = {
  CLIENT: 1,
  DEVICE: 2,
  SOLID_COLOR: 3,
};

export class TreeGenerator {
  private isOnlyVisibleView = false;
  private isSimplifyNames = false;
  private isFlatView = false;
  private filter: FilterType;
  private inputEntry: TraceTreeNode;
  private previousEntry: TraceTreeNode | null = null;
  private getNodeId?: GetNodeIdCallbackType;
  private isModified?: IsModifiedCallbackType;
  private newMapping: Map<string, TraceTreeNode> | null = null;
  private oldMapping: Map<string, TraceTreeNode> | null = null;
  private readonly pinnedIds: string[];
  private pinnedItems: HierarchyTreeNode[] = [];
  private relZParentIds: string[] = [];
  private flattenedChildren: HierarchyTreeNode[] = [];

  constructor(inputEntry: TraceTreeNode, filter: FilterType, pinnedIds?: string[]) {
    this.inputEntry = inputEntry;
    this.filter = filter;
    this.pinnedIds = pinnedIds ?? [];
  }

  setIsOnlyVisibleView(enabled: boolean): TreeGenerator {
    this.isOnlyVisibleView = enabled;
    return this;
  }

  setIsSimplifyNames(enabled: boolean): TreeGenerator {
    this.isSimplifyNames = enabled;
    return this;
  }

  setIsFlatView(enabled: boolean): TreeGenerator {
    this.isFlatView = enabled;
    return this;
  }

  generateTree(): HierarchyTreeNode | null {
    return this.getCustomizedTree(this.inputEntry);
  }

  compareWith(previousEntry: TraceTreeNode | null): TreeGenerator {
    this.previousEntry = previousEntry;
    return this;
  }

  withUniqueNodeId(getNodeId?: GetNodeIdCallbackType): TreeGenerator {
    this.getNodeId = (node: TraceTreeNode | null) => {
      const id = getNodeId ? getNodeId(node) : this.defaultNodeIdCallback(node);
      if (id === null || id === undefined) {
        console.error('Null node ID for node', node);
        throw new Error("Node ID can't be null or undefined");
      }
      return id;
    };
    return this;
  }

  withModifiedCheck(isModified?: IsModifiedCallbackType): TreeGenerator {
    this.isModified = isModified ?? this.defaultModifiedCheck;
    return this;
  }

  generateFinalTreeWithDiff(): HierarchyTreeNode | null {
    this.newMapping = this.generateIdToNodeMapping(this.inputEntry);
    this.oldMapping = this.previousEntry ? this.generateIdToNodeMapping(this.previousEntry) : null;

    const diffTrees = this.generateDiffTree(this.inputEntry, this.previousEntry, [], []);

    let diffTree: TraceTreeNode;
    if (diffTrees.length > 1) {
      diffTree = {
        kind: '',
        name: 'DiffTree',
        parent: undefined,
        children: diffTrees,
        stableId: 'DiffTree',
      };
    } else {
      diffTree = diffTrees[0];
    }
    return this.getCustomizedTree(diffTree);
  }

  private getCustomizedTree(tree: TraceTreeNode | null): HierarchyTreeNode | null {
    if (!tree) return null;
    let newTree = this.generateTreeWithUserOptions(tree, false);
    if (!newTree) return null;
    newTree = this.updateTreeWithRelZParentChips(newTree);

    if (this.isFlatView && newTree.children) {
      this.flattenChildren(newTree.children);
      newTree.children = this.flattenedChildren;
    }
    return Object.freeze(newTree);
  }

  getPinnedItems(): HierarchyTreeNode[] {
    return this.pinnedItems;
  }

  private flattenChildren(children: HierarchyTreeNode[]) {
    for (let i = 0; i < children.length; i++) {
      const child = children[i];
      const childIsVisibleNode =
        child.isVisible && UiTreeUtils.isVisibleNode(child.kind, child.type);
      const showInOnlyVisibleView = this.isOnlyVisibleView && childIsVisibleNode;
      const passVisibleCheck = !this.isOnlyVisibleView || showInOnlyVisibleView;
      if (this.filterMatches(child) && passVisibleCheck) {
        this.flattenedChildren.push(child);
      }
      if (child.children) {
        this.flattenChildren(child.children);
      }
    }
  }

  private filterMatches(item?: TreeNode | null): boolean {
    return this.filter(item) ?? false;
  }

  private generateTreeWithUserOptions(
    tree: TraceTreeNode,
    parentFilterMatch: boolean
  ): HierarchyTreeNode | null {
    return this.applyChecks(tree, parentFilterMatch);
  }

  private updateTreeWithRelZParentChips(tree: HierarchyTreeNode): HierarchyTreeNode {
    return this.applyRelZParentCheck(tree);
  }

  private applyRelZParentCheck(tree: HierarchyTreeNode) {
    if (tree.id && this.relZParentIds.includes(`${tree.id}`)) {
      tree.chips.push(RELATIVE_Z_PARENT_CHIP);
    }

    const numOfChildren = tree.children?.length ?? 0;
    for (let i = 0; i < numOfChildren; i++) {
      tree.children[i] = this.updateTreeWithRelZParentChips(tree.children[i]);
    }

    return tree;
  }

  private addChips(tree: HierarchyTreeNode): HierarchyTreeNode {
    if (tree.hwcCompositionType === HwcCompositionType.CLIENT) {
      tree.chips.push(GPU_CHIP);
    } else if (
      tree.hwcCompositionType === HwcCompositionType.DEVICE ||
      tree.hwcCompositionType === HwcCompositionType.SOLID_COLOR
    ) {
      tree.chips.push(HWC_CHIP);
    }
    if (tree.isVisible && UiTreeUtils.isVisibleNode(tree.kind, tree.type)) {
      tree.chips.push(VISIBLE_CHIP);
    }
    if (
      tree.zOrderRelativeOfId !== undefined &&
      tree.zOrderRelativeOfId !== -1 &&
      !UiTreeUtils.isParentNode(tree.kind) &&
      !tree.isRootLayer
    ) {
      tree.chips.push(RELATIVE_Z_CHIP);
      this.relZParentIds.push(`${tree.zOrderRelativeOfId}`);
    }
    if (tree.isMissing) {
      tree.chips.push(MISSING_LAYER);
    }
    return tree;
  }

  private applyChecks(tree: TraceTreeNode, parentFilterMatch: boolean): HierarchyTreeNode | null {
    let newTree = this.makeTreeNode(tree);

    // add id field to tree if id does not exist (e.g. for WM traces)
    if (!newTree?.id && newTree?.layerId) {
      newTree.id = newTree.layerId;
    }

    // simplify names check
    newTree.simplifyNames = this.isSimplifyNames;

    // check item either matches filter, or has parents/children matching filter
    if (UiTreeUtils.isParentNode(tree.kind) || parentFilterMatch) {
      newTree.showInFilteredView = true;
    } else {
      newTree.showInFilteredView = this.filterMatches(tree);
      parentFilterMatch = newTree.showInFilteredView;
    }

    if (this.isOnlyVisibleView) {
      newTree.showInOnlyVisibleView = UiTreeUtils.isParentNode(tree.kind)
        ? true
        : newTree.isVisible;
    }

    newTree.children = [];
    const numOfChildren = tree.children?.length ?? 0;
    for (let i = 0; i < numOfChildren; i++) {
      const child = tree.children[i];
      const newTreeChild = this.generateTreeWithUserOptions(child, parentFilterMatch);

      if (newTreeChild) {
        if (newTreeChild.showInFilteredView) {
          newTree.showInFilteredView = true;
        }

        if (this.isOnlyVisibleView && newTreeChild.showInOnlyVisibleView) {
          newTree.showInOnlyVisibleView = true;
        }

        newTree.children.push(newTreeChild);
      }
    }

    const doNotShowInOnlyVisibleView = this.isOnlyVisibleView && !newTree.showInOnlyVisibleView;
    if (!newTree.showInFilteredView || doNotShowInOnlyVisibleView) {
      return null;
    }

    newTree = this.addChips(newTree);

    if (this.pinnedIds.includes(`${newTree.id}`)) {
      this.pinnedItems.push(newTree);
    }

    return newTree;
  }

  private generateIdToNodeMapping(
    node: TraceTreeNode,
    acc?: Map<string, TraceTreeNode>
  ): Map<string, TraceTreeNode> {
    acc = acc || new Map<string, TraceTreeNode>();

    const nodeId: string = this.getNodeId!(node)!;

    if (acc.get(nodeId)) {
      throw new Error(`Duplicate node id '${nodeId}' detected...`);
    }
    acc.set(nodeId, node);

    if (node.children) {
      for (const child of node.children) {
        this.generateIdToNodeMapping(child, acc);
      }
    }
    return acc;
  }

  private cloneDiffTreeNode(node: TraceTreeNode | null): TraceTreeNode | null {
    const clone = ObjectFormatter.cloneObject(node);
    if (node) {
      clone.children = node.children;
      clone.name = node.name;
      clone.kind = node.kind;
      clone.stableId = node.stableId;
      clone.shortName = node.shortName;
    }
    return clone;
  }

  private makeTreeNode(node: TraceTreeNode): HierarchyTreeNode {
    const clone = new HierarchyTreeNode(node.name, node.kind, node.stableId);
    if (node.shortName) clone.shortName = node.shortName;
    if (node.type) clone.type = node.type;
    if (node.id) clone.id = node.id;
    if (node.layerId) clone.layerId = node.layerId;
    if (node.isVisible) clone.isVisible = node.isVisible;
    if (node.isMissing) clone.isMissing = node.isMissing;
    if (node.hwcCompositionType) clone.hwcCompositionType = node.hwcCompositionType;
    if (node.zOrderRelativeOfId) clone.zOrderRelativeOfId = node.zOrderRelativeOfId;
    if (node.isRootLayer) clone.isRootLayer = node.isRootLayer;
    if (node.diffType) clone.diffType = node.diffType;
    if (node.skip) clone.skip = node.skip;

    return clone;
  }

  private generateDiffTree(
    newTree: TraceTreeNode | null,
    oldTree: TraceTreeNode | null,
    newTreeSiblings: Array<TraceTreeNode | null>,
    oldTreeSiblings: Array<TraceTreeNode | null>
  ): TraceTreeNode[] {
    const diffTrees = [];
    // NOTE: A null ID represents a non existent node.
    if (!this.getNodeId) {
      return [];
    }
    const newId = newTree ? this.getNodeId(newTree) : null;
    const oldId = oldTree ? this.getNodeId(oldTree) : null;

    const newTreeSiblingIds = newTreeSiblings.map(this.getNodeId);
    const oldTreeSiblingIds = oldTreeSiblings.map(this.getNodeId);

    if (newTree) {
      // Clone is required because trees are frozen objects — we can't modify the original tree object.
      const diffTree = this.cloneDiffTreeNode(newTree)!;

      // Default to no changes
      diffTree.diffType = DiffType.NONE;

      if (!UiTreeUtils.isParentNode(newTree.kind) && newId !== oldId) {
        // A move, addition, or deletion has occurred
        let nextOldTree: TraceTreeNode | null = null;

        // Check if newTree has been added or moved
        if (newId && !oldTreeSiblingIds.includes(newId)) {
          if (this.oldMapping && this.oldMapping.get(newId)) {
            // Objected existed in old tree, so DELETED_MOVE will be/has been flagged and added to the
            // diffTree when visiting it in the oldTree.
            diffTree.diffType = DiffType.ADDED_MOVE;

            // Switch out oldTree for new one to compare against
            nextOldTree = this.oldMapping.get(newId) ?? null;
          } else {
            diffTree.diffType = DiffType.ADDED;

            // Stop comparing against oldTree
            nextOldTree = null;
          }
        }

        // Check if oldTree has been deleted of moved
        if (oldId && oldTree && !newTreeSiblingIds.includes(oldId)) {
          const deletedTreeDiff = this.cloneDiffTreeNode(oldTree)!;

          if (this.newMapping && this.newMapping.get(oldId)) {
            deletedTreeDiff.diffType = DiffType.DELETED_MOVE;

            // Stop comparing against oldTree, will be/has been
            // visited when object is seen in newTree
            nextOldTree = null;
          } else {
            deletedTreeDiff.diffType = DiffType.DELETED;

            // Visit all children to check if they have been deleted or moved
            deletedTreeDiff.children = this.visitChildren(null, oldTree);
          }

          diffTrees.push(deletedTreeDiff);
        }

        oldTree = nextOldTree;
      } else {
        if (this.isModified && this.isModified(newTree, oldTree)) {
          diffTree.diffType = DiffType.MODIFIED;
        }
      }

      diffTree.children = this.visitChildren(newTree, oldTree);
      diffTrees.push(diffTree);
    } else if (oldTree) {
      if (oldId && !newTreeSiblingIds.includes(oldId)) {
        // Deep clone oldTree omitting children field
        const diffTree = this.cloneDiffTreeNode(oldTree)!;

        // newTree doesn't exist, oldTree has either been moved or deleted.
        if (this.newMapping && this.newMapping.get(oldId)) {
          diffTree.diffType = DiffType.DELETED_MOVE;
        } else {
          diffTree.diffType = DiffType.DELETED;
        }

        diffTree.children = this.visitChildren(null, oldTree);
        diffTrees.push(diffTree);
      }
    } else {
      throw new Error('Both newTree and oldTree are undefined...');
    }

    return diffTrees;
  }

  private visitChildren(
    newTree: TraceTreeNode | null,
    oldTree: TraceTreeNode | null
  ): TraceTreeNode[] {
    // Recursively traverse all children of new and old tree.
    const diffChildren = [];
    const numOfChildren = Math.max(newTree?.children?.length ?? 0, oldTree?.children?.length ?? 0);
    for (let i = 0; i < numOfChildren; i++) {
      const newChild = newTree?.children ? newTree.children[i] : null;
      const oldChild = oldTree?.children ? oldTree.children[i] : null;

      const childDiffTrees = this.generateDiffTree(
        newChild,
        oldChild,
        newTree?.children ?? [],
        oldTree?.children ?? []
      ).filter((tree) => tree != null);
      diffChildren.push(...childDiffTrees);
    }

    return diffChildren;
  }

  private defaultNodeIdCallback(node: TraceTreeNode | null): string | null {
    return node ? node.stableId : null;
  }

  private defaultModifiedCheck(
    newNode: TraceTreeNode | null,
    oldNode: TraceTreeNode | null
  ): boolean {
    if (!newNode && !oldNode) {
      return false;
    } else if (newNode && UiTreeUtils.isParentNode(newNode.kind)) {
      return false;
    } else if ((newNode && !oldNode) || (!newNode && oldNode)) {
      return true;
    } else if (newNode?.equals) {
      return !newNode.equals(oldNode);
    }
    return false;
  }
}
