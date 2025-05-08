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
import {
  getTimestampConverter,
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {LegacyParserProvider} from 'test/unit/fixture_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserSurfaceFlingerDump', () => {
  let userNotifierChecker: UserNotifierChecker;

  beforeAll(() => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    userNotifierChecker = new UserNotifierChecker();
  });

  describe('trace with real timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = await new LegacyParserProvider()
        .addFilename('traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb')
        .getParser<HierarchyTreeNode>();
    });

    afterEach(() => {
      userNotifierChecker.expectNone();
      userNotifierChecker.reset();
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
      const parserWithTimezoneInfo = await new LegacyParserProvider()
        .addFilename('traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb')
        .setTimestampConverter(getTimestampConverter(true))
        .getParser<HierarchyTreeNode>();

      const expected = [TimestampConverterUtils.makeElapsedTimestamp(0n)];
      expect(parserWithTimezoneInfo.getTimestamps()).toEqual(expected);
    });

    it('does not provide entry', () => {
      expect(parser.getEntry).toThrow();
    });

    it('converts to valid perfetto packets', async () => {
      const packets = parser.convertToPerfettoPackets!(10);
      expect(packets.length).toEqual(1);
      expect(packets[0].timestamp).toEqual(Long.fromInt(0));
      expect(packets[0].timestampClockId).toEqual(
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC,
      );
      expect(packets[0].trustedPacketSequenceId).toEqual(10);
      expect(
        packets[0].surfaceflingerLayersSnapshot?.layers?.layers?.length,
      ).toEqual(94);
    });

    it('converts to valid perfetto trace', async () => {
      await checkValidPerfettoTraceConversion(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb',
        95,
      );
    });
  });

  describe('trace with only elapsed timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = await new LegacyParserProvider()
        .addFilename('traces/elapsed_timestamp/dump_SurfaceFlinger.pb')
        .getParser<HierarchyTreeNode>();
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

    it('converts to valid perfetto packets', async () => {
      const packets = parser.convertToPerfettoPackets!(10);
      expect(packets.length).toEqual(1);
      expect(packets[0].timestamp).toEqual(Long.fromInt(0));
      expect(packets[0].timestampClockId).toEqual(
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC,
      );
      expect(packets[0].trustedPacketSequenceId).toEqual(10);
      expect(
        packets[0].surfaceflingerLayersSnapshot?.layers?.layers?.length,
      ).toEqual(91);
    });

    it('does not provide entry', () => {
      expect(parser.getEntry).toThrow();
    });

    it('converts to valid perfetto trace', async () => {
      await checkValidPerfettoTraceConversion(
        'traces/elapsed_timestamp/dump_SurfaceFlinger.pb',
        92,
      );
    });
  });

  async function checkValidPerfettoTraceConversion(
    filename: string,
    nodeCount: number,
  ) {
    const perfettoParser = await new LegacyParserProvider()
      .addFilename(filename)
      .setConvertToPerfetto(true)
      .getParser<HierarchyTreeNode>();
    const expected = [TimestampConverterUtils.makeZeroTimestamp()];
    expect(assertDefined(perfettoParser.getTimestamps())).toEqual(expected);
    const entry = await perfettoParser.getEntry(0);
    let count = 0;
    entry.forEachNodeDfs(() => count++);
    expect(count).toEqual(nodeCount);
  }
});
