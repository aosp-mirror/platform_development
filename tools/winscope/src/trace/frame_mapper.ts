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
import {CustomQueryType} from './custom_query';
import {FrameMapBuilder} from './frame_map_builder';
import {FramesRange, TraceEntry} from './trace';
import {Traces} from './traces';
import {TraceType} from './trace_type';

export class FrameMapper {
  // Value used to narrow time-based searches of corresponding trace entries
  private static readonly MAX_UI_PIPELINE_LATENCY_NS = 2000000000n; // 2 seconds

  constructor(private traces: Traces) {}

  async computeMapping() {
    this.pickMostReliableTraceAndSetInitialFrameInfo();
    await this.propagateFrameInfoToOtherTraces();
  }

  private pickMostReliableTraceAndSetInitialFrameInfo() {
    const TRACES_IN_PREFERENCE_ORDER = [
      TraceType.SCREEN_RECORDING,
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
    ];

    const type = TRACES_IN_PREFERENCE_ORDER.find(
      (type) => this.traces.getTrace(type) !== undefined,
    );
    if (type === undefined) {
      return;
    }

    const trace = assertDefined(this.traces.getTrace(type));
    const frameMapBuilder = new FrameMapBuilder(
      trace.lengthEntries,
      trace.lengthEntries,
    );

    for (let i = 0; i < trace.lengthEntries; ++i) {
      frameMapBuilder.setFrames(i, {start: i, end: i + 1});
    }

    const frameMap = frameMapBuilder.build();
    trace.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
  }

  private async propagateFrameInfoToOtherTraces() {
    this.tryPropagateFromScreenRecordingToSurfaceFlinger();
    await this.tryPropagateFromSurfaceFlingerToTransactions();
    this.tryPropagateFromTransactionsToWindowManager();
    this.tryPropagateFromWindowManagerToProtoLog();
    this.tryPropagateFromWindowManagerToIme();
  }

  private tryPropagateFromScreenRecordingToSurfaceFlinger() {
    const frameMapBuilder = this.tryStartFrameMapping(
      TraceType.SCREEN_RECORDING,
      TraceType.SURFACE_FLINGER,
    );
    if (!frameMapBuilder) {
      return;
    }

    const screenRecording = assertDefined(
      this.traces.getTrace(TraceType.SCREEN_RECORDING),
    );
    const surfaceFlinger = assertDefined(
      this.traces.getTrace(TraceType.SURFACE_FLINGER),
    );

    screenRecording.forEachEntry((srcEntry) => {
      const startSearchTime = srcEntry
        .getTimestamp()
        .add(-FrameMapper.MAX_UI_PIPELINE_LATENCY_NS);
      const endSearchTime = srcEntry.getTimestamp();
      const matches = surfaceFlinger.sliceTime(startSearchTime, endSearchTime);
      if (matches.lengthEntries > 0) {
        const dstEntry = matches.getEntry(matches.lengthEntries - 1);
        frameMapBuilder.setFrames(
          dstEntry.getIndex(),
          srcEntry.getFramesRange(),
        );
      }
    });

    const frameMap = frameMapBuilder.build();
    surfaceFlinger.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
  }

  private async tryPropagateFromSurfaceFlingerToTransactions() {
    const frameMapBuilder = this.tryStartFrameMapping(
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS,
    );
    if (!frameMapBuilder) {
      return;
    }

    const transactions = assertDefined(
      this.traces.getTrace(TraceType.TRANSACTIONS),
    );
    const transactionEntries = await transactions.customQuery(
      CustomQueryType.VSYNCID,
    );

    const surfaceFlinger = assertDefined(
      this.traces.getTrace(TraceType.SURFACE_FLINGER),
    );
    const surfaceFlingerEntries = await surfaceFlinger.customQuery(
      CustomQueryType.VSYNCID,
    );

    const vsyncIdToFrames = new Map<bigint, FramesRange>();

    surfaceFlingerEntries.forEach((srcEntry) => {
      const vsyncId = srcEntry.getValue();
      const srcFrames = srcEntry.getFramesRange();
      if (!srcFrames) {
        return;
      }
      let frames = vsyncIdToFrames.get(vsyncId);
      if (!frames) {
        frames = {start: Number.MAX_VALUE, end: Number.MIN_VALUE};
      }
      frames.start = Math.min(frames.start, srcFrames.start);
      frames.end = Math.max(frames.end, srcFrames.end);
      vsyncIdToFrames.set(vsyncId, frames);
    });

    transactionEntries.forEach((dstEntry) => {
      const vsyncId = dstEntry.getValue();
      const frames = vsyncIdToFrames.get(vsyncId);
      if (frames === undefined) {
        return;
      }
      frameMapBuilder.setFrames(dstEntry.getIndex(), frames);
    });

    const frameMap = frameMapBuilder.build();
    transactions.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
  }

