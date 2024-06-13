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

import {Timestamp} from 'common/time';
import {AbsoluteFrameIndex} from './index_types';
import {TraceEntry} from './trace';

export class TracePosition {
  static fromTimestamp(timestamp: Timestamp): TracePosition {
    return new TracePosition(timestamp);
  }

  static fromTraceEntry(
    entry: TraceEntry<{}>,
    explicitTimestamp?: Timestamp,
  ): TracePosition {
    let frame: AbsoluteFrameIndex | undefined;
    if (entry.getFullTrace().hasFrameInfo()) {
      const frames = entry.getFramesRange();
      frame = frames && frames.start < frames.end ? frames.start : undefined;
    }
    const timestamp = explicitTimestamp
      ? explicitTimestamp
      : entry.getTimestamp();
    return new TracePosition(timestamp, frame, entry);
  }

  isEqual(other: TracePosition): boolean {
    return (
      this.timestamp.getValueNs() === other.timestamp.getValueNs() &&
      this.frame === other.frame &&
      this.entry?.getFullTrace().type === other.entry?.getFullTrace().type &&
      this.entry?.getIndex() === other.entry?.getIndex()
    );
  }

  private constructor(
    readonly timestamp: Timestamp,
    readonly frame?: AbsoluteFrameIndex,
    readonly entry?: TraceEntry<{}>,
  ) {}
}
