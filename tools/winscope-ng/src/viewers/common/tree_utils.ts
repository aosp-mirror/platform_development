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
import { Layer, BaseLayerTraceEntry } from "common/trace/flickerlib/common";
import ObjectFormatter from "common/trace/flickerlib/ObjectFormatter";
import { UserOptions } from "viewers/common/user_options";
import { HWC_CHIP, GPU_CHIP, MISSING_LAYER, VISIBLE_CHIP, RELATIVE_Z_CHIP, RELATIVE_Z_PARENT_CHIP } from "viewers/common/chip";

type GetNodeIdCallbackType = (node: Tree | null) => number | null;
type IsModifiedCallbackType = (newTree: Tree | null, oldTree: Tree | null) => boolean;
interface IdNodeMap { [key: string]: Tree }
const DiffType = {
  NONE: "none",
  ADDED: "added",
  DELETED: "deleted",
  ADDED_MOVE: "addedMove",
  DELETED_MOVE: "deletedMove",
  MODIFIED: "modified",
};
const HwcCompositionType = {
  CLIENT: 1,
  DEVICE: 2,
  SOLID_COLOR: 3,
};

export type FilterType = (item: Tree | null) => boolean;
export type Tree = Layer | BaseLayerTraceEntry;

export function diffClass(item: Tree): string {
  const diff = item!.diff;
  return diff ?? "";
}

export function isHighlighted(item: Tree, highlightedItems: Array<string>) {
  return highlightedItems.includes(`${item.id}`);
}

export function getFilter(filterString: string): FilterType {
  const filterStrings = filterString.split(",");
  const positive: Tree | null[] = [];
  const negative: Tree | null[] = [];
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
  const filter = (item:Tree | null) => {
    if (item) {
      const apply = (f:any) => f(`${item.name}`);
      return (positive.length === 0 || positive.some(apply)) &&
        (negative.length === 0 || negative.every(apply));
    }
    return false;
  };
  return filter;
}

export class TreeGenerator {
  private userOptions: UserOptions;
  private filter: FilterType;
  private tree: Tree;
  private diffWithTree: Tree | null = null;
  private getNodeId?: GetNodeIdCallbackType;
  private isModified?: IsModifiedCallbackType;
  private newMapping: IdNodeMap | null = null;
  private oldMapping: IdNodeMap | null = null;
  private readonly pinnedIds: Array<string>;
  private pinnedItems: Array<Tree> = [];
  private relZParentIds: Array<number> = [];
  private flattenedChildren: Array<Tree> = [];

  constructor(tree: Tree, userOptions: UserOptions, filter: FilterType, pinnedIds?: Array<string>) {
    this.tree = tree;
    this.userOptions = userOptions;
    this.filter = filter;
    this.pinnedIds = pinnedIds ?? [];
  }

  public generateTree(): Tree {
    return this.getCustomisedTree(this.tree);
  }

  public compareWith(tree: Tree | null): TreeGenerator {
    this.diffWithTree = tree;
    return this;
  }

  public withUniqueNodeId(getNodeId?: GetNodeIdCallbackType): TreeGenerator {
    this.getNodeId = (node: Tree | null) => {
      const id = getNodeId ? getNodeId(node) : this.defaultNodeIdCallback(node);
      if (id === null || id === undefined) {
        console.error("Null node ID for node", node);
        throw new Error("Node ID can't be null or undefined");
      }
      return id;
    };
    return this;
  }

  public withModifiedCheck(isModified?: IsModifiedCallbackType): TreeGenerator {
    this.isModified = isModified ?? this.defaultModifiedCheck;
    return this;
  }

  public generateFinalDiffTree(): Tree {
    this.newMapping = this.generateIdToNodeMapping(this.tree);
    this.oldMapping = this.diffWithTree ? this.generateIdToNodeMapping(this.diffWithTree) : null;

    const diffTrees = this.generateDiffTree(this.tree, this.diffWithTree, [], []);

    let diffTree;
    if (diffTrees.length > 1) {
      diffTree = {
        kind: "",
        name: "DiffTree",
        children: diffTrees,
        stableId: "DiffTree",
      };
    } else {
      diffTree = diffTrees[0];
    }
    return this.getCustomisedTree(diffTree);
  }

  private getCustomisedTree(tree: Tree | null) {
    if (!tree) return null;
    tree = this.generateTreeWithUserOptions(tree, false);
    tree = this.updateTreeWithRelZParentChips(tree);

    if (this.isFlatView() && tree.children) {
      this.flattenChildren(tree.children);
      tree.children = this.flattenedChildren;
    }
    return Object.freeze(tree);
  }

