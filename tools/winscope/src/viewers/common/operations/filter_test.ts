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

import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {TreeNode} from 'trace/tree_node/tree_node';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {Filter} from './filter';

describe('Filter', () => {
  let hierarchyRoot: UiHierarchyTreeNode;
  let operation: Filter<UiHierarchyTreeNode>;

  describe('keeping parents and children', () => {
    beforeEach(() => {
      jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
      const filter = (item: TreeNode | undefined) => {
        if (item) {
          return item.name === 'keep';
        }
        return false;
      };
      operation = new Filter<UiHierarchyTreeNode>([filter], true);
    });

    it('discards leaf that does not match filter', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([{id: 'node', name: 'discard'}])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test').setName('root').build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps leaf that matches filter', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([{id: 'node', name: 'keep'}])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([{id: 'node', name: 'keep'}])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('discards node with children if node and children do not match filter', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'discard',
              children: [
                {
                  id: 'node',
                  name: 'discard',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test').setName('root').build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps leaf that matches filter and its non-matching parent', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'discard',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'discard',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps parent that matches filter and its non-matching children', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'discard',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'discard',
                },
              ],
            },
          ])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps parent that matches filter and its matching children', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('applies filter to children even if root matches', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('keep')
          .setChildren([{id: 'node', name: 'discard'}])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test').setName('keep').build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });
  });

  describe('without keeping parents and children', () => {
    beforeEach(() => {
      jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
      const filter = (item: TreeNode | undefined) => {
        if (item) {
          return item.name === 'keep';
        }
        return false;
      };
      operation = new Filter<UiHierarchyTreeNode>([filter], false);
    });

    it('discards leaf that does not match filter', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([{id: 'node', name: 'discard'}])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test').setName('root').build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps leaf that matches filter', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([{id: 'node', name: 'keep'}])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([{id: 'node', name: 'keep'}])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('discards node with children if node and children do not match filter', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'discard',
              children: [
                {
                  id: 'child',
                  name: 'discard',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test').setName('root').build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('discards leaf that matches filter but has non-matching parent', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'discard',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('test').setName('root').build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps parent that matches filter and discards its non-matching children', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
                {
                  id: 'child',
                  name: 'discard',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });

    it('keeps parent that matches filter and its matching children', () => {
      hierarchyRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      const expectedRoot = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('test')
          .setName('root')
          .setChildren([
            {
              id: 'parent',
              name: 'keep',
              children: [
                {
                  id: 'child',
                  name: 'keep',
                },
              ],
            },
          ])
          .build(),
      );

      operation.apply(hierarchyRoot);
      expect(hierarchyRoot).toEqual(expectedRoot);
    });
  });
});
