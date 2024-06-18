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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {UnitTestUtils} from 'test/unit/utils';
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

    it('has expected timestamps', () => {
      const timestamps = assertDefined(
        parser.getTimestamps(TimestampType.REAL),
      );

      expect(timestamps.length).toEqual(184);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681207047981157174n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681207047991161039n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681207047991310494n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it("doesn't provide elapsed timestamps", () => {
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(undefined);
    });

    it('contains parsed jank CUJ events', async () => {
      const entry = await parser.getEntry(18, TimestampType.REAL);

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

    it('applies timezone info to real timestamps', async () => {
      const parserWithTimezoneInfo = assertDefined(
        await UnitTestUtils.getParser('traces/eventlog.winscope', true),
      ) as Parser<PropertyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.EVENT_LOG,
      );

      const timestamps = assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
      );
      expect(timestamps.length).toEqual(184);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681226847981157174n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681226847991161039n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681226847991310494n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
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
      const timestamps = assertDefined(
        parser.getTimestamps(TimestampType.REAL),
      );

      expect(timestamps.length).toEqual(3);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681207047981157174n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681207047991161039n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681207047991310494n),
      ];
      expect(timestamps).toEqual(expected);
    });

    it('contains parsed events', async () => {
      const entry = await parser.getEntry(0, TimestampType.REAL);

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

    it('applies timezone info to real timestamps', async () => {
      const parserWithTimezoneInfo = assertDefined(
        await UnitTestUtils.getParser(
          'traces/eventlog_timestamps_not_monotonically_increasing.winscope',
          true,
        ),
      ) as Parser<PropertyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.EVENT_LOG,
      );

      const timestamps = assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
      );
      expect(timestamps.length).toEqual(3);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681226847981157174n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681226847991161039n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1681226847991310494n),
      ];
      expect(timestamps).toEqual(expected);
    });
  });
});
