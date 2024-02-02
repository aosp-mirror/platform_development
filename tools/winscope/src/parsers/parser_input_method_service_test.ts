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

describe('ParserInputMethodService', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<any>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/InputMethodService.pb'
      );
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_SERVICE);
    });

    it('provides elapsed timestamps', () => {
      const expected = [new Timestamp(TimestampType.ELAPSED, 16578752896n)];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [new Timestamp(TimestampType.REAL, 1659107091180519857n)];
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(expected);
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(0, TimestampType.REAL);
      expect(BigInt(entry.elapsedRealtimeNanos)).toEqual(16578752896n);
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<any>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_timestamp/InputMethodService.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_SERVICE);
    });

    it('provides elapsed timestamps', () => {
      expect(parser.getTimestamps(TimestampType.ELAPSED)![0]).toEqual(
        new Timestamp(TimestampType.ELAPSED, 1149230019887n)
      );
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(0, TimestampType.ELAPSED);
      expect(BigInt(entry.elapsedRealtimeNanos)).toEqual(1149230019887n);
    });
  });
});
