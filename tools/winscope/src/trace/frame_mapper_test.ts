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

import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesUtils} from 'test/unit/traces_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {CustomQueryType} from './custom_query';
import {FrameMapper} from './frame_mapper';
import {AbsoluteFrameIndex} from './index_types';
import {MediaBasedTraceEntry} from './media_based_trace_entry';
import {Trace} from './trace';
import {Traces} from './traces';
import {TraceType} from './trace_type';
import {HierarchyTreeNode} from './tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from './tree_node/property_tree_node';

describe('FrameMapper', () => {
  const time0 = TimestampConverterUtils.makeRealTimestamp(0n);
  const time1 = TimestampConverterUtils.makeRealTimestamp(1n);
  const time2 = TimestampConverterUtils.makeRealTimestamp(2n);
  const time3 = TimestampConverterUtils.makeRealTimestamp(3n);
  const time4 = TimestampConverterUtils.makeRealTimestamp(4n);
  const time5 = TimestampConverterUtils.makeRealTimestamp(5n);
  const time6 = TimestampConverterUtils.makeRealTimestamp(6n);
  const time7 = TimestampConverterUtils.makeRealTimestamp(7n);
  const time8 = TimestampConverterUtils.makeRealTimestamp(8n);
  const time10seconds = TimestampConverterUtils.makeRealTimestamp(
    10n * 1000000000n,
  );

  describe('ProtoLog <-> WindowManager', () => {
    let protoLog: Trace<PropertyTreeNode>;
    let windowManager: Trace<HierarchyTreeNode>;
    let traces: Traces;

    beforeAll(async () => {
      // Frames              F0        F1
      //                 |<------>|  |<->|
      // PROTO_LOG:      0  1  2     3  4  5
      // WINDOW_MANAGER:          0     1
      // Time:           0  1  2  3  4  5  6
      protoLog = new TraceBuilder<PropertyTreeNode>()
        .setType(TraceType.PROTO_LOG)
        .setEntries([
          'entry-0' as unknown as PropertyTreeNode,
          'entry-1' as unknown as PropertyTreeNode,
          'entry-2' as unknown as PropertyTreeNode,
          'entry-3' as unknown as PropertyTreeNode,
          'entry-4' as unknown as PropertyTreeNode,
          'entry-5' as unknown as PropertyTreeNode,
        ])
        .setTimestamps([time0, time1, time2, time4, time5, time6])
        .build();

      windowManager = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.WINDOW_MANAGER)
        .setEntries([
          'entry-0' as unknown as HierarchyTreeNode,
          'entry-1' as unknown as HierarchyTreeNode,
        ])
        .setTimestamps([time3, time5])
        .build();

      traces = new Traces();
      traces.addTrace(protoLog);
      traces.addTrace(windowManager);
      await new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', async () => {
      const expectedFrames = new Map<
        AbsoluteFrameIndex,
        Map<TraceType, Array<{}>>
      >();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.PROTO_LOG, ['entry-0', 'entry-1', 'entry-2']],
          [TraceType.WINDOW_MANAGER, ['entry-0']],
        ]),
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.PROTO_LOG, ['entry-3', 'entry-4']],
          [TraceType.WINDOW_MANAGER, ['entry-1']],
        ]),
      );

      expect(await TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('IME <-> WindowManager', () => {
    let ime: Trace<HierarchyTreeNode>;
    let windowManager: Trace<HierarchyTreeNode>;
    let traces: Traces;

    beforeAll(async () => {
      // IME:            0--1--2     3
      //                    |        |
      // WINDOW_MANAGER:    0        1  2
      // Time:           0  1  2  3  4  5
      ime = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.INPUT_METHOD_CLIENTS)
        .setEntries([
          'entry-0' as unknown as HierarchyTreeNode,
          'entry-1' as unknown as HierarchyTreeNode,
          'entry-2' as unknown as HierarchyTreeNode,
          'entry-3' as unknown as HierarchyTreeNode,
        ])
        .setTimestamps([time0, time1, time2, time4])
        .build();

      windowManager = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.WINDOW_MANAGER)
        .setEntries([
          'entry-0' as unknown as HierarchyTreeNode,
          'entry-1' as unknown as HierarchyTreeNode,
          'entry-2' as unknown as HierarchyTreeNode,
        ])
        .setTimestamps([time1, time4, time5])
        .build();

      traces = new Traces();
      traces.addTrace(ime);
      traces.addTrace(windowManager);
      await new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', async () => {
      const expectedFrames = new Map<
        AbsoluteFrameIndex,
        Map<TraceType, Array<{}>>
      >();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.INPUT_METHOD_CLIENTS, ['entry-0', 'entry-1', 'entry-2']],
          [TraceType.WINDOW_MANAGER, ['entry-0']],
        ]),
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.INPUT_METHOD_CLIENTS, ['entry-3']],
          [TraceType.WINDOW_MANAGER, ['entry-1']],
        ]),
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.INPUT_METHOD_CLIENTS, []],
          [TraceType.WINDOW_MANAGER, ['entry-2']],
        ]),
      );

      expect(await TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('WindowManager <-> Transactions', () => {
    let windowManager: Trace<HierarchyTreeNode>;
    let transactions: Trace<PropertyTreeNode>;
    let traces: Traces;

    beforeAll(async () => {
      // WINDOW_MANAGER:     0  1     2  3
      //                     |  |     |    \
      // TRANSACTIONS:    0  1  2--3  4     5  ... 6  <-- ignored (not connected) because too far
      //                  |  |   |    |     |      |
      // Frames:          0  1   2    3     4  ... 5
      // Time:            0  1  2  3  4  5  6  ... 10s
      windowManager = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.WINDOW_MANAGER)
        .setEntries([
          'entry-0' as unknown as HierarchyTreeNode,
          'entry-1' as unknown as HierarchyTreeNode,
          'entry-2' as unknown as HierarchyTreeNode,
          'entry-3' as unknown as HierarchyTreeNode,
        ])
        .setTimestamps([time1, time2, time4, time5])
        .build();

      transactions = new TraceBuilder<PropertyTreeNode>()
        .setType(TraceType.TRANSACTIONS)
        .setEntries([
          'entry-0' as unknown as PropertyTreeNode,
          'entry-1' as unknown as PropertyTreeNode,
          'entry-2' as unknown as PropertyTreeNode,
          'entry-3' as unknown as PropertyTreeNode,
          'entry-4' as unknown as PropertyTreeNode,
          'entry-5' as unknown as PropertyTreeNode,
          'entry-6' as unknown as PropertyTreeNode,
        ])
        .setTimestamps([
          time0,
          time1,
          time2,
          time3,
          time4,
          time5,
          time10seconds,
        ])
        .setFrame(0, 0)
        .setFrame(1, 1)
        .setFrame(2, 2)
        .setFrame(3, 2)
        .setFrame(4, 3)
        .setFrame(5, 4)
        .setFrame(6, 5)
        .build();

      traces = new Traces();
      traces.addTrace(windowManager);
      traces.addTrace(transactions);
      await new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', async () => {
      const expectedFrames = new Map<
        AbsoluteFrameIndex,
        Map<TraceType, Array<{}>>
      >();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, []],
          [TraceType.TRANSACTIONS, ['entry-0']],
        ]),
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-0']],
          [TraceType.TRANSACTIONS, ['entry-1']],
        ]),
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-1']],
          [TraceType.TRANSACTIONS, ['entry-2', 'entry-3']],
        ]),
      );
      expectedFrames.set(
        3,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-2']],
          [TraceType.TRANSACTIONS, ['entry-4']],
        ]),
      );
      expectedFrames.set(
        4,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-3']],
          [TraceType.TRANSACTIONS, ['entry-5']],
        ]),
      );
      expectedFrames.set(
        5,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, []],
          [TraceType.TRANSACTIONS, ['entry-6']],
        ]),
      );

      expect(await TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('ViewCapture <-> SurfaceFlinger', () => {
    let viewCapture: Trace<PropertyTreeNode>;
    let surfaceFlinger: Trace<HierarchyTreeNode>;
    let traces: Traces;

    beforeAll(async () => {
      // VIEW_CAPTURE:   0  1  2---     3
      //                  \     \  \     \
      //                   \     \  \     \
      // SURFACE_FLINGER:   0     1  2     3
      // Time:           0  1  2  3  4  5  6
      viewCapture = new TraceBuilder<PropertyTreeNode>()
        .setType(TraceType.VIEW_CAPTURE)
        .setEntries([
          'entry-0' as unknown as PropertyTreeNode,
          'entry-1' as unknown as PropertyTreeNode,
          'entry-2' as unknown as PropertyTreeNode,
          'entry-3' as unknown as PropertyTreeNode,
        ])
        .setTimestamps([time0, time1, time2, time5])
        .build();

      surfaceFlinger = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.SURFACE_FLINGER)
        .setEntries([
          'entry-0' as unknown as HierarchyTreeNode,
          'entry-1' as unknown as HierarchyTreeNode,
          'entry-2' as unknown as HierarchyTreeNode,
          'entry-3' as unknown as HierarchyTreeNode,
        ])
        .setTimestamps([time1, time3, time4, time6])
        .setFrame(0, 0)
        .setFrame(1, 1)
        .setFrame(2, 2)
        .setFrame(3, 3)
        .build();

      traces = new Traces();
      traces.addTrace(viewCapture);
      traces.addTrace(surfaceFlinger);
      await new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', async () => {
      const expectedFrames = new Map<
        AbsoluteFrameIndex,
        Map<TraceType, Array<{}>>
      >();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.VIEW_CAPTURE, [await viewCapture.getEntry(0).getValue()]],
          [
            TraceType.SURFACE_FLINGER,
            [await surfaceFlinger.getEntry(0).getValue()],
          ],
        ]),
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.VIEW_CAPTURE, [await viewCapture.getEntry(2).getValue()]],
          [
            TraceType.SURFACE_FLINGER,
            [await surfaceFlinger.getEntry(1).getValue()],
          ],
        ]),
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.VIEW_CAPTURE, [await viewCapture.getEntry(2).getValue()]],
          [
            TraceType.SURFACE_FLINGER,
            [await surfaceFlinger.getEntry(2).getValue()],
          ],
        ]),
      );
      expectedFrames.set(
        3,
        new Map<TraceType, Array<{}>>([
          [TraceType.VIEW_CAPTURE, [await viewCapture.getEntry(3).getValue()]],
          [
            TraceType.SURFACE_FLINGER,
            [await surfaceFlinger.getEntry(3).getValue()],
          ],
        ]),
      );

      expect(await TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  const TRACES_WITH_VSYNC_IDS = [
    TraceType.TRANSACTIONS,
    TraceType.INPUT_EVENT_MERGED,
  ];

  TRACES_WITH_VSYNC_IDS.forEach((traceType) => {
    describe(`TraceType[${traceType}] <-> SurfaceFlinger`, () => {
      let trace: Trace<PropertyTreeNode>;
      let surfaceFlinger: Trace<HierarchyTreeNode>;
      let traces: Traces;

      beforeAll(async () => {
        // TRACE:          0  1--2        3  4
        //                  \     \        \
        //                   \     \        \
        // SURFACE_FLINGER:   0     1        2
        trace = new TraceBuilder<PropertyTreeNode>()
          .setType(traceType)
          .setEntries([
            'entry-0' as unknown as PropertyTreeNode,
            'entry-1' as unknown as PropertyTreeNode,
            'entry-2' as unknown as PropertyTreeNode,
            'entry-3' as unknown as PropertyTreeNode,
            'entry-4' as unknown as PropertyTreeNode,
          ])
          .setTimestamps([time0, time1, time2, time5, time6])
          .setParserCustomQueryResult(CustomQueryType.VSYNCID, [
            0n,
            10n,
            10n,
            20n,
            30n,
          ])
          .build();

        surfaceFlinger = new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.SURFACE_FLINGER)
          .setEntries([
            'entry-0' as unknown as HierarchyTreeNode,
            'entry-1' as unknown as HierarchyTreeNode,
            'entry-2' as unknown as HierarchyTreeNode,
          ])
          .setTimestamps([time0, time1, time2])
          .setParserCustomQueryResult(CustomQueryType.VSYNCID, [0n, 10n, 20n])
          .build();

        traces = new Traces();
        traces.addTrace(trace);
        traces.addTrace(surfaceFlinger);
        await new FrameMapper(traces).computeMapping();
      });

      it('associates entries/frames', async () => {
        const expectedFrames = new Map<
          AbsoluteFrameIndex,
          Map<TraceType, Array<{}>>
        >();
        expectedFrames.set(
          0,
          new Map<TraceType, Array<{}>>([
            [traceType, [await trace.getEntry(0).getValue()]],
            [
              TraceType.SURFACE_FLINGER,
              [await surfaceFlinger.getEntry(0).getValue()],
            ],
          ]),
        );
        expectedFrames.set(
          1,
          new Map<TraceType, Array<{}>>([
            [
              traceType,
              [
                await trace.getEntry(1).getValue(),
                await trace.getEntry(2).getValue(),
              ],
            ],
            [
              TraceType.SURFACE_FLINGER,
              [await surfaceFlinger.getEntry(1).getValue()],
            ],
          ]),
        );
        expectedFrames.set(
          2,
          new Map<TraceType, Array<{}>>([
            [traceType, [await trace.getEntry(3).getValue()]],
            [
              TraceType.SURFACE_FLINGER,
              [await surfaceFlinger.getEntry(2).getValue()],
            ],
          ]),
        );

        expect(await TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
      });
    });
  });

  describe('SurfaceFlinger <-> ScreenRecording', () => {
    let surfaceFlinger: Trace<HierarchyTreeNode>;
    let screenRecording: Trace<MediaBasedTraceEntry>;
    let traces: Traces;

    beforeAll(async () => {
      // SURFACE_FLINGER:      0  1  2---  3     4  5  6
      //                              \  \  \        \
      //                               \  \  \        \
      // SCREEN_RECORDING:     0        1  2  3        4 ... 5 <-- ignored (not connected) because too far
      // Time:                 0  1  2  3  4  5  6  7  8     10s
      surfaceFlinger = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.SURFACE_FLINGER)
        .setEntries([
          'entry-0' as unknown as HierarchyTreeNode,
          'entry-1' as unknown as HierarchyTreeNode,
          'entry-2' as unknown as HierarchyTreeNode,
          'entry-3' as unknown as HierarchyTreeNode,
          'entry-4' as unknown as HierarchyTreeNode,
          'entry-5' as unknown as HierarchyTreeNode,
          'entry-6' as unknown as HierarchyTreeNode,
        ])
        .setTimestamps([time0, time1, time2, time4, time6, time7, time8])
        .build();

      screenRecording = new TraceBuilder<MediaBasedTraceEntry>()
        .setType(TraceType.SCREEN_RECORDING)
        .setEntries([
          'entry-0' as unknown as MediaBasedTraceEntry,
          'entry-1' as unknown as MediaBasedTraceEntry,
          'entry-2' as unknown as MediaBasedTraceEntry,
          'entry-3' as unknown as MediaBasedTraceEntry,
          'entry-4' as unknown as MediaBasedTraceEntry,
          'entry-5' as unknown as MediaBasedTraceEntry,
        ])
        .setTimestamps([time0, time3, time4, time5, time8, time10seconds])
        .build();

      traces = new Traces();
      traces.addTrace(surfaceFlinger);
      traces.addTrace(screenRecording);
      await new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', async () => {
      const expectedFrames = new Map<
        AbsoluteFrameIndex,
        Map<TraceType, Array<{}>>
      >();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, []],
          [TraceType.SCREEN_RECORDING, ['entry-0']],
        ]),
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-2']],
          [TraceType.SCREEN_RECORDING, ['entry-1']],
        ]),
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-2']],
          [TraceType.SCREEN_RECORDING, ['entry-2']],
        ]),
      );
      expectedFrames.set(
        3,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-3']],
          [TraceType.SCREEN_RECORDING, ['entry-3']],
        ]),
      );
      expectedFrames.set(
        4,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-5']],
          [TraceType.SCREEN_RECORDING, ['entry-4']],
        ]),
      );
      expectedFrames.set(
        5,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, []],
          [TraceType.SCREEN_RECORDING, ['entry-5']],
        ]),
      );

      expect(await TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  it('supports multiple traces with same type', async () => {
    // SURFACE_FLINGER_0:    0
    //                        \
    //                         \
    // SURFACE_FLINGER_1:    0  \
    //                        \ |
    //                         \|
    // SCREEN_RECORDING:        0
    // Time:                 0  1
    const surfaceFlinger0 = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries(['entry-0' as unknown as HierarchyTreeNode])
      .setTimestamps([time0])
      .build();

    const surfaceFlinger1 = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries(['entry-0' as unknown as HierarchyTreeNode])
      .setTimestamps([time0])
      .build();

    const screenRecording = new TraceBuilder<MediaBasedTraceEntry>()
      .setType(TraceType.SCREEN_RECORDING)
      .setEntries(['entry-0' as unknown as MediaBasedTraceEntry])
      .setTimestamps([time1])
      .build();

    const traces = new Traces();
    traces.addTrace(surfaceFlinger0);
    traces.addTrace(surfaceFlinger1);
    traces.addTrace(screenRecording);
    await new FrameMapper(traces).computeMapping();

    expect(surfaceFlinger0.getEntry(0).getFramesRange()).toEqual({
      start: 0,
      end: 1,
    });
    expect(surfaceFlinger1.getEntry(0).getFramesRange()).toEqual({
      start: 0,
      end: 1,
    });
    expect(screenRecording.getEntry(0).getFramesRange()).toEqual({
      start: 0,
      end: 1,
    });
  });
});
