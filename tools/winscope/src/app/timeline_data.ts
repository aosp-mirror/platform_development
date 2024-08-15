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

import {TimeRange, Timestamp} from 'common/time';
import {ComponentTimestampConverter} from 'common/timestamp_converter';
import {ScreenRecordingUtils} from 'trace/screen_recording_utils';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class TimelineData {
  private traces = new Traces();
  private screenRecordingVideo?: Blob;
  private firstEntry?: TraceEntry<object>;
  private lastEntry?: TraceEntry<object>;
  private explicitlySetPosition?: TracePosition;
  private explicitlySetSelection?: TimeRange;
  private explicitlySetZoomRange?: TimeRange;
  private lastReturnedCurrentPosition?: TracePosition;
  private lastReturnedFullTimeRange?: TimeRange;
  private lastReturnedCurrentEntries = new Map<
    Trace<object>,
    TraceEntry<any> | undefined
  >();
  private activeTrace: Trace<object> | undefined;
  private transitions: PropertyTreeNode[] = []; // cached trace entries to avoid TP and object creation latencies each time transition timeline is redrawn
  private timestampConverter: ComponentTimestampConverter | undefined;

  async initialize(
    traces: Traces,
    screenRecordingVideo: Blob | undefined,
    timestampConverter: ComponentTimestampConverter,
  ) {
    this.clear();

    this.timestampConverter = timestampConverter;

    this.traces = new Traces();
    traces.forEachTrace((trace, type) => {
      // Filter out empty traces or dumps with invalid timestamp (would mess up the timeline)
      if (trace.lengthEntries === 0 || trace.isDumpWithoutTimestamp()) {
        return;
      }

      this.traces.addTrace(trace);
    });

    const transitionTrace = this.traces.getTrace(TraceType.TRANSITION);
    if (transitionTrace) {
      try {
        this.transitions = await Promise.all(
          transitionTrace.mapEntry(async (entry) => await entry.getValue()),
        );
      } catch (error) {
        transitionTrace.setCorruptedState(
          true,
          'Cannot parse all transitions.',
        );
        throw error;
      }
    }

    this.screenRecordingVideo = screenRecordingVideo;
    this.firstEntry = this.findFirstEntry();
    this.lastEntry = this.findLastEntry();

    const tracesSortedByDisplayOrder = traces
      .mapTrace((trace) => trace)
      .filter(
        (trace) =>
          TraceTypeUtils.isTraceTypeWithViewer(trace.type) &&
          trace.type !== TraceType.SCREEN_RECORDING,
      )
      .sort((a, b) => {
        return TraceTypeUtils.compareByDisplayOrder(a.type, b.type);
      });
    if (tracesSortedByDisplayOrder.length > 0) {
      this.trySetActiveTrace(tracesSortedByDisplayOrder[0]);
    }
  }

  getTransitions(): PropertyTreeNode[] {
    return this.transitions;
  }

  getTimestampConverter(): ComponentTimestampConverter | undefined {
    return this.timestampConverter;
  }

  getCurrentPosition(): TracePosition | undefined {
    if (this.explicitlySetPosition) {
      return this.explicitlySetPosition;
    }

    let currentPosition: TracePosition | undefined = undefined;
    if (this.firstEntry) {
      currentPosition = TracePosition.fromTraceEntry(this.firstEntry);
    }

    const firstActiveEntry = this.getFirstEntryOfActiveViewTrace();
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

    this.explicitlySetPosition = position;
  }

  makePositionFromActiveTrace(timestamp: Timestamp): TracePosition {
    if (!this.activeTrace) {
      return TracePosition.fromTimestamp(timestamp);
    }

    const entry = this.activeTrace.findClosestEntry(timestamp);
    if (!entry) {
      return TracePosition.fromTimestamp(timestamp);
    }

    return TracePosition.fromTraceEntry(entry, timestamp);
  }

  trySetActiveTrace(trace: Trace<object>): boolean {
    const isTraceWithValidTimestamps = this.traces.hasTrace(trace);
    if (this.activeTrace !== trace && isTraceWithValidTimestamps) {
      this.activeTrace = trace;
      return true;
    }
    return false;
  }

  getActiveTrace() {
    return this.activeTrace;
  }

  getFullTimeRange(): TimeRange {
    if (!this.firstEntry || !this.lastEntry) {
      throw new Error(
        'Trying to get full time range when there are no timestamps',
      );
    }

    const fullTimeRange = new TimeRange(
      this.firstEntry.getTimestamp(),
      this.lastEntry.getTimestamp(),
    );

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

  hasTrace(trace: Trace<object>): boolean {
    return this.traces.hasTrace(trace);
  }

  getScreenRecordingVideo(): Blob | undefined {
    return this.screenRecordingVideo;
  }

  searchCorrespondingScreenRecordingTimeSeconds(
    position: TracePosition,
  ): number | undefined {
    const trace = this.traces.getTrace(TraceType.SCREEN_RECORDING);
    if (!trace) {
      return undefined;
    }

    const firstTimestamp = trace.getEntry(0).getTimestamp();
    const entry = TraceEntryFinder.findCorrespondingEntry(trace, position);
    if (!entry) {
      return undefined;
    }

    return ScreenRecordingUtils.timestampToVideoTimeSeconds(
      firstTimestamp.getValueNs(),
      entry.getTimestamp().getValueNs(),
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

  getPreviousEntryFor(trace: Trace<object>): TraceEntry<object> | undefined {
    if (trace.lengthEntries === 0) {
      return undefined;
    }

    const currentIndex = this.findCurrentEntryFor(trace)?.getIndex();
    if (currentIndex === undefined || currentIndex === 0) {
      return undefined;
    }

    return trace.getEntry(currentIndex - 1);
  }

  getNextEntryFor(trace: Trace<object>): TraceEntry<object> | undefined {
    if (trace.lengthEntries === 0) {
      return undefined;
    }

    const currentIndex = this.findCurrentEntryFor(trace)?.getIndex();
    if (currentIndex === undefined) {
      return trace.getEntry(0);
    }

    if (currentIndex + 1 >= trace.lengthEntries) {
      return undefined;
    }

    return trace.getEntry(currentIndex + 1);
  }

  findCurrentEntryFor(trace: Trace<object>): TraceEntry<object> | undefined {
    const position = this.getCurrentPosition();
    if (!position) {
      return undefined;
    }

    const entry = TraceEntryFinder.findCorrespondingEntry(trace, position);

    if (
      this.lastReturnedCurrentEntries.get(trace)?.getIndex() !==
      entry?.getIndex()
    ) {
      this.lastReturnedCurrentEntries.set(trace, entry);
    }

    return this.lastReturnedCurrentEntries.get(trace);
  }

  moveToPreviousEntryFor(trace: Trace<object>) {
    const prevEntry = this.getPreviousEntryFor(trace);
    if (prevEntry !== undefined) {
      this.setPosition(TracePosition.fromTraceEntry(prevEntry));
    }
  }

  moveToNextEntryFor(trace: Trace<object>) {
    const nextEntry = this.getNextEntryFor(trace);
    if (nextEntry !== undefined) {
      this.setPosition(TracePosition.fromTraceEntry(nextEntry));
    }
  }

  clear() {
    this.traces = new Traces();
    this.firstEntry = undefined;
    this.lastEntry = undefined;
    this.explicitlySetPosition = undefined;
    this.explicitlySetSelection = undefined;
    this.lastReturnedCurrentPosition = undefined;
    this.screenRecordingVideo = undefined;
    this.lastReturnedFullTimeRange = undefined;
    this.lastReturnedCurrentEntries.clear();
    this.activeTrace = undefined;
  }

  private findFirstEntry(): TraceEntry<{}> | undefined {
    let first: TraceEntry<{}> | undefined;

    this.traces.forEachTrace((trace) => {
      let candidate: TraceEntry<{}> | undefined;
      for (let i = 0; i < trace.lengthEntries; i++) {
        const entry = trace.getEntry(i);
        if (entry.hasValidTimestamp()) {
          candidate = entry;
          break;
        }
      }
      if (
        candidate &&
        (!first || candidate.getTimestamp() < first.getTimestamp())
      ) {
        first = candidate;
      }
    });

    return first;
  }

  private findLastEntry(): TraceEntry<{}> | undefined {
    let last: TraceEntry<{}> | undefined = undefined;

    this.traces.forEachTrace((trace) => {
      const candidate = trace.getEntry(trace.lengthEntries - 1);
      if (!last || candidate.getTimestamp() > last.getTimestamp()) {
        last = candidate;
      }
    });

    return last;
  }

  private getFirstEntryOfActiveViewTrace(): TraceEntry<{}> | undefined {
    if (!this.activeTrace) {
      return undefined;
    }
    return this.activeTrace.getEntry(0);
  }
}
