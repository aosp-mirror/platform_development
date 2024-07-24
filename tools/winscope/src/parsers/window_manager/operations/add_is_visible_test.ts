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
import {AddIsVisible} from './add_is_visible';

describe('AddIsVisible', () => {
  let operation: AddIsVisible;

  beforeEach(() => {
    operation = new AddIsVisible();
  });

  it('creates isComputedVisible node from true visible node', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'visible', value: true}])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'visible', value: true},
        {
          name: 'isComputedVisible',
          value: true,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates isComputedVisible node from false visible node', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'visible', value: false}])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'visible', value: false},
        {
          name: 'isComputedVisible',
          value: false,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates isComputedVisible node from true isVisible and zero alpha', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'isVisible', value: true},
        {name: 'attributes', children: [{name: 'alpha', value: 0}]},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'isVisible', value: true},
        {name: 'attributes', children: [{name: 'alpha', value: 0}]},
        {
          name: 'isComputedVisible',
          value: false,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates isComputedVisible node from true isVisible and non-zero alpha', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'isVisible', value: true},
        {name: 'attributes', children: [{name: 'alpha', value: 1}]},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'isVisible', value: true},
        {name: 'attributes', children: [{name: 'alpha', value: 1}]},
        {
          name: 'isComputedVisible',
          value: true,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
