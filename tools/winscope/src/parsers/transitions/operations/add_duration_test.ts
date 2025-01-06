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

import {TimestampConverterUtils} from 'common/time/test_utils';
import {TimeDuration} from 'common/time/time_duration';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TIMESTAMP_NODE_FORMATTER} from 'trace/tree_node/formatters';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {AddDuration} from './add_duration';

describe('AddDuration', () => {
  let operation: AddDuration;
  const TIMESTAMP_10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const TIMESTAMP_30 = TimestampConverterUtils.makeRealTimestamp(30n);

  beforeEach(() => {
    operation = new AddDuration();
  });

  it('adds duration based on send and finish time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [
            {name: 'sendTimeNs', value: TIMESTAMP_10},
            {name: 'finishTimeNs', value: TIMESTAMP_30},
          ],
        },
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [
            {name: 'sendTimeNs', value: TIMESTAMP_10},
            {name: 'finishTimeNs', value: TIMESTAMP_30},
          ],
        },
        {
          name: 'duration',
          value: new TimeDuration(20n),
          source: PropertySource.CALCULATED,
          formatter: TIMESTAMP_NODE_FORMATTER,
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('does not add duration due to missing send time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'finishTimeNs', value: TIMESTAMP_30}],
        },
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'finishTimeNs', value: TIMESTAMP_30}],
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('does not add duration due to missing finish time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', children: [{name: 'sendTimeNs', value: TIMESTAMP_10}]},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', children: [{name: 'sendTimeNs', value: TIMESTAMP_10}]},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
