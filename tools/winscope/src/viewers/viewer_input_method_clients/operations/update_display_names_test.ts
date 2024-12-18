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
import {DEFAULT_PROPERTY_FORMATTER} from 'trace/tree_node/formatters';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UpdateDisplayNames} from './update_display_names';

describe('UpdateDisplayNames', () => {
  let operation: UpdateDisplayNames;

  beforeEach(() => {
    operation = new UpdateDisplayNames();
  });

  it('changes display name of client node with view', () => {
    const hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('client')
        .addChildProperty({
          name: 'viewRootImpl',
          children: [
            {
              name: 'view',
              value: 'view 7654321',
              formatter: DEFAULT_PROPERTY_FORMATTER,
            },
          ],
        })
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getDisplayName()).toEqual('view 7654321');
  });

  it('changes display name of client node without view', () => {
    const hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder().setId('test').setName('client').build(),
    );

    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getDisplayName()).toEqual('null');
  });

  it('does not change name of non-client node', () => {
    const hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('service')
        .addChildProperty({
          name: 'viewRootImpl',
          children: [
            {
              name: 'view',
              value: 'view 7654321',
              formatter: DEFAULT_PROPERTY_FORMATTER,
            },
          ],
        })
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(hierarchyRoot.getDisplayName()).toEqual('service');
  });
});
