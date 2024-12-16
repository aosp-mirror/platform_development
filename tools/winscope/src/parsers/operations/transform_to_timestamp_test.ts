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

import {assertDefined} from 'common/assert_utils';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {MockLong} from 'test/unit/mock_long';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TransformToTimestamp} from './transform_to_timestamp';

describe('TransformToTimestamp', () => {
  const timestampProperties = ['timestamp'];
  const longTimestamp = new MockLong(10, 0);
  let propertyRoot: PropertyTreeNode;

  beforeEach(() => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'timestamp', value: longTimestamp},
        {name: 'otherTimestamp', value: longTimestamp},
      ])
      .build();
  });

  it('transforms only specified property to real timestamp', () => {
    const operation = new TransformToTimestamp(
      timestampProperties,
      makeRealTimestampStrategy,
    );
    operation.apply(propertyRoot);
    expect(
      assertDefined(propertyRoot.getChildByName('timestamp')).getValue(),
    ).toEqual(TimestampConverterUtils.makeRealTimestamp(10n));
    expect(
      assertDefined(propertyRoot.getChildByName('otherTimestamp')).getValue(),
    ).toEqual(longTimestamp);
  });

  it('transforms only specified property to elapsed timestamp', () => {
    const operation = new TransformToTimestamp(
      timestampProperties,
      makeElapsedTimestampStrategy,
    );
    operation.apply(propertyRoot);
    expect(
      assertDefined(propertyRoot.getChildByName('timestamp')).getValue(),
    ).toEqual(TimestampConverterUtils.makeElapsedTimestamp(10n));
    expect(
      assertDefined(propertyRoot.getChildByName('otherTimestamp')).getValue(),
    ).toEqual(longTimestamp);
  });

  function makeRealTimestampStrategy(valueNs: bigint) {
    return TimestampConverterUtils.makeRealTimestamp(valueNs);
  }

  function makeElapsedTimestampStrategy(valueNs: bigint) {
    return TimestampConverterUtils.makeElapsedTimestamp(valueNs);
  }
});
