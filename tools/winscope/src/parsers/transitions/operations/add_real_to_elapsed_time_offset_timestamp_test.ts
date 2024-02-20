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

import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {AddRealToElapsedTimeOffsetTimestamp} from './add_real_to_elapsed_time_offset_timestamp';

describe('AddRealToElapsedTimeOffsetTimestamp', () => {
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
        {
          name: 'realToElapsedTimeOffsetTimestamp',
          value: undefined,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    const operation = new AddRealToElapsedTimeOffsetTimestamp(undefined);
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds offset timestamp', () => {
    const realToElapsedTimeOffsetTimestamp =
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(12345n);
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
        {
          name: 'realToElapsedTimeOffsetTimestamp',
          value: realToElapsedTimeOffsetTimestamp,
          source: PropertySource.CALCULATED,
        },
      ])
      .build();

    const operation = new AddRealToElapsedTimeOffsetTimestamp(
      realToElapsedTimeOffsetTimestamp,
    );
    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
