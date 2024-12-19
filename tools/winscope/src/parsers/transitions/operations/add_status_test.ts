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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {AddStatus} from './add_status';

describe('AddStatus', () => {
  let operation: AddStatus;
  const time0 = TimestampConverterUtils.makeRealTimestamp(0n);
  const time10 = TimestampConverterUtils.makeRealTimestamp(10n);

  beforeEach(() => {
    operation = new AddStatus();
  });

  it('adds aborted true due to wm abort time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', children: [{name: 'abortTimeNs', value: time10}]},
        {name: 'shellData', value: null},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', children: [{name: 'abortTimeNs', value: time10}]},
        {name: 'shellData', value: null},
        {name: 'aborted', value: true, source: PropertySource.CALCULATED},
        {name: 'merged', value: false, source: PropertySource.CALCULATED},
        {name: 'played', value: false, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds aborted true due to shell abort time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'shellData', children: [{name: 'abortTimeNs', value: time10}]},
        {name: 'wmData', value: null},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'shellData', children: [{name: 'abortTimeNs', value: time10}]},
        {name: 'wmData', value: null},
        {name: 'aborted', value: true, source: PropertySource.CALCULATED},
        {name: 'merged', value: false, source: PropertySource.CALCULATED},
        {name: 'played', value: false, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds aborted false due to 0 abort time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', children: [{name: 'abortTimeNs', value: time0}]},
        {name: 'shellData', children: [{name: 'abortTimeNs', value: time0}]},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', children: [{name: 'abortTimeNs', value: time0}]},
        {
          name: 'shellData',
          children: [{name: 'abortTimeNs', value: time0}],
        },
        {name: 'aborted', value: false, source: PropertySource.CALCULATED},
        {name: 'merged', value: false, source: PropertySource.CALCULATED},
        {name: 'played', value: false, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds merged true', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', value: null},
        {name: 'shellData', children: [{name: 'mergeTimeNs', value: time10}]},
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
          children: [{name: 'mergeTimeNs', value: time10}],
        },
        {name: 'aborted', value: false, source: PropertySource.CALCULATED},
        {name: 'merged', value: true, source: PropertySource.CALCULATED},
        {name: 'played', value: false, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds merged false due to 0 merge time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', value: null},
        {name: 'shellData', children: [{name: 'mergeTimeNs', value: time0}]},
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
          children: [{name: 'mergeTimeNs', value: time0}],
        },
        {name: 'aborted', value: false, source: PropertySource.CALCULATED},
        {name: 'merged', value: false, source: PropertySource.CALCULATED},
        {name: 'played', value: false, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds played true', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'finishTimeNs', value: time10}],
        },
        {name: 'shellData', value: null},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'finishTimeNs', value: time10}],
        },
        {name: 'shellData', value: null},
        {name: 'aborted', value: false, source: PropertySource.CALCULATED},
        {name: 'merged', value: false, source: PropertySource.CALCULATED},
        {name: 'played', value: true, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('adds played false due to 0 finish time', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {name: 'wmData', value: null},
        {name: 'shellData', children: [{name: 'finishTimeNs', value: time0}]},
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
          children: [{name: 'finishTimeNs', value: time0}],
        },
        {name: 'aborted', value: false, source: PropertySource.CALCULATED},
        {name: 'merged', value: false, source: PropertySource.CALCULATED},
        {name: 'played', value: false, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
