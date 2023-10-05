/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {FunctionUtils} from 'common/function_utils';
import {TimeUtils} from 'common/time_utils';
import {ScreenRecordingUtils} from 'trace/screen_recording_utils';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {assertDefined} from '../common/assert_utils';

export type TracePositionCallbackType = (position: TracePosition) => void;
export interface TimeRange {
  from: Timestamp;
  to: Timestamp;
}

export class TimelineData {
  private traces = new Traces();
  private screenRecordingVideo?: Blob;
  private timestampType?: TimestampType;
  private firstEntry?: TraceEntry<{}>;
  private lastEntry?: TraceEntry<{}>;
  private explicitlySetPosition?: TracePosition;
  private explicitlySetSelection?: TimeRange;
  private activeViewTraceTypes: TraceType[] = []; // dependencies of current active view
  private onTracePositionUpdate: TracePositionCallbackType = FunctionUtils.DO_NOTHING;

  initialize(traces: Traces, screenRecordingVideo: Blob | undefined) {
    this.clear();

    this.traces = traces;
    this.screenRecordingVideo = screenRecordingVideo;
    this.firstEntry = this.findFirstEntry();
    this.lastEntry = this.findLastEntry();
    this.timestampType = this.firstEntry?.getTimestamp().getType();

    const position = this.getCurrentPosition();
    if (position) {
      this.onTracePositionUpdate(position);
    }
  }

  setOnTracePositionUpdate(callback: TracePositionCallbackType) {
    this.onTracePositionUpdate = callback;
  }

  getCurrentPosition(): TracePosition | undefined {
    if (this.explicitlySetPosition) {
      return this.explicitlySetPosition;
    }
    const firstActiveEntry = this.getFirstEntryOfActiveViewTraces();
    if (firstActiveEntry) {
      return TracePosition.fromTraceEntry(firstActiveEntry);
    }
    if (this.firstEntry) {
      return TracePosition.fromTraceEntry(this.firstEntry);
    }
    return undefined;
  }

  setPosition(position: TracePosition | undefined) {
    if (!this.hasTimestamps()) {
      console.warn('Attempted to set position on traces with no timestamps/entries...');
      return;
    }

    if (position) {
      if (this.timestampType === undefined) {
        throw Error('Attempted to set explicit position but no timestamp type is available');
      }
      if (position.timestamp.getType() !== this.timestampType) {
        throw Error('Attempted to set explicit position with incompatible timestamp type');
      }
    }

    this.applyOperationAndNotifyIfCurrentPositionChanged(() => {
      this.explicitlySetPosition = position;
    });
  }

  setActiveViewTraceTypes(types: TraceType[]) {
    this.applyOperationAndNotifyIfCurrentPositionChanged(() => {
      this.activeViewTraceTypes = types;
    });
  }

  getTimestampType(): TimestampType | undefined {
    return this.timestampType;
  }

  getFullTimeRange(): TimeRange {
    if (!this.firstEntry || !this.lastEntry) {
      throw Error('Trying to get full time range when there are no timestamps');
    }
    return {
      from: this.firstEntry.getTimestamp(),
      to: this.lastEntry.getTimestamp(),
    };
  }

  getSelectionTimeRange(): TimeRange {
    if (this.explicitlySetSelection === undefined) {
      return this.getFullTimeRange();
    } else {
      return this.explicitlySetSelection;
    }
  }

  setSelectionTimeRange(selection: TimeRange) {
    this.explicitlySetSelection = selection;
  }

  getTraces(): Traces {
    return this.traces;
  }

  getScreenRecordingVideo(): Blob | undefined {
    return this.screenRecordingVideo;
  }

  searchCorrespondingScreenRecordingTimeSeconds(position: TracePosition): number | undefined {
    const trace = this.traces.getTrace(TraceType.SCREEN_RECORDING);
    if (!trace || trace.lengthEntries === 0) {
      return undefined;
    }

    const firstTimestamp = trace.getEntry(0).getTimestamp();
    const entry = TraceEntryFinder.findCorrespondingEntry(trace, position);
    if (!entry) {
      return undefined;
    }

    return ScreenRecordingUtils.timestampToVideoTimeSeconds(firstTimestamp, entry.getTimestamp());
  }

