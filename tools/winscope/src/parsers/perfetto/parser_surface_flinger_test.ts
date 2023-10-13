/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {ElapsedTimestamp, RealTimestamp, TimestampType} from 'common/time';
import {Layer} from 'flickerlib/common';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

describe('Perfetto ParserSurfaceFlinger', () => {
  describe('valid trace', () => {
    let parser: Parser<LayerTraceEntry>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getPerfettoParser(
        TraceType.SURFACE_FLINGER,
        'traces/perfetto/layers_trace.perfetto-trace'
      );
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamps', () => {
      const expected = [
        new ElapsedTimestamp(14500282843n),
        new ElapsedTimestamp(14631249355n),
        new ElapsedTimestamp(15403446377n),
      ];
      const actual = assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).slice(0, 3);
      expect(actual).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        new RealTimestamp(1659107089102062832n),
        new RealTimestamp(1659107089233029344n),
        new RealTimestamp(1659107090005226366n),
      ];
      const actual = assertDefined(parser.getTimestamps(TimestampType.REAL)).slice(0, 3);
      expect(actual).toEqual(expected);
    });

    it('formats entry timestamps (elapsed)', async () => {
      const entry = await parser.getEntry(1, TimestampType.ELAPSED);
      expect(entry.name).toEqual('14s631ms249355ns');
      expect(BigInt(entry.timestamp.systemUptimeNanos.toString())).toEqual(14631249355n);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(1659107089233029344n);
    });

    it('formats entry timestamps (real)', async () => {
      const entry = await parser.getEntry(1, TimestampType.REAL);
      expect(entry.name).toEqual('2022-07-29T15:04:49.233029376');
      expect(BigInt(entry.timestamp.systemUptimeNanos.toString())).toEqual(14631249355n);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(1659107089233029344n);
    });

    it('decodes layer state flags', async () => {
      const entry = await parser.getEntry(0, TimestampType.REAL);
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
  });

  describe('invalid traces', () => {
    it('is robust to duplicated layer ids', async () => {
      const parser = await UnitTestUtils.getPerfettoParser(
        TraceType.SURFACE_FLINGER,
        'traces/perfetto/layers_trace_with_duplicated_ids.perfetto-trace'
      );
      const entry = await parser.getEntry(0, TimestampType.REAL);
      expect(entry).toBeTruthy();
    });
  });
});
