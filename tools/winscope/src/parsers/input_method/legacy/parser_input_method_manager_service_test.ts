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
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import Long from 'long';
import {LegacyParserProvider} from 'test/unit/fixture_utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserInputMethodManagerService', () => {
  describe('trace with real timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = await new LegacyParserProvider()
        .addFilename(
          'traces/elapsed_and_real_timestamp/InputMethodManagerService.pb',
        )
        .getParser<HierarchyTreeNode>();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(
        TraceType.INPUT_METHOD_MANAGER_SERVICE,
      );
    });
    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('provides timestamps', () => {
      expect(parser.getTimestamps()).toEqual([
        TimestampConverterUtils.makeRealTimestamp(1659107090565549479n),
      ]);
    });

    it('does not provide entry', () => {
      expect(parser.getEntry).toThrow();
    });

    it('converts to valid perfetto packets', async () => {
      const packets = parser.convertToPerfettoPackets!(10);
      expect(packets.length).toEqual(1);
      expect(packets[0].trustedPacketSequenceId).toEqual(10);
      const data =
        packets[0].winscopeExtensions?.[
          '.perfetto.protos.WinscopeExtensionsImpl.inputmethodManagerService'
        ];
      expect(data?.inputMethodManagerService).toBeDefined();
      expect(data?.where).toEqual(
        'InputMethodManagerService#startInputOrWindowGainedFocus',
      );
      const ts = Long.fromString(BigInt(15963782518).toString());
      ts.unsigned = true;
      expect(packets[0].timestamp).toEqual(ts);
    });

    it('converts to valid perfetto trace', async () => {
      const perfettoParser = await new LegacyParserProvider()
        .addFilename(
          'traces/elapsed_and_real_timestamp/InputMethodManagerService.pb',
        )
        .setConvertToPerfetto(true)
        .getParser<HierarchyTreeNode>();

      expect(perfettoParser.getTimestamps()).toEqual([
        TimestampConverterUtils.makeRealTimestamp(1659107090565549479n),
      ]);

      const entry = await perfettoParser.getEntry(0);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.getEagerPropertyByName('where')?.getValue()).toEqual(
        'InputMethodManagerService#startInputOrWindowGainedFocus',
      );
    });
  });

  describe('trace with only elapsed timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = await new LegacyParserProvider()
        .addFilename('traces/elapsed_timestamp/InputMethodManagerService.pb')
        .getParser<HierarchyTreeNode>();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(
        TraceType.INPUT_METHOD_MANAGER_SERVICE,
      );
    });

    it('provides timestamps', () => {
      expect(assertDefined(parser.getTimestamps())[0]).toEqual(
        TimestampConverterUtils.makeElapsedTimestamp(1149226290110n),
      );
    });

    it('does not provide entry', () => {
      expect(parser.getEntry).toThrow();
    });

    it('converts to valid perfetto packets', async () => {
      const packets = parser.convertToPerfettoPackets!(10);
      expect(packets.length).toEqual(3);
      expect(packets[0].trustedPacketSequenceId).toEqual(10);
      const data =
        packets[0].winscopeExtensions?.[
          '.perfetto.protos.WinscopeExtensionsImpl.inputmethodManagerService'
        ];
      expect(data?.inputMethodManagerService).toBeDefined();
      expect(data?.where).toEqual(
        'InputMethodManagerService#startInputOrWindowGainedFocus',
      );
      const ts = Long.fromString(BigInt(1149226290110).toString());
      ts.unsigned = true;
      expect(packets[0].timestamp).toEqual(ts);
    });
  });
});
