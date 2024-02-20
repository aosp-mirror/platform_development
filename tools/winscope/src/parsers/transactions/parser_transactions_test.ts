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
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('ParserTransactions', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/Transactions.pb',
      )) as Parser<PropertyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
    });

    it('provides elapsed timestamps', () => {
      const timestamps = assertDefined(
        parser.getTimestamps(TimestampType.ELAPSED),
      );

      expect(timestamps.length).toEqual(712);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(2450981445n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(2517952515n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(4021151449n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const timestamps = assertDefined(
        parser.getTimestamps(TimestampType.REAL),
      );

      expect(timestamps.length).toEqual(712);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659507541051480997n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659507541118452067n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659507542621651001n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('applies timezone info to real timestamps only', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/Transactions.pb',
        true,
      )) as Parser<PropertyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.TRANSACTIONS,
      );

      const expectedElapsed = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(2450981445n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(2517952515n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(4021151449n),
      ];
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        ).slice(0, 3),
      ).toEqual(expectedElapsed);

      const expectedReal = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659527341051480997n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659527341118452067n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659527342621651001n),
      ];
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
        ).slice(0, 3),
      ).toEqual(expectedReal);
    });

    it('retrieves trace entry from real timestamp', async () => {
      const entry = await parser.getEntry(1, TimestampType.REAL);
      expect(entry.id).toEqual('TransactionsTraceEntry entry');
    });

    it("decodes 'what' field in proto", async () => {
      {
        const entry = await parser.getEntry(0, TimestampType.REAL);
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
        const entry = await parser.getEntry(222, TimestampType.REAL);
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

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<PropertyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/Transactions.pb',
      )) as Parser<PropertyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
    });

    it('provides elapsed timestamps', () => {
      const timestamps = assertDefined(
        parser.getTimestamps(TimestampType.ELAPSED),
      );

      expect(timestamps.length).toEqual(4997);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14862317023n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14873423549n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14884850511n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('does not apply timezone info', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/Transactions.pb',
        true,
      )) as Parser<PropertyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.TRANSACTIONS,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        )[0],
      ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14862317023n));
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });
  });
});
