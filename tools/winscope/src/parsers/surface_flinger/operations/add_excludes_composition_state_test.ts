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
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {AddExcludesCompositionState} from './add_excludes_composition_state';

describe('AddExcludesCompositionState', () => {
  let propertyRoot: PropertyTreeNode;
  let expectedRoot: PropertyTreeNode;
  let operation: AddExcludesCompositionState;

  beforeEach(() => {
    propertyRoot = new PropertyTreeNode(
      'test node',
      'node',
      PropertySource.PROTO,
      undefined,
    );
    expectedRoot = new PropertyTreeNode(
      'test node',
      'node',
      PropertySource.PROTO,
      undefined,
    );
  });

  it('creates excludesCompositionState node with true value', () => {
    operation = new AddExcludesCompositionState(true);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'excludesCompositionState',
      true,
    );
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates excludesCompositionState node with false value', () => {
    operation = new AddExcludesCompositionState(false);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'excludesCompositionState',
      false,
    );
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
