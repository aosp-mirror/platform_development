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

import {AbsoluteFrameIndex} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {TraceUtils} from './trace_utils';

export class TracesUtils {
  static extractEntries(traces: Traces): Map<TraceType, Array<{}>> {
    const entries = new Map<TraceType, Array<{}>>();

    traces.forEachTrace((trace) => {
      entries.set(trace.type, TraceUtils.extractEntries(trace));
    });

    return entries;
  }

  static extractFrames(traces: Traces): Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>> {
    const frames = new Map<AbsoluteFrameIndex, Map<TraceType, Array<{}>>>();

    traces.forEachFrame((frame, index) => {
      frames.set(index, new Map<TraceType, Array<{}>>());
      frame.forEachTrace((trace, type) => {
        frames.get(index)?.set(type, TraceUtils.extractEntries(trace));
      });
    });

    return frames;
  }
}
