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

import {assertDefined} from 'common/assert_utils';
import {TreeNode} from 'trace/tree_node/tree_node';
import {DiffNode} from 'viewers/common/diff_node';
import {DiffType} from 'viewers/common/diff_type';

export abstract class AddDiffs<T extends DiffNode> {
  private newIdNodeMap = new Map<string, T>();
  private oldIdNodeMap = new Map<string, T>();
  protected abstract addDiffsToNewRoot: boolean;
  protected abstract processModifiedNodes(newNode: T, oldNode: T): void;
  protected abstract processOldNode(oldNode: T): void;

  constructor(
    private isModified: IsModifiedCallbackType,
    private denylistProperties: string[],
  ) {}

  async executeInPlace(newRoot: T, oldRoot?: T): Promise<void> {
    this.newIdNodeMap = this.updateIdNodeMap(newRoot);
    this.oldIdNodeMap = this.updateIdNodeMap(oldRoot);
    await this.generateDiffNodes(newRoot, oldRoot, [], []);
  }

  private updateIdNodeMap(root: T | undefined): Map<string, T> {
    const idNodeMap = new Map<string, T>();

    root?.forEachNodeDfs((node) => {
      idNodeMap.set(node.id, node);
    });

    return idNodeMap;
  }

  private async generateDiffNodes(
    newNode: T | undefined,
    oldNode: T | undefined,
    newNodeSiblingIds: string[],
    oldNodeSiblingIds: string[],
  ): Promise<T[]> {
    const diffNodes: T[] = [];

    if (!oldNode && !newNode) {
      console.error('both new and old trees undefined');
      return diffNodes;
    }

    if (!newNode && oldNode) {
      if (!newNodeSiblingIds.includes(oldNode.id)) {
        //oldNode deleted or moved
        if (this.newIdNodeMap.get(oldNode.id)) {
          oldNode.setDiff(DiffType.DELETED_MOVE);
        } else {
          oldNode.setDiff(DiffType.DELETED);
        }
        const newChildren = await this.visitChildren(undefined, oldNode);
        oldNode.removeAllChildren();
        newChildren.forEach((child) => {
          assertDefined(oldNode).addOrReplaceChild(child);
        });
        this.processOldNode(oldNode);
        diffNodes.push(oldNode);
      }
      return diffNodes;
    }

    newNode = assertDefined(newNode);

    if (!newNode.isRoot() && newNode.id !== oldNode?.id) {
      let nextOldNode: T | undefined;

      if (!oldNodeSiblingIds.includes(newNode.id)) {
        if (this.oldIdNodeMap.get(newNode.id)) {
          if (this.addDiffsToNewRoot) {
            newNode.setDiff(DiffType.ADDED_MOVE);
            nextOldNode = this.oldIdNodeMap.get(newNode.id);
          }
        } else {
          newNode.setDiff(DiffType.ADDED);
          nextOldNode = undefined; //newNode has no equiv in old tree
        }
      }

      if (oldNode && !newNodeSiblingIds.includes(oldNode.id)) {
        if (this.newIdNodeMap.get(oldNode.id)) {
          //oldNode still exists
          oldNode.setDiff(DiffType.DELETED_MOVE);
          nextOldNode = undefined;
        } else {
          oldNode.setDiff(DiffType.DELETED);

          const newChildren = await this.visitChildren(undefined, oldNode);
          oldNode.removeAllChildren();

          newChildren.forEach((child) => {
            assertDefined(oldNode).addOrReplaceChild(child);
          });
        }
        this.processOldNode(oldNode);
        diffNodes.push(oldNode);
      }

      oldNode = nextOldNode;
    } else if (!newNode.isRoot()) {
      if (
        oldNode &&
        oldNode.id === newNode.id &&
        (await this.isModified(newNode, oldNode, this.denylistProperties))
      ) {
        this.processModifiedNodes(newNode, oldNode);
      }
    }

    const newChildren = await this.visitChildren(newNode, oldNode);
    newNode.removeAllChildren();
    newChildren.forEach((child) =>
      assertDefined(newNode).addOrReplaceChild(child),
    );

    diffNodes.push(newNode);
    return diffNodes;
  }

  async visitChildren(
    newNode: T | undefined,
    oldNode: T | undefined,
  ): Promise<T[]> {
    const diffChildren: T[] = [];
    const numOfChildren = Math.max(
      newNode?.getAllChildren().length ?? 0,
      oldNode?.getAllChildren().length ?? 0,
    );
    for (let i = 0; i < numOfChildren; i++) {
      const newChild = newNode?.getAllChildren()[i];
      let oldChild = oldNode?.getAllChildren()[i];

      if (!oldChild && newChild) {
        oldChild = oldNode
          ?.getAllChildren()
          .find((node) => node.name === newChild.name);
      }

      const childDiffTrees = await this.generateDiffNodes(
        newChild,
        oldChild,
        newNode?.getAllChildren().map((child) => child.id) ?? [],
        oldNode?.getAllChildren().map((child) => child.id) ?? [],
      );
      childDiffTrees.forEach((child) => diffChildren.push(child));
    }

    return diffChildren;
  }
}

export type IsModifiedCallbackType = (
  newTree: TreeNode | undefined,
  oldTree: TreeNode | undefined,
  denylistProperties: string[],
) => Promise<boolean>;
