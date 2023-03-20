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
import {TraceType} from './trace_type';

export class TraceEntryFinder {
  static readonly UI_PIPELINE_ORDER = [
    TraceType.INPUT_METHOD_CLIENTS,
    TraceType.INPUT_METHOD_SERVICE,
    TraceType.INPUT_METHOD_MANAGER_SERVICE,
    TraceType.PROTO_LOG,
    TraceType.WINDOW_MANAGER,
    TraceType.TRANSACTIONS,
    TraceType.SURFACE_FLINGER,
    TraceType.SCREEN_RECORDING,
  ];

  static findCorrespondingEntry<T>(
    trace: Trace<T>,
    position: TracePosition
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

      const indexPosition = TraceEntryFinder.UI_PIPELINE_ORDER.findIndex((type) => {
        return type === entryTraceType;
      });
      const indexTrace = TraceEntryFinder.UI_PIPELINE_ORDER.findIndex((type) => {
        return type === trace.type;
      });

      if (indexPosition !== undefined && indexTrace !== undefined) {
        if (indexPosition < indexTrace) {
          return trace.findFirstGreaterEntry(position.entry.getTimestamp());
        } else {
          return trace.findLastLowerEntry(position.entry.getTimestamp());
        }
      }
    }

    return trace.findLastLowerOrEqualEntry(position.timestamp);
  }
}
