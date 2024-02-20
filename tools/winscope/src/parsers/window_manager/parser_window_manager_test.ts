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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserWindowManager', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;
    let trace: Trace<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
      )) as Parser<HierarchyTreeNode>;
      trace = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.WINDOW_MANAGER)
        .setParser(parser)
        .build();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
    });

    it('provides elapsed timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14474594000n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15398076788n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15409222011n),
      ];
      expect(
        assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).slice(0, 3),
      ).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107089075566202n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107089999048990n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107090010194213n),
      ];
      expect(
        assertDefined(parser.getTimestamps(TimestampType.REAL)).slice(0, 3),
      ).toEqual(expected);
    });

    it('applies timezone info to real timestamps only', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
        true,
      )) as Parser<HierarchyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.WINDOW_MANAGER,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        )[0],
      ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14474594000n));
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
        )[0],
      ).toEqual(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126889075566202n),
      );
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(1, TimestampType.REAL);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.id).toEqual('WindowManagerState root');
    });

    it('supports WM_WINDOWS_TOKEN_AND_TITLE custom query', async () => {
      const tokenAndTitles = await trace
        .sliceEntries(0, 1)
        .customQuery(CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE);
      expect(tokenAndTitles.length).toEqual(69);
      expect(tokenAndTitles).toContain({token: 'c06766f', title: 'Leaf:36:36'});
    });
  });

  describe('trace elapsed (only) timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/WindowManager.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
    });

    it('provides timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850254319343n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850763506110n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850782750048n),
      ];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it('does not apply timezone info', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/WindowManager.pb',
        true,
      )) as Parser<HierarchyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.WINDOW_MANAGER,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        )[0],
      ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850254319343n));
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(1, TimestampType.ELAPSED);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.id).toEqual('WindowManagerState root');
    });
  });
});
