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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {EventTag} from './event_tag';

describe('ParserEventLog', () => {
  describe('trace with monotonically increasing timestamps', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = assertDefined(
        await UnitTestUtils.getParser('traces/eventlog.winscope'),
      ) as Parser<PropertyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.EVENT_LOG);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('has expected timestamps', () => {
      const timestamps = assertDefined(parser.getTimestamps());

      expect(timestamps.length).toEqual(184);

      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1681207047981157174n),
        TimestampConverterUtils.makeRealTimestamp(1681207047991161039n),
        TimestampConverterUtils.makeRealTimestamp(1681207047991310494n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('contains parsed jank CUJ events', async () => {
      const entry = await parser.getEntry(18);

      const expected = new PropertyTreeBuilder()
        .setRootId('EventLogTrace')
        .setName('event')
        .setIsRoot(true)
        .setChildren([
          {name: 'eventTimestamp', value: 1681207048025596830n},
          {name: 'pid', value: 2806},
          {name: 'uid', value: 10227},
          {name: 'tid', value: 3604},
          {name: 'tag', value: EventTag.JANK_CUJ_BEGIN_TAG},
          {
            name: 'eventData',
            value: '[66,1681207048025580000,2661012903966,2661012904007,]',
          },
        ])
        .build();

      expect(entry).toEqual(expected);
    });
  });

  describe('trace with timestamps not monotonically increasing', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = assertDefined(
        await UnitTestUtils.getParser(
          'traces/eventlog_timestamps_not_monotonically_increasing.winscope',
        ),
      ) as Parser<PropertyTreeNode>;
    });

    it('sorts entries to make timestamps monotonically increasing', () => {
      const timestamps = assertDefined(parser.getTimestamps());

      expect(timestamps.length).toEqual(3);

      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1681207047981157174n),
        TimestampConverterUtils.makeRealTimestamp(1681207047991161039n),
        TimestampConverterUtils.makeRealTimestamp(1681207047991310494n),
      ];
      expect(timestamps).toEqual(expected);
    });

    it('contains parsed events', async () => {
      const entry = await parser.getEntry(0);

      const expected = new PropertyTreeBuilder()
        .setRootId('EventLogTrace')
        .setName('event')
        .setIsRoot(true)
        .setChildren([
          {name: 'eventTimestamp', value: 1681207047981157174n},
          {name: 'pid', value: 1654},
          {name: 'uid', value: 1000},
          {name: 'tid', value: 1821},
          {name: 'tag', value: 'input_interaction'},
          {name: 'eventData', value: 'Interaction with'},
        ])
        .build();

      expect(entry).toEqual(expected);
    });
  });
});