  private tryPropagateFromTransactionsToWindowManager() {
    const frameMapBuilder = this.tryStartFrameMapping(
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
    );
    if (!frameMapBuilder) {
      return;
    }

    const windowManager = assertDefined(
      this.traces.getTrace(TraceType.WINDOW_MANAGER),
    );
    const transactions = assertDefined(
      this.traces.getTrace(TraceType.TRANSACTIONS),
    );

    let prevWindowManagerEntry: TraceEntry<object> | undefined;
    windowManager.forEachEntry((windowManagerEntry) => {
      if (prevWindowManagerEntry) {
        const matches = transactions.sliceTime(
          prevWindowManagerEntry.getTimestamp(),
          windowManagerEntry.getTimestamp(),
        );
        frameMapBuilder.setFrames(
          prevWindowManagerEntry.getIndex(),
          matches.getFramesRange(),
        );
      }
      prevWindowManagerEntry = windowManagerEntry;
    });

    if (windowManager.lengthEntries > 0) {
      const lastWindowManagerEntry = windowManager.getEntry(-1);
      const startSearchTime = lastWindowManagerEntry.getTimestamp();
      const endSearchTime = startSearchTime.add(
        FrameMapper.MAX_UI_PIPELINE_LATENCY_NS,
      );
      const matches = transactions.sliceTime(startSearchTime, endSearchTime);
      frameMapBuilder.setFrames(
        lastWindowManagerEntry.getIndex(),
        matches.getFramesRange(),
      );
    }

    const frameMap = frameMapBuilder.build();
    windowManager.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
  }

  private tryPropagateFromWindowManagerToProtoLog() {
    const frameMapBuilder = this.tryStartFrameMapping(
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
    );
    if (!frameMapBuilder) {
      return;
    }

    const protoLog = assertDefined(this.traces.getTrace(TraceType.PROTO_LOG));
    const windowManager = assertDefined(
      this.traces.getTrace(TraceType.WINDOW_MANAGER),
    );

    windowManager.forEachEntry((prevSrcEntry) => {
      const srcEntryIndex = prevSrcEntry.getIndex() + 1;
      const srcEntry =
        srcEntryIndex < windowManager.lengthEntries
          ? windowManager.getEntry(srcEntryIndex)
          : undefined;
      if (srcEntry === undefined) {
        return;
      }
      const startSearchTime = prevSrcEntry.getTimestamp().add(1n);
      const endSearchTime = srcEntry.getTimestamp().add(1n);
      const matches = protoLog.sliceTime(startSearchTime, endSearchTime);
      matches.forEachEntry((dstEntry) => {
        frameMapBuilder.setFrames(
          dstEntry.getIndex(),
          srcEntry.getFramesRange(),
        );
      });
    });

    if (windowManager.lengthEntries > 0) {
      const firstEntry = windowManager.getEntry(0);
      const startSearchTime = firstEntry
        .getTimestamp()
        .add(-FrameMapper.MAX_UI_PIPELINE_LATENCY_NS);
      const endSearchTime = firstEntry.getTimestamp().add(1n);
      const matches = protoLog.sliceTime(startSearchTime, endSearchTime);
      matches.forEachEntry((dstEntry) => {
        frameMapBuilder.setFrames(
          dstEntry.getIndex(),
          firstEntry.getFramesRange(),
        );
      });
    }

    const frameMap = frameMapBuilder.build();
    protoLog.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
  }

  private tryPropagateFromWindowManagerToIme() {
    const imeTypes = [
      TraceType.INPUT_METHOD_CLIENTS,
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
      TraceType.INPUT_METHOD_SERVICE,
    ];
    for (const imeType of imeTypes) {
      const frameMapBuilder = this.tryStartFrameMapping(
        TraceType.WINDOW_MANAGER,
        imeType,
      );
      if (frameMapBuilder) {
        this.propagateFromWindowManagerToIme(imeType, frameMapBuilder);
      }
    }
  }

  private propagateFromWindowManagerToIme(
    imeTraceType: TraceType,
    frameMapBuilder: FrameMapBuilder,
  ) {
    // Value used to narrow time-based searches of corresponding WindowManager entries
    const MAX_TIME_DIFFERENCE_NS = 200000000n; // 200 ms

    const ime = assertDefined(this.traces.getTrace(imeTraceType));
    const windowManager = assertDefined(
      this.traces.getTrace(TraceType.WINDOW_MANAGER),
    );
    const abs = (n: bigint): bigint => (n < 0n ? -n : n);

    ime.forEachEntry((dstEntry) => {
      const srcEntry = windowManager.findClosestEntry(dstEntry.getTimestamp());
      if (!srcEntry) {
        return;
      }
      const timeDifferenceNs = abs(
        srcEntry.getTimestamp().getValueNs() -
          dstEntry.getTimestamp().getValueNs(),
      );
      if (timeDifferenceNs > MAX_TIME_DIFFERENCE_NS) {
        return;
      }
      frameMapBuilder.setFrames(dstEntry.getIndex(), srcEntry.getFramesRange());
    });

    const frameMap = frameMapBuilder.build();
    ime.setFrameInfo(frameMap, frameMap.getFullTraceFramesRange());
  }

  private tryStartFrameMapping(
    srcTraceType: TraceType,
    dstTraceType: TraceType,
  ): FrameMapBuilder | undefined {
    const srcTrace = this.traces.getTrace(srcTraceType);
    const dstTrace = this.traces.getTrace(dstTraceType);
    if (!srcTrace || !dstTrace || !srcTrace.hasFrameInfo()) {
      return undefined;
    }

    const framesRange = srcTrace.getFramesRange();
    const lengthFrames = framesRange ? framesRange.end : 0;
    return new FrameMapBuilder(dstTrace.lengthEntries, lengthFrames);
  }
}
