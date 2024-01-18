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

import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {TreeNode} from 'trace/tree_node/tree_node';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {Filter} from './filter';

describe('Filter', () => {
  let hierarchyRoot: UiHierarchyTreeNode;
  let operation: Filter<UiHierarchyTreeNode>;

  describe('keeping parents and children', () => {
    beforeEach(() => {
      const filter = (item: TreeNode | undefined) => {
        if (item) {
          return item.name === 'keep';
        }
        return false;
      };
      operation = new Filter<UiHierarchyTreeNode>([filter], true);
    });

    it('discards leaf that does not match filter', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      hierarchyRoot.addChild(
        TreeNodeUtils.makeUiHierarchyNode({
          id: 'node',
          name: 'discard',
        })
      );
      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([]);
    });

    it('keeps leaf that matches filter', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'node',
        name: 'keep',
      });
      hierarchyRoot.addChild(child);
      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([child]);
    });

    it('discards node with children if node and children do not match filter', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'discard',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'discard',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([]);
    });

    it('keeps leaf that matches filter and its non-matching parent', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'discard',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'keep',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([parent]);
      expect(parent.getAllChildren()).toEqual([child]);
    });

    it('keeps parent that matches filter and its non-matching children', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'keep',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'discard',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([parent]);
      expect(parent.getAllChildren()).toEqual([child]);
    });

    it('keeps parent that matches filter and its matching children', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'keep',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'keep',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([parent]);
      expect(parent.getAllChildren()).toEqual([child]);
    });
  });

  describe('without keeping parents and children', () => {
    beforeEach(() => {
      const filter = (item: TreeNode | undefined) => {
        if (item) {
          return item.name === 'keep';
        }
        return false;
      };
      operation = new Filter<UiHierarchyTreeNode>([filter], false);
    });

    it('discards leaf that does not match filter', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      hierarchyRoot.addChild(
        TreeNodeUtils.makeUiHierarchyNode({
          id: 'node',
          name: 'discard',
        })
      );
      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([]);
    });

    it('keeps leaf that matches filter', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'node',
        name: 'keep',
      });
      hierarchyRoot.addChild(child);
      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([child]);
    });

    it('discards node with children if node and children do not match filter', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'discard',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'discard',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([]);
    });

    it('discards leaf that matches filter but has non-matching parent', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'discard',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'keep',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([]);
    });

    it('keeps parent that matches filter and discards its non-matching children', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'keep',
      });
      const childToKeep = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'keep',
      });
      const childToDiscard = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'discard',
      });
      parent.addChild(childToKeep);
      parent.addChild(childToDiscard);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([parent]);
      expect(parent.getAllChildren()).toEqual([childToKeep]);
    });

    it('keeps parent that matches filter and its matching children', () => {
      hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
        id: 'test',
        name: 'root',
      });
      const parent = TreeNodeUtils.makeUiHierarchyNode({
        id: 'parent',
        name: 'keep',
      });
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: 'child',
        name: 'keep',
      });
      parent.addChild(child);
      hierarchyRoot.addChild(parent);

      const root = operation.apply(hierarchyRoot);
      expect(root.getAllChildren()).toEqual([parent]);
      expect(parent.getAllChildren()).toEqual([child]);
    });
  });
});