  public getPinnedItems() {
    return this.pinnedItems;
  }

  private flattenChildren(children: Array<Tree>): Tree {
    for (let i = 0; i < children.length; i++) {
      const child = children[i];
      const showInOnlyVisibleView = this.isOnlyVisibleView() && child.isVisible;
      const passVisibleCheck = !this.isOnlyVisibleView() || showInOnlyVisibleView;
      if (this.filterMatches(child) && passVisibleCheck) {
        this.flattenedChildren.push(child);
      }
      if (child.children) {
        this.flattenChildren(child.children);
      }
    }
  }

  private isOnlyVisibleView(): boolean {
    return this.userOptions["onlyVisible"]?.enabled ?? false;
  }

  private isSimplifyNames(): boolean {
    return this.userOptions["simplifyNames"]?.enabled ?? false;
  }

  private isFlatView(): boolean {
    return this.userOptions["flat"]?.enabled ?? false;
  }

  private filterMatches(item: Tree | null): boolean {
    return this.filter(item) ?? false;
  }

  private generateTreeWithUserOptions(tree: Tree | null, parentFilterMatch: boolean): Tree | null {
    return tree ? this.applyChecks(tree, this.cloneNode(tree), parentFilterMatch) : null;
  }

  private updateTreeWithRelZParentChips(tree: Tree): Tree {
    return this.applyRelZParentCheck(tree);
  }

  private applyRelZParentCheck(tree: Tree) {
    if (this.relZParentIds.includes(tree.id)) {
      tree.chips.push(RELATIVE_Z_PARENT_CHIP);
    }

    const numOfChildren = tree.children?.length ?? 0;
    for (let i = 0; i < numOfChildren; i++) {
      tree.children[i] = this.updateTreeWithRelZParentChips(tree.children[i]);
    }

    return tree;
  }

  private addChips(tree: Tree) {
    tree.chips = [];
    if (tree.hwcCompositionType == HwcCompositionType.CLIENT) {
      tree.chips.push(GPU_CHIP);
    } else if ((tree.hwcCompositionType == HwcCompositionType.DEVICE || tree.hwcCompositionType == HwcCompositionType.SOLID_COLOR)) {
      tree.chips.push(HWC_CHIP);
    }
    if (tree.isVisible && tree.kind !== "entry") {
      tree.chips.push(VISIBLE_CHIP);
    }
    if (tree.zOrderRelativeOfId !== -1 && tree.kind !== "entry" && !tree.isRootLayer) {
      tree.chips.push(RELATIVE_Z_CHIP);
      this.relZParentIds.push(tree.zOrderRelativeOfId);
    }
    if (tree.isMissing) {
      tree.chips.push(MISSING_LAYER);
    }
    return tree;
  }

  private applyChecks(tree: Tree | null, newTree: Tree | null, parentFilterMatch: boolean): Tree | null {
    if (!tree || !newTree)  {
      return null;
    }

    // simplify names check
    newTree.simplifyNames = this.isSimplifyNames();

    // check item either matches filter, or has parents/children matching filter
    if (tree.kind === "entry" || parentFilterMatch) {
      newTree.showInFilteredView = true;
    } else {
      newTree.showInFilteredView = this.filterMatches(tree);
      parentFilterMatch = newTree.showInFilteredView;
    }

    if (this.isOnlyVisibleView()) {
      newTree.showInOnlyVisibleView = newTree.isVisible;
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

        if (this.isOnlyVisibleView() && newTreeChild.showInOnlyVisibleView) {
          newTree.showInOnlyVisibleView = true;
        }

        newTree.children.push(newTreeChild);
      }
    }

    const doNotShowInOnlyVisibleView = this.isOnlyVisibleView() && !newTree.showInOnlyVisibleView;
    if (!newTree.showInFilteredView || doNotShowInOnlyVisibleView) {
      return null;
    }

    newTree = this.addChips(newTree);

    if (this.pinnedIds.includes(`${newTree.id}`)) {
      this.pinnedItems.push(newTree);
    }

