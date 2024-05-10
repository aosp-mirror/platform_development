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
import {Timestamp, TimestampType} from 'common/time';
import {Layer} from 'flickerlib/layers/Layer';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';

describe('ParserSurfaceFlinger', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<LayerTraceEntry>;
    let trace: Trace<LayerTraceEntry>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb');
      trace = new TraceBuilder().setType(TraceType.SURFACE_FLINGER).setParser(parser).build();
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

    it('formats entry timestamps', async () => {
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

    it('supports VSYNCID custom query', async () => {
      const entries = await trace.sliceEntries(0, 3).customQuery(CustomQueryType.VSYNCID);
      const values = entries.map((entry) => entry.getValue());
      expect(values).toEqual([4891n, 5235n, 5748n]);
    });

    it('supports SF_LAYERS_ID_AND_NAME custom query', async () => {
      const idAndNames = await trace
        .sliceEntries(0, 1)
        .customQuery(CustomQueryType.SF_LAYERS_ID_AND_NAME);
      expect(idAndNames).toContain({id: 4, name: 'WindowedMagnification:0:31#4'});
      expect(idAndNames).toContain({id: 5, name: 'HideDisplayCutout:0:14#5'});
    });

    it('is robust to duplicated layer ids', async () => {
      const parser = await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger_with_duplicated_ids.pb'
      );
      const entry = await parser.getEntry(0, TimestampType.REAL);
      expect(entry).toBeTruthy();
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<LayerTraceEntry>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/SurfaceFlinger.pb'
      )) as Parser<LayerTraceEntry>;
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

    it('formats entry timestamps', async () => {
      const entry = await parser.getEntry(0, TimestampType.ELAPSED);
      expect(entry.name).toEqual('14m10s335ms483446ns');
    });
  });
});
