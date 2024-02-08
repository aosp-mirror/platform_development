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
import {AddRealToElapsedTimeOffsetNs} from './add_real_to_elapsed_time_offset_ns';

describe('AddRealToElapsedTimeOffsetNs', () => {
  it('adds undefined offset', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'realToElapsedTimeOffsetNs', value: undefined, source: PropertySource.CALCULATED},
      ])
      .build();

    const operation = new AddRealToElapsedTimeOffsetNs(undefined);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds bigint offset', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'realToElapsedTimeOffsetNs', value: 12345n, source: PropertySource.CALCULATED},
      ])
      .build();

    const operation = new AddRealToElapsedTimeOffsetNs(12345n);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
