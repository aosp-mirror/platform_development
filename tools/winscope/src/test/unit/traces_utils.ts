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
import {AbsoluteFrameIndex, Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {TraceUtils} from './trace_utils';

export class TracesUtils {
  static extractTraces(traces: Traces): Array<Trace<{}>> {
    return traces.mapTrace((trace) => trace);
  }

  static async extractEntries(
    traces: Traces,
  ): Promise<Map<TraceType, Array<{}>>> {
    const entries = new Map<TraceType, Array<{}>>();

    const promises = traces.mapTrace(async (trace) => {
      entries.set(trace.type, await TraceUtils.extractEntries(trace));
    });
    await Promise.all(promises);

    return entries;
  }

  static async extractFrames(
    traces: Traces,
  ): Promise<Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>> {
    const frames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();

    const framePromises = traces.mapFrame(async (frame, index) => {
      frames.set(index, new Map<TraceType, Array<{}>>());
      const tracePromises = frame.mapTrace(async (trace, type) => {
        assertDefined(frames.get(index)).set(
          type,
          await TraceUtils.extractEntries(trace),
        );
      });
      await Promise.all(tracePromises);
    });
    await Promise.all(framePromises);

    return frames;
  }
}
