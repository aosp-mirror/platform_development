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
import {AddVerboseFlags} from './add_verbose_flags';

describe('AddVerboseFlags', () => {
  let propertyRoot: PropertyTreeNode;
  let expectedRoot: PropertyTreeNode;
  let operation: AddVerboseFlags;

  beforeEach(() => {
    operation = new AddVerboseFlags();
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

  it('adds no verbose flags', () => {
    const flags = TreeNodeUtils.makePropertyNode(propertyRoot.id, 'flags', 0x0);
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      '',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds HIDDEN', () => {
    const flags = TreeNodeUtils.makePropertyNode(propertyRoot.id, 'flags', 0x1);
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'HIDDEN (0x1)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds OPAQUE', () => {
    const flags = TreeNodeUtils.makePropertyNode(propertyRoot.id, 'flags', 0x2);
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'OPAQUE (0x2)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds SKIP_SCREENSHOT', () => {
    const flags = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'flags',
      0x40,
    );
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'SKIP_SCREENSHOT (0x40)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds SECURE', () => {
    const flags = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'flags',
      0x80,
    );
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'SECURE (0x80)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds ENABLE_BACKPRESSURE', () => {
    const flags = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'flags',
      0x100,
    );
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'ENABLE_BACKPRESSURE (0x100)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds DISPLAY_DECORATION', () => {
    const flags = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'flags',
      0x200,
    );
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'DISPLAY_DECORATION (0x200)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds IGNORE_DESTINATION_FRAME', () => {
    const flags = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'flags',
      0x400,
    );
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'IGNORE_DESTINATION_FRAME (0x400)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds multiple flags depending on bits set', () => {
    const flags = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'flags',
      0x101,
    );
    propertyRoot.addOrReplaceChild(flags);
    const expectedResult = TreeNodeUtils.makeCalculatedPropertyNode(
      expectedRoot.id,
      'verboseFlags',
      'HIDDEN|ENABLE_BACKPRESSURE (0x101)',
    );
    expectedRoot.addOrReplaceChild(flags);
    expectedRoot.addOrReplaceChild(expectedResult);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
