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

import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {WindowType, WindowTypePrefix} from 'trace/window_type';
import {AddWindowType} from './add_window_type';

describe('AddWindowType', () => {
  let operation: AddWindowType;

  beforeEach(() => {
    operation = new AddWindowType();
  });

  it('creates WindowType.STARTING from WindowTypePrefix.STARTING name', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName(WindowTypePrefix.STARTING)
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName(WindowTypePrefix.STARTING)
      .setChildren([
        {
          name: 'windowType',
          value: WindowType.STARTING,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates WindowType.STARTING from WindowTypePrefix.DEBUGGER name', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName(WindowTypePrefix.DEBUGGER)
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName(WindowTypePrefix.DEBUGGER)
      .setChildren([
        {
          name: 'windowType',
          value: WindowType.STARTING,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates WindowType.EXITING when name does not start with WindowTypePrefix.DEBUGGER', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName('state')
      .setChildren([{name: 'animatingExit', value: null}])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName('state')
      .setChildren([
        {name: 'animatingExit', value: null},
        {
          name: 'windowType',
          value: WindowType.EXITING,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates WindowType.EXITING when name starts with WindowTypePrefix.DEBUGGER', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName(WindowTypePrefix.DEBUGGER)
      .setChildren([{name: 'animatingExit', value: null}])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test node')
      .setIsRoot(true)
      .setName(WindowTypePrefix.DEBUGGER)
      .setChildren([
        {name: 'animatingExit', value: null},
        {
          name: 'windowType',
          value: WindowType.EXITING,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