  hasTimestamps(): boolean {
    return this.firstEntry !== undefined;
  }

  hasMoreThanOneDistinctTimestamp(): boolean {
    return (
      this.hasTimestamps() &&
      this.firstEntry?.getTimestamp().getValueNs() !== this.lastEntry?.getTimestamp().getValueNs()
    );
  }

  getPreviousEntryFor(type: TraceType): TraceEntry<{}> | undefined {
    const trace = assertDefined(this.traces.getTrace(type));
    if (trace.lengthEntries === 0) {
      return undefined;
    }

    const currentIndex = this.findCurrentEntryFor(type)?.getIndex();
    if (currentIndex === undefined || currentIndex === 0) {
      return undefined;
    }

    return trace.getEntry(currentIndex - 1);
  }

  getNextEntryFor(type: TraceType): TraceEntry<{}> | undefined {
    const trace = assertDefined(this.traces.getTrace(type));
    if (trace.lengthEntries === 0) {
      return undefined;
    }

    const currentIndex = this.findCurrentEntryFor(type)?.getIndex();
    if (currentIndex === undefined) {
      return trace.getEntry(0);
    }

    if (currentIndex + 1 >= trace.lengthEntries) {
      return undefined;
    }

    return trace.getEntry(currentIndex + 1);
  }

  findCurrentEntryFor(type: TraceType): TraceEntry<{}> | undefined {
    const position = this.getCurrentPosition();
    if (!position) {
      return undefined;
    }
    return TraceEntryFinder.findCorrespondingEntry(
      assertDefined(this.traces.getTrace(type)),
      position
    );
  }

  moveToPreviousEntryFor(type: TraceType) {
    const prevEntry = this.getPreviousEntryFor(type);
    if (prevEntry !== undefined) {
      this.setPosition(TracePosition.fromTraceEntry(prevEntry));
    }
  }

  moveToNextEntryFor(type: TraceType) {
    const nextEntry = this.getNextEntryFor(type);
    if (nextEntry !== undefined) {
      this.setPosition(TracePosition.fromTraceEntry(nextEntry));
    }
  }

  clear() {
    this.applyOperationAndNotifyIfCurrentPositionChanged(() => {
      this.traces = new Traces();
      this.firstEntry = undefined;
      this.lastEntry = undefined;
      this.explicitlySetPosition = undefined;
      this.timestampType = undefined;
      this.explicitlySetSelection = undefined;
      this.screenRecordingVideo = undefined;
      this.activeViewTraceTypes = [];
    });
  }

  private findFirstEntry(): TraceEntry<{}> | undefined {
    let first: TraceEntry<{}> | undefined = undefined;

    this.traces.forEachTrace((trace) => {
      if (trace.lengthEntries === 0) {
        return;
      }
      const candidate = trace.getEntry(0);
      if (!first || candidate.getTimestamp() < first.getTimestamp()) {
        first = candidate;
      }
    });

    return first;
  }

  private findLastEntry(): TraceEntry<{}> | undefined {
    let last: TraceEntry<{}> | undefined = undefined;

    this.traces.forEachTrace((trace) => {
      if (trace.lengthEntries === 0) {
        return;
      }
      const candidate = trace.getEntry(trace.lengthEntries - 1);
      if (!last || candidate.getTimestamp() > last.getTimestamp()) {
        last = candidate;
      }
    });

    return last;
  }

  private getFirstEntryOfActiveViewTraces(): TraceEntry<{}> | undefined {
    const activeEntries = this.activeViewTraceTypes
      .map((traceType) => assertDefined(this.traces.getTrace(traceType)))
      .filter((trace) => trace.lengthEntries > 0)
      .map((trace) => trace.getEntry(0))
      .sort((a, b) => {
        return TimeUtils.compareFn(a.getTimestamp(), b.getTimestamp());
      });
    if (activeEntries.length === 0) {
      return undefined;
    }
    return activeEntries[0];
  }

  private applyOperationAndNotifyIfCurrentPositionChanged(op: () => void) {
    const prevPosition = this.getCurrentPosition();
    op();
    const currentPosition = this.getCurrentPosition();
    if (currentPosition && (!prevPosition || !currentPosition.isEqual(prevPosition))) {
      this.onTracePositionUpdate(currentPosition);
    }
  }
}
