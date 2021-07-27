/*
 * Copyright 2020, The Android Open Source Project
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

// TODO (b/162300507): Get rid of cloning
import ObjectFormatter from '../flickerlib/ObjectFormatter';

export const DiffType = Object.freeze({
  NONE: 'none',
  ADDED: 'added',
  DELETED: 'deleted',
  ADDED_MOVE: 'addedMove',
  DELETED_MOVE: 'deletedMove',
  MODIFIED: 'modified',
});

export function defaultModifiedCheck(newNode, oldNode) {
  if (!newNode && !oldNode) {
    return false;
  }

  if ((newNode && !oldNode) || (!newNode && oldNode)) {
    return true;
  }

  return !newNode.equals(oldNode);
}

export class DiffGenerator {
  constructor(tree) {
    this.tree = tree;
  }

  compareWith(tree) {
    this.diffWithTree = tree;
    return this;
  }

  withUniqueNodeId(getNodeId) {
    this.getNodeId = (node) => {
      const id = getNodeId(node);
      if (id === null || id === undefined) {
        console.error('Null node ID for node', node);
        throw new Error('Node ID can\'t be null or undefined');
      }
      return id;
    };
    return this;
  }

  withModifiedCheck(isModified) {
    this.isModified = isModified;
    return this;
  }

  generateDiffTree() {
    this.newMapping = this._generateIdToNodeMapping(this.tree);
    this.oldMapping = this.diffWithTree ?
      this._generateIdToNodeMapping(this.diffWithTree) : {};

    const diffTrees =
      this._generateDiffTree(this.tree, this.diffWithTree, [], []);

    let diffTree;
    if (diffTrees.length > 1) {
      diffTree = {
        kind: '',
        name: 'DiffTree',
        children: diffTrees,
        stableId: 'DiffTree',
      };
    } else {
      diffTree = diffTrees[0];
    }

    return Object.freeze(diffTree);
  }

  _generateIdToNodeMapping(node, acc) {
    acc = acc || {};

    const nodeId = this.getNodeId(node);

    if (acc[nodeId]) {
      throw new Error(`Duplicate node id '${nodeId}' detected...`);
    }

    acc[this.getNodeId(node)] = node;

    if (node.children) {
      for (const child of node.children) {
        this._generateIdToNodeMapping(child, acc);
      }
    }

    return acc;
  }

  _objIsEmpty(obj) {
    return Object.keys(obj).length === 0 && obj.constructor === Object;
  }

  _cloneNode(node) {
    const clone = ObjectFormatter.cloneObject(node);
    clone.children = node.children;
    clone.name = node.name;
    clone.kind = node.kind;
    clone.stableId = node.stableId;
    clone.shortName = node.shortName;
    return clone;
  }

  _generateDiffTree(newTree, oldTree, newTreeSiblings, oldTreeSiblings) {
    const diffTrees = [];

    // NOTE: A null ID represents a non existent node.
    const newId = newTree ? this.getNodeId(newTree) : null;
    const oldId = oldTree ? this.getNodeId(oldTree) : null;

    const newTreeSiblingIds = newTreeSiblings.map(this.getNodeId);
    const oldTreeSiblingIds = oldTreeSiblings.map(this.getNodeId);

    if (newTree) {
      // Deep clone newTree omitting children field
      // Clone is required because trees are frozen objects — we can't
      // modify the original tree object. Also means there is no side effect.
      const diffTree = this._cloneNode(newTree);

      // Default to no changes
      diffTree.diff = {type: DiffType.NONE};

      if (newId !== oldId) {
        // A move, addition, or deletion has occurred
        let nextOldTree;

        // Check if newTree has been added or moved
        if (!oldTreeSiblingIds.includes(newId)) {
          if (this.oldMapping[newId]) {
            // Objected existed in old tree, the counter party
            // DELETED_MOVE will be/has been flagged and added to the
            // diffTree when visiting it in the oldTree.
            diffTree.diff = {type: DiffType.ADDED_MOVE};

            // TODO: Figure out if oldTree is ever visited now...

            // Switch out oldTree for new one to compare against
            nextOldTree = this.oldMapping[newId];
          } else {
            diffTree.diff = {type: DiffType.ADDED};

            // TODO: Figure out if oldTree is ever visited now...

            // Stop comparing against oldTree
            nextOldTree = null;
          }
        }

        // Check if oldTree has been deleted of moved
        if (oldTree && !newTreeSiblingIds.includes(oldId)) {
          const deletedTreeDiff = this._cloneNode(oldTree);

          if (this.newMapping[oldId]) {
            deletedTreeDiff.diff = {type: DiffType.DELETED_MOVE};

            // Stop comparing against oldTree, will be/has been
            // visited when object is seen in newTree
            nextOldTree = null;
          } else {
            deletedTreeDiff.diff = {type: DiffType.DELETED};

            // Visit all children to check if they have been deleted or moved
            deletedTreeDiff.children = this._visitChildren(null, oldTree);
          }

          diffTrees.push(deletedTreeDiff);
        }

        oldTree = nextOldTree;
      } else {
        // TODO: Always have modified check and add modified tags on top of
        // others
        // NOTE: Doesn't check for moved and modified at the same time
        if (this.isModified && this.isModified(newTree, oldTree)) {
          diffTree.diff = {type: DiffType.MODIFIED};
        }
      }

      diffTree.children = this._visitChildren(newTree, oldTree);
      diffTrees.push(diffTree);
    } else if (oldTree) {
      if (!newTreeSiblingIds.includes(oldId)) {
        // Deep clone oldTree omitting children field
        const diffTree = this._cloneNode(oldTree);

        // newTree doesn't exists, oldTree has either been moved or deleted.
        if (this.newMapping[oldId]) {
          diffTree.diff = {type: DiffType.DELETED_MOVE};
        } else {
          diffTree.diff = {type: DiffType.DELETED};
        }

        diffTree.children = this._visitChildren(null, oldTree);
        diffTrees.push(diffTree);
      }
    } else {
      throw new Error('Both newTree and oldTree are undefined...');
    }

    return diffTrees;
  }

  _visitChildren(newTree, oldTree) {
    // Recursively traverse all children of new and old tree.
    const diffChildren = [];

    // TODO: Try replacing this with some sort of zipWith.
    const numOfChildren = Math.max(
        newTree?.children?.length ?? 0, oldTree?.children?.length ?? 0);
    for (let i = 0; i < numOfChildren; i++) {
      const newChild = i < newTree?.children?.length ?
        newTree.children[i] : null;

      const oldChild = i < oldTree?.children?.length ?
        oldTree.children[i] : null;

      const childDiffTrees = this._generateDiffTree(
          newChild, oldChild,
          newTree?.children ?? [], oldTree?.children ?? [],
      );
      diffChildren.push(...childDiffTrees);
    }

    return diffChildren;
  }
}
