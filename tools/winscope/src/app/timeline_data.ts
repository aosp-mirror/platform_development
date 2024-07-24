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

import {assertDefined} from 'common/assert_utils';
import {
  INVALID_TIME_NS,
  TimeRange,
  Timestamp,
  TimestampType,
} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {ScreenRecordingUtils} from 'trace/screen_recording_utils';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';

export class TimelineData {
  private traces = new Traces();
  private screenRecordingVideo?: Blob;
  private timestampType?: TimestampType;
  private firstEntry?: TraceEntry<{}>;
  private lastEntry?: TraceEntry<{}>;
  private explicitlySetPosition?: TracePosition;
  private explicitlySetSelection?: TimeRange;
  private explicitlySetZoomRange?: TimeRange;
  private lastReturnedCurrentPosition?: TracePosition;
  private lastReturnedFullTimeRange?: TimeRange;
  private lastReturnedCurrentEntries = new Map<
    TraceType,
    TraceEntry<any> | undefined
  >();
  private activeViewTraceTypes: TraceType[] = []; // dependencies of current active view

  initialize(traces: Traces, screenRecordingVideo: Blob | undefined) {
    this.clear();

    this.traces = new Traces();
    traces.forEachTrace((trace, type) => {
      // Filter out dumps with invalid timestamp (would mess up the timeline)
      const isDump =
        trace.lengthEntries === 1 &&
        trace.getEntry(0).getTimestamp().getValueNs() === INVALID_TIME_NS;
      if (isDump) {
        return;
      }

      this.traces.setTrace(type, trace);
    });

    this.screenRecordingVideo = screenRecordingVideo;
    this.firstEntry = this.findFirstEntry();
    this.lastEntry = this.findLastEntry();
    this.timestampType = this.firstEntry?.getTimestamp().getType();

    const types = traces
      .mapTrace((trace, type) => type)
      .filter(
        (type) =>
          TraceTypeUtils.isTraceTypeWithViewer(type) &&
          type !== TraceType.SCREEN_RECORDING,
      )
      .sort(TraceTypeUtils.compareByDisplayOrder);
    if (types.length > 0) {
      this.setActiveViewTraceTypes([types[0]]);
    }
  }

  getCurrentPosition(): TracePosition | undefined {
    if (this.explicitlySetPosition) {
      return this.explicitlySetPosition;
    }

    let currentPosition: TracePosition | undefined = undefined;
    if (this.firstEntry) {
      currentPosition = TracePosition.fromTraceEntry(this.firstEntry);
    }

    const firstActiveEntry = this.getFirstEntryOfActiveViewTraces();
    if (firstActiveEntry) {
      currentPosition = TracePosition.fromTraceEntry(firstActiveEntry);
    }

    if (
      this.lastReturnedCurrentPosition === undefined ||
      currentPosition === undefined ||
      !this.lastReturnedCurrentPosition.isEqual(currentPosition)
    ) {
      this.lastReturnedCurrentPosition = currentPosition;
    }

    return this.lastReturnedCurrentPosition;
  }

  setPosition(position: TracePosition | undefined) {
    if (!this.hasTimestamps()) {
      console.warn(
        'Attempted to set position on traces with no timestamps/entries...',
      );
      return;
    }

    if (position) {
      if (this.timestampType === undefined) {
        throw Error(
          'Attempted to set explicit position but no timestamp type is available',
        );
      }
      if (position.timestamp.getType() !== this.timestampType) {
        throw Error(
          'Attempted to set explicit position with incompatible timestamp type',
        );
      }
    }

    this.explicitlySetPosition = position;
  }

  makePositionFromActiveTrace(timestamp: Timestamp): TracePosition {
    let trace: Trace<{}> | undefined;
    if (this.activeViewTraceTypes.length > 0) {
      trace = this.traces.getTrace(this.activeViewTraceTypes[0]);
    }

    if (!trace) {
      return TracePosition.fromTimestamp(timestamp);
    }

    const entry = trace.findClosestEntry(timestamp);
    if (!entry) {
      return TracePosition.fromTimestamp(timestamp);
    }

    return TracePosition.fromTraceEntry(entry, timestamp);
  }

