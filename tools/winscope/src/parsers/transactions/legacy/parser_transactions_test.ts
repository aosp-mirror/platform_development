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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('ParserTransactions', () => {
  describe('trace with real timestamps', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/Transactions.pb',
      )) as Parser<PropertyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('provides timestamps', () => {
      const timestamps = assertDefined(parser.getTimestamps());

      expect(timestamps.length).toEqual(712);

      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1659507541051480997n),
        TimestampConverterUtils.makeRealTimestamp(1659507541118452067n),
        TimestampConverterUtils.makeRealTimestamp(1659507542621651001n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('retrieves trace entry from timestamp', async () => {
      const entry = await parser.getEntry(1);
      expect(entry.id).toEqual('TransactionsTraceEntry entry');
    });

    it("decodes 'what' field in proto", async () => {
      {
        const entry = await parser.getEntry(0);
        const transactions = assertDefined(
          entry.getChildByName('transactions'),
        );

        expect(
          transactions
            .getChildByName('0')
            ?.getChildByName('layerChanges')
            ?.getChildByName('0')
            ?.getChildByName('what')
            ?.formattedValue(),
        ).toEqual('eLayerChanged');

        expect(
          transactions
            .getChildByName('1')
            ?.getChildByName('layerChanges')
            ?.getChildByName('0')
            ?.getChildByName('what')
            ?.formattedValue(),
        ).toEqual('eFlagsChanged | eDestinationFrameChanged');
      }
      {
        const entry = await parser.getEntry(222);
        const transactions = assertDefined(
          entry.getChildByName('transactions'),
        );

        expect(
          transactions
            .getChildByName('1')
            ?.getChildByName('displayChanges')
            ?.getChildByName('0')
            ?.getChildByName('what')
            ?.formattedValue(),
        ).toEqual(
          'eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged',
        );
      }
    });

    it('supports VSYNCID custom query', async () => {
      const trace = new TraceBuilder()
        .setType(TraceType.TRANSACTIONS)
        .setParser(parser)
        .build();
      const entries = await trace
        .sliceEntries(0, 3)
        .customQuery(CustomQueryType.VSYNCID);
      const values = entries.map((entry) => entry.getValue());
      expect(values).toEqual([1n, 2n, 3n]);
    });
  });

  describe('trace with only elapsed timestamps', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/Transactions.pb',
      )) as Parser<PropertyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
    });

    it('provides timestamps', () => {
      const timestamps = assertDefined(parser.getTimestamps());

      expect(timestamps.length).toEqual(4997);

      const expected = [
        TimestampConverterUtils.makeElapsedTimestamp(14862317023n),
        TimestampConverterUtils.makeElapsedTimestamp(14873423549n),
        TimestampConverterUtils.makeElapsedTimestamp(14884850511n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });
  });
});
