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

import {TracesUtils} from 'test/unit/traces_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {LayerTraceEntry} from './flickerlib/layers/LayerTraceEntry';
import {WindowManagerState} from './flickerlib/windows/WindowManagerState';
import {FrameMapper} from './frame_mapper';
import {AbsoluteFrameIndex} from './index_types';
import {LogMessage} from './protolog';
import {ScreenRecordingTraceEntry} from './screen_recording';
import {RealTimestamp} from './timestamp';
import {Trace} from './trace';
import {Traces} from './traces';
import {TraceType} from './trace_type';

describe('FrameMapper', () => {
  const time0 = new RealTimestamp(0n);
  const time1 = new RealTimestamp(1n);
  const time2 = new RealTimestamp(2n);
  const time3 = new RealTimestamp(3n);
  const time4 = new RealTimestamp(4n);
  const time5 = new RealTimestamp(5n);
  const time6 = new RealTimestamp(6n);
  const time7 = new RealTimestamp(7n);
  const time8 = new RealTimestamp(8n);
  const time9 = new RealTimestamp(9n);
  const time10 = new RealTimestamp(10n);
  const time10seconds = new RealTimestamp(10n * 1000000000n);

  describe('ProtoLog <-> WindowManager', () => {
    let protoLog: Trace<LogMessage>;
    let windowManager: Trace<WindowManagerState>;
    let traces: Traces;

    beforeAll(() => {
      // Frames              F0        F1
      //                 |<------>|  |<->|
      // PROTO_LOG:      0  1  2     3  4  5
      // WINDOW_MANAGER:          0     1
      // Time:           0  1  2  3  4  5  6
      protoLog = new TraceBuilder<LogMessage>()
        .setEntries([
          'entry-0' as unknown as LogMessage,
          'entry-1' as unknown as LogMessage,
          'entry-2' as unknown as LogMessage,
          'entry-3' as unknown as LogMessage,
          'entry-4' as unknown as LogMessage,
          'entry-5' as unknown as LogMessage,
        ])
        .setTimestamps([time0, time1, time2, time4, time5, time6])
        .build();

      windowManager = new TraceBuilder<WindowManagerState>()
        .setEntries([
          'entry-0' as unknown as WindowManagerState,
          'entry-1' as unknown as WindowManagerState,
        ])
        .setTimestamps([time3, time5])
        .build();

      traces = new Traces();
      traces.setTrace(TraceType.PROTO_LOG, protoLog);
      traces.setTrace(TraceType.WINDOW_MANAGER, windowManager);
      new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', () => {
      const expectedFrames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.PROTO_LOG, ['entry-0', 'entry-1', 'entry-2']],
          [TraceType.WINDOW_MANAGER, ['entry-0']],
        ])
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.PROTO_LOG, ['entry-3', 'entry-4']],
          [TraceType.WINDOW_MANAGER, ['entry-1']],
        ])
      );

      expect(TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('IME <-> WindowManager', () => {
    let ime: Trace<object>;
    let windowManager: Trace<WindowManagerState>;
    let traces: Traces;

    beforeAll(() => {
      // IME:            0--1--2     3
      //                    |        |
      // WINDOW_MANAGER:    0        1  2
      // Time:           0  1  2  3  4  5
      ime = new TraceBuilder<object>()
        .setEntries([
          'entry-0' as unknown as object,
          'entry-1' as unknown as object,
          'entry-2' as unknown as object,
          'entry-3' as unknown as object,
        ])
        .setTimestamps([time0, time1, time2, time4])
        .build();

      windowManager = new TraceBuilder<WindowManagerState>()
        .setEntries([
          'entry-0' as unknown as WindowManagerState,
          'entry-1' as unknown as WindowManagerState,
          'entry-2' as unknown as WindowManagerState,
        ])
        .setTimestamps([time1, time4, time5])
        .build();

      traces = new Traces();
      traces.setTrace(TraceType.INPUT_METHOD_CLIENTS, ime);
      traces.setTrace(TraceType.WINDOW_MANAGER, windowManager);
      new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', () => {
      const expectedFrames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.INPUT_METHOD_CLIENTS, ['entry-0', 'entry-1', 'entry-2']],
          [TraceType.WINDOW_MANAGER, ['entry-0']],
        ])
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.INPUT_METHOD_CLIENTS, ['entry-3']],
          [TraceType.WINDOW_MANAGER, ['entry-1']],
        ])
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.INPUT_METHOD_CLIENTS, []],
          [TraceType.WINDOW_MANAGER, ['entry-2']],
        ])
      );

      expect(TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('WindowManager <-> Transactions', () => {
    let windowManager: Trace<WindowManagerState>;
    let transactions: Trace<object>;
    let traces: Traces;

    beforeAll(() => {
      // WINDOW_MANAGER:     0  1     2  3
      //                     |  |     |    \
      // TRANSACTIONS:    0  1  2--3  4     5  ... 6  <-- ignored (not connected) because too far
      //                  |  |   |    |     |      |
      // Frames:          0  1   2    3     4  ... 5
      // Time:            0  1  2  3  4  5  6  ... 10s
      windowManager = new TraceBuilder<LogMessage>()
        .setEntries([
          'entry-0' as unknown as WindowManagerState,
          'entry-1' as unknown as WindowManagerState,
          'entry-2' as unknown as WindowManagerState,
          'entry-3' as unknown as WindowManagerState,
        ])
        .setTimestamps([time1, time2, time4, time5])
        .build();

      transactions = new TraceBuilder<object>()
        .setEntries([
          'entry-0' as unknown as object,
          'entry-1' as unknown as object,
          'entry-2' as unknown as object,
          'entry-3' as unknown as object,
          'entry-4' as unknown as object,
          'entry-5' as unknown as object,
          'entry-6' as unknown as object,
        ])
        .setTimestamps([time0, time1, time2, time3, time4, time5, time10seconds])
        .setFrame(0, 0)
        .setFrame(1, 1)
        .setFrame(2, 2)
        .setFrame(3, 2)
        .setFrame(4, 3)
        .setFrame(5, 4)
        .setFrame(6, 5)
        .build();

      traces = new Traces();
      traces.setTrace(TraceType.WINDOW_MANAGER, windowManager);
      traces.setTrace(TraceType.TRANSACTIONS, transactions);
      new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', () => {
      const expectedFrames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, []],
          [TraceType.TRANSACTIONS, ['entry-0']],
        ])
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-0']],
          [TraceType.TRANSACTIONS, ['entry-1']],
        ])
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-1']],
          [TraceType.TRANSACTIONS, ['entry-2', 'entry-3']],
        ])
      );
      expectedFrames.set(
        3,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-2']],
          [TraceType.TRANSACTIONS, ['entry-4']],
        ])
      );
      expectedFrames.set(
        4,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, ['entry-3']],
          [TraceType.TRANSACTIONS, ['entry-5']],
        ])
      );
      expectedFrames.set(
        5,
        new Map<TraceType, Array<{}>>([
          [TraceType.WINDOW_MANAGER, []],
          [TraceType.TRANSACTIONS, ['entry-6']],
        ])
      );

      expect(TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('Transactions <-> SurfaceFlinger', () => {
    let transactions: Trace<object>;
    let surfaceFlinger: Trace<LayerTraceEntry>;
    let traces: Traces;

    beforeAll(() => {
      // TRANSACTIONS:   0  1--2        3  4
      //                  \     \        \
      //                   \     \        \
      // SURFACE_FLINGER:   0     1        2
      transactions = new TraceBuilder<object>()
        .setEntries([
          {id: 0, vsyncId: createVsyncId(0)},
          {id: 1, vsyncId: createVsyncId(10)},
          {id: 2, vsyncId: createVsyncId(10)},
          {id: 3, vsyncId: createVsyncId(20)},
          {id: 4, vsyncId: createVsyncId(30)},
        ])
        .setTimestamps([time0, time1, time2, time5, time6])
        .build();

      surfaceFlinger = new TraceBuilder<LayerTraceEntry>()
        .setEntries([
          {id: 0, vSyncId: createVsyncId(0)} as unknown as LayerTraceEntry,
          {id: 1, vSyncId: createVsyncId(10)} as unknown as LayerTraceEntry,
          {id: 2, vSyncId: createVsyncId(20)} as unknown as LayerTraceEntry,
        ])
        .setTimestamps([time0, time1, time2])
        .build();

      traces = new Traces();
      traces.setTrace(TraceType.TRANSACTIONS, transactions);
      traces.setTrace(TraceType.SURFACE_FLINGER, surfaceFlinger);
      new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', () => {
      const expectedFrames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.TRANSACTIONS, [transactions.getEntry(0).getValue()]],
          [TraceType.SURFACE_FLINGER, [surfaceFlinger.getEntry(0).getValue()]],
        ])
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [
            TraceType.TRANSACTIONS,
            [transactions.getEntry(1).getValue(), transactions.getEntry(2).getValue()],
          ],
          [TraceType.SURFACE_FLINGER, [surfaceFlinger.getEntry(1).getValue()]],
        ])
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.TRANSACTIONS, [transactions.getEntry(3).getValue()]],
          [TraceType.SURFACE_FLINGER, [surfaceFlinger.getEntry(2).getValue()]],
        ])
      );

      expect(TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  describe('SurfaceFlinger <-> ScreenRecording', () => {
    let surfaceFlinger: Trace<LayerTraceEntry>;
    let screenRecording: Trace<ScreenRecordingTraceEntry>;
    let traces: Traces;

    beforeAll(() => {
      // SURFACE_FLINGER:      0  1  2---  3     4  5  6
      //                              \  \  \        \
      //                               \  \  \        \
      // SCREEN_RECORDING:     0        1  2  3        4 ... 5 <-- ignored (not connected) because too far
      // Time:                 0  1  2  3  4  5  6  7  8     10s
      surfaceFlinger = new TraceBuilder<LayerTraceEntry>()
        .setEntries([
          'entry-0' as unknown as LayerTraceEntry,
          'entry-1' as unknown as LayerTraceEntry,
          'entry-2' as unknown as LayerTraceEntry,
          'entry-3' as unknown as LayerTraceEntry,
          'entry-4' as unknown as LayerTraceEntry,
          'entry-5' as unknown as LayerTraceEntry,
          'entry-6' as unknown as LayerTraceEntry,
        ])
        .setTimestamps([time0, time1, time2, time4, time6, time7, time8])
        .build();

      screenRecording = new TraceBuilder<ScreenRecordingTraceEntry>()
        .setEntries([
          'entry-0' as unknown as ScreenRecordingTraceEntry,
          'entry-1' as unknown as ScreenRecordingTraceEntry,
          'entry-2' as unknown as ScreenRecordingTraceEntry,
          'entry-3' as unknown as ScreenRecordingTraceEntry,
          'entry-4' as unknown as ScreenRecordingTraceEntry,
          'entry-5' as unknown as ScreenRecordingTraceEntry,
        ])
        .setTimestamps([time0, time3, time4, time5, time8, time10seconds])
        .build();

      traces = new Traces();
      traces.setTrace(TraceType.SURFACE_FLINGER, surfaceFlinger);
      traces.setTrace(TraceType.SCREEN_RECORDING, screenRecording);
      new FrameMapper(traces).computeMapping();
    });

    it('associates entries/frames', () => {
      const expectedFrames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();
      expectedFrames.set(
        0,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, []],
          [TraceType.SCREEN_RECORDING, ['entry-0']],
        ])
      );
      expectedFrames.set(
        1,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-2']],
          [TraceType.SCREEN_RECORDING, ['entry-1']],
        ])
      );
      expectedFrames.set(
        2,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-2']],
          [TraceType.SCREEN_RECORDING, ['entry-2']],
        ])
      );
      expectedFrames.set(
        3,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-3']],
          [TraceType.SCREEN_RECORDING, ['entry-3']],
        ])
      );
      expectedFrames.set(
        4,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, ['entry-5']],
          [TraceType.SCREEN_RECORDING, ['entry-4']],
        ])
      );
      expectedFrames.set(
        5,
        new Map<TraceType, Array<{}>>([
          [TraceType.SURFACE_FLINGER, []],
          [TraceType.SCREEN_RECORDING, ['entry-5']],
        ])
      );

      expect(TracesUtils.extractFrames(traces)).toEqual(expectedFrames);
    });
  });

  const createVsyncId = (value: number): object => {
    return {
      toString() {
        return value.toString();
      },
    };
  };
});
