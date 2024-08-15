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
import {FunctionUtils} from 'common/function_utils';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TracesUtils} from 'test/unit/traces_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TraceUtils} from 'test/unit/trace_utils';
import {FrameMapBuilder} from './frame_map_builder';
import {AbsoluteFrameIndex} from './index_types';
import {Traces} from './traces';
import {TraceType} from './trace_type';

describe('Traces', () => {
  let traces: Traces;

  const time1 = TimestampConverterUtils.makeRealTimestamp(1n);
  const time2 = TimestampConverterUtils.makeRealTimestamp(2n);
  const time3 = TimestampConverterUtils.makeRealTimestamp(3n);
  const time4 = TimestampConverterUtils.makeRealTimestamp(4n);
  const time5 = TimestampConverterUtils.makeRealTimestamp(5n);
  const time6 = TimestampConverterUtils.makeRealTimestamp(6n);
  const time7 = TimestampConverterUtils.makeRealTimestamp(7n);
  const time8 = TimestampConverterUtils.makeRealTimestamp(8n);
  const time9 = TimestampConverterUtils.makeRealTimestamp(9n);
  const time10 = TimestampConverterUtils.makeRealTimestamp(10n);

  let extractedEntriesEmpty: Map<TraceType, Array<{}>>;
  let extractedEntriesFull: Map<TraceType, Array<{}>>;
  let extractedFramesEmpty: Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>;
  let extractedFramesFull: Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>;

  beforeAll(() => {
    // Time:               1  2  3  4  5  6  7  8  9 10
    //
    // TEST_TRACE_STRING:  0     1--2     3        4
    //                      \        \     \        \
    //                       \        \     \        \
    // TEST_TRACE_NUMBER:     0        1     2--3     4
    //                         \        \        \     \
    //                          \        \        \     \
    // Frame on screen:          0        1        2     3---4
    traces = new Traces();
    traces.addTrace(
      new TraceBuilder<string>()
        .setType(TraceType.TEST_TRACE_STRING)
        .setEntries(['0', '1', '2', '3', '4'])
        .setTimestamps([time1, time3, time4, time6, time9])
        .setFrame(0, 0)
        .setFrame(1, 1)
        .setFrame(2, 1)
        .setFrame(3, 2)
        .setFrame(4, 3)
        .setFrame(4, 4)
        .build(),
    );
    traces.addTrace(
      new TraceBuilder<number>()
        .setType(TraceType.TEST_TRACE_NUMBER)
        .setEntries([0, 1, 2, 3, 4])
        .setTimestamps([time2, time5, time7, time8, time10])
        .setFrame(0, 0)
        .setFrame(1, 1)
        .setFrame(2, 2)
        .setFrame(3, 2)
        .setFrame(4, 3)
        .setFrame(4, 4)
        .build(),
    );

    extractedEntriesEmpty = new Map<TraceType, Array<{}>>([
      [TraceType.TEST_TRACE_STRING, []],
      [TraceType.TEST_TRACE_NUMBER, []],
    ]);

    extractedEntriesFull = new Map<TraceType, Array<{}>>([
      [TraceType.TEST_TRACE_STRING, ['0', '1', '2', '3', '4']],
      [TraceType.TEST_TRACE_NUMBER, [0, 1, 2, 3, 4]],
    ]);

    extractedFramesEmpty = new Map<
      AbsoluteFrameIndex,
      Map<TraceType, Array<{}>>
    >();

    extractedFramesFull = new Map<
      AbsoluteFrameIndex,
      Map<TraceType, Array<{}>>
    >();
    extractedFramesFull.set(
      0,
      new Map<TraceType, Array<{}>>([
        [TraceType.TEST_TRACE_STRING, ['0']],
        [TraceType.TEST_TRACE_NUMBER, [0]],
      ]),
    );
    extractedFramesFull.set(
      1,
      new Map<TraceType, Array<{}>>([
        [TraceType.TEST_TRACE_STRING, ['1', '2']],
        [TraceType.TEST_TRACE_NUMBER, [1]],
      ]),
    );
    extractedFramesFull.set(
      2,
      new Map<TraceType, Array<{}>>([
        [TraceType.TEST_TRACE_STRING, ['3']],
        [TraceType.TEST_TRACE_NUMBER, [2, 3]],
      ]),
    );
    extractedFramesFull.set(
      3,
      new Map<TraceType, Array<{}>>([
        [TraceType.TEST_TRACE_STRING, ['4']],
        [TraceType.TEST_TRACE_NUMBER, [4]],
      ]),
    );
    extractedFramesFull.set(
      4,
      new Map<TraceType, Array<{}>>([
        [TraceType.TEST_TRACE_STRING, ['4']],
        [TraceType.TEST_TRACE_NUMBER, [4]],
      ]),
    );
  });

  it('getTrace()', async () => {
    expect(
      await TraceUtils.extractEntries(
        assertDefined(traces.getTrace(TraceType.TEST_TRACE_STRING)),
      ),
    ).toEqual(
      extractedEntriesFull.get(TraceType.TEST_TRACE_STRING) as string[],
    );
    expect(
      await TraceUtils.extractEntries(
        assertDefined(traces.getTrace(TraceType.TEST_TRACE_NUMBER)),
      ),
    ).toEqual(
      extractedEntriesFull.get(TraceType.TEST_TRACE_NUMBER) as number[],
    );
    expect(traces.getTrace(TraceType.SURFACE_FLINGER)).toBeUndefined();
  });

  it('getTraces()', async () => {
    expect(traces.getTraces(TraceType.TEST_TRACE_NUMBER)).toEqual([
      assertDefined(traces.getTrace(TraceType.TEST_TRACE_NUMBER)),
    ]);
  });

  it('deleteTrace()', () => {
    const trace0 = new TraceBuilder<string>()
      .setType(TraceType.TEST_TRACE_STRING)
      .setEntries([])
      .build();
    const trace1 = new TraceBuilder<number>()
      .setType(TraceType.TEST_TRACE_NUMBER)
      .setEntries([])
      .build();

    const traces = new Traces();
    traces.addTrace(trace0);
    traces.addTrace(trace1);

    expect(TracesUtils.extractTraces(traces)).toEqual([trace0, trace1]);

    traces.deleteTrace(trace0);
    expect(TracesUtils.extractTraces(traces)).toEqual([trace1]);

    traces.deleteTrace(trace1);
    expect(TracesUtils.extractTraces(traces)).toEqual([]);

    traces.deleteTrace(trace1);
    expect(TracesUtils.extractTraces(traces)).toEqual([]);
  });

  it('hasTrace()', () => {
    const trace0 = new TraceBuilder<string>()
      .setType(TraceType.TEST_TRACE_STRING)
      .setEntries([])
      .build();
    const trace1 = new TraceBuilder<number>()
      .setType(TraceType.TEST_TRACE_NUMBER)
      .setEntries([])
      .build();

    const traces = new Traces();
    traces.addTrace(trace0);

    expect(traces.hasTrace(trace0)).toBeTrue();
    expect(traces.hasTrace(trace1)).toBeFalse();
  });

  it('sliceTime()', async () => {
    // empty
    {
      const slice = traces.sliceTime(time3, time3);
      expect(await TracesUtils.extractEntries(slice)).toEqual(
        extractedEntriesEmpty,
      );
    }
    // full
    {
      const slice = traces.sliceTime();
      expect(await TracesUtils.extractEntries(slice)).toEqual(
        extractedEntriesFull,
      );
    }
    // middle
    {
      const slice = traces.sliceTime(time4, time8);
      expect(await TracesUtils.extractEntries(slice)).toEqual(
        new Map<TraceType, Array<{}>>([
          [TraceType.TEST_TRACE_STRING, ['2', '3']],
          [TraceType.TEST_TRACE_NUMBER, [1, 2]],
        ]),
      );
    }
    // slice away front
    {
      const slice = traces.sliceTime(time8);
      expect(await TracesUtils.extractEntries(slice)).toEqual(
        new Map<TraceType, Array<{}>>([
          [TraceType.TEST_TRACE_STRING, ['4']],
          [TraceType.TEST_TRACE_NUMBER, [3, 4]],
        ]),
      );
    }
    // slice away back
    {
      const slice = traces.sliceTime(undefined, time8);
      expect(await TracesUtils.extractEntries(slice)).toEqual(
        new Map<TraceType, Array<{}>>([
          [TraceType.TEST_TRACE_STRING, ['0', '1', '2', '3']],
          [TraceType.TEST_TRACE_NUMBER, [0, 1, 2]],
        ]),
      );
    }
  });

  it('sliceFrames()', async () => {
    // empty
    {
      const slice = traces.sliceFrames(1, 1);
      expect(await TracesUtils.extractFrames(slice)).toEqual(
        extractedFramesEmpty,
      );
    }
    // full
    {
      const slice = traces.sliceFrames();
      expect(await TracesUtils.extractFrames(slice)).toEqual(
        extractedFramesFull,
      );
    }
    // middle
    {
      const slice = traces.sliceFrames(1, 4);
      const expectedFrames = structuredClone(extractedFramesFull);
      expectedFrames.delete(0);
      expectedFrames.delete(4);
      expect(await TracesUtils.extractFrames(slice)).toEqual(expectedFrames);
    }
    // slice away front
    {
      const slice = traces.sliceFrames(2);
      const expectedFrames = structuredClone(extractedFramesFull);
      expectedFrames.delete(0);
      expectedFrames.delete(1);
      expect(await TracesUtils.extractFrames(slice)).toEqual(expectedFrames);
    }
    // slice away back
    {
      const slice = traces.sliceFrames(undefined, 2);
      const expectedFrames = structuredClone(extractedFramesFull);
      expectedFrames.delete(2);
      expectedFrames.delete(3);
      expectedFrames.delete(4);
      expect(await TracesUtils.extractFrames(slice)).toEqual(expectedFrames);
    }
  });

  it('mapTrace()', async () => {
    const promises = traces.mapTrace(async (trace) => {
      const expectedEntries = extractedEntriesFull.get(trace.type) as Array<{}>;
      const actualEntries = await TraceUtils.extractEntries(trace);
      expect(actualEntries).toEqual(expectedEntries);
    });
    await Promise.all(promises);
  });

  it('mapFrame()', async () => {
    expect(await TracesUtils.extractFrames(traces)).toEqual(
      extractedFramesFull,
    );
  });

  it('supports empty traces', async () => {
    const traces = new TracesBuilder()
      .setEntries(TraceType.TEST_TRACE_STRING, [])
      .setFrameMap(
        TraceType.TEST_TRACE_STRING,
        new FrameMapBuilder(0, 0).build(),
      )

      .setEntries(TraceType.TEST_TRACE_NUMBER, [])
      .setFrameMap(
        TraceType.TEST_TRACE_NUMBER,
        new FrameMapBuilder(0, 0).build(),
      )
      .build();

    expect(await TracesUtils.extractEntries(traces)).toEqual(
      extractedEntriesEmpty,
    );
    expect(await TracesUtils.extractFrames(traces)).toEqual(
      extractedFramesEmpty,
    );

    expect(
      await TracesUtils.extractEntries(traces.sliceTime(time1, time10)),
    ).toEqual(extractedEntriesEmpty);
    expect(
      await TracesUtils.extractFrames(traces.sliceTime(time1, time10)),
    ).toEqual(extractedFramesEmpty);

    expect(await TracesUtils.extractEntries(traces.sliceFrames(0, 10))).toEqual(
      extractedEntriesEmpty,
    );
    expect(await TracesUtils.extractFrames(traces.sliceFrames(0, 10))).toEqual(
      extractedFramesEmpty,
    );
  });

  it('supports unavailable frame mapping', async () => {
    const traces = new TracesBuilder()
      .setEntries(TraceType.TEST_TRACE_STRING, ['entry-0'])
      .setTimestamps(TraceType.TEST_TRACE_STRING, [time1])
      .setFrameMap(TraceType.TEST_TRACE_STRING, undefined)

      .setEntries(TraceType.TEST_TRACE_NUMBER, [0])
      .setTimestamps(TraceType.TEST_TRACE_NUMBER, [time1])
      .setFrameMap(TraceType.TEST_TRACE_NUMBER, undefined)
      .build();

    const expectedEntries = new Map<TraceType, Array<{}>>([
      [TraceType.TEST_TRACE_STRING, ['entry-0']],
      [TraceType.TEST_TRACE_NUMBER, [0]],
    ]);

    expect(await TracesUtils.extractEntries(traces)).toEqual(expectedEntries);
    expect(await TracesUtils.extractEntries(traces.sliceTime())).toEqual(
      expectedEntries,
    );

    expect(() => {
      traces.sliceFrames();
    }).toThrow();
    expect(() => {
      traces.forEachFrame(FunctionUtils.DO_NOTHING);
    }).toThrow();
    expect(() => {
      traces.mapFrame(FunctionUtils.DO_NOTHING);
    }).toThrow();
  });

  it('supports multiple traces with same type', () => {
    const traceShort = new TraceBuilder<number>()
      .setType(TraceType.TEST_TRACE_NUMBER)
      .setEntries([0])
      .build();
    const traceLong = new TraceBuilder<number>()
      .setType(TraceType.TEST_TRACE_NUMBER)
      .setEntries([1, 2])
      .build();

    const traces = new Traces();
    traces.addTrace(traceShort);
    traces.addTrace(traceLong);

    expect(traces.getTraces(TraceType.TEST_TRACE_NUMBER)).toEqual([
      traceShort,
      traceLong,
    ]);
    expect(traces.getTrace(TraceType.TEST_TRACE_NUMBER)).toEqual(traceLong);
  });
});
