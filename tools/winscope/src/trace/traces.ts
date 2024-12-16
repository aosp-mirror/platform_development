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

import {Timestamp} from 'common/time/time';
import {AbsoluteFrameIndex} from './index_types';
import {Trace} from './trace';
import {TraceEntryTypeMap, TraceType} from './trace_type';

export class Traces {
  private traces = new Set<Trace<{}>>();

  addTrace(trace: Trace<{}>) {
    this.traces.add(trace);
  }

  getTrace<T extends TraceType>(
    type: T,
  ): Trace<TraceEntryTypeMap[T]> | undefined {
    let longestTraceWithMatchingType: Trace<{}> | undefined;
    this.traces.forEach((trace) => {
      if (trace.type !== type) {
        return;
      }

      // If multiple traces match the target type, return the longest one.
      // Assuming that covering this scenario is good enough (hopefully):
      // - Two traces have the same type because one is a dump (e.g. SF trace + dump)
      // - A viewer needs to access another trace (e.g. IME viewers accesses SF trace)
      if (
        !longestTraceWithMatchingType ||
        trace.lengthEntries > longestTraceWithMatchingType.lengthEntries
      ) {
        longestTraceWithMatchingType = trace;
      }
    });
    return longestTraceWithMatchingType as
      | Trace<TraceEntryTypeMap[T]>
      | undefined;
  }

  getTraces<T extends TraceType>(type: T): Array<Trace<TraceEntryTypeMap[T]>> {
    return Array.from(this.traces).filter(
      (trace) => trace.type === type,
    ) as Array<Trace<TraceEntryTypeMap[T]>>;
  }

  deleteTrace(trace: Trace<{}>) {
    this.traces.delete(trace);
  }

  hasTrace(trace: Trace<{}>) {
    return this.traces.has(trace);
  }

  sliceTime(start?: Timestamp, end?: Timestamp): Traces {
    const slice = new Traces();
    this.traces.forEach((trace) => {
      slice.addTrace(trace.sliceTime(start, end));
    });
    return slice;
  }

  sliceFrames(start?: AbsoluteFrameIndex, end?: AbsoluteFrameIndex): Traces {
    const slice = new Traces();
    this.traces.forEach((trace) => {
      slice.addTrace(trace.sliceFrames(start, end));
    });
    return slice;
  }

  forEachTrace(callback: (trace: Trace<{}>, type: TraceType) => void): void {
    this.traces.forEach((trace, type) => {
      callback(trace, trace.type);
    });
  }

  mapTrace<T>(callback: (trace: Trace<{}>, type: TraceType) => T): T[] {
    const result: T[] = [];
    this.forEachTrace((trace, type) => {
      result.push(callback(trace, type));
    });
    return result;
  }

  forEachFrame(
    callback: (traces: Traces, index: AbsoluteFrameIndex) => void,
  ): void {
    let startFrameIndex: AbsoluteFrameIndex = Number.MAX_VALUE;
    let endFrameIndex: AbsoluteFrameIndex = Number.MIN_VALUE;

    this.traces.forEach((trace) => {
      const framesRange = trace.getFramesRange();
      if (framesRange && framesRange.start < framesRange.end) {
        startFrameIndex = Math.min(startFrameIndex, framesRange.start);
        endFrameIndex = Math.max(endFrameIndex, framesRange.end);
      }
    });

    for (let i = startFrameIndex; i < endFrameIndex; ++i) {
      callback(this.sliceFrames(i, i + 1), i);
    }
  }

  mapFrame<T>(callback: (traces: Traces, index: AbsoluteFrameIndex) => T): T[] {
    const result: T[] = [];
    this.forEachFrame((traces, index) => {
      result.push(callback(traces, index));
    });
    return result;
  }

  getSize(): number {
    return this.traces.size;
  }

  [Symbol.iterator]() {
    return this.traces.values();
  }
}
