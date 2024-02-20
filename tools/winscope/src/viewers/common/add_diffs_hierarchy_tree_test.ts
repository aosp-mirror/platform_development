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
import {AddDiffsHierarchyTree} from './add_diffs_hierarchy_tree';
import {executeAddDiffsTests} from './add_diffs_test_utils';
import {DiffType} from './diff_type';

describe('AddDiffsHierarchyTree', () => {
  let newRoot: UiHierarchyTreeNode;
  let oldRoot: UiHierarchyTreeNode;
  let expectedRoot: UiHierarchyTreeNode;

  const isModified = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
  ) => {
    return (
      (newTree as UiHierarchyTreeNode)
        .getEagerPropertyByName('exampleProperty')
        ?.getValue() !==
      (oldTree as UiHierarchyTreeNode)
        .getEagerPropertyByName('exampleProperty')
        ?.getValue()
    );
  };
  const addDiffs = new AddDiffsHierarchyTree(isModified);

  describe('AddDiffs tests', () => {
    executeAddDiffsTests(
      TreeNodeUtils.treeNodeEqualityTester,
      makeRoot,
      makeChildAndAddToRoot,
      addDiffs,
    );
  });

  describe('Hierarchy tree tests', () => {
    beforeEach(() => {
      jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
      newRoot = makeRoot();
      oldRoot = makeRoot();
      expectedRoot = makeRoot();
    });

    it('does not add MODIFIED to hierarchy root', async () => {
      oldRoot = makeRoot('oldValue');
      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });

    it('adds ADDED_MOVE and DELETED_MOVE', async () => {
      const newParent = makeParentAndAddToRoot(newRoot);
      makeChildAndAddToRoot(newParent);
      makeParentAndAddToRoot(oldRoot);
      makeChildAndAddToRoot(oldRoot);

      const expectedParent = makeParentAndAddToRoot(expectedRoot);

      const expectedNewChild = makeChildAndAddToRoot(expectedParent);
      expectedNewChild.setDiff(DiffType.ADDED_MOVE);

      const expectedOldChild = makeChildAndAddToRoot(expectedRoot);
      expectedOldChild.setDiff(DiffType.DELETED_MOVE);

      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });

    it('adds ADDED, ADDED_MOVE and DELETED_MOVE', async () => {
      const newParent = makeParentAndAddToRoot(newRoot);
      makeChildAndAddToRoot(newParent);
      makeChildAndAddToRoot(oldRoot);

      const expectedOldChild = makeChildAndAddToRoot(expectedRoot);
      expectedOldChild.setDiff(DiffType.DELETED_MOVE);

      const expectedParent = makeParentAndAddToRoot(expectedRoot);
      expectedParent.setDiff(DiffType.ADDED);

      const expectedNewChild = makeChildAndAddToRoot(expectedParent);
      expectedNewChild.setDiff(DiffType.ADDED_MOVE);

      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });
  });

  function makeRoot(value = 'value'): UiHierarchyTreeNode {
    return TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
      exampleProperty: value,
    });
  }

  function makeChildAndAddToRoot(
    rootNode: UiHierarchyTreeNode,
    value = 'value',
  ): UiHierarchyTreeNode {
    const child = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test node',
      name: 'child',
      exampleProperty: value,
    });
    rootNode.addOrReplaceChild(child);
    child.setZParent(rootNode);
    return child;
  }

  function makeParentAndAddToRoot(
    rootNode: UiHierarchyTreeNode,
  ): UiHierarchyTreeNode {
    const parent = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test node',
      name: 'parent',
      exampleProperty: 'value',
    });
    rootNode.addOrReplaceChild(parent);
    parent.setZParent(rootNode);
    return parent;
  }
});
