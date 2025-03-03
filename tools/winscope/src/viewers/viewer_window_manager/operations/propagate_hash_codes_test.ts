/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {PropagateHashCodes} from './propagate_hash_codes';

describe('PropagateHashCodes', () => {
  let operation: PropagateHashCodes;

  beforeEach(() => {
    operation = new PropagateHashCodes();
  });

  it('allows hashCode propagation', () => {
    const root = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test 12345')
        .setName('root')
        .setChildren([{name: 'hashCode', value: 67890}])
        .build(),
    );
    operation.apply(root);
    const hashCode = assertDefined(root.getChildByName('hashCode'));
    expect(hashCode.canPropagate()).toEqual(true);
  });

  it('does not allow hashCode propagation for layer fields', () => {
    const root = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test 12345')
        .setName('root')
        .setChildren([
          {
            name: 'surfaceControl',
            children: [{name: 'hashCode', value: 67890}],
          },
          {name: 'leash', children: [{name: 'hashCode', value: 67890}]},
          {name: 'capturedLeash', children: [{name: 'hashCode', value: 67890}]},
          {name: 'startLeash', children: [{name: 'hashCode', value: 67890}]},
        ])
        .build(),
    );

    operation.apply(root);
    root.getAllChildren().forEach((child) => {
      const hashCode = assertDefined(child.getChildByName('hashCode'));
      expect(hashCode.canPropagate()).toEqual(false);
    });
  });

  it('does not allow hashCode propagation if value is for current node', () => {
    const root = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test ' + (12345).toString(16) + ' title')
        .setName('root')
        .setChildren([{name: 'hashCode', value: 12345}])
        .build(),
    );
    operation.apply(root);
    const hashCode = assertDefined(root.getChildByName('hashCode'));
    expect(hashCode.canPropagate()).toEqual(false);
  });

  it('does not allow propagation for non hashCode fields', () => {
    const root = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('test 12345')
        .setName('root')
        .setChildren([{name: 'hash', value: 67890}])
        .build(),
    );
    operation.apply(root);
    const hash = assertDefined(root.getChildByName('hash'));
    expect(hash.canPropagate()).toEqual(false);
  });
});