    return newTree;
  }

  private generateIdToNodeMapping(node: Tree, acc?: IdNodeMap): IdNodeMap {
    acc = acc || {};

    const nodeId = this.getNodeId!(node)!;

    if (acc[nodeId]) {
      throw new Error(`Duplicate node id '${nodeId}' detected...`);
    }
    acc[nodeId] = node;

    if (node.children) {
      for (const child of node.children) {
        this.generateIdToNodeMapping(child, acc);
      }
    }
    return acc;
  }

  private cloneNode(node: Tree | null): Tree | null {
    const clone = ObjectFormatter.cloneObject(node);
    if (node) {
      clone.children = node.children;
      clone.name = node.name;
      clone.kind = node.kind;
      clone.stableId = node.stableId;
      clone.shortName = node.shortName;
      if ("chips" in node) {
        clone.chips = node.chips.slice();
      }
      if ("diff" in node) {
        clone.diff = node.diff;
      }
    }
    return clone;
  }

  private generateDiffTree(
    newTree: Tree | null,
    oldTree: Tree | null,
    newTreeSiblings: Array<Tree | null>,
    oldTreeSiblings: Array<Tree | null>
  ): Array<Tree | null> {
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
      const diffTree = this.cloneNode(newTree)!;

      // Default to no changes
      diffTree.diff = DiffType.NONE;

      if (newTree.kind !== "entry" && newId !== oldId) {
        // A move, addition, or deletion has occurred
        let nextOldTree = null;

        // Check if newTree has been added or moved
        if (newId && !oldTreeSiblingIds.includes(newId)) {
          if (this.oldMapping && this.oldMapping[newId]) {
            // Objected existed in old tree, so DELETED_MOVE will be/has been flagged and added to the
            // diffTree when visiting it in the oldTree.
            diffTree.diff = DiffType.ADDED_MOVE;

            // Switch out oldTree for new one to compare against
            nextOldTree = this.oldMapping[newId];
          } else {
            diffTree.diff = DiffType.ADDED;

            // Stop comparing against oldTree
            nextOldTree = null;
          }
        }

        // Check if oldTree has been deleted of moved
        if (oldId && oldTree && !newTreeSiblingIds.includes(oldId)) {
          const deletedTreeDiff = this.cloneNode(oldTree)!;

          if (this.newMapping![oldId]) {
            deletedTreeDiff.diff = DiffType.DELETED_MOVE;

            // Stop comparing against oldTree, will be/has been
            // visited when object is seen in newTree
            nextOldTree = null;
          } else {
            deletedTreeDiff.diff = DiffType.DELETED;

            // Visit all children to check if they have been deleted or moved
            deletedTreeDiff.children = this.visitChildren(null, oldTree);
          }

          diffTrees.push(deletedTreeDiff);
        }

        oldTree = nextOldTree;
      } else {
        if (this.isModified && this.isModified(newTree, oldTree)) {
          diffTree.diff = DiffType.MODIFIED;
        }
      }

      diffTree.children = this.visitChildren(newTree, oldTree);
      diffTrees.push(diffTree);
    } else if (oldTree) {
      if (oldId && !newTreeSiblingIds.includes(oldId)) {
        // Deep clone oldTree omitting children field
        const diffTree = this.cloneNode(oldTree)!;

        // newTree doesn't exist, oldTree has either been moved or deleted.
        if (this.newMapping![oldId]) {
          diffTree.diff = DiffType.DELETED_MOVE;
        } else {
          diffTree.diff = DiffType.DELETED;
        }

        diffTree.children = this.visitChildren(null, oldTree);
        diffTrees.push(diffTree);
      }
    } else {
      throw new Error("Both newTree and oldTree are undefined...");
    }

    return diffTrees;
  }

  private visitChildren(newTree: Tree | null, oldTree: Tree | null) {
    // Recursively traverse all children of new and old tree.
    const diffChildren = [];

    if (!newTree) newTree = {};
    if (!oldTree) oldTree = {};
    const numOfChildren = Math.max(newTree.children?.length ?? 0, oldTree.children?.length ?? 0);
    for (let i = 0; i < numOfChildren; i++) {
      const newChild = newTree.children ? newTree.children[i] : null;
      const oldChild = oldTree.children ? oldTree.children[i] : null;

      const childDiffTrees = this.generateDiffTree(
        newChild, oldChild,
        newTree.children ?? [], oldTree.children ?? [],
      );
      diffChildren.push(...childDiffTrees);
    }

    return diffChildren;
  }

  private defaultNodeIdCallback(node: Tree | null): number | null {
    return node ? node.stableId : null;
  }

  private defaultModifiedCheck(newNode: Tree | null, oldNode: Tree | null): boolean {
    if (!newNode && !oldNode) {
      return false;
    } else if (newNode && newNode.kind==="entry") {
      return false;
    } else if ((newNode && !oldNode) || (!newNode && oldNode)) {
      return true;
    }
    return !newNode.equals(oldNode);
  }
}
