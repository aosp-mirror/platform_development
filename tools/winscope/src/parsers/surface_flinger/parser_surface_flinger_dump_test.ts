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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserSurfaceFlingerDump', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamp', () => {
      const expected = [NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n)];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it('provides real timestamp (always zero)', () => {
      const expected = [NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n)];
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(expected);
    });

    it('does not apply timezone info', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb',
        true,
      )) as Parser<HierarchyTreeNode>;

      const expectedElapsed = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n),
      ];
      expect(
        parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
      ).toEqual(expectedElapsed);

      const expectedReal = [NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n)];
      expect(parserWithTimezoneInfo.getTimestamps(TimestampType.REAL)).toEqual(
        expectedReal,
      );
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(0, TimestampType.ELAPSED);
      expect(entry).toBeTruthy();
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/dump_SurfaceFlinger.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamp (always zero)', () => {
      const expected = [NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n)];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it("doesn't provide real timestamp", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });
  });
});
