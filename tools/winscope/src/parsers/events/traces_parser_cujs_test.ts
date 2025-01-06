/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {
  CUJ_TYPE_FORMATTER,
  DEFAULT_PROPERTY_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('TracesParserCujs', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = (await UnitTestUtils.getTracesParser([
      'traces/eventlog.winscope',
    ])) as Parser<PropertyTreeNode>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.CUJS);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual(['eventlog.winscope']);
  });

  it('provides timestamps', () => {
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1681207048025446000n),
      TimestampConverterUtils.makeRealTimestamp(1681207048025551000n),
      TimestampConverterUtils.makeRealTimestamp(1681207048025580000n),
    ];

    const timestamps = assertDefined(parser.getTimestamps());
    expect(timestamps.length).toEqual(16);
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('contains parsed CUJ events', async () => {
    const entry = await parser.getEntry(2);

    const expected = new PropertyTreeBuilder()
      .setRootId('CujTrace')
      .setName('cuj')
      .setIsRoot(true)
      .setChildren([
        {
          name: 'cujType',
          value: 66,
          formatter: CUJ_TYPE_FORMATTER,
        },
        {
          name: 'startTimestamp',
          value:
            TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(
              1681207048025580000n,
            ),
          formatter: DEFAULT_PROPERTY_FORMATTER,
        },
        {
          name: 'endTimestamp',
          value:
            TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(
              1681207048643085000n,
            ),
          formatter: DEFAULT_PROPERTY_FORMATTER,
        },
        {name: 'canceled', value: true, formatter: DEFAULT_PROPERTY_FORMATTER},
      ])
      .build();

    expect(entry).toEqual(expected);
  });
});
