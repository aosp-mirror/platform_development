/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserInputMethodClients', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/InputMethodClients.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    });

    it('provides elapsed timestamps', () => {
      expect(parser.getTimestamps(TimestampType.ELAPSED)!.length).toEqual(13);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15613638434n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15647516364n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15677650967n),
      ];
      expect(
        assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).slice(0, 3),
      ).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107090215405395n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107090249283325n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107090279417928n),
      ];
      expect(
        assertDefined(parser.getTimestamps(TimestampType.REAL)).slice(0, 3),
      ).toEqual(expected);
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(1, TimestampType.REAL);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.id).toEqual('InputMethodClients entry');
    });

    it('applies timezone info to real timestamps only', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/InputMethodClients.pb',
        true,
      )) as Parser<HierarchyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.INPUT_METHOD_CLIENTS,
      );

      const expectedElapsed = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15613638434n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15647516364n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15677650967n),
      ];
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        ).slice(0, 3),
      ).toEqual(expectedElapsed);

      const expectedReal = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126890215405395n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126890249283325n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126890279417928n),
      ];
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
        ).slice(0, 3),
      ).toEqual(expectedReal);
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/InputMethodClients.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    });

    it('provides elapsed timestamps', () => {
      expect(
        assertDefined(parser.getTimestamps(TimestampType.ELAPSED))[0],
      ).toEqual(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1149083651642n),
      );
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toBeUndefined();
    });

    it('retrieves trace entry from elapsed timestamp', async () => {
      const entry = await parser.getEntry(0, TimestampType.ELAPSED);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.id).toEqual('InputMethodClients entry');
    });

    it('does not apply timezone info to elapsed timestamps', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/InputMethodClients.pb',
        true,
      )) as Parser<HierarchyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.INPUT_METHOD_CLIENTS,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        )[0],
      ).toEqual(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1149083651642n),
      );
    });
  });
});
