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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

describe('Perfetto ParserTransactions', () => {
  let parser: Parser<object>;

  beforeAll(async () => {
    parser = await UnitTestUtils.getPerfettoParser(
      TraceType.TRANSACTIONS,
      'traces/perfetto/transactions_trace.perfetto-trace'
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.ELAPSED));

    expect(timestamps.length).toEqual(712);

    const expected = [
      new ElapsedTimestamp(2450981445n),
      new ElapsedTimestamp(2517952515n),
      new ElapsedTimestamp(4021151449n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.REAL));

    expect(timestamps.length).toEqual(712);

    const expected = [
      new RealTimestamp(1659507541051480997n),
      new RealTimestamp(1659507541118452067n),
      new RealTimestamp(1659507542621651001n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry from real timestamp', async () => {
    const entry = await parser.getEntry(1, TimestampType.REAL);
    expect(BigInt((entry as any).elapsedRealtimeNanos)).toEqual(2517952515n);
  });

  it('transforms fake proto built from trace processor args', async () => {
    const entry0 = (await parser.getEntry(0, TimestampType.REAL)) as any;
    const entry2 = (await parser.getEntry(2, TimestampType.REAL)) as any;

    // Add empty arrays
    expect(entry0.addedDisplays).toEqual([]);
    expect(entry0.destroyedLayers).toEqual([]);
    expect(entry0.removedDisplays).toEqual([]);
    expect(entry0.destroyedLayerHandles).toEqual([]);
    expect(entry0.displays).toEqual([]);

    // Add default values
    expect(entry0.transactions[1].pid).toEqual(0);

    // Convert value types (bigint -> number)
    expect(entry0.transactions[1].uid).toEqual(1003);

    // Decode enum IDs
    expect(entry0.transactions[0].layerChanges[0].dropInputMode).toEqual('NONE');
    expect(entry2.transactions[0].layerChanges[0].bufferData.pixelFormat).toEqual(
      'PIXEL_FORMAT_RGBA_1010102'
    );
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

  it('supports VSYNCID custom query', async () => {
    const trace = new TraceBuilder().setType(TraceType.TRANSACTIONS).setParser(parser).build();
    const entries = await trace.sliceEntries(0, 3).customQuery(CustomQueryType.VSYNCID);
    const values = entries.map((entry) => entry.getValue());
    expect(values).toEqual([1n, 2n, 3n]);
  });
});
