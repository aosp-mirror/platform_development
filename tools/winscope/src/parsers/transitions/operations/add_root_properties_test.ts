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
import {AddRootProperties} from './add_root_properties';

describe('AddRootProperties', () => {
  let operation: AddRootProperties;

  beforeEach(() => {
    operation = new AddRootProperties();
  });

  it('adds properties from wmData to root', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'id', value: 10}],
        },
        {name: 'shellData', value: null},
        {name: 'duration', value: 5n, source: PropertySource.CALCULATED},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'id', value: 10}],
        },
        {name: 'shellData', value: null},
        {name: 'duration', value: 5n, source: PropertySource.CALCULATED},
        {name: 'id', value: 10, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds properties from shellData to root', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', value: null},
        {
          name: 'shellData',
          children: [{name: 'id', value: 10}],
        },
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', value: null},
        {
          name: 'shellData',
          children: [{name: 'id', value: 10}],
        },
        {name: 'id', value: 10, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('takes properties from wmData over shellData', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'id', value: 10}],
        },
        {
          name: 'shellData',
          children: [{name: 'id', value: 20}],
        },
        {name: 'aborted', value: true, source: PropertySource.CALCULATED},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'id', value: 10}],
        },
        {
          name: 'shellData',
          children: [{name: 'id', value: 20}],
        },
        {name: 'aborted', value: true, source: PropertySource.CALCULATED},
        {name: 'id', value: 10, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
