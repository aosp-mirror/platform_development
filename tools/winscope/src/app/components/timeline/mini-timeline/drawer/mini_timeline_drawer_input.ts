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

import {Transformer} from 'app/components/timeline/mini-timeline/transformer';
import {Segment} from 'app/components/timeline/segment';
import {TimelineUtils} from 'app/components/timeline/timeline_utils';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {TimeRange, Timestamp} from 'common/time';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  MiniCanvasDrawerData,
  TimelineTrace,
  TimelineTraces,
} from './mini_canvas_drawer_data';

export class MiniTimelineDrawerInput {
  constructor(
    public fullRange: TimeRange,
    public selectedPosition: Timestamp,
    public selection: TimeRange,
    public zoomRange: TimeRange,
    public traces: Array<Trace<object>>,
    public timelineData: TimelineData,
    public bookmarks: Timestamp[],
    public isDarkMode: boolean,
  ) {}

  transform(mapToRange: Segment): MiniCanvasDrawerData {
    const transformer = new Transformer(
      this.zoomRange,
      mapToRange,
      assertDefined(this.timelineData.getTimestampConverter()),
    );

    return new MiniCanvasDrawerData(
      transformer.transform(this.selectedPosition),
      {
        from: transformer.transform(this.selection.from),
        to: transformer.transform(this.selection.to),
      },
      () => {
        return this.transformTracesTimestamps(transformer);
      },
      transformer,
      this.transformBookmarks(transformer),
    );
  }

  private async transformTracesTimestamps(
    transformer: Transformer,
  ): Promise<TimelineTraces> {
    const transformedTraceSegments = new Map<Trace<object>, TimelineTrace>();

    this.traces.forEach((trace) => {
      const activeEntry = this.timelineData.findCurrentEntryFor(trace);

      if (trace.type === TraceType.TRANSITION) {
        // Transition trace is a special case, with entries with time ranges
        transformedTraceSegments.set(trace, {
          points: [],
          activePoint: undefined,
          segments: this.transformTransitionTraceTimestamps(
            transformer,
            trace as Trace<PropertyTreeNode>,
          ),
          activeSegment: activeEntry
            ? this.transformTransitionEntry(
                transformer,
                activeEntry as TraceEntry<PropertyTreeNode>,
              )
            : undefined,
        });
      } else {
        transformedTraceSegments.set(trace, {
          points: this.transformTraceTimestamps(transformer, trace),
          activePoint: activeEntry
            ? transformer.transform(activeEntry.getTimestamp())
            : undefined,
          segments: [],
          activeSegment: undefined,
        });
      }
    });

    return transformedTraceSegments;
  }

  private transformTransitionTraceTimestamps(
    transformer: Transformer,
    trace: Trace<PropertyTreeNode>,
  ): Segment[] {
    return trace
      .mapEntry((entry) => this.transformTransitionEntry(transformer, entry))
      .filter((it) => it !== undefined) as Segment[];
  }

  private transformBookmarks(transformer: Transformer): number[] {
    return this.bookmarks.map((bookmarkedTimestamp) =>
      transformer.transform(bookmarkedTimestamp),
    );
  }

  private transformTransitionEntry(
    transformer: Transformer,
    entry: TraceEntry<PropertyTreeNode>,
  ): Segment | undefined {
    const transition: PropertyTreeNode =
      this.timelineData.getTransitions()[entry.getIndex()];

    const timeRange = TimelineUtils.getTimeRangeForTransition(
      transition,
      this.selection,
      assertDefined(this.timelineData.getTimestampConverter()),
    );

    if (!timeRange) {
      return undefined;
    }

    return {
      from: transformer.transform(timeRange.from),
      to: transformer.transform(timeRange.to),
      unknownStart: TimelineUtils.isTransitionWithUnknownStart(transition),
      unknownEnd: TimelineUtils.isTransitionWithUnknownEnd(transition),
    };
  }

  private transformTraceTimestamps(
    transformer: Transformer,
    trace: Trace<{}>,
  ): number[] {
    const result: number[] = [];

    trace.forEachTimestamp((timestamp) => {
      result.push(transformer.transform(timestamp));
    });

    return result;
  }
}
