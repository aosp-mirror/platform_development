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

describe('ParserInputMethodClients', () => {
  describe('trace with real timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = await new LegacyParserProvider()
        .addFilename('traces/elapsed_and_real_timestamp/InputMethodClients.pb')
        .getParser<HierarchyTreeNode>();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('provides timestamps', () => {
      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1659107090215405395n),
        TimestampConverterUtils.makeRealTimestamp(1659107090249283325n),
        TimestampConverterUtils.makeRealTimestamp(1659107090279417928n),
      ];
      expect(parser.getTimestamps()?.slice(0, 3)).toEqual(expected);
    });

    it('does not provide entry', () => {
      expect(parser.getEntry).toThrow();
    });

    it('converts to valid perfetto packets', async () => {
      const packets = parser.convertToPerfettoPackets!(10);
      expect(packets.length).toEqual(13);
      expect(packets[0].trustedPacketSequenceId).toEqual(10);
      const data =
        packets[0].winscopeExtensions?.[
          '.perfetto.protos.WinscopeExtensionsImpl.inputmethodClients'
        ];
      expect(data?.client).toBeDefined();
      expect(data?.where).toEqual('InsetsSourceConsumer#setControl');
      const ts = Long.fromString(BigInt(15613638434).toString());
      ts.unsigned = true;
      expect(packets[0].timestamp).toEqual(ts);
    });

    it('converts to valid perfetto trace', async () => {
      const perfettoParser = await new LegacyParserProvider()
        .addFilename('traces/elapsed_and_real_timestamp/InputMethodClients.pb')
        .setConvertToPerfetto(true)
        .getParser<HierarchyTreeNode>();

      expect(perfettoParser.getTimestamps()?.slice(0, 3)).toEqual([
        TimestampConverterUtils.makeRealTimestamp(1659107090215405395n),
        TimestampConverterUtils.makeRealTimestamp(1659107090249283325n),
        TimestampConverterUtils.makeRealTimestamp(1659107090279417928n),
      ]);

      const entry = await perfettoParser.getEntry(10);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.getEagerPropertyByName('where')?.getValue()).toEqual(
        'InsetsSourceConsumer#setControl',
      );
      const client = assertDefined(entry.getChildByName('client'));
      const properties = await client.getAllProperties();
      const intdefProperty = properties
        ?.getChildByName('viewRootImpl')
        ?.getChildByName('windowAttributes')
        ?.getChildByName('type');
      expect(intdefProperty?.formattedValue()).toEqual('TYPE_BASE_APPLICATION');
    });
  });

  describe('trace with only elapsed timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = await new LegacyParserProvider()
        .addFilename('traces/elapsed_timestamp/InputMethodClients.pb')
        .getParser<HierarchyTreeNode>();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    });

    it('provides timestamps', () => {
      expect(assertDefined(parser.getTimestamps())[0]).toEqual(
        TimestampConverterUtils.makeElapsedTimestamp(1149083651642n),
      );
    });

    it('does not provide entry', () => {
      expect(parser.getEntry).toThrow();
    });

    it('converts to valid perfetto packets', async () => {
      const packets = parser.convertToPerfettoPackets!(10);
      expect(packets.length).toEqual(33);
      expect(packets[0].trustedPacketSequenceId).toEqual(10);
      const data =
        packets[0].winscopeExtensions?.[
          '.perfetto.protos.WinscopeExtensionsImpl.inputmethodClients'
        ];
      expect(data?.client).toBeDefined();
      expect(data?.where).toEqual('InsetsSourceConsumer#setControl');
      const ts = Long.fromString(BigInt(1149083651642).toString());
      ts.unsigned = true;
      expect(packets[0].timestamp).toEqual(ts);
    });
  });
});
