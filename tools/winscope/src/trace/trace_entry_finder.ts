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

import {Trace, TraceEntry} from './trace';
import {TracePosition} from './trace_position';
import {TraceTypeUtils} from './trace_type';

export class TraceEntryFinder {
  static findCorrespondingEntry<T>(
    trace: Trace<T>,
    position: TracePosition,
  ): TraceEntry<T> | undefined {
    if (position.entry?.getFullTrace().type === trace.type) {
      return position.entry as TraceEntry<T>;
    }

    if (position.frame !== undefined && trace.hasFrameInfo()) {
      const frame = trace.getFrame(position.frame);
      if (frame.lengthEntries > 0) {
        return frame.getEntry(0);
      }
    }

    if (position.entry) {
      const entryTraceType = position.entry.getFullTrace().type;
      const timestamp = position.entry.getTimestamp();
      if (TraceTypeUtils.compareByUiPipelineOrder(entryTraceType, trace.type)) {
        return (
          trace.findFirstGreaterEntry(timestamp) ??
          trace.findFirstGreaterOrEqualEntry(timestamp)
        );
      } else {
        return (
          trace.findLastLowerEntry(timestamp) ??
          trace.findLastLowerOrEqualEntry(timestamp)
        );
      }
    }

    return trace.findLastLowerOrEqualEntry(position.timestamp);
  }
}
