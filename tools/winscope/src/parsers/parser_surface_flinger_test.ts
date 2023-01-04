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
import {Layer} from 'common/trace/flickerlib/layers/Layer';
import {LayerTraceEntry} from 'common/trace/flickerlib/layers/LayerTraceEntry';
import {Timestamp, TimestampType} from 'common/trace/timestamp';
import {TraceType} from 'common/trace/trace_type';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from './parser';

describe('ParserSurfaceFlinger', () => {
  it('decodes layer state flags', async () => {
    const parser = await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb'
    );
    const timestamp = new Timestamp(TimestampType.REAL, 1659107089102062832n);
    const entry = parser.getTraceEntry(timestamp);

    {
      const layer = entry.flattenedLayers.find((layer: Layer) => layer.id === 27);
      expect(layer.name).toEqual('Leaf:24:25#27');
      expect(layer.flags).toEqual(0x0);
      expect(layer.verboseFlags).toEqual('');
    }
    {
      const layer = entry.flattenedLayers.find((layer: Layer) => layer.id === 48);
      expect(layer.name).toEqual('Task=4#48');
      expect(layer.flags).toEqual(0x1);
      expect(layer.verboseFlags).toEqual('HIDDEN (0x1)');
    }
    {
      const layer = entry.flattenedLayers.find((layer: Layer) => layer.id === 77);
      expect(layer.name).toEqual('Wallpaper BBQ wrapper#77');
      expect(layer.flags).toEqual(0x100);
      expect(layer.verboseFlags).toEqual('ENABLE_BACKPRESSURE (0x100)');
    }
  });

  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamps', () => {
      const expected = [
        new Timestamp(TimestampType.ELAPSED, 14500282843n),
        new Timestamp(TimestampType.ELAPSED, 14631249355n),
        new Timestamp(TimestampType.ELAPSED, 15403446377n),
      ];
      expect(parser.getTimestamps(TimestampType.ELAPSED)!.slice(0, 3)).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        new Timestamp(TimestampType.REAL, 1659107089102062832n),
        new Timestamp(TimestampType.REAL, 1659107089233029344n),
        new Timestamp(TimestampType.REAL, 1659107090005226366n),
      ];
      expect(parser.getTimestamps(TimestampType.REAL)!.slice(0, 3)).toEqual(expected);
    });

    it('retrieves trace entry from elapsed timestamp', () => {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 14631249355n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(LayerTraceEntry);
      expect(BigInt(entry.timestamp.systemUptimeNanos.toString())).toEqual(14631249355n);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(1659107089233029344n);
    });

    it('retrieves trace entry from real timestamp', () => {
      const timestamp = new Timestamp(TimestampType.REAL, 1659107089233029376n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(LayerTraceEntry);
      expect(BigInt(entry.timestamp.systemUptimeNanos.toString())).toEqual(14631249355n);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(1659107089233029344n);
    });

    it('formats entry timestamps', () => {
      const timestamp = new Timestamp(TimestampType.REAL, 1659107089233029344n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry.name).toEqual('2022-07-29T15:04:49.233029376');
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_timestamp/SurfaceFlinger.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamps', () => {
      expect(parser.getTimestamps(TimestampType.ELAPSED)![0]).toEqual(
        new Timestamp(TimestampType.ELAPSED, 850335483446n)
      );
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });

    it('formats entry timestamps', () => {
      expect(() => {
        const timestamp = new Timestamp(TimestampType.REAL, 1659107089233029344n);
        parser.getTraceEntry(timestamp);
      }).toThrow(Error('Timestamps with type "REAL" not available'));

      const timestamp = new Timestamp(TimestampType.ELAPSED, 850335483446n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry.name).toEqual('14m10s335ms483446ns');
    });
  });
});
