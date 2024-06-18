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
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {FlattenChildren} from './flatten_children';

describe('FlattenChildren', () => {
  let hierarchyRoot: UiHierarchyTreeNode;
  let operation: FlattenChildren;

  beforeEach(() => {
    operation = new FlattenChildren();
  });

  it('flattens children of nested tree', () => {
    hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
    });

    let prevChild = TreeNodeUtils.makeUiHierarchyNode({
      id: 0,
      name: 'child',
    });
    hierarchyRoot.addOrReplaceChild(prevChild);

    for (let i = 1; i < 10; i++) {
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: i,
        name: 'child',
      });
      prevChild.addOrReplaceChild(child);
      prevChild = child;
    }

    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getAllChildren().length).toEqual(10);
  });

  it('flattens children in expected order for multiple root children', () => {
    hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
    });

    const expectedChildren = [];

    const firstChild = TreeNodeUtils.makeUiHierarchyNode({
      id: 0,
      name: 'child',
    });
    hierarchyRoot.addOrReplaceChild(firstChild);
    expectedChildren.push(firstChild);

    let prevChild = firstChild;
    for (let i = 1; i < 5; i++) {
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: i,
        name: 'child',
      });
      prevChild.addOrReplaceChild(child);
      expectedChildren.push(child);
      prevChild = child;
    }

    const secondChild = TreeNodeUtils.makeUiHierarchyNode({
      id: 5,
      name: 'child',
    });
    hierarchyRoot.addOrReplaceChild(secondChild);
    expectedChildren.push(secondChild);

    prevChild = secondChild;
    for (let i = 6; i < 10; i++) {
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: i,
        name: 'child',
      });
      prevChild.addOrReplaceChild(child);
      expectedChildren.push(child);
      prevChild = child;
    }

    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getAllChildren()).toEqual(expectedChildren);
  });

  it('leaves flat tree unchanged', () => {
    hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
    });

    for (let i = 0; i < 10; i++) {
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: i,
        name: 'child',
      });
      hierarchyRoot.addOrReplaceChild(child);
    }
    expect(hierarchyRoot.getAllChildren().length).toEqual(10);
    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getAllChildren().length).toEqual(10);
  });
});
