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
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserTransactions', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<object>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_and_real_timestamp/Transactions.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
    });

    it('provides elapsed timestamps', () => {
      const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

      expect(timestamps.length).toEqual(712);

      const expected = [
        new Timestamp(TimestampType.ELAPSED, 2450981445n),
        new Timestamp(TimestampType.ELAPSED, 2517952515n),
        new Timestamp(TimestampType.ELAPSED, 4021151449n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const timestamps = parser.getTimestamps(TimestampType.REAL)!;

      expect(timestamps.length).toEqual(712);

      const expected = [
        new Timestamp(TimestampType.REAL, 1659507541051480997n),
        new Timestamp(TimestampType.REAL, 1659507541118452067n),
        new Timestamp(TimestampType.REAL, 1659507542621651001n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it('retrieves trace entry from real timestamp', async () => {
      const entry = await parser.getEntry(1, TimestampType.REAL);
      expect(BigInt((entry as any).elapsedRealtimeNanos)).toEqual(2517952515n);
    });

    it("decodes 'what' field in proto", async () => {
      {
        const entry = (await parser.getEntry(0, TimestampType.REAL)) as any;
        expect(entry.transactions[0].layerChanges[0].what).toEqual('eLayerChanged');
        expect(entry.transactions[1].layerChanges[0].what).toEqual(
          'eFlagsChanged | eDestinationFrameChanged'
        );
      }
      {
        const entry = (await parser.getEntry(222, TimestampType.REAL)) as any;
        expect(entry.transactions[1].displayChanges[0].what).toEqual(
          'eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged'
        );
      }
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<object>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_timestamp/Transactions.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
    });

    it('provides elapsed timestamps', () => {
      const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

      expect(timestamps.length).toEqual(4997);

      const expected = [
        new Timestamp(TimestampType.ELAPSED, 14862317023n),
        new Timestamp(TimestampType.ELAPSED, 14873423549n),
        new Timestamp(TimestampType.ELAPSED, 14884850511n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });
  });
});
