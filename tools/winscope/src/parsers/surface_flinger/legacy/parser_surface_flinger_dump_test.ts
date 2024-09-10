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

import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserSurfaceFlingerDump', () => {
  beforeAll(() => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
  });

  describe('trace with real timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('provides timestamps (always zero)', () => {
      const expected = [TimestampConverterUtils.makeElapsedTimestamp(0n)];
      expect(parser.getTimestamps()).toEqual(expected);
    });

    it('does not apply timezone info', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb',
        UnitTestUtils.getTimestampConverter(true),
      )) as Parser<HierarchyTreeNode>;

      const expected = [TimestampConverterUtils.makeElapsedTimestamp(0n)];
      expect(parserWithTimezoneInfo.getTimestamps()).toEqual(expected);
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(0);
      expect(entry).toBeTruthy();
    });
  });

  describe('trace with only elapsed timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/dump_SurfaceFlinger.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('provides timestamp (always zero)', () => {
      const expected = [TimestampConverterUtils.makeElapsedTimestamp(0n)];
      expect(parser.getTimestamps()).toEqual(expected);
    });
  });
});
