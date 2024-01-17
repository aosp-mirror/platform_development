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
import {AddDiffsPropertiesTree} from './add_diffs_properties_tree';
import {executeAddDiffsTests} from './add_diffs_test_utils';
import {UiPropertyTreeNode} from './ui_property_tree_node';

describe('AddDiffsPropertiesTree', () => {
  let newRoot: UiPropertyTreeNode;
  let oldRoot: UiPropertyTreeNode;
  let expectedRoot: UiPropertyTreeNode;

  const isModified = async (newTree: TreeNode | undefined, oldTree: TreeNode | undefined) => {
    return (
      (newTree as UiPropertyTreeNode)?.getValue() !== (oldTree as UiPropertyTreeNode)?.getValue()
    );
  };
  const addDiffs = new AddDiffsPropertiesTree(isModified);

  describe('AddDiffs tests', () => {
    executeAddDiffsTests(nodeEqualityTester, makeRoot, makeChildAndAddToRoot, addDiffs);
  });

  describe('Property tree tests', () => {
    beforeEach(() => {
      jasmine.addCustomEqualityTester(nodeEqualityTester);
      newRoot = makeRoot();
      oldRoot = makeRoot();
      expectedRoot = makeRoot();
    });

    it('does not add MODIFIED to property tree root', async () => {
      oldRoot = makeRoot('oldValue');
      await addDiffs.executeInPlace(newRoot, oldRoot);
      expect(newRoot).toEqual(expectedRoot);
    });

    it('does not add any diffs to property tree that has no old tree', async () => {
      await addDiffs.executeInPlace(newRoot, undefined);
      expect(newRoot).toEqual(expectedRoot);
    });
  });

  function makeRoot(value = 'value'): UiPropertyTreeNode {
    const root = TreeNodeUtils.makeUiPropertyNode('test', 'root', value);
    root.setIsRoot(true);
    return root;
  }

  function makeChildAndAddToRoot(
    rootNode: UiPropertyTreeNode,
    value = 'value'
  ): UiPropertyTreeNode {
    const child = TreeNodeUtils.makeUiPropertyNode('test node', 'child', value);
    rootNode.addChild(child);
    return child;
  }

  function nodeEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof UiPropertyTreeNode && second instanceof UiPropertyTreeNode) {
      return testPropertyTreeNodes(first, second);
    }
    return undefined;
  }

  function testPropertyTreeNodes(
    node: UiPropertyTreeNode,
    expectedNode: UiPropertyTreeNode
  ): boolean {
    if (node.id !== expectedNode.id) return false;
    if (node.name !== expectedNode.name) return false;
    if (node.getDiff() !== expectedNode.getDiff()) return false;

    for (const [index, child] of node.getAllChildren().entries()) {
      if (!testPropertyTreeNodes(child, expectedNode.getAllChildren()[index])) return false;
    }
    return true;
  }
});
