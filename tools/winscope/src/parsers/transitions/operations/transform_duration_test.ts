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

import {TimeDuration} from 'common/time/time_duration';
import Long from 'long';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {TransformDuration} from './transform_duration';

describe('TransformDuration', () => {
  const expectedRootWithDuration = new PropertyTreeBuilder()
    .setIsRoot(true)
    .setRootId('TransitionsTraceEntry')
    .setName('transition')
    .setChildren([
      {
        name: 'durationNs',
        value: new TimeDuration(123456n),
        source: PropertySource.TP,
      },
    ])
    .build();

  it('transforms duration from bigint', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([{name: 'durationNs', value: 123456n}])
      .build();

    new TransformDuration().apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRootWithDuration);
  });
  it('transforms duration from long', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([{name: 'durationNs', value: Long.fromNumber(123456)}])
      .build();

    new TransformDuration().apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRootWithDuration);
  });

  it('robust to missing duration', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([{name: 'duration', value: 123456n}])
      .build();

    new TransformDuration().apply(propertyRoot);
    expect(propertyRoot).toEqual(propertyRoot);
  });
});
