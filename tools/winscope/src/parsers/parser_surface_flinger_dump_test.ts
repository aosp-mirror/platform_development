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
import {LayerTraceEntry} from 'trace/flickerlib/layers/LayerTraceEntry';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserSurfaceFlingerDump', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<LayerTraceEntry>;
    const DUMP_REAL_TIME = 1659176624505188647n;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb'
      );
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamp', () => {
      const expected = [new Timestamp(TimestampType.ELAPSED, 0n)];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it('provides real timestamp (always zero)', () => {
      const expected = [new Timestamp(TimestampType.REAL, 0n)];
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(expected);
    });

    it('retrieves trace entry', () => {
      const entry = parser.getEntry(0, TimestampType.ELAPSED);
      expect(entry).toBeInstanceOf(LayerTraceEntry);
      expect(BigInt(entry.timestamp.systemUptimeNanos.toString())).toEqual(0n);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(DUMP_REAL_TIME);
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<LayerTraceEntry>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_timestamp/dump_SurfaceFlinger.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamp (always zero)', () => {
      const expected = [new Timestamp(TimestampType.ELAPSED, 0n)];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it("doesn't provide real timestamp", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });
  });
});