  setActiveViewTraceTypes(types: TraceType[]) {
    this.activeViewTraceTypes = types;
  }

  getTimestampType(): TimestampType | undefined {
    return this.timestampType;
  }

  getFullTimeRange(): TimeRange {
    if (!this.firstEntry || !this.lastEntry) {
      throw Error('Trying to get full time range when there are no timestamps');
    }

    const fullTimeRange = {
      from: this.firstEntry.getTimestamp(),
      to: this.lastEntry.getTimestamp(),
    };

    if (
      this.lastReturnedFullTimeRange === undefined ||
      this.lastReturnedFullTimeRange.from.getValueNs() !==
        fullTimeRange.from.getValueNs() ||
      this.lastReturnedFullTimeRange.to.getValueNs() !==
        fullTimeRange.to.getValueNs()
    ) {
      this.lastReturnedFullTimeRange = fullTimeRange;
    }

    return this.lastReturnedFullTimeRange;
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

  getZoomRange(): TimeRange {
    if (this.explicitlySetZoomRange === undefined) {
      return this.getFullTimeRange();
    } else {
      return this.explicitlySetZoomRange;
    }
  }

  setZoom(zoomRange: TimeRange) {
    this.explicitlySetZoomRange = zoomRange;
  }

  getTraces(): Traces {
    return this.traces;
  }

  getScreenRecordingVideo(): Blob | undefined {
    return this.screenRecordingVideo;
  }

  searchCorrespondingScreenRecordingTimeSeconds(
    position: TracePosition,
  ): number | undefined {
    const trace = this.traces.getTrace(TraceType.SCREEN_RECORDING);
    if (!trace || trace.lengthEntries === 0) {
      return undefined;
    }

    const firstTimestamp = trace.getEntry(0).getTimestamp();
    const entry = TraceEntryFinder.findCorrespondingEntry(trace, position);
    if (!entry) {
      return undefined;
    }

    return ScreenRecordingUtils.timestampToVideoTimeSeconds(
      firstTimestamp,
      entry.getTimestamp(),
    );
  }

  hasTimestamps(): boolean {
    return this.firstEntry !== undefined;
  }

  hasMoreThanOneDistinctTimestamp(): boolean {
    return (
      this.hasTimestamps() &&
      this.firstEntry?.getTimestamp().getValueNs() !==
        this.lastEntry?.getTimestamp().getValueNs()
    );
  }

  getPreviousEntryFor(type: TraceType): TraceEntry<{}> | undefined {
    const trace = this.traces.getTrace(type);
    if (!trace || trace.lengthEntries === 0) {
      return undefined;
    }

    const currentIndex = this.findCurrentEntryFor(type)?.getIndex();
    if (currentIndex === undefined || currentIndex === 0) {
      return undefined;
    }

    return trace.getEntry(currentIndex - 1);
  }

  getNextEntryFor(type: TraceType): TraceEntry<{}> | undefined {
    const trace = this.traces.getTrace(type);
    if (!trace || trace.lengthEntries === 0) {
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

    const entry = TraceEntryFinder.findCorrespondingEntry(
      assertDefined(this.traces.getTrace(type)),
      position,
    );

    if (
      this.lastReturnedCurrentEntries.get(type)?.getIndex() !==
      entry?.getIndex()
    ) {
      this.lastReturnedCurrentEntries.set(type, entry);
    }

    return this.lastReturnedCurrentEntries.get(type);
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
    this.traces = new Traces();
    this.firstEntry = undefined;
    this.lastEntry = undefined;
    this.explicitlySetPosition = undefined;
    this.timestampType = undefined;
    this.explicitlySetSelection = undefined;
    this.lastReturnedCurrentPosition = undefined;
    this.screenRecordingVideo = undefined;
    this.lastReturnedFullTimeRange = undefined;
    this.lastReturnedCurrentEntries.clear();
    this.activeViewTraceTypes = [];
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
      .filter((it) => this.traces.getTrace(it) !== undefined)
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
}
