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
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UpdateDisplayNames} from './update_display_names';

describe('UpdateDisplayNames', () => {
  let operation: UpdateDisplayNames;

  beforeEach(() => {
    operation = new UpdateDisplayNames();
  });

  it('changes display name of task node', () => {
    const hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('root')
        .setChildren([
          {id: 'Task 1234567', name: 'Task', properties: {id: '8'}},
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getChildByName('Task')?.getDisplayName()).toEqual('8');
  });

  it('does not change name of non-task node', () => {
    const hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('root')
        .setChildren([
          {
            id: 'TaskFragment 1234567',
            name: 'TaskFragment',
            properties: {id: '8'},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      hierarchyRoot.getChildByName('TaskFragment')?.getDisplayName(),
    ).toEqual('TaskFragment');
  });
});
