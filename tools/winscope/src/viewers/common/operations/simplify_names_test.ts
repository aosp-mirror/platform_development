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
import {SimplifyNames} from './simplify_names';

describe('SimplifyNames', () => {
  let hierarchyRoot: UiHierarchyTreeNode;
  let operation: SimplifyNames;

  beforeEach(() => {
    operation = new SimplifyNames();
  });

  it('shortens long names', () => {
    hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
    });

    for (let i = 0; i < 10; i++) {
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: i,
        name: 'node' + '.child'.repeat(10),
      });
      hierarchyRoot.addOrReplaceChild(child);
    }

    operation.apply(hierarchyRoot);
    hierarchyRoot
      .getAllChildren()
      .forEach((child) =>
        expect(child.getDisplayName()).toEqual('node.child.(...).child'),
      );
  });

  it('does not change already short names', () => {
    hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
    });

    for (let i = 0; i < 10; i++) {
      const child = TreeNodeUtils.makeUiHierarchyNode({
        id: i,
        name: 'node.child',
      });
      hierarchyRoot.addOrReplaceChild(child);
    }

    operation.apply(hierarchyRoot);
    hierarchyRoot
      .getAllChildren()
      .forEach((child) => expect(child.getDisplayName()).toEqual('node.child'));
  });
});
